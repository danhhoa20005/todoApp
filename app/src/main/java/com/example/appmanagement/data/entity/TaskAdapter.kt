package com.example.appmanagement.ui.menu

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.example.appmanagement.R
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.databinding.ItemTaskBinding
import java.text.SimpleDateFormat
import java.util.*

class TaskAdapter(
    private val onEditClick: (Task) -> Unit,
    private val onDeleteClick: (Task) -> Unit,
    private val onCheckClick: (Task) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    private val items = mutableListOf<Task>()
    private var selectedId: Long? = null
    var dragHelper: ItemTouchHelper? = null

    init { setHasStableIds(true) }
    override fun getItemId(position: Int) = items[position].id
    override fun getItemCount(): Int = items.size

    inner class TaskViewHolder(private val b: ItemTaskBinding) :
        RecyclerView.ViewHolder(b.root) {

        private val df = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
                    .setMessage("Bạn có chắc muốn xóa công việc này không?")
                    .setPositiveButton("Có") { _, _ -> onDeleteClick(t) }
                    .setNegativeButton("Không", null)
                    .show()
            }
            b.btnCheck.setOnClickListener { onCheckClick(t) }

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

            // Long-press để kéo thả
            b.cardTask.isLongClickable = true
            b.cardTask.setOnLongClickListener {
                dragHelper?.startDrag(this)
                true
            }
        }

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

    fun submitDataOnce(list: List<Task>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    // chỉ đổi thứ tự trong working list; notifyItemMoved() đã gọi ở Fragment
    fun swapItems(from: Int, to: Int) {
        if (from == to) return
        val t = items.removeAt(from)
        items.add(to, t)
    }

    fun snapshotIdsWithIndex(): List<Pair<Long, Int>> =
        items.mapIndexed { i, t -> t.id to i }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): TaskViewHolder {
        val b = ItemTaskBinding.inflate(LayoutInflater.from(p.context), p, false)
        return TaskViewHolder(b)
    }

    override fun onBindViewHolder(h: TaskViewHolder, i: Int) {
        h.bind(items[i])
    }
}
