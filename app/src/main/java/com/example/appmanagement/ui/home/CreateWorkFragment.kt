package com.example.appmanagement.ui.home

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.pref.UserPreferences
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.databinding.FragmentCreateWorkBinding
import kotlinx.coroutines.launch
import java.util.Calendar

class CreateWorkFragment : Fragment() {

    private var _binding: FragmentCreateWorkBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CreateWorkViewModel
    private var pickedAvatar: Uri? = null


    private val pickAvatar =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
            uri?.let {
                pickedAvatar = it
                binding.imgAvatar?.setImageURI(it)
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateWorkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        val db = AppDatabase.getInstance(requireContext())
        val prefs = UserPreferences(requireContext())
        val repo = AccountRepository(db.userDao(), prefs)
        viewModel = ViewModelProvider(
            this,
            CreateWorkViewModelFactory(repo)
        )[CreateWorkViewModel::class.java]


        binding.btnBack1?.setOnClickListener {
            findNavController().navigate(R.id.action_createWorkFragment_to_onboardFragment)
        }
        // Chọn avatar
        binding.imgAvatar?.setOnClickListener(OnClickListener {
            pickAvatar.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        })

        // Chọn ngày sinh
        binding.edtDob?.setOnClickListener { showDatePicker() }

        // Lưu & sang Home
        binding.btnNext?.setOnClickListener(OnClickListener {
            val username = binding.edtUsername?.text?.toString()?.trim().orEmpty()
            val dob = binding.edtDob?.text?.toString()?.trim().orEmpty()
            val avatarUrl = pickedAvatar?.toString()
            viewModel.updateProfile(username, dob, avatarUrl)
        })

        // Quan sát cập nhật thành công -> điều hướng Home
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.success.collect { ok ->
                if (ok) findNavController().navigate(R.id.homeFragment)
            }
        }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, y, m, d -> binding.edtDob?.setText(String.format("%02d/%02d/%04d", d, m + 1, y)) },
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH),
            c.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
