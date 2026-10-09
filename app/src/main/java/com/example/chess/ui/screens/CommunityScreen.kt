package com.example.chess.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.data.ChessRepository
import com.example.chess.data.models.CommunityPost
import com.example.chess.data.models.DirectMessage
import com.example.chess.data.models.UserProfile
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityScreen(
    userProfile: UserProfile,
    repository: ChessRepository,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    var selectedTab by remember { mutableStateOf(0) } // 0 = Board, 1 = Direct Messages
    var posts by remember { mutableStateOf<List<CommunityPost>>(emptyList()) }
    var showCreatePostDialog by remember { mutableStateOf(false) }

    // Direct Messages state
    var dmRecipientId by remember { mutableStateOf("") }
    var dmRecipientName by remember { mutableStateOf("") }
    var dmConversationActive by remember { mutableStateOf(false) }
    var directMessages by remember { mutableStateOf<List<DirectMessage>>(emptyList()) }
    var dmInputText by remember { mutableStateOf("") }

    // Load community posts
    LaunchedEffect(Unit) {
        repository.observeCommunityPosts().collect { postList ->
            posts = postList
        }
    }

    // Load DM conversation
    LaunchedEffect(dmRecipientId, dmConversationActive) {
        if (dmConversationActive && dmRecipientId.isNotBlank()) {
            repository.observeDirectMessages(dmRecipientId).collect { msgs ->
                directMessages = msgs
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chess Community & Chat", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("community_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(
                    onClick = { showCreatePostDialog = true },
                    modifier = Modifier.testTag("create_post_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Create Post")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Community Board") },
                    icon = { Icon(imageVector = Icons.Default.Forum, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Direct Messages") },
                    icon = { Icon(imageVector = Icons.Default.Mail, contentDescription = null) }
                )
            }

            if (selectedTab == 0) {
                // Community Board List
                if (posts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No community posts yet. Be the first to share an opening or analysis!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(posts) { post ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = post.title,
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                        Surface(
                                            color = MaterialTheme.colorScheme.primaryContainer,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "${post.authorName} (${post.authorRating})",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = post.content,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        repository.likeCommunityPost(post.postId)
                                                    }
                                                }
                                            ) {
                                                Icon(imageVector = Icons.Default.ThumbUp, contentDescription = "Like", tint = MaterialTheme.colorScheme.primary)
                                            }
                                            Text(
                                                text = "${post.likesCount} likes",
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }

                                        TextButton(
                                            onClick = {
                                                dmRecipientId = post.authorId
                                                dmRecipientName = post.authorName
                                                dmConversationActive = true
                                                selectedTab = 1
                                            }
                                        ) {
                                            Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Message Author")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Direct Messages
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!dmConversationActive) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Start a Private Conversation", fontWeight = FontWeight.Bold)
                                OutlinedTextField(
                                    value = dmRecipientId,
                                    onValueChange = { dmRecipientId = it },
                                    label = { Text("Recipient User ID") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                OutlinedTextField(
                                    value = dmRecipientName,
                                    onValueChange = { dmRecipientName = it },
                                    label = { Text("Recipient Name") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = {
                                        if (dmRecipientId.isNotBlank()) {
                                            dmConversationActive = true
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Open Conversation")
                                }
                            }
                        }
                    } else {
                        // Active DM thread
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Chat with ${dmRecipientName.ifBlank { dmRecipientId.take(6) }}",
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { dmConversationActive = false }) {
                                Text("Switch Player")
                            }
                        }

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (directMessages.isEmpty()) {
                                item {
                                    Text(
                                        text = "No messages yet. Send a greeting!",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                items(directMessages) { dm ->
                                    val isMe = dm.senderId == repository.currentUserId()
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                    ) {
                                        Surface(
                                            color = if (isMe) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text(
                                                text = dm.text,
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = dmInputText,
                                onValueChange = { dmInputText = it },
                                placeholder = { Text("Private message...") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    if (dmInputText.isNotBlank()) {
                                        coroutineScope.launch {
                                            repository.sendDirectMessage(
                                                receiverId = dmRecipientId,
                                                receiverName = dmRecipientName,
                                                senderName = userProfile.displayName,
                                                text = dmInputText.trim()
                                            )
                                            dmInputText = ""
                                        }
                                    }
                                }
                            ) {
                                Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreatePostDialog) {
        var postTitle by remember { mutableStateOf("") }
        var postContent by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreatePostDialog = false },
            title = { Text("New Chess Post") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = postTitle,
                        onValueChange = { postTitle = it },
                        label = { Text("Title (e.g. Sicilian Defense tip)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = postContent,
                        onValueChange = { postContent = it },
                        label = { Text("Discussion or Analysis") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (postTitle.isNotBlank() && postContent.isNotBlank()) {
                            coroutineScope.launch {
                                repository.createCommunityPost(
                                    authorName = userProfile.displayName,
                                    authorRating = userProfile.rating,
                                    title = postTitle.trim(),
                                    content = postContent.trim()
                                )
                                showCreatePostDialog = false
                                Toast.makeText(context, "Post shared with community!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                ) {
                    Text("Publish")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePostDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
