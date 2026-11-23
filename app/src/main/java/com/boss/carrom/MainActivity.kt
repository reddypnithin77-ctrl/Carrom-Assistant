package com.boss.carrom

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28, 28, 28, 28)
            setBackgroundColor(0xFF171717.toInt())
        }
        root.addView(TextView(this).apply {
            text = "BOSS • CARROM ASSISTANT\n\nCarrom practice and simulation"
            textSize = 23f
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(8, 8, 8, 30)
        })
        root.addView(TextView(this).apply {
            text = "Practice modes\n• Own colour only\n• Own colour + queen\n• Free style\n\nThe floating guide lets you position the aiming line manually and calculates its angle live in degrees."
            textSize = 16f
            setTextColor(0xFFE0E0E0.toInt())
            setPadding(8, 8, 8, 24)
        })
        root.addView(Button(this).apply {
            text = "Open automatic carrom simulator"
            setOnClickListener { startActivity(Intent(this@MainActivity, CarromSimulatorActivity::class.java)) }
        })
        root.addView(Button(this).apply {
            text = "Enable floating aiming guide"
            setOnClickListener {
                if (!Settings.canDrawOverlays(this@MainActivity)) {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                } else {
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                }
            }
        })
        root.addView(Button(this).apply {
            text = "Start guide"
            setOnClickListener {
                if (Settings.canDrawOverlays(this@MainActivity)) {
                    startService(Intent(this@MainActivity, OverlayService::class.java))
                } else {
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
                }
            }
        })
        setContentView(root)
    }
}
