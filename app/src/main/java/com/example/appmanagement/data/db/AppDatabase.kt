// Lớp AppDatabase cấu hình Room Database lưu bảng người dùng và công việc, dùng singleton toàn ứng dụng
package com.example.appmanagement.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.appmanagement.data.dao.TaskDao
import com.example.appmanagement.data.dao.UserDao
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.entity.User

@Database(
    entities = [User::class, Task::class],
    version = 3,            // tăng version khi thay đổi schema
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao     // truy cập bảng users
    abstract fun taskDao(): TaskDao     // truy cập bảng tasks

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null


        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // thêm cột cho bảng users
                db.execSQL("ALTER TABLE users ADD COLUMN remote_id TEXT")
                db.execSQL("ALTER TABLE users ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")

                // thêm cột cho bảng tasks
                db.execSQL("ALTER TABLE tasks ADD COLUMN remote_id TEXT")
                db.execSQL("ALTER TABLE tasks ADD COLUMN updated_at INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // thêm cột user_remote_id để liên kết với uid Firebase
                db.execSQL("ALTER TABLE tasks ADD COLUMN user_remote_id TEXT")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "todo.db"            // tên file cơ sở dữ liệu
                )
                    // dùng migration để giữ lại dữ liệu cũ khi nâng version
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
