package com.example.appmanagement.ui.menu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.databinding.FragmentListBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ListFragment : Fragment() {

    private var _binding: FragmentListBinding? = null
    private val binding get() = _binding!!

    private lateinit var taskAdapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Khởi tạo DB và Repository cần dùng
        val appContext = requireContext().applicationContext
        val database = AppDatabase.getInstance(appContext)
        val accountRepository = AccountRepository(database.userDao())
        val taskDao = database.taskDao()

        // Khởi tạo RecyclerView + Adapter với callback hành động
        taskAdapter = TaskAdapter(
            onEditClick = { task ->
                val action = ListFragmentDirections.actionListFragmentToEditFragment(task.id)
                findNavController().navigate(action)
            },
            onDeleteClick = { task ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskDao.delete(task)
                }
            },
            onCheckClick = { task ->
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    taskDao.setCompleted(task.id, !task.isCompleted)
                }
            }
        )

        binding.rvTasks.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = taskAdapter
        }


        // Lấy user hiện tại và quan sát danh sách Task theo userId
        viewLifecycleOwner.lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() }
            user?.let {
                // ⚠️ CHÚ Ý: dùng đúng tên hàm trong TaskDao của bạn
                // Nếu bạn đặt là getByUser(userId) thì dùng như dưới:
                taskDao.getByUser(it.id).observe(viewLifecycleOwner) { tasks ->
                    taskAdapter.submitList(tasks)
                }

                // Nếu trong DAO bạn đặt tên là getTasksByUser(userId) → đổi lại cho khớp:
                // taskDao.getTasksByUser(it.id).observe(viewLifecycleOwner) { tasks -> ... }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
