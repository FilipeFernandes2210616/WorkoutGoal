package com.philopes.workoutgoal.helpers.database

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.philopes.workoutgoal.data.models.Record


class FirebaseDatabase() {

    companion object{

        private var database: DatabaseReference = Firebase.database.reference
        private lateinit var onListener : ValueEventListener

        fun setOnListener(onListener: ValueEventListener){
            this.onListener = onListener
        }

        fun registerChallenge(record : Record) : Boolean{
            if(record.exerciseId != null){
                return database.child(record.placeId).child(record.exerciseId!!).child(record.user!!.userID).setValue(record.user!!.displayName+"("+record.user.email+"): "+record.value).isSuccessful
            }
            return false
        }

        fun getRecords() {
            database.addValueEventListener(onListener)
        }
    }

}