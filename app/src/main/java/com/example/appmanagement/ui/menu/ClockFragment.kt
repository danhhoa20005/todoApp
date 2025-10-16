package com.example.appmanagement.ui.menu

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.appmanagement.databinding.FragmentClockBinding
import kotlin.math.floor

class ClockFragment : Fragment() {

    private var _binding: FragmentClockBinding? = null
    private val binding get() = _binding!!

    // Trạng thái bấm giờ
    private var isRunning = false
    private var startTimeUptime = 0L         // mốc bắt đầu (uptime)
    private var elapsedBeforePause = 0L      // tổng thời gian trước khi pause

    // Cập nhật UI ~60fps
    private val uiHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateElapsedTimeText()
            uiHandler.postDelayed(this, 16L) // ~60fps
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

        // Khôi phục trạng thái (nếu có)
        if (savedInstanceState != null) {
            isRunning = savedInstanceState.getBoolean("isRunning", false)
            startTimeUptime = savedInstanceState.getLong("startTimeUptime", 0L)
            elapsedBeforePause = savedInstanceState.getLong("elapsedBeforePause", 0L)
        }

        // Gán sự kiện
        binding.toggleButton.setOnClickListener {
            if (isRunning) pauseTimer() else startTimer()
        }
        binding.resetButton.setOnClickListener { resetTimer() }

        // Hiển thị ban đầu & đồng bộ nút
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
    }

    private fun resetTimer() {
        isRunning = false
        startTimeUptime = 0L
        elapsedBeforePause = 0L
        uiHandler.removeCallbacks(updateRunnable)
        updateElapsedTimeText()
        syncToggleButton()
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
        val centiseconds = (elapsed % 1000) / 10 // 0..99

        binding.elapsedTimeText.text = String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
    }

    /** Đồng bộ giao diện nút Play/Pause với trạng thái hiện tại */
    private fun syncToggleButton() {
        if (isRunning) {
            binding.toggleButton.text = "Pause"
            binding.toggleButton.setIconResource(android.R.drawable.ic_media_pause)
            binding.toggleButton.contentDescription = "Pause stopwatch"
            // Khi đang chạy có thể vô hiệu hóa Reset nếu muốn:
            // binding.resetButton.isEnabled = false
        } else {
            binding.toggleButton.text = "Play"
            binding.toggleButton.setIconResource(android.R.drawable.ic_media_play)
            binding.toggleButton.contentDescription = "Start stopwatch"
            // binding.resetButton.isEnabled = true
        }
    }

    override fun onPause() {
        super.onPause()
        // Dừng cập nhật để tránh rò rỉ
        uiHandler.removeCallbacks(updateRunnable)
        // Nếu đang chạy, ghi nhận phần đã trôi qua đến lúc onPause
        if (isRunning) {
            elapsedBeforePause += SystemClock.uptimeMillis() - startTimeUptime
        }
    }

    override fun onResume() {
        super.onResume()
        if (isRunning) {
            // Tiếp tục chạy: đặt lại mốc bắt đầu và cập nhật UI
            startTimeUptime = SystemClock.uptimeMillis()
            uiHandler.post(updateRunnable)
        } else {
            updateElapsedTimeText()
        }
        syncToggleButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Lưu trạng thái để xoay màn hình không mất
        outState.putBoolean("isRunning", isRunning)
        outState.putLong("startTimeUptime", startTimeUptime)
        outState.putLong("elapsedBeforePause", elapsedBeforePause)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiHandler.removeCallbacks(updateRunnable)
        _binding = null
    }
}
