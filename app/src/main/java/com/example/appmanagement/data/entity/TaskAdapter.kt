package com.example.appmanagement.ui.menu

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.appmanagement.R
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

// RecyclerView adapter quản lý danh sách công việc với thao tác chọn và kéo thả
class TaskAdapter(
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onCheckClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    // Bộ nhớ tạm cho danh sách hiện tại
    private val items = mutableListOf<Task>()
    // Id item đang được chọn để đổi màu nền
    private var selectedId: Long? = null
    // Hỗ trợ kéo thả từ bên ngoài
    var dragHelper: ItemTouchHelper? = null

    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = items[position].id
    override fun getItemCount(): Int = items.size

    // DiffUtil để so sánh danh sách cũ và mới
    private class TaskDiff(
        private val old: List<Task>,
        private val new: List<Task>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
            old[oldPos].id == new[newPos].id
        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
            old[oldPos] == new[newPos]
    }

    // Cập nhật danh sách và xử lý trạng thái chọn hiện tại
    fun submitList(list: List<Task>) {
        if (selectedId != null && list.none { it.id == selectedId }) selectedId = null
        val diff = DiffUtil.calculateDiff(TaskDiff(items, list))
        items.clear()
        items.addAll(list)
        diff.dispatchUpdatesTo(this)
    }

    // Đổi chỗ hai phần tử trong danh sách tạm khi kéo thả
    fun swapItems(from: Int, to: Int) {
        if (from == to) return
        val t = items.removeAt(from)
        items.add(to, t)
    }

    // Lấy ảnh chụp của id và vị trí để lưu thứ tự
    fun snapshotIdsWithIndex(): List<Pair<Long, Int>> =
        items.mapIndexed { i, t -> t.id to i }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): TaskViewHolder {
        val b = ItemTaskBinding.inflate(LayoutInflater.from(p.context), p, false)
        return TaskViewHolder(b)
    }

    override fun onBindViewHolder(h: TaskViewHolder, i: Int) {
        h.bind(items[i])
    }

    // ViewHolder hiển thị thông tin từng công việc
    inner class TaskViewHolder(private val b: ItemTaskBinding) :
        RecyclerView.ViewHolder(b.root) {

        // Định dạng ngày tạo công việc
        private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // Gán dữ liệu và xử lý sự kiện cho item
        fun bind(t: Task) {
            b.tvTitle.text = t.title
            b.tvDetail.text = t.description
            b.tvTimeRange.text = buildTimeRange(t.startTime, t.endTime)
            b.tvCreatedDate.text = df.format(Date(t.createdAt))

            val ctx = b.root.context
            val colorRes = when {
                t.isCompleted      -> R.color.done_color
                selectedId == t.id -> R.color.selected_color
                else               -> R.color.card_dark
            }
            b.cardTask.setCardBackgroundColor(ContextCompat.getColor(ctx, colorRes))

            b.btnEdit.setOnClickListener { onEditClick(t) }
            b.btnDelete.setOnClickListener {
                AlertDialog.Builder(ctx)
                    .setTitle("Xác nhận")
                    .setMessage("Bạn có chắc muốn xoá công việc này không?")
                    .setPositiveButton("Có") { _, _ -> onDeleteClick(t) }
                    .setNegativeButton("Không", null)
                    .show()
            }
            b.btnCheck.setOnClickListener { onCheckClick(t) }

            // Nhấn chọn để đổi màu item
            b.root.setOnClickListener {
                val oldId = selectedId
                selectedId = t.id

                oldId?.let { oid ->
                    val oldPos = items.indexOfFirst { it.id == oid }
                    if (oldPos != -1) notifyItemChanged(oldPos)
                }
                val newPos = bindingAdapterPosition
                if (newPos != RecyclerView.NO_POSITION) notifyItemChanged(newPos)
            }

            // Nhấn giữ để kích hoạt kéo thả
            b.cardTask.isLongClickable = true
            b.cardTask.setOnLongClickListener {
                dragHelper?.startDrag(this)
                true
            }
        }

        // Ghép chuỗi thời gian hiển thị cho item
        private fun buildTimeRange(s: String, e: String): String {
            val a = s.trim(); val b2 = e.trim()
            return when {
                a.isNotEmpty() && b2.isNotEmpty() -> "$a - $b2"
                a.isNotEmpty() -> a
                b2.isNotEmpty() -> b2
                else -> "Không có thời gian"
            }
        }
    }
}
