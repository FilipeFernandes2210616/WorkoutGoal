package com.philopes.workoutgoal.helpers.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.philopes.workoutgoal.MapsFragment
import java.lang.StringBuilder


class GeofenceReceiver : BroadcastReceiver(){
    lateinit var key : String
    lateinit var message : String

    override fun onReceive(context: Context?, intent: Intent?) {
       if(context != null && intent != null){
           val geofencingEvent = GeofencingEvent.fromIntent(intent)
           val geofenceTransition = geofencingEvent.geofenceTransition

           key = intent.getStringExtra("key").toString()
           message = intent.getStringExtra("message").toString()

           if(geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER || geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT){
                MapsFragment.showNotification(context.applicationContext,message)
           }
       }
    }
}
