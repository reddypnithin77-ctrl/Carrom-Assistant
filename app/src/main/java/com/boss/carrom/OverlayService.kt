package com.boss.carrom

import android.app.Service
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView

class OverlayService : Service() {
    private lateinit var wm: WindowManager
    private var panel: View? = null
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (panel != null) return START_STICKY
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val layout = WindowManager.LayoutParams(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT).apply { gravity = Gravity.TOP or Gravity.START; x = 30; y = 180 }
        val box = android.widget.LinearLayout(this).apply { orientation = android.widget.LinearLayout.VERTICAL; setPadding(18, 12, 18, 12); setBackgroundColor(0xDD202020.toInt()) }
        val title = TextView(this).apply { text = "BOSS  •  AIM GUIDE"; textSize = 14f; setTextColor(Color.WHITE); setPadding(8, 8, 8, 8) }
        val line = AimLineView(this).apply { minimumWidth = 230; minimumHeight = 160 }
        val close = TextView(this).apply { text = "Close guide"; textSize = 14f; setTextColor(0xFFFF7777.toInt()); setPadding(8, 8, 8, 8); setOnClickListener { stopSelf() } }
        box.addView(title); box.addView(line); box.addView(close)
        title.setOnTouchListener(object : View.OnTouchListener { var sx=0f; var sy=0f; var ox=0; var oy=0
            override fun onTouch(v: View, e: MotionEvent): Boolean { when(e.action) { MotionEvent.ACTION_DOWN -> { sx=e.rawX; sy=e.rawY; ox=layout.x; oy=layout.y; return true }; MotionEvent.ACTION_MOVE -> { layout.x=ox+(e.rawX-sx).toInt(); layout.y=oy+(e.rawY-sy).toInt(); wm.updateViewLayout(box, layout); return true } }; return false }
        })
        panel = box; wm.addView(box, layout); return START_STICKY
    }
    override fun onDestroy() { panel?.let { wm.removeView(it) }; panel = null; super.onDestroy() }
}

private class AimLineView(context: android.content.Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFF5252.toInt(); strokeWidth = 5f; style = Paint.Style.STROKE }
    private var endX = 190f; private var endY = 30f
    override fun onDraw(canvas: Canvas) { super.onDraw(canvas); canvas.drawLine(width * .12f, height * .82f, endX.coerceIn(0f,width.toFloat()), endY.coerceIn(0f,height.toFloat()), paint); paint.style=Paint.Style.FILL; canvas.drawCircle(endX.coerceIn(0f,width.toFloat()), endY.coerceIn(0f,height.toFloat()), 7f, paint); paint.style=Paint.Style.STROKE }
    override fun onTouchEvent(event: MotionEvent): Boolean { when(event.action) { MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> { endX=event.x; endY=event.y; invalidate(); return true } }; return true }
}
