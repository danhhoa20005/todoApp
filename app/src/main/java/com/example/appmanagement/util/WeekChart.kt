// Đối tượng WeekChart xử lý thống kê số lượng công việc theo tuần và dựng biểu đồ cột tương ứng
package com.example.appmanagement.util

import android.animation.ValueAnimator
import android.view.View
import android.widget.TextView
import androidx.core.view.isVisible
import com.example.appmanagement.data.entity.Task
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.max

object WeekChart {

    /** Đếm task tuần hiện tại (T2..CN). */
    fun computeWeekCounts(all: List<Task>, today: LocalDate = LocalDate.now()): IntArray {
        val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val end = start.plusDays(6)
        val out = IntArray(7)
        all.forEach { t ->
            val d = t.taskDate.toDateOrNull() ?: return@forEach
            if (d.isBefore(start) || d.isAfter(end)) return@forEach
            when (d.dayOfWeek) {
                DayOfWeek.MONDAY    -> out[0]++
                DayOfWeek.TUESDAY   -> out[1]++
                DayOfWeek.WEDNESDAY -> out[2]++
                DayOfWeek.THURSDAY  -> out[3]++
                DayOfWeek.FRIDAY    -> out[4]++
                DayOfWeek.SATURDAY  -> out[5]++
                DayOfWeek.SUNDAY    -> out[6]++
            }
        }
        return out
    }

    /**
     * Vẽ cột (View): ẩn khi = 0, cao tỉ lệ. fullScaleCap mặc định = 30 task.
     */
    fun render(
        rowBars: View,
        bars: List<View>,
        labels: List<TextView>,
        tvEmptyWeek: TextView,
        counts: IntArray,
        onBarClick: ((Int) -> Unit)? = null,
        fullScaleCap: Int? = 30
    ) {
        rowBars.post {
            tvEmptyWeek.isVisible = counts.sum() == 0
            labels.forEachIndexed { i, tv -> tv.text = counts[i].toString() }

            val zoneH = rowBars.height
            val usableH = (zoneH - dp(rowBars, 8)).coerceAtLeast(0)

            val dynamicMax = counts.maxOrNull() ?: 0
            val scaleMax = when {
                fullScaleCap != null -> max(1, fullScaleCap) // 30 = full cột
                dynamicMax == 0      -> 1
                else                 -> dynamicMax
            }

            bars.forEachIndexed { i, v ->
                val c = counts[i]
                if (c <= 0) {
                    v.visibility = View.INVISIBLE
                } else {
                    v.visibility = View.VISIBLE
                    val target = (c.coerceAtMost(scaleMax).toFloat() / scaleMax * usableH)
                        .toInt()
                        .coerceAtLeast(dp(rowBars, 8))
                    animateHeight(v, target)
                }
                v.setOnClickListener { onBarClick?.invoke(i) }
                v.contentDescription = "Ngày ${i + 2}, $c công việc"
            }
        }
    }

    private fun animateHeight(v: View, target: Int) {
        val start = v.height
        if (start == target) return
        ValueAnimator.ofInt(start, target).apply {
            duration = 350
            addUpdateListener {
                val h = it.animatedValue as Int
                v.layoutParams = v.layoutParams.apply { height = h }
                v.requestLayout()
            }
            start()
        }
    }

    private fun dp(anchor: View, value: Int): Int =
        (value * anchor.resources.displayMetrics.density).toInt()
}
