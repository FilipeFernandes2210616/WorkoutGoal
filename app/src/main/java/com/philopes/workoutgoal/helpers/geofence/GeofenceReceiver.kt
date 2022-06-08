package com.philopes.workoutgoal.helpers.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.philopes.workoutgoal.MainActivity
import com.philopes.workoutgoal.MapsFragment
import java.lang.StringBuilder


class GeofenceReceiver : BroadcastReceiver(){
    lateinit var key : String
    lateinit var title : String
    lateinit var message : String

    override fun onReceive(context: Context?, intent: Intent?) {
       if(context != null && intent != null){
           val geofencingEvent = GeofencingEvent.fromIntent(intent)
           val geofenceTransition = geofencingEvent.geofenceTransition

           key = intent.getStringExtra("key").toString()

           if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){
               title = "Entered Workout Pin Location"
               message = "Try to break the Location's Workout Record"
               MainActivity.enableStartChallenge()
           }else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
                   title = "Exited Workout Pin Location"
                   message = "We showed them who is the boss!"
                   MainActivity.disableStartChallenge()
           }

           MapsFragment.showNotification(context.applicationContext,title,message)
       }
    }
}
