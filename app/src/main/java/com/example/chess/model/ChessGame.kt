package com.example.chess.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class GameStatus {
    IN_PROGRESS,
    CHECKMATE,
    STALEMATE,
    DRAW_INSUFFICIENT_MATERIAL,
    DRAW_FIFTY_MOVES,
    DRAW_AGREED,
    RESIGNED;

    fun isFinished(): Boolean = this != IN_PROGRESS
}

data class MoveRecord(
    val move: Move,
    val previousBoardState: Board,
    val capturedPiece: Piece?
)

class ChessGame(
    var board: Board = Board.fromFen()
) {
    val moveHistory = mutableListOf<Move>()
    private val stateHistory = mutableListOf<MoveRecord>()
    val capturedByWhite = mutableListOf<Piece>()
    val capturedByBlack = mutableListOf<Piece>()

    var status: GameStatus = GameStatus.IN_PROGRESS
    var winner: PieceColor? = null
    var resigner: PieceColor? = null

    // PGN Metadata
    var whitePlayerName: String = "White"
    var blackPlayerName: String = "Black"
    var eventName: String = "Casual Match"
    var roundName: String = "1"

    init {
        updateGameStatus()
    }

    fun activeColor(): PieceColor = board.activeColor

    fun isCheck(): Boolean = board.isCheck(board.activeColor)

    fun getPiece(pos: Position): Piece? = board.getPiece(pos)

    fun getLegalMovesForSquare(pos: Position): List<Move> {
        val piece = board.getPiece(pos) ?: return emptyList()
        if (piece.color != board.activeColor) return emptyList()
        return generateLegalMoves().filter { it.from == pos }
    }

    fun generateLegalMoves(): List<Move> {
        if (status.isFinished()) return emptyList()
        val pseudoMoves = generatePseudoLegalMoves(board.activeColor)
        val legalMoves = mutableListOf<Move>()

        for (move in pseudoMoves) {
            val testBoard = board.copy()
            applyMoveToBoard(testBoard, move)
            if (!testBoard.isCheck(board.activeColor)) {
                legalMoves.add(move)
            }
        }
        return legalMoves
    }

    private fun generatePseudoLegalMoves(color: PieceColor): List<Move> {
        val moves = mutableListOf<Move>()
        for (r in 0..7) {
            for (c in 0..7) {
                val pos = Position(r, c)
                val piece = board.getPiece(pos) ?: continue
                if (piece.color != color) continue

                when (piece.type) {
                    PieceType.PAWN -> generatePawnMoves(pos, piece, moves)
                    PieceType.KNIGHT -> generateKnightMoves(pos, piece, moves)
                    PieceType.BISHOP -> generateSlidingMoves(pos, piece, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), moves)
                    PieceType.ROOK -> generateSlidingMoves(pos, piece, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), moves)
                    PieceType.QUEEN -> {
                        generateSlidingMoves(pos, piece, listOf(-1 to -1, -1 to 1, 1 to -1, 1 to 1), moves)
                        generateSlidingMoves(pos, piece, listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1), moves)
                    }
                    PieceType.KING -> generateKingMoves(pos, piece, moves)
                }
            }
        }
        return moves
    }

    private fun generatePawnMoves(from: Position, piece: Piece, moves: MutableList<Move>) {
        val dir = if (piece.color == PieceColor.WHITE) -1 else 1
        val startRow = if (piece.color == PieceColor.WHITE) 6 else 1
        val promoRow = if (piece.color == PieceColor.WHITE) 0 else 7

        // 1 step forward
        val forward1Row = from.row + dir
        if (forward1Row in 0..7 && board.getPiece(forward1Row, from.col) == null) {
            val toPos = Position(forward1Row, from.col)
            if (forward1Row == promoRow) {
                listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { promo ->
                    moves.add(Move(from, toPos, piece, promotion = promo))
                }
            } else {
                moves.add(Move(from, toPos, piece))
            }

            // 2 steps forward
            val forward2Row = from.row + 2 * dir
            if (from.row == startRow && board.getPiece(forward2Row, from.col) == null) {
                moves.add(Move(from, Position(forward2Row, from.col), piece))
            }
        }

        // Captures
        for (dc in listOf(-1, 1)) {
            val targetCol = from.col + dc
            val targetRow = from.row + dir
            if (targetRow in 0..7 && targetCol in 0..7) {
                val targetPos = Position(targetRow, targetCol)
                val targetPiece = board.getPiece(targetPos)

                if (targetPiece != null && targetPiece.color != piece.color) {
                    if (targetRow == promoRow) {
                        listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT).forEach { promo ->
                            moves.add(Move(from, targetPos, piece, capturedPiece = targetPiece, promotion = promo))
                        }
                    } else {
                        moves.add(Move(from, targetPos, piece, capturedPiece = targetPiece))
                    }
                } else if (targetPiece == null && board.enPassantTarget == targetPos) {
                    // En Passant capture
                    val victimRow = from.row
                    val victimPiece = board.getPiece(victimRow, targetCol)
                    if (victimPiece != null && victimPiece.color != piece.color) {
                        moves.add(Move(from, targetPos, piece, capturedPiece = victimPiece, isEnPassant = true))
                    }
                }
            }
        }
    }

    private fun generateKnightMoves(from: Position, piece: Piece, moves: MutableList<Move>) {
        val deltas = listOf(
            -2 to -1, -2 to 1, -1 to -2, -1 to 2,
            1 to -2, 1 to 2, 2 to -1, 2 to 1
        )
        for ((dr, dc) in deltas) {
            val r = from.row + dr
            val c = from.col + dc
            if (r in 0..7 && c in 0..7) {
                val targetPos = Position(r, c)
                val targetPiece = board.getPiece(targetPos)
                if (targetPiece == null) {
                    moves.add(Move(from, targetPos, piece))
                } else if (targetPiece.color != piece.color) {
                    moves.add(Move(from, targetPos, piece, capturedPiece = targetPiece))
                }
            }
        }
    }

    private fun generateSlidingMoves(from: Position, piece: Piece, directions: List<Pair<Int, Int>>, moves: MutableList<Move>) {
        for ((dr, dc) in directions) {
            var r = from.row + dr
            var c = from.col + dc
            while (r in 0..7 && c in 0..7) {
                val targetPos = Position(r, c)
                val targetPiece = board.getPiece(targetPos)
                if (targetPiece == null) {
                    moves.add(Move(from, targetPos, piece))
                } else {
                    if (targetPiece.color != piece.color) {
                        moves.add(Move(from, targetPos, piece, capturedPiece = targetPiece))
                    }
                    break
                }
                r += dr
                c += dc
            }
        }
    }

    private fun generateKingMoves(from: Position, piece: Piece, moves: MutableList<Move>) {
        // Standard 1-step moves
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = from.row + dr
                val c = from.col + dc
                if (r in 0..7 && c in 0..7) {
                    val targetPos = Position(r, c)
                    val targetPiece = board.getPiece(targetPos)
                    if (targetPiece == null) {
                        moves.add(Move(from, targetPos, piece))
                    } else if (targetPiece.color != piece.color) {
                        moves.add(Move(from, targetPos, piece, capturedPiece = targetPiece))
                    }
                }
            }
        }

        // Castling
        val rights = board.castlingRights
        val isWhite = piece.color == PieceColor.WHITE
        val rank = if (isWhite) 7 else 0
        if (from.row == rank && from.col == 4 && !board.isCheck(piece.color)) {
            // Kingside castling (O-O)
            val canKingside = if (isWhite) rights.whiteKingside else rights.blackKingside
            if (canKingside) {
                val rookPiece = board.getPiece(rank, 7)
                if (rookPiece?.type == PieceType.ROOK && rookPiece.color == piece.color) {
                    if (board.getPiece(rank, 5) == null && board.getPiece(rank, 6) == null) {
                        if (!board.isSquareAttacked(Position(rank, 5), piece.color.opposite()) &&
                            !board.isSquareAttacked(Position(rank, 6), piece.color.opposite())
                        ) {
                            moves.add(Move(from, Position(rank, 6), piece, isCastling = true))
                        }
                    }
                }
            }

            // Queenside castling (O-O-O)
            val canQueenside = if (isWhite) rights.whiteQueenside else rights.blackQueenside
            if (canQueenside) {
                val rookPiece = board.getPiece(rank, 0)
                if (rookPiece?.type == PieceType.ROOK && rookPiece.color == piece.color) {
                    if (board.getPiece(rank, 1) == null && board.getPiece(rank, 2) == null && board.getPiece(rank, 3) == null) {
                        if (!board.isSquareAttacked(Position(rank, 3), piece.color.opposite()) &&
                            !board.isSquareAttacked(Position(rank, 2), piece.color.opposite())
                        ) {
                            moves.add(Move(from, Position(rank, 2), piece, isCastling = true))
                        }
                    }
                }
            }
        }
    }

    fun makeMove(move: Move): Boolean {
        if (status.isFinished()) return false
        val legalMoves = generateLegalMoves()
        val matchedMove = legalMoves.find {
            it.from == move.from && it.to == move.to &&
            (it.promotion == null || it.promotion == (move.promotion ?: PieceType.QUEEN))
        } ?: return false

        // Save board copy for undo
        val prevBoard = board.copy()

        // Apply
        applyMoveToBoard(board, matchedMove)

        // Record captures for UI
        matchedMove.capturedPiece?.let {
            if (it.color == PieceColor.WHITE) capturedByBlack.add(it) else capturedByWhite.add(it)
        }

        // Build SAN notation
        buildSanForMove(matchedMove, legalMoves)
        moveHistory.add(matchedMove)
        stateHistory.add(MoveRecord(matchedMove, prevBoard, matchedMove.capturedPiece))

        updateGameStatus()
        return true
    }

    private fun applyMoveToBoard(b: Board, m: Move) {
        val movingPiece = b.getPiece(m.from) ?: m.piece
        b.setPiece(m.from, null)

        if (m.isCastling) {
            val rank = m.from.row
            if (m.to.col == 6) { // Kingside
                val rook = b.getPiece(rank, 7)
                b.setPiece(rank, 7, null)
                b.setPiece(rank, 5, rook)
            } else if (m.to.col == 2) { // Queenside
                val rook = b.getPiece(rank, 0)
                b.setPiece(rank, 0, null)
                b.setPiece(rank, 3, rook)
            }
        } else if (m.isEnPassant) {
            val victimRow = m.from.row
            b.setPiece(victimRow, m.to.col, null)
        }

        val placedPiece = if (m.promotion != null) Piece(m.promotion, movingPiece.color) else movingPiece
        b.setPiece(m.to, placedPiece)

        // Update castling rights
        var cr = b.castlingRights
        if (movingPiece.type == PieceType.KING) {
            cr = if (movingPiece.color == PieceColor.WHITE) {
                cr.copy(whiteKingside = false, whiteQueenside = false)
            } else {
                cr.copy(blackKingside = false, blackQueenside = false)
            }
        } else if (movingPiece.type == PieceType.ROOK) {
            if (m.from.row == 7 && m.from.col == 7) cr = cr.copy(whiteKingside = false)
            if (m.from.row == 7 && m.from.col == 0) cr = cr.copy(whiteQueenside = false)
            if (m.from.row == 0 && m.from.col == 7) cr = cr.copy(blackKingside = false)
            if (m.from.row == 0 && m.from.col == 0) cr = cr.copy(blackQueenside = false)
        }
        b.castlingRights = cr

        // Update En Passant target
        if (movingPiece.type == PieceType.PAWN && kotlin.math.abs(m.to.row - m.from.row) == 2) {
            b.enPassantTarget = Position((m.from.row + m.to.row) / 2, m.from.col)
        } else {
            b.enPassantTarget = null
        }

        // Halfmove clock (reset on pawn moves or captures)
        if (movingPiece.type == PieceType.PAWN || m.capturedPiece != null) {
            b.halfmoveClock = 0
        } else {
            b.halfmoveClock++
        }

        // Turn & Fullmove number
        if (b.activeColor == PieceColor.BLACK) {
            b.fullmoveNumber++
            b.activeColor = PieceColor.WHITE
        } else {
            b.activeColor = PieceColor.BLACK
        }
    }

    private fun buildSanForMove(m: Move, candidates: List<Move>) {
        if (m.isCastling) {
            m.san = if (m.to.col == 6) "O-O" else "O-O-O"
        } else {
            val sb = StringBuilder()
            if (m.piece.type != PieceType.PAWN) {
                sb.append(m.piece.type.symbol)

                // Disambiguation if needed
                val others = candidates.filter {
                    it != m && it.piece.type == m.piece.type && it.to == m.to
                }
                if (others.isNotEmpty()) {
                    val sameFile = others.any { it.from.col == m.from.col }
                    val sameRank = others.any { it.from.row == m.from.row }
                    if (!sameFile) {
                        sb.append(m.from.fileChar)
                    } else if (!sameRank) {
                        sb.append(m.from.rankChar)
                    } else {
                        sb.append(m.from.algebraic)
                    }
                }
            } else if (m.capturedPiece != null) {
                sb.append(m.from.fileChar)
            }

            if (m.capturedPiece != null) {
                sb.append('x')
            }

            sb.append(m.to.algebraic)

            if (m.promotion != null) {
                sb.append('=').append(m.promotion.symbol)
            }
            m.san = sb.toString()
        }

        // Check if next position has check or mate
        val nextMoves = generateLegalMoves()
        val inCheck = board.isCheck(board.activeColor)
        if (inCheck) {
            if (nextMoves.isEmpty()) {
                m.isCheckmate = true
                m.san += "#"
            } else {
                m.isCheck = true
                m.san += "+"
            }
        }
    }

    fun undoMove(): Boolean {
        if (stateHistory.isEmpty()) return false
        val lastRecord = stateHistory.removeAt(stateHistory.size - 1)
        moveHistory.removeAt(moveHistory.size - 1)
        board = lastRecord.previousBoardState

        lastRecord.capturedPiece?.let { piece ->
            if (piece.color == PieceColor.WHITE) {
                capturedByBlack.remove(piece)
            } else {
                capturedByWhite.remove(piece)
            }
        }

        status = GameStatus.IN_PROGRESS
        winner = null
        resigner = null
        return true
    }

    fun resign(color: PieceColor) {
        status = GameStatus.RESIGNED
        resigner = color
        winner = color.opposite()
    }

    fun offerDraw() {
        status = GameStatus.DRAW_AGREED
        winner = null
    }

    private fun updateGameStatus() {
        if (status == GameStatus.RESIGNED || status == GameStatus.DRAW_AGREED) return

        val moves = generateLegalMoves()
        val inCheck = board.isCheck(board.activeColor)

        if (moves.isEmpty()) {
            if (inCheck) {
                status = GameStatus.CHECKMATE
                winner = board.activeColor.opposite()
            } else {
                status = GameStatus.STALEMATE
                winner = null
            }
            return
        }

        if (board.halfmoveClock >= 100) {
            status = GameStatus.DRAW_FIFTY_MOVES
            winner = null
            return
        }

        if (isInsufficientMaterial()) {
            status = GameStatus.DRAW_INSUFFICIENT_MATERIAL
            winner = null
            return
        }

        status = GameStatus.IN_PROGRESS
    }

    private fun isInsufficientMaterial(): Boolean {
        val pieces = mutableListOf<Piece>()
        for (r in 0..7) {
            for (c in 0..7) {
                board.getPiece(r, c)?.let { pieces.add(it) }
            }
        }
        if (pieces.size == 2) return true // K vs K
        if (pieces.size == 3) {
            val nonKing = pieces.find { it.type != PieceType.KING }
            if (nonKing?.type == PieceType.BISHOP || nonKing?.type == PieceType.KNIGHT) return true
        }
        return false
    }

    fun toPgn(): String {
        val dateStr = SimpleDateFormat("yyyy.MM.dd", Locale.US).format(Date())
        val resultStr = when (status) {
            GameStatus.CHECKMATE -> if (winner == PieceColor.WHITE) "1-0" else "0-1"
            GameStatus.RESIGNED -> if (winner == PieceColor.WHITE) "1-0" else "0-1"
            GameStatus.STALEMATE, GameStatus.DRAW_FIFTY_MOVES,
            GameStatus.DRAW_INSUFFICIENT_MATERIAL, GameStatus.DRAW_AGREED -> "1/2-1/2"
            else -> "*"
        }

        val sb = StringBuilder()
        sb.append("[Event \"$eventName\"]\n")
        sb.append("[Site \"Chess Arena Android\"]\n")
        sb.append("[Date \"$dateStr\"]\n")
        sb.append("[Round \"$roundName\"]\n")
        sb.append("[White \"$whitePlayerName\"]\n")
        sb.append("[Black \"$blackPlayerName\"]\n")
        sb.append("[Result \"$resultStr\"]\n")
        sb.append("\n")

        for (i in moveHistory.indices) {
            if (i % 2 == 0) {
                sb.append("${(i / 2) + 1}. ")
            }
            sb.append(moveHistory[i].san).append(" ")
        }
        sb.append(resultStr)

        return sb.toString().trim()
    }
}
