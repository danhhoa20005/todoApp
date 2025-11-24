// Fragment HomeFragment tổng hợp thống kê, tìm kiếm và điều hướng nhanh cho bảng điều khiển công việc
package com.example.appmanagement.ui.home

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmanagement.R
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.viewmodel.TaskViewModel
import com.example.appmanagement.databinding.FragmentHomeBinding
import com.example.appmanagement.ui.menu.TaskAdapter
import com.example.appmanagement.util.*
import com.google.android.material.progressindicator.LinearProgressIndicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _b: FragmentHomeBinding? = null
    private val b get() = _b!!

    private val vm: TaskViewModel by activityViewModels()
    private lateinit var searchAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _b = FragmentHomeBinding.inflate(inflater, container, false)
        return b.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        b.tvWeekTitle.text = "Tasks in this week"
        b.progress.isIndeterminate = false
        b.progress.showAnimationBehavior = LinearProgressIndicator.SHOW_NONE

        showCards(true)

        vm.loadTasksForCurrentUser()
        vm.syncTasksForCurrentUser()

        // User + avatar
        viewLifecycleOwner.lifecycleScope.launch {
            val u = withContext(Dispatchers.IO) { vm.getCurrentUserSafe() }
            if (u != null) {
                b.tvHello.text = "${u.name}'s manager"
                when (u.avatarUrl) {
                    "male" -> b.imgAvatar.setImageResource(R.drawable.avatar_male)
                    "female" -> b.imgAvatar.setImageResource(R.drawable.avatar_female)
                    else -> b.imgAvatar.setImageResource(R.drawable.ic_logo)
                }
                b.imgAvatar.setOnClickListener {
                    val action = HomeFragmentDirections
                        .actionHomeFragmentToSettingFragment(userId = u.id)
                    findNavController().navigate(action)
                }
            } else {
                b.tvHello.text = "Guest"
                b.imgAvatar.setImageResource(R.drawable.ic_logo)
            }
        }

        // Quan sát tất cả task để cập nhật thống kê + search
        vm.tasksAll.observe(viewLifecycleOwner) { list ->
            val data = list.orEmpty()

            // 1) Thống kê hôm nay
            applyTodayStats(data)

            // 2) Thống kê đã hoàn thành từ dữ liệu DB (không tự cộng tay)
            val completed = data.filter { it.isCompleted }
            val counts = WeekChart.computeWeekCounts(completed)
            WeekChart.render(
                rowBars = b.rowBars,
                bars = listOf(b.barMon, b.barTue, b.barWed, b.barThu, b.barFri, b.barSat, b.barSun),
                labels = listOf(b.lblMonCount, b.lblTueCount, b.lblWedCount, b.lblThuCount, b.lblFriCount, b.lblSatCount, b.lblSunCount),
                tvEmptyWeek = b.tvEmptyWeek,
                counts = counts,
                onBarClick = { onDayClicked(it) },
                fullScaleCap = 20
            )
            b.tvCompleted.text = completed.size.toString()

            // 3) Cập nhật kết quả tìm kiếm theo text hiện tại
            submitSearch(data, b.etSearch.text?.toString().orEmpty())
        }
        vm.filterByDate(toDayString())

        // Adapter danh sách tìm kiếm (working list)
        searchAdapter = TaskAdapter(
            onEditClick = { task ->
                findNavController().navigate(
                    HomeFragmentDirections.actionHomeFragmentToEditFragment(task.id)
                )
            },
            onDeleteClick = { task ->
                vm.deleteTask(task)
            },
            onCheckClick = { task ->
                // Cập nhật DB; UI sẽ tự đồng bộ lại qua observer ở trên
                vm.toggleTask(task)
            }
        )

        b.rvSearchResults.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = searchAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(8)
        }

        // Tìm kiếm động
        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                submitSearch(vm.tasksAll.value.orEmpty(), s?.toString().orEmpty())
            }
        })

        // Điều hướng nhanh
        b.cardCompleted.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_doneFragment)
        }
        b.cardProgress.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_todayFragment)
        }

        // Back = thoát app
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() { requireActivity().finishAffinity() }
            }
        )
    }

    private fun showCards(show: Boolean) {
        b.cardProgress.isVisible = show
        b.cardWeek.isVisible = show
        b.cardCompleted.isVisible = show
        b.rvSearchResults.isVisible = !show
    }

    private fun submitSearch(all: List<Task>, raw: String) {
        val key = raw.trim().lowercase(Locale.getDefault())
        if (key.isEmpty()) {
            searchAdapter.submitDataOnce(emptyList())
            showCards(true)
            return
        }

        val filtered = all.filter {
            val t = it.title.lowercase()
            val d = it.description.lowercase()
            val day = it.taskDate.lowercase()
            val s = it.startTime.lowercase()
            val e = it.endTime.lowercase()
            t.contains(key) || d.contains(key) || day.contains(key) || s.contains(key) || e.contains(key)
        }

        searchAdapter.submitDataOnce(filtered)
        showCards(false)
    }

    /** Thống kê hôm nay + tổng đã hoàn thành */
    private fun applyTodayStats(all: List<Task>) {
        val today = LocalDate.now()
        val todayTasks = all.filter { it.taskDate.toDateOrNull() == today }
        val todayDone = todayTasks.count { it.isCompleted }
        val todayTotal = todayTasks.size

        b.tvPrioritySub.text = getString(R.string.today_completed_sub, todayDone, todayTotal)

        val percentRaw = if (todayTotal > 0) todayDone * 100.0 / todayTotal else 0.0
        val percent = percentRaw.coerceIn(0.0, 100.0)
        b.progress.setProgressCompat(percent.roundToInt(), false)
        b.tvPercent.text = getString(R.string.percent_format, percent)
    }

    private fun onDayClicked(index: Int) {
        // Điều hướng theo ngày nếu cần
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _b = null
    }
}
