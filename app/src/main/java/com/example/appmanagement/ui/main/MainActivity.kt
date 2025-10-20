// Tập tin định nghĩa MainActivity chịu trách nhiệm khởi tạo giao diện chính và điều hướng BottomNavigation
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
import com.google.android.material.navigation.NavigationBarView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Áp padding cho root theo system bars (status + navigation bar)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Lấy NavController từ NavHostFragment
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Các màn hình top-level (tức là các tab trong BottomNavigation)
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

        // Gắn NavigationController cho BottomNavigationView
        binding.bottomNav.labelVisibilityMode = NavigationBarView.LABEL_VISIBILITY_LABELED
        binding.bottomNav.setupWithNavController(navController)

        // Ẩn BottomNavigation ở các màn hình không thuộc top-level
        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.visibility =
                if (destination.id in topLevelDestinations) View.VISIBLE else View.GONE
        }
    }
}
