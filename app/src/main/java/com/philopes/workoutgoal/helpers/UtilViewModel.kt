package com.philopes.workoutgoal.helpers

import androidx.lifecycle.ViewModel
import com.philopes.workoutgoal.data.models.User

class UtilViewModel : ViewModel() {

    var reps: Int? = null
    var user: User? = null
    var placeId: String? = null
    var exerciseId: String? = null

}