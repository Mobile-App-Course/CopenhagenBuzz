package dk.itu.moapd.copenhagenbuzz.ralc.nhca.View

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.DynamicColors
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.R
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.ActivityLoginBinding
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.ActivityMainBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Apply dynamic colors if running on Android 12+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            DynamicColors.applyToActivityIfAvailable(this)
        }

        // Inflate the layout for this activity
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)


        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // onClickListener for the login button
        // Creating a new intent and starting the MainActivity
        binding.materialButtonLogin.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("isLoggedIn", true)
            startActivity(intent)
            finish()
        }

        // onClickListener for the guest button
        // Creating a new intent and starting the MainActivity
        binding.materialButtonGuest.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("isLoggedIn", false)
            startActivity(intent)
            finish()
        }
    }
}