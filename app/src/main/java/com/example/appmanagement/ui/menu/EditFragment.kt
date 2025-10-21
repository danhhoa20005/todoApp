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

// Fragment chỉnh sửa thông tin công việc hiện có
class EditFragment : Fragment() {

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!

    // Safe Args truyền id công việc cần chỉnh sửa
    private val args: EditFragmentArgs by navArgs()

    // Khởi tạo view binding cho layout chỉnh sửa
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Nạp dữ liệu task và thiết lập các hành động khi view được tạo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())
        val taskDao = db.taskDao()

        taskDao.getByIdLive(args.editId).observe(viewLifecycleOwner) { t ->
            t ?: return@observe
            binding.edtTitle.setText(t.title)
            binding.edtDetail.setText(t.description)
            binding.edtDate.setText(t.taskDate)
            binding.edtStartTime.setText(t.startTime)
            binding.edtEndTime.setText(t.endTime)
            binding.switchCompleted.isChecked = t.isCompleted
        }

        binding.edtDate.setOnClickListener { showDatePicker() }
        binding.edtStartTime.setOnClickListener { showTimePicker(isStart = true) }
        binding.edtEndTime.setOnClickListener { showTimePicker(isStart = false) }

        binding.btnEditTask.setOnClickListener {
            val title = binding.edtTitle.text?.toString()?.trim().orEmpty()
            val detail = binding.edtDetail.text?.toString()?.trim().orEmpty()
            val date = binding.edtDate.text?.toString()?.trim().orEmpty()
            val start = binding.edtStartTime.text?.toString()?.trim().orEmpty()
            val end = binding.edtEndTime.text?.toString()?.trim().orEmpty()
            val completed = binding.switchCompleted.isChecked

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
                    isCompleted = completed
                )
                taskDao.update(updated)
                withContext(Dispatchers.Main) { findNavController().navigateUp() }
            }
        }
    }

    // Dọn binding khi view bị huỷ
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Hiển thị DatePicker để người dùng chọn ngày
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

    // Hiển thị TimePicker cho giờ bắt đầu hoặc kết thúc
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
