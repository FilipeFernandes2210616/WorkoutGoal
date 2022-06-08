package com.philopes.workoutgoal.data.models

import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place

data class Surrounding(
    var name: String?,
    var addresses : String?,
    var attributions: String?,
    var latLng: LatLng?,
    var type : List<Place.Type>?,
    var id : String?
)