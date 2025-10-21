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
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.repo.TaskRepository
import com.example.appmanagement.databinding.FragmentCalendarBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Màn hình lịch cho phép xem và sắp xếp công việc theo ngày cụ thể
class CalendarFragment : Fragment() {

    private var _binding: FragmentCalendarBinding? = null
    private val binding get() = _binding!!

    private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedDate: String = dateFormatter.format(Date())

    private lateinit var accountRepository: AccountRepository
    private lateinit var taskRepository: TaskRepository

    private lateinit var taskAdapter: TaskAdapter

    private var currentLiveDataSource: LiveData<List<Task>>? = null

    private var isDragging = false
    private var originalAnimator: RecyclerView.ItemAnimator? = null

    // Khởi tạo binding cho layout lịch
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCalendarBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập adapter, kéo thả và phản hồi chọn ngày khi view được tạo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = AppDatabase.getInstance(requireContext().applicationContext)
        accountRepository = AccountRepository(database.userDao())
        taskRepository = TaskRepository(database.taskDao())

        taskAdapter = TaskAdapter(
            onEditClick = { },
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

        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(8)
            originalAnimator = itemAnimator
        }

        val touchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun isLongPressDragEnabled() = true

            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    isDragging = true
                    binding.rvTasks.itemAnimator = null
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    isDragging = false
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

                taskAdapter.swapItems(from, to)
                taskAdapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) { }

            override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
                super.clearView(recyclerView, viewHolder)
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

        binding.calendarView.setDate(System.currentTimeMillis(), false, true)
        loadTasksByDate(selectedDate)

        binding.calendarView.setOnDateChangeListener { _, year, month, day ->
            selectedDate = String.format("%02d/%02d/%04d", day, month + 1, year)
            loadTasksByDate(selectedDate)
        }
    }

    // Nạp danh sách task theo ngày, sắp xếp và gỡ observer cũ nếu có
    private fun loadTasksByDate(date: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() } ?: return@launch

            currentLiveDataSource?.removeObservers(viewLifecycleOwner)

            val liveDataSource = taskRepository.byDate(user.id, date)
            currentLiveDataSource = liveDataSource

            liveDataSource.observe(viewLifecycleOwner) { rawList ->
                val resultList = rawList.sortedWith(
                    compareBy<Task> { it.isCompleted }
                        .thenBy { it.orderIndex }
                        .thenByDescending { it.id }
                )
                if (!isDragging) {
                    taskAdapter.submitList(resultList)
                    if (resultList.isNotEmpty()) binding.rvTasks.scrollToPosition(0)
                }
            }
        }
    }

    // Dọn binding và bỏ đăng ký observer khi view bị huỷ
    override fun onDestroyView() {
        super.onDestroyView()
        currentLiveDataSource?.removeObservers(viewLifecycleOwner)
        _binding = null
    }
}
