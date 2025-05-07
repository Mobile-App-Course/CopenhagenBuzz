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

/**
 * Activity for handling user login and authentication.
 *
 * This activity provides functionality for both anonymous and standard user login
 * using Firebase Authentication. It supports multiple authentication providers
 * (Email, Google, and Anonymous) and redirects the user to the main activity upon
 * successful login.
 */
class LoginActivity : AppCompatActivity() {

    private val signInLauncher = registerForActivityResult(FirebaseAuthUIActivityResultContract()) { result -> onSignInResult(result) }

    /**
     * Called when the activity is created.
     *
     * This method initializes the activity, loads environment variables, and determines
     * whether to initiate an anonymous login or display the sign-in UI.
     *
     * @param savedInstanceState The saved instance state of the activity.
     */
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

    /**
     * Initiates an anonymous sign-in process.
     *
     * This method uses Firebase Authentication to log in the user anonymously.
     * If the login is successful, the user is redirected to the main activity.
     * Otherwise, an error message is displayed.
     */
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

    /**
     * Creates and launches the Firebase sign-in intent.
     *
     * This method configures the Firebase Authentication UI with available providers
     * (Email, Google, and Anonymous) and launches the sign-in activity.
     */
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

    /**
     * Handles the result of the Firebase authentication process.
     *
     * This method is called when the Firebase authentication UI returns a result.
     * It checks the result code and redirects the user to the main activity if the
     * login is successful. Otherwise, it displays an error message.
     *
     * @param result The result of the Firebase authentication process.
     */
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

    /**
     * Starts the main activity.
     *
     * This method redirects the user to the main activity and passes a flag
     * indicating whether the user is logged in anonymously.
     *
     * @param anonymous A flag indicating whether the user is logged in anonymously.
     */
    private fun startMainActivity(anonymous: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java)

        intent.putExtra("isLoggedIn", !anonymous)
        startActivity(intent)
        finish()
    }

    /**
     * Displays a snackbar with the provided message.
     *
     * @param message The message to display in the snackbar.
     */
    private fun showSnackBar(message: String) {
        Snackbar.make(findViewById(android.R.id.content), message, Snackbar.LENGTH_SHORT).show()
    }
}