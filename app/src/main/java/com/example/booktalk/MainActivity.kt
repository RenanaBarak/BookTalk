package com.example.booktalk

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.NavOptions
import androidx.navigation.navOptions
import androidx.navigation.findNavController

class MainActivity : AppCompatActivity() {

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)


        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment -> bottomNav.visibility = View.GONE
                else -> bottomNav.visibility = View.VISIBLE
            }
        }


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

    private fun navOptionsWithClearStack(): NavOptions {
        return navOptions {
            popUpTo(R.id.nav_graph) {
                inclusive = false
                saveState = false
            }
            launchSingleTop = true
            restoreState = false
        }
    }
}
