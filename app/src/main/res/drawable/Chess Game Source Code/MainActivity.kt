package com.example.myapplication

import android.content.Context
import android.content.SharedPreferences
import android.widget.GridLayout
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity() {
    private lateinit var chessBoard: GridLayout
    private val boardSize = 8
    private val board = Array(boardSize) { Array<ImageView?>(boardSize) { null } }
    private val pieces = mutableMapOf<Pair<Int, Int>, ChessPiece>()
    private var selectedPiece: Pair<Int, Int>? = null
    private var currentTurn = true // true for White's turn, false for Black's turn
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences = getSharedPreferences("ChessGame", Context.MODE_PRIVATE)


        chessBoard = findViewById(R.id.chessBoard)


        val newGameButton: Button = findViewById(R.id.btnNewGame)
        val continueGameButton: Button = findViewById(R.id.btnContinueGame)
        val exitButton: Button = findViewById(R.id.btnExit)
        val exitButton2: Button = findViewById(R.id.btnExit2)
        val home: LinearLayout = findViewById(R.id.home)


        newGameButton.setOnClickListener {
            chessBoard.visibility = View.VISIBLE
            setupBoard() // Start a new game
            home.visibility = View.GONE
            exitButton2.visibility = View.VISIBLE

        }

        continueGameButton.setOnClickListener {
            chessBoard.visibility = View.VISIBLE
            loadPreviousGame()
            home.visibility = View.GONE
            exitButton2.visibility = View.VISIBLE

        }

        exitButton.setOnClickListener { finishAffinity() }

        exitButton2.setOnClickListener {
            val builder = AlertDialog.Builder(this)
            builder.setMessage("Are you sure you want to exit the game?")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    chessBoard.visibility = View.GONE
                    home.visibility = View.VISIBLE
                    exitButton2.visibility = View.GONE
                    saveGame()
                }
                .setNegativeButton("No") { dialog, id ->
                    // Do nothing, just dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }


    }

    private fun setupBoard() {
        // Clear saved game data
        val editor = sharedPreferences.edit()
        editor.remove("pieces")
        editor.remove("currentTurn")
        editor.apply()

        chessBoard.post {
            pieces.clear()
            currentTurn = true
            selectedPiece = null
            chessBoard.removeAllViews()

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

                    // Place pieces
                    val piece = getInitialPiece(i, j)
                    if (piece != null) {
                        cell.setImageResource(piece.imageRes)
                        pieces[Pair(i, j)] = piece
                    }
                }
            }
        }
    }

    private fun loadPreviousGame() {
        pieces.clear()
        currentTurn = sharedPreferences.getBoolean("currentTurn", true)

        chessBoard.post {
            chessBoard.removeAllViews()
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
                        board[x][y]?.setImageResource(imageRes)
                    }
                }
                Toast.makeText(this, "Game Loaded!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "No saved game found!", Toast.LENGTH_SHORT).show()
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

    private fun onCellClick(view: View) {
        val cell = view as ImageView
        val position = cell.tag as Pair<Int, Int>

        if (selectedPiece == null) {
            // Attempt to select a piece
            val piece = pieces[position]
            if (piece != null) {
                if (piece.isWhite == currentTurn) {
                    // Valid piece selection
                    selectedPiece = position
                    Toast.makeText(this, "Selected: ${piece.type}", Toast.LENGTH_SHORT).show()
                } else {
                    // Trying to select opponent's piece
                    Toast.makeText(this, "It's ${if (currentTurn) "White" else "Black"}'s turn", Toast.LENGTH_SHORT).show()
                }
            } else {
                // No piece in the selected cell
                Toast.makeText(this, "No piece to select!", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Attempt to move the selected piece
            val piece = pieces[selectedPiece]
            if (piece != null && isValidMove(selectedPiece!!, position, piece)) {
                // Capture opponent's piece if present
                val capturedPiece = pieces.remove(position)
                if (capturedPiece != null) {
                    Toast.makeText(this, "${capturedPiece.type} captured!", Toast.LENGTH_SHORT).show()
                }

                // Move the selected piece
                pieces.remove(selectedPiece)
                pieces[position] = piece

                // Update the UI
                board[selectedPiece!!.first][selectedPiece!!.second]?.setImageResource(0)
                cell.setImageResource(piece.imageRes)

                // Check for game-over condition
                if (isKingCaptured(piece)) {
                    Toast.makeText(this, "Game Over! ${if (piece.isWhite) "White" else "Black"} wins!", Toast.LENGTH_LONG).show()
                    resetBoard()
                    return
                }

                // Handle pawn promotion if the pawn reaches the last row
                if ((piece.type == "Pawn" && (position.first == 0 || position.first == 7))) {
                    promotePawn(piece, position)
                    return
                }

                // If no piece was captured, switch turns, otherwise, continue playing
                if (capturedPiece != null) {
                    // Stay on the same player's turn
                    Toast.makeText(this, "It's ${if (currentTurn) "White" else "Black"}'s turn", Toast.LENGTH_SHORT).show()
                } else {
                    // Switch turns if no capture
                    currentTurn = !currentTurn
                    Toast.makeText(this, "It's ${if (currentTurn) "White" else "Black"}'s turn", Toast.LENGTH_SHORT).show()
                }

                // Reset selection
                selectedPiece = null
            } else {
                // Invalid move
                Toast.makeText(this, "Invalid move", Toast.LENGTH_SHORT).show()
                selectedPiece = null
            }
        }
    }

    private fun promotePawn(piece: ChessPiece, position: Pair<Int, Int>) {
        // Create a list of promotion options
        val promotionOptions = arrayOf("Queen", "Rook", "Bishop", "Knight")

        // Create an AlertDialog to let the player choose the promotion
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Choose a piece for promotion")
        builder.setItems(promotionOptions) { _, which ->
            // Determine the promoted piece based on the selection
            val promotedPieceType = promotionOptions[which]
            val promotedPiece = when (promotedPieceType) {
                "Queen" -> ChessPiece(promotedPieceType, if (piece.isWhite) R.drawable.white_queen else R.drawable.black_queen, piece.isWhite)
                "Rook" -> ChessPiece(promotedPieceType, if (piece.isWhite) R.drawable.white_rook else R.drawable.black_rook, piece.isWhite)
                "Bishop" -> ChessPiece(promotedPieceType, if (piece.isWhite) R.drawable.white_bishop else R.drawable.black_bishop, piece.isWhite)
                "Knight" -> ChessPiece(promotedPieceType, if (piece.isWhite) R.drawable.white_knight else R.drawable.black_knight, piece.isWhite)
                else -> return@setItems
            }

            // Replace the pawn with the promoted piece
            pieces[position] = promotedPiece

            // Update the UI with the promoted piece
            board[position.first][position.second]?.setImageResource(promotedPiece.imageRes)

            // Continue the game with the same player's turn
            Toast.makeText(this, "${promotedPieceType} promoted!", Toast.LENGTH_SHORT).show()
        }
        builder.show()
    }

    private fun isValidMove(from: Pair<Int, Int>, to: Pair<Int, Int>, piece: ChessPiece): Boolean {
        // Check if the destination cell contains a piece of the same color
        val targetPiece = pieces[to]
        if (targetPiece != null && targetPiece.isWhite == piece.isWhite) {
            return false // Cannot move to a cell occupied by a piece of the same color
        }

        // Existing piece-specific movement logic
        return when (piece.type) {
            "Pawn" -> {
                val direction = if (piece.isWhite) -1 else 1
                val startRow = if (piece.isWhite) 6 else 1

                if (from.second == to.second && to.first - from.first == direction && !pieces.containsKey(to)) {
                    true
                } else if (from.second == to.second && from.first == startRow && to.first - from.first == 2 * direction && !pieces.containsKey(to)) {
                    true
                } else if (Math.abs(from.second - to.second) == 1 && to.first - from.first == direction && pieces[to]?.isWhite != piece.isWhite) {
                    true
                } else {
                    false
                }
            }
            "Rook" -> (from.first == to.first || from.second == to.second) && isPathClear(from, to)
            "Bishop" -> Math.abs(from.first - to.first) == Math.abs(from.second - to.second) && isPathClear(from, to)
            "Queen" -> (from.first == to.first || from.second == to.second || Math.abs(from.first - to.first) == Math.abs(from.second - to.second)) && isPathClear(from, to)
            "Knight" -> Math.abs(from.first - to.first) == 2 && Math.abs(from.second - to.second) == 1 || Math.abs(from.first - to.first) == 1 && Math.abs(from.second - to.second) == 2
            "King" -> Math.abs(from.first - to.first) <= 1 && Math.abs(from.second - to.second) <= 1
            else -> false
        }
    }

    private fun isPathClear(from: Pair<Int, Int>, to: Pair<Int, Int>): Boolean {
        val deltaX = to.first - from.first
        val deltaY = to.second - from.second
        val stepX = Integer.signum(deltaX)
        val stepY = Integer.signum(deltaY)

        var x = from.first + stepX
        var y = from.second + stepY

        while (x != to.first || y != to.second) {
            if (pieces[Pair(x, y)] != null) return false
            x += stepX
            y += stepY
        }
        return true
    }

    private fun isKingCaptured(piece: ChessPiece): Boolean {
        return pieces.values.none { it.type == "King" && it.isWhite != piece.isWhite }
    }

    private fun resetBoard() {
        chessBoard.removeAllViews()
        pieces.clear()
        selectedPiece = null
        currentTurn = true // Reset to White's turn
        setupBoard()
    }































    private fun isCheck(kingPosition: Pair<Int, Int>, isWhite: Boolean): Boolean {
        for ((pos, piece) in pieces) {
            if (piece.isWhite != isWhite && isValidMove(pos, kingPosition, piece)) {
                return true
            }
        }
        return false
    }

    private fun hasLegalMoves(isWhite: Boolean): Boolean {
        for ((pos, piece) in pieces) {
            if (piece.isWhite == isWhite) {
                for (i in 0 until boardSize) {
                    for (j in 0 until boardSize) {
                        val target = Pair(i, j)
                        if (isValidMove(pos, target, piece)) {
                            val originalPiece = pieces[target]
                            pieces.remove(pos)
                            pieces[target] = piece
                            val inCheck = isCheck(getKingPosition(isWhite), isWhite)
                            pieces.remove(target)
                            if (originalPiece != null) pieces[target] = originalPiece
                            pieces[pos] = piece
                            if (!inCheck) return true
                        }
                    }
                }
            }
        }
        return false
    }

    private fun getKingPosition(isWhite: Boolean): Pair<Int, Int> {
        return pieces.entries.first { it.value.type == "King" && it.value.isWhite == isWhite }.key
    }

    private fun checkGameState() {
        val whiteKingPos = getKingPosition(true)
        val blackKingPos = getKingPosition(false)
        val whiteInCheck = isCheck(whiteKingPos, true)
        val blackInCheck = isCheck(blackKingPos, false)

        when {
            whiteInCheck && !hasLegalMoves(true) -> {
                Toast.makeText(this, "Checkmate! Black wins!", Toast.LENGTH_LONG).show()
                resetBoard()
            }
            blackInCheck && !hasLegalMoves(false) -> {
                Toast.makeText(this, "Checkmate! White wins!", Toast.LENGTH_LONG).show()
                resetBoard()
            }
            !whiteInCheck && !hasLegalMoves(true) -> {
                Toast.makeText(this, "Stalemate! It's a draw!", Toast.LENGTH_LONG).show()
                resetBoard()
            }
            !blackInCheck && !hasLegalMoves(false) -> {
                Toast.makeText(this, "Stalemate! It's a draw!", Toast.LENGTH_LONG).show()
                resetBoard()
            }
        }
    }





}

data class ChessPiece(val type: String, val imageRes: Int, val isWhite: Boolean)
