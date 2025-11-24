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
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.repo.TaskRepository
import com.example.appmanagement.data.remote.TaskRemoteDataSource
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val database by lazy { AppDatabase.getInstance(application) }
    private val taskRepository by lazy {
        TaskRepository(
            database.taskDao(),
            database.userDao(),
            TaskRemoteDataSource(FirebaseFirestore.getInstance())
        )
    }
    private val accountRepository by lazy { AccountRepository(database.userDao()) }

    private var currentUser: User? = null
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
        // 1) Nạp user đăng nhập hiện tại
        loadCurrentUser()

        // 2) Khi có userId -> gắn nguồn dữ liệu
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
        // _tasksByDate gắn nguồn khi filterByDate(...)
    }

    private fun loadCurrentUser() = viewModelScope.launch {
        val user = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() }
        currentUser = user
        currentUserId.value = user?.id
    }

    /** Lọc theo ngày */
    fun filterByDate(date: String) {
        val uid = currentUserId.value ?: run {
            _tasksByDate.value = emptyList()
            return
        }
        _tasksByDate.value = emptyList()
        val source = taskRepository.byDate(uid, date)
        _tasksByDate.addSource(source) { _tasksByDate.value = it }
    }

    /** Thêm Task */
    fun addTask(
        title: String,
        description: String,
        taskDate: String,
        startTime: String,
        endTime: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val user = currentUser ?: return@launch
        taskRepository.add(user, title, description, taskDate, startTime, endTime)
    }

    /** Cập nhật Task */
    fun updateTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.update(task)
    }

    /** Xoá Task */
    fun deleteTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.delete(task)
    }

    /** Đổi trạng thái hoàn thành */
    fun toggleTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.toggle(task)
    }
}
