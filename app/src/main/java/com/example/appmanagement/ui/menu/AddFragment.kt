// Fragment AddFragment cung cấp biểu mẫu tạo công việc mới với kiểm tra ngày giờ chi tiết
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
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.remote.TaskRemoteDataSource
import com.example.appmanagement.data.repo.TaskRepository
import com.example.appmanagement.databinding.FragmentAddBinding
import com.example.appmanagement.notifications.NotificationScheduler
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Calendar

class AddFragment : Fragment() {

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    private var isSaving = false
    private var addJob: Job? = null

    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    private val timeFmt = DateTimeFormatter.ofPattern("HH:mm")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // mở picker đúng theo XML
        binding.edtDate.setOnClickListener { showDatePicker() }
        binding.edtStartTime.setOnClickListener { showTimePicker(isStart = true) }
        binding.edtEndTime.setOnClickListener { showTimePicker(isStart = false) }

        // lưu
        binding.btnAddTask.setOnClickListener { addTask() }
    }

    private fun addTask() {
        if (isSaving || addJob?.isActive == true) return

        val title = binding.edtTitle.text?.toString()?.trim().orEmpty()
        val detail = binding.edtDetail.text?.toString()?.trim().orEmpty()
        val date = binding.edtDate.text?.toString()?.trim().orEmpty()
        val start = binding.edtStartTime.text?.toString()?.trim().orEmpty()
        val end = binding.edtEndTime.text?.toString()?.trim().orEmpty()

        // xoá lỗi cũ
        binding.edtTitle.error = null
        binding.edtDate.error = null
        binding.edtStartTime.error = null
        binding.edtEndTime.error = null

        // --- validate tối thiểu theo đúng form ---
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

        // nếu có nhập giờ thì yêu cầu đủ cả 2 và start <= end
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
        // -----------------------------------------

        setSaving(true)
        addJob = viewLifecycleOwner.lifecycleScope.launch {
            val appContext = requireContext().applicationContext
            try {
                val db = AppDatabase.getInstance(appContext)
                val currentUser = withContext(Dispatchers.IO) {
                    AccountRepository(db.userDao()).getCurrentUser()
                }
                if (currentUser == null) {
                    Toast.makeText(requireContext(), "Chưa có người dùng đăng nhập", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val taskRepository = TaskRepository(
                    dao = db.taskDao(),
                    userDao = db.userDao(),
                    remoteDataSource = TaskRemoteDataSource(FirebaseFirestore.getInstance())
                )

                val insertedId = withContext(Dispatchers.IO) {
                    taskRepository.add(
                        user = currentUser,
                        title = title,
                        description = detail,
                        taskDate = date,
                        startTime = start,
                        endTime = end
                    )
                }
                if (insertedId > 0) {
                    if (startObj != null && dateObj != null) {
                        NotificationScheduler.scheduleTaskReminder(
                            context = appContext,
                            taskId = insertedId,
                            title = title,
                            date = date,
                            startTime = start
                        )
                    }
                    Toast.makeText(requireContext(), "Đã thêm công việc", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_addFragment_to_homeFragment)
                } else {
                    Toast.makeText(requireContext(), "Thêm thất bại", Toast.LENGTH_SHORT).show()
                }
            } catch (t: Throwable) {
                Toast.makeText(requireContext(), "Thêm thất bại: ${t.message ?: "Lỗi không xác định"}", Toast.LENGTH_SHORT).show()
            } finally {
                setSaving(false)
                addJob = null
            }
        }
    }

    private fun setSaving(saving: Boolean) {
        isSaving = saving
        binding.btnAddTask.isEnabled = !saving
        binding.btnAddTask.alpha = if (saving) 0.6f else 1f
    }

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
