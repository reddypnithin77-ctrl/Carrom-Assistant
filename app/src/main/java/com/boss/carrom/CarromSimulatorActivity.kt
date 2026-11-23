package com.boss.carrom

import android.app.Activity
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.min

class CarromSimulatorActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(14, 12, 14, 12)
            setBackgroundColor(0xFF171B22.toInt())
        }
        root.addView(TextView(this).apply {
            text = "MANUAL AIM PRACTICE"
            textSize = 21f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(4, 8, 4, 6)
        })
        root.addView(TextView(this).apply {
            text = "Touch the blue striker and drag backward to set your own aim. This is an offline practice board; no automatic shots."
            textSize = 13f
            setTextColor(0xFFB8C3D1.toInt())
            gravity = Gravity.CENTER
            setPadding(8, 4, 8, 10)
        })
        val status = TextView(this).apply {
            text = "Drag from the blue striker to aim"
            textSize = 14f
            setTextColor(0xFF80D8FF.toInt())
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 8)
        }
        val board = CarromBoardView(this) { status.text = it }
        root.addView(board, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(status)
        root.addView(Button(this).apply {
            text = "RESET PRACTICE BOARD"
            setOnClickListener { board.resetBoard(); status.text = "Board reset — drag from the striker to aim" }
        })
        setContentView(root)
    }
}

private class CarromBoardView(
    context: android.content.Context,
    private val report: (String) -> Unit
) : View(context) {
    private data class Coin(val x: Float, val y: Float, val red: Boolean)

    private val wood = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFB87943.toInt() }
    private val surface = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFE8C99A.toInt() }
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF6D4028.toInt(); style = Paint.Style.STROKE; strokeWidth = 3f
    }
    private val pocketPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF242424.toInt() }
    private val white = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val black = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF30343B.toInt() }
    private val red = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFC83E45.toInt() }
    private val strikerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF287ED1.toInt() }
    private val aimPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE; strokeWidth = 3.5f; style = Paint.Style.STROKE
    }
    private val dottedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFB9E8FF.toInt(); strokeWidth = 4f; style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(2f, 9f), 0f)
    }
    private val impactPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF1687D9.toInt(); strokeWidth = 3f; style = Paint.Style.STROKE
    }
    private val pockets = listOf(0.07f to 0.07f, 0.93f to 0.07f, 0.07f to 0.93f, 0.93f to 0.93f)
    private val coins = mutableListOf<Coin>()
    private var strikerX = 0.5f
    private var strikerY = 0.86f
    private var dragging = false
    private var dragX = 0f
    private var dragY = 0f
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
        offsets.forEachIndexed { i, p ->
            coins.add(Coin(0.5f + p.first, 0.48f + p.second, i % 2 == 0))
        }
        strikerX = 0.5f
        strikerY = 0.86f
        dragging = false
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
            boardLeft + boardSize * .94f, boardTop + boardSize * .94f, border)
        canvas.drawCircle(boardLeft + boardSize * .5f, boardTop + boardSize * .5f,
            boardSize * .13f, border)
        pockets.forEach { (x, y) ->
            canvas.drawCircle(boardLeft + x * boardSize, boardTop + y * boardSize,
                boardSize * .055f, pocketPaint)
        }
        coins.forEachIndexed { index, coin ->
            val x = boardLeft + coin.x * boardSize
            val y = boardTop + coin.y * boardSize
            val paint = if (coin.red) red else if (index % 2 == 0) white else black
            canvas.drawCircle(x, y, radius, paint)
            canvas.drawCircle(x, y, radius, border)
        }

        val sx = boardLeft + strikerX * boardSize
        val sy = boardTop + strikerY * boardSize
        if (dragging) drawAimGuide(canvas, sx, sy)
        canvas.drawCircle(sx, sy, radius * 1.45f, strikerPaint)
        canvas.drawCircle(sx, sy, radius * 1.45f, border)
    }

    private fun drawAimGuide(canvas: Canvas, sx: Float, sy: Float) {
        var dx = sx - dragX
        var dy = sy - dragY
        val length = hypot(dx, dy).coerceAtLeast(1f)
        dx /= length
        dy /= length

        val left = boardLeft + boardSize * 0.06f
        val right = boardLeft + boardSize * 0.94f
        val top = boardTop + boardSize * 0.06f
        val bottom = boardTop + boardSize * 0.94f
        var edgeT = Float.POSITIVE_INFINITY
        if (dx > 0f) edgeT = min(edgeT, (right - sx) / dx)
        if (dx < 0f) edgeT = min(edgeT, (left - sx) / dx)
        if (dy > 0f) edgeT = min(edgeT, (bottom - sy) / dy)
        if (dy < 0f) edgeT = min(edgeT, (top - sy) / dy)
        if (!edgeT.isFinite() || edgeT <= 0f) return

        var hitT = Float.POSITIVE_INFINITY
        var hitX = 0f
        var hitY = 0f
        for (coin in coins) {
            val cx = boardLeft + coin.x * boardSize
            val cy = boardTop + coin.y * boardSize
            val vx = cx - sx
            val vy = cy - sy
            val projection = vx * dx + vy * dy
            if (projection <= radius) continue
            val perpendicular = abs(vx * dy - vy * dx)
            if (perpendicular <= radius * 1.8f && projection < hitT) {
                hitT = projection
                hitX = cx
                hitY = cy
            }
        }
        val guideEnd = if (hitT.isFinite()) hitT else edgeT
        val ex = sx + dx * guideEnd
        val ey = sy + dy * guideEnd
        canvas.drawLine(sx, sy, ex, ey, aimPaint)
        if (hitT.isFinite()) {
            canvas.drawCircle(hitX, hitY, radius * 1.8f, impactPaint)
            canvas.drawLine(ex, ey, sx + dx * edgeT, sy + dy * edgeT, dottedPaint)
        } else {
            canvas.drawLine(sx + dx * (guideEnd * 0.65f), sy + dy * (guideEnd * 0.65f),
                sx + dx * edgeT, sy + dy * edgeT, dottedPaint)
        }
        canvas.drawCircle(dragX, dragY, 7f, impactPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val sx = boardLeft + strikerX * boardSize
        val sy = boardTop + strikerY * boardSize
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                if (hypot(event.x - sx, event.y - sy) > radius * 3.5f) return false
                dragging = true
                dragX = event.x
                dragY = event.y
                parent?.requestDisallowInterceptTouchEvent(true)
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (!dragging) return false
                dragX = event.x
                dragY = event.y
                val dx = sx - dragX
                val dy = sy - dragY
                val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
                val pull = hypot(dx, dy) / boardSize * 100f
                report("Aim angle: %.1f°   •   Pull: %.1f%%".format(angle, pull))
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (!dragging) return false
                dragging = false
                report("Aim set — drag the striker again to adjust")
                invalidate()
                return true
            }
        }
        return true
    }
}
