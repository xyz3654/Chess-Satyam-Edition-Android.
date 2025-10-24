package com.example.chesssatyamedition

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

// Data class for a move, used by the AI
data class Move(val from: Pair<Int, Int>, val to: Pair<Int, Int>, val score: Int)

// Data class for a chess piece
data class ChessPiece(val type: String, val imageRes: Int, val isWhite: Boolean)

class MainActivity : AppCompatActivity() {
    private lateinit var chessBoard: GridLayout
    private val boardSize = 8
    private val board = Array(boardSize) { arrayOfNulls<ImageView>(boardSize) }
    private var pieces = mutableMapOf<Pair<Int, Int>, ChessPiece>()
    private var selectedPiece: Pair<Int, Int>? = null
    private var currentTurn = true // true for White's turn (Player), false for Black's turn (AI)
    private lateinit var sharedPreferences: SharedPreferences

    // --- AI VARIABLES ---
    private var isAiMode = false // Is the game against the computer?
    private val aiPlayerIsWhite = false // The AI will play as Black

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("ChessGame", Context.MODE_PRIVATE)
        chessBoard = findViewById(R.id.chessBoard)

        val playWithComputerButton: Button = findViewById(R.id.btnPlayWithComputer)
        val newGameButton: Button = findViewById(R.id.btnNewGame)
        val continueGameButton: Button = findViewById(R.id.btnContinueGame)
        val exitButton: Button = findViewById(R.id.btnExit)
        val exitButton2: Button = findViewById(R.id.btnExit2)
        val home: LinearLayout = findViewById(R.id.home)

        playWithComputerButton.setOnClickListener {
            isAiMode = true // Enable AI mode
            chessBoard.visibility = View.VISIBLE
            setupBoard()
            home.visibility = View.GONE
            exitButton2.visibility = View.VISIBLE
        }

        newGameButton.setOnClickListener {
            isAiMode = false // Disable AI mode for 2-player
            chessBoard.visibility = View.VISIBLE
            setupBoard()
            home.visibility = View.GONE
            exitButton2.visibility = View.VISIBLE
        }

        continueGameButton.setOnClickListener {
            isAiMode = false
            chessBoard.visibility = View.VISIBLE
            loadPreviousGame()
            home.visibility = View.GONE
            exitButton2.visibility = View.VISIBLE
        }

        exitButton.setOnClickListener { finishAffinity() }
        exitButton2.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Are you sure you want to exit?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    chessBoard.visibility = View.GONE
                    home.visibility = View.VISIBLE
                    exitButton2.visibility = View.GONE
                    saveGame()
                }
                .setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
            builder.create().show()
        }
    }

    private fun onCellClick(view: View) {
        if (isAiMode && currentTurn == aiPlayerIsWhite) {
            Toast.makeText(this, "Computer is thinking...", Toast.LENGTH_SHORT).show()
            return
        }

        val cell = view as ImageView
        val position = cell.tag as Pair<Int, Int>

        if (selectedPiece == null) {
            val piece = pieces[position]
            if (piece != null && piece.isWhite == currentTurn) {
                selectedPiece = position
                highlightCell(position, true)
                Toast.makeText(this, "Selected: ${piece.type}", Toast.LENGTH_SHORT).show()
            }
        } else {
            val piece = pieces[selectedPiece!!]
            if (piece != null && isValidMove(selectedPiece!!, position, piece)) {
                movePiece(selectedPiece!!, position)
            } else {
                Toast.makeText(this, "Invalid move", Toast.LENGTH_SHORT).show()
            }
            highlightCell(selectedPiece!!, false)
            selectedPiece = null
        }
    }

    private fun movePiece(from: Pair<Int, Int>, to: Pair<Int, Int>) {
        val piece = pieces[from] ?: return

        val capturedPiece = pieces.remove(to)
        if (capturedPiece != null) {
            Toast.makeText(this, "${capturedPiece.type} captured!", Toast.LENGTH_SHORT).show()
        }

        pieces.remove(from)
        pieces[to] = piece
        updateBoardUI()

        if (isKingCaptured(!piece.isWhite)) {
            val winner = if (piece.isWhite) "White" else "Black"
            showGameOverDialog("$winner wins by capturing the king!")
            return
        }

        currentTurn = !currentTurn

        // THIS IS THE LINE THAT WAS FIXED
        if (isAiMode && currentTurn == aiPlayerIsWhite) {
            Handler(Looper.getMainLooper()).postDelayed({ performAiMove() }, 1000)
        }
    }

    private fun performAiMove() {
        Toast.makeText(this, "Computer's turn...", Toast.LENGTH_SHORT).show()
        val bestMove = findBestMove()
        if (bestMove != null) {
            movePiece(bestMove.from, bestMove.to)
        } else {
            showGameOverDialog("Game Over! Player wins!")
        }
    }

    private fun findBestMove(): Move? {
        val possibleMoves = mutableListOf<Move>()
        val allAiPieces = pieces.filter { it.value.isWhite == aiPlayerIsWhite }

        for ((fromPos, piece) in allAiPieces) {
            for (i in 0 until boardSize) {
                for (j in 0 until boardSize) {
                    val toPos = Pair(i, j)
                    if (isValidMove(fromPos, toPos, piece)) {
                        val score = evaluateMove(toPos)
                        possibleMoves.add(Move(fromPos, toPos, score))
                    }
                }
            }
        }
        return possibleMoves.maxByOrNull { it.score }
    }

    private fun evaluateMove(to: Pair<Int, Int>): Int {
        var score = 0
        val targetPiece = pieces[to]
        if (targetPiece != null) {
            score = getPieceValue(targetPiece)
        }
        return score
    }

    private fun getPieceValue(piece: ChessPiece): Int {
        return when (piece.type) {
            "Pawn" -> 1
            "Knight" -> 3
            "Bishop" -> 3
            "Rook" -> 5
            "Queen" -> 9
            "King" -> 1000
            else -> 0
        }
    }

    private fun showGameOverDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("Game Over")
            .setMessage(message)
            .setPositiveButton("New Game") { _, _ -> setupBoard() }
            .setCancelable(false)
            .show()
    }

    private fun updateBoardUI() {
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
                val pos = Pair(i, j)
                val piece = pieces[pos]
                board[i][j]?.setImageResource(piece?.imageRes ?: 0)
            }
        }
    }

    private fun highlightCell(position: Pair<Int, Int>, highlight: Boolean) {
        val cell = board[position.first][position.second]
        val originalColor = if ((position.first + position.second) % 2 == 0)
            android.graphics.Color.parseColor("#8B4513") else android.graphics.Color.parseColor("#D7B899")
        cell?.setBackgroundColor(if (highlight) android.graphics.Color.YELLOW else originalColor)
    }

    private fun isKingCaptured(isWhiteKing: Boolean): Boolean {
        return pieces.values.none { it.type == "King" && it.isWhite == isWhiteKing }
    }

    private fun setupBoard() {
        val editor = sharedPreferences.edit()
        editor.remove("pieces")
        editor.remove("currentTurn")
        editor.apply()
        pieces.clear()
        currentTurn = true
        selectedPiece = null
        chessBoard.removeAllViews()

        chessBoard.post {
            val cellSize = chessBoard.width / boardSize
            for (i in 0 until boardSize) {
                for (j in 0 until boardSize) {
                    val cell = ImageView(this)
                    val color = if ((i + j) % 2 == 0) android.graphics.Color.parseColor("#8B4513") else android.graphics.Color.parseColor("#D7B899")
                    cell.setBackgroundColor(color)
                    cell.tag = Pair(i, j)
                    val params = GridLayout.LayoutParams()
                    params.width = cellSize
                    params.height = cellSize
                    cell.layoutParams = params
                    cell.setOnClickListener { onCellClick(it) }
                    chessBoard.addView(cell)
                    board[i][j] = cell
                }
            }
            initializePieces()
            updateBoardUI()
        }
    }

    private fun initializePieces() {
        pieces.clear()
        for (i in 0 until boardSize) {
            for (j in 0 until boardSize) {
                val piece = getInitialPiece(i, j)
                if (piece != null) {
                    pieces[Pair(i, j)] = piece
                }
            }
        }
    }

    private fun loadPreviousGame() {
        isAiMode = false // Assume saved games are 2-player for now
        pieces.clear()
        currentTurn = sharedPreferences.getBoolean("currentTurn", true)

        chessBoard.post {
            val savedPieces = sharedPreferences.getString("pieces", "") ?: ""
            if (savedPieces.isNotEmpty()) {
                val pieceArray = savedPieces.split(";")
                for (pieceData in pieceArray) {
                    val parts = pieceData.split(",")
                    if (parts.size == 4) {
                        val x = parts[0].toInt()
                        val y = parts[1].toInt()
                        val type = parts[2]
                        val isWhite = parts[3].toBoolean()
                        val imageRes = getImageResource(type, isWhite)
                        val piece = ChessPiece(type, imageRes, isWhite)
                        pieces[Pair(x, y)] = piece
                    }
                }
                updateBoardUI()
                Toast.makeText(this, "Game Loaded!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No saved game found! Starting new game.", Toast.LENGTH_SHORT).show()
                setupBoard()
            }
        }
    }

    private fun saveGame() {
        val editor = sharedPreferences.edit()
        val pieceData = pieces.entries.joinToString(";") { (pos, piece) ->
            "${pos.first},${pos.second},${piece.type},${piece.isWhite}"
        }
        editor.putString("pieces", pieceData)
        editor.putBoolean("currentTurn", currentTurn)
        editor.apply()
        Toast.makeText(this, "Game Saved!", Toast.LENGTH_SHORT).show()
    }

    private fun getImageResource(type: String, isWhite: Boolean): Int {
        return when (type) {
            "Pawn" -> if (isWhite) R.drawable.white_pawn else R.drawable.black_pawn
            "Rook" -> if (isWhite) R.drawable.white_rook else R.drawable.black_rook
            "Knight" -> if (isWhite) R.drawable.white_knight else R.drawable.black_knight
            "Bishop" -> if (isWhite) R.drawable.white_bishop else R.drawable.black_bishop
            "Queen" -> if (isWhite) R.drawable.white_queen else R.drawable.black_queen
            "King" -> if (isWhite) R.drawable.white_king else R.drawable.black_king
            else -> 0
        }
    }

    private fun getInitialPiece(i: Int, j: Int): ChessPiece? {
        return when (i) {
            1 -> ChessPiece("Pawn", R.drawable.black_pawn, isWhite = false)
            6 -> ChessPiece("Pawn", R.drawable.white_pawn, isWhite = true)
            0 -> when (j) {
                0, 7 -> ChessPiece("Rook", R.drawable.black_rook, isWhite = false)
                1, 6 -> ChessPiece("Knight", R.drawable.black_knight, isWhite = false)
                2, 5 -> ChessPiece("Bishop", R.drawable.black_bishop, isWhite = false)
                3 -> ChessPiece("Queen", R.drawable.black_queen, isWhite = false)
                4 -> ChessPiece("King", R.drawable.black_king, isWhite = false)
                else -> null
            }
            7 -> when (j) {
                0, 7 -> ChessPiece("Rook", R.drawable.white_rook, isWhite = true)
                1, 6 -> ChessPiece("Knight", R.drawable.white_knight, isWhite = true)
                2, 5 -> ChessPiece("Bishop", R.drawable.white_bishop, isWhite = true)
                3 -> ChessPiece("Queen", R.drawable.white_queen, isWhite = true)
                4 -> ChessPiece("King", R.drawable.white_king, isWhite = true)
                else -> null
            }
            else -> null
        }
    }

    private fun isValidMove(from: Pair<Int, Int>, to: Pair<Int, Int>, piece: ChessPiece): Boolean {
        val targetPiece = pieces[to]
        if (targetPiece != null && targetPiece.isWhite == piece.isWhite) {
            return false
        }

        return when (piece.type) {
            "Pawn" -> {
                val direction = if (piece.isWhite) -1 else 1
                val startRow = if (piece.isWhite) 6 else 1
                if (from.second == to.second && to.first - from.first == direction && !pieces.containsKey(to)) {
                    true
                } else if (from.second == to.second && from.first == startRow && to.first - from.first == 2 * direction && !pieces.containsKey(to) && isPathClear(from, to)) {
                    true
                } else Math.abs(from.second - to.second) == 1 && to.first - from.first == direction && pieces.containsKey(to)
            }
            "Rook" -> (from.first == to.first || from.second == to.second) && isPathClear(from, to)
            "Bishop" -> Math.abs(from.first - to.first) == Math.abs(from.second - to.second) && isPathClear(from, to)
            "Queen" -> (from.first == to.first || from.second == to.second || Math.abs(from.first - to.first) == Math.abs(from.second - to.second)) && isPathClear(from, to)
            "Knight" -> {
                val dx = Math.abs(from.first - to.first)
                val dy = Math.abs(from.second - to.second)
                (dx == 2 && dy == 1) || (dx == 1 && dy == 2)
            }
            "King" -> Math.abs(from.first - to.first) <= 1 && Math.abs(from.second - to.second) <= 1
            else -> false
        }
    }

    private fun isPathClear(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        val dx = to.first - from.first
        val dy = to.second - from.second
        val stepX = if (dx == 0) 0 else dx / Math.abs(dx)
        val stepY = if (dy == 0) 0 else dy / Math.abs(dy)
        var currentX = from.first + stepX
        var currentY = from.second + stepY
        while (currentX != to.first || currentY != to.second) {
            if (pieces.containsKey(Pair(currentX, currentY))) {
                return false
            }
            currentX += stepX
            currentY += stepY
        }
        return true
    }
}
