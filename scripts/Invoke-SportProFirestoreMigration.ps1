[CmdletBinding()]
param(
    [ValidateSet('DryRun', 'Plan', 'Seed', 'Apply', 'Verify', 'Restore')]
    [string]$Mode = 'DryRun',
    [Parameter(Mandatory)][string]$Project,
    [Parameter(Mandatory)][string]$Workspace,
    [string]$DestinationProject,
    [string]$SourceEmulator,
    [string]$DestinationEmulator,
    [switch]$AuthorizeLiveWrite
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function New-Endpoint([string]$Id, [string]$Emulator, [bool]$Write) {
    if ($Id -cnotmatch '^[a-z][a-z0-9-]{3,35}$') { throw 'Identificador de proyecto inválido.' }
    if ($Emulator) {
        if ($Emulator -notmatch '^(127\.0\.0\.1|localhost):[0-9]+$' -or $Id -notlike 'demo-*') {
            throw 'Usa un proyecto demo-* y un emulador local.'
        }
        $hostUrl = 'http://' + $Emulator
        $token = 'owner'
    } else {
        if ($Write -and !$AuthorizeLiveWrite) { throw 'Una escritura remota requiere autorización explícita.' }
        if ($Id -like 'demo-*') { throw 'Indica el host local del emulador para el proyecto demo.' }
        $hostUrl = 'https://firestore.googleapis.com'
        $token = $env:SPORTPRO_FIRESTORE_TOKEN
        if (!$token) { throw 'Define SPORTPRO_FIRESTORE_TOKEN con un token IAM de corta duración.' }
    }
    $base = "projects/$Id/databases/(default)/documents"
    return @{ Url = "$hostUrl/v1/$base"; Base = $base; Headers = @{ Authorization = "Bearer $token" } }
}

function Encode-Path([string]$Path) {
    return (($Path.Split('/') | ForEach-Object { [Uri]::EscapeDataString($_) }) -join '/')
}

function Send-Firestore($Endpoint, [string]$Verb, [string]$Suffix, $Body, [switch]$AllowMissing) {
    $request = @{ Uri = $Endpoint.Url + $Suffix; Method = $Verb; Headers = $Endpoint.Headers; TimeoutSec = 30 }
    if ($null -ne $Body) {
        $request.ContentType = 'application/json; charset=utf-8'
        $request.Body = [Text.Encoding]::UTF8.GetBytes((ConvertTo-Json -InputObject $Body -Depth 100 -Compress))
    }
    try {
        $response = Invoke-WebRequest @request -UseBasicParsing
        return [Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray()) | ConvertFrom-Json
    }
    catch {
        if ($AllowMissing -and $_.Exception.Response -and [int]$_.Exception.Response.StatusCode -eq 404) { return $null }
        throw
    }
}

function Get-Property($Object, [string]$Name) {
    if ($null -ne $Object) {
        if ($Object -is [System.Collections.IDictionary]) { return $Object[$Name] }
        $member = $Object.PSObject.Properties[$Name]
        if ($member) { return $member.Value }
    }
}

function Get-Inventory($Endpoint, [string]$Parent = '') {
    $suffix = if ($Parent) { '/' + (Encode-Path $Parent) } else { '' }
    $cursor = ''
    do {
        $response = Send-Firestore $Endpoint Post "${suffix}:listCollectionIds" @{ pageSize = 250; pageToken = $cursor }
        foreach ($collection in @(Get-Property $response 'collectionIds')) {
            if (!$collection) { continue }
            $collectionPath = "$suffix/" + (Encode-Path $collection)
            $nextDoc = ''
            do {
                $query = "?pageSize=250&showMissing=true&pageToken=" + [Uri]::EscapeDataString($nextDoc)
                $page = Send-Firestore $Endpoint Get ($collectionPath + $query) $null
                foreach ($doc in @(Get-Property $page 'documents')) {
                    if (!$doc) { continue }
                    $relative = $doc.name.Substring($Endpoint.Base.Length + 1)
                    if (Get-Property $doc 'createTime') {
                        [pscustomobject]@{ Path = $relative; Fields = (Get-Property $doc 'fields'); UpdateTime = $doc.updateTime }
                    }
                    Get-Inventory $Endpoint $relative
                }
                $nextDoc = [string](Get-Property $page 'nextPageToken')
            } while ($nextDoc)
        }
        $cursor = [string](Get-Property $response 'nextPageToken')
    } while ($cursor)
}

function Write-SignedJson([string]$Path, $Data) {
    if ((Test-Path -LiteralPath $Path) -or (Test-Path -LiteralPath "$Path.sha256")) {
        throw "Ya existe un archivo del inventario o del plan en $Path."
    }
    ConvertTo-Json -InputObject $Data -Depth 100 | Set-Content -LiteralPath $Path -Encoding UTF8
    (Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash | Set-Content -LiteralPath "$Path.sha256" -Encoding ASCII
}

function Read-SignedJson([string]$Path) {
    if ((Get-FileHash -LiteralPath $Path -Algorithm SHA256).Hash -cne
        (Get-Content -LiteralPath "$Path.sha256" -Raw).Trim()) { throw "Huella inválida de $Path." }
    return Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
}

function TextField($Doc, [string]$Key) {
    return [string](Get-Property (Get-Property $Doc.Fields $Key) 'stringValue')
}

$rootEntities = @{
    Admins = 'admins'; players = 'players'; teams = 'teams'; matches = 'matches'; trainings = 'trainings'
    attendance = 'attendance'; announcements = 'announcements'; posts = 'posts'; notifications = 'notifications'
    reports = 'reports'; tryoutReports = 'reports'; summaries = 'summaries'
    tryouts = 'tryouts'; event_catalog = 'eventCatalog'; eventCancellationRequests = 'eventCancellations'; auditLogs = 'auditLogs'
}
$prefixes = @{
    admins = 'ADM'; directors = 'DT'; users = 'USU'; players = 'JUG'; teams = 'EQU'; matches = 'PAR'
    trainings = 'ENT'; attendance = 'ASI'; monthlyFees = 'CUO'; events = 'EVT'; goals = 'GOL'; cards = 'TAR'; substitutions = 'SUS'
    tryouts = 'PRU'; candidates = 'POS'; announcements = 'ANU'; posts = 'PUB'; comments = 'COM'
    callups = 'CON'; lineups = 'ALI'; reports = 'REP'; summaries = 'RES'
    notifications = 'NOT'; eventCatalog = 'CAT'; eventCancellations = 'SOL'; auditLogs = 'AUD'; eventHistory = 'HIS'
}

function Entity-For($Doc) {
    $parts = $Doc.Path.Split('/')
    if ($parts.Count -eq 2) {
        if ($parts[0] -ceq 'users') {
            switch (TextField $Doc 'role') {
                'ADM' { return 'admins' }
                'DT' { return 'directors' }
                default { return 'users' }
            }
        }
        return $rootEntities[$parts[0]]
    }
    if ($parts.Count -eq 6 -and $parts[0] -ceq 'matches' -and $parts[4] -ceq 'history') { return 'eventHistory' }
    if ($parts.Count -ne 4) { return }
    switch ($parts[2]) {
        'comments' { return 'comments' }
        'callups' { return 'callups' }
        'lineup' { return 'lineups' }
        'candidates' { return 'candidates' }
        'events' {
            switch (TextField $Doc 'type') {
                'GOL' { return 'goals' }
                { $_ -in 'TARJETA_ROJA', 'TARJETA_AMARILLA' } { return 'cards' }
                'CAMBIO' { return 'substitutions' }
                default { return 'events' }
            }
        }
    }
}

function Get-NumericId([string]$Entity, [string]$Id) {
    $prefix = $prefixes[$Entity]
    if ($Id -cmatch "^SP-$prefix-([0-9]{6})$") { return [int]$Matches[1] }
    return 0
}

function New-Plan($Inventory, [switch]$ExistingOnly) {
    $byPath = [System.Collections.Generic.Dictionary[string,object]]::new([StringComparer]::Ordinal)
    $max = @{}
    foreach ($entity in $prefixes.Keys) { $max[$entity] = 0 }
    foreach ($doc in $Inventory.Documents) {
        if ($doc.Path -cmatch '^players/[^/]+$') {
            $fees = Get-Property (Get-Property (Get-Property $doc.Fields 'monthlyFees') 'arrayValue') 'values'
            foreach ($fee in @($fees)) {
                $data = Get-Property (Get-Property $fee 'mapValue') 'fields'
                $number = Get-NumericId 'monthlyFees' (Get-Property (Get-Property $data 'id') 'stringValue')
                $max.monthlyFees = [Math]::Max($max.monthlyFees, $number)
            }
        }
        if ($doc.Path -cmatch '^counters/([^/]+)$' -and $max.ContainsKey($Matches[1])) {
            $last = Get-Property (Get-Property $doc.Fields 'lastId') 'integerValue'
            if ($null -eq $last -or [long]$last -lt 0 -or [long]$last -gt 999999) { throw "Contador inválido en $($doc.Path)." }
            $max[$Matches[1]] = [Math]::Max($max[$Matches[1]], [int]$last)
        }
    }
    $assigned = [System.Collections.Generic.Dictionary[string,string]]::new([StringComparer]::Ordinal)
    $sorted = @($Inventory.Documents | Sort-Object -Property @{Expression={ $_.Path.Split('/').Count }}, @{Expression={ $_.Path }})
    foreach ($doc in $sorted) {
        $entity = Entity-For $doc
        if (!$entity) { continue }
        $candidate = if ($doc.Path -clike 'users/*') { TextField $doc 'id' } else { $doc.Path.Split('/')[-1] }
        $number = Get-NumericId $entity $candidate
        if ($number -gt 0) {
            $max[$entity] = [Math]::Max($max[$entity], $number)
            $assigned[$doc.Path] = $candidate
        }
    }
    if ($ExistingOnly) { return [pscustomobject]@{ Counters = $max } }
    foreach ($doc in $sorted) {
        $entity = Entity-For $doc
        if (!$entity -or ($doc.Path -clike 'users/*' -and $entity -eq 'admins')) { continue }
        if (!$assigned.ContainsKey($doc.Path)) {
            $max[$entity]++
            if ($max[$entity] -gt 999999) { throw "Se agotaron los correlativos de $entity." }
            $assigned[$doc.Path] = 'SP-{0}-{1:D6}' -f $prefixes[$entity], $max[$entity]
        }
    }
    $adminByUid = [System.Collections.Generic.Dictionary[string,string]]::new([StringComparer]::Ordinal)
    foreach ($doc in $sorted | Where-Object Path -CLike 'Admins/*') {
        $authUid = TextField $doc 'authUid'
        if (!$authUid) { $authUid = TextField $doc 'id' }
        if ($authUid -and !$authUid.StartsWith('SP-')) {
            if ($adminByUid.ContainsKey($authUid)) { throw 'Hay dos fichas administrativas para un UID.' }
            $adminByUid[$authUid] = $assigned[$doc.Path]
        }
    }
    foreach ($doc in $sorted) {
        $parts = $doc.Path.Split('/')
        $oldKey = $parts[-1]
        $path = $doc.Path
        $businessId = $null
        if ($assigned.ContainsKey($doc.Path)) { $businessId = $assigned[$doc.Path] }
        if ($parts[0] -ceq 'users' -and (TextField $doc 'role') -eq 'ADM') {
            if (!$adminByUid.TryGetValue($oldKey, [ref]$businessId) -and !$assigned.ContainsKey($doc.Path)) {
                throw 'Falta vincular una ficha administrativa heredada a su UID en el inventario.'
            }
            if (!$businessId) { $businessId = $assigned[$doc.Path] }
        }
        if ($parts.Count -gt 2) {
            for ($length = $parts.Count - 2; $length -ge 2; $length -= 2) {
                $parent = $parts[0..($length - 1)] -join '/'
                if ($byPath.ContainsKey($parent)) {
                    $path = $byPath[$parent].To + '/' + ($parts[$length..($parts.Count - 1)] -join '/')
                    break
                }
            }
        }
        if ($businessId -and $parts[0] -cne 'users') {
            $path = $path.Substring(0, $path.LastIndexOf('/') + 1) + $businessId
        }
        $byPath[$doc.Path] = [pscustomobject]@{ From = $doc.Path; To = $path; Id = $businessId }
    }

    foreach ($doc in $sorted) {
        $entry = $byPath[$doc.Path]
        if ($entry.Id) { continue }
        $parts = $doc.Path.Split('/')
        $key = $parts[-1]
        $ref = switch ($parts[0]) {
            'summaries' { if ($parts.Count -eq 2) { "matches/$key" } }
            'reports' { if ($parts.Count -eq 2) { 'posts/' + (TextField $doc 'postId') } }
            'tryoutReports' { if ($parts.Count -eq 2) { 'tryouts/' + (TextField $doc 'tryoutId') } }
        }
        if ($parts.Count -eq 4) {
            $ref = switch ($parts[2]) {
                'callups' { "players/$key" }
                'lineup' { if ($key -ne 'starting') { "teams/$key" } }
                'attendanceHistory' { "trainings/$key" }
            }
        }
        if ($ref -and $byPath.ContainsKey($ref)) {
            $newKey = $byPath[$ref].To.Split('/')[-1]
            if ($parts[0] -in 'reports', 'tryoutReports') { $newKey += '_' + (TextField $doc 'reporterUid') }
            $entry.To = $entry.To.Substring(0, $entry.To.LastIndexOf('/') + 1) + $newKey
        }
    }
    $targets = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($entry in $byPath.Values) {
        if (!$targets.Add($entry.To)) { throw 'Dos documentos tienen el mismo destino. Revisa el inventario.' }
    }
    return [pscustomobject]@{ Mapping = @($byPath.Values | Sort-Object From); Counters = $max }
}

function Rewrite-Value($Value, [string]$Field, $Context) {
    if ($null -eq $Value) { return $Value }
    $string = Get-Property $Value 'stringValue'
    if ($string) {
        $collection = switch -Regex ($Field) {
            '^(teamId|homeTeamId|awayTeamId|rivalTeamId|lastLineupTeam)$' { 'teams' }
            '^(playerId|jugadorSaleId|jugadorEntraId|starterIds|substituteIds|substitutePlayerIds|playerIds)$' { 'players' }
            '^(matchId|matchIds)$' { 'matches' }
            '^(trainingId|duplicatedFromId)$' { 'trainings' }
            '^attendanceId$' { 'attendance' }
            '^postId$' { if ($Context.From -clike 'tryoutReports/*') { 'tryouts' } else { 'posts' } }
            '^tryoutId$' { 'tryouts' }
            '^relatedId$' {
                switch -Wildcard (TextField $Context.Document 'type') {
                    'MATCH*' { 'matches' }; 'TRAINING*' { 'trainings' }; 'ANNOUNCEMENT*' { 'announcements' }
                    'COMMUNITY*' { 'posts' }; 'SUMMARY*' { 'matches' }; '*LINK*' { 'players' }; '*JOIN*' { 'players' }
                }
            }
        }
        if ($collection -and $map.ContainsKey("$collection/$string")) {
            $Value.stringValue = $map["$collection/$string"].To.Split('/')[-1]
        } elseif ($Field -eq 'eventId') {
            $matchId = $Context.OriginalMatch
            $old = "matches/$matchId/events/$string"
            if ($map.ContainsKey($old)) { $Value.stringValue = $map[$old].To.Split('/')[-1] }
        }
    }
    $reference = Get-Property $Value 'referenceValue'
    if ($reference -and $reference.StartsWith($sourceBase + '/')) {
        $old = $reference.Substring($sourceBase.Length + 1)
        if (!$map.ContainsKey($old)) { throw 'Referencia Firestore interna sin documento en el inventario.' }
        $Value.referenceValue = $target.Base + '/' + $map[$old].To
    }
    $nested = Get-Property (Get-Property $Value 'mapValue') 'fields'
    if ($nested) {
        foreach ($property in $nested.PSObject.Properties) {
            $property.Value = Rewrite-Value $property.Value $property.Name $Context
        }
    }
    $array = Get-Property (Get-Property $Value 'arrayValue') 'values'
    foreach ($item in @($array)) { if ($null -ne $item) { $null = Rewrite-Value $item $Field $Context } }
    return $Value
}

function Canonical-Json($Value) {
    if ($Value -is [pscustomobject]) {
        $ordered = [ordered]@{}
        foreach ($p in ($Value.PSObject.Properties.Name | Sort-Object -CaseSensitive)) {
            $ordered[$p] = Canonical-Json $Value.$p
        }
        return ConvertTo-Json -InputObject $ordered -Compress -Depth 100
    }
    if ($Value -is [array]) {
        return ConvertTo-Json -InputObject @($Value | ForEach-Object { Canonical-Json $_ }) -Compress -Depth 100
    }
    return ConvertTo-Json -InputObject $Value -Compress -Depth 100
}

$backupFile = Join-Path $Workspace 'firestore-backup.json'
$planFile = Join-Path $Workspace 'firestore-plan.json'
if ($Mode -eq 'DryRun') {
    $source = New-Endpoint $Project $SourceEmulator $false
    if (!(Test-Path -LiteralPath $Workspace)) { New-Item -ItemType Directory -Path $Workspace | Out-Null }
    $docs = @(Get-Inventory $source)
    $backup = [pscustomobject]@{ Project = $Project; CapturedAt = [DateTime]::UtcNow.ToString('o'); Documents = $docs }
    Write-SignedJson $backupFile $backup
    Write-SignedJson $planFile (New-Plan $backup)
    $docs | Group-Object { $_.Path.Split('/')[0] } | Select-Object Name,Count
    return
}

$backup = Read-SignedJson $backupFile
if ($backup.Project -cne $Project) { throw 'El proyecto del respaldo es distinto.' }
if ($Mode -eq 'Plan') {
    Write-SignedJson $planFile (New-Plan $backup)
    return
}
$plan = Read-SignedJson $planFile
$sourceBase = "projects/$Project/databases/(default)/documents"
if (!$DestinationProject) { $DestinationProject = $Project }
if ($Mode -in 'Apply', 'Restore' -and ($DestinationProject -ceq $Project -or !$DestinationEmulator)) {
    throw 'Apply y Restore requieren un proyecto demo distinto en el emulador. El corte remoto se revisa por separado.'
}
$target = New-Endpoint $DestinationProject $DestinationEmulator ($Mode -ne 'Verify')
$map = [System.Collections.Generic.Dictionary[string,object]]::new([StringComparer]::Ordinal)
foreach ($entry in $plan.Mapping) { $map[$entry.From] = $entry }
if ($Mode -eq 'Seed') {
    $livePlan = New-Plan ([pscustomobject]@{ Documents = @(Get-Inventory $target) }) -ExistingOnly
    foreach ($counter in $prefixes.Keys) {
        $minimum = [Math]::Max([long](Get-Property $plan.Counters $counter), [long]$livePlan.Counters[$counter])
        $suffix = '/counters/' + (Encode-Path $counter)
        $existing = Send-Firestore $target Get $suffix $null -AllowMissing
        $old = if ($existing) { [long]$existing.fields.lastId.integerValue } else { 0 }
        if ($existing -and $old -ge $minimum) { continue }
        $guard = if ($existing) { 'currentDocument.updateTime=' + [Uri]::EscapeDataString($existing.updateTime) }
            else { 'currentDocument.exists=false' }
        $body = @{ fields = @{ lastId = @{ integerValue = [string]$minimum } } }
        $null = Send-Firestore $target Patch "${suffix}?updateMask.fieldPaths=lastId&$guard" $body
    }
    return
}

$restore = $Mode -eq 'Restore'
if ($restore) {
    $map = [System.Collections.Generic.Dictionary[string,object]]::new([StringComparer]::Ordinal)
    foreach ($record in $plan.Mapping) {
        $map[$record.From] = [pscustomobject]@{ To = $record.From }
    }
}

$linked = [System.Collections.Generic.Dictionary[string,object]]::new([StringComparer]::Ordinal)
$derived = [System.Collections.Generic.Dictionary[string,object]]::new([StringComparer]::Ordinal)
if (!$restore) {
    foreach ($doc in $backup.Documents) {
        $parts = $doc.Path.Split('/')
        $match = $null
        if ($doc.Path -cmatch '^summaries/[^/]+$') {
            $match = TextField $doc 'matchId'
            if (!$match) { $match = $parts[1] }
        } elseif ($doc.Path -cmatch '^matches/[^/]+/(callups|lineup)/[^/]+$') {
            $match = $parts[1]
        }
        if ($match) {
            $owner = "matches/$match"
            if (!$map.ContainsKey($owner)) { throw 'Falta el partido de una convocatoria, alineación o resumen.' }
            if (!$linked.ContainsKey($owner)) {
                $linked[$owner] = @{ callupIds = @{}; callupUserIds = @{}; lineupIds = @{}; summaryId = '' }
            }
            $links = $linked[$owner]
            $id = $map[$doc.Path].Id
            if ($parts[0] -ceq 'summaries') {
                if ($links.summaryId -and $links.summaryId -cne $id) { throw 'Dos resúmenes para el mismo partido.' }
                $links.summaryId = $id
            } elseif ($parts[2] -ceq 'callups') {
                $player = TextField $doc 'playerId'
                if (!$player) { $player = $parts[3] }
                if (!$map.ContainsKey("players/$player")) { throw 'Falta el jugador de una convocatoria.' }
                $newPlayer = $map["players/$player"].Id
                if ($links.callupIds.ContainsKey($newPlayer)) { throw 'Jugador convocado dos veces para el mismo partido.' }
                $links.callupIds[$newPlayer] = @{ stringValue = $id }
                $uid = TextField $doc 'userUid'
                if ($uid) { $links.callupUserIds[$uid] = @{ stringValue = $newPlayer } }
            } else {
                $team = TextField $doc 'teamId'
                if (!$team) { $team = $parts[3] }
                if ($team -cne 'starting' -and $map.ContainsKey("teams/$team")) { $team = $map["teams/$team"].Id }
                if ($links.lineupIds.ContainsKey($team)) { throw 'Dos alineaciones para el mismo partido y equipo.' }
                $links.lineupIds[$team] = @{ stringValue = $id }
            }
        }
        if ($doc.Path -cmatch '^attendance/[^/]+$') {
            $training = TextField $doc 'trainingId'
            if (!$training) { $training = $parts[1] }
            if (!$map.ContainsKey("trainings/$training")) { throw 'Falta el entrenamiento de una asistencia.' }
            $newTraining = $map["trainings/$training"].Id
            $rows = Get-Property (Get-Property (Get-Property $doc.Fields 'items') 'arrayValue') 'values'
            foreach ($row in @($rows)) {
                $data = Get-Property (Get-Property $row 'mapValue') 'fields'
                $player = Get-Property (Get-Property $data 'playerId') 'stringValue'
                if (!$player) { continue }
                if (!$map.ContainsKey("players/$player")) { throw 'Falta un jugador en el historial de asistencia.' }
                $history = @{
                    trainingId = @{ stringValue = $newTraining }
                    attendanceId = @{ stringValue = $map[$doc.Path].Id }
                }
                foreach ($field in 'teamName', 'date') {
                    $value = Get-Property $doc.Fields $field
                    if ($null -ne $value) { $history[$field] = $value }
                }
                foreach ($field in 'status', 'justificationReason') {
                    $value = Get-Property $data $field
                    if ($null -ne $value) { $history[$field] = $value }
                }
                $team = TextField $doc 'teamId'
                if ($team -and $map.ContainsKey("teams/$team")) {
                    $history.teamId = @{ stringValue = $map["teams/$team"].Id }
                }
                $timestamp = Get-Property $doc.Fields 'createdAt'
                if ($null -ne $timestamp) { $history.recordedAt = $timestamp }
                $derived["$($map["players/$player"].To)/attendanceHistory/$newTraining"] = $history
            }
        }
        if ($doc.Path -cmatch '^(reports|tryoutReports)/[^/]+$') {
            $collection = if ($parts[0] -ceq 'reports') { 'posts' } else { 'tryouts' }
            $relatedField = if ($collection -ceq 'posts') { 'postId' } else { 'tryoutId' }
            $post = TextField $doc $relatedField
            $uid = TextField $doc 'reporterUid'
            if (!$post -or !$uid -or !$map.ContainsKey("$collection/$post")) { throw 'Falta la publicación o el reportante de un reporte.' }
            $key = "$($map["$collection/$post"].To)/reportOwners/$uid"
            if ($derived.ContainsKey($key)) { throw 'Dos reportes para el mismo registro y cuenta.' }
            $derived[$key] = @{ reportId = @{ stringValue = $map[$doc.Path].Id } }
        }
    }
}
$privateKeys = 'dni','birthDate','phone','email','heightCm','weightKg','emergencyName','emergencyRelation','emergencyPhone','parentRelation'
$count = 0
foreach ($doc in $backup.Documents) {
    if ($doc.Path -clike 'counters/*' -and !$restore) { continue }
    $entry = $map[$doc.Path]
    $path = if ($restore) { $doc.Path } else { $entry.To }
    $fields = if ($doc.Fields) { $doc.Fields } else { [pscustomobject]@{} }
    $context = @{ Document = $doc; From = $doc.Path; OriginalMatch = (TextField $doc 'matchId') }
    if (!$restore) {
        foreach ($property in @($fields.PSObject.Properties)) {
            $property.Value = Rewrite-Value $property.Value $property.Name $context
        }
        if ($linked.ContainsKey($doc.Path)) {
            $links = $linked[$doc.Path]
            foreach ($key in 'callupIds', 'callupUserIds', 'lineupIds') {
                if ($links[$key].Count) {
                    $fields | Add-Member -NotePropertyName $key -NotePropertyValue @{ mapValue = @{ fields = $links[$key] } } -Force
                }
            }
            if ($links.summaryId) {
                $fields | Add-Member -NotePropertyName summaryId -NotePropertyValue @{ stringValue = $links.summaryId } -Force
            }
        }
        if ($doc.Path -cmatch '^players/[^/]+$') {
            $contact = @{}
            foreach ($key in $privateKeys) {
                $property = $fields.PSObject.Properties[$key]
                if ($property) {
                    $contact[$key] = $property.Value
                    $fields.PSObject.Properties.Remove($key)
                }
            }
            if ($contact.Count -gt 0) {
                $derived["$path/private/contact"] = $contact
                $fields | Add-Member -NotePropertyName privateProfile -NotePropertyValue @{ booleanValue = $true } -Force
            }
        }
        if ($entry.Id) {
            if ($doc.Path -clike 'Admins/*' -and !(TextField $doc 'authUid')) {
                $uid = TextField $doc 'id'
                if (!$uid) { throw 'Falta verificar la relación Auth de un administrador.' }
                $fields | Add-Member -NotePropertyName authUid -NotePropertyValue @{ stringValue = $uid } -Force
            }
            if ($doc.Path -clike 'users/*') {
                $fields | Add-Member -NotePropertyName authUid -NotePropertyValue @{ stringValue = $doc.Path.Split('/')[-1] } -Force
            }
            $fields | Add-Member -NotePropertyName id -NotePropertyValue @{ stringValue = $entry.Id } -Force
        }
        if ($doc.Path -clike 'auditLogs/*') {
            $relatedPath = TextField $doc 'documentPath'
            if ($relatedPath -and $map.ContainsKey($relatedPath)) {
                $newPath = $map[$relatedPath].To
                $fields | Add-Member -NotePropertyName documentPath -NotePropertyValue @{ stringValue = $newPath } -Force
                $fields | Add-Member -NotePropertyName documentId -NotePropertyValue @{ stringValue = $newPath.Split('/')[-1] } -Force
            }
        }
        if ($doc.Path -clike 'trainings/*') {
            $attendance = 'attendance/' + $doc.Path.Split('/')[-1]
            if ($map.ContainsKey($attendance)) {
                $fields | Add-Member -NotePropertyName attendanceId -NotePropertyValue @{ stringValue = $map[$attendance].To.Split('/')[-1] } -Force
            }
        }
    } else {
        # Native references must point at the restored project, even when paths are unchanged.

        foreach ($property in @($fields.PSObject.Properties)) {
            $property.Value = Rewrite-Value $property.Value $property.Name $context
        }
    }
    $suffix = '/' + (Encode-Path $path)
    $expected = @{ fields = $fields }
    # Normalize the generated metadata to the same JSON object types returned by the REST API.
    $canonicalFields = (ConvertTo-Json -InputObject $fields -Depth 100 | ConvertFrom-Json)
    $existing = Send-Firestore $target Get $suffix $null -AllowMissing
    if (!$existing -and $Mode -ne 'Verify') {
        $null = Send-Firestore $target Patch "${suffix}?currentDocument.exists=false" $expected
        $existing = Send-Firestore $target Get $suffix $null
    }
    $actual = Get-Property $existing 'fields'
    if (!$actual) { $actual = [pscustomobject]@{} }
    if (!$existing -or (Canonical-Json $actual) -cne (Canonical-Json $canonicalFields)) {
        throw "El destino difiere del respaldo en la colección $($path.Split('/')[0]). Revisa el plan antes de reanudar."
    }
    $count++
}
if (!$restore) {
    $originalPaths = [System.Collections.Generic.HashSet[string]]::new([StringComparer]::Ordinal)
    foreach ($entry in $plan.Mapping) { $null = $originalPaths.Add($entry.To) }
    foreach ($path in $derived.Keys) {
        if ($originalPaths.Contains($path)) { continue }
        $suffix = '/' + (Encode-Path $path)
        $fields = ConvertTo-Json -InputObject $derived[$path] -Depth 100 | ConvertFrom-Json
        $existing = Send-Firestore $target Get $suffix $null -AllowMissing
        if (!$existing -and $Mode -ne 'Verify') {
            $null = Send-Firestore $target Patch "${suffix}?currentDocument.exists=false" @{ fields = $fields }
            $existing = Send-Firestore $target Get $suffix $null
        }
        if (!$existing -or (Canonical-Json (Get-Property $existing 'fields')) -cne (Canonical-Json $fields)) {
            throw 'El índice técnico o perfil privado difiere del plan.'
        }
        $count++
    }
    foreach ($counter in $prefixes.Keys) {
        $floor = [long](Get-Property $plan.Counters $counter)
        $suffix = '/counters/' + (Encode-Path $counter)
        $current = Send-Firestore $target Get $suffix $null -AllowMissing
        if (!$current -and $Mode -eq 'Apply') {
            $null = Send-Firestore $target Patch "${suffix}?currentDocument.exists=false" @{ fields = @{ lastId = @{ integerValue = [string]$floor } } }
        } elseif (!$current -or [long]$current.fields.lastId.integerValue -lt $floor) {
            throw "El contador $counter está por debajo del plan."
        }
    }
}
$actualDocs = @(Get-Inventory $target)
if (!$restore) {
    $actualDocs = @($actualDocs | Where-Object Path -CNotLike 'counters/*')
}
if ($actualDocs.Count -ne $count) {
    throw "El destino contiene $($actualDocs.Count) documentos operativos y el respaldo esperado contiene $count."
}
Write-Output "Documentos verificados: $count"
