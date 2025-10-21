package com.example.appmanagement.util

import com.example.appmanagement.data.entity.Task

// Utilities for searching through task fields using user input
object TaskSearch {

    // Filter tasks whose metadata contains the provided keyword
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
