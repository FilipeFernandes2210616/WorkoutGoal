package com.philopes.workoutgoal.helpers.database

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.philopes.workoutgoal.data.models.Record


class FirebaseDatabase() {

    companion object{

        private var database: DatabaseReference = Firebase.database.reference

        fun registerChallenge(record : Record) : Boolean{
            return database.child(record.placeId).child(record.exerciseId).child(record.userId).setValue(record.value).isSuccessful
        }

        fun getRecords(placeId : String,exerciseId : String): ArrayList<Record>{

            val records = arrayListOf<Record>()

            database.child(placeId).child(exerciseId).get().addOnSuccessListener {
                for(users : DataSnapshot in it.children){
                    records.add(Record(placeId, users.key!!,exerciseId, users.value as Int))
                }
            }

            return records
        }
    }

}