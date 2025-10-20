// Lớp AppDatabase cấu hình Room Database lưu bảng người dùng và công việc với singleton dùng toàn ứng dụng
package com.example.appmanagement.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appmanagement.data.dao.TaskDao
import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.entity.User

@Database(
    entities = [User::class, Task::class],
    version = 1, // version hiện tại
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo.db"
                )
                    // Dòng fallback bảo đảm xóa cơ sở dữ liệu cũ khi tăng version để tránh xung đột lược đồ
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
