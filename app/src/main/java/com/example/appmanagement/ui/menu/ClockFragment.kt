package com.example.appmanagement.ui.menu

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.appmanagement.R
import com.example.appmanagement.databinding.FragmentClockBinding
import kotlin.math.floor

class ClockFragment : Fragment() {

    private var _binding: FragmentClockBinding? = null
    private val binding get() = _binding!!

    // stopwatch state
    private var isRunning = false
    private var startTimeUptime = 0L            // uptime lúc bấm "Play" gần nhất
    private var elapsedBeforePause = 0L         // tổng ms đã chạy trước lần Play hiện tại
    private var lapCount = 0                    // số mốc đã lưu

    // handler để update UI ~60fps
    private val uiHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateElapsedTimeText()
            uiHandler.postDelayed(this, 16L) // ~60fps
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClockBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khôi phục trạng thái sau rotate
        if (savedInstanceState != null) {
            isRunning = savedInstanceState.getBoolean(KEY_IS_RUNNING, false)
            startTimeUptime = savedInstanceState.getLong(KEY_START_TIME, 0L)
            elapsedBeforePause = savedInstanceState.getLong(KEY_ELAPSED_BEFORE_PAUSE, 0L)
            lapCount = savedInstanceState.getInt(KEY_LAP_COUNT, 0)
        }

        // Nút play/pause
        binding.toggleButton.setOnClickListener {
            if (isRunning) {
                pauseTimer()
            } else {
                startTimer()
            }
        }

        // Nút reset
        binding.resetButton.setOnClickListener {
            resetTimer()
        }

        // Đồng bộ UI ban đầu
        updateElapsedTimeText()
        syncToggleButton()
    }

    // region Stopwatch core

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

        // Gom thời gian vừa chạy vào tổng
        elapsedBeforePause += (SystemClock.uptimeMillis() - startTimeUptime)

        // Ngưng update UI
        uiHandler.removeCallbacks(updateRunnable)
        syncToggleButton()

        // Lưu mốc (lap)
        val snapshot = binding.elapsedTimeText.text?.toString().orEmpty()
        if (snapshot.isNotBlank()) {
            lapCount += 1
            addLapRow(lapCount, snapshot)
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

        // Xoá toàn bộ lap
        binding.lapsContainer?.removeAllViews()
    }

    private fun updateElapsedTimeText() {
        // Thời gian đã trôi qua = tổng cũ + (now - start) nếu đang chạy
        val elapsedNow = if (isRunning) {
            elapsedBeforePause + (SystemClock.uptimeMillis() - startTimeUptime)
        } else {
            elapsedBeforePause
        }

        val totalSeconds = floor(elapsedNow / 1000.0).toLong()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val centiseconds = (elapsedNow % 1000) / 10 // phần trăm giây (00-99)

        binding.elapsedTimeText.text = String.format(
            "%02d:%02d.%02d",
            minutes,
            seconds,
            centiseconds
        )
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

    // endregion

    // region Lap list

    /**
     * Thêm 1 dòng mốc thời gian kiểu:
     * Time 01        00:30.24
     * -----------
     *
     * Giờ ta inflate từ XML (item_lap_row.xml) để:
     * - tự dùng @color/... => dark/light ok
     * - dễ chỉnh style
     */
    private fun addLapRow(index: Int, timeText: String) {
        val container = binding.lapsContainer ?: return
        val inflater = LayoutInflater.from(requireContext())

        // inflate layout 1 lap
        val rowView = inflater.inflate(R.layout.item_lap_row, container, false)

        val tvLapLabel = rowView.findViewById<TextView>(R.id.tvLapLabel)
        val tvLapValue = rowView.findViewById<TextView>(R.id.tvLapValue)

        tvLapLabel.text = String.format("Time %02d", index)
        tvLapValue.text = timeText

        container.addView(rowView)

        // Giới hạn số dòng nếu bạn muốn tránh vô hạn (ví dụ giữ tối đa 30 mốc)
        val maxChildren = 30
        if (container.childCount > maxChildren) {
            container.removeViewAt(0)
        }
    }

    // endregion

    // region Lifecycle & state keep

    override fun onPause() {
        super.onPause()
        // luôn bỏ callback khi fragment background để tránh leak handler
        uiHandler.removeCallbacks(updateRunnable)

        // Nếu đang chạy, chốt thời gian vào elapsedBeforePause
        if (isRunning) {
            elapsedBeforePause += (SystemClock.uptimeMillis() - startTimeUptime)
        }
    }

    override fun onResume() {
        super.onResume()
        // Khi quay lại: nếu đang chạy thì thiết lập lại startTimeUptime và resume tick
        if (isRunning) {
            startTimeUptime = SystemClock.uptimeMillis()
            uiHandler.post(updateRunnable)
        } else {
            // Nếu không chạy thì chỉ cần đồng bộ text
            updateElapsedTimeText()
        }
        syncToggleButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_RUNNING, isRunning)
        outState.putLong(KEY_START_TIME, startTimeUptime)
        outState.putLong(KEY_ELAPSED_BEFORE_PAUSE, elapsedBeforePause)
        outState.putInt(KEY_LAP_COUNT, lapCount)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        uiHandler.removeCallbacks(updateRunnable)
        _binding = null
    }

    // endregion

    companion object {
        private const val KEY_IS_RUNNING = "isRunning"
        private const val KEY_START_TIME = "startTimeUptime"
        private const val KEY_ELAPSED_BEFORE_PAUSE = "elapsedBeforePause"
        private const val KEY_LAP_COUNT = "lapCount"
    }
}
