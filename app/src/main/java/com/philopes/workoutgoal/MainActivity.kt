package com.philopes.workoutgoal

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.philopes.workoutgoal.databinding.ActivityMainBinding
import okhttp3.Challenge
import kotlin.random.Random

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener{

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController : NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = this.findNavController(R.id.myNavHostFragment)
        NavigationUI.setupWithNavController(binding.bottomNavView,navController)

        binding.bottomNavView.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when(p0.itemId){
            R.id.start_challenge -> {
                if(onStartChallenge){
                    val intent = Intent(this, CameraActivity::class.java)
                    startActivity(intent);
                }else{
                    Toast.makeText(applicationContext, "Walk to a pin location", Toast.LENGTH_SHORT).show()
                }
                return true
            }
            R.id.profile -> {
                return true
            }
            R.id.ranking -> {
                return true
            }
        }
        return false
    }

    companion object{

        var onStartChallenge = false

        fun enableStartChallenge(){
            onStartChallenge = true
        }

        fun disableStartChallenge(){
            onStartChallenge = false
        }
    }

}