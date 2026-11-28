[CmdletBinding()]
param(
    [ValidatePattern('^(127\.0\.0\.1|localhost):[0-9]+$')]
    [string]$FirestoreEmulator = '127.0.0.1:8080'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$run = [Guid]::NewGuid().ToString('N').Substring(0, 8)
$source = "demo-sp-source-$run"
$target = "demo-sp-target-$run"
$restore = "demo-sp-restore-$run"
$workspace = Join-Path ([IO.Path]::GetTempPath()) "sportpro-migration-$run"
$migration = Join-Path $PSScriptRoot 'Invoke-SportProFirestoreMigration.ps1'
$headers = @{ Authorization = 'Bearer owner' }

function Text-Value([string]$Value) {
    return @{ stringValue = $Value }
}

function Write-DemoDocument([string]$Path, [hashtable]$Fields) {
    $uri = "http://$FirestoreEmulator/v1/projects/$source/databases/(default)/documents/$Path"
    $json = ConvertTo-Json @{ fields = $Fields } -Depth 25 -Compress
    $null = Invoke-RestMethod -Uri $uri -Method Patch -Headers $headers -ContentType 'application/json; charset=utf-8' -Body ([Text.Encoding]::UTF8.GetBytes($json)) -TimeoutSec 15
}

function Read-DemoDocument([string]$Project, [string]$Path) {
    $uri = "http://$FirestoreEmulator/v1/projects/$Project/databases/(default)/documents/$Path"
    $response = Invoke-WebRequest -UseBasicParsing -Uri $uri -Method Get -Headers $headers -TimeoutSec 15
    return [Text.Encoding]::UTF8.GetString($response.RawContentStream.ToArray()) | ConvertFrom-Json
}

function Confirm-Equal($Expected, $Actual) {
    if ($Expected -cne $Actual) { throw "Esperaba '$Expected', recibí '$Actual'." }
}

$teamName = "Equipo sint$([char]0xE9)tico"
Write-DemoDocument 'teams/legacy-team' @{ name = (Text-Value $teamName) }
Write-DemoDocument 'players/legacy-player' @{
    teamId = (Text-Value 'legacy-team')
    phone = (Text-Value '900000000')
    name = (Text-Value 'Jugador ficticio')
}
Write-DemoDocument 'trainings/legacy-training' @{ teamId = (Text-Value 'legacy-team') }
Write-DemoDocument 'attendance/legacy-training' @{
    teamId = (Text-Value 'legacy-team')
    trainingId = (Text-Value 'legacy-training')
    items = @{ arrayValue = @{ values = @(
        @{ mapValue = @{ fields = @{ playerId = (Text-Value 'legacy-player'); status = (Text-Value 'ASISTIO') } } }
    ) } }
}
Write-DemoDocument 'matches/legacy-match' @{ teamId = (Text-Value 'legacy-team') }
Write-DemoDocument 'matches/legacy-match/callups/legacy-player' @{
    matchId = (Text-Value 'legacy-match'); playerId = (Text-Value 'legacy-player')
}
Write-DemoDocument 'matches/legacy-match/lineup/legacy-team' @{
    matchId = (Text-Value 'legacy-match'); teamId = (Text-Value 'legacy-team')
    substitutePlayerIds = @{ arrayValue = @{ values = @((Text-Value 'legacy-player')) } }
}
Write-DemoDocument 'summaries/legacy-match' @{ matchId = (Text-Value 'legacy-match') }
Write-DemoDocument 'posts/legacy-post' @{ text = (Text-Value 'Publicacion de prueba') }
Write-DemoDocument 'reports/legacy-post_reporter' @{
    postId = (Text-Value 'legacy-post'); reporterUid = (Text-Value 'reporter')
}
Write-DemoDocument 'counters/teams' @{ lastId = @{ integerValue = '4' } }

Write-Output "Proyectos de prueba: $source, $target, $restore"
& $migration -Mode DryRun -Project $source -SourceEmulator $FirestoreEmulator -Workspace $workspace
& $migration -Mode Apply -Project $source -DestinationProject $target -DestinationEmulator $FirestoreEmulator -Workspace $workspace
& $migration -Mode Apply -Project $source -DestinationProject $target -DestinationEmulator $FirestoreEmulator -Workspace $workspace
& $migration -Mode Verify -Project $source -DestinationProject $target -DestinationEmulator $FirestoreEmulator -Workspace $workspace

$team = Read-DemoDocument $target 'teams/SP-EQU-000005'
Confirm-Equal $teamName $team.fields.name.stringValue
$player = Read-DemoDocument $target 'players/SP-JUG-000001'
Confirm-Equal 'SP-EQU-000005' $player.fields.teamId.stringValue
if ($player.fields.PSObject.Properties['phone']) { throw 'El teléfono sigue en la ficha deportiva.' }
$private = Read-DemoDocument $target 'players/SP-JUG-000001/private/contact'
Confirm-Equal '900000000' $private.fields.phone.stringValue
$training = Read-DemoDocument $target 'trainings/SP-ENT-000001'
Confirm-Equal 'SP-ASI-000001' $training.fields.attendanceId.stringValue
$history = Read-DemoDocument $target 'players/SP-JUG-000001/attendanceHistory/SP-ENT-000001'
Confirm-Equal 'ASISTIO' $history.fields.status.stringValue
$match = Read-DemoDocument $target 'matches/SP-PAR-000001'
Confirm-Equal 'SP-CON-000001' $match.fields.callupIds.mapValue.fields.'SP-JUG-000001'.stringValue
Confirm-Equal 'SP-ALI-000001' $match.fields.lineupIds.mapValue.fields.'SP-EQU-000005'.stringValue
Confirm-Equal 'SP-RES-000001' $match.fields.summaryId.stringValue
$lineup = Read-DemoDocument $target 'matches/SP-PAR-000001/lineup/SP-ALI-000001'
Confirm-Equal 'SP-JUG-000001' $lineup.fields.substitutePlayerIds.arrayValue.values[0].stringValue
$reportIndex = Read-DemoDocument $target 'posts/SP-PUB-000001/reportOwners/reporter'
Confirm-Equal 'SP-REP-000001' $reportIndex.fields.reportId.stringValue
$sourcePlayer = Read-DemoDocument $source 'players/legacy-player'
Confirm-Equal '900000000' $sourcePlayer.fields.phone.stringValue

& $migration -Mode Restore -Project $source -DestinationProject $restore -DestinationEmulator $FirestoreEmulator -Workspace $workspace
$original = Read-DemoDocument $restore 'players/legacy-player'
Confirm-Equal 'legacy-team' $original.fields.teamId.stringValue
Confirm-Equal '900000000' $original.fields.phone.stringValue
$counter = Read-DemoDocument $restore 'counters/teams'
Confirm-Equal '4' $counter.fields.lastId.integerValue
Write-Output 'Migración, reejecución, verificación y restauración superadas en proyectos demo.'
