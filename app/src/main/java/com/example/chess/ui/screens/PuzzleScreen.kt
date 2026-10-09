package com.example.chess.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.chess.model.*
import com.example.chess.puzzle.ChessPuzzle
import com.example.chess.puzzle.PuzzleRepository
import com.example.chess.ui.components.BoardColorScheme
import com.example.chess.ui.components.ChessBoardView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuzzleScreen(
    isDailyMode: Boolean,
    repository: ChessRepository,
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    BackHandler {
        onBack()
    }

    var selectedPuzzle by remember {
        mutableStateOf(if (isDailyMode) PuzzleRepository.getDailyPuzzle() else PuzzleRepository.PUZZLES.first())
    }

    var chessGame by remember { mutableStateOf(ChessGame(Board.fromFen(selectedPuzzle.fen))) }
    var selectedPos by remember { mutableStateOf<Position?>(null) }
    var legalMovesForSelected by remember { mutableStateOf<List<Move>>(emptyList()) }
    var currentMoveIndex by remember { mutableStateOf(0) }
    var isSolved by remember { mutableStateOf(false) }
    var isFailed by remember { mutableStateOf(false) }
    var showSolution by remember { mutableStateOf(false) }

    fun resetPuzzle(puzzle: ChessPuzzle) {
        selectedPuzzle = puzzle
        chessGame = ChessGame(Board.fromFen(puzzle.fen))
        selectedPos = null
        legalMovesForSelected = emptyList()
        currentMoveIndex = 0
        isSolved = false
        isFailed = false
        showSolution = false
    }

    fun handleSquareClick(pos: Position) {
        if (isSolved || showSolution) return

        val currentSelected = selectedPos
        if (currentSelected == null) {
            val piece = chessGame.getPiece(pos)
            if (piece != null && piece.color == selectedPuzzle.playerColor) {
                selectedPos = pos
                legalMovesForSelected = chessGame.getLegalMovesForSquare(pos)
            }
        } else {
            val targetPiece = chessGame.getPiece(pos)
            if (targetPiece != null && targetPiece.color == selectedPuzzle.playerColor) {
                selectedPos = pos
                legalMovesForSelected = chessGame.getLegalMovesForSquare(pos)
                return
            }

            val matchedMove = legalMovesForSelected.find { it.to == pos }
            if (matchedMove != null) {
                val expectedUci = selectedPuzzle.solutionMoves.getOrNull(currentMoveIndex)
                if (matchedMove.uci.startsWith(expectedUci ?: "")) {
                    // Correct move!
                    chessGame.makeMove(matchedMove)
                    chessGame = ChessGame(chessGame.board)
                    currentMoveIndex++

                    if (currentMoveIndex >= selectedPuzzle.solutionMoves.size) {
                        isSolved = true
                        isFailed = false
                        coroutineScope.launch {
                            repository.recordPuzzleSolved(isDailyChallenge = isDailyMode)
                        }
                    } else {
                        // Play opponent reply if puzzle has multiple moves
                        coroutineScope.launch {
                            delay(500)
                            val replyUci = selectedPuzzle.solutionMoves.getOrNull(currentMoveIndex)
                            if (replyUci != null) {
                                val replyMoves = chessGame.generateLegalMoves()
                                val replyMove = replyMoves.find { it.uci.startsWith(replyUci) }
                                if (replyMove != null) {
                                    chessGame.makeMove(replyMove)
                                    chessGame = ChessGame(chessGame.board)
                                    currentMoveIndex++
                                }
                            }
                        }
                    }
                } else {
                    // Incorrect move!
                    isFailed = true
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
                            text = if (isDailyMode) "⭐ Daily Challenge" else "Tactics Trainer",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${selectedPuzzle.theme} • Rating ${selectedPuzzle.rating}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("puzzle_back_button")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Puzzle selector row (if not daily mode)
            if (!isDailyMode) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(PuzzleRepository.PUZZLES) { p ->
                        FilterChip(
                            selected = p.id == selectedPuzzle.id,
                            onClick = { resetPuzzle(p) },
                            label = { Text(p.theme) }
                        )
                    }
                }
            }

            // Objective prompt card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        isSolved -> Color(0xFF065F46).copy(alpha = 0.2f)
                        isFailed -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                isSolved -> "🎉 Solved! Outstanding tactical vision!"
                                isFailed -> "❌ Incorrect move. Find the best tactic!"
                                else -> "${if (selectedPuzzle.playerColor == PieceColor.WHITE) "White" else "Black"} to move and win"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = when {
                                isSolved -> Color(0xFF10B981)
                                isFailed -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.onSurface
                            }
                        )
                        if (isSolved || showSolution) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = selectedPuzzle.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isSolved) {
                        Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(28.dp))
                    }
                }
            }

            // Chess Board
            ChessBoardView(
                board = chessGame.board,
                playerPerspective = selectedPuzzle.playerColor,
                selectedPos = selectedPos,
                legalMovesForSelected = legalMovesForSelected,
                lastMove = chessGame.moveHistory.lastOrNull(),
                isCheck = chessGame.isCheck(),
                boardTheme = BoardColorScheme.CLASSIC_WOOD,
                onSquareClicked = { handleSquareClick(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
            )

            // Bottom action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { resetPuzzle(selectedPuzzle) },
                    modifier = Modifier.testTag("retry_puzzle_button")
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restart")
                }

                FilledTonalButton(
                    onClick = { showSolution = true },
                    modifier = Modifier.testTag("solution_puzzle_button")
                ) {
                    Icon(imageVector = Icons.Default.Lightbulb, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Solution")
                }

                if (!isDailyMode) {
                    Button(
                        onClick = {
                            val currIdx = PuzzleRepository.PUZZLES.indexOfFirst { it.id == selectedPuzzle.id }
                            val nextIdx = (currIdx + 1) % PuzzleRepository.PUZZLES.size
                            resetPuzzle(PuzzleRepository.PUZZLES[nextIdx])
                        },
                        modifier = Modifier.testTag("next_puzzle_button")
                    ) {
                        Text("Next Puzzle")
                    }
                }
            }
        }
    }
}
