package com.boss.carrom

import android.animation.ValueAnimator
import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.hypot
import kotlin.math.min

class CarromSimulatorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 18, 16, 16)
            setBackgroundColor(Color.rgb(22, 24, 29))
        }
        root.addView(TextView(this).apply {
            text = "AUTOMATIC CARROM SIMULATOR"
            textSize = 20f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(4, 8, 4, 8)
        })
        root.addView(TextView(this).apply {
            text = "Offline physics sandbox • Bot selects and plays shots only on this simulated board."
            textSize = 13f
            setTextColor(0xFFB8C3D1.toInt())
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 12)
        })
        val status = TextView(this).apply {
            text = "Ready — tap Auto shot"
            textSize = 14f
            setTextColor(0xFF80D8FF.toInt())
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }
        val board = CarromBoardView(this) { message -> status.text = message }
        root.addView(board, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(status)
        root.addView(Button(this).apply {
            text = "AUTO SHOT"
            setOnClickListener { board.playBestShot() }
        })
        root.addView(Button(this).apply {
            text = "RESET BOARD"
            setOnClickListener { board.resetBoard(); status.text = "Board reset — ready" }
        })
        setContentView(root)
    }
}

private class CarromBoardView(
    context: android.content.Context,
    private val report: (String) -> Unit
) : View(context) {
    private data class Coin(var x: Float, var y: Float, val red: Boolean)
    private val wood = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB87943.toInt() }
    private val surface = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE8C99A.toInt() }
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6D4028.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val dark = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF242424.toInt() }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val black = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF30343B.toInt() }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC83E45.toInt() }
    private val strikerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3B83D5.toInt() }
    private val pockets = listOf(0.07f to 0.07f, 0.93f to 0.07f, 0.07f to 0.93f, 0.93f to 0.93f)
    private val coins = mutableListOf<Coin>()
    private var strikerX = 0.5f
    private var strikerY = 0.86f
    private var busy = false
    private var boardLeft = 0f
    private var boardTop = 0f
    private var boardSize = 0f
    private val radius get() = boardSize * 0.027f

    init { resetBoard() }

    fun resetBoard() {
        coins.clear()
        coins.add(Coin(0.5f, 0.48f, true))
        val offsets = listOf(
            -0.055f to 0f, 0.055f to 0f, -0.027f to -0.048f, 0.027f to -0.048f,
            -0.027f to 0.048f, 0.027f to 0.048f, 0f to -0.096f, 0f to 0.096f,
            -0.082f to -0.048f, 0.082f to 0.048f, 0.082f to -0.048f, -0.082f to 0.048f
        )
        offsets.forEachIndexed { i, p -> coins.add(Coin(0.5f + p.first, 0.48f + p.second, i % 2 == 0)) }
        strikerX = 0.5f; strikerY = 0.86f
        busy = false
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        boardSize = min(width.toFloat() - 28f, height.toFloat() - 28f).coerceAtLeast(100f)
        boardLeft = (width - boardSize) / 2f
        boardTop = (height - boardSize) / 2f
        canvas.drawRoundRect(boardLeft - 12, boardTop - 12, boardLeft + boardSize + 12,
            boardTop + boardSize + 12, 18f, 18f, wood)
        canvas.drawRect(boardLeft, boardTop, boardLeft + boardSize, boardTop + boardSize, surface)
        canvas.drawRect(boardLeft + boardSize * .06f, boardTop + boardSize * .06f,
            boardLeft + boardSize * .94f, boardTop + boardSize * .94f, line)
        canvas.drawCircle(boardLeft + boardSize * .5f, boardTop + boardSize * .5f,
            boardSize * .13f, line)
        pockets.forEach { (x, y) ->
            canvas.drawCircle(boardLeft + x * boardSize, boardTop + y * boardSize,
                boardSize * .055f, dark)
        }
        coins.forEachIndexed { index, coin ->
            val p = if (coin.red) red else if (index % 2 == 0) white else black
            canvas.drawCircle(boardLeft + coin.x * boardSize, boardTop + coin.y * boardSize, radius, p)
            canvas.drawCircle(boardLeft + coin.x * boardSize, boardTop + coin.y * boardSize, radius, line)
        }
        canvas.drawCircle(boardLeft + strikerX * boardSize, boardTop + strikerY * boardSize,
            radius * 1.45f, strikerPaint)
    }

    fun playBestShot() {
        if (busy) return
        if (coins.isEmpty()) { report("All coins pocketed — reset to play again"); return }
        val shot = chooseShot()
        if (shot == null) { report("No clear simulated shot found"); return }
        busy = true
        report("Bot selected a target • playing calculated shot…")
        val startX = strikerX
        val startY = strikerY
        ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 850
            addUpdateListener { animator ->
                val t = animator.animatedValue as Float
                strikerX = startX + (shot.contactX - startX) * t
                strikerY = startY + (shot.contactY - startY) * t
                shot.coin.x = shot.startCoinX + (shot.pocketX - shot.startCoinX) * t
                shot.coin.y = shot.startCoinY + (shot.pocketY - shot.startCoinY) * t
                invalidate()
            }
            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    coins.remove(shot.coin)
                    strikerX = 0.5f
                    strikerY = 0.86f
                    busy = false
                    report("Shot complete • ${coins.size} coins remaining")
                    invalidate()
                }
            })
            start()
        }
    }

    private fun chooseShot(): Shot? {
        var best: Shot? = null
        for (coin in coins) for ((px, py) in pockets) {
            val dx = px - coin.x
            val dy = py - coin.y
            val length = hypot(dx, dy).coerceAtLeast(0.001f)
            val cx = coin.x - dx / length * 0.055f
            val cy = coin.y - dy / length * 0.055f
            if (cx !in 0.08f..0.92f || cy !in 0.08f..0.92f) continue
            val score = hypot(cx - strikerX, cy - strikerY) + hypot(dx, dy) * 0.55f
            if (best == null || score < best!!.score) {
                best = Shot(coin, px, py, cx, cy, score, coin.x, coin.y)
            }
        }
        return best
    }

    private data class Shot(
        val coin: Coin, val pocketX: Float, val pocketY: Float,
        val contactX: Float, val contactY: Float, val score: Float,
        val startCoinX: Float, val startCoinY: Float
    )
}
