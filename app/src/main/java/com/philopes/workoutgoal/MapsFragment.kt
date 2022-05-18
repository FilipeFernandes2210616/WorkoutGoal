package com.philopes.workoutgoal

import android.Manifest
import android.location.Location
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.PlacesClient
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ApiException
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import com.google.android.gms.maps.GoogleMap
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.PlaceLikelihood
import com.google.android.libraries.places.api.net.FindCurrentPlaceResponse
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import java.lang.Exception
import java.util.*
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

    data class Surroundings(
        var mLikelyPlaceNames: ArrayList<String?> = arrayListOf(),
        var mLikelyPlaceAddresses: ArrayList<String?> = arrayListOf(),
        var mLikelyPlaceAttributions: ArrayList<String?> = arrayListOf(),
        var mLikelyPlaceLatLngs: ArrayList<LatLng?> = arrayListOf(),
    )

    private val surroundings : Surroundings = Surroundings()

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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        Places.initialize(requireContext(), getString(R.string.google_maps_key));
        mPlacesClient = Places.createClient(requireContext());
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

    }

    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        mLocationPermissionGranted = false
        if (ContextCompat.checkSelfPermission(this.requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true
        } else {
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION)
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
        val placeFields: List<Place.Field> = listOf(Place.Field.NAME, Place.Field.ADDRESS, Place.Field.LAT_LNG, Place.Field.TYPES)

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
                        surroundings.mLikelyPlaceNames.add(placeLikelihood.place.name)
                        surroundings.mLikelyPlaceAddresses.add(placeLikelihood.place.address)
                        val currPlaceAttributes = if(placeLikelihood.place.attributions == null) null else TextUtils.join("",placeLikelihood.place.attributions)
                        surroundings.mLikelyPlaceAttributions.add(currPlaceAttributes)
                        surroundings.mLikelyPlaceLatLngs.add(placeLikelihood.place.latLng)
                        val currLatLng = surroundings.mLikelyPlaceAddresses.last() ?: ""
                        Log.i(TAG, String.format("Place " + placeLikelihood.place.name + " has likelihood: " + placeLikelihood.likelihood + " at " + currLatLng.toString()))
                    }

                    if(surroundings.mLikelyPlaceNames.isNotEmpty())
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
        surroundings.mLikelyPlaceNames.forEachIndexed {
            index, name ->
            surroundings.mLikelyPlaceLatLngs[index]?.let {
                googleMap.addMarker(MarkerOptions().position(it).title(name))
            }

        }
    }


}