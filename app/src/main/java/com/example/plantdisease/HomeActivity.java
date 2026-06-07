package com.example.plantdisease;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private TextView welcomeText;
    private View plantDiseaseBtn, insectDetectionBtn;

    private String userName, userEmail, userCountry;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Get user info from WelcomeActivity
        Intent intent = getIntent();
        userName = intent.getStringExtra("USER_NAME");
        userEmail = intent.getStringExtra("USER_EMAIL");
        userCountry = intent.getStringExtra("USER_COUNTRY");

        // Find all views
        welcomeText = findViewById(R.id.welcomeText);
        plantDiseaseBtn = findViewById(R.id.plantDiseaseBtn);
        insectDetectionBtn = findViewById(R.id.insectDetectionBtn);
        View plantInfoBtn = findViewById(R.id.plantInfoBtn);
        View encyclopediaBtn = findViewById(R.id.encyclopediaBtn);
        View regionalGuideBtn = findViewById(R.id.regionalGuideBtn);
        View acreAgentBtn = findViewById(R.id.acreAgentBtn);
        View satelliteBtn = findViewById(R.id.satelliteBtn);

        // Display welcome message
        welcomeText.setText(
                "Welcome back, " + userName +
                        "!\nCountry: " + userCountry +
                        "\nEmail: " + userEmail
        );

        // Plant Disease button
        plantDiseaseBtn.setOnClickListener(v -> {
            Intent plantIntent = new Intent(HomeActivity.this, MainActivity.class);
            plantIntent.putExtra("USER_NAME", userName);
            plantIntent.putExtra("USER_EMAIL", userEmail);
            plantIntent.putExtra("USER_COUNTRY", userCountry);
            startActivity(plantIntent);
        });

        // Insect Detection button
        insectDetectionBtn.setOnClickListener(v -> {
            Intent insectIntent = new Intent(HomeActivity.this, InsectActivity.class);
            insectIntent.putExtra("USER_NAME", userName);
            insectIntent.putExtra("USER_EMAIL", userEmail);
            insectIntent.putExtra("USER_COUNTRY", userCountry);
            startActivity(insectIntent);
        });

        // Encyclopedia button
        encyclopediaBtn.setOnClickListener(v -> {
            Intent encyclopediaIntent = new Intent(HomeActivity.this, EncyclopediaActivity.class);
            startActivity(encyclopediaIntent);
        });

        // Regional Guide button
        regionalGuideBtn.setOnClickListener(v -> {
            Intent regionalIntent = new Intent(HomeActivity.this, RegionalGuideActivity.class);
            regionalIntent.putExtra("USER_COUNTRY", userCountry);
            startActivity(regionalIntent);
        });

        // Plant Info button
        plantInfoBtn.setOnClickListener(v -> {
            Intent plantInfoIntent = new Intent(HomeActivity.this, PlantInfoActivity.class);
            startActivity(plantInfoIntent);
        });

        acreAgentBtn.setOnClickListener(v -> {
            Intent agentIntent = new Intent(HomeActivity.this, AcreAgentActivity.class);
            startActivity(agentIntent);
        });

        // Agricultural Data (Satellite) button
        satelliteBtn.setOnClickListener(v -> {
            Intent satIntent = new Intent(HomeActivity.this, SatelliteActivity.class);
            satIntent.putExtra("USER_COUNTRY", userCountry);
            startActivity(satIntent);
        });
    }
}
