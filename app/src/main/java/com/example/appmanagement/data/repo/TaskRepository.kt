// Lớp TaskRepository gom các thao tác với TaskDao để tầng ViewModel sử dụng gọn gàng
package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.TaskDao
import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.data.remote.TaskRemoteDataSource

class TaskRepository(
    private val dao: TaskDao,
    private val userDao: UserDao,
    private val remoteDataSource: TaskRemoteDataSource
) {

    fun all(userId: Long) = dao.getByUserOrdered(userId)
    fun uncompleted(userId: Long) = dao.getUncompletedOrdered(userId)
    fun completed(userId: Long) = dao.getCompletedOrdered(userId)
    fun byDate(userId: Long, date: String) = dao.getByDate(userId, date)
    fun byId(id: Long) = dao.getByIdLive(id)
    fun observeTasksByUserId(userId: Long) = dao.observeTasksByUserId(userId)

    suspend fun add(
        user: User,
        title: String,
        description: String,
        taskDate: String,
        startTime: String,
        endTime: String
    ): Long {
        val now = System.currentTimeMillis()
        val newTask = Task(
            userId = user.id,
            userRemoteId = user.remoteId,
            title = title.trim(),
            description = description.trim(),
            taskDate = taskDate,
            startTime = startTime,
            endTime = endTime,
            orderIndex = Int.MAX_VALUE,
            updatedAt = now,
            createdAt = now
        )
        val id = dao.insert(newTask)
        val savedTask = newTask.copy(id = id)
        syncRemote(user, savedTask)
        return id
    }

    suspend fun addTask(user: User, task: Task): Task {
        val now = System.currentTimeMillis()
        val toSave = task.copy(
            userId = user.id,
            userRemoteId = user.remoteId ?: task.userRemoteId,
            updatedAt = now,
            createdAt = task.createdAt
        )
        val id = dao.insert(toSave)
        val saved = toSave.copy(id = id)
        val synced = syncRemote(user, saved)
        return synced ?: saved
    }

    suspend fun update(task: Task) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        dao.update(updated)
        syncRemote(null, updated)
    }

    suspend fun delete(task: Task) {
        dao.delete(task)
        val userRemoteId = resolveRemoteUserId(null, task)
        if (!task.remoteId.isNullOrBlank() && !userRemoteId.isNullOrBlank()) {
            remoteDataSource.deleteTask(task.remoteId)
        }
    }

    suspend fun toggle(task: Task) {
        val toggled = task.copy(isCompleted = !task.isCompleted, updatedAt = System.currentTimeMillis())
        dao.update(toggled)
        syncRemote(null, toggled)
    }

    suspend fun updateOrderIndex(id: Long, index: Int) {
        val task = dao.getByIdOnce(id) ?: return
        val updated = task.copy(orderIndex = index, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        syncRemote(null, updated)
    }

    suspend fun updateOrderMany(pairs: List<Pair<Long, Int>>) {
        pairs.forEach { (id, idx) -> updateOrderIndex(id, idx) }
    }

    suspend fun syncFromRemote(user: User) {
        val userRemoteId = user.remoteId ?: return
        val remoteTasks = remoteDataSource.fetchTasks(userRemoteId)
        remoteTasks.forEach { remoteTask ->
            val existing = remoteTask.remoteId?.let { dao.getByRemoteId(it) }
            val taskForUser = remoteTask.copy(userId = user.id, userRemoteId = user.remoteId)
            if (existing == null) {
                dao.insert(taskForUser)
            } else if (remoteTask.updatedAt >= existing.updatedAt) {
                dao.update(taskForUser.copy(id = existing.id))
            }
        }
    }

    private suspend fun syncRemote(user: User?, task: Task): Task? {
        val userRemoteId = resolveRemoteUserId(user, task) ?: return null
        val taskWithRemoteUser = if (task.userRemoteId == userRemoteId) task else task.copy(userRemoteId = userRemoteId)
        val remoteId = remoteDataSource.upsertTask(userRemoteId, taskWithRemoteUser)
        val needsUpdate = remoteId != task.remoteId || task.userRemoteId != userRemoteId
        if (needsUpdate) {
            val updatedTask = taskWithRemoteUser.copy(remoteId = remoteId)
            dao.update(updatedTask)
            return updatedTask
        }
        return taskWithRemoteUser
    }

    private suspend fun getUserRemoteId(userId: Long): String? {
        val user = userDao.getById(userId)
        return user?.remoteId
    }

    private suspend fun resolveRemoteUserId(user: User?, task: Task): String? {
        val remoteId = user?.remoteId ?: task.userRemoteId ?: getUserRemoteId(task.userId)
        return remoteId?.takeIf { it.isNotBlank() }
    }
}
