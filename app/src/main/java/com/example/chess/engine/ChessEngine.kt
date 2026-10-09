package com.example.chess.engine

import com.example.chess.model.*
import kotlin.random.Random

enum class EngineDifficulty(val label: String, val eloEstimate: Int, val depth: Int) {
    NOVICE("Novice (Bot)", 800, 1),
    INTERMEDIATE("Club Player (Bot)", 1350, 2),
    MASTER("Grandmaster (Bot)", 2100, 3)
}

data class EngineEvaluation(
    val score: Int, // in centipawns: positive means White advantage, negative means Black advantage
    val bestMove: Move?,
    val rationale: String = ""
)

object ChessEngine {

    // Piece-Square Tables (White perspective; flipped for Black)
    private val PAWN_TABLE = arrayOf(
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0),
        intArrayOf(50, 50, 50, 50, 50, 50, 50, 50),
        intArrayOf(10, 10, 20, 30, 30, 20, 10, 10),
        intArrayOf(5,  5, 10, 25, 25, 10,  5,  5),
        intArrayOf(0,  0,  0, 20, 20,  0,  0,  0),
        intArrayOf(5, -5,-10,  0,  0,-10, -5,  5),
        intArrayOf(5, 10, 10,-20,-20, 10, 10,  5),
        intArrayOf(0,  0,  0,  0,  0,  0,  0,  0)
    )

    private val KNIGHT_TABLE = arrayOf(
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50),
        intArrayOf(-40,-20,  0,  0,  0,  0,-20,-40),
        intArrayOf(-30,  0, 10, 15, 15, 10,  0,-30),
        intArrayOf(-30,  5, 15, 20, 20, 15,  5,-30),
        intArrayOf(-30,  0, 15, 20, 20, 15,  0,-30),
        intArrayOf(-30,  5, 10, 15, 15, 10,  5,-30),
        intArrayOf(-40,-20,  0,  5,  5,  0,-20,-40),
        intArrayOf(-50,-40,-30,-30,-30,-30,-40,-50)
    )

    private val BISHOP_TABLE = arrayOf(
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20),
        intArrayOf(-10,  0,  0,  0,  0,  0,  0,-10),
        intArrayOf(-10,  0,  5, 10, 10,  5,  0,-10),
        intArrayOf(-10,  5,  5, 10, 10,  5,  5,-10),
        intArrayOf(-10,  0, 10, 10, 10, 10,  0,-10),
        intArrayOf(-10, 10, 10, 10, 10, 10, 10,-10),
        intArrayOf(-10,  5,  0,  0,  0,  0,  5,-10),
        intArrayOf(-20,-10,-10,-10,-10,-10,-10,-20)
    )

    private val KING_TABLE = arrayOf(
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-30,-40,-40,-50,-50,-40,-40,-30),
        intArrayOf(-20,-30,-30,-40,-40,-30,-30,-20),
        intArrayOf(-10,-20,-20,-20,-20,-20,-20,-10),
        intArrayOf( 20, 20,  0,  0,  0,  0, 20, 20),
        intArrayOf( 20, 30, 10,  0,  0, 10, 30, 20)
    )

    fun evaluateBoard(board: Board): Int {
        var score = 0
        for (r in 0..7) {
            for (c in 0..7) {
                val piece = board.getPiece(r, c) ?: continue
                val baseVal = piece.type.value
                val posVal = getPieceSquareValue(piece, r, c)
                val total = baseVal + posVal

                if (piece.color == PieceColor.WHITE) {
                    score += total
                } else {
                    score -= total
                }
            }
        }
        return score
    }

    private fun getPieceSquareValue(piece: Piece, row: Int, col: Int): Int {
        val tableRow = if (piece.color == PieceColor.WHITE) row else 7 - row
        return when (piece.type) {
            PieceType.PAWN -> PAWN_TABLE[tableRow][col]
            PieceType.KNIGHT -> KNIGHT_TABLE[tableRow][col]
            PieceType.BISHOP -> BISHOP_TABLE[tableRow][col]
            PieceType.KING -> KING_TABLE[tableRow][col]
            else -> 0
        }
    }

    fun chooseMove(game: ChessGame, difficulty: EngineDifficulty): Move? {
        val legalMoves = game.generateLegalMoves()
        if (legalMoves.isEmpty()) return null

        if (difficulty == EngineDifficulty.NOVICE) {
            // 40% chance pick a capture if available, otherwise random
            val captures = legalMoves.filter { it.capturedPiece != null }
            return if (captures.isNotEmpty() && Random.nextFloat() < 0.6f) {
                captures.random()
            } else {
                legalMoves.random()
            }
        }

        val isWhite = game.activeColor() == PieceColor.WHITE
        var bestScore = if (isWhite) Int.MIN_VALUE else Int.MAX_VALUE
        var bestMoves = mutableListOf<Move>()

        // Order moves (captures first)
        val orderedMoves = legalMoves.sortedByDescending {
            val victimVal = it.capturedPiece?.type?.value ?: 0
            val attackerVal = it.piece.type.value
            victimVal - attackerVal / 10
        }

        val depth = difficulty.depth
        var alpha = -1000000
        var beta = 1000000

        for (move in orderedMoves) {
            val testGame = ChessGame(game.board.copy())
            testGame.makeMove(move)

            val score = minimax(testGame, depth - 1, alpha, beta, !isWhite)

            if (isWhite) {
                if (score > bestScore) {
                    bestScore = score
                    bestMoves = mutableListOf(move)
                    alpha = maxOf(alpha, score)
                } else if (score == bestScore) {
                    bestMoves.add(move)
                }
            } else {
                if (score < bestScore) {
                    bestScore = score
                    bestMoves = mutableListOf(move)
                    beta = minOf(beta, score)
                } else if (score == bestScore) {
                    bestMoves.add(move)
                }
            }
        }

        return bestMoves.randomOrNull() ?: legalMoves.first()
    }

    private fun minimax(game: ChessGame, depth: Int, alpha: Int, beta: Int, isMaximizing: Boolean): Int {
        if (game.status == GameStatus.CHECKMATE) {
            return if (game.winner == PieceColor.WHITE) 99999 + depth else -99999 - depth
        }
        if (game.status.isFinished()) return 0
        if (depth == 0) return evaluateBoard(game.board)

        val legalMoves = game.generateLegalMoves()
        var currentAlpha = alpha
        var currentBeta = beta

        if (isMaximizing) {
            var maxEval = -1000000
            for (move in legalMoves) {
                val testGame = ChessGame(game.board.copy())
                testGame.makeMove(move)
                val eval = minimax(testGame, depth - 1, currentAlpha, currentBeta, false)
                maxEval = maxOf(maxEval, eval)
                currentAlpha = maxOf(currentAlpha, eval)
                if (currentBeta <= currentAlpha) break
            }
            return maxEval
        } else {
            var minEval = 1000000
            for (move in legalMoves) {
                val testGame = ChessGame(game.board.copy())
                testGame.makeMove(move)
                val eval = minimax(testGame, depth - 1, currentAlpha, currentBeta, true)
                minEval = minOf(minEval, eval)
                currentBeta = minOf(currentBeta, eval)
                if (currentBeta <= currentAlpha) break
            }
            return minEval
        }
    }

    fun getHint(game: ChessGame): EngineEvaluation {
        val bestMove = chooseMove(game, EngineDifficulty.MASTER)
        val score = evaluateBoard(game.board)
        val rationale = when {
            bestMove == null -> "No legal moves remaining."
            bestMove.isCheckmate -> "Checkmate! Immediate winning strike."
            bestMove.capturedPiece != null -> "Wins material by capturing ${bestMove.capturedPiece.type.name.lowercase()}."
            bestMove.isCastling -> "Castles to safety and connects rooks."
            bestMove.piece.type == PieceType.PAWN -> "Advances pawn to secure central space."
            bestMove.piece.type == PieceType.KNIGHT -> "Controls vital central squares."
            else -> "Improves piece activity and tactical positioning."
        }
        return EngineEvaluation(score, bestMove, rationale)
    }
}
