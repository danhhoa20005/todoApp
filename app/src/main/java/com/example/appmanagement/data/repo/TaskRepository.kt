// Lớp TaskRepository gom các thao tác với TaskDao để tầng ViewModel sử dụng gọn gàng
package com.example.appmanagement.data.repo

import com.example.appmanagement.data.dao.TaskDao
import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.entity.User
import com.example.appmanagement.data.remote.TaskRemoteDataSource
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TaskRepository(
    private val dao: TaskDao,
    private val userDao: UserDao,
    private val remoteDataSource: TaskRemoteDataSource
) {

    private val addMutex = Mutex()

    // all: lấy toàn bộ task đã sắp xếp của user
    fun all(userId: Long) = dao.getByUserOrdered(userId)
    // uncompleted: danh sách chưa hoàn thành của user
    fun uncompleted(userId: Long) = dao.getUncompletedOrdered(userId)
    // completed: danh sách đã hoàn thành của user
    fun completed(userId: Long) = dao.getCompletedOrdered(userId)
    // byDate: lọc task theo ngày
    fun byDate(userId: Long, date: String) = dao.getByDate(userId, date)
    // byId: LiveData 1 task
    fun byId(id: Long) = dao.getByIdLive(id)
    // observeTasksByUserId: stream toàn bộ task theo user
    fun observeTasksByUserId(userId: Long) = dao.observeTasksByUserId(userId)

    // add: tạo task mới cho user hiện tại
    suspend fun add(
        user: User,
        title: String,
        description: String,
        taskDate: String,
        startTime: String,
        endTime: String
    ): Long = addMutex.withLock {
        val now = System.currentTimeMillis()
        val trimmedTitle = title.trim()
        val duplicate = dao.findDuplicate(user.id, trimmedTitle, taskDate, startTime, endTime)
        if (duplicate != null) return@withLock duplicate.id

        val newTask = Task(
            userId = user.id,
            userRemoteId = user.remoteId,
            title = trimmedTitle,
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
        syncRemoteSafe(user, savedTask)
        return@withLock id
    }

    // addTask: lưu task có sẵn (ví dụ import) và gắn user
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
        val synced = syncRemoteSafe(user, saved)
        return synced ?: saved
    }

    // update: cập nhật nội dung task
    suspend fun update(task: Task) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        dao.update(updated)
        syncRemoteSafe(null, updated)
    }

    // delete: xoá task cả local và Firestore
    suspend fun delete(task: Task) {
        dao.delete(task)
        val userRemoteId = resolveRemoteUserId(null, task)
        if (!task.remoteId.isNullOrBlank() && !userRemoteId.isNullOrBlank()) {
            runCatching { remoteDataSource.deleteTask(task.remoteId) }
        }
    }

    // toggle: đổi trạng thái hoàn thành
    suspend fun toggle(task: Task) {
        val toggled = task.copy(isCompleted = !task.isCompleted, updatedAt = System.currentTimeMillis())
        dao.update(toggled)
        syncRemoteSafe(null, toggled)
    }

    // updateOrderIndex: cập nhật thứ tự hiển thị 1 task
    suspend fun updateOrderIndex(id: Long, index: Int) {
        val task = dao.getByIdOnce(id) ?: return
        val updated = task.copy(orderIndex = index, updatedAt = System.currentTimeMillis())
        dao.update(updated)
        syncRemote(null, updated)
    }

    // updateOrderMany: cập nhật thứ tự cho nhiều task
    suspend fun updateOrderMany(pairs: List<Pair<Long, Int>>) {
        pairs.forEach { (id, idx) -> updateOrderIndex(id, idx) }
    }

    // syncFromRemote: tải task của user từ Firestore về Room
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

    // syncRemote: đẩy task lên Firestore và cập nhật remoteId/userRemoteId nếu cần
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

    // getUserRemoteId: lấy uid Firebase từ bảng users
    private suspend fun getUserRemoteId(userId: Long): String? {
        val user = userDao.getById(userId)
        return user?.remoteId
    }

    // resolveRemoteUserId: ưu tiên user hiện tại, fallback task.userRemoteId hoặc DB
    private suspend fun resolveRemoteUserId(user: User?, task: Task): String? {
        val remoteId = user?.remoteId ?: task.userRemoteId ?: getUserRemoteId(task.userId)
        return remoteId?.takeIf { it.isNotBlank() }
    }

    // syncRemoteSafe: đẩy Firestore nhưng không làm rơi coroutine khi offline/lỗi mạng
    private suspend fun syncRemoteSafe(user: User?, task: Task): Task? =
        runCatching { syncRemote(user, task) }.getOrNull()
}
