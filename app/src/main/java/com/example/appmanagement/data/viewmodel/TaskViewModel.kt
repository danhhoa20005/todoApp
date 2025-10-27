package com.example.appmanagement.data.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.repo.TaskRepository
import kotlinx.coroutines.*
import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * ViewModel quản lý dữ liệu công việc (Task)
 * Chịu trách nhiệm trung gian giữa UI ↔ Repository
 * - Kết nối Room Database (TaskDao, UserDao)
 * - Cung cấp LiveData cho giao diện (UI quan sát thay đổi)
 * - Thực hiện các thao tác CRUD (thêm, sửa, xóa, lọc, thống kê)
 */
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    /* ------------------ KẾT NỐI DATABASE & REPOSITORY ------------------ */

    private val database by lazy { AppDatabase.getInstance(application) }          // Lấy thể hiện DB
    private val taskRepository by lazy { TaskRepository(database.taskDao()) }      // Repo công việc
    private val accountRepository by lazy { AccountRepository(database.userDao()) } // Repo người dùng

    /* ------------------ QUẢN LÝ USER ĐANG ĐĂNG NHẬP ------------------ */

    private val currentUserId = MutableLiveData<Long?>()  // Lưu id người dùng hiện tại (null nếu chưa login)

    /* ------------------ LIVE DATA CHO CÁC LOẠI DANH SÁCH ------------------ */

    private val _tasksAll = MediatorLiveData<List<Task>>()   // tất cả task
    val tasksAll: LiveData<List<Task>> get() = _tasksAll

    private val _tasksTodo = MediatorLiveData<List<Task>>()  // task chưa hoàn thành
    val tasksTodo: LiveData<List<Task>> get() = _tasksTodo

    private val _tasksDone = MediatorLiveData<List<Task>>()  // task đã hoàn thành
    val tasksDone: LiveData<List<Task>> get() = _tasksDone

    private val _tasksByDate = MediatorLiveData<List<Task>>() // task theo ngày cụ thể
    val tasksByDate: LiveData<List<Task>> get() = _tasksByDate

    /* ------------------ DỮ LIỆU BIỂU ĐỒ TUẦN ------------------ */

    private val _weekCounts = MediatorLiveData<IntArray>()    // số task hoàn thành [Mon..Sun]
    val weekCounts: LiveData<IntArray> get() = _weekCounts

    private val _weekPercents = MediatorLiveData<IntArray>()  // tỉ lệ % hoàn thành theo ngày
    val weekPercents: LiveData<IntArray> get() = _weekPercents

    /* ------------------ NGUỒN DỮ LIỆU ĐỘNG (để removeSource chính xác) ------------------ */
    private var allSource: LiveData<List<Task>>? = null
    private var todoSource: LiveData<List<Task>>? = null
    private var doneSource: LiveData<List<Task>>? = null
    private var byDateSource: LiveData<List<Task>>? = null

    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

    /* ------------------ KHỞI TẠO VIEWMODEL ------------------ */
    init {
        // 1️⃣ Nạp user hiện tại khi ViewModel được tạo
        loadCurrentUser()

        // 2️⃣ Quan sát currentUserId → thay đổi dữ liệu theo user
        _tasksAll.addSource(currentUserId) { uid ->
            _tasksAll.value = emptyList()
            allSource?.let { _tasksAll.removeSource(it) }

            if (uid != null) {
                val src = taskRepository.all(uid)   // lấy danh sách task của user
                allSource = src
                _tasksAll.addSource(src) { list ->
                    _tasksAll.value = list
                    computeWeeklySeries(list.orEmpty()) // cập nhật dữ liệu biểu đồ tuần
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
        // _tasksByDate sẽ gắn khi gọi filterByDate()
    }

    /* ------------------ HÀM NỘI BỘ ------------------ */

    /** Nạp user hiện tại (lấy từ AccountRepository) */
    private fun loadCurrentUser() = viewModelScope.launch {
        val user = withContext(Dispatchers.IO) { accountRepository.getCurrentUser() }
        currentUserId.value = user?.id
    }

    /** Cho phép màn hình gọi lại khi người dùng login/logout */
    fun reloadCurrentUser() = loadCurrentUser()

    /** Lọc công việc theo ngày (date format "dd/MM/yyyy") */
    fun filterByDate(date: String) {
        val uid = currentUserId.value
        _tasksByDate.value = emptyList()
        byDateSource?.let { _tasksByDate.removeSource(it) }

        if (uid == null) return
        val src = taskRepository.byDate(uid, date)
        byDateSource = src
        _tasksByDate.addSource(src) { _tasksByDate.value = it }
    }

    /* ------------------ HÀM THAO TÁC VỚI CÔNG VIỆC ------------------ */

    /** Thêm task mới cho user hiện tại */
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

    /** Cập nhật thông tin task */
    fun updateTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.update(task)
    }

    /** Xóa task */
    fun deleteTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.delete(task)
    }

    /** Đổi trạng thái hoàn thành (done ↔ todo) */
    fun toggleTask(task: Task) = viewModelScope.launch(Dispatchers.IO) {
        taskRepository.toggle(task)
    }

    /* ------------------ XỬ LÝ DỮ LIỆU BIỂU ĐỒ TUẦN ------------------ */

    /** Tính toán dữ liệu biểu đồ tuần: số lượng + % hoàn thành theo ngày */
    private fun computeWeeklySeries(all: List<Task>) {
        val counts = countCompletedLast7DaysByDow(all)
        _weekCounts.value = counts
        _weekPercents.value = countsToPercents(counts, capPerDay = 20)
    }

    /** Đếm số task hoàn thành trong 7 ngày gần nhất theo thứ (Mon..Sun) */
    private fun countCompletedLast7DaysByDow(all: List<Task>): IntArray {
        val today = LocalDate.now()
        val start = today.minusDays(6)
        val byDay = IntArray(7) { 0 }

        all.forEach { task ->
            if (!task.isCompleted) return@forEach   // chỉ tính task đã hoàn thành
            val date = runCatching { LocalDate.parse(task.taskDate, dateFormatter) }.getOrNull()
                ?: return@forEach
            if (date.isBefore(start) || date.isAfter(today)) return@forEach

            // Cộng vào đúng thứ trong tuần
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

    /** Chuyển số lượng task hoàn thành sang % hiển thị trong biểu đồ */
    private fun countsToPercents(counts: IntArray, capPerDay: Int): IntArray {
        return IntArray(7) { i ->
            val clamped = counts[i].coerceAtMost(capPerDay) // giới hạn tối đa 1 ngày
            (clamped * 100f / capPerDay).roundToInt()       // đổi sang %
        }
    }
}
