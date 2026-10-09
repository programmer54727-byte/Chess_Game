package com.example.chess.data.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val rating: Int = 1200,
    val gamesPlayed: Int = 0,
    val wins: Int = 0,
    val losses: Int = 0,
    val draws: Int = 0,
    val puzzlesSolved: Int = 0,
    val dailyStreak: Int = 0,
    val lastDailyPuzzleDate: String = "",
    val bio: String = "Chess enthusiast",
    val achievements: List<String> = emptyList(),
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class MatchmakingQueueEntry(
    val userId: String = "",
    val displayName: String = "",
    val rating: Int = 1200,
    val status: String = "waiting",
    val gameId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class OnlineGame(
    val gameId: String = "",
    val whitePlayerId: String = "",
    val whitePlayerName: String = "",
    val whitePlayerRating: Int = 1200,
    val blackPlayerId: String = "",
    val blackPlayerName: String = "",
    val blackPlayerRating: Int = 1200,
    val fen: String = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
    val moves: List<String> = emptyList(),
    val pgn: String = "",
    val currentTurn: String = "white",
    val status: String = "in_progress",
    val statusReason: String = "",
    val winnerId: String = "",
    val isRanked: Boolean = true,
    val whiteRatingChange: Int = 0,
    val blackRatingChange: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class GameMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class CommunityPost(
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorRating: Int = 1200,
    val title: String = "",
    val content: String = "",
    val likesCount: Int = 0,
    @ServerTimestamp val createdAt: Timestamp? = null
)

data class DirectMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val participants: List<String> = emptyList(),
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)
