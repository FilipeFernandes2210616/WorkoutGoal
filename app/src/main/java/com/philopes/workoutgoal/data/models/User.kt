package com.philopes.workoutgoal.data.models

import java.io.Serializable

data class User(val userID: String = "", val email: String = "", val displayName: String = "") : Serializable