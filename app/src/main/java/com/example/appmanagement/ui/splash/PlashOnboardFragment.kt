package com.example.appmanagement.ui.splash

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.appmanagement.R
import com.example.appmanagement.data.db.AppDatabase
import com.example.appmanagement.data.repo.AccountRepository
import com.example.appmanagement.databinding.FragmentPsplashBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import com.example.appmanagement.util.AppGlobals

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
            delay(1200)

            val repo = AccountRepository(AppDatabase.getInstance(requireContext().applicationContext).userDao())
            val currentUser = withContext(Dispatchers.IO) { repo.getCurrentUser() }


            AppGlobals.isLoggedIn = currentUser != null
            AppGlobals.currentUserId = currentUser?.id

            if (currentUser != null) {            // hoặc if (AppGlobals.isLoggedIn)
                when (currentUser.avatarUrl) {
                    "male" -> binding.logo.setImageResource(R.drawable.avatar_male)
                    "female" -> binding.logo.setImageResource(R.drawable.avatar_female)
                    else -> binding.logo.setImageResource(R.drawable.ic_logo)
                }
                binding.tvTODOAPP.text = currentUser.name
                binding.tvAPP.text = "'s Manager"

                delay(1000)
                // pop sạch Splash khỏi back stack cho chắc
                findNavController().navigate(
                    R.id.homeFragment,
                    null,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.plashOnboardFragment) { inclusive = true }
                    }
                )
            } else {
                binding.logo.setImageResource(R.drawable.ic_logo)
                binding.tvTODOAPP.text = "TODO"
                binding.tvAPP.text = " APP"

                delay(1000)
                findNavController().navigate(
                    R.id.onboardFragment,
                    null,
                    androidx.navigation.navOptions {
                        popUpTo(R.id.plashOnboardFragment) { inclusive = true }
                    }
                )
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
