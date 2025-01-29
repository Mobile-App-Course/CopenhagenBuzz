package dk.itu.moapd.copenhagenbuzz.ralc.nhca

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import dk.itu.moapd.copenhagenbuzz.ralc.nhca.databinding.ActivityMainBinding
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val editTextEventDate: TextInputEditText = findViewById(R.id.edit_text_event_date)
        // Set a OnClickListener to show the calendar when clicked
        editTextEventDate.setOnClickListener {
            showCalendar(editTextEventDate)
        }
    }

    // Function to show the calendar
    private fun showCalendar(editText: TextInputEditText) {
        // Values used to get the current date
        val calendar = Calendar.getInstance()
        val calYear = calendar.get(Calendar.YEAR)
        val calMonth = calendar.get(Calendar.MONTH)
        val calDay = calendar.get(Calendar.DAY_OF_MONTH)

        // Used to create and show the calendar
        val calendarPicker = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Formats the selected date to be displayed in the EditText
                val selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                editText.setText(selectedDate)
            },
            calYear, calMonth, calDay
        )
        calendarPicker.show()
    }
}