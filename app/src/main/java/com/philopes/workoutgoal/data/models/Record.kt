package com.philopes.workoutgoal.data.models

import java.io.Serializable

class Record(val placeId : String, val user: User?, var exerciseId : String? , var value: Long?) : Serializable {
}