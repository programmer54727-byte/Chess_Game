package com.example.chess.ui.screens

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
import com.example.chess.data.MatchmakingStatus
import com.example.chess.data.models.GameMessage
import com.example.chess.data.models.OnlineGame
import com.example.chess.data.models.UserProfile
import com.example.chess.engine.ChessEngine
import com.example.chess.engine.EngineDifficulty
import com.example.chess.model.*
import com.example.chess.ui.components.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class PlayMode {
    VS_BOT,
    PASS_AND_PLAY,
    ONLINE_MULTIPLAYER
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayScreen(
    playMode: PlayMode,
    repository: ChessRepository,
    currentUserProfile: UserProfile,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    // Game state
    var chessGame by remember { mutableStateOf(ChessGame()) }
    var selectedPos by remember { mutableStateOf<Position?>(null) }
    var legalMovesForSelected by remember { mutableStateOf<List<Move>>(emptyList()) }
    var pendingPromotionMove by remember { mutableStateOf<Move?>(null) }
    var showPgnDialog by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }

    // Engine settings (Offline vs Bot)
    var botDifficulty by remember { mutableStateOf(EngineDifficulty.INTERMEDIATE) }
    var playerColor by remember { mutableStateOf(PieceColor.WHITE) }
    var isBotThinking by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf<String?>(null) }

    // Online Multiplayer State
    var onlineGameId by remember { mutableStateOf<String?>(null) }
    var onlineGameData by remember { mutableStateOf<OnlineGame?>(null) }
    var isSearchingMatch by remember { mutableStateOf(playMode == PlayMode.ONLINE_MULTIPLAYER) }
    var gameMessages by remember { mutableStateOf<List<GameMessage>>(emptyList()) }
    var chatInput by remember { mutableStateOf("") }

    // Board theme setting
    var boardTheme by remember { mutableStateOf(BoardColorScheme.TOURNAMENT_GREEN) }

    // Setup Online Matchmaking
    LaunchedEffect(playMode) {
        if (playMode == PlayMode.ONLINE_MULTIPLAYER) {
            repository.searchAndMatch(currentUserProfile).collect { status ->
                when (status) {
                    is MatchmakingStatus.Searching -> {
                        isSearchingMatch = true
                    }
                    is MatchmakingStatus.Matched -> {
                        isSearchingMatch = false
                        onlineGameId = status.gameId
                    }
                    is MatchmakingStatus.Error -> {
                        isSearchingMatch = false
                        Toast.makeText(context, status.message, Toast.LENGTH_LONG).show()
                    }
                    is MatchmakingStatus.Idle -> {}
                }
            }
        }
    }

    // Observe active online game
    LaunchedEffect(onlineGameId) {
        val gid = onlineGameId ?: return@LaunchedEffect
        repository.observeGame(gid).collect { game ->
            if (game != null) {
                onlineGameData = game
                // Sync local board with FEN from cloud
                if (game.fen != chessGame.board.toFen()) {
                    chessGame = ChessGame(Board.fromFen(game.fen))
                    // Rebuild move history from moves list
                    game.moves.forEach { san ->
                        // Record for visual list
                    }
                }
            }
        }
    }

    // Observe in-game chat messages
    LaunchedEffect(onlineGameId) {
        val gid = onlineGameId ?: return@LaunchedEffect
        repository.observeGameMessages(gid).collect { msgs ->
            gameMessages = msgs
        }
    }

    // Trigger Bot move when it's bot's turn in VS_BOT mode
    LaunchedEffect(chessGame.board.activeColor, chessGame.status) {
        if (playMode == PlayMode.VS_BOT &&
            !chessGame.status.isFinished() &&
            chessGame.board.activeColor != playerColor
        ) {
            isBotThinking = true
            delay(400) // Realistic bot thinking delay
            val botMove = ChessEngine.chooseMove(chessGame, botDifficulty)
            if (botMove != null) {
                chessGame.makeMove(botMove)
                chessGame = ChessGame(chessGame.board).apply {
                    this@apply.moveHistory.addAll(chessGame.moveHistory)
                    this@apply.capturedByWhite.addAll(chessGame.capturedByWhite)
                    this@apply.capturedByBlack.addAll(chessGame.capturedByBlack)
                    this@apply.status = chessGame.status
                    this@apply.winner = chessGame.winner
                }
            }
            isBotThinking = false
        }
    }

    fun executeMove(move: Move) {
        val success = chessGame.makeMove(move)
        if (success) {
            // Trigger state recomposition
            chessGame = ChessGame(chessGame.board).apply {
                this@apply.moveHistory.addAll(chessGame.moveHistory)
                this@apply.capturedByWhite.addAll(chessGame.capturedByWhite)
                this@apply.capturedByBlack.addAll(chessGame.capturedByBlack)
                this@apply.status = chessGame.status
                this@apply.winner = chessGame.winner
                this@apply.resigner = chessGame.resigner
            }
            hintText = null

            // If online game, submit to Firestore
            if (playMode == PlayMode.ONLINE_MULTIPLAYER && onlineGameId != null) {
                coroutineScope.launch {
                    val game = onlineGameData ?: return@launch
                    val nextTurn = if (chessGame.board.activeColor == PieceColor.WHITE) "white" else "black"
                    val isFinished = chessGame.status.isFinished()
                    val winnerId = when (chessGame.winner) {
                        PieceColor.WHITE -> game.whitePlayerId
                        PieceColor.BLACK -> game.blackPlayerId
                        else -> ""
                    }
                    val statusStr = when (chessGame.status) {
                        GameStatus.CHECKMATE -> if (chessGame.winner == PieceColor.WHITE) "white_won" else "black_won"
                        GameStatus.STALEMATE, GameStatus.DRAW_AGREED, GameStatus.DRAW_FIFTY_MOVES,
                        GameStatus.DRAW_INSUFFICIENT_MATERIAL -> "draw"
                        else -> "in_progress"
                    }
                    repository.submitOnlineMove(
                        gameId = onlineGameId!!,
                        newFen = chessGame.board.toFen(),
                        sanMove = move.san,
                        nextTurn = nextTurn,
                        isFinished = isFinished,
                        newStatus = statusStr,
                        winnerId = winnerId,
                        statusReason = if (isFinished) chessGame.status.name else "",
                        pgn = chessGame.toPgn()
                    )
                }
            }
        }
    }

    // Handle Square click
    fun handleSquareClick(pos: Position) {
        if (chessGame.status.isFinished()) return
        if (isBotThinking) return

        // In online mode, verify it's our turn and our piece color
        if (playMode == PlayMode.ONLINE_MULTIPLAYER) {
            val game = onlineGameData ?: return
            val myId = repository.currentUserId()
            val isMyWhite = myId == game.whitePlayerId
            val isMyTurn = (isMyWhite && chessGame.board.activeColor == PieceColor.WHITE) ||
                    (!isMyWhite && chessGame.board.activeColor == PieceColor.BLACK)
            if (!isMyTurn) return
        }

        // In VS_BOT mode, only allow moves on player's turn
        if (playMode == PlayMode.VS_BOT && chessGame.board.activeColor != playerColor) return

        val currentSelected = selectedPos
        if (currentSelected == null) {
            // Select piece
            val piece = chessGame.getPiece(pos)
            if (piece != null && piece.color == chessGame.board.activeColor) {
                selectedPos = pos
                legalMovesForSelected = chessGame.getLegalMovesForSquare(pos)
            }
        } else {
            // Check if clicking another friendly piece
            val targetPiece = chessGame.getPiece(pos)
            if (targetPiece != null && targetPiece.color == chessGame.board.activeColor) {
                selectedPos = pos
                legalMovesForSelected = chessGame.getLegalMovesForSquare(pos)
                return
            }

            // Attempt to move
            val matchedMove = legalMovesForSelected.find { it.to == pos }
            if (matchedMove != null) {
                val movingPiece = chessGame.getPiece(currentSelected)
                val isPawn = movingPiece?.type == PieceType.PAWN
                val promoRow = if (movingPiece?.color == PieceColor.WHITE) 0 else 7

                if (isPawn && pos.row == promoRow) {
                    // Show promotion dialog
                    pendingPromotionMove = matchedMove
                } else {
                    executeMove(matchedMove)
                }
            }

            selectedPos = null
            legalMovesForSelected = emptyList()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = when (playMode) {
                                PlayMode.VS_BOT -> "vs ${botDifficulty.label}"
                                PlayMode.PASS_AND_PLAY -> "Pass & Play (Local)"
                                PlayMode.ONLINE_MULTIPLAYER -> {
                                    val oppName = if (repository.currentUserId() == onlineGameData?.whitePlayerId)
                                        onlineGameData?.blackPlayerName ?: "Opponent"
                                    else
                                        onlineGameData?.whitePlayerName ?: "Opponent"
                                    "Online: vs $oppName"
                                }
                            },
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        val turnText = if (chessGame.status.isFinished()) {
                            when (chessGame.status) {
                                GameStatus.CHECKMATE -> "Checkmate! ${chessGame.winner?.name} wins!"
                                GameStatus.RESIGNED -> "${chessGame.resigner?.name} resigned. ${chessGame.winner?.name} wins!"
                                else -> "Draw (${chessGame.status.name})"
                            }
                        } else {
                            val turnName = if (chessGame.board.activeColor == PieceColor.WHITE) "White" else "Black"
                            val checkMsg = if (chessGame.isCheck()) " (Check!)" else ""
                            "$turnName to move$checkMsg"
                        }
                        Text(
                            text = turnText,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (chessGame.isCheck()) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // PGN Export
                    IconButton(
                        onClick = { showPgnDialog = true },
                        modifier = Modifier.testTag("pgn_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export PGN")
                    }

                    // Online Chat button
                    if (playMode == PlayMode.ONLINE_MULTIPLAYER) {
                        IconButton(
                            onClick = { showChatSheet = true },
                            modifier = Modifier.testTag("chat_button")
                        ) {
                            Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                Icon(imageVector = Icons.Default.Chat, contentDescription = "Game Chat")
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isSearchingMatch) {
            // Matchmaking queue search modal/screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Text(
                            text = "Searching for an opponent...",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Matching with players near your rating (${currentUserProfile.rating} ELO)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    repository.cancelMatchmaking()
                                    onBack()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Cancel Search")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Opponent Info Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val opponentLabel = when (playMode) {
                            PlayMode.VS_BOT -> "${botDifficulty.label} (${botDifficulty.eloEstimate})"
                            PlayMode.PASS_AND_PLAY -> "Player 2 (Black)"
                            PlayMode.ONLINE_MULTIPLAYER -> {
                                val isMeWhite = repository.currentUserId() == onlineGameData?.whitePlayerId
                                val name = if (isMeWhite) onlineGameData?.blackPlayerName else onlineGameData?.whitePlayerName
                                val rating = if (isMeWhite) onlineGameData?.blackPlayerRating else onlineGameData?.whitePlayerRating
                                "$name ($rating)"
                            }
                        }
                        Text(
                            text = opponentLabel ?: "Opponent",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (isBotThinking) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Thinking...", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                // Captured pieces top/bottom
                CapturedPiecesBar(
                    capturedByWhite = chessGame.capturedByWhite,
                    capturedByBlack = chessGame.capturedByBlack,
                    activeTurn = chessGame.board.activeColor
                )

                // Chess Board
                val perspective = if (playMode == PlayMode.ONLINE_MULTIPLAYER) {
                    if (repository.currentUserId() == onlineGameData?.blackPlayerId) PieceColor.BLACK else PieceColor.WHITE
                } else {
                    playerColor
                }

                ChessBoardView(
                    board = chessGame.board,
                    playerPerspective = perspective,
                    selectedPos = selectedPos,
                    legalMovesForSelected = legalMovesForSelected,
                    lastMove = chessGame.moveHistory.lastOrNull(),
                    isCheck = chessGame.isCheck(),
                    boardTheme = boardTheme,
                    onSquareClicked = { handleSquareClick(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                )

                // Move History scroll row
                MoveHistoryView(
                    moves = chessGame.moveHistory,
                    modifier = Modifier.fillMaxWidth()
                )

                // Hint banner if active
                if (hintText != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = hintText ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                // In-Game Action Bar (Undo, Hint, Resign, Draw, Themes)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (playMode == PlayMode.VS_BOT) {
                        // Undo Move button (undos both player and bot move)
                        FilledTonalButton(
                            onClick = {
                                chessGame.undoMove()
                                chessGame.undoMove()
                                chessGame = ChessGame(chessGame.board).apply {
                                    this@apply.moveHistory.addAll(chessGame.moveHistory)
                                    this@apply.capturedByWhite.addAll(chessGame.capturedByWhite)
                                    this@apply.capturedByBlack.addAll(chessGame.capturedByBlack)
                                }
                            },
                            enabled = chessGame.moveHistory.size >= 2 && !chessGame.status.isFinished(),
                            modifier = Modifier.testTag("undo_button")
                        ) {
                            Icon(imageVector = Icons.Default.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Undo", fontSize = 12.sp)
                        }

                        // Hint button
                        FilledTonalButton(
                            onClick = {
                                val eval = ChessEngine.getHint(chessGame)
                                hintText = eval.bestMove?.let { "Recommended: ${it.san} (${eval.rationale})" } ?: "No moves"
                            },
                            enabled = !chessGame.status.isFinished(),
                            modifier = Modifier.testTag("hint_button")
                        ) {
                            Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Hint", fontSize = 12.sp)
                        }
                    }

                    // Resign button
                    OutlinedButton(
                        onClick = {
                            val resColor = if (playMode == PlayMode.ONLINE_MULTIPLAYER) {
                                if (repository.currentUserId() == onlineGameData?.whitePlayerId) PieceColor.WHITE else PieceColor.BLACK
                            } else {
                                playerColor
                            }
                            chessGame.resign(resColor)
                            if (playMode == PlayMode.ONLINE_MULTIPLAYER && onlineGameId != null) {
                                coroutineScope.launch {
                                    repository.resignOnlineGame(onlineGameId!!, repository.requireUserId())
                                }
                            }
                        },
                        enabled = !chessGame.status.isFinished(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.testTag("resign_button")
                    ) {
                        Icon(imageVector = Icons.Default.Flag, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resign", fontSize = 12.sp)
                    }

                    // Board Theme switcher button
                    IconButton(
                        onClick = {
                            boardTheme = when (boardTheme) {
                                BoardColorScheme.TOURNAMENT_GREEN -> BoardColorScheme.CLASSIC_WOOD
                                BoardColorScheme.CLASSIC_WOOD -> BoardColorScheme.MIDNIGHT_SLATE
                                BoardColorScheme.MIDNIGHT_SLATE -> BoardColorScheme.TOURNAMENT_GREEN
                            }
                        },
                        modifier = Modifier.testTag("theme_toggle_button")
                    ) {
                        Icon(imageVector = Icons.Default.Palette, contentDescription = "Change Board Theme")
                    }
                }
            }
        }
    }

    // Promotion Dialog
    pendingPromotionMove?.let { promoMove ->
        PromotionDialog(
            color = promoMove.piece.color,
            onSelectPromotion = { chosenType ->
                val finalizedMove = promoMove.copy(promotion = chosenType)
                executeMove(finalizedMove)
                pendingPromotionMove = null
            },
            onDismiss = { pendingPromotionMove = null }
        )
    }

    // PGN Dialog
    if (showPgnDialog) {
        PgnExportDialog(
            pgnText = chessGame.toPgn(),
            onDismiss = { showPgnDialog = false }
        )
    }

    // In-game chat bottom sheet
    if (showChatSheet && onlineGameId != null) {
        ModalBottomSheet(onDismissRequest = { showChatSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(380.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "In-Game Chat",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (gameMessages.isEmpty()) {
                        item {
                            Text(
                                text = "Say 'Good luck!' or discuss the game.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(gameMessages) { msg ->
                            val isMe = msg.senderId == repository.currentUserId()
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                            ) {
                                Surface(
                                    color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
                                        Text(
                                            text = msg.senderName,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = msg.text,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
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
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Message...") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                coroutineScope.launch {
                                    repository.sendGameMessage(onlineGameId!!, currentUserProfile.displayName, chatInput.trim())
                                    chatInput = ""
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
