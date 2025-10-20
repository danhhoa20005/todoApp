// Fragment ClockFragment triển khai đồng hồ bấm giờ với chức năng lưu mốc và khôi phục trạng thái
package com.example.appmanagement.ui.menu

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.appmanagement.databinding.FragmentClockBinding
import kotlin.math.floor

class ClockFragment : Fragment() {

    private var _binding: FragmentClockBinding? = null
    private val binding get() = _binding!!

    private var isRunning = false
    private var startTimeUptime = 0L
    private var elapsedBeforePause = 0L
    private var lapCount = 0

    private val uiHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateElapsedTimeText()
            uiHandler.postDelayed(this, 16L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState != null) {
            isRunning = savedInstanceState.getBoolean("isRunning", false)
            startTimeUptime = savedInstanceState.getLong("startTimeUptime", 0L)
            elapsedBeforePause = savedInstanceState.getLong("elapsedBeforePause", 0L)
            lapCount = savedInstanceState.getInt("lapCount", 0)
        }

        binding.toggleButton.setOnClickListener {
            if (isRunning) pauseTimer() else startTimer()
        }

        binding.resetButton.setOnClickListener { resetTimer() }

        updateElapsedTimeText()
        syncToggleButton()
    }

    private fun startTimer() {
        if (isRunning) return
        isRunning = true
        startTimeUptime = SystemClock.uptimeMillis()
        uiHandler.post(updateRunnable)
        syncToggleButton()
    }

    private fun pauseTimer() {
        if (!isRunning) return
        isRunning = false
        elapsedBeforePause += SystemClock.uptimeMillis() - startTimeUptime
        uiHandler.removeCallbacks(updateRunnable)
        syncToggleButton()

        // Lưu mốc thời gian
        val lapText = binding.elapsedTimeText.text?.toString().orEmpty()
        if (lapText.isNotBlank()) {
            lapCount += 1
            addLapRow(lapCount, lapText)
        }
    }

    private fun resetTimer() {
        isRunning = false
        startTimeUptime = 0L
        elapsedBeforePause = 0L
        lapCount = 0
        uiHandler.removeCallbacks(updateRunnable)
        updateElapsedTimeText()
        syncToggleButton()
        binding.lapsContainer?.removeAllViews()
    }

    private fun updateElapsedTimeText() {
        val elapsed = if (isRunning) {
            elapsedBeforePause + (SystemClock.uptimeMillis() - startTimeUptime)
        } else {
            elapsedBeforePause
        }

        val totalSeconds = floor(elapsed / 1000.0).toLong()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = (elapsed % 1000) / 10

        binding.elapsedTimeText.text = String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
    }

    private fun syncToggleButton() {
        if (isRunning) {
            binding.toggleButton.text = "Pause"
            binding.toggleButton.setIconResource(android.R.drawable.ic_media_pause)
        } else {
            binding.toggleButton.text = "Play"
            binding.toggleButton.setIconResource(android.R.drawable.ic_media_play)
        }
    }

    /** Thêm 1 dòng mốc thời gian chuyên nghiệp: Time 01 | 00:30.24 */
    private fun addLapRow(index: Int, timeText: String) {
        val ctx = context ?: return
        val container = binding.lapsContainer ?: return

        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val left = TextView(ctx).apply {
            text = String.format("Time %02d", index)
            setTextColor(Color.parseColor("#CCFFFFFF"))
            textSize = 14f
        }

        val right = TextView(ctx).apply {
            text = timeText
            setTextColor(Color.WHITE)
            textSize = 18f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }

        val leftParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        row.addView(left, leftParams)
        row.addView(right)

        // Divider mảnh
        val divider = View(ctx).apply {
            setBackgroundColor(Color.parseColor("#22FFFFFF"))
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)
            )
        }

        container.addView(row, 0)
        container.addView(divider, 1)

        if (container.childCount > 60) container.removeViews(container.childCount - 2, 2)
    }

    private fun dp(v: Int): Int =
        (v * resources.displayMetrics.density).toInt()

    override fun onPause() {
        super.onPause()
        uiHandler.removeCallbacks(updateRunnable)
        if (isRunning) {
            elapsedBeforePause += SystemClock.uptimeMillis() - startTimeUptime
        }
    }

    override fun onResume() {
        super.onResume()
        if (isRunning) {
            startTimeUptime = SystemClock.uptimeMillis()
            uiHandler.post(updateRunnable)
        } else {
            updateElapsedTimeText()
        }
        syncToggleButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isRunning", isRunning)
        outState.putLong("startTimeUptime", startTimeUptime)
        outState.putLong("elapsedBeforePause", elapsedBeforePause)
        outState.putInt("lapCount", lapCount)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiHandler.removeCallbacks(updateRunnable)
        _binding = null
    }
}
