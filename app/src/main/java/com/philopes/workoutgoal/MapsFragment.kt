package com.philopes.workoutgoal

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class MapsFragment : Fragment() {

    // New variables for Current Place picker
    private val TAG = "MapsActivity"
    private lateinit var mPlacesClient : PlacesClient
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private var mLastKnownLocation: Location? = null

    // A default location (Sydney, Australia) and default zoom to use when location permission is
    // not granted.
    private val mDefaultLocation = LatLng(-33.8523341, 151.2106085)
    private val DEFAULT_ZOOM = 15
    private val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1
    private var mLocationPermissionGranted = false
    lateinit var geofencingClient: GeofencingClient

    companion object {
        internal const val ACTION_GEOFENCE_EVENT =
            "MapsFragment.action.ACTION_GEOFENCE_EVENT"
    }

    data class Surrounding(
        var name: String?,
        var addresses : String?,
        var attributions: String?,
        var latLng: LatLng?,
        var type : List<Place.Type>?,
        var id : String?
    )

    class BroadcastFenceReceiver : BroadcastReceiver(){

        private val TAG = "Broadcast"
        private val NOTIFICATION_ID = 33
        private val CHANNEL_ID = "GeofenceChannel"

        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent != null) {
                if (intent.action == ACTION_GEOFENCE_EVENT) {
                    val geofencingEvent = GeofencingEvent.fromIntent(intent)

                    if (geofencingEvent.hasError()) {
                        val errorMessage = errorMessage(context!!, geofencingEvent.errorCode)
                        Log.e(TAG, errorMessage)
                        return
                    }

                    if (geofencingEvent.geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
                        Log.v(TAG, "ENTERED FENCE")
                        val fenceId = when {
                            geofencingEvent.triggeringGeofences.isNotEmpty() ->
                                geofencingEvent.triggeringGeofences[0].requestId
                            else -> {
                                Log.e(TAG, "No Geofence Trigger Found! Abort mission!")
                                return
                            }
                        }
                        val notificationManager = ContextCompat.getSystemService(
                            context!!,
                            NotificationManager::class.java
                        ) as NotificationManager

                        notificationManager.sendGeofenceEnteredNotification(context, fenceId)
                    }
                }
            }
        }

        fun NotificationManager.sendGeofenceEnteredNotification(context: Context,id : String ) {
            val contentIntent = Intent(context, MainActivity::class.java)
            contentIntent.putExtra("PLACE ID", id)
            val contentPendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                contentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
            )
            val mapImage = BitmapFactory.decodeResource(
                context.resources,
                R.drawable.ic_launcher_foreground
            )
            val bigPicStyle = NotificationCompat.BigPictureStyle()
                .bigPicture(mapImage)
                .bigLargeIcon(null)

            // We use the name resource ID from the LANDMARK_DATA along with content_text to create
            // a custom message when a Geofence triggers.
            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("Fence entered: $id")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentPendingIntent)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setStyle(bigPicStyle)
                .setLargeIcon(mapImage)

            notify(NOTIFICATION_ID, builder.build())
        }

        private fun errorMessage(context: Context, errorCode: Int): String {
            val resources = context.resources
            return when (errorCode) {
                GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> "NOT AVAILABLE"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_GEOFENCES -> "TOO MANY"
                GeofenceStatusCodes.GEOFENCE_TOO_MANY_PENDING_INTENTS -> "TOO MANY PENDING INTENTS"
                else -> "UNKNOWN"
            }
        }
    }

    private val surroundings : ArrayList<Surrounding> = arrayListOf()
    private val geofenceList : ArrayList<Geofence> = arrayListOf()

    @RequiresApi(Build.VERSION_CODES.Q)
    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */

        googleMap.uiSettings.isZoomControlsEnabled = true;
        getLocationPermission();
        getDeviceLocation(googleMap)
        addMarkers(googleMap)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        Places.initialize(requireContext(), getString(R.string.google_maps_key));
        mPlacesClient = Places.createClient(requireContext());
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        mLocationPermissionGranted = false
        if (ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            &&
            ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        mLocationPermissionGranted = false
        when (requestCode) {
            PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true
                }
            }
        }
    }

    private fun getCurrentPlaceLikelihoods(googleMap: GoogleMap) {
        // Use fields to define the data types to return.
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.TYPES, Place.Field.ID)

        // Get the likely places - that is, the businesses and other points of interest that
        // are the best match for the device's current location.
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val request = FindCurrentPlaceRequest.builder(placeFields).build()
            val placeResponse: Task<FindCurrentPlaceResponse> = mPlacesClient.findCurrentPlace(request)
            placeResponse.addOnCompleteListener(requireActivity()) {
                task ->
                if (task.isSuccessful) {
                    val response = task.result

                    for(placeLikelihood : PlaceLikelihood in response.placeLikelihoods){
                        val currPlaceAttributes = if(placeLikelihood.place.attributions == null) null else TextUtils.join("",placeLikelihood.place.attributions)
                        val surrounding = Surrounding(
                            placeLikelihood.place.name,
                            placeLikelihood.place.address,
                            currPlaceAttributes,
                            placeLikelihood.place.latLng,
                            placeLikelihood.place.types,
                            placeLikelihood.place.id
                        )
                        surroundings.add(surrounding)
                        val currLatLng = surroundings.last().latLng ?: ""
                        Log.i(TAG, String.format(
                            "Place " + placeLikelihood.place.name +
                            " types: " + placeLikelihood.place.types +
                            " id: " + placeLikelihood.place.id +
                            " has likelihood: " + placeLikelihood.likelihood + " at " + currLatLng.toString()))
                    }

                    if(surroundings.isNotEmpty())
                        addMarkers(googleMap)
                    else
                        Toast.makeText(requireContext(), "Empty surroundings",Toast.LENGTH_LONG).show()

                } else {
                    val exception: Exception? = task.exception
                    if (exception is ApiException) {
                        Log.e(TAG, "Place not found: " + exception.statusCode)
                    }
                }
            }
        }else{
            Toast.makeText(requireContext(), "No permission granted",Toast.LENGTH_LONG).show()
        }
    }

    private fun getDeviceLocation(googleMap : GoogleMap) {
        try {
            if (mLocationPermissionGranted) {
                val locationResult: Task<Location> = mFusedLocationProviderClient.lastLocation
                locationResult.addOnCompleteListener(requireActivity()) {
                    task ->
                        if (task.isSuccessful) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.result
                            Log.d(TAG, "Latitude: " + mLastKnownLocation!!.latitude)
                            Log.d(TAG, "Longitude: " + mLastKnownLocation!!.longitude)
                            googleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        mLastKnownLocation!!.latitude,
                                        mLastKnownLocation!!.longitude
                                    ), DEFAULT_ZOOM.toFloat()
                                )
                            )
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.")
                            Log.e(TAG, "Exception: %s", task.exception)
                            googleMap.moveCamera(
                                CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM.toFloat())
                            )
                        }
                        getCurrentPlaceLikelihoods(googleMap)
                    }
            }
        } catch (e: SecurityException) {
            e.message?.let { Log.e("Exception: %s", it) }
        }
    }

    private fun addMarkers(googleMap: GoogleMap){
        surroundings.forEach {
            googleMap.addMarker(MarkerOptions().position(it.latLng!!).title(it.name))
            addGeofence(it,googleMap)
        }
    }

    private fun addGeofence(surrounding: Surrounding, googleMap: GoogleMap){

        surrounding.id?.let {
            val geofence = Geofence.Builder()
                .setRequestId(it)
                .setCircularRegion(
                    surrounding.latLng!!.latitude,
                    surrounding.latLng!!.longitude,
                    5.0f
                )
                .setExpirationDuration(TimeUnit.HOURS.toMillis(1))
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()

            geofenceList.add(
                Geofence.Builder()
                    .setRequestId(it)
                    .setCircularRegion(
                        surrounding.latLng!!.latitude,
                        surrounding.latLng!!.longitude,
                        5.0f
                    )
                    .setExpirationDuration(TimeUnit.HOURS.toMillis(1))
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            )

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()


            if (ActivityCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent)?.run {
                    addOnSuccessListener {
                        Toast.makeText(requireContext(), "Added Fence",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        Log.i("Add Geofence", geofence.requestId)
                    }
                    addOnFailureListener {
                        error ->
                        Toast.makeText(
                            requireContext(), "Failed add geofence",
                            Toast.LENGTH_SHORT
                        ).show()
                        if ((error.message != null)) {
                            Log.e(TAG, error.message!!)
                        }
                    }
                }
            }

        }
    }

    private fun getGeofencingRequest(): GeofencingRequest {
        return GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()
    }

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(requireContext(), BroadcastFenceReceiver::class.java)
        intent.action = ACTION_GEOFENCE_EVENT
        PendingIntent.getBroadcast(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
    }



}