package com.example.appmanagement.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appmanagement.data.dao.TaskDao
import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.entity.User

// Room database chứa bảng người dùng và công việc
@Database(
    entities = [User::class, Task::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    // Dao truy xuất người dùng
    abstract fun userDao(): UserDao
    // Dao truy xuất công việc
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Lấy instance singleton cho toàn ứng dụng
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo.db"
                )
                    // Cho phép xoá và tạo lại cơ sở dữ liệu khi tăng version
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
