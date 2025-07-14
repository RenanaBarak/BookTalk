package com.example.booktalk.controller

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import com.example.booktalk.R
import com.example.booktalk.databinding.ActivityMainBinding

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

        // Set up BottomNavigationView manually to avoid "same destination" issue
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val currentDestination = navController.currentDestination?.id
            when (item.itemId) {
                R.id.feedFragment -> {
                    if (currentDestination != R.id.feedFragment) {
                        navController.navigate(R.id.feedFragment)
                    }
                    true
                }
                R.id.createPostFragment -> {
                    if (currentDestination != R.id.createPostFragment) {
                        navController.navigate(R.id.createPostFragment)
                    }
                    true
                }
                R.id.profileFragment -> {
                    if (currentDestination != R.id.profileFragment) {
                        navController.navigate(R.id.profileFragment)
                    }
                    true
                }
                else -> false
            }
        }

        // Hide bottom nav in login/register fragments
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.loginFragment || destination.id == R.id.registerFragment) {
                binding.bottomNavigationView.visibility = View.GONE
            } else {
                binding.bottomNavigationView.visibility = View.VISIBLE
            }
        }

        // Initialize Cloudinary (if needed)
        val config: HashMap<String, String> = HashMap()
        config["cloud_name"] = "ddaexvcdu"
        config["api_key"] = "269842741757611"
        config["api_secret"] = "mqC83IzTe8Ar4IZVeX4V9Po2nV8"
    }
}
