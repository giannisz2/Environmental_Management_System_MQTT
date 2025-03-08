package com.project_networks.project_android_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ManualMode extends AppCompatActivity {

    Integer seconds = 0;

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



        Button backButton = findViewById(R.id.buttonBack);
        backButton.setOnClickListener(v -> {
            Intent backToMain = new Intent(ManualMode.this, MainActivity.class);
            backToMain.putExtra("SECONDS", seconds);
            startActivity(backToMain);
        });

        Button buttonSet = findViewById(R.id.buttonSet);
        buttonSet.setOnClickListener(v -> {
            EditText setSeconds = findViewById(R.id.editTextSeconds);
            seconds = Integer.parseInt(setSeconds.getText().toString());
            Toast.makeText(ManualMode.this, "Sending time set to: " + seconds + " seconds", Toast.LENGTH_LONG).show();

        });
    }

}