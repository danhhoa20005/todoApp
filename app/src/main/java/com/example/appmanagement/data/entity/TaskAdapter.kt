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

class TaskAdapter(
    private val onEditClick: (Task) -> Unit,      // callback khi bấm sửa
    private val onDeleteClick: (Task) -> Unit,    // callback khi bấm xóa
    private val onCheckClick: (Task) -> Unit      // callback khi đánh dấu hoàn thành
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val items = mutableListOf<Task>()     // danh sách dữ liệu hiển thị
    private var selectedId: Long? = null          // id item đang được chọn (để đổi màu)
    var dragHelper: ItemTouchHelper? = null       // hỗ trợ kéo–thả sắp xếp

    init { setHasStableIds(true) }                // bật stableId để RecyclerView tối ưu diff/animation
    override fun getItemId(position: Int) = items[position].id
    override fun getItemCount(): Int = items.size

    private class TaskDiff(
        private val old: List<Task>,
        private val new: List<Task>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = old.size
        override fun getNewListSize() = new.size
        override fun areItemsTheSame(oldPos: Int, newPos: Int): Boolean =
            old[oldPos].id == new[newPos].id           // so sánh theo id
        override fun areContentsTheSame(oldPos: Int, newPos: Int): Boolean =
            old[oldPos] == new[newPos]                 // so sánh toàn bộ nội dung
    }

    // Nạp danh sách mới: tính diff rồi cập nhật RecyclerView
    fun submitList(list: List<Task>) {
        // Nếu item đang chọn không còn trong danh sách mới thì bỏ chọn
        if (selectedId != null && list.none { it.id == selectedId }) selectedId = null
        val diff = DiffUtil.calculateDiff(TaskDiff(items, list))
        items.clear()
        items.addAll(list)
        diff.dispatchUpdatesTo(this)                   // áp dụng các thay đổi vi sai
    }

    // Hoán đổi 2 vị trí trong danh sách đang hiển thị (phục vụ kéo–thả)
    fun swapItems(from: Int, to: Int) {
        if (from == to) return
        val t = items.removeAt(from)
        items.add(to, t)
    }

    // Trả ra snapshot (id, index) hiện tại để tiện lưu/tracking thứ tự bên ngoài
    fun snapshotIdsWithIndex(): List<Pair<Long, Int>> =
        items.mapIndexed { i, t -> t.id to i }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): TaskViewHolder {
        val b = ItemTaskBinding.inflate(LayoutInflater.from(p.context), p, false)
        return TaskViewHolder(b)
    }

    override fun onBindViewHolder(h: TaskViewHolder, i: Int) {
        h.bind(items[i])
    }

    // ViewHolder: giữ tham chiếu view và bind dữ liệu cho 1 item
    inner class TaskViewHolder(private val b: ItemTaskBinding) :
        RecyclerView.ViewHolder(b.root) {

        private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) // định dạng ngày tạo

        fun bind(t: Task) {
            // Gán dữ liệu text
            b.tvTitle.text = t.title
            b.tvDetail.text = t.description
            b.tvTimeRange.text = buildTimeRange(t.startTime, t.endTime)
            b.tvCreatedDate.text = df.format(Date(t.createdAt))

            // Đổi màu nền thẻ theo trạng thái: hoàn thành / đang chọn / mặc định
            val ctx = b.root.context
            val colorRes = when {
                t.isCompleted      -> R.color.done_color
                selectedId == t.id -> R.color.selected_color
                else               -> R.color.card_dark
            }
            b.cardTask.setCardBackgroundColor(ContextCompat.getColor(ctx, colorRes))

            // Sự kiện nút Sửa/Xóa/Đánh dấu
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

            // Click vào item: cập nhật selectedId để đổi màu highlight
            b.root.setOnClickListener {
                val oldId = selectedId
                selectedId = t.id

                // Cập nhật lại item cũ (bỏ highlight)
                oldId?.let { oid ->
                    val oldPos = items.indexOfFirst { it.id == oid }
                    if (oldPos != -1) notifyItemChanged(oldPos)
                }
                // Cập nhật lại item mới (thêm highlight)
                val newPos = bindingAdapterPosition
                if (newPos != RecyclerView.NO_POSITION) notifyItemChanged(newPos)
            }

            // Nhấn giữ để bắt đầu kéo–thả (nếu dragHelper được gán từ Fragment)
            b.cardTask.isLongClickable = true
            b.cardTask.setOnLongClickListener {
                dragHelper?.startDrag(this)
                true
            }
        }

        // Ghép khoảng thời gian hiển thị: "start - end" hoặc fallback hợp lý
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
