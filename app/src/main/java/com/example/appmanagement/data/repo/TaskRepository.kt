package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.TaskDao
import com.example.appmanagement.data.entity.Task

// Repository trung gian giữa ViewModel và Room
// Gói toàn bộ thao tác CRUD cho Task, giúp tách biệt logic DB khỏi UI
class TaskRepository(private val dao: TaskDao) {

    // Lấy danh sách task theo user
    fun all(userId: Long) = dao.getByUser(userId)

    // Lấy task chưa hoàn thành
    fun uncompleted(userId: Long) = dao.getUncompleted(userId)

    // Lấy task đã hoàn thành
    fun completed(userId: Long) = dao.getCompleted(userId)

    // Lấy task theo ngày cụ thể
    fun byDate(userId: Long, date: String) = dao.getByDate(userId, date)

    // Lấy task theo id
    fun byId(id: Long) = dao.getByIdLive(id)

    // Thêm task mới
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
            endTime = endTime
        )
    )

    // Cập nhật task
    suspend fun update(task: Task) = dao.update(task)

    // Xoá task
    suspend fun delete(task: Task) = dao.delete(task)

    // Đổi trạng thái hoàn thành ↔ chưa hoàn thành
    suspend fun toggle(task: Task) = dao.setCompleted(task.id, !task.isCompleted)
}
