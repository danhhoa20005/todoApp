package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.TaskDao
import com.example.appmanagement.data.entity.Task

// Repository cung cấp lớp trung gian giữa ViewModel và TaskDao
class TaskRepository(private val dao: TaskDao) {

    // Trả về toàn bộ công việc sắp xếp theo trạng thái và thứ tự
    fun all(userId: Long) = dao.getByUserOrdered(userId)
    // Trả về danh sách chưa hoàn thành
    fun uncompleted(userId: Long) = dao.getUncompletedOrdered(userId)
    // Trả về danh sách đã hoàn thành
    fun completed(userId: Long) = dao.getCompletedOrdered(userId)
    // Truy vấn công việc theo ngày cụ thể
    fun byDate(userId: Long, date: String) = dao.getByDate(userId, date)
    // Quan sát công việc theo id
    fun byId(id: Long) = dao.getByIdLive(id)

    // Thêm công việc mới với dữ liệu đã chuẩn hóa
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
            title = title.trim(),
            description = description.trim(),
            taskDate = taskDate,
            startTime = startTime,
            endTime = endTime,
            orderIndex = Int.MAX_VALUE
        )
    )

    // Cập nhật nội dung công việc
    suspend fun update(task: Task) = dao.update(task)
    // Xoá công việc khỏi cơ sở dữ liệu
    suspend fun delete(task: Task) = dao.delete(task)
    // Đảo trạng thái hoàn thành của công việc
    suspend fun toggle(task: Task) = dao.setCompleted(task.id, !task.isCompleted)

    // Lưu thứ tự hiển thị cho một công việc
    suspend fun updateOrderIndex(id: Long, index: Int) = dao.updateOrderIndex(id, index)
    // Lưu thứ tự cho nhiều công việc trong một lần gọi
    suspend fun updateOrderMany(pairs: List<Pair<Long, Int>>) = dao.updateOrderMany(pairs)
}
