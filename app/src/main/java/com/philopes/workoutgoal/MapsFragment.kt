package com.philopes.workoutgoal

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
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
import android.graphics.Color
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.material.snackbar.Snackbar
import com.philopes.workoutgoal.data.models.Surrounding
import com.philopes.workoutgoal.helpers.Constants.CAMERA_ZOOM
import com.philopes.workoutgoal.helpers.Constants.GEOFENCE_RADIUS
import com.philopes.workoutgoal.helpers.Constants.MAPS_TAG
import com.philopes.workoutgoal.helpers.geofence.GeofenceReceiver
import java.lang.Exception
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.random.Random


class MapsFragment : Fragment() {

    companion object{
        fun showNotification(context: Context, title : String, message : String){
            val CHANNEL_ID = "GEOFENCE NOTIFICATION"
            var notificationID = Random(111).nextInt(1,30)

            val notificationBuilder = NotificationCompat.Builder(context,CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(CHANNEL_ID,"WorkoutGoal",NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Description"
            }

            notificationManager.createNotificationChannel(channel)

            notificationManager.notify(notificationID,notificationBuilder.build())
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
        permissions ->
        permissions.entries.forEach {
            val permissionName = it.key
            val isGranted = it.value
            if (!isGranted) {
                Log.e(MAPS_TAG,"Permission denied: $permissionName")
                mLocationPermissionGranted = false
                return@registerForActivityResult
            }
        }
        mLocationPermissionGranted = true
    }

    private val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.TYPES, Place.Field.ID)
    private lateinit var mPlacesClient : PlacesClient
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var geofencingClient: GeofencingClient
    private var mLastKnownLocation: Location? = null
    private val mDefaultLocation = LatLng(-33.8523341, 151.2106085)
    private var mLocationPermissionGranted = false

    private val surroundings : ArrayList<Surrounding> = arrayListOf()
    private val geofenceList : ArrayList<Geofence> = arrayListOf()

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.Q)
    private val onMapReady = OnMapReadyCallback {
        googleMap ->

        googleMap.uiSettings.isZoomControlsEnabled = true
        getLocationPermission()
        if(mLocationPermissionGranted){
            googleMap.isMyLocationEnabled = true
            getDeviceLocation(googleMap)
            addMarkers(googleMap)
        }

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
        mapFragment?.getMapAsync(onMapReady)

        Places.initialize(requireContext(), getString(R.string.google_maps_key));
        mPlacesClient = Places.createClient(requireContext());
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        geofencingClient = LocationServices.getGeofencingClient(requireActivity())
    }

    private fun getLocationPermission() {
        mLocationPermissionGranted = false
        if (ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
        } else {
            requestPermissions(this.requireView())
        }
    }

    private fun getCurrentPlaceLikelihoods(googleMap: GoogleMap) {

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
                        Log.i(
                            MAPS_TAG, String.format(
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
                        Log.e(MAPS_TAG, "Place not found: " + exception.statusCode)
                    }
                }
            }
        }else{
            Toast.makeText(requireContext(), "No permission granted",Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getDeviceLocation(googleMap : GoogleMap) {
        try {
            mFusedLocationProviderClient.lastLocation.addOnCompleteListener {
                task ->
                if (task.isSuccessful || task.result != null) {
                    mLastKnownLocation = task.result
                    Log.d(MAPS_TAG, "Latitude: " + mLastKnownLocation!!.latitude + " Longitude: " + mLastKnownLocation!!.longitude)
                    googleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                mLastKnownLocation!!.latitude,
                                mLastKnownLocation!!.longitude
                            ), CAMERA_ZOOM
                        )
                    )
                } else {
                    Log.d(MAPS_TAG, "Current location is null. Using defaults.")
                    Log.e(MAPS_TAG, "Exception: %s", task.exception)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(mDefaultLocation, CAMERA_ZOOM))
                }
                getCurrentPlaceLikelihoods(googleMap)
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

    @SuppressLint("MissingPermission")
    private fun addGeofence(surrounding: Surrounding, googleMap: GoogleMap){

        val geofence = Geofence.Builder()
            .setRequestId(surrounding.id!!)
            .setCircularRegion(surrounding.latLng!!.latitude, surrounding.latLng!!.longitude, GEOFENCE_RADIUS.toFloat())
            .setExpirationDuration(TimeUnit.HOURS.toMillis(1))
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
            .build()

        geofenceList.add(geofence)

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        val intent = Intent(requireContext(), GeofenceReceiver::class.java).putExtra("ID",surrounding.id!!)

        val pendingIntent = PendingIntent.getBroadcast(requireContext(),0,intent,PendingIntent.FLAG_UPDATE_CURRENT)

        geofencingClient.addGeofences(geofencingRequest, pendingIntent).run {
            addOnSuccessListener {
                Log.i("Add Geofence", geofence.requestId)
                googleMap.addCircle(
                    CircleOptions()
                        .center(surrounding.latLng!!)
                        .strokeColor(Color.argb(50,70,70,70))
                        .fillColor(Color.argb(70,150,150,150))
                        .radius(GEOFENCE_RADIUS.toDouble())
                )
            }
            addOnFailureListener { error ->
                if ((error.message != null)) {
                    Log.e(MAPS_TAG, error.message!!)
                }
            }
        }
    }

    private fun requestPermissions(view: View){

        val permissions : Array<String> = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_BACKGROUND_LOCATION)

        when{
            ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            -> {
                Snackbar.make(view,"Permission Granted",Snackbar.LENGTH_LONG).show()
                mLocationPermissionGranted = true
            }
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.ACCESS_FINE_LOCATION)  ||
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),Manifest.permission.ACCESS_BACKGROUND_LOCATION) -> {
                Snackbar.make(view,"Requires Permissions",Snackbar.LENGTH_INDEFINITE).setAction("OK"){
                    requestPermissionLauncher.launch(permissions)
                }.show()
            }
            else -> {
                requestPermissionLauncher.launch(permissions)
            }
        }
    }

}