package com.philopes.workoutgoal

import CustomAdapter
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.philopes.workoutgoal.data.models.Record
import com.philopes.workoutgoal.helpers.Constants
import com.philopes.workoutgoal.helpers.UtilViewModel
import com.philopes.workoutgoal.helpers.database.FirebaseDatabase

class ScoreActivity : AppCompatActivity() {

    val utilModel: UtilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_score)

        // getting the recyclerview by its id
        val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)

        // this creates a vertical layout Manager
        recyclerview.layoutManager = LinearLayoutManager(this)
        val data = arrayListOf<String>()
        var adapter = CustomAdapter(data)
        recyclerview.adapter = adapter
        val record = intent.getSerializableExtra(Constants.RECORD) as Record

        FirebaseDatabase.setOnListener(object : ValueEventListener{

            override fun onDataChange(it : DataSnapshot) {
                val result = it.child(record.placeId).child(record.exerciseId!!)
                for(users : DataSnapshot in result.children){
                    data.add(users.key!!+": "+users.value as Long?)
                }
                adapter = CustomAdapter(data)
                recyclerview.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        }).also {
            FirebaseDatabase.getRecords()
        }

        // Setting the Adapter with the recyclerview
        val button= findViewById<Button>(R.id.button_startChallenge)

        button.setOnClickListener(){
            val intent = Intent(applicationContext,CameraActivity::class.java)
            intent.putExtra(Constants.RECORD,record)
            startActivity(intent)
        }
    }
}