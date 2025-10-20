package com.example.appmanagement.data.viewmodel

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
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val database by lazy { AppDatabase.getInstance(application) }
    private val taskRepository by lazy { TaskRepository(database.taskDao()) }
    private val accountRepository by lazy { AccountRepository(database.userDao()) }

    /* Lưu user hiện tại (id) */
    private val currentUserId = MutableLiveData<Long?>()

    /* LiveData công khai cho UI */
    private val _tasksAll = MediatorLiveData<List<Task>>()
    val tasksAll: LiveData<List<Task>> get() = _tasksAll

    private val _tasksTodo = MediatorLiveData<List<Task>>()
    val tasksTodo: LiveData<List<Task>> get() = _tasksTodo

    private val _tasksDone = MediatorLiveData<List<Task>>()
    val tasksDone: LiveData<List<Task>> get() = _tasksDone

    private val _tasksByDate = MediatorLiveData<List<Task>>()
    val tasksByDate: LiveData<List<Task>> get() = _tasksByDate

    /* Dữ liệu cho biểu đồ tuần */
    private val _weekCounts = MediatorLiveData<IntArray>()    // [Mon..Sun]
    val weekCounts: LiveData<IntArray> get() = _weekCounts

    private val _weekPercents = MediatorLiveData<IntArray>()  // [Mon..Sun] 0..100 theo trần cap
    val weekPercents: LiveData<IntArray> get() = _weekPercents

    /* Giữ tham chiếu nguồn hiện tại để removeSource đúng cách */
    private var allSource: LiveData<List<Task>>? = null
    private var todoSource: LiveData<List<Task>>? = null
    private var doneSource: LiveData<List<Task>>? = null
    private var byDateSource: LiveData<List<Task>>? = null

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    init {
        /* Nạp user hiện tại khi ViewModel tạo */
        loadCurrentUser()

        /* Gắn nguồn động theo user cho từng nhóm LiveData */
        _tasksAll.addSource(currentUserId) { uid ->
            _tasksAll.value = emptyList()
            allSource?.let { _tasksAll.removeSource(it) }
            if (uid != null) {
                val src = taskRepository.all(uid)
                allSource = src
                _tasksAll.addSource(src) { list ->
                    _tasksAll.value = list
                    computeWeeklySeries(list.orEmpty())
                }
            } else {
                computeWeeklySeries(emptyList())
            }
        }

        _tasksTodo.addSource(currentUserId) { uid ->
            _tasksTodo.value = emptyList()
            todoSource?.let { _tasksTodo.removeSource(it) }
            if (uid != null) {
                val src = taskRepository.uncompleted(uid)
                todoSource = src
                _tasksTodo.addSource(src) { _tasksTodo.value = it }
            }
        }

        _tasksDone.addSource(currentUserId) { uid ->
            _tasksDone.value = emptyList()
            doneSource?.let { _tasksDone.removeSource(it) }
            if (uid != null) {
                val src = taskRepository.completed(uid)
                doneSource = src
                _tasksDone.addSource(src) { _tasksDone.value = it }
            }
        }
        /* _tasksByDate chỉ gắn khi gọi filterByDate(...) */
    }

    /* Nạp user hiện tại (lấy từ AccountRepository) */
    private fun loadCurrentUser() = viewModelScope.launch {
        val user = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() }
        currentUserId.value = user?.id
    }

    /* Cho phép màn hình gọi lại sau khi đăng nhập/đăng xuất */
    fun reloadCurrentUser() = loadCurrentUser()

    /* Lọc theo ngày – nguồn động theo user + date */
    fun filterByDate(date: String) {
        val uid = currentUserId.value
        _tasksByDate.value = emptyList()
        byDateSource?.let { _tasksByDate.removeSource(it) }
        if (uid == null) return
        val src = taskRepository.byDate(uid, date)
        byDateSource = src
        _tasksByDate.addSource(src) { _tasksByDate.value = it }
    }

    /* Thêm Task – gắn đúng userId hiện tại */
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

    /* Cập nhật Task (giữ nguyên userId trong task) */
    fun updateTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.update(task)
    }

    /* Xoá Task */
    fun deleteTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.delete(task)
    }

    /* Đổi trạng thái hoàn thành */
    fun toggleTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.toggle(task)
    }

    /* --------- XỬ LÝ DỮ LIỆU CHO BIỂU ĐỒ TUẦN --------- */

    private fun computeWeeklySeries(all: List<Task>) {
        val counts = countCompletedLast7DaysByDow(all)
        _weekCounts.value = counts
        _weekPercents.value = countsToPercents(counts, capPerDay = 20)
    }

    /* Đếm số task đã hoàn thành trong 7 ngày gần nhất theo thứ (Mon..Sun) */
    private fun countCompletedLast7DaysByDow(all: List<Task>): IntArray {
        val today = LocalDate.now()
        val start = today.minusDays(6)
        val byDay = IntArray(7) { 0 }

        all.forEach { task ->
            if (!task.isCompleted) return@forEach
            val date = runCatching { LocalDate.parse(task.taskDate, dateFormatter) }.getOrNull()
                ?: return@forEach
            if (date.isBefore(start) || date.isAfter(today)) return@forEach
            when (date.dayOfWeek) {
                DayOfWeek.MONDAY    -> byDay[0]++
                DayOfWeek.TUESDAY   -> byDay[1]++
                DayOfWeek.WEDNESDAY -> byDay[2]++
                DayOfWeek.THURSDAY  -> byDay[3]++
                DayOfWeek.FRIDAY    -> byDay[4]++
                DayOfWeek.SATURDAY  -> byDay[5]++
                DayOfWeek.SUNDAY    -> byDay[6]++
            }
        }
        return byDay
    }

    /* Quy đổi số lượng sang phần trăm với ngưỡng trần mỗi ngày */
    private fun countsToPercents(counts: IntArray, capPerDay: Int): IntArray {
        return IntArray(7) { i ->
            val clamped = counts[i].coerceAtMost(capPerDay)
            (clamped * 100f / capPerDay).roundToInt()
        }
    }
}
