package com.example.appmanagement.ui.home

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.data.viewmodel.CreateWorkViewModel
import com.example.appmanagement.databinding.FragmentCreateWorkBinding
import java.util.Calendar

// Fragment cho màn "Tạo thông tin làm việc"/cập nhật hồ sơ ban đầu
class CreateWorkFragment : Fragment() {

    // ViewBinding cho layout fragment_create_work.xml
    private var _binding: FragmentCreateWorkBinding? = null
    private val binding get() = _binding!!

    // Cấp phát ViewModel có tham số bằng Factory (lấy UserDao từ Room → AccountRepository → ViewModel)
    private val viewModel: CreateWorkViewModel by viewModels {
        val db = AppDatabase.getInstance(requireContext())
        CreateWorkViewModel.provideFactory(AccountRepository(db.userDao()))
    }

    // Biến lưu trạng thái lựa chọn avatar hiện tại: null | "male" | "female"
    private var selectedAvatarKey: String? = null

    // Khởi tạo ViewBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateWorkBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Thiết lập UI, lắng nghe sự kiện, bắt đầu observe và yêu cầu ViewModel tải dữ liệu
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Nút quay lại màn hình Onboard
        binding.btnBack1?.setOnClickListener {
            findNavController().navigate(R.id.action_createWorkFragment_to_onboardFragment)
        }

        // Chọn/đổi avatar khi nhấn ảnh
        binding.imgAvatar?.setOnClickListener { toggleAvatar() }

        // Mở DatePicker để chọn ngày sinh
        binding.edtDob?.setOnClickListener { showDatePicker() }

        // Lưu thông tin hồ sơ
        binding.btnNext?.setOnClickListener { onClickSave() }

        // Bắt đầu quan sát dữ liệu từ ViewModel
        observeUser()
        observeSaveState()

        // Tải người dùng hiện tại (nếu có)
        viewModel.loadUser()
    }

    // Giải phóng binding để tránh memory leak
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // Quan sát thông tin người dùng từ ViewModel.currentUser và đổ lên UI
    private fun observeUser() {
        viewModel.currentUser.observe(viewLifecycleOwner) { user ->
            user ?: return@observe

            // Gán tên và ngày sinh
            binding.edtUsername?.setText(user.name)
            binding.edtDob?.setText(user.birthDate)

            // Hiển thị avatar theo key đang lưu trong DB
            when (user.avatarUrl) {
                "male" -> setAvatar("male", true)   // applyStateOnly = true để chỉ hiển thị, không thay đổi biến selectedAvatarKey
                "female" -> setAvatar("female", true)
                else -> {
                    // Chưa có avatar → về mặc định
                    selectedAvatarKey = null
                    binding.imgAvatar?.setImageResource(R.drawable.ic_logo)
                }
            }
        }
    }

    // Quan sát cờ lưu thành công từ ViewModel.isSaveSuccessful để điều hướng/hiển thị thông báo
    private fun observeSaveState() {
        viewModel.isSaveSuccessful.observe(viewLifecycleOwner) { success ->
            if (success == true) {
                toast("Đã lưu thông tin")
                findNavController().navigate(R.id.homeFragment)
                // Reset cờ để tránh xử lý lặp lại khi cấu hình thay đổi
                viewModel.resetSuccess()
            }
        }
    }

    // Validate dữ liệu đầu vào và gọi ViewModel cập nhật
    private fun onClickSave() {
        val name = binding.edtUsername?.text?.toString()?.trim().orEmpty()
        val dob = binding.edtDob?.text?.toString()?.trim().orEmpty()

        // Kiểm tra tên
        if (name.isEmpty()) {
            binding.edtUsername?.error = "Vui lòng nhập tên"
            return
        }
        // Kiểm tra ngày sinh
        if (dob.isEmpty()) {
            binding.edtDob?.error = "Vui lòng chọn ngày sinh"
            return
        }

        // Nếu chưa chọn avatar thì gán mặc định là "male" để có giá trị hợp lệ
        if (selectedAvatarKey == null) setAvatar("male")

        // Gọi ViewModel cập nhật hồ sơ
        viewModel.updateProfile(name, dob, selectedAvatarKey)
    }

    // Đổi trạng thái avatar giữa male ↔ female mỗi lần nhấn ảnh
    private fun toggleAvatar() {
        selectedAvatarKey = when (selectedAvatarKey) {
            null -> "male"
            "male" -> "female"
            "female" -> "male"
            else -> "male"
        }
        applyAvatar(selectedAvatarKey)
    }

    // Thiết lập avatar theo key; applyStateOnly = true thì chỉ cập nhật UI, không sửa selectedAvatarKey
    private fun setAvatar(key: String, applyStateOnly: Boolean = false) {
        if (!applyStateOnly) selectedAvatarKey = key
        applyAvatar(key)
    }

    // Hiển thị ảnh avatar đúng với key hiện tại
    private fun applyAvatar(key: String?) {
        when (key) {
            "male" -> binding.imgAvatar?.setImageResource(R.drawable.avatar_male)
            "female" -> binding.imgAvatar?.setImageResource(R.drawable.avatar_female)
            else -> binding.imgAvatar?.setImageResource(R.drawable.ic_logo)
        }
    }

    // Hiển thị DatePicker và đổ kết quả về edtDob theo định dạng dd/MM/yyyy
    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                binding.edtDob?.setText(String.format("%02d/%02d/%04d", day, month + 1, year))
            },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    // Tiện ích hiển thị Toast ngắn
    private fun toast(message: String) =
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}
