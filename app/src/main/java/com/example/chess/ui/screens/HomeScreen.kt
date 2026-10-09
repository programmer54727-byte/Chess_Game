package com.example.chess.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.chess.data.models.UserProfile

sealed class AppScreen {
    object Home : AppScreen()
    data class Play(val mode: PlayMode) : AppScreen()
    data class Puzzle(val isDaily: Boolean) : AppScreen()
    object Profile : AppScreen()
    object Community : AppScreen()
    object Leaderboard : AppScreen()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    userProfile: UserProfile,
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    onNavigate: (AppScreen) -> Unit
) {
    val (tierTitle, tierColor) = getRatingTier(userProfile.rating)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_chess_app),
                            contentDescription = null,
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Chess Arena", fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleDarkMode,
                        modifier = Modifier.testTag("dark_mode_toggle")
                    ) {
                        Icon(
                            imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Light/Dark Theme"
                        )
                    }
                    IconButton(
                        onClick = { onNavigate(AppScreen.Profile) },
                        modifier = Modifier.testTag("nav_profile")
                    ) {
                        Icon(imageVector = Icons.Default.AccountCircle, contentDescription = "Profile")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Banner Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Image(
                            painter = painterResource(id = R.drawable.chess_hero_banner),
                            contentDescription = "Chess Master Hero Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        // Gradient scrim
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Transparent, Color(0xCC0F172A))
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Welcome, ${userProfile.displayName}",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Online matchmaking, engine practice, and tactical puzzles",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            // User Rating & Daily Streak Status
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Current Rating", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "${userProfile.rating} ELO",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = tierColor.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = tierTitle,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = tierColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text("Daily Streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "${userProfile.dailyStreak} Days",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFF97316)
                                )
                            }
                        }
                    }
                }
            }

            // Main Play Modes Section
            item {
                Text(
                    text = "Play Chess",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            // Real-Time Ranked Matchmaking
            item {
                PlayModeCard(
                    title = "⚔️ Real-Time Ranked Matchmaking",
                    subtitle = "Compete against players online with live rating updates",
                    icon = Icons.Default.Public,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    testTag = "play_online_button",
                    onClick = { onNavigate(AppScreen.Play(PlayMode.ONLINE_MULTIPLAYER)) }
                )
            }

            // Offline vs Computer Engine
            item {
                PlayModeCard(
                    title = "🤖 Practice vs Computer Engine",
                    subtitle = "Offline engine with Novice, Club, and Grandmaster difficulties + undo & hints",
                    icon = Icons.Default.Computer,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    testTag = "play_bot_button",
                    onClick = { onNavigate(AppScreen.Play(PlayMode.VS_BOT)) }
                )
            }

            // Pass & Play (Local 2 player)
            item {
                PlayModeCard(
                    title = "👥 Pass & Play",
                    subtitle = "Local two-player chess on a single device",
                    icon = Icons.Default.People,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    testTag = "play_local_button",
                    onClick = { onNavigate(AppScreen.Play(PlayMode.PASS_AND_PLAY)) }
                )
            }

            // Training & Daily Challenge Section
            item {
                Text(
                    text = "Training & Puzzles",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureCard(
                        title = "⭐ Daily Challenge",
                        subtitle = "Fresh daily tactic",
                        icon = Icons.Default.Star,
                        modifier = Modifier.weight(1f),
                        testTag = "daily_challenge_button",
                        onClick = { onNavigate(AppScreen.Puzzle(isDaily = true)) }
                    )
                    FeatureCard(
                        title = "🧩 Tactical Puzzles",
                        subtitle = "Forks, pins, mates",
                        icon = Icons.Default.Extension,
                        modifier = Modifier.weight(1f),
                        testTag = "puzzles_button",
                        onClick = { onNavigate(AppScreen.Puzzle(isDaily = false)) }
                    )
                }
            }

            // Community & Social Section
            item {
                Text(
                    text = "Social & Community",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FeatureCard(
                        title = "💬 Community & Chat",
                        subtitle = "Forum & private DMs",
                        icon = Icons.Default.Forum,
                        modifier = Modifier.weight(1f),
                        testTag = "community_button",
                        onClick = { onNavigate(AppScreen.Community) }
                    )
                    FeatureCard(
                        title = "🏆 Leaderboard",
                        subtitle = "Global standings",
                        icon = Icons.Default.Leaderboard,
                        modifier = Modifier.weight(1f),
                        testTag = "leaderboard_button",
                        onClick = { onNavigate(AppScreen.Leaderboard) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun PlayModeCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor, contentColor = contentColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(contentColor.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = contentColor.copy(alpha = 0.8f))
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null)
        }
    }
}

@Composable
fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.testTag(testTag),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Text(text = title, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
