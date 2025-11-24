// Fragment EditFragment phục vụ chỉnh sửa thông tin một công việc hiện có thông qua Safe Args
package com.proptit.room_database.ui.addedit

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.databinding.FragmentEditBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

class EditFragment : Fragment() {

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!

    // Safe Args: nav_graph cần <argument name="editId" app:argType="long" />
    private val args: EditFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy DB singleton
        val db = AppDatabase.getInstance(requireContext())
        val taskDao = db.taskDao()

        // 1) Nạp task theo id và đổ lên UI
        taskDao.getByIdLive(args.editId).observe(viewLifecycleOwner) { t ->
            t ?: return@observe
            binding.edtTitle.setText(t.title)
            binding.edtDetail.setText(t.description)
            binding.edtDate.setText(t.taskDate)
            binding.edtStartTime.setText(t.startTime)
            binding.edtEndTime.setText(t.endTime)

            // Gắn trạng thái hoàn thành vào switch hiển thị
            binding.switchCompleted.isChecked = t.isCompleted
            // (màu chữ của switch đã set cố định #40FFFFFF trong XML nên không cần code)
        }

        // 2) Pickers
        binding.edtDate.setOnClickListener { showDatePicker() }
        binding.edtStartTime.setOnClickListener { showTimePicker(isStart = true) }
        binding.edtEndTime.setOnClickListener { showTimePicker(isStart = false) }

        // 3) Lưu cập nhật
        binding.btnEditTask.setOnClickListener {
            val title = binding.edtTitle.text?.toString()?.trim().orEmpty()
            val detail = binding.edtDetail.text?.toString()?.trim().orEmpty()
            val date = binding.edtDate.text?.toString()?.trim().orEmpty()
            val start = binding.edtStartTime.text?.toString()?.trim().orEmpty()
            val end = binding.edtEndTime.text?.toString()?.trim().orEmpty()
            val completed = binding.switchCompleted.isChecked  // Lấy trạng thái hoàn thành từ switch

            if (title.isBlank()) {
                binding.edtTitle.error = "Vui lòng nhập tiêu đề"
                return@setOnClickListener
            }
            if (date.isBlank()) {
                binding.edtDate.error = "Vui lòng chọn ngày"
                return@setOnClickListener
            }

            viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                val cur = taskDao.getByIdOnce(args.editId) ?: return@launch
                val updated = cur.copy(
                    title = title,
                    description = detail,
                    taskDate = date,
                    startTime = start,
                    endTime = end,
                    isCompleted = completed   // Lưu trạng thái hoàn thành khi cập nhật
                )
                taskDao.update(updated)
                withContext(Dispatchers.Main) { findNavController().navigateUp() }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }



    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d ->
                binding.edtDate.setText(String.format("%02d/%02d/%04d", d, m + 1, y))
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker(isStart: Boolean) {
        val c = Calendar.getInstance()
        TimePickerDialog(
            requireContext(),
            { _, hh, mm ->
                val txt = String.format("%02d:%02d", hh, mm)
                if (isStart) binding.edtStartTime.setText(txt) else binding.edtEndTime.setText(txt)
            },
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            true
        ).show()
    }
}
