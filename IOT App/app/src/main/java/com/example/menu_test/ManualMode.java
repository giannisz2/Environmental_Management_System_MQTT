package com.example.menu_test;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ManualMode extends AppCompatActivity {
    Double manualLatitude = 0.0;
    Double manualLongitude = 0.0;


    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_manual_mode_screen);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Find EditText fields for latitude and longitude
        EditText latitudeInput = findViewById(R.id.editTextLatitude);
        EditText longitudeInput = findViewById(R.id.editTextLongitude);

        // Back Button
        Button backButton = findViewById(R.id.buttonBack);
        backButton.setOnClickListener(v -> {
            Intent backToMain = new Intent(ManualMode.this, MainActivity.class);
            startActivity(backToMain);
        });

        // Set Button
        Button buttonSet = findViewById(R.id.buttonSet);
        buttonSet.setOnClickListener(v -> {
            try {
                // Get user input
                String latText = latitudeInput.getText().toString().trim();
                String lonText = longitudeInput.getText().toString().trim();

                // Validate input
                if (lonText.isEmpty() || latText.isEmpty()) {
                    Toast.makeText(ManualMode.this, "Please enter both latitude and longitude.", Toast.LENGTH_LONG).show();
                    return;
                }

                manualLatitude = Double.parseDouble(latText);
                manualLongitude = Double.parseDouble(lonText);


                if (manualLatitude < -90 || manualLatitude > 90) {
                    Toast.makeText(ManualMode.this, "Latitude must be between -90 and 90", Toast.LENGTH_LONG).show();
                    return;
                }
                if (manualLongitude < -180 || manualLongitude > 180) {
                    Toast.makeText(ManualMode.this, "Longitude must be between -180 and 180", Toast.LENGTH_LONG).show();
                    return;
                }

                // Send values back to MainActivity
                Intent backToMain = new Intent(ManualMode.this, MainActivity.class);
                backToMain.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                startActivity(backToMain);
                finish(); // Finish ManualMode activity

                Toast.makeText(ManualMode.this, "Coordinates set: " + manualLatitude + ", " + manualLongitude, Toast.LENGTH_LONG).show();
            } catch (NumberFormatException e) {
                Toast.makeText(ManualMode.this, "Invalid input. Please enter numeric values.", Toast.LENGTH_LONG).show();
            }
        });
    }
}