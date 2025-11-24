package com.example.appmanagement.data.remote

import com.example.appmanagement.data.entity.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore

class TaskRemoteDataSource(
    private val firestore: FirebaseFirestore
) {

    private val collection get() = firestore.collection("tasks")

    // upsertTask: thêm mới hoặc cập nhật task trên Firestore theo userRemoteId
    suspend fun upsertTask(userRemoteId: String, task: Task): String {
        val data = mapOf(
            "userId" to userRemoteId,
            "title" to task.title,
            "description" to task.description,
            "isCompleted" to task.isCompleted,
            "taskDate" to task.taskDate,
            "startTime" to task.startTime,
            "endTime" to task.endTime,
            "orderIndex" to task.orderIndex,
            "createdAt" to task.createdAt,
            "updatedAt" to task.updatedAt
        )

        return if (task.remoteId.isNullOrBlank()) {
            val docRef = Tasks.await(collection.add(data))
            docRef.id
        } else {
            Tasks.await(collection.document(task.remoteId).set(data))
            task.remoteId
        }
    }

    // deleteTask: xoá document task trên Firestore
    suspend fun deleteTask(remoteId: String) {
        Tasks.await(collection.document(remoteId).delete())
    }

    // fetchTasks: lấy danh sách task của 1 user từ Firestore
    suspend fun fetchTasks(userRemoteId: String): List<Task> {
        val snapshot = Tasks.await(collection.whereEqualTo("userId", userRemoteId).get())
        return snapshot.documents.map { doc ->
            val data = doc.data.orEmpty()
            Task(
                id = 0,
                userId = 0,
                userRemoteId = data["userId"] as? String,
                remoteId = doc.id,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                isCompleted = data["isCompleted"] as? Boolean ?: false,
                taskDate = data["taskDate"] as? String ?: "",
                startTime = data["startTime"] as? String ?: "",
                endTime = data["endTime"] as? String ?: "",
                orderIndex = (data["orderIndex"] as? Long ?: 0L).toInt(),
                createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis(),
                updatedAt = data["updatedAt"] as? Long ?: System.currentTimeMillis()
            )
        }
    }
}
