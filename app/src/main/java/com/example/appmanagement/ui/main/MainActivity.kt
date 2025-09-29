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

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Insets
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        // NavController
        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        // Gắn bottom nav với navController (nếu bạn có menu)
        binding.bottomNav.setupWithNavController(navController)

        // Chỉ hiện bottom nav ở Home
        navController.addOnDestinationChangedListener { _, dest, _ ->
            binding.bottomNav.visibility =
                if (dest.id == R.id.homeFragment) View.VISIBLE else View.GONE
        }
    }
}
