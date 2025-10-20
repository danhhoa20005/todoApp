package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.TaskDao
import com.example.appmanagement.data.entity.Task

class TaskRepository(private val dao: TaskDao) {

    fun all(userId: Long) = dao.getByUserOrdered(userId)
    fun uncompleted(userId: Long) = dao.getUncompletedOrdered(userId)
    fun completed(userId: Long) = dao.getCompletedOrdered(userId)
    fun byDate(userId: Long, date: String) = dao.getByDate(userId, date)
    fun byId(id: Long) = dao.getByIdLive(id)

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

    suspend fun update(task: Task) = dao.update(task)
    suspend fun delete(task: Task) = dao.delete(task)
    suspend fun toggle(task: Task) = dao.setCompleted(task.id, !task.isCompleted)

    suspend fun updateOrderIndex(id: Long, index: Int) = dao.updateOrderIndex(id, index)
    suspend fun updateOrderMany(pairs: List<Pair<Long, Int>>) = dao.updateOrderMany(pairs)
}
