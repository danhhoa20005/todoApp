package com.example.appmanagement.data.entity

import androidx.room.*

// Room entity describing a task owned by a specific user
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

    // Khóa chính tự tăng
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    // Khóa ngoại tham chiếu user
    @ColumnInfo(name = "user_id")
    val userId: Long,

    // Thứ tự hiển thị trong danh sách
    @ColumnInfo(name = "order_index")
    val orderIndex: Int = 0,

    // Tiêu đề công việc
    @ColumnInfo(name = "title")
    val title: String,

    // Mô tả chi tiết
    @ColumnInfo(name = "description")
    val description: String = "",

    // Cờ trạng thái hoàn thành
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    // Ngày thực hiện ví dụ: "2025-10-04"
    @ColumnInfo(name = "task_date")
    val taskDate: String = "",

    // Thời gian bắt đầu ví dụ: "08:30"
    @ColumnInfo(name = "start_time")
    val startTime: String = "",

    // Thời gian kết thúc ví dụ: "10:00"
    @ColumnInfo(name = "end_time")
    val endTime: String = "",

    // Thời điểm tạo mặc định là thời gian hiện tại
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)
