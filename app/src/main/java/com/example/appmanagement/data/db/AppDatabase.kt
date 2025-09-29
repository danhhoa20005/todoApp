package com.example.appmanagement.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.User

// RoomDatabase: điểm vào của Room, cung cấp các DAO
@Database(
    entities = [User::class], // hiện tại chỉ có User
    version = 2,
    exportSchema = false   // tránh warning nếu chưa config schema
)
abstract class AppDatabase : RoomDatabase() {

    // khai báo DAO để Room tự sinh code triển khai
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton: chỉ khởi tạo 1 DB cho toàn app
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo.db" // tên file DB
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
