package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import android.content.Intent
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.database.database
import io.github.cdimascio.dotenv.dotenv

class LoginActivity : AppCompatActivity() {

    private val signInLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result -> onSignInResult(result) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Load environment variables
        val dotenv = dotenv {
            directory = "./assets"
            filename = "env"
        }



        val isAnonymousLogin = intent.getBooleanExtra("isAnonymousLogin", false)
        if (isAnonymousLogin) {
            createAnonymousSignInIntent()
        } else {
            createSignInIntent()
        }
    }

    private fun createAnonymousSignInIntent() {
        FirebaseAuth.getInstance().signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showSnackBar("User logged in anonymously.")
                    startMainActivity(true)
                } else {
                    showSnackBar("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun createSignInIntent() {
        // Choose authentication providers.
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build(),
            AuthUI.IdpConfig.GoogleBuilder().build(),
            AuthUI.IdpConfig.AnonymousBuilder().build())

        // Create and launch sign-in intent.
        val signInIntent = AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAvailableProviders(providers)
            .setIsSmartLockEnabled(false)
            .setLogo(R.drawable.baseline_firebase)
            .setTheme(R.style.Theme_FirebaseAuthentication)
            .apply {
                setTosAndPrivacyPolicyUrls(
                    "https://firebase.google.com/terms/",
                    "https://firebase.google.com/policies/…"
                )
            }
            .build()

        signInLauncher.launch(signInIntent)
    }

    private fun onSignInResult(
        result: FirebaseAuthUIAuthenticationResult
    ) {
        when (result.resultCode) {
            RESULT_OK -> {
                // Successfully signed in.
                val user = FirebaseAuth.getInstance().currentUser
                if (user != null && user.isAnonymous) {
                    showSnackBar("User logged in anonymously.")
                    startMainActivity(true)
                } else {
                    showSnackBar("User logged in the app.")
                    startMainActivity()
                }
            }
            else -> {
                    showSnackBar("Authentication failed")

            }
        }
    }
    private fun startMainActivity(anonymous: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java)

        intent.putExtra("isLoggedIn", !anonymous)
        startActivity(intent)
        finish()
    }

    private fun showSnackBar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
}