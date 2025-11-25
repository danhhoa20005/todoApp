// Fragment CalendarFragment hiển thị lịch và danh sách công việc theo ngày với khả năng kéo thả sắp xếp
package com.example.appmanagement.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.remote.TaskRemoteDataSource
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.repo.TaskRepository
import com.example.appmanagement.util.NetworkChecker
import com.example.appmanagement.util.NetworkUtils
import com.google.firebase.firestore.FirebaseFirestore
import com.example.appmanagement.databinding.FragmentCalendarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    // Định dạng ngày đồng bộ DB
    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // Ngày đang chọn (mặc định: hôm nay)
    private var selectedDate: String = dateFormatter.format(Date())

    // Repo
    private lateinit var accountRepository: AccountRepository
    private lateinit var taskRepository: TaskRepository

    // Adapter hiển thị task (working list)
    private lateinit var taskAdapter: TaskAdapter

    // LiveData nguồn hiện tại (để gỡ khi đổi ngày)
    private var currentLiveDataSource: LiveData<List<Task>>? = null

    // Trạng thái kéo + animator gốc
    private var isDragging = false
    private var originalAnimator: RecyclerView.ItemAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo DB + Repo
        val database = AppDatabase.getInstance(requireContext().applicationContext)
        accountRepository = AccountRepository(database.userDao())
        taskRepository = TaskRepository(
            database.taskDao(),
            database.userDao(),
            TaskRemoteDataSource(FirebaseFirestore.getInstance()),
            NetworkChecker { NetworkUtils.isOnline(requireContext().applicationContext) }
        )

        // Khởi tạo adapter (đã là working list)
        taskAdapter = TaskAdapter(
            onEditClick = { /* mở Edit nếu cần */ },
            onDeleteClick = { task ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskRepository.delete(task)
                }
            },
            onCheckClick = { task ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskRepository.toggle(task)
                }
            }
        )

        // RecyclerView
        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(8)
            originalAnimator = itemAnimator
        }

        // Kéo–thả sắp xếp + lưu order_index
        val touchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun isLongPressDragEnabled() = true

            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    isDragging = true
                    // Tắt animator để tránh khựng khi kéo dài
                    binding.rvTasks.itemAnimator = null
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    isDragging = false
                    // Bật lại animator sau khi thả
                    binding.rvTasks.itemAnimator = originalAnimator
                }
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = viewHolder.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false

                // Đổi thứ tự trong working list và báo UI
                taskAdapter.swapItems(from, to)
                taskAdapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
                // Lưu thứ tự mới về DB từ working list
                val idIndexPairs = taskAdapter.snapshotIdsWithIndex()
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskRepository.updateOrderMany(idIndexPairs)
                }
            }
        }
        ItemTouchHelper(touchCallback).apply {
            attachToRecyclerView(binding.rvTasks)
            taskAdapter.dragHelper = this
        }

        // Mặc định nạp hôm nay
        binding.calendarView.setDate(System.currentTimeMillis(), false, true)
        loadTasksByDate(selectedDate)

        // Đổi ngày → nạp lại
        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            selectedDate = String.format("%02d/%02d/%04d", day, month + 1, year) // month: 0-based
            loadTasksByDate(selectedDate)
        }
    }

    /**
     * Lấy danh sách task theo ngày và sắp xếp:
     * - chưa hoàn thành trước
     * - theo orderIndex tăng dần
     * - id giảm dần
     */
    private fun loadTasksByDate(date: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() } ?: return@launch

            // Gỡ observer cũ để tránh chồng dữ liệu
            currentLiveDataSource?.removeObservers(viewLifecycleOwner)

            // Lấy LiveData theo ngày từ Repo
            val liveDataSource = taskRepository.byDate(user.id, date)
            currentLiveDataSource = liveDataSource

            liveDataSource.observe(viewLifecycleOwner) { rawList ->
                val resultList = rawList.sortedWith(
                    compareBy<Task> { it.isCompleted }      // false trước
                        .thenBy { it.orderIndex }           // theo thứ tự kéo–thả
                        .thenByDescending { it.id }         // mới tạo trước
                )
                // Không cập nhật khi đang kéo để tránh khựng
                if (!isDragging) {
                    taskAdapter.submitDataOnce(resultList)
                    if (resultList.isNotEmpty()) binding.rvTasks.scrollToPosition(0)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        currentLiveDataSource?.removeObservers(viewLifecycleOwner)
        _binding = null
    }
}
