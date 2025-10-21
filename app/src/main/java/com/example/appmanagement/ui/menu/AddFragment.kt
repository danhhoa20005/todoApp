package com.example.appmanagement.ui.menu

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.entity.Task
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.databinding.FragmentAddBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar

// Fragment tạo công việc mới với form nhập ngày và giờ
class AddFragment : Fragment() {

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    // Khởi tạo binding cho layout thêm công việc
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Gắn sự kiện cho form sau khi view được tạo
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.edtDate.setOnClickListener { showDatePicker() }
        binding.edtStartTime.setOnClickListener { showTimePicker(isStart = true) }
        binding.edtEndTime.setOnClickListener { showTimePicker(isStart = false) }

        binding.btnAddTask.setOnClickListener { addTask() }
    }

    // Thu thập dữ liệu, kiểm tra hợp lệ và lưu công việc mới
    private fun addTask() {
        val title = binding.edtTitle.text?.toString()?.trim().orEmpty()
        val detail = binding.edtDetail.text?.toString()?.trim().orEmpty()
        val date = binding.edtDate.text?.toString()?.trim().orEmpty()
        val start = binding.edtStartTime.text?.toString()?.trim().orEmpty()
        val end = binding.edtEndTime.text?.toString()?.trim().orEmpty()

        binding.edtTitle.error = null
        binding.edtDate.error = null
        binding.edtStartTime.error = null
        binding.edtEndTime.error = null

        var ok = true

        if (title.isEmpty()) {
            binding.edtTitle.error = "Vui lòng nhập tiêu đề"
            ok = false
        }

        val dateObj: LocalDate? = try {
            if (date.isEmpty()) {
                binding.edtDate.error = "Vui lòng chọn ngày"
                ok = false
                null
            } else LocalDate.parse(date, dateFmt)
        } catch (_: DateTimeParseException) {
            binding.edtDate.error = "Định dạng ngày dd/MM/yyyy"
            ok = false
            null
        }

        val hasStart = start.isNotEmpty()
        val hasEnd = end.isNotEmpty()
        var startObj: LocalTime? = null
        var endObj: LocalTime? = null

        if (hasStart xor hasEnd) {
            if (!hasStart) binding.edtStartTime.error = "Chọn giờ bắt đầu"
            if (!hasEnd) binding.edtEndTime.error = "Chọn giờ kết thúc"
            ok = false
        } else if (hasStart && hasEnd) {
            try { startObj = LocalTime.parse(start, timeFmt) }
            catch (_: DateTimeParseException) { binding.edtStartTime.error = "Định dạng HH:mm"; ok = false }
            try { endObj = LocalTime.parse(end, timeFmt) }
            catch (_: DateTimeParseException) { binding.edtEndTime.error = "Định dạng HH:mm"; ok = false }

            if (startObj != null && endObj != null && startObj.isAfter(endObj)) {
                binding.edtStartTime.error = "Bắt đầu phải trước kết thúc"
                binding.edtEndTime.error = "Kết thúc phải sau bắt đầu"
                ok = false
            }
        }

        if (!ok) return
        if (dateObj == null) return

        viewLifecycleOwner.lifecycleScope.launch {
            val db = AppDatabase.getInstance(requireContext().applicationContext)
            val currentUser = withContext(Dispatchers.IO) {
                AccountRepository(db.userDao()).getCurrentUser()
            }
            if (currentUser == null) {
                Toast.makeText(requireContext(), "Chưa có người dùng đăng nhập", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val task = Task(
                id = 0L,
                userId = currentUser.id,
                title = title,
                description = detail,
                isCompleted = false,
                taskDate = date,
                startTime = start,
                endTime = end
            )

            val insertedId = withContext(Dispatchers.IO) { db.taskDao().insert(task) }
            if (insertedId > 0) {
                Toast.makeText(requireContext(), "Đã thêm công việc", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_addFragment_to_homeFragment)
            } else {
                Toast.makeText(requireContext(), "Thêm thất bại", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Hiển thị DatePicker và gán kết quả vào trường ngày
    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d -> binding.edtDate.setText(String.format("%02d/%02d/%04d", d, m + 1, y)) },
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
                val t = String.format("%02d:%02d", hh, mm)
                if (isStart) binding.edtStartTime.setText(t) else binding.edtEndTime.setText(t)
            },
            c.get(Calendar.HOUR_OF_DAY),
            c.get(Calendar.MINUTE),
            true
        ).show()
    }

    // Dọn binding khi view bị huỷ
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
