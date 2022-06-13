package com.philopes.workoutgoal
import CustomAdapter
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.philopes.workoutgoal.data.models.Record
import com.philopes.workoutgoal.helpers.Constants
import com.philopes.workoutgoal.helpers.UtilViewModel

class ExercisesActivity : AppCompatActivity() {

    val utilModel: UtilViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_exercises)

        // getting the recyclerview by its id
        val recyclerview = findViewById<RecyclerView>(R.id.recyclerview)

        // this creates a vertical layout Manager
        recyclerview.layoutManager = LinearLayoutManager(this)
        val data =  listOf(
            "Squats",
            "Push-Ups",
            "Burpees"
        )

        val adapter = CustomAdapter(data)

        // Setting the Adapter with the recyclerview
        recyclerview.adapter = adapter

        adapter.setOnClickListener(object: CustomAdapter.OnClickListener{
            override fun click(id: String) {
                val record = intent.getSerializableExtra(Constants.RECORD) as Record
                val intent = Intent(applicationContext,ScoreActivity::class.java)
                record.exerciseId = id
                intent.putExtra(Constants.RECORD,record)
                startActivity(intent)
            }
        } )

    }
}