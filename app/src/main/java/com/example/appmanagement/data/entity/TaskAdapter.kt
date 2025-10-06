package com.example.appmanagement.ui.menu

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.databinding.ItemTaskBinding

class TaskAdapter(
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onCheckClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.TaskViewHolder>(DiffCallback()) {

    inner class TaskViewHolder(private val binding: ItemTaskBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            binding.tvTitle.text = task.title
            binding.tvDetail.text = task.description

            // Nếu task đã hoàn thành → đổi màu mờ và ẩn nút check
            if (task.isCompleted) {
                binding.tvTitle.alpha = 0.5f
                binding.tvDetail.alpha = 0.5f
                binding.btnCheck.alpha = 0.3f
            } else {
                binding.tvTitle.alpha = 1f
                binding.tvDetail.alpha = 1f
                binding.btnCheck.alpha = 1f
            }

            // Sự kiện khi nhấn nút Sửa
            binding.btnEdit.setOnClickListener {
                onEditClick(task)
            }

            // Sự kiện khi nhấn nút Xóa
            binding.btnDelete.setOnClickListener {
                onDeleteClick(task)
            }

            // Sự kiện khi nhấn nút Check (hoàn thành)
            binding.btnCheck.setOnClickListener {
                onCheckClick(task)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Task, newItem: Task): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding =
            ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}
