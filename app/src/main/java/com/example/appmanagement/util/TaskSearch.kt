// Đối tượng TaskSearch cung cấp tiện ích lọc danh sách công việc theo từ khóa người dùng nhập
package com.example.appmanagement.util

import com.example.appmanagement.data.entity.Task

object TaskSearch {

    /** Lọc danh sách theo chuỗi nhập (title/description/day/start/end). */
    fun filter(all: List<Task>, raw: String): List<Task> {
        val key = raw.trim().lowercase()
        if (key.isEmpty()) return emptyList()
        return all.filter {
            val t = it.title.lowercase()
            val d = it.description.lowercase()
            val day = it.taskDate.lowercase()
            val s = it.startTime.lowercase()
            val e = it.endTime.lowercase()
            t.contains(key) || d.contains(key) || day.contains(key) || s.contains(key) || e.contains(key)
        }
    }
}
