package com.example.appmanagement.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmanagement.R
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.repo.TaskRepository
import com.example.appmanagement.databinding.FragmentTodayBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Danh sách công việc trong ngày hiện tại với khả năng kéo thả sắp xếp
class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskRepo: TaskRepository
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    private var isDragging = false
    private var originalAnimator: RecyclerView.ItemAnimator? = null

    // Khởi tạo binding cho layout danh sách hôm nay
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập adapter, kéo thả và quan sát task trong ngày
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContext = requireContext().applicationContext
        val db = AppDatabase.getInstance(appContext)
        val accountRepo = AccountRepository(db.userDao())
        taskRepo = TaskRepository(db.taskDao())

        taskAdapter = TaskAdapter(
            onEditClick = { task ->
                findNavController().navigate(
                    R.id.editFragment,
                    bundleOf("editId" to task.id)
                )
            },
            onDeleteClick = { task ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { taskRepo.delete(task) }
            },
            onCheckClick = { task ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) { taskRepo.toggle(task) }
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
                rv: RecyclerView,
                vh: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val from = vh.bindingAdapterPosition
                val to = target.bindingAdapterPosition
                if (from == RecyclerView.NO_POSITION || to == RecyclerView.NO_POSITION) return false

                taskAdapter.swapItems(from, to)
                taskAdapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) { }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                val pairs = taskAdapter.snapshotIdsWithIndex()
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskRepo.updateOrderMany(pairs)
                }
            }
        }
        ItemTouchHelper(touchCallback).attachToRecyclerView(binding.rvTasks)

        val today = df.format(Calendar.getInstance().time)

        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { accountRepo.getCurrentUser() } ?: return@launch
            taskRepo.byDate(user.id, today).observe(viewLifecycleOwner) { tasks ->
                val sorted = tasks.sortedWith(
                    compareBy<com.example.appmanagement.data.entity.Task> { it.isCompleted }
                        .thenBy { it.orderIndex }
                        .thenByDescending { it.id }
                )
                if (!isDragging) {
                    taskAdapter.submitList(sorted)
                    if (sorted.isNotEmpty()) binding.rvTasks.scrollToPosition(0)
                }
            }
        }
    }

    // Dọn binding khi view bị huỷ
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
