package com.philopes.workoutgoal

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.philopes.workoutgoal.data.models.Record
import com.philopes.workoutgoal.data.models.User
import com.philopes.workoutgoal.databinding.ActivityMainBinding
import com.philopes.workoutgoal.helpers.Constants
import com.philopes.workoutgoal.helpers.UtilViewModel

class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener{

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController : NavController
    val utilModel: UtilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navController = this.findNavController(R.id.myNavHostFragment)
        NavigationUI.setupWithNavController(binding.bottomNavView,navController)

        binding.bottomNavView.setOnNavigationItemSelectedListener(this)

        binding.btnStartChallenge.setOnClickListener {
            if(onStartChallenge){
                if(!placeId.isNullOrEmpty()){
                    Toast.makeText(applicationContext, "PlaceID: $placeId", Toast.LENGTH_SHORT).show()
                    val user = intent.getSerializableExtra(Constants.USER) as User
                    val record = Record(placeId,user,null,null)
                    val bundle = bundleOf(Constants.RECORD to record)
                    navController.navigate(R.id.exercisesActivity,bundle)
                }
            }else{
                Toast.makeText(applicationContext, "Walk to a pin location", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        when(p0.itemId){
            R.id.mapsFragment -> {
                navController.navigate(R.id.mapsFragment)
                return true
            }
            R.id.profileFragment -> {
                navController.navigate(R.id.profileFragment)
                return true
            }
            R.id.rankingFragment -> {
                navController.navigate(R.id.rankingFragment)
                return true
            }
        }
        return false
    }

    companion object{

        var onStartChallenge = false
        var placeId = ""

        fun enableStartChallenge(placeId : String){
            onStartChallenge = true
            this.placeId = placeId
        }

        fun disableStartChallenge(){
            onStartChallenge = false
            placeId = ""
        }
    }

}