package com.example.booktalk

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.navOptions


class MainActivity : AppCompatActivity() {
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navController = findNavController(R.id.nav_host_fragment)
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)


        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.feedFragment -> {
                    navController.navigate(R.id.feedFragment, null, navOptionsWithClearStack())
                    true
                }
                R.id.createPostFragment -> {
                    navController.navigate(R.id.createPostFragment, null, navOptionsWithClearStack())
                    true
                }
                R.id.profileFragment -> {
                    navController.navigate(R.id.profileFragment, null, navOptionsWithClearStack())
                    true
                }
                else -> false
            }
        }



    }
    fun navOptionsWithClearStack() = androidx.navigation.navOptions {
        popUpTo(R.id.nav_graph) {
            inclusive = false
            saveState = false
        }
        launchSingleTop = true
        restoreState = false
    }
}

