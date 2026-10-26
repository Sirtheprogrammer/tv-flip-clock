package com.ambient.flipclock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.ambient.flipclock.view.FlipCardView
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var flipHours: FlipCardView
    private lateinit var flipMinutes: FlipCardView
    private lateinit var flipSeconds: FlipCardView

    private var is24HourFormat: Boolean = false
    private val clockHandler = Handler(Looper.getMainLooper())

    private val clockRunnable = object : Runnable {
        override fun run() {
            updateTime(animate = true)
            // Precise atomic second synchronization (fires on millisecond 0 of every second)
            val now = System.currentTimeMillis()
            val nextDelay = 1000L - (now % 1000L)
            clockHandler.postDelayed(this, nextDelay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)

        flipHours = findViewById(R.id.flip_hours)
        flipMinutes = findViewById(R.id.flip_minutes)
        flipSeconds = findViewById(R.id.flip_seconds)

        // Seconds animation: snappy 340ms flip
        flipSeconds.animationDuration = 340L
        flipMinutes.animationDuration = 450L
        flipHours.animationDuration = 450L

        // Initial setup without animation
        updateTime(animate = false)

        val toggleListener = {
            is24HourFormat = !is24HourFormat
            updateTime(animate = true)
        }
        flipHours.setOnClickListener { toggleListener() }
        flipMinutes.setOnClickListener { toggleListener() }
        flipSeconds.setOnClickListener { toggleListener() }
    }

    override fun onResume() {
        super.onResume()
        clockHandler.post(clockRunnable)
    }

    override fun onPause() {
        super.onPause()
        clockHandler.removeCallbacks(clockRunnable)
    }

    private fun updateTime(animate: Boolean) {
        val calendar = Calendar.getInstance()

        val rawHour = if (is24HourFormat) {
            calendar.get(Calendar.HOUR_OF_DAY)
        } else {
            val h = calendar.get(Calendar.HOUR)
            if (h == 0) 12 else h
        }
        val rawMinute = calendar.get(Calendar.MINUTE)
        val rawSecond = calendar.get(Calendar.SECOND)

        val hourStr = String.format("%02d", rawHour)
        val minStr = String.format("%02d", rawMinute)
        val secStr = String.format("%02d", rawSecond)

        val amPm = if (!is24HourFormat) {
            if (calendar.get(Calendar.AM_PM) == Calendar.AM) "AM" else "PM"
        } else {
            ""
        }

        flipHours.amPmText = amPm
        flipHours.setValue(hourStr, animate = animate)
        flipMinutes.setValue(minStr, animate = animate)
        flipSeconds.setValue(secStr, animate = animate)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                is24HourFormat = !is24HourFormat
                updateTime(animate = true)
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
