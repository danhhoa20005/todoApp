// Thực thể Task lưu thông tin công việc gắn với người dùng
package com.example.appmanagement.data.entity

import androidx.room.*

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["user_id"])]
)
data class Task(

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,               // id tự tăng

    @ColumnInfo(name = "user_id")
    val userId: Long,                // id user sở hữu task

    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,         // thứ tự hiển thị

    @ColumnInfo(name = "title")
    val title: String,               // tiêu đề công việc

    @ColumnInfo(name = "description")
    val description: String = "",    // mô tả chi tiết

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,// đã hoàn thành hay chưa

    @ColumnInfo(name = "task_date")
    val taskDate: String = "",       // ngày thực hiện (yyyy-MM-dd)

    @ColumnInfo(name = "start_time")
    val startTime: String = "",      // giờ bắt đầu (HH:mm)

    @ColumnInfo(name = "end_time")
    val endTime: String = "",        // giờ kết thúc (HH:mm)

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(), // thời điểm tạo

    @ColumnInfo(name = "remote_id")
    val remoteId: String? = null,    // id document trên Firestore

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()  // thời điểm cập nhật gần nhất
)
