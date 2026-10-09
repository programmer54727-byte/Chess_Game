package com.example.chess.model

data class Position(
    val row: Int, // 0 = rank 8, 7 = rank 1
    val col: Int  // 0 = file a, 7 = file h
) {
    init {
        require(row in 0..7 && col in 0..7) { "Invalid board position: ($row, $col)" }
    }

    val fileChar: Char get() = ('a' + col)
    val rankChar: Char get() = ('8' - row)
    val algebraic: String get() = "$fileChar$rankChar"

    fun isLightSquare(): Boolean = (row + col) % 2 == 0

    companion object {
        fun fromAlgebraic(alg: String): Position? {
            if (alg.length != 2) return null
            val file = alg[0].lowercaseChar()
            val rank = alg[1]
            if (file !in 'a'..'h' || rank !in '1'..'8') return null
            val col = file - 'a'
            val row = '8' - rank
            return Position(row, col)
        }
    }
}
