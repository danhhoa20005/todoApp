package com.example.appmanagement.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.repo.TaskRepository
import com.example.appmanagement.databinding.FragmentListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Danh sách toàn bộ công việc với khả năng kéo thả sắp xếp thứ tự
class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskRepo: TaskRepository

    private var isDragging = false
    private var originalAnimator: RecyclerView.ItemAnimator? = null

    // Khởi tạo binding cho layout danh sách
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập adapter, kéo thả và quan sát dữ liệu task
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContext = requireContext().applicationContext
        val db = AppDatabase.getInstance(appContext)
        val accountRepo = AccountRepository(db.userDao())
        taskRepo = TaskRepository(db.taskDao())

        taskAdapter = TaskAdapter(
            onEditClick = { task ->
                val action = ListFragmentDirections.actionListFragmentToEditFragment(task.id)
                findNavController().navigate(action)
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
        val helper = ItemTouchHelper(touchCallback)
        helper.attachToRecyclerView(binding.rvTasks)
        taskAdapter.dragHelper = helper

        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { accountRepo.getCurrentUser() }
            user?.let {
                taskRepo.all(it.id).observe(viewLifecycleOwner) { tasks ->
                    if (!isDragging) {
                        taskAdapter.submitList(tasks)
                    }
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
