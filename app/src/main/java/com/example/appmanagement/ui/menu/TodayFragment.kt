// Fragment TodayFragment tập trung vào danh sách công việc của ngày hiện tại với thao tác kéo thả
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

class TodayFragment : Fragment() {

    private var _binding: FragmentTodayBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskRepo: TaskRepository
    private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    // trạng thái kéo & animator gốc
    private var isDragging = false
    private var originalAnimator: RecyclerView.ItemAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTodayBinding.inflate(inflater, container, false)
        return binding.root
    }

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
            setItemViewCacheSize(8) // tuỳ chọn: giảm rebind
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
                    binding.rvTasks.itemAnimator = null // tắt animator để mượt khi kéo
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    isDragging = false
                    binding.rvTasks.itemAnimator = originalAnimator // bật lại khi thả
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

                taskAdapter.swapItems(from, to)          // đổi trong working list
                taskAdapter.notifyItemMoved(from, to)    // UI trượt theo tay
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) { }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                // lưu thứ tự mới từ working list
                val pairs = taskAdapter.snapshotIdsWithIndex()
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskRepo.updateOrderMany(pairs)
                }
            }
        }
        ItemTouchHelper(touchCallback).attachToRecyclerView(binding.rvTasks)

        val today = df.format(Calendar.getInstance().time)

        // Lấy task NGÀY HÔM NAY theo user hiện tại
        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { accountRepo.getCurrentUser() } ?: return@launch
            taskRepo.byDate(user.id, today).observe(viewLifecycleOwner) { tasks ->
                // Sắp xếp: chưa hoàn thành trước → orderIndex tăng → id giảm
                val sorted = tasks.sortedWith(
                    compareBy<com.example.appmanagement.data.entity.Task> { it.isCompleted }
                        .thenBy { it.orderIndex }
                        .thenByDescending { it.id }
                )
                if (!isDragging) {
                    taskAdapter.submitDataOnce(sorted)   // KHÔNG cập nhật khi đang kéo
                    if (sorted.isNotEmpty()) binding.rvTasks.scrollToPosition(0)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
