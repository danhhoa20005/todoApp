package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.TaskDao
import com.example.appmanagement.data.entity.Task

/**
 * Lớp Repository trung gian giữa ViewModel ↔ Room (TaskDao)
 * Quản lý toàn bộ thao tác với bảng công việc (Task)
 * Bao gồm: lấy danh sách, thêm, sửa, xóa, đánh dấu hoàn thành, sắp xếp thứ tự.
 */
class TaskRepository(private val dao: TaskDao) {

    /** Lấy toàn bộ công việc của user theo thứ tự (tất cả công việc) */
    fun all(userId: Long) = dao.getByUserOrdered(userId)

    /** Lấy danh sách công việc chưa hoàn thành (ordered theo thời gian hoặc thứ tự) */
    fun uncompleted(userId: Long) = dao.getUncompletedOrdered(userId)

    /** Lấy danh sách công việc đã hoàn thành */
    fun completed(userId: Long) = dao.getCompletedOrdered(userId)

    /** Lọc công việc theo ngày cụ thể (theo định dạng yyyy-MM-dd hoặc dd/MM/yyyy) */
    fun byDate(userId: Long, date: String) = dao.getByDate(userId, date)

    /** Lấy công việc theo ID (trả về LiveData để quan sát thay đổi realtime) */
    fun byId(id: Long) = dao.getByIdLive(id)

    /**
     * Thêm mới một công việc cho người dùng.
     * Hàm là suspend (chạy trong coroutine), để tránh block luồng chính.
     */
    suspend fun add(
        userId: Long,
        title: String,
        description: String,
        taskDate: String,
        startTime: String,
        endTime: String
    ): Long = dao.insert(
        Task(
            userId = userId,
            title = title.trim(),              // loại bỏ khoảng trắng đầu cuối
            description = description.trim(),  // loại bỏ khoảng trắng đầu cuối
            taskDate = taskDate,               // ngày thực hiện công việc
            startTime = startTime,             // giờ bắt đầu
            endTime = endTime,                 // giờ kết thúc
            orderIndex = Int.MAX_VALUE         // mặc định đẩy công việc mới xuống cuối danh sách
        )
    )

    /** Cập nhật thông tin một công việc (chỉnh sửa tiêu đề, thời gian, mô tả, v.v.) */
    suspend fun update(task: Task) = dao.update(task)

    /** Xóa công việc khỏi cơ sở dữ liệu */
    suspend fun delete(task: Task) = dao.delete(task)

    /**
     * Đảo trạng thái hoàn thành:
     * Nếu đang chưa hoàn thành → chuyển sang hoàn thành,
     * ngược lại thì bỏ hoàn thành.
     */
    suspend fun toggle(task: Task) = dao.setCompleted(task.id, !task.isCompleted)

    /** Cập nhật thứ tự sắp xếp (orderIndex) cho một công việc cụ thể */
    suspend fun updateOrderIndex(id: Long, index: Int) = dao.updateOrderIndex(id, index)

    /**
     * Cập nhật thứ tự cho nhiều công việc cùng lúc.
     * `pairs`: danh sách gồm (id công việc, vị trí mới)
     * Dùng khi người dùng kéo thả thay đổi thứ tự trên RecyclerView.
     */
    suspend fun updateOrderMany(pairs: List<Pair<Long, Int>>) = dao.updateOrderMany(pairs)
}
