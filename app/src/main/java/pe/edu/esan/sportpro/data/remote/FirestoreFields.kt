package pe.edu.esan.sportpro.data.remote

import pe.edu.esan.sportpro.data.model.AnnouncementModel
import pe.edu.esan.sportpro.data.model.CommentModel
import pe.edu.esan.sportpro.data.model.EventCancellationRequestModel
import pe.edu.esan.sportpro.data.model.EventCatalogModel
import pe.edu.esan.sportpro.data.model.MatchEventModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.PostModel
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TrainingModel
import pe.edu.esan.sportpro.data.model.TryoutCandidateModel
import pe.edu.esan.sportpro.data.model.TryoutModel
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.model.ReportModel
import pe.edu.esan.sportpro.data.model.MatchSummaryModel
import pe.edu.esan.sportpro.data.model.CallUpItemModel
import pe.edu.esan.sportpro.data.model.LineupModel

private fun sportTimestamp(day: String, time: String): com.google.firebase.Timestamp? {
    val date = pe.edu.esan.sportpro.util.Validators.parseDate(day) ?: return null
    val clock = try {
        java.time.LocalTime.parse(time)
    } catch (_: java.time.format.DateTimeParseException) {
        return null
    }
    val instant = date.atTime(clock).atZone(java.time.ZoneId.of("America/Lima")).toInstant()
    return com.google.firebase.Timestamp(java.util.Date.from(instant))
}

internal fun TeamModel.firestoreFields(): Map<String, Any?> = mapOf(
    "name" to name,
    "nameKey" to nameKey,
    "category" to category,
    "field" to field,
    "district" to district,
    "trainingDays" to trainingDays,
    "trainingTime" to trainingTime,
    "dtUid" to dtUid,
    "dtName" to dtName,
    "status" to status,
    "rejectReason" to rejectReason,
    "reviewedBy" to reviewedBy,
    "reviewedAt" to reviewedAt,
    "createdAt" to createdAt
)

internal fun PlayerModel.firestoreFields(): Map<String, Any?> = mapOf(
    "teamId" to teamId,
    "teamName" to teamName,
    "category" to category,
    "name" to name,
    "lastName" to lastName,
    "birthDate" to birthDate,
    "dni" to dni,
    "sex" to sex,
    "photoUrl" to photoUrl,
    "position" to position,
    "shirtNumber" to shirtNumber,
    "heightCm" to heightCm,
    "weightKg" to weightKg,
    "foot" to foot,
    "phone" to phone,
    "email" to email,
    "emergencyName" to emergencyName,
    "emergencyRelation" to emergencyRelation,
    "emergencyPhone" to emergencyPhone,
    "parentUid" to parentUid,
    "parentRelation" to parentRelation,
    "userUid" to userUid,
    "dtUid" to dtUid,
    "linkStatus" to linkStatus,
    "active" to active,
    "createdBy" to createdBy,
    "createdAt" to createdAt
)

internal fun TrainingModel.firestoreFields(): Map<String, Any?> = mapOf(
    "dtUid" to dtUid,
    "dtName" to dtName,
    "teamId" to teamId,
    "teamName" to teamName,
    "category" to category,
    "date" to date,
    "startTime" to startTime,
    "trainingDate" to sportTimestamp(date, startTime),
    "durationMinutes" to durationMinutes,
    "location" to location,
    "objectiveType" to objectiveType,
    "objectiveDescription" to objectiveDescription,
    "exercises" to exercises.map { exercise ->
        mapOf(
            "id" to exercise.id,
            "name" to exercise.name,
            "description" to exercise.description,
            "type" to exercise.type,
            "durationMinutes" to exercise.durationMinutes
        )
    },
    "status" to status,
    "cancelReason" to cancelReason,
    "duplicatedFromId" to duplicatedFromId,
    "createdAt" to createdAt
)

internal fun MatchModel.firestoreFields(): Map<String, Any?> = mapOf(
    "dtUid" to dtUid,
    "dtName" to dtName,
    "teamId" to teamId,
    "teamName" to teamName,
    "category" to category,
    "rival" to rival,
    "rivalTeamId" to rivalTeamId,
    "matchType" to matchType,
    "condition" to condition,
    "modality" to modality,
    "periodMinutes" to periodMinutes,
    "date" to date,
    "time" to time,
    "matchDate" to sportTimestamp(date, time),
    "location" to location,
    "notes" to notes,
    "operatorUid" to operatorUid,
    "operatorName" to operatorName,
    "memberUids" to memberUids,
    "status" to status,
    "cancelReason" to cancelReason,
    "hasCallUp" to hasCallUp,
    "callUpDeadlineDate" to callUpDeadlineDate,
    "callUpDeadlineTime" to callUpDeadlineTime,
    "formation" to formation,
    "hasLineup" to hasLineup,
    "score" to score,
    "timerStartTime" to timerStartTime,
    "timerAccumulatedSeconds" to timerAccumulatedSeconds,
    "timerIsRunning" to timerIsRunning,
    "currentPeriod" to currentPeriod,
    "timerPauseTime" to timerPauseTime,
    "createdAt" to createdAt
)

internal fun AnnouncementModel.firestoreFields(): Map<String, Any?> = mapOf(
    "title" to title,
    "description" to description,
    "type" to type,
    "teamId" to teamId,
    "teamName" to teamName,
    "targetRoles" to targetRoles,
    "validUntil" to validUntil,
    "authorUid" to authorUid,
    "authorName" to authorName,
    "authorRole" to authorRole,
    "edited" to edited,
    "readBy" to readBy,
    "createdAt" to createdAt
)

internal fun PostModel.firestoreFields(): Map<String, Any?> = mapOf(
    "text" to text,
    "imageUrl" to imageUrl,
    "visibility" to visibility,
    "teamId" to teamId,
    "teamName" to teamName,
    "authorUid" to authorUid,
    "authorName" to authorName,
    "authorRole" to authorRole,
    "reactions" to reactions,
    "commentCount" to commentCount,
    "reportCount" to reportCount,
    "status" to status,
    "createdAt" to createdAt
)

internal fun CommentModel.firestoreFields(): Map<String, Any?> = mapOf(
    "postId" to postId,
    "text" to text,
    "authorUid" to authorUid,
    "authorName" to authorName,
    "authorRole" to authorRole,
    "createdAt" to createdAt
)

internal fun MatchEventModel.firestoreFields(): Map<String, Any?> = mapOf(
    "matchId" to matchId,
    "type" to type,
    "minute" to minute,
    "minutoCorregido" to minutoCorregido,
    "team" to team,
    "playerId" to playerId,
    "playerName" to playerName,
    "jugadorNoIdentificado" to jugadorNoIdentificado,
    "jugadorSaleId" to jugadorSaleId,
    "jugadorSaleName" to jugadorSaleName,
    "jugadorEntraId" to jugadorEntraId,
    "jugadorEntraName" to jugadorEntraName,
    "observation" to observation,
    "registeredBy" to registeredBy,
    "registeredByName" to registeredByName,
    "status" to status,
    "pendingSync" to pendingSync,
    "registeredAt" to registeredAt
)

internal fun EventCancellationRequestModel.firestoreFields(): Map<String, Any?> = mapOf(
    "matchId" to matchId,
    "matchTitle" to matchTitle,
    "eventId" to eventId,
    "eventSummary" to eventSummary,
    "requestingDtUid" to requestingDtUid,
    "requestingDtName" to requestingDtName,
    "requestingTeamName" to requestingTeamName,
    "justification" to justification,
    "status" to status,
    "adminResponseComment" to adminResponseComment,
    "createdAt" to createdAt,
    "resolvedAt" to resolvedAt
)

internal fun TryoutModel.firestoreFields(): Map<String, Any?> = mapOf(
    "teamId" to teamId,
    "teamName" to teamName,
    "verifiedTeam" to verifiedTeam,
    "title" to title,
    "description" to description,
    "category" to category,
    "positionsSought" to positionsSought,
    "tryoutDate" to tryoutDate,
    "tryoutTime" to tryoutTime,
    "address" to address,
    "district" to district,
    "requirements" to requirements,
    "slots" to slots,
    "registrationDeadline" to registrationDeadline,
    "free" to isFree,
    "dtUid" to dtUid,
    "dtName" to dtName,
    "interestedCount" to interestedCount,
    "createdAt" to createdAt
)

internal fun TryoutCandidateModel.firestoreFields(): Map<String, Any?> = mapOf(
    "tryoutId" to tryoutId,
    "userUid" to userUid,
    "name" to name,
    "age" to age,
    "position" to position,
    "role" to role,
    "createdAt" to createdAt
)

internal fun EventCatalogModel.firestoreFields(): Map<String, Any?> = mapOf(
    "name" to name,
    "icon" to icon,
    "colorHex" to colorHex,
    "requiresTeam" to requiresTeam,
    "requiresPlayer" to requiresPlayer,
    "requiresSecondPlayer" to requiresSecondPlayer,
    "affectsScore" to affectsScore,
    "accumulatesStat" to accumulatesStat,
    "active" to active,
    "systemDefault" to isSystemDefault,
    "createdAt" to createdAt
)

internal fun UserModel.firestoreFields(): Map<String, Any?> = mapOf(
    "uid" to uid,
    "authUid" to uid,
    "name" to name,
    "lastName" to lastName,
    "email" to email,
    "phone" to phone,
    "role" to role,
    "birthDate" to birthDate,
    "acceptedTerms" to acceptedTerms,
    "active" to active
)

internal fun ReportModel.firestoreFields(): Map<String, Any?> = mapOf(
    "postId" to postId,
    "postText" to postText,
    "postAuthorUid" to postAuthorUid,
    "postAuthorName" to postAuthorName,
    "reason" to reason,
    "detail" to detail,
    "reporterUid" to reporterUid,
    "status" to status,
    "action" to action,
    "resolvedBy" to resolvedBy,
    "resolvedAt" to resolvedAt
)

internal fun MatchSummaryModel.firestoreFields(): Map<String, Any?> = mapOf(
    "matchId" to matchId,
    "teamId" to teamId,
    "teamName" to teamName,
    "rival" to rival,
    "date" to date,
    "ownScore" to ownScore,
    "rivalScore" to rivalScore,
    "aiFacts" to aiFacts,
    "aiAnalysis" to aiAnalysis,
    "finalFacts" to finalFacts,
    "finalAnalysis" to finalAnalysis,
    "warnings" to warnings,
    "status" to status,
    "generation" to generation,
    "model" to model,
    "promptVersion" to promptVersion,
    "memberUids" to memberUids,
    "createdBy" to createdBy,
    "approvedBy" to approvedBy,
    "approvedByName" to approvedByName,
    "approvedAt" to approvedAt
)

internal fun CallUpItemModel.firestoreFields(): Map<String, Any?> = mapOf(
    "matchId" to matchId,
    "teamId" to teamId,
    "playerId" to playerId,
    "playerName" to playerName,
    "playerPhotoUrl" to playerPhotoUrl,
    "position" to position,
    "minor" to isMinor,
    "isStarter" to isStarter,
    "userUid" to userUid,
    "parentUid" to parentUid,
    "responseStatus" to responseStatus,
    "rejectReason" to rejectReason,
    "rejectComment" to rejectComment,
    "deadlineDate" to deadlineDate,
    "deadlineTime" to deadlineTime
)

internal fun LineupModel.firestoreFields(): Map<String, Any?> = mapOf(
    "matchId" to matchId,
    "teamId" to teamId,
    "teamName" to teamName,
    "formation" to formation,
    "modality" to modality,
    "positions" to positions.map { position ->
        mapOf(
            "slotIndex" to position.slotIndex,
            "positionName" to position.positionName,
            "xRatio" to position.xRatio.toDouble(),
            "yRatio" to position.yRatio.toDouble(),
            "playerId" to position.playerId,
            "playerName" to position.playerName,
            "shirtNumber" to position.shirtNumber
        )
    },
    "substitutePlayerIds" to substitutePlayerIds,
    "savedBy" to savedBy,
    "savedAt" to savedAt
)
