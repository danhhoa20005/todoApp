package com.example.todoapp

data class Task(
    var id: Int = 0,
    var title: String,
    var description: String,
    var startTime: String, // HH:mm
    var endTime: String,   // HH:mm
    var date: String,      // yyyy-MM-dd
    var isCompleted: Boolean = false
)