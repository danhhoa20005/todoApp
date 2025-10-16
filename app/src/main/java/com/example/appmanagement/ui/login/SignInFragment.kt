package com.example.appmanagement.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.data.viewmodel.SignInViewModel
import com.example.appmanagement.databinding.FragmentSignInBinding
import com.example.appmanagement.util.AppGlobals

class SignInFragment : Fragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SignInViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Prefill email passed from SignEmailFragment
        val emailFromArgs = arguments?.getString("email").orEmpty()
        binding.tvEmailDynamic.setText(emailFromArgs)

        // Back
        binding.btnBack?.setOnClickListener { findNavController().popBackStack() }

        // Sign in
        binding.btnSignIn.setOnClickListener {
            val email = binding.tvEmailDynamic.text?.toString()?.trim().orEmpty()
            val password = binding.edtPassword.text?.toString()?.trim().orEmpty()

            if (email.isEmpty() || password.isEmpty()) {
                showToast("Please enter both email and password")
                return@setOnClickListener
            }

            setLoading(true)
            viewModel.login(email, password)
        }

        // Observe login result
        viewModel.loginResult.observe(viewLifecycleOwner) { success ->
            setLoading(false)
            if (success == true) {
                AppGlobals.isLoggedIn = true
                findNavController().navigate(R.id.action_signInFragment_to_homeFragment)
            } else {
                showToast("Incorrect email or password")
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.btnSignIn.isEnabled = !loading
        binding.edtPassword.isEnabled = !loading
        // If you have a ProgressBar in the layout, toggle it here.
        // binding.progressBar.isVisible = loading
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
