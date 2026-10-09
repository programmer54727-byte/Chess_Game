package com.example.chess.model

data class Move(
    val from: Position,
    val to: Position,
    val piece: Piece,
    val capturedPiece: Piece? = null,
    val promotion: PieceType? = null,
    val isCastling: Boolean = false,
    val isEnPassant: Boolean = false,
    var isCheck: Boolean = false,
    var isCheckmate: Boolean = false,
    var san: String = ""
) {
    val uci: String get() {
        val promoStr = promotion?.let {
            when (it) {
                PieceType.QUEEN -> "q"
                PieceType.ROOK -> "r"
                PieceType.BISHOP -> "b"
                PieceType.KNIGHT -> "n"
                else -> ""
            }
        } ?: ""
        return "${from.algebraic}${to.algebraic}$promoStr"
    }
}
