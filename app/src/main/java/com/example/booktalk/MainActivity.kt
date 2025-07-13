package com.example.booktalk

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.cloudinary.android.MediaManager
import com.example.booktalk.databinding.ActivityMainBinding
import com.cloudinary.Cloudinary
import com.cloudinary.utils.ObjectUtils


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // BottomNavigation setup
        NavigationUI.setupWithNavController(binding.bottomNavigationView, navController)

        // Hide bottom nav in login/register fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment || destination.id == R.id.registerFragment) {
                binding.bottomNavigationView.visibility = android.view.View.GONE
            } else {
                binding.bottomNavigationView.visibility = android.view.View.VISIBLE
            }
        }

        // Initialize Cloudinary
        val config: HashMap<String, String> = HashMap()
        config["cloud_name"] = "ddaexvcdu"
        config["api_key"] = "269842741757611"
        config["api_secret"] = "mqC83IzTe8Ar4IZVeX4V9Po2nV8"

    }
}
