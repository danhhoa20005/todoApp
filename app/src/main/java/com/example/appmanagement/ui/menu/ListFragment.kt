// Fragment ListFragment hiển thị danh sách công việc chính với kéo thả và điều hướng chỉnh sửa
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
import com.example.appmanagement.data.remote.TaskRemoteDataSource
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.repo.TaskRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.example.appmanagement.databinding.FragmentListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskRepo: TaskRepository

    // thêm
    private var isDragging = false
    private var originalAnimator: RecyclerView.ItemAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val appContext = requireContext().applicationContext
        val db = AppDatabase.getInstance(appContext)
        val accountRepo = AccountRepository(db.userDao())
        taskRepo = TaskRepository(
            db.taskDao(),
            db.userDao(),
            TaskRemoteDataSource(FirebaseFirestore.getInstance())
        )

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
            setItemViewCacheSize(8) // tuỳ chọn: giảm rebind
            originalAnimator = itemAnimator
        }

        // ItemTouchHelper: kéo–thả, lưu order_index khi thả
        val touchCallback = object : ItemTouchHelper.SimpleCallback(
            ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0
        ) {
            override fun isLongPressDragEnabled() = true

            override fun onSelectedChanged(vh: RecyclerView.ViewHolder?, actionState: Int) {
                super.onSelectedChanged(vh, actionState)
                if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) {
                    isDragging = true
                    // tắt animator để tránh khựng khi kéo dài
                    binding.rvTasks.itemAnimator = null
                } else if (actionState == ItemTouchHelper.ACTION_STATE_IDLE) {
                    isDragging = false
                    // bật lại animator sau khi thả
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

                // đổi thứ tự trong working list của adapter
                taskAdapter.swapItems(from, to)
                // báo UI trượt theo tay
                taskAdapter.notifyItemMoved(from, to)
                return true
            }

            override fun onSwiped(vh: RecyclerView.ViewHolder, dir: Int) { }

            override fun clearView(rv: RecyclerView, vh: RecyclerView.ViewHolder) {
                super.clearView(rv, vh)
                // Lưu thứ tự mới về DB từ working list
                val pairs = taskAdapter.snapshotIdsWithIndex()
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskRepo.updateOrderMany(pairs)
                }
            }
        }
        val helper = ItemTouchHelper(touchCallback)
        helper.attachToRecyclerView(binding.rvTasks)
        taskAdapter.dragHelper = helper

        // Quan sát danh sách đã sắp xếp từ Repo/DAO
        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { accountRepo.getCurrentUser() }
            user?.let {
                taskRepo.all(it.id).observe(viewLifecycleOwner) { tasks ->
                    if (!isDragging) {
                        // đồng bộ một lần mỗi khi DB thay đổi – KHÔNG gọi trong lúc kéo
                        taskAdapter.submitDataOnce(tasks)
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
