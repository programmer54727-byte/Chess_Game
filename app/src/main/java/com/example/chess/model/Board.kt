package com.example.chess.model

data class CastlingRights(
    val whiteKingside: Boolean = true,
    val whiteQueenside: Boolean = true,
    val blackKingside: Boolean = true,
    val blackQueenside: Boolean = true
) {
    fun toFenString(): String {
        val sb = StringBuilder()
        if (whiteKingside) sb.append('K')
        if (whiteQueenside) sb.append('Q')
        if (blackKingside) sb.append('k')
        if (blackQueenside) sb.append('q')
        return if (sb.isEmpty()) "-" else sb.toString()
    }

    companion object {
        fun fromFenString(s: String): CastlingRights {
            return CastlingRights(
                whiteKingside = s.contains('K'),
                whiteQueenside = s.contains('Q'),
                blackKingside = s.contains('k'),
                blackQueenside = s.contains('q')
            )
        }
    }
}

class Board(
    private val grid: Array<Array<Piece?>> = Array(8) { Array(8) { null } },
    var activeColor: PieceColor = PieceColor.WHITE,
    var castlingRights: CastlingRights = CastlingRights(),
    var enPassantTarget: Position? = null,
    var halfmoveClock: Int = 0,
    var fullmoveNumber: Int = 1
) {

    fun getPiece(pos: Position): Piece? = grid[pos.row][pos.col]
    fun getPiece(row: Int, col: Int): Piece? = grid[row][col]

    fun setPiece(pos: Position, piece: Piece?) {
        grid[pos.row][pos.col] = piece
    }

    fun setPiece(row: Int, col: Int, piece: Piece?) {
        grid[row][col] = piece
    }

    fun copy(): Board {
        val newGrid = Array(8) { r ->
            Array(8) { c -> grid[r][c] }
        }
        return Board(
            grid = newGrid,
            activeColor = activeColor,
            castlingRights = castlingRights.copy(),
            enPassantTarget = enPassantTarget,
            halfmoveClock = halfmoveClock,
            fullmoveNumber = fullmoveNumber
        )
    }

    fun findKing(color: PieceColor): Position? {
        for (r in 0..7) {
            for (c in 0..7) {
                val p = grid[r][c]
                if (p?.type == PieceType.KING && p.color == color) {
                    return Position(r, c)
                }
            }
        }
        return null
    }

    fun isSquareAttacked(target: Position, byColor: PieceColor): Boolean {
        // Pawns
        val pawnRow = if (byColor == PieceColor.WHITE) target.row + 1 else target.row - 1
        if (pawnRow in 0..7) {
            for (dc in listOf(-1, 1)) {
                val c = target.col + dc
                if (c in 0..7) {
                    val p = grid[pawnRow][c]
                    if (p?.type == PieceType.PAWN && p.color == byColor) return true
                }
            }
        }

        // Knights
        val knightDeltas = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((dr, dc) in knightDeltas) {
            val r = target.row + dr
            val c = target.col + dc
            if (r in 0..7 && c in 0..7) {
                val p = grid[r][c]
                if (p?.type == PieceType.KNIGHT && p.color == byColor) return true
            }
        }

        // Kings
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = target.row + dr
                val c = target.col + dc
                if (r in 0..7 && c in 0..7) {
                    val p = grid[r][c]
                    if (p?.type == PieceType.KING && p.color == byColor) return true
                }
            }
        }

        // Straight lines (Rook / Queen)
        val straightDirs = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)
        for ((dr, dc) in straightDirs) {
            var r = target.row + dr
            var c = target.col + dc
            while (r in 0..7 && c in 0..7) {
                val p = grid[r][c]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        // Diagonals (Bishop / Queen)
        val diagDirs = listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1)
        for ((dr, dc) in diagDirs) {
            var r = target.row + dr
            var c = target.col + dc
            while (r in 0..7 && c in 0..7) {
                val p = grid[r][c]
                if (p != null) {
                    if (p.color == byColor && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
                        return true
                    }
                    break
                }
                r += dr
                c += dc
            }
        }

        return false
    }

    fun isCheck(color: PieceColor): Boolean {
        val kingPos = findKing(color) ?: return false
        return isSquareAttacked(kingPos, color.opposite())
    }

    fun toFen(): String {
        val sb = StringBuilder()
        for (r in 0..7) {
            var empty = 0
            for (c in 0..7) {
                val p = grid[r][c]
                if (p == null) {
                    empty++
                } else {
                    if (empty > 0) {
                        sb.append(empty)
                        empty = 0
                    }
                    sb.append(p.toFenChar())
                }
            }
            if (empty > 0) sb.append(empty)
            if (r < 7) sb.append('/')
        }

        sb.append(' ')
        sb.append(if (activeColor == PieceColor.WHITE) 'w' else 'b')
        sb.append(' ')
        sb.append(castlingRights.toFenString())
        sb.append(' ')
        sb.append(enPassantTarget?.algebraic ?: "-")
        sb.append(' ')
        sb.append(halfmoveClock)
        sb.append(' ')
        sb.append(fullmoveNumber)

        return sb.toString()
    }

    companion object {
        const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"

        fun fromFen(fen: String = STARTING_FEN): Board {
            val parts = fen.trim().split("\\s+".toRegex())
            val rows = parts[0].split('/')
            val grid = Array(8) { Array<Piece?>(8) { null } }

            for (r in 0..7) {
                var c = 0
                for (ch in rows[r]) {
                    if (ch.isDigit()) {
                        c += ch.digitToInt()
                    } else {
                        grid[r][c] = Piece.fromFenChar(ch)
                        c++
                    }
                }
            }

            val activeColor = if (parts.size > 1 && parts[1] == "b") PieceColor.BLACK else PieceColor.WHITE
            val castlingRights = if (parts.size > 2) CastlingRights.fromFenString(parts[2]) else CastlingRights()
            val enPassant = if (parts.size > 3 && parts[3] != "-") Position.fromAlgebraic(parts[3]) else null
            val halfmove = if (parts.size > 4) parts[4].toIntOrNull() ?: 0 else 0
            val fullmove = if (parts.size > 5) parts[5].toIntOrNull() ?: 1 else 1

            return Board(grid, activeColor, castlingRights, enPassant, halfmove, fullmove)
        }
    }
}
