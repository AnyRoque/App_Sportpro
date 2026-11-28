Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$directory = Join-Path ([IO.Path]::GetTempPath()) ('sportpro-plan-' + [Guid]::NewGuid())
New-Item -ItemType Directory -Path $directory | Out-Null
function Test-Equal($Expected, $Actual) {
    if ($Expected -cne $Actual) { throw "Esperaba $Expected y recibí $Actual." }
}
function New-Row($Path, $Fields = @{}) {
    [pscustomobject]@{ Path = $Path; Fields = $Fields; UpdateTime = '2025-01-01T00:00:00Z' }
}
function String-Field($Value) { @{ stringValue = $Value } }

$docs = @(
    (New-Row 'counters/teams' @{ lastId = @{ integerValue = '9' } }),
    (New-Row 'teams/SP-EQU-000010'),
    (New-Row 'teams/team-old'),
    (New-Row 'players/player-old' @{ teamId = (String-Field 'team-old') }),
    (New-Row 'trainings/training-old' @{ teamId = (String-Field 'team-old') }),
    (New-Row 'attendance/training-old' @{ trainingId = (String-Field 'training-old') }),
    (New-Row 'players/player-old/attendanceHistory/training-old'),
    (New-Row 'matches/match-old'),
    (New-Row 'matches/match-old/callups/player-old'),
    (New-Row 'matches/match-old/lineup/team-old'),
    (New-Row 'summaries/match-old' @{ matchId = (String-Field 'match-old') }),
    (New-Row 'posts/post-old'),
    (New-Row 'reports/post-old_coachuid' @{ postId = (String-Field 'post-old'); reporterUid = (String-Field 'coachuid') }),
    (New-Row 'matches/match-old/events/event-old' @{ type = (String-Field 'GOL') }),
    (New-Row 'matches/match-old/events/event-old/history/history-old'),
    (New-Row 'Admins/legacy@example.test' @{ id = (String-Field 'adminuid') }),
    (New-Row 'users/adminuid' @{ role = (String-Field 'ADM') }),
    (New-Row 'users/coachuid' @{ role = (String-Field 'DT') })
)
$backup = @{ Project = 'demo-sportpro'; Documents = $docs }
$file = Join-Path $directory 'firestore-backup.json'
ConvertTo-Json -InputObject $backup -Depth 20 | Set-Content -LiteralPath $file -Encoding UTF8
(Get-FileHash -LiteralPath $file).Hash | Set-Content -LiteralPath "$file.sha256"
try {
    & (Join-Path $PSScriptRoot 'Invoke-SportProFirestoreMigration.ps1') -Mode Plan -Project demo-sportpro -Workspace $directory
} catch {
    Write-Host $_.ScriptStackTrace
    throw
}
$result = Get-Content -LiteralPath (Join-Path $directory 'firestore-plan.json') -Raw | ConvertFrom-Json
$rows = @{}
foreach ($row in $result.Mapping) { $rows[$row.From] = $row }

Test-Equal 11 $result.Counters.teams
Test-Equal 'teams/SP-EQU-000011' $rows['teams/team-old'].To
Test-Equal 'attendance/SP-ASI-000001' $rows['attendance/training-old'].To
Test-Equal 'players/SP-JUG-000001/attendanceHistory/SP-ENT-000001' $rows['players/player-old/attendanceHistory/training-old'].To
Test-Equal 'matches/SP-PAR-000001/callups/SP-CON-000001' $rows['matches/match-old/callups/player-old'].To
Test-Equal 'matches/SP-PAR-000001/events/SP-GOL-000001/history/SP-HIS-000001' $rows['matches/match-old/events/event-old/history/history-old'].To
Test-Equal 'SP-ADM-000001' $rows['users/adminuid'].Id
Test-Equal 'users/coachuid' $rows['users/coachuid'].To
Test-Equal 'SP-DT-000001' $rows['users/coachuid'].Id
Test-Equal 'matches/SP-PAR-000001/lineup/SP-ALI-000001' $rows['matches/match-old/lineup/team-old'].To
Test-Equal 'summaries/SP-RES-000001' $rows['summaries/match-old'].To
Test-Equal 'reports/SP-REP-000001' $rows['reports/post-old_coachuid'].To
Write-Output '12 aserciones sobre el plan de migración completadas.'
