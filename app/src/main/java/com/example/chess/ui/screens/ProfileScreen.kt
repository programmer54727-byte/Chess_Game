package com.example.chess.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.chess.data.ChessRepository
import com.example.chess.data.models.UserProfile
import com.example.chess.ui.auth.signOut
import kotlinx.coroutines.launch

fun getRatingTier(rating: Int): Pair<String, Color> {
    return when {
        rating >= 2000 -> "Grandmaster" to Color(0xFFEF4444)
        rating >= 1800 -> "Diamond" to Color(0xFF38BDF8)
        rating >= 1600 -> "Platinum" to Color(0xFFA855F7)
        rating >= 1400 -> "Gold" to Color(0xFFF59E0B)
        rating >= 1200 -> "Silver" to Color(0xFF94A3B8)
        else -> "Bronze" to Color(0xFFCD7F32)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userProfile: UserProfile,
    repository: ChessRepository,
    onSignOut: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    BackHandler {
        onBack()
    }

    var editBioDialog by remember { mutableStateOf(false) }
    var newBio by remember { mutableStateOf(userProfile.bio) }

    val (tierTitle, tierColor) = getRatingTier(userProfile.rating)
    val winRate = if (userProfile.gamesPlayed > 0) {
        (userProfile.wins * 100) / userProfile.gamesPlayed
    } else {
        0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Player Profile & Stats", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("profile_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            signOut(
                                context = context,
                                credentialManager = credentialManager,
                                onSignOutComplete = onSignOut,
                                scope = coroutineScope
                            )
                        },
                        modifier = Modifier.testTag("sign_out_button")
                    ) {
                        Icon(imageVector = Icons.Default.Logout, contentDescription = "Sign Out")
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
            // Header Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "♔",
                                fontSize = 42.sp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Text(
                            text = userProfile.displayName,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        // Rating & Rank Badge
                        Surface(
                            color = tierColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$tierTitle • ${userProfile.rating} ELO",
                                    fontWeight = FontWeight.Bold,
                                    color = tierColor
                                )
                            }
                        }

                        Text(
                            text = "\"${userProfile.bio}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        TextButton(onClick = { editBioDialog = true }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Edit Bio")
                        }
                    }
                }
            }

            // Stats Grid
            item {
                Text(
                    text = "Competitive Statistics",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Matches", "${userProfile.gamesPlayed}", Icons.Default.SportsScore, Modifier.weight(1f))
                    StatCard("Win Rate", "$winRate%", Icons.Default.TrendingUp, Modifier.weight(1f))
                    StatCard("Wins", "${userProfile.wins}", Icons.Default.EmojiEvents, Modifier.weight(1f))
                }
            }

            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatCard("Losses", "${userProfile.losses}", Icons.Default.Close, Modifier.weight(1f))
                    StatCard("Draws", "${userProfile.draws}", Icons.Default.DragHandle, Modifier.weight(1f))
                    StatCard("Puzzles", "${userProfile.puzzlesSolved}", Icons.Default.Extension, Modifier.weight(1f))
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Whatshot, contentDescription = null, tint = Color(0xFFF97316), modifier = Modifier.size(28.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Daily Challenge Streak", fontWeight = FontWeight.Bold)
                                Text("Solve daily puzzles consecutively", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        Text(
                            text = "${userProfile.dailyStreak} Days",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFF97316)
                        )
                    }
                }
            }

            // Achievements Section
            item {
                Text(
                    text = "Achievements & Badges",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            items(userProfile.achievements) { badge ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.MilitaryTech, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(32.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(badge, fontWeight = FontWeight.Bold)
                                Text("Unlocked honor badge", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        IconButton(
                            onClick = {
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "Chess Arena Achievement")
                                    putExtra(
                                        Intent.EXTRA_TEXT,
                                        "🏆 I just unlocked the '$badge' achievement in Chess Arena! My rating is ${userProfile.rating} ELO. Can you defeat me on the board?"
                                    )
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Achievement"))
                            },
                            modifier = Modifier.testTag("share_achievement_$badge")
                        ) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = "Share")
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (editBioDialog) {
        AlertDialog(
            onDismissRequest = { editBioDialog = false },
            title = { Text("Update Bio") },
            text = {
                OutlinedTextField(
                    value = newBio,
                    onValueChange = { newBio = it },
                    label = { Text("Bio") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.updateProfile(userProfile.copy(bio = newBio.trim()))
                            editBioDialog = false
                            Toast.makeText(context, "Bio updated", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { editBioDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Text(text = value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
