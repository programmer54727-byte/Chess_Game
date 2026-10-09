package com.example.chess.puzzle

import com.example.chess.model.PieceColor
import java.util.Calendar

data class ChessPuzzle(
    val id: String,
    val title: String,
    val theme: String, // "Mate in 1", "Mate in 2", "Knight Fork", "Queen Sacrifice", "Pin", "Back-Rank Mate"
    val difficulty: String, // "Easy", "Medium", "Hard"
    val rating: Int,
    val fen: String,
    val playerColor: PieceColor,
    val solutionMoves: List<String>, // List of UCI moves e.g. ["e7e8q", "f8g8", ...] or algebraic
    val explanation: String
)

object PuzzleRepository {

    val PUZZLES = listOf(
        ChessPuzzle(
            id = "puz_001",
            title = "Scholar's Nemesis",
            theme = "Mate in 1",
            difficulty = "Easy",
            rating = 800,
            fen = "r1bqkb1r/pppp1ppp/2n5/4p3/2B1n3/5Q2/PPPP1PPP/RNB1K1NR w KQkq - 0 5",
            playerColor = PieceColor.WHITE,
            solutionMoves = listOf("f3f7"),
            explanation = "Qxf7# delivers an unstoppable checkmate on the weak f7 square!"
        ),
        ChessPuzzle(
            id = "puz_002",
            title = "Smothered Mate",
            theme = "Mate in 2",
            difficulty = "Medium",
            rating = 1450,
            fen = "6k1/5ppp/8/8/8/8/1Q4PP/6NK w - - 0 1",
            playerColor = PieceColor.WHITE,
            solutionMoves = listOf("b2b8"),
            explanation = "Queen to b8 delivers back rank mate as the king has no escape."
        ),
        ChessPuzzle(
            id = "puz_003",
            title = "Royal Fork Strike",
            theme = "Knight Fork",
            difficulty = "Easy",
            rating = 1100,
            fen = "r3k2r/pppq1ppp/2n5/4p3/2B5/3P1N2/PPP2PPP/R2QK2R w KQkq - 0 10",
            playerColor = PieceColor.WHITE,
            solutionMoves = listOf("c4f7"),
            explanation = "Bxf7+ strips the castling rights and disrupts Black's coordination."
        ),
        ChessPuzzle(
            id = "puz_004",
            title = "Back-Rank Decapitation",
            theme = "Back-Rank Mate",
            difficulty = "Medium",
            rating = 1300,
            fen = "3r2k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1",
            playerColor = PieceColor.WHITE,
            solutionMoves = listOf("d1d8"),
            explanation = "Rxd8# exploits the trapped back rank pawn shield."
        ),
        ChessPuzzle(
            id = "puz_005",
            title = "Opera House Deflection",
            theme = "Mate in 2",
            difficulty = "Hard",
            rating = 1750,
            fen = "4kb1r/p2rqppp/5n2/1B2p1B1/4P3/1Q6/PPP2PPP/2KR4 w k - 0 14",
            playerColor = PieceColor.WHITE,
            solutionMoves = listOf("b5d7", "f6d7", "b3b8"),
            explanation = "Morphy's tactical masterclass pinning the defense and overpowering the files."
        ),
        ChessPuzzle(
            id = "puz_006",
            title = "Queen Sac on h7",
            theme = "Queen Sacrifice",
            difficulty = "Hard",
            rating = 1850,
            fen = "r1b2rk1/pp1nbppp/1q2p3/2ppP3/3P1N2/2PB1N2/PP3PPP/R2Q1RK1 w - - 0 12",
            playerColor = PieceColor.WHITE,
            solutionMoves = listOf("d3h7", "g8h7", "f3g5"),
            explanation = "Classic Greek Gift sacrifice opening the king's shelter for devastating knight check."
        ),
        ChessPuzzle(
            id = "puz_007",
            title = "Anand's Central Pin",
            theme = "Pin",
            difficulty = "Medium",
            rating = 1400,
            fen = "r1b1k2r/ppp2ppp/2n5/3qp3/1b1P4/2N2N2/PPP2PPP/R1BQKB1R w KQkq - 0 7",
            playerColor = PieceColor.WHITE,
            solutionMoves = listOf("c1d2"),
            explanation = "Bd2 breaks the pin on the knight, protecting the king and preparing attacks."
        ),
        ChessPuzzle(
            id = "puz_008",
            title = "Anastasia's Mate",
            theme = "Mate in 2",
            difficulty = "Hard",
            rating = 1900,
            fen = "5rk1/1p3ppp/8/1N6/8/8/1Q3PPP/R5K1 w - - 0 1",
            playerColor = PieceColor.WHITE,
            solutionMoves = listOf("b5d6"),
            explanation = "Knight leap restricts key escape squares."
        )
    )

    fun getDailyPuzzle(): ChessPuzzle {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR)
        val index = dayOfYear % PUZZLES.size
        return PUZZLES[index]
    }
}
