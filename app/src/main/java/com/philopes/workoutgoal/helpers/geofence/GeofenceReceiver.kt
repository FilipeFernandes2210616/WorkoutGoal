package com.philopes.workoutgoal.helpers.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.philopes.workoutgoal.MainActivity
import com.philopes.workoutgoal.MapsFragment
import com.philopes.workoutgoal.helpers.Constants.PLACE_ID
import java.lang.StringBuilder


class GeofenceReceiver : BroadcastReceiver(){
    lateinit var placeId : String
    lateinit var title : String
    lateinit var message : String

    override fun onReceive(context: Context?, intent: Intent?) {
       if(context != null && intent != null){
           val geofencingEvent = GeofencingEvent.fromIntent(intent)
           val geofenceTransition = geofencingEvent.geofenceTransition

           placeId = intent.getStringExtra(PLACE_ID).toString()

           if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER){
               title = "Entered Workout Pin Location"
               message = "Try to break the Location's Workout Record"
               MainActivity.enableStartChallenge(placeId)
           }else if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
                   title = "Exited Workout Pin Location"
                   message = "We showed them who is the boss!"
                   MainActivity.disableStartChallenge()
           }

           MapsFragment.showNotification(context.applicationContext,title,message)
       }
    }
}
