package com.example.chess.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.data.ChessRepository
import com.example.chess.data.models.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    repository: ChessRepository,
    onBack: () -> Unit
) {
    BackHandler {
        onBack()
    }

    var leaders by remember { mutableStateOf<List<UserProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        repository.observeTopPlayers().collect { list ->
            leaders = list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Global Ranked Leaderboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("leaderboard_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (leaders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "Top Competitive Grandmasters",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                itemsIndexed(leaders) { index, player ->
                    val rank = index + 1
                    val rankBadgeColor = when (rank) {
                        1 -> Color(0xFFEAB308) // Gold
                        2 -> Color(0xFF94A3B8) // Silver
                        3 -> Color(0xFFCD7F32) // Bronze
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                    val isTopThree = rank <= 3

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (player.userId == repository.currentUserId()) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(rankBadgeColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isTopThree) {
                                        Icon(imageVector = Icons.Default.EmojiEvents, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    } else {
                                        Text(
                                            text = "$rank",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = player.displayName,
                                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "${player.gamesPlayed} games played • ${player.wins}W / ${player.losses}L",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${player.rating} ELO",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                val (tier, tierColor) = getRatingTier(player.rating)
                                Text(
                                    text = tier,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = tierColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
