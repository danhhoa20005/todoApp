package com.example.appmanagement.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.data.pref.UserPreferences
import com.example.appmanagement.databinding.FragmentPsplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PlashOnboardFragment : Fragment() {

    private var _binding: FragmentPsplashBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPsplashBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            delay(1200) // hiệu ứng splash nhỏ

            val prefs = UserPreferences(requireContext().applicationContext)
            val loggedIn = prefs.isLoggedInFlow.first()

            if (!isAdded || _binding == null) return@launch

            if (loggedIn) {
                // Nếu muốn sang màn chính thì navigate thẳng tới createWorkFragment
                //findNavController().navigate(R.id.createWorkFragment)
                findNavController().navigate(R.id.action_plash_to_onboard)
            } else {
                // Nếu chưa đăng nhập thì sang Onboard (đúng với action trong nav_graph)
                findNavController().navigate(R.id.action_plash_to_onboard)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
