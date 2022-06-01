package com.philopes.workoutgoal

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig
import com.firebase.ui.auth.AuthUI.IdpConfig.*
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.FirebaseAuth
import java.lang.Error
import java.util.*


class LoginFirebaseUIActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_firebase_uiactivity)

        // Choose authentication providers
        // Choose authentication providers
        val providers: List<IdpConfig> = Arrays.asList(
            EmailBuilder().build(),
            GoogleBuilder().build(),
        )
        // Create and launch sign-in intent
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .build()
        signInLauncher.launch(signInIntent)
    }

    // See: https://developer.android.com/training/basics/intents/result
    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            if(user != null){
                Toast.makeText(applicationContext,"Login: "+user.displayName +user.email, Toast.LENGTH_SHORT)
                val intent = Intent(this, MainActivity::class.java).apply {
                    //putExtra(EXTRA_MESSAGE, user)
                }
                startActivity(intent);
            }
        } else {
            println("Login Failed")
            Toast.makeText(applicationContext,"Login Failed", Toast.LENGTH_SHORT)
        }
    }

}