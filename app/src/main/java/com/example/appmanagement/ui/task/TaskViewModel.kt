// ViewModel TaskViewModel trong gói ui điều phối danh sách công việc hiển thị theo các bộ lọc khác nhau
package com.example.appmanagement.ui.task

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.repo.TaskRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// ViewModel nhẹ cho tầng UI tái sử dụng lại repository chung
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val database by lazy { AppDatabase.getInstance(application) }
    private val taskRepository by lazy { TaskRepository(database.taskDao()) }
    private val accountRepository by lazy { AccountRepository(database.userDao()) }

    private val currentUserId = MutableLiveData<Long?>()

    private val _tasksAll = MediatorLiveData<List<Task>>()
    val tasksAll: LiveData<List<Task>> get() = _tasksAll

    private val _tasksTodo = MediatorLiveData<List<Task>>()
    val tasksTodo: LiveData<List<Task>> get() = _tasksTodo

    private val _tasksDone = MediatorLiveData<List<Task>>()
    val tasksDone: LiveData<List<Task>> get() = _tasksDone

    private val _tasksByDate = MediatorLiveData<List<Task>>()
    val tasksByDate: LiveData<List<Task>> get() = _tasksByDate

    init {
        // Nạp user đăng nhập hiện tại
        loadCurrentUser()

        // Khi có userId thì gắn nguồn dữ liệu tương ứng
        _tasksAll.addSource(currentUserId) { uid ->
            _tasksAll.value = emptyList()
            if (uid != null) {
                val src = taskRepository.all(uid)
                _tasksAll.addSource(src) { _tasksAll.value = it }
            }
        }

        _tasksTodo.addSource(currentUserId) { uid ->
            _tasksTodo.value = emptyList()
            if (uid != null) {
                val src = taskRepository.uncompleted(uid)
                _tasksTodo.addSource(src) { _tasksTodo.value = it }
            }
        }

        _tasksDone.addSource(currentUserId) { uid ->
            _tasksDone.value = emptyList()
            if (uid != null) {
                val src = taskRepository.completed(uid)
                _tasksDone.addSource(src) { _tasksDone.value = it }
            }
        }
        // tasksByDate sẽ gắn nguồn khi filterByDate được gọi
    }

    // Tải user hiện tại từ repository
    private fun loadCurrentUser() = viewModelScope.launch {
        val user = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() }
        currentUserId.value = user?.id
    }

    // Lọc danh sách theo ngày cụ thể
    fun filterByDate(date: String) {
        val uid = currentUserId.value ?: run {
            _tasksByDate.value = emptyList()
            return
        }
        _tasksByDate.value = emptyList()
        val source = taskRepository.byDate(uid, date)
        _tasksByDate.addSource(source) { _tasksByDate.value = it }
    }

    // Thêm công việc mới cho user hiện tại
    fun addTask(
        title: String,
        description: String,
        taskDate: String,
        startTime: String,
        endTime: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val uid = currentUserId.value ?: return@launch
        taskRepository.add(uid, title, description, taskDate, startTime, endTime)
    }

    // Cập nhật thông tin công việc
    fun updateTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.update(task)
    }

    // Xoá công việc
    fun deleteTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.delete(task)
    }

    // Đổi trạng thái hoàn thành
    fun toggleTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.toggle(task)
    }
}
