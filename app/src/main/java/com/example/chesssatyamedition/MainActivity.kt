package com.example.chesssatyamedition

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children

// Data classes for our game logic
data class Move(val from: Pair<Int, Int>, val to: Pair<Int, Int>, val score: Int)
data class ChessPiece(val type: String, val imageRes: Int, val isWhite: Boolean)

class MainActivity : AppCompatActivity() {

    // --- UI and Board State Variables ---
    private lateinit var chessBoard: GridLayout
    private val boardSize = 8
    private val boardCells = Array(boardSize) { arrayOfNulls<FrameLayout>(boardSize) } // Use FrameLayout to overlay dots
    private var pieces = mutableMapOf<Pair<Int, Int>, ChessPiece>()
    private var selectedPiece: Pair<Int, Int>? = null
    private var currentTurn = true // true for White, false for Black

    // --- Game Mode Variables ---
    private var isAiMode = false
    private var playerIsWhite = true
    private var isProMode = false
    private var aiDifficulty = 1 // 1=Easy, 2=Medium, 3=Hard

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // --- Find all UI elements ---
        chessBoard = findViewById(R.id.chessBoard)
        val home: LinearLayout = findViewById(R.id.home)
        val btnEasy: Button = findViewById(R.id.btnEasy)
        val btnMedium: Button = findViewById(R.id.btnMedium)
        val btnHard: Button = findViewById(R.id.btnHard)
        val btnNewGame: Button = findViewById(R.id.btnNewGame)
        val btnExit: Button = findViewById(R.id.btnExit)
        val btnExit2: Button = findViewById(R.id.btnExit2)
        val rgPlayerColor: RadioGroup = findViewById(R.id.rgPlayerColor)
        val cbProMode: CheckBox = findViewById(R.id.cbProMode)

        // --- Set up button clicks ---
        btnEasy.setOnClickListener { startGame(isAi = true, difficulty = 1, rgPlayerColor.checkedRadioButtonId, cbProMode.isChecked) }
        btnMedium.setOnClickListener { startGame(isAi = true, difficulty = 2, rgPlayerColor.checkedRadioButtonId, cbProMode.isChecked) }
        btnHard.setOnClickListener { startGame(isAi = true, difficulty = 3, rgPlayerColor.checkedRadioButtonId, cbProMode.isChecked) }
        btnNewGame.setOnClickListener { startGame(isAi = false, difficulty = 0, rgPlayerColor.checkedRadioButtonId, cbProMode.isChecked) }

        btnExit.setOnClickListener { finishAffinity() }
        btnExit2.setOnClickListener {
            home.visibility = View.VISIBLE
            chessBoard.visibility = View.GONE
            btnExit2.visibility = View.GONE
        }
    }

    private fun startGame(isAi: Boolean, difficulty: Int, colorSelectionId: Int, proMode: Boolean) {
        isAiMode = isAi
        aiDifficulty = difficulty
        playerIsWhite = (colorSelectionId == R.id.rbPlayAsWhite)
        isProMode = proMode

        setupBoard()

        findViewById<LinearLayout>(R.id.home).visibility = View.GONE
        chessBoard.visibility = View.VISIBLE
        findViewById<Button>(R.id.btnExit2).visibility = View.VISIBLE

        // If player chose Black, AI (White) makes the first move
        if (isAiMode && !playerIsWhite) {
            currentTurn = true // White's turn
            Handler(Looper.getMainLooper()).postDelayed({ performAiMove() }, 1000)
        }
    }

    private fun onCellClick(view: View) {
        val position = view.tag as Pair<Int, Int>

        // STRICT TURN CHECK: Prevent player from moving during AI's turn
        if (isAiMode && currentTurn != playerIsWhite) {
            Toast.makeText(this, "Computer is thinking...", Toast.LENGTH_SHORT).show()
            return
        }

        // Case 1: A piece is already selected
        if (selectedPiece != null) {
            // If the tapped cell is a valid move for the selected piece and is legal
            if (isValidMove(selectedPiece!!, position, pieces[selectedPiece!!]!!, pieces) && isMoveLegal(selectedPiece!!, position)) {
                movePiece(selectedPiece!!, position)
            }
            // Always deselect after a tap
            clearHighlights()
            selectedPiece = null
        }
        // Case 2: No piece is selected, try to select one
        else {
            val piece = pieces[position]
            // Can only select own pieces on your turn
            if (piece != null && piece.isWhite == currentTurn) {
                selectedPiece = position
                highlightPossibleMoves(position)
            }
        }
    }

    private fun movePiece(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        val piece = pieces[from]!!
        pieces.remove(from)
        pieces[to] = piece

        updateBoardUI()
        currentTurn = !currentTurn

        if (isKingInCheck(currentTurn)) {
            if (isCheckmate(currentTurn)) {
                val winner = if (currentTurn == playerIsWhite) "Computer" else "You"
                showGameOverDialog("Checkmate! $winner win!")
                return
            } else {
                Toast.makeText(this, "Check!", Toast.LENGTH_SHORT).show()
            }
        }

        if (isAiMode && currentTurn != playerIsWhite) {
            Handler(Looper.getMainLooper()).postDelayed({ performAiMove() }, 500)
        }
    }

    private fun highlightPossibleMoves(position: Pair<Int, Int>) {
        if (isProMode) return
        clearHighlights()
        val piece = pieces[position] ?: return
        boardCells[position.first][position.second]?.setBackgroundColor(Color.YELLOW)

        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
                val toPos = Pair(i, j)
                if (isValidMove(position, toPos, piece, pieces) && isMoveLegal(position, toPos)) {
                    val cell = boardCells[i][j]
                    val dot = ImageView(this).apply {
                        setImageResource(R.drawable.highlight_dot)
                        tag = "highlight_dot"
                    }
                    cell?.addView(dot)
                }
            }
        }
    }

    private fun clearHighlights() {
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
                val originalColor = if ((i + j) % 2 == 0) Color.parseColor("#8B4513") else Color.parseColor("#D7B899")
                boardCells[i][j]?.setBackgroundColor(originalColor)
                val cell = boardCells[i][j]
                val dotsToRemove = cell?.children?.filter { it.tag == "highlight_dot" }?.toList()
                dotsToRemove?.forEach { dotView -> cell.removeView(dotView) }
            }
        }
    }

    private fun isMoveLegal(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        val piece = pieces[from] ?: return false
        val tempPieces = pieces.toMutableMap()
        val targetPiece = tempPieces[to]
        tempPieces.remove(from)
        tempPieces[to] = piece
        val isLegal = !isKingInCheck(piece.isWhite, tempPieces)
        // Backtrack to restore original state for the main board
        tempPieces[from] = piece
        if(targetPiece != null) tempPieces[to] = targetPiece else tempPieces.remove(to)
        return isLegal
    }

    private fun isKingInCheck(kingIsWhite: Boolean, currentPieces: Map<Pair<Int, Int>, ChessPiece> = this.pieces): Boolean {
        val kingPos = currentPieces.entries.find { it.value.type == "King" && it.value.isWhite == kingIsWhite }?.key ?: return false
        val opponentIsWhite = !kingIsWhite
        for ((pos, piece) in currentPieces) {
            if (piece.isWhite == opponentIsWhite) {
                if (isValidMove(pos, kingPos, piece, currentPieces)) {
                    return true
                }
            }
        }
        return false
    }

    private fun isCheckmate(kingIsWhite: Boolean): Boolean {
        if (!isKingInCheck(kingIsWhite)) return false
        val allPossibleMoves = getAllPossibleMoves(kingIsWhite, pieces)
        return allPossibleMoves.none { move -> isMoveLegal(move.from, move.to) }
    }

    private fun performAiMove() {
        Toast.makeText(this, "Computer is thinking...", Toast.LENGTH_SHORT).show()
        Thread {
            val bestMove = minimaxRoot(aiDifficulty, currentTurn, pieces)
            Handler(Looper.getMainLooper()).post {
                if (bestMove != null) {
                    movePiece(bestMove.from, bestMove.to)
                } else {
                    showGameOverDialog("Game Over! You win!")
                }
            }
        }.start()
    }

    private fun minimaxRoot(depth: Int, isMaximizingPlayer: Boolean, boardState: Map<Pair<Int, Int>, ChessPiece>): Move? {
        val possibleMoves = getAllPossibleMoves(isMaximizingPlayer, boardState)
        var bestMoveValue = if (isMaximizingPlayer) Int.MIN_VALUE else Int.MAX_VALUE
        var bestMoveFound: Move? = null
        for (move in possibleMoves) {
            if (!isMoveLegal(move.from, move.to)) continue
            val newBoardState = applyMove(boardState, move)
            val moveValue = minimax(depth - 1, !isMaximizingPlayer, newBoardState, Int.MIN_VALUE, Int.MAX_VALUE)
            if (isMaximizingPlayer) {
                if (moveValue >= bestMoveValue) {
                    bestMoveValue = moveValue
                    bestMoveFound = move
                }
            } else {
                if (moveValue <= bestMoveValue) {
                    bestMoveValue = moveValue
                    bestMoveFound = move
                }
            }
        }
        return bestMoveFound
    }

    private fun minimax(depth: Int, isMaximizingPlayer: Boolean, boardState: Map<Pair<Int, Int>, ChessPiece>, alpha: Int, beta: Int): Int {
        if (depth == 0) return evaluateBoard(boardState)
        val possibleMoves = getAllPossibleMoves(isMaximizingPlayer, boardState)
        if (possibleMoves.isEmpty()) return evaluateBoard(boardState)
        var a = alpha
        var b = beta
        if (isMaximizingPlayer) {
            var bestValue = Int.MIN_VALUE
            for (move in possibleMoves) {
                if (!isMoveLegal(move.from, move.to)) continue
                val newBoardState = applyMove(boardState, move)
                bestValue = maxOf(bestValue, minimax(depth - 1, !isMaximizingPlayer, newBoardState, a, b))
                a = maxOf(a, bestValue)
                if (b <= a) break
            }
            return bestValue
        } else {
            var bestValue = Int.MAX_VALUE
            for (move in possibleMoves) {
                if (!isMoveLegal(move.from, move.to)) continue
                val newBoardState = applyMove(boardState, move)
                bestValue = minOf(bestValue, minimax(depth - 1, !isMaximizingPlayer, newBoardState, a, b))
                b = minOf(b, bestValue)
                if (b <= a) break
            }
            return bestValue
        }
    }

    private fun getAllPossibleMoves(isWhite: Boolean, boardState: Map<Pair<Int, Int>, ChessPiece>): List<Move> {
        val moves = mutableListOf<Move>()
        for ((from, piece) in boardState) {
            if (piece.isWhite == isWhite) {
                for (i in 0 until boardSize) {
                    for (j in 0 until boardSize) {
                        val to = Pair(i, j)
                        if (isValidMove(from, to, piece, boardState)) {
                            moves.add(Move(from, to, 0))
                        }
                    }
                }
            }
        }
        return moves
    }

    private fun applyMove(boardState: Map<Pair<Int, Int>, ChessPiece>, move: Move): Map<Pair<Int, Int>, ChessPiece> {
        val newBoard = boardState.toMutableMap()
        val piece = newBoard[move.from] ?: return boardState
        newBoard.remove(move.from)
        newBoard[move.to] = piece
        return newBoard
    }

    private fun evaluateBoard(boardState: Map<Pair<Int, Int>, ChessPiece>): Int {
        var totalScore = 0
        for (piece in boardState.values) {
            val score = getPieceValue(piece)
            totalScore += if (piece.isWhite != playerIsWhite) score else -score
        }
        return totalScore
    }

    private fun getPieceValue(piece: ChessPiece): Int {
        return when (piece.type) {
            "Pawn" -> 10
            "Knight" -> 30
            "Bishop" -> 30
            "Rook" -> 50
            "Queen" -> 90
            "King" -> 900
            else -> 0
        }
    }

    private fun showGameOverDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage(message)
            .setPositiveButton("Main Menu") { _, _ ->
                findViewById<LinearLayout>(R.id.home).visibility = View.VISIBLE
                chessBoard.visibility = View.GONE
                findViewById<Button>(R.id.btnExit2).visibility = View.GONE
            }
            .setCancelable(false)
            .show()
    }

    // THIS IS THE CORRECTED FUNCTION
    private fun updateBoardUI() {
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
                // The boardCells are FrameLayouts. The first child (at index 0) is the ImageView for the piece.
                val pieceImage = boardCells[i][j]?.getChildAt(0) as? ImageView
                val piece = pieces[Pair(i, j)]
                pieceImage?.setImageResource(piece?.imageRes ?: 0) // Use ?: 0 to clear the image if no piece
            }
        }
    }

    // THIS IS THE CORRECTED FUNCTION
    private fun setupBoard() {
        pieces.clear()
        currentTurn = true
        selectedPiece = null
        chessBoard.removeAllViews()

        chessBoard.post {
            val cellSize = chessBoard.width / boardSize
            for (i in 0 until boardSize) {
                for (j in 0 until boardSize) {
                    val cell = FrameLayout(this).apply {
                        layoutParams = GridLayout.LayoutParams().apply { width = cellSize; height = cellSize }
                        tag = Pair(i, j)
                        val backgroundColor = if ((i + j) % 2 == 0) Color.parseColor("#8B4513") else Color.parseColor("#D7B899")
                        setBackgroundColor(backgroundColor)
                        setOnClickListener { onCellClick(this) }
                    }
                    val pieceImage = ImageView(this).apply {
                        layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    }
                    cell.addView(pieceImage)
                    chessBoard.addView(cell)
                    boardCells[i][j] = cell
                }
            }
            initializePieces()
            updateBoardUI()
        }
    }

    private fun initializePieces() {
        pieces.clear()
        (0..7).forEach { j -> pieces[Pair(1, j)] = ChessPiece("Pawn", R.drawable.black_pawn, false) }
        pieces[Pair(0, 0)] = ChessPiece("Rook", R.drawable.black_rook, false)
        pieces[Pair(0, 7)] = ChessPiece("Rook", R.drawable.black_rook, false)
        pieces[Pair(0, 1)] = ChessPiece("Knight", R.drawable.black_knight, false)
        pieces[Pair(0, 6)] = ChessPiece("Knight", R.drawable.black_knight, false)
        pieces[Pair(0, 2)] = ChessPiece("Bishop", R.drawable.black_bishop, false)
        pieces[Pair(0, 5)] = ChessPiece("Bishop", R.drawable.black_bishop, false)
        pieces[Pair(0, 3)] = ChessPiece("Queen", R.drawable.black_queen, false)
        pieces[Pair(0, 4)] = ChessPiece("King", R.drawable.black_king, false)

        (0..7).forEach { j -> pieces[Pair(6, j)] = ChessPiece("Pawn", R.drawable.white_pawn, true) }
        pieces[Pair(7, 0)] = ChessPiece("Rook", R.drawable.white_rook, true)
        pieces[Pair(7, 7)] = ChessPiece("Rook", R.drawable.white_rook, true)
        pieces[Pair(7, 1)] = ChessPiece("Knight", R.drawable.white_knight, true)
        pieces[Pair(7, 6)] = ChessPiece("Knight", R.drawable.white_knight, true)
        pieces[Pair(7, 2)] = ChessPiece("Bishop", R.drawable.white_bishop, true)
        pieces[Pair(7, 5)] = ChessPiece("Bishop", R.drawable.white_bishop, true)
        pieces[Pair(7, 3)] = ChessPiece("Queen", R.drawable.white_queen, true)
        pieces[Pair(7, 4)] = ChessPiece("King", R.drawable.white_king, true)
    }

    private fun isValidMove(from: Pair<Int, Int>, to: Pair<Int, Int>, piece: ChessPiece, currentPieces: Map<Pair<Int, Int>, ChessPiece>): Boolean {
        if (from == to) return false
        val targetPiece = currentPieces[to]
        if (targetPiece != null && targetPiece.isWhite == piece.isWhite) return false
        val fromRow = from.first
        val fromCol = from.second
        val toRow = to.first
        val toCol = to.second

        return when (piece.type) {
            "Pawn" -> {
                val direction = if (piece.isWhite) -1 else 1
                val startRow = if (piece.isWhite) 6 else 1
                // Standard 1-step move
                if (fromCol == toCol && fromRow + direction == toRow && currentPieces[to] == null) {
                    true
                }
                // Initial 2-step move
                else if (fromCol == toCol && fromRow == startRow && fromRow + 2 * direction == toRow && currentPieces[to] == null && currentPieces[Pair(fromRow + direction, fromCol)] == null) {
                    true
                }
                // Capture move
                else if (Math.abs(fromCol - toCol) == 1 && fromRow + direction == toRow && targetPiece != null) {
                    true
                } else {
                    false
                }
            }
            "Rook" -> (fromRow == toRow || fromCol == toCol) && isPathClear(from, to, currentPieces)
            "Bishop" -> Math.abs(fromRow - toRow) == Math.abs(fromCol - toCol) && isPathClear(from, to, currentPieces)
            "Queen" -> (fromRow == toRow || fromCol == toCol || Math.abs(fromRow - toRow) == Math.abs(fromCol - toCol)) && isPathClear(from, to, currentPieces)
            "Knight" -> {
                val dx = Math.abs(fromRow - toRow)
                val dy = Math.abs(fromCol - toCol)
                (dx == 2 && dy == 1) || (dx == 1 && dy == 2)
            }
            "King" -> Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1
            else -> false
        }
    }

    private fun isPathClear(from: Pair<Int, Int>, to: Pair<Int, Int>, currentPieces: Map<Pair<Int, Int>, ChessPiece>): Boolean {
        val dx = to.first - from.first
        val dy = to.second - from.second
        val stepX = if (dx == 0) 0 else dx / Math.abs(dx)
        val stepY = if (dy == 0) 0 else dy / Math.abs(dy)
        var currentX = from.first + stepX
        var currentY = from.second + stepY
        while (currentX != to.first || currentY != to.second) {
            if (currentPieces.containsKey(Pair(currentX, currentY))) return false
            currentX += stepX
            currentY += stepY
        }
        return true
    }
}
