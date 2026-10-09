package com.example.chess.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.model.*

@Composable
fun CapturedPiecesBar(
    capturedByWhite: List<Piece>,
    capturedByBlack: List<Piece>,
    activeTurn: PieceColor,
    modifier: Modifier = Modifier
) {
    val whiteVal = capturedByWhite.sumOf { it.type.value }
    val blackVal = capturedByBlack.sumOf { it.type.value }
    val advantage = (whiteVal - blackVal) / 100

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // White captures (Black pieces lost)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "⚪ ",
                    fontSize = 12.sp
                )
                capturedByWhite.forEach { piece ->
                    Text(
                        text = piece.type.unicode(PieceColor.BLACK),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (advantage > 0) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+$advantage",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }

            // Black captures (White pieces lost)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (advantage < 0) {
                    Text(
                        text = "+${-advantage}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                capturedByBlack.forEach { piece ->
                    Text(
                        text = piece.type.unicode(PieceColor.WHITE),
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = " ⚫",
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun MoveHistoryView(
    moves: List<Move>,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(moves.size) {
        if (moves.isNotEmpty()) {
            listState.animateScrollToItem((moves.size - 1) / 2)
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        if (moves.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Move history will appear here (e.g. 1. e4 e5)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val turnPairs = (moves.indices step 2).toList()
                itemsIndexed(turnPairs) { pairIdx, startIdx ->
                    val moveNum = pairIdx + 1
                    val whiteMove = moves[startIdx]
                    val blackMove = if (startIdx + 1 < moves.size) moves[startIdx + 1] else null

                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$moveNum.",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = whiteMove.san,
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (blackMove != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = blackMove.san,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PromotionDialog(
    color: PieceColor,
    onSelectPromotion: (PieceType) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Promote Pawn",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { type ->
                    Card(
                        onClick = { onSelectPromotion(type) },
                        modifier = Modifier
                            .size(64.dp)
                            .testTag("promote_${type.name.lowercase()}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = type.unicode(color),
                                fontSize = 32.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

@Composable
fun PgnExportDialog(
    pgnText: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "PGN Export & Analysis",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Portable Game Notation (PGN) can be imported into chess analysis tools like Lichess or Chess.com.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 220.dp)
                ) {
                    Text(
                        text = pgnText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Share button
                FilledTonalButton(
                    onClick = {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, "Chess Arena Match PGN")
                            putExtra(Intent.EXTRA_TEXT, pgnText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share PGN via"))
                    },
                    modifier = Modifier.testTag("share_pgn_button")
                ) {
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }

                // Copy button
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Chess Match PGN", pgnText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "PGN copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.testTag("copy_pgn_button")
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
