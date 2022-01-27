package com.example.where2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.ui.AppBarConfiguration
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var thirdPartyLogin: Button
    private lateinit var loginButton: Button
    private lateinit var signupButton: Button

    private lateinit var emailEdit: EditText
    private lateinit var passwordEdit: EditText

//    private lateinit var appBarConfiguration: AppBarConfiguration

    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        thirdPartyLogin = findViewById(R.id.acc_login_btn)
        thirdPartyLogin.setOnClickListener(listener)

        loginButton = findViewById(R.id.loginButton)
        loginButton.setOnClickListener(listener)

        signupButton = findViewById(R.id.signupButton)
        signupButton.setOnClickListener(listener)

        emailEdit = findViewById(R.id.emailArea)
        passwordEdit = findViewById(R.id.passwordArea)

        auth = Firebase.auth
    }

    private val signInLauncher = registerForActivityResult(
        FirebaseAuthUIActivityResultContract()
    ) { res ->
        this.onSignInResult(res)
    }

    // Choose authentication providers
    private val providers = arrayListOf(
        AuthUI.IdpConfig.GoogleBuilder().build(),
        AuthUI.IdpConfig.TwitterBuilder().build(),
    )

    // Create and launch sign-in intent
    val signInIntent = AuthUI.getInstance()
        .createSignInIntentBuilder()
        .setAvailableProviders(providers)
        .setLogo(R.drawable.clipart2688616)
        .build()

    private fun onSignInResult(result: FirebaseAuthUIAuthenticationResult) {
        Log.d("BIP BOP", "signin start")

        val response = result.idpResponse
        if (result.resultCode == RESULT_OK) {
            // Successfully signed in
            val user = FirebaseAuth.getInstance().currentUser
            Log.d("BIP BOP", "AUTH SUCCESSFUL")
            Toast.makeText(this, "LOGIN SUCCESSFUL", Toast.LENGTH_SHORT).show()

            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
        } else {
            Log.d("DEAD", "AUTH SUCCESSFUL")
            Toast.makeText(this, "LOGIN UNSUCCESSFUL", Toast.LENGTH_SHORT).show()
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if(currentUser != null){
            Log.d("USER", "user logged in")
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun reloadUI() {
        val currentUser = auth.currentUser
        if(currentUser != null){
            Log.d("USER", "user logged in")
            val intent = Intent(this, WelcomeActivity::class.java)
            startActivity(intent)
        }
    }

    private fun accLogin() {
        signInLauncher.launch(signInIntent)
    }

  

    private val listener = View.OnClickListener { view ->
        when (view.id){
            R.id.acc_login_btn -> {
                signInLauncher.launch(signInIntent)
            }

            R.id.loginButton -> {
                val email = emailEdit.text.toString()
                val password = passwordEdit.text.toString()

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("SUCCESS", "siginUserWithEmail:success")
                            val user = auth.currentUser
                            updateUI(user)
                            reloadUI()
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("FAILURE", "createUserWithEmail:failure", task.exception)
                            Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                            updateUI(null)
                        }
                    }
            }

            R.id.signupButton -> {
                val email = emailEdit.text.toString()
                val password = passwordEdit.text.toString()

                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d("SUCCESS", "createUserWithEmail:success")
                            val user = auth.currentUser
                            updateUI(user)
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w("FAILURE", "createUserWithEmail:failure", task.exception)
                            Toast.makeText(baseContext, "Authentication failed.",
                                Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if(user == null){
            return
        } else {
            return
        }
    }
}