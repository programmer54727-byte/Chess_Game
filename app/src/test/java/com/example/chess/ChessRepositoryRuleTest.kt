package com.example.chess

import com.example.base.FirestoreEmulatorTestBase
import com.example.chess.data.ChessRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test

class ChessRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun unauthenticatedUser_cannotAccessRepoWrite() {
        val repo = ChessRepository(firestore)
        auth.signOut()

        assertThrows(IllegalStateException::class.java) {
            repo.requireUserId()
        }
    }

    @Test
    fun authenticatedUser_canCreateAndReadProfile() = runBlocking {
        val uid = signInTestUser("player1@test.com")
        val repo = ChessRepository(firestore)

        val profile = repo.getOrCreateProfile(displayName = "ChessGrandmaster", email = "player1@test.com")
        assertEquals(uid, profile.userId)
        assertEquals(1200, profile.rating)

        val retrieved = repo.observeProfile(uid).first()
        assertNotNull(retrieved)
        assertEquals("ChessGrandmaster", retrieved?.displayName)
    }

    @Test
    fun authenticatedUser_canPublishCommunityPost() = runBlocking {
        val uid = signInTestUser("player2@test.com")
        val repo = ChessRepository(firestore)

        repo.createCommunityPost(
            authorName = "Bobby",
            authorRating = 1500,
            title = "King's Indian Setup",
            content = "Great attacking possibilities on the kingside."
        )

        val posts = repo.observeCommunityPosts().first()
        assertTrue(posts.any { it.title == "King's Indian Setup" && it.authorId == uid })
    }
}
