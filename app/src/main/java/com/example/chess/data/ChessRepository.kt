package com.example.chess.data

import android.content.Context
import com.example.R
import com.example.chess.data.models.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ChessRepository(
    val db: FirebaseFirestore
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
    }

    fun currentUserId(): String? = auth.currentUser?.uid

    // --- Profile Management ---

    suspend fun getOrCreateProfile(displayName: String, email: String): UserProfile {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val snapshot = docRef.get().await()

        if (snapshot.exists()) {
            return snapshot.toObject(UserProfile::class.java) ?: UserProfile(userId = uid, displayName = displayName, email = email)
        }

        val initialProfile = UserProfile(
            userId = uid,
            displayName = displayName.ifBlank { "Grandmaster" },
            email = email,
            rating = 1200,
            gamesPlayed = 0,
            wins = 0,
            losses = 0,
            draws = 0,
            puzzlesSolved = 0,
            dailyStreak = 0,
            bio = "Ready for the next match!",
            achievements = listOf("Newcomer")
        )

        val data = mapOf(
            "userId" to uid,
            "displayName" to initialProfile.displayName,
            "email" to initialProfile.email,
            "rating" to initialProfile.rating,
            "gamesPlayed" to 0,
            "wins" to 0,
            "losses" to 0,
            "draws" to 0,
            "puzzlesSolved" to 0,
            "dailyStreak" to 0,
            "lastDailyPuzzleDate" to "",
            "bio" to initialProfile.bio,
            "achievements" to initialProfile.achievements,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        docRef.set(data).await()
        return initialProfile
    }

    fun observeProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun updateProfile(profile: UserProfile) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val data = mapOf(
            "userId" to uid,
            "displayName" to profile.displayName,
            "email" to profile.email,
            "rating" to profile.rating,
            "gamesPlayed" to profile.gamesPlayed,
            "wins" to profile.wins,
            "losses" to profile.losses,
            "draws" to profile.draws,
            "puzzlesSolved" to profile.puzzlesSolved,
            "dailyStreak" to profile.dailyStreak,
            "lastDailyPuzzleDate" to profile.lastDailyPuzzleDate,
            "bio" to profile.bio,
            "achievements" to profile.achievements,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        docRef.set(data).await()
    }

    suspend fun recordPuzzleSolved(isDailyChallenge: Boolean) {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val profile = docRef.get().await().toObject(UserProfile::class.java) ?: return

        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        var newStreak = profile.dailyStreak
        var newLastDate = profile.lastDailyPuzzleDate

        if (isDailyChallenge && profile.lastDailyPuzzleDate != todayStr) {
            newStreak += 1
            newLastDate = todayStr
        }

        val newAchievements = profile.achievements.toMutableList()
        val newSolved = profile.puzzlesSolved + 1
        if (newSolved >= 1 && !newAchievements.contains("Puzzle Novice")) newAchievements.add("Puzzle Novice")
        if (newSolved >= 5 && !newAchievements.contains("Tactical Mind")) newAchievements.add("Tactical Mind")
        if (newStreak >= 3 && !newAchievements.contains("Daily Master")) newAchievements.add("Daily Master")

        docRef.update(
            mapOf(
                "puzzlesSolved" to newSolved,
                "dailyStreak" to newStreak,
                "lastDailyPuzzleDate" to newLastDate,
                "achievements" to newAchievements,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    fun observeTopPlayers(): Flow<List<UserProfile>> {
        val path = "users"
        return db.collection("users")
            .orderBy("rating", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { it.toObjects(UserProfile::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    // --- Real-time Matchmaking ---

    fun searchAndMatch(userProfile: UserProfile): Flow<MatchmakingStatus> = callbackFlow {
        val uid = requireUserId()
        trySend(MatchmakingStatus.Searching)

        val queueRef = db.collection("matchmaking_queue")

        // Look for existing waiting player in queue who is not ourselves
        val snapshot = queueRef
            .whereEqualTo("status", "waiting")
            .limit(10)
            .get()
            .await()

        val opponent = snapshot.documents
            .mapNotNull { it.toObject(MatchmakingQueueEntry::class.java) }
            .firstOrNull { it.userId != uid }

        if (opponent != null) {
            // Pair with opponent! Create game
            val gameId = "game_${UUID.randomUUID().toString().take(8)}"
            val isWhite = System.currentTimeMillis() % 2 == 0L

            val whiteId = if (isWhite) uid else opponent.userId
            val whiteName = if (isWhite) userProfile.displayName else opponent.displayName
            val whiteRating = if (isWhite) userProfile.rating else opponent.rating

            val blackId = if (isWhite) opponent.userId else uid
            val blackName = if (isWhite) opponent.displayName else userProfile.displayName
            val blackRating = if (isWhite) opponent.rating else userProfile.rating

            val gameData = mapOf(
                "gameId" to gameId,
                "whitePlayerId" to whiteId,
                "whitePlayerName" to whiteName,
                "whitePlayerRating" to whiteRating,
                "blackPlayerId" to blackId,
                "blackPlayerName" to blackName,
                "blackPlayerRating" to blackRating,
                "fen" to "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
                "moves" to emptyList<String>(),
                "pgn" to "",
                "currentTurn" to "white",
                "status" to "in_progress",
                "statusReason" to "",
                "winnerId" to "",
                "isRanked" to true,
                "whiteRatingChange" to 0,
                "blackRatingChange" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("games").document(gameId).set(gameData).await()

            // Update opponent queue entry to point to this game
            queueRef.document(opponent.userId).update(
                mapOf("status" to "matched", "gameId" to gameId)
            ).await()

            trySend(MatchmakingStatus.Matched(gameId))
            close()
        } else {
            // No waiting opponent, register self in queue
            val selfEntry = mapOf(
                "userId" to uid,
                "displayName" to userProfile.displayName,
                "rating" to userProfile.rating,
                "status" to "waiting",
                "gameId" to "",
                "createdAt" to FieldValue.serverTimestamp()
            )
            queueRef.document(uid).set(selfEntry).await()

            // Listen to our own queue entry to see when someone pairs us
            val registration = queueRef.document(uid).addSnapshotListener { doc, err ->
                if (err != null) {
                    trySend(MatchmakingStatus.Error(err.localizedMessage ?: "Queue error"))
                    return@addSnapshotListener
                }
                val entry = doc?.toObject(MatchmakingQueueEntry::class.java)
                if (entry?.status == "matched" && entry.gameId.isNotBlank()) {
                    trySend(MatchmakingStatus.Matched(entry.gameId))
                }
            }

            awaitClose {
                registration.remove()
                // Cancel queue entry on cancel
                queueRef.document(uid).delete()
            }
        }
    }

    suspend fun cancelMatchmaking() {
        val uid = currentUserId() ?: return
        db.collection("matchmaking_queue").document(uid).delete().await()
    }

    // --- Online Game Synchronization ---

    fun observeGame(gameId: String): Flow<OnlineGame?> {
        val path = "games/$gameId"
        return db.collection("games").document(gameId)
            .snapshots()
            .map { it.toObject(OnlineGame::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun submitOnlineMove(
        gameId: String,
        newFen: String,
        sanMove: String,
        nextTurn: String,
        isFinished: Boolean = false,
        newStatus: String = "in_progress",
        winnerId: String = "",
        statusReason: String = "",
        pgn: String = ""
    ) {
        val gameRef = db.collection("games").document(gameId)
        val game = gameRef.get().await().toObject(OnlineGame::class.java) ?: return

        val updatedMoves = game.moves + sanMove
        val updates = mutableMapOf<String, Any>(
            "fen" to newFen,
            "moves" to updatedMoves,
            "currentTurn" to nextTurn,
            "status" to newStatus,
            "pgn" to pgn,
            "updatedAt" to FieldValue.serverTimestamp()
        )

        if (isFinished) {
            updates["winnerId"] = winnerId
            updates["statusReason"] = statusReason

            // Calculate ELO changes
            if (game.isRanked) {
                val (whiteDelta, blackDelta) = calculateEloChanges(
                    game.whitePlayerRating,
                    game.blackPlayerRating,
                    winnerId == game.whitePlayerId,
                    winnerId == game.blackPlayerId
                )
                updates["whiteRatingChange"] = whiteDelta
                updates["blackRatingChange"] = blackDelta

                // Update both players' profiles
                updateRatingsPostGame(game.whitePlayerId, whiteDelta, winnerId == game.whitePlayerId, winnerId.isBlank())
                updateRatingsPostGame(game.blackPlayerId, blackDelta, winnerId == game.blackPlayerId, winnerId.isBlank())
            }
        }

        gameRef.update(updates).await()
    }

    suspend fun resignOnlineGame(gameId: String, resigningPlayerId: String) {
        val gameRef = db.collection("games").document(gameId)
        val game = gameRef.get().await().toObject(OnlineGame::class.java) ?: return
        val winnerId = if (resigningPlayerId == game.whitePlayerId) game.blackPlayerId else game.whitePlayerId
        val newStatus = if (winnerId == game.whitePlayerId) "white_won" else "black_won"

        val (whiteDelta, blackDelta) = calculateEloChanges(
            game.whitePlayerRating,
            game.blackPlayerRating,
            winnerId == game.whitePlayerId,
            winnerId == game.blackPlayerId
        )

        gameRef.update(
            mapOf(
                "status" to newStatus,
                "winnerId" to winnerId,
                "statusReason" to "Resignation",
                "whiteRatingChange" to whiteDelta,
                "blackRatingChange" to blackDelta,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()

        updateRatingsPostGame(game.whitePlayerId, whiteDelta, winnerId == game.whitePlayerId, false)
        updateRatingsPostGame(game.blackPlayerId, blackDelta, winnerId == game.blackPlayerId, false)
    }

    private suspend fun updateRatingsPostGame(playerId: String, delta: Int, isWin: Boolean, isDraw: Boolean) {
        if (playerId.isBlank()) return
        val userRef = db.collection("users").document(playerId)
        val user = userRef.get().await().toObject(UserProfile::class.java) ?: return

        val newRating = maxOf(100, user.rating + delta)
        val games = user.gamesPlayed + 1
        val wins = if (isWin) user.wins + 1 else user.wins
        val losses = if (!isWin && !isDraw) user.losses + 1 else user.losses
        val draws = if (isDraw) user.draws + 1 else user.draws

        val achievements = user.achievements.toMutableList()
        if (wins >= 1 && !achievements.contains("First Victory")) achievements.add("First Victory")
        if (wins >= 10 && !achievements.contains("Chess Warrior")) achievements.add("Chess Warrior")
        if (newRating >= 1500 && !achievements.contains("Expert Ranked")) achievements.add("Expert Ranked")
        if (newRating >= 1800 && !achievements.contains("Master Ranked")) achievements.add("Master Ranked")

        userRef.update(
            mapOf(
                "rating" to newRating,
                "gamesPlayed" to games,
                "wins" to wins,
                "losses" to losses,
                "draws" to draws,
                "achievements" to achievements,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private fun calculateEloChanges(whiteRating: Int, blackRating: Int, whiteWon: Boolean, blackWon: Boolean): Pair<Int, Int> {
        val k = 32.0
        val expectedWhite = 1.0 / (1.0 + Math.pow(10.0, (blackRating - whiteRating) / 400.0))
        val expectedBlack = 1.0 - expectedWhite

        val actualWhite = when {
            whiteWon -> 1.0
            blackWon -> 0.0
            else -> 0.5
        }
        val actualBlack = 1.0 - actualWhite

        val whiteChange = Math.round(k * (actualWhite - expectedWhite)).toInt()
        val blackChange = Math.round(k * (actualBlack - expectedBlack)).toInt()
        return whiteChange to blackChange
    }

    // --- In-Game Chat ---

    fun observeGameMessages(gameId: String): Flow<List<GameMessage>> {
        val path = "games/$gameId/messages"
        return db.collection("games").document(gameId).collection("messages")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { it.toObjects(GameMessage::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun sendGameMessage(gameId: String, senderName: String, text: String) {
        val uid = requireUserId()
        val msgId = "msg_${UUID.randomUUID().toString().take(8)}"
        val msgData = mapOf(
            "messageId" to msgId,
            "senderId" to uid,
            "senderName" to senderName,
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("games").document(gameId).collection("messages").document(msgId).set(msgData).await()
    }

    // --- Community Forum Posts ---

    fun observeCommunityPosts(): Flow<List<CommunityPost>> {
        val path = "community_posts"
        return db.collection("community_posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .snapshots()
            .map { it.toObjects(CommunityPost::class.java) }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun createCommunityPost(authorName: String, authorRating: Int, title: String, content: String) {
        val uid = requireUserId()
        val postId = "post_${UUID.randomUUID().toString().take(8)}"
        val postData = mapOf(
            "postId" to postId,
            "authorId" to uid,
            "authorName" to authorName,
            "authorRating" to authorRating,
            "title" to title,
            "content" to content,
            "likesCount" to 0,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("community_posts").document(postId).set(postData).await()
    }

    suspend fun likeCommunityPost(postId: String) {
        val docRef = db.collection("community_posts").document(postId)
        docRef.update("likesCount", FieldValue.increment(1)).await()
    }

    // --- Direct Messages ---

    fun observeDirectMessages(otherUserId: String): Flow<List<DirectMessage>> {
        val uid = requireUserId()
        val path = "direct_messages"
        return db.collection("direct_messages")
            .whereArrayContains("participants", uid)
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.toObjects(DirectMessage::class.java)
                    .filter { it.participants.contains(otherUserId) }
            }
            .catch { error ->
                if (error is Exception) handleFirestoreError(error, OperationType.LIST, path)
                emit(emptyList())
            }
    }

    suspend fun sendDirectMessage(receiverId: String, receiverName: String, senderName: String, text: String) {
        val uid = requireUserId()
        val msgId = "dm_${UUID.randomUUID().toString().take(8)}"
        val data = mapOf(
            "messageId" to msgId,
            "senderId" to uid,
            "senderName" to senderName,
            "receiverId" to receiverId,
            "receiverName" to receiverName,
            "participants" to listOf(uid, receiverId),
            "text" to text,
            "createdAt" to FieldValue.serverTimestamp()
        )
        db.collection("direct_messages").document(msgId).set(data).await()
    }
}

sealed class MatchmakingStatus {
    object Idle : MatchmakingStatus()
    object Searching : MatchmakingStatus()
    data class Matched(val gameId: String) : MatchmakingStatus()
    data class Error(val message: String) : MatchmakingStatus()
}
