package com.example.chess.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.*

enum class BoardColorScheme(
    val lightSquare: Color,
    val darkSquare: Color,
    val highlightColor: Color,
    val checkColor: Color
) {
    CLASSIC_WOOD(
        lightSquare = Color(0xFFF0D9B5),
        darkSquare = Color(0xFFB58863),
        highlightColor = Color(0x66F6E05E),
        checkColor = Color(0x88E53E3E)
    ),
    TOURNAMENT_GREEN(
        lightSquare = Color(0xFFEEEED2),
        darkSquare = Color(0xFF769656),
        highlightColor = Color(0x66ECC94B),
        checkColor = Color(0x88E53E3E)
    ),
    MIDNIGHT_SLATE(
        lightSquare = Color(0xFFCBD5E1),
        darkSquare = Color(0xFF475569),
        highlightColor = Color(0x6638BDF8),
        checkColor = Color(0x88EF4444)
    )
}

@Composable
fun ChessBoardView(
    board: Board,
    playerPerspective: PieceColor = PieceColor.WHITE,
    selectedPos: Position? = null,
    legalMovesForSelected: List<Move> = emptyList(),
    lastMove: Move? = null,
    isCheck: Boolean = false,
    boardTheme: BoardColorScheme = BoardColorScheme.TOURNAMENT_GREEN,
    onSquareClicked: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFlipped = playerPerspective == PieceColor.BLACK

    val kingInCheckPos = if (isCheck) board.findKing(board.activeColor) else null

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .shadow(8.dp, RoundedCornerShape(12.dp))
            .background(Color(0xFF1E293B))
            .padding(6.dp)
            .testTag("chess_board")
    ) {
        val squareSize = maxWidth / 8

        Column(modifier = Modifier.fillMaxSize()) {
            for (displayRow in 0..7) {
                val r = if (isFlipped) 7 - displayRow else displayRow

                Row(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    for (displayCol in 0..7) {
                        val c = if (isFlipped) 7 - displayCol else displayCol
                        val pos = Position(r, c)
                        val piece = board.getPiece(pos)

                        val isLight = pos.isLightSquare()
                        val isSelected = selectedPos == pos
                        val isLastMove = lastMove?.from == pos || lastMove?.to == pos
                        val isKingInCheck = kingInCheckPos == pos
                        val isDestination = legalMovesForSelected.any { it.to == pos }
                        val isCaptureDestination = legalMovesForSelected.any { it.to == pos && it.capturedPiece != null }

                        // Base square color
                        val baseColor = if (isLight) boardTheme.lightSquare else boardTheme.darkSquare
                        val squareBg = when {
                            isKingInCheck -> boardTheme.checkColor
                            isSelected -> Color(0x99D97706) // Rich amber selection
                            isLastMove -> boardTheme.highlightColor
                            else -> baseColor
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(squareBg)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onSquareClicked(pos)
                                }
                                .testTag("square_${pos.algebraic}"),
                            contentAlignment = Alignment.Center
                        ) {
                            // File/Rank labels on edges
                            val showRankLabel = displayCol == 0
                            val showFileLabel = displayRow == 7

                            if (showRankLabel) {
                                Text(
                                    text = pos.rankChar.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLight) boardTheme.darkSquare else boardTheme.lightSquare,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(start = 2.dp, top = 1.dp)
                                )
                            }
                            if (showFileLabel) {
                                Text(
                                    text = pos.fileChar.toString(),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLight) boardTheme.darkSquare else boardTheme.lightSquare,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 2.dp, bottom = 1.dp)
                                )
                            }

                            // Piece rendering
                            if (piece != null) {
                                PieceView(piece = piece)
                            }

                            // Move destination indicator
                            if (isDestination) {
                                if (isCaptureDestination || piece != null) {
                                    // Hollow ring around enemy piece
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize(0.85f)
                                            .border(3.dp, Color(0x99D97706), CircleShape)
                                    )
                                } else {
                                    // Solid dot in empty square
                                    Box(
                                        modifier = Modifier
                                            .size(14.dp)
                                            .background(Color(0x660F172A), CircleShape)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PieceView(piece: Piece) {
    val isWhite = piece.color == PieceColor.WHITE

    val primaryColor = if (isWhite) Color(0xFFFFFFFF) else Color(0xFF1E293B)
    val shadowColor = if (isWhite) Color(0xFF475569) else Color(0xFFF1F5F9)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        // Shadow layer for extra crispness
        Text(
            text = piece.type.unicode(piece.color),
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = shadowColor.copy(alpha = 0.4f),
            modifier = Modifier.offset(x = 1.dp, y = 1.5.dp)
        )
        // Main piece symbol
        Text(
            text = piece.type.unicode(piece.color),
            fontSize = 38.sp,
            fontWeight = FontWeight.Bold,
            color = primaryColor
        )
    }
}
