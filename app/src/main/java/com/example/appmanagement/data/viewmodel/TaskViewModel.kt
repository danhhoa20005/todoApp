// ViewModel TaskViewModel điều phối dữ liệu công việc, lọc theo trạng thái và tính toán thống kê tuần
package com.example.appmanagement.data.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.repo.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private var currentUser: User? = null

    private val _tasksAll = MediatorLiveData<List<Task>>()
    val tasksAll: LiveData<List<Task>> get() = _tasksAll

    private val _tasksTodo = MediatorLiveData<List<Task>>()
    val tasksTodo: LiveData<List<Task>> get() = _tasksTodo

    private val _tasksDone = MediatorLiveData<List<Task>>()
    val tasksDone: LiveData<List<Task>> get() = _tasksDone

    private val _tasksByDate = MediatorLiveData<List<Task>>()
    val tasksByDate: LiveData<List<Task>> get() = _tasksByDate

    /** --- NEW: dữ liệu cho biểu đồ tuần --- */
    private val _weekCounts = MediatorLiveData<IntArray>()    // [Mon..Sun]
    val weekCounts: LiveData<IntArray> get() = _weekCounts

    private val _weekPercents = MediatorLiveData<IntArray>()  // [Mon..Sun] 0..100 theo trần 20
    val weekPercents: LiveData<IntArray> get() = _weekPercents

    private var tasksAllSource: LiveData<List<Task>>? = null
    private var tasksTodoSource: LiveData<List<Task>>? = null
    private var tasksDoneSource: LiveData<List<Task>>? = null
    private var tasksByDateSource: LiveData<List<Task>>? = null

    private val addMutex = Mutex()
    private val _isAdding = MutableStateFlow(false)
    val isAdding = _isAdding.asStateFlow()
    private val _addResults = MutableSharedFlow<Result<Long>>(extraBufferCapacity = 1)
    val addResults = _addResults.asSharedFlow()

    init {
        loadTasksForCurrentUser()
    }

    fun loadTasksForCurrentUser() = viewModelScope.launch {
        val user = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() }
        currentUser = user
        attachSources(user)
    }

    private fun attachSources(user: User?) {
        tasksAllSource?.let { _tasksAll.removeSource(it) }
        tasksTodoSource?.let { _tasksTodo.removeSource(it) }
        tasksDoneSource?.let { _tasksDone.removeSource(it) }

        _tasksAll.value = emptyList()
        _tasksTodo.value = emptyList()
        _tasksDone.value = emptyList()
        computeWeeklySeries(emptyList())

        if (user == null) return

        taskRepository.observeTasksByUserId(user.id).also { source ->
            tasksAllSource = source
            _tasksAll.addSource(source) { list ->
                _tasksAll.value = list
                computeWeeklySeries(list.orEmpty())
            }
        }

        taskRepository.uncompleted(user.id).also { source ->
            tasksTodoSource = source
            _tasksTodo.addSource(source) { _tasksTodo.value = it }
        }

        taskRepository.completed(user.id).also { source ->
            tasksDoneSource = source
            _tasksDone.addSource(source) { _tasksDone.value = it }
        }
    }

    /** Lọc theo ngày */
    fun filterByDate(date: String) {
        val user = currentUser ?: run {
            _tasksByDate.value = emptyList()
            return
        }
        _tasksByDate.value = emptyList()
        tasksByDateSource?.let { _tasksByDate.removeSource(it) }
        val source = taskRepository.byDate(user.id, date)
        tasksByDateSource = source
        _tasksByDate.addSource(source) { _tasksByDate.value = it }
    }

    /** Thêm Task có khóa chống spam + emit kết quả cho UI */
    fun addTask(
        title: String,
        description: String,
        taskDate: String,
        startTime: String,
        endTime: String
    ) = viewModelScope.launch {
        if (_isAdding.value) return@launch
        addMutex.withLock {
            _isAdding.value = true
            val result = runCatching {
                val user = getCurrentUserSafe() ?: error("Chưa có người dùng đăng nhập")
                withContext(Dispatchers.IO) {
                    taskRepository.add(user, title, description, taskDate, startTime, endTime)
                }
            }
            _isAdding.value = false
            _addResults.emit(result)
        }
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

    fun syncTasksForCurrentUser() = viewModelScope.launch(Dispatchers.IO) {
        val user = getCurrentUserSafe() ?: return@launch
        taskRepository.syncFromRemote(user)
    }

    // ------------------ WEEKLY CHART HELPERS ------------------

    private val dateFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd/MM/yyyy")


    private fun computeWeeklySeries(all: List<Task>) {
        val counts = countCompletedLast7DaysByDow(all)
        _weekCounts.value = counts
        _weekPercents.value = countsToPercents(counts, capPerDay = 20)
    }

    private fun countCompletedLast7DaysByDow(all: List<Task>): IntArray {
        val today = LocalDate.now()
        val start = today.minusDays(6)
        val byDay = IntArray(7) { 0 } // Mon..Sun

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

    /** Quy đổi theo trần (capPerDay = 20), trả về % 0..100 cho 7 ngày */
    private fun countsToPercents(counts: IntArray, capPerDay: Int): IntArray {
        return IntArray(7) { i ->
            val clamped = counts[i].coerceAtMost(capPerDay)
            // Nếu muốn có “tối thiểu 5% khi >0” để luôn thấy cột thì dùng:
            // val raw = (clamped * 100f / capPerDay).roundToInt()
            // if (counts[i] > 0) maxOf(raw, 5) else 0
            (clamped * 100f / capPerDay).roundToInt()
        }
    }

    suspend fun getCurrentUserSafe(): User? {
        val cached = currentUser
        if (cached != null) return cached
        val refreshed = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() }
        currentUser = refreshed
        return refreshed
    }
}
