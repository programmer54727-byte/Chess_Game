package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.chess.data.ChessRepository
import com.example.chess.data.models.UserProfile
import com.example.chess.ui.auth.AuthScreen
import com.example.chess.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemDark = isSystemInDarkTheme()
            var isDarkMode by remember { mutableStateOf(systemDark) }

            MyApplicationTheme(darkTheme = isDarkMode) {
                ChessAppRoot(
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode }
                )
            }
        }
    }
}

@Composable
fun ChessAppRoot(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose {
            Firebase.auth.removeAuthStateListener(listener)
        }
    }

    if (currentUser == null) {
        AuthScreen(
            onAuthSuccess = {
                currentUser = Firebase.auth.currentUser
            }
        )
    } else {
        val uid = currentUser!!.uid
        val repository = remember(uid) { ChessRepository(context) }
        var userProfile by remember { mutableStateOf<UserProfile?>(null) }
        var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }

        // Fetch or create profile on login
        LaunchedEffect(uid) {
            val profile = repository.getOrCreateProfile(
                displayName = currentUser?.displayName ?: "Chess Master",
                email = currentUser?.email ?: ""
            )
            userProfile = profile
        }

        // Live observation of profile updates (rating changes, streaks, achievements)
        LaunchedEffect(uid) {
            repository.observeProfile(uid).collect { updated ->
                if (updated != null) {
                    userProfile = updated
                }
            }
        }

        if (userProfile == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            when (val screen = currentScreen) {
                is AppScreen.Home -> {
                    HomeScreen(
                        userProfile = userProfile!!,
                        isDarkMode = isDarkMode,
                        onToggleDarkMode = onToggleDarkMode,
                        onNavigate = { currentScreen = it }
                    )
                }
                is AppScreen.Play -> {
                    PlayScreen(
                        playMode = screen.mode,
                        repository = repository,
                        currentUserProfile = userProfile!!,
                        onBack = { currentScreen = AppScreen.Home }
                    )
                }
                is AppScreen.Puzzle -> {
                    PuzzleScreen(
                        isDailyMode = screen.isDaily,
                        repository = repository,
                        onBack = { currentScreen = AppScreen.Home }
                    )
                }
                is AppScreen.Profile -> {
                    ProfileScreen(
                        userProfile = userProfile!!,
                        repository = repository,
                        onSignOut = {
                            currentUser = null
                            currentScreen = AppScreen.Home
                        },
                        onBack = { currentScreen = AppScreen.Home }
                    )
                }
                is AppScreen.Community -> {
                    CommunityScreen(
                        userProfile = userProfile!!,
                        repository = repository,
                        onBack = { currentScreen = AppScreen.Home }
                    )
                }
                is AppScreen.Leaderboard -> {
                    LeaderboardScreen(
                        repository = repository,
                        onBack = { currentScreen = AppScreen.Home }
                    )
                }
            }
        }
    }
}
