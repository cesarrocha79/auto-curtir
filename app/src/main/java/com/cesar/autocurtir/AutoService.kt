package com.cesar.autocurtir

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.LinearLayout
import android.widget.TextView
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AutoService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: AutoService? = null
    }

    private val ui = Handler(Looper.getMainLooper())
    private var wm: WindowManager? = null
    private var panel: LinearLayout? = null
    private var statusTv: TextView? = null
    private var playTv: TextView? = null

    @Volatile
    private var running = false
    private var worker: Thread? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        stopRun()
    }

    override fun onDestroy() {
        stopRun()
        hidePanel()
        instance = null
        super.onDestroy()
    }

    // ---------- Painel flutuante ----------

    fun showPanel() {
        if (panel != null) return
        val manager = getSystemService(WINDOW_SERVICE) as WindowManager
        wm = manager
        val density = resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(26).toFloat()
                setColor(Color.argb(230, 32, 32, 32))
            }
        }

        val handle = TextView(this).apply {
            text = "⠿"
            textSize = 24f
            setTextColor(Color.WHITE)
            setPadding(dp(6), dp(4), dp(10), dp(4))
        }
        val st = TextView(this).apply {
            text = "Parado"
            textSize = 14f
            setTextColor(Color.WHITE)
            setPadding(dp(4), 0, dp(10), 0)
        }
        val play = TextView(this).apply {
            text = "▶"
            textSize = 28f
            setTextColor(Color.WHITE)
            setPadding(dp(10), dp(2), dp(10), dp(2))
        }
        val close = TextView(this).apply {
            text = "✕"
            textSize = 20f
            setTextColor(Color.LTGRAY)
            setPadding(dp(10), dp(4), dp(6), dp(4))
        }

        row.addView(handle)
        row.addView(st)
        row.addView(play)
        row.addView(close)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = dp(16)
            y = dp(120)
        }

        var startX = 0
        var startY = 0
        var touchX = 0f
        var touchY = 0f
        handle.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    startX = params.x
                    startY = params.y
                    touchX = e.rawX
                    touchY = e.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = startX + (e.rawX - touchX).toInt()
                    params.y = startY + (e.rawY - touchY).toInt()
                    manager.updateViewLayout(row, params)
                    true
                }
                else -> false
            }
        }

        play.setOnClickListener {
            if (running) stopRun() else startRun()
        }
        close.setOnClickListener {
            stopRun()
            hidePanel()
        }

        manager.addView(row, params)
        panel = row
        statusTv = st
        playTv = play
    }

    private fun hidePanel() {
        val p = panel ?: return
        try {
            wm?.removeView(p)
        } catch (_: Exception) {
        }
        panel = null
        statusTv = null
        playTv = null
    }

    private fun setStatus(text: String) {
        ui.post { statusTv?.text = text }
    }

    private fun refreshPlayButton() {
        ui.post { playTv?.text = if (running) "⏹" else "▶" }
    }

    // ---------- Loop de automação ----------

    private fun startRun() {
        if (running) return
        running = true
        refreshPlayButton()
        val cfg = Config.load(this)
        val t = Thread { runLoop(cfg) }
        worker = t
        t.start()
    }

    private fun stopRun() {
        running = false
        worker?.interrupt()
        refreshPlayButton()
    }

    private fun runLoop(cfg: Config) {
        val dm = resources.displayMetrics
        val w = dm.widthPixels
        val h = dm.heightPixels
        try {
            var videos = 0
            while (running && (cfg.maxVideos == 0 || videos < cfg.maxVideos)) {
                videos++
                for (i in 1..cfg.taps) {
                    if (!running) break
                    val x = w / 2f + rnd(-40, 40)
                    val y = h / 2f + rnd(-60, 60)
                    tap(x, y)
                    setStatus("Vídeo $videos • $i/${cfg.taps}")
                    sleepJitter(cfg.tapIntervalMs)
                }
                if (!running) break
                swipeUp(w, h)
                setStatus("Vídeo $videos • rolando")
                sleepJitter(cfg.waitAfterSwipeMs)
            }
        } catch (_: InterruptedException) {
        } finally {
            running = false
            refreshPlayButton()
            setStatus("Parado")
        }
    }

    private fun rnd(min: Int, max: Int): Int = min + (Math.random() * (max - min)).toInt()

    private fun sleepJitter(ms: Int) {
        val v = ms * (0.75 + Math.random() * 0.5)
        Thread.sleep(v.toLong())
    }

    private fun tap(x: Float, y: Float) {
        val path = Path().apply { moveTo(x, y) }
        gesture(path, 40)
    }

    private fun swipeUp(w: Int, h: Int) {
        val x = w / 2f + rnd(-30, 30)
        val path = Path().apply {
            moveTo(x, h * 0.78f)
            lineTo(x, h * 0.25f)
        }
        gesture(path, 260)
    }

    private fun gesture(path: Path, durationMs: Long) {
        val latch = CountDownLatch(1)
        val g = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, durationMs))
            .build()
        val accepted = dispatchGesture(g, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                latch.countDown()
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                latch.countDown()
            }
        }, null)
        if (accepted) latch.await(3, TimeUnit.SECONDS)
    }
}
