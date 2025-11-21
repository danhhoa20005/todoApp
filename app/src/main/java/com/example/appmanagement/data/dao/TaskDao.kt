// Giao diện TaskDao định nghĩa các truy vấn Room quản lý danh sách công việc theo người dùng
package com.example.appmanagement.data.dao

import androidx.lifecycle.LiveData
import androidx.room.*
import com.example.appmanagement.data.entity.Task

@Dao
interface TaskDao {

    // ===== CREATE =====
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    // ===== READ =====
    @Query("SELECT * FROM tasks WHERE user_id = :userId ORDER BY id DESC")
    fun getByUser(userId: Long): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE user_id = :userId ORDER BY task_date, start_time, order_index")
    fun observeTasksByUserId(userId: Long): LiveData<List<Task>>

    // LiveData theo id (nullable để dễ check null khi observe)
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    fun getByIdLive(id: Long): LiveData<Task?>

    // Dùng trong coroutine (IO) để lấy 1 lần
    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Long): Task?

    @Query("SELECT * FROM tasks WHERE remote_id = :remoteId LIMIT 1")
    suspend fun getByRemoteId(remoteId: String): Task?

    // Lọc theo trạng thái
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND is_completed = 0 ORDER BY id DESC")
    fun getUncompleted(userId: Long): LiveData<List<Task>>

    @Query("SELECT * FROM tasks WHERE user_id = :userId AND is_completed = 1 ORDER BY id DESC")
    fun getCompleted(userId: Long): LiveData<List<Task>>

    // Lọc theo ngày
    @Query("SELECT * FROM tasks WHERE user_id = :userId AND task_date = :date ORDER BY start_time ASC")
    fun getByDate(userId: Long, date: String): LiveData<List<Task>>

    // ===== UPDATE =====   
    @Update
    suspend fun update(task: Task): Int

    @Query("UPDATE tasks SET is_completed = :done WHERE id = :id")
    suspend fun setCompleted(id: Long, done: Boolean): Int

    // ===== DELETE =====
    @Delete
    suspend fun delete(task: Task): Int

    @Query("DELETE FROM tasks WHERE user_id = :userId")
    suspend fun deleteByUser(userId: Long): Int

    // ===== Lấy danh sách đã sắp xếp =====

    // Toàn bộ: ưu tiên chưa hoàn thành trước, rồi theo orderIndex
    @Query("""
SELECT * FROM tasks
WHERE user_id = :userId
ORDER BY is_completed ASC, order_index ASC, id DESC
""")
    fun getByUserOrdered(userId: Long): LiveData<List<Task>>

    // Nếu bạn có 2 tab:
    @Query("""
SELECT * FROM tasks
WHERE user_id = :userId AND is_completed = 0
ORDER BY order_index ASC, id DESC
""")
    fun getUncompletedOrdered(userId: Long): LiveData<List<Task>>

    @Query("""
SELECT * FROM tasks
WHERE user_id = :userId AND is_completed = 1
ORDER BY order_index ASC, id DESC
""")
    fun getCompletedOrdered(userId: Long): LiveData<List<Task>>

    // ===== Cập nhật order_index 1 item =====
    @Query("UPDATE tasks SET order_index = :orderIndex WHERE id = :id")
    suspend fun updateOrderIndex(id: Long, orderIndex: Int)

    // (Tùy chọn) Cập nhật nhiều item trong 1 transaction cho nhanh
    @Transaction
    suspend fun updateOrderMany(pairs: List<Pair<Long, Int>>) {
        pairs.forEach { (id, idx) -> updateOrderIndex(id, idx) }
    }
}
