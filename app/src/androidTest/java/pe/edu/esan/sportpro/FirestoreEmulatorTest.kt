package pe.edu.esan.sportpro

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import pe.edu.esan.sportpro.data.model.AdminModel
import pe.edu.esan.sportpro.data.model.TeamModel
import pe.edu.esan.sportpro.data.model.TeamStatus
import pe.edu.esan.sportpro.data.model.PostModel
import pe.edu.esan.sportpro.data.model.PlayerModel
import pe.edu.esan.sportpro.data.model.AttendanceRecordModel
import pe.edu.esan.sportpro.data.model.AttendanceStatus
import pe.edu.esan.sportpro.data.model.LineupModel
import pe.edu.esan.sportpro.data.model.LinkStatus
import pe.edu.esan.sportpro.data.model.MatchEventModel
import pe.edu.esan.sportpro.data.model.MatchModel
import pe.edu.esan.sportpro.data.model.CallUpStatus
import pe.edu.esan.sportpro.data.model.MatchStatus
import pe.edu.esan.sportpro.data.model.TrainingStatus
import pe.edu.esan.sportpro.data.model.PlayerAttendanceItem
import pe.edu.esan.sportpro.data.model.TacticalPosition
import pe.edu.esan.sportpro.data.model.TrainingModel
import pe.edu.esan.sportpro.data.model.NotificationModel
import pe.edu.esan.sportpro.data.remote.AdminRepository
import pe.edu.esan.sportpro.data.remote.PlayerRepository
import pe.edu.esan.sportpro.data.remote.AttendanceRepository
import pe.edu.esan.sportpro.data.remote.MatchRepository
import pe.edu.esan.sportpro.data.remote.TrainingRepository
import java.net.HttpURLConnection
import java.net.URL
import pe.edu.esan.sportpro.data.remote.CommunityRepository
import pe.edu.esan.sportpro.data.model.UserModel
import pe.edu.esan.sportpro.data.remote.FirebaseAuthManager
import pe.edu.esan.sportpro.data.remote.TeamRepository
import pe.edu.esan.sportpro.data.remote.SportProEntity
import pe.edu.esan.sportpro.data.remote.SportProIdGenerator

@RunWith(AndroidJUnit4::class)
class FirestoreEmulatorTest {
    @Before
    fun requireDemoProject() = runBlocking {
        assumeTrue(BuildConfig.USE_FIREBASE_EMULATOR)
        check(FirebaseApp.getInstance().options.projectId == "demo-sportpro")
        seedMissingCounters()
    }

    @After
    fun signOut() {
        if (BuildConfig.USE_FIREBASE_EMULATOR) FirebaseAuthManager.logout()
    }

    @Test
    fun concurrentTeamsHaveDistinctIdsAndAppearInQueries() = runBlocking {
        withTimeout(30_000) {
            val unique = System.nanoTime().toString()
            val coach = registerCoach(unique)
            assertTrue(coach.id.matches(Regex("SP-DT-[0-9]{6}")))
            assertEquals(coach.uid, coach.authUid)

            val query = FirebaseFirestore.getInstance().collection("teams").whereEqualTo("dtUid", coach.uid)
            val observed = CompletableDeferred<List<String>>()
            val registration = query.addSnapshotListener { snapshot, error ->
                if (error != null) observed.completeExceptionally(error)
                val ids = snapshot?.documents.orEmpty().map { it.id }
                if (ids.size == 2) observed.complete(ids)
            }
            try {
                val teams = listOf("A", "B").map { letter ->
                    async {
                        TeamRepository.createTeam(TeamModel(
                            name = "Equipo $letter $unique",
                            category = "Sub-18",
                            dtUid = coach.uid,
                            dtName = coach.fullName,
                            emblemPath = if (letter == "A") "${coach.uid}/equipo_first.jpg" else ""
                        )).getOrThrow()
                    }
                }
                teams.awaitAll()
                assertEquals(2, observed.await().distinct().size)
            } finally {
                registration.remove()
            }

            val saved = query.get(Source.SERVER).await().documents
            assertEquals(2, saved.size)
            assertNotEquals(saved[0].id, saved[1].id)
            val emblems = saved.mapNotNull { it.toObject(TeamModel::class.java)?.emblemPath }
            assertEquals(setOf("", "${coach.uid}/equipo_first.jpg"), emblems.toSet())
            assertTrue(saved.all { doc ->
                doc.id.matches(Regex("SP-EQU-[0-9]{6}")) &&
                    doc.getString("id") == doc.id &&
                    doc.toObject(TeamModel::class.java)?.id == doc.id &&
                    doc.getString("coachId") == coach.id
            })
        }
    }

    @Test
    fun reportUsesSequentialIdAndKeepsOneReportPerAccount() = runBlocking {
        withTimeout(30_000) {
            val unique = System.nanoTime().toString()
            val coach = registerCoach(unique)
            val firestore = FirebaseFirestore.getInstance()
            val content = "Publicación de prueba $unique"
            CommunityRepository.createPost(PostModel(
                text = content, authorUid = coach.uid, authorName = coach.fullName
            )).getOrThrow()
            val post = firestore.collection("posts").whereEqualTo("text", content).get().await()
                .toObjects(PostModel::class.java).single()

            CommunityRepository.reportPost(post, coach.uid, "Otro", "Prueba en emulador").getOrThrow()
            assertTrue(CommunityRepository.reportPost(post, coach.uid, "Otro", "Reintento").isFailure)

            val saved = firestore.collection("posts").document(post.id)
            val index = saved.collection("reportOwners").document(coach.uid).get().await()
            val reportId = index.getString("reportId").orEmpty()
            assertTrue(reportId.matches(Regex("SP-REP-[0-9]{6}")))
            assertEquals(1L, saved.get().await().getLong("reportCount"))
            val report = firestore.collection("reports").document(reportId).get().await()
            assertEquals(post.id, report.getString("postId"))
            assertEquals(reportId, report.getString("id"))
        }
    }

    @Test
    fun privatePlayerContactIsReadableByOwnerAndDeniedToAnotherPlayer() = runBlocking {
        withTimeout(30_000) {
            val unique = System.nanoTime().toString()
            val owner = registerPlayer("owner-$unique")
            val playerId = PlayerRepository.createPlayer(PlayerModel(
                name = owner.name, lastName = owner.lastName, userUid = owner.uid, phone = "900000000"
            )).getOrThrow()
            val player = FirebaseFirestore.getInstance().collection("players").document(playerId)
            val contact = player.collection("private").document("contact")
            val roster = player.get(Source.SERVER).await()
            assertTrue(!roster.contains("phone"))
            assertEquals("900000000", contact.get(Source.SERVER).await().getString("phone"))
            val profile = PlayerRepository.getPlayer(playerId).getOrThrow()
            PlayerRepository.updatePlayer(profile.copy(phone = "900000001")).getOrThrow()
            assertEquals("900000001", contact.get(Source.SERVER).await().getString("phone"))

            FirebaseAuthManager.logout()
            registerPlayer("other-$unique")
            val failure = runCatching { contact.get(Source.SERVER).await() }.exceptionOrNull()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,
                (failure as? FirebaseFirestoreException)?.code)
        }
    }

    @Test
    fun unavailableConnectionDoesNotCommitBeforeRetry() = runBlocking {
        withTimeout(60_000) {
            val unique = System.nanoTime().toString()
            val coach = registerCoach(unique)
            val firestore = FirebaseFirestore.getInstance()
            val text = "Publicación sin conexión $unique"
            val isolatedApp = FirebaseApp.initializeApp(
                InstrumentationRegistry.getInstrumentation().targetContext,
                FirebaseApp.getInstance().options,
                "unavailable-$unique"
            )
            val unavailable = FirebaseFirestore.getInstance(isolatedApp)
            unavailable.useEmulator("127.0.0.1", 1)
            try {
                val result = runCatching {
                    SportProIdGenerator(unavailable).create(
                        unavailable.collection("posts"), SportProEntity.POST,
                        mapOf("text" to text, "authorUid" to coach.uid)
                    )
                }
                assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("conexión"))
            } finally {
                unavailable.terminate().await()
                isolatedApp.delete()
            }
            val query = firestore.collection("posts").whereEqualTo("text", text)
            assertTrue(query.get(Source.SERVER).await().isEmpty)
            CommunityRepository.createPost(PostModel(text = text, authorUid = coach.uid)).getOrThrow()
            assertEquals(1, query.get(Source.SERVER).await().size())
        }
    }

    @Test
    fun matchLineupAndTrainingKeepTheirLinkedRecords() = runBlocking {
        withTimeout(60_000) {
            val unique = System.nanoTime().toString()
            val coach = registerCoach(unique)
            val firestore = FirebaseFirestore.getInstance()
            val team = TeamModel(name = "Academia $unique", category = "Sub-18", dtUid = coach.uid)
            TeamRepository.createTeam(team).getOrThrow()
            val teamId = firestore.collection("teams").whereEqualTo("dtUid", coach.uid)
                .get(Source.SERVER).await().documents.single().id

            val administrator = FirebaseAuth.getInstance()
                .createUserWithEmailAndPassword("admin-$unique@example.test", "sportpro-demo")
                .await().user ?: error("Falta la cuenta ADM del emulador")
            seedDemoAdministrator(administrator.uid)
            TeamRepository.approveTeam(teamId, administrator.uid).getOrThrow()
            FirebaseAuthManager.loginUser(coach.email, "sportpro-demo").getOrThrow()
            val emblem = "${coach.uid}/equipo_new.jpg"
            TeamRepository.updateEmblem(teamId, emblem)
            val teamWithEmblem = TeamRepository.getTeam(teamId).getOrThrow()
            assertEquals(emblem, teamWithEmblem.emblemPath)
            assertEquals(TeamStatus.APPROVED, teamWithEmblem.status)

            val linkedAccount = registerPlayer("member-$unique")
            FirebaseAuthManager.loginUser(coach.email, "sportpro-demo").getOrThrow()
            val player = PlayerModel(
                name = "Titular", lastName = unique, dtUid = coach.uid,
                teamId = teamId, teamName = team.name, userUid = linkedAccount.uid,
                linkStatus = LinkStatus.ACCEPTED
            )
            val playerId = PlayerRepository.createPlayer(player).getOrThrow()
            val match = MatchModel(
                teamId = teamId, teamName = team.name, rival = "Equipo externo",
                dtUid = coach.uid, date = "27/11/2030"
            )
            val matchId = MatchRepository.createMatch(match).getOrThrow()
            val savedMatch = match.copy(id = matchId)
            MatchRepository.createCallUp(savedMatch, listOf(player.copy(id = playerId)),
                "26/11/2030", "19:00").getOrThrow()
            MatchRepository.saveLineup(matchId, LineupModel(
                teamId = teamId, teamName = team.name, savedBy = coach.uid,
                positions = listOf(TacticalPosition(playerId = playerId, playerName = player.fullName))
            )).getOrThrow()
            MatchRepository.addMatchEvent(MatchEventModel(
                matchId = matchId, playerId = playerId, playerName = player.fullName, registeredBy = coach.uid
            )).getOrThrow()

            val matchRef = firestore.collection("matches").document(matchId)
            assertTrue(matchRef.collection("callups").get(Source.SERVER).await().documents
                .single().id.matches(Regex("SP-CON-[0-9]{6}")))
            val lineup = MatchRepository.getLineup(matchId, teamId).getOrThrow()
                ?: error("Falta la alineación")
            assertTrue(lineup.id.matches(Regex("SP-ALI-[0-9]{6}")))
            assertEquals(playerId, lineup.positions.single().playerId)
            assertTrue(matchRef.collection("events").get(Source.SERVER).await().documents
                .single().id.matches(Regex("SP-GOL-[0-9]{6}")))

            val trainingId = TrainingRepository.createTraining(TrainingModel(
                teamId = teamId, teamName = team.name, dtUid = coach.uid, date = "27/11/2030"
            )).getOrThrow()
            val entry = PlayerAttendanceItem(playerId = playerId, status = AttendanceStatus.ATTENDED)
            val record = AttendanceRecordModel(
                trainingId = trainingId, teamId = teamId, dtUid = coach.uid, items = listOf(entry)
            )
            AttendanceRepository.saveAttendanceRecord(record).getOrThrow()
            val original = AttendanceRepository.getAttendanceRecord(trainingId).getOrThrow()
            AttendanceRepository.saveAttendanceRecord(record).getOrThrow()
            val revised = AttendanceRepository.getAttendanceRecord(trainingId).getOrThrow()
            assertTrue(original.id.matches(Regex("SP-ASI-[0-9]{6}")))
            assertEquals(original.id, revised.id)
            val history = AttendanceRepository.getPlayerAttendanceForTraining(trainingId, player.copy(id = playerId))
                .getOrThrow()
            assertEquals(AttendanceStatus.ATTENDED, history?.status)

            FirebaseAuthManager.loginUser(linkedAccount.email, "sportpro-demo").getOrThrow()
            assertEquals(matchId, MatchRepository.getMatch(matchId).getOrThrow().id)
            MatchRepository.respondCallUp(matchId, playerId, CallUpStatus.CONFIRMED).getOrThrow()
            val response = matchRef.collection("callups").get(Source.SERVER).await().documents.single()
            assertEquals(CallUpStatus.CONFIRMED, response.getString("responseStatus"))
            assertEquals(trainingId, TrainingRepository.getTraining(trainingId).getOrThrow().id)
            val playerTrainings = TrainingRepository.observeTrainingsForUser(linkedAccount)
                .first { it.isNotEmpty() }
            assertEquals(listOf(trainingId), playerTrainings.map { it.id })
            val notices = firestore.collection("notifications").whereEqualTo("recipientUid", linkedAccount.uid)
                .get(Source.SERVER).await().toObjects(NotificationModel::class.java)
            assertTrue(notices.any { it.relatedId == matchId && it.id.startsWith("SP-NOT-") })
            assertTrue(notices.any { it.relatedId == trainingId && it.id.startsWith("SP-NOT-") })
            assertEquals(AttendanceStatus.ATTENDED,
                AttendanceRepository.getPlayerAttendanceForTraining(trainingId, player.copy(id = playerId))
                    .getOrThrow()?.status)

            registerPlayer("unrelated-$unique")
            val trainingRef = firestore.collection("trainings").document(trainingId)
            for (reference in listOf(matchRef, trainingRef)) {
                val deniedRead = runCatching { reference.get(Source.SERVER).await() }.exceptionOrNull()
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,
                    (deniedRead as? FirebaseFirestoreException)?.code)
            }

            val otherCoach = registerCoach("other-$unique")
            val emblemFailure = runCatching {
                TeamRepository.updateEmblem(teamId, "${otherCoach.uid}/equipo_other.jpg")
            }.exceptionOrNull()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,
                (emblemFailure as? FirebaseFirestoreException)?.code)
            val training = firestore.collection("trainings").document(trainingId)
            val projection = firestore.collection("players").document(playerId)
                .collection("attendanceHistory").document(trainingId)
            for ((reference, field) in listOf(training to "location", projection to "status")) {
                val error = runCatching { reference.update(field, "No autorizado").await() }.exceptionOrNull()
                assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED,
                    (error as? FirebaseFirestoreException)?.code)
            }

            FirebaseAuthManager.loginUser(coach.email, "sportpro-demo").getOrThrow()
            val coachNotices = firestore.collection("notifications").whereEqualTo("recipientUid", coach.uid)
                .get(Source.SERVER).await()
            assertTrue(coachNotices.documents.any { it.getString("callUpId") == response.id })

            val originalMatch = MatchRepository.getMatch(matchId).getOrThrow()
            MatchRepository.updateMatch(originalMatch.copy(notes = "Horario confirmado")).getOrThrow()
            val updatedMatch = MatchRepository.getMatch(matchId).getOrThrow()
            assertEquals("Horario confirmado", updatedMatch.notes)
            assertTrue(updatedMatch.hasCallUp && updatedMatch.hasLineup)
            assertEquals(originalMatch.score, updatedMatch.score)
            MatchRepository.cancelMatch(matchId, "Prueba", coach.uid).getOrThrow()
            assertEquals(MatchStatus.CANCELED, MatchRepository.getMatch(matchId).getOrThrow().status)

            val originalTraining = TrainingRepository.getTraining(trainingId).getOrThrow()
            TrainingRepository.updateTraining(originalTraining.copy(location = "Cancha secundaria")).getOrThrow()
            val updatedTraining = TrainingRepository.getTraining(trainingId).getOrThrow()
            assertEquals("Cancha secundaria", updatedTraining.location)
            assertEquals(TrainingStatus.COMPLETED, updatedTraining.status)
            val anotherTraining = TrainingRepository.createTraining(originalTraining.copy(id = "")).getOrThrow()
            TrainingRepository.cancelTraining(anotherTraining, "Prueba", coach.uid).getOrThrow()
            assertEquals(TrainingStatus.CANCELED, TrainingRepository.getTraining(anotherTraining).getOrThrow().status)
        }
    }

    @Test
    fun onlyAnAdministratorCanCreateAndLinkAnAdminAccount() = runBlocking {
        withTimeout(30_000) {
            val unique = System.nanoTime().toString()
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val admin = AdminModel(name = "Admin", lastName = "Prueba",
                email = "new-admin-$unique@example.test", phone = "900000000")
            val coach = registerCoach("admin-check-$unique")
            assertTrue(AdminRepository.registerNewAdminWithAuth(admin, "sportpro-demo", coach.email, context).isFailure)

            val actorEmail = "bootstrap-$unique@example.test"
            val actor = FirebaseAuth.getInstance().createUserWithEmailAndPassword(actorEmail, "sportpro-demo")
                .await().user ?: error("Falta la cuenta ADM del emulador")
            seedDemoAdministrator(actor.uid)
            AdminRepository.registerNewAdminWithAuth(admin, "sportpro-demo", actorEmail, context).getOrThrow()
            assertEquals(actor.uid, FirebaseAuthManager.currentUid)

            FirebaseAuthManager.logout()
            val invited = FirebaseAuthManager.loginUser(admin.email, "sportpro-demo").getOrThrow()
            assertEquals("ADM", invited.role)
            assertEquals(invited.uid, invited.authUid)
            assertTrue(invited.id.matches(Regex("SP-ADM-[0-9]{6}")))
            val record = FirebaseFirestore.getInstance().collection("Admins").document(invited.id)
                .get(Source.SERVER).await()
            assertEquals(invited.uid, record.getString("authUid"))
            assertTrue(!record.contains("password"))
        }
    }

    @Test
    fun parentCanRegisterAndRequestPasswordRecovery() = runBlocking {
        withTimeout(30_000) {
            val email = "tutor-${System.nanoTime()}@example.test"
            val registered = FirebaseAuthManager.registerUser(
                UserModel(name = "Tutor", lastName = "Prueba", email = email, role = "PAD", acceptedTerms = true),
                "sportpro-demo"
            ).getOrThrow()
            assertEquals("PAD", registered.role)
            FirebaseAuthManager.logout()
            assertEquals(registered.uid, FirebaseAuthManager.loginUser(email, "sportpro-demo").getOrThrow().uid)
            FirebaseAuthManager.sendPasswordReset(email).getOrThrow()
        }
    }

    private suspend fun seedMissingCounters() = withContext(Dispatchers.IO) {
        val resource = "projects/demo-sportpro/databases/(default)/documents"
        val base = "http://10.0.2.2:8080/v1/$resource"
        val list = (URL("$base/counters?pageSize=100").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 5_000
            setRequestProperty("Authorization", "Bearer owner")
        }
        val existing = try {
            check(list.responseCode == 200) { "No se pudieron listar los contadores del emulador" }
            val response = JSONObject(list.inputStream.bufferedReader().use { it.readText() })
            val docs = response.optJSONArray("documents") ?: JSONArray()
            (0 until docs.length()).map { docs.getJSONObject(it).getString("name").substringAfterLast('/') }.toSet()
        } finally {
            list.disconnect()
        }

        val writes = JSONArray()
        SportProEntity.entries.filter { it.counter !in existing }.forEach { entity ->
            val fields = JSONObject().put("lastId", JSONObject().put("integerValue", "0"))
            val document = JSONObject().put("name", "$resource/counters/${entity.counter}").put("fields", fields)
            writes.put(JSONObject().put("update", document).put("currentDocument", JSONObject().put("exists", false)))
        }
        if (writes.length() != 0) {
            val commit = (URL("$base:commit").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 5_000
                readTimeout = 5_000
                doOutput = true
                setRequestProperty("Authorization", "Bearer owner")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            try {
                commit.outputStream.use { it.write(JSONObject().put("writes", writes).toString().toByteArray()) }
                check(commit.responseCode == 200) { "No se pudieron preparar los contadores del emulador" }
            } finally {
                commit.disconnect()
            }
        }
    }

    private suspend fun seedDemoAdministrator(uid: String) = withContext(Dispatchers.IO) {
        val resource = "projects/demo-sportpro/databases/(default)/documents"
        val url = URL("http://10.0.2.2:8080/v1/$resource:commit")
        val connection = url.openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.doOutput = true
            connection.setRequestProperty("Authorization", "Bearer owner")
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            connection.outputStream.use {
                it.write("""{"writes":[{"update":{"name":"$resource/users/$uid","fields":{"uid":{"stringValue":"$uid"},"role":{"stringValue":"ADM"},"active":{"booleanValue":true}}}}]}"""
                    .toByteArray(Charsets.UTF_8))
            }
            check(connection.responseCode == 200) { "No se pudo preparar la cuenta ADM del emulador" }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun registerPlayer(unique: String): UserModel =
        FirebaseAuthManager.registerUser(
            UserModel(
                name = "Jugador", lastName = unique,
                email = "jugador-$unique@example.test", role = "JUG", acceptedTerms = true
            ), "sportpro-demo"
        ).getOrThrow()

    private suspend fun registerCoach(unique: String): UserModel =
        FirebaseAuthManager.registerUser(
            UserModel(
                name = "Director",
                lastName = "Prueba",
                email = "director-$unique@example.test",
                role = "DT",
                acceptedTerms = true
            ),
            "sportpro-demo"
        ).getOrThrow()
}
