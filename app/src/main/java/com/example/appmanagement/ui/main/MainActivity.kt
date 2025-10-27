package com.example.appmanagement.ui.main

import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.appmanagement.R
import com.example.appmanagement.databinding.ActivityMainBinding
import com.example.appmanagement.util.ThemePreferences
import com.google.android.material.navigation.NavigationBarView

// Activity chính chứa NavHost và điều khiển BottomNavigation
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Thiết lập điều hướng và xử lý insets khi Activity khởi tạo
    override fun onCreate(savedInstanceState: Bundle?) {
        ThemePreferences.applySavedTheme(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val topLevelDestinations = setOf(
            R.id.homeFragment,
            R.id.addFragment,
            R.id.listFragment,
            R.id.calendarFragment,
            R.id.clockFragment,
            R.id.doneFragment,
            R.id.todayFragment,
            R.id.dateDetailFragment,
            R.id.editFragment
        )

        binding.bottomNav.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        binding.bottomNav.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in topLevelDestinations) View.VISIBLE else View.GONE
        }
    }
}
