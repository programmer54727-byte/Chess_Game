package com.example.chess.model

enum class PieceColor {
    WHITE, BLACK;

    fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE
}

enum class PieceType(val symbol: String, val value: Int) {
    PAWN("P", 100),
    KNIGHT("N", 320),
    BISHOP("B", 330),
    ROOK("R", 500),
    QUEEN("Q", 900),
    KING("K", 20000);

    fun unicode(color: PieceColor): String = when (color) {
        PieceColor.WHITE -> when (this) {
            PAWN -> "♙"
            KNIGHT -> "♘"
            BISHOP -> "♗"
            ROOK -> "♖"
            QUEEN -> "♕"
            KING -> "♔"
        }
        PieceColor.BLACK -> when (this) {
            PAWN -> "♟"
            KNIGHT -> "♞"
            BISHOP -> "♝"
            ROOK -> "♜"
            QUEEN -> "♛"
            KING -> "♚"
        }
    }
}

data class Piece(
    val type: PieceType,
    val color: PieceColor
) {
    fun toFenChar(): Char {
        val c = when (type) {
            PieceType.PAWN -> 'p'
            PieceType.KNIGHT -> 'n'
            PieceType.BISHOP -> 'b'
            PieceType.ROOK -> 'r'
            PieceType.QUEEN -> 'q'
            PieceType.KING -> 'k'
        }
        return if (color == PieceColor.WHITE) c.uppercaseChar() else c
    }

    companion object {
        fun fromFenChar(c: Char): Piece? {
            val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
            val type = when (c.lowercaseChar()) {
                'p' -> PieceType.PAWN
                'n' -> PieceType.KNIGHT
                'b' -> PieceType.BISHOP
                'r' -> PieceType.ROOK
                'q' -> PieceType.QUEEN
                'k' -> PieceType.KING
                else -> return null
            }
            return Piece(type, color)
        }
    }
}
