package com.example.plantdisease;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SatelliteActivity extends AppCompatActivity {

    private static final int LOCATION_REQUEST_CODE = 1001;

    private TextView regionText, vegetationHealthText, moistureText, evapText;
    private EditText latInput, lonInput;
    private Button useCurrentLocationBtn, fetchDataBtn, openAcreAgentButton;

    private FusedLocationProviderClient fusedLocationClient;
    private final OkHttpClient httpClient = new OkHttpClient();
    private double latestVegetationHealth = Double.NaN;
    private double latestSoilMoisture = Double.NaN;
    private double latestEvapotranspiration = Double.NaN;
    private String latestRegion = "Unknown";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_satellite);

        // Link XML views to Java variables
        regionText = findViewById(R.id.regionText);
        vegetationHealthText = findViewById(R.id.vegetationHealthText);
        moistureText = findViewById(R.id.moistureText);
        evapText = findViewById(R.id.evapText);
        latInput = findViewById(R.id.latInput);
        lonInput = findViewById(R.id.lonInput);
        useCurrentLocationBtn = findViewById(R.id.useCurrentLocationBtn);
        fetchDataBtn = findViewById(R.id.fetchDataBtn);
        openAcreAgentButton = findViewById(R.id.openAcreAgentButton);

        // Initialize location service
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Button to use current device location
        useCurrentLocationBtn.setOnClickListener(v -> requestLocationPermission());
        openAcreAgentButton.setOnClickListener(v ->
                startActivity(new Intent(this, AcreAgentActivity.class))
        );

        // Button to fetch data for manually entered coordinates
        fetchDataBtn.setOnClickListener(v -> {
            String latStr = latInput.getText().toString().trim();
            String lonStr = lonInput.getText().toString().trim();

            // Validate input
            if (latStr.isEmpty() || lonStr.isEmpty()) {
                Toast.makeText(this, "Please enter latitude and longitude", Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                double lat = Double.parseDouble(latStr);
                double lon = Double.parseDouble(lonStr);

                // Check if coordinates are valid
                if (lat < -90 || lat > 90 || lon < -180 || lon > 180) {
                    Toast.makeText(this, "Invalid coordinates range", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Update region display
                latestRegion = String.format(Locale.US, "%.4f, %.4f", lat, lon);
                regionText.setText("Region: " + latestRegion);

                // Fetch data for this location
                fetchAllData(lat, lon);

            } catch (NumberFormatException e) {
                Toast.makeText(this, "Invalid number format", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ---------------- PERMISSIONS ----------------

    // Check if we have location permission, if not request it
    private void requestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            // Ask user for permission
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST_CODE
            );
            return;
        }

        // If we already have permission, get location
        fetchCurrentLocation();
    }

    // Get device's current location
    private void fetchCurrentLocation() {

        // Double-check permissions before accessing location
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST_CODE
            );
            return;
        }

        // Get current location
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnSuccessListener(location -> {
            if (location != null) {
                double lat = location.getLatitude();
                double lon = location.getLongitude();

                // Display location on screen and in input fields
                latestRegion = String.format(Locale.US, "%.4f, %.4f", lat, lon);
                regionText.setText("Region: " + latestRegion);
                latInput.setText(String.format("%.4f", lat));
                lonInput.setText(String.format("%.4f", lon));

                // Fetch all data
                fetchAllData(lat, lon);

            } else {
                Toast.makeText(this, "Unable to get location. Try entering manually.", Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(e -> {
            Log.e("SATELLITE_ACTIVITY", "Failed to get location", e);
            Toast.makeText(this, "Failed to get location. Please ensure location is enabled on your device.", Toast.LENGTH_LONG).show();
        });
    }

    // Fetch all data from APIs for given coordinates
    private void fetchAllData(double lat, double lon) {
        fetchOpenMeteoData(lat, lon);
        fetchNasaPowerData(lat, lon);
    }

    // ---------------- OPEN-METEO (SOIL + EVAPO) ----------------

    // Request soil moisture and evapotranspiration data from Open-Meteo API
    private void fetchOpenMeteoData(double lat, double lon) {
        String url = "https://api.open-meteo.com/v1/forecast?latitude=" + lat +
                "&longitude=" + lon +
                "&hourly=soil_moisture_0_1cm,evapotranspiration";

        Request request = new Request.Builder().url(url).build();

        // Make network request in background thread
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // Show error if network request fails
                runOnUiThread(() -> {
                    moistureText.setText("Soil Moisture: Network error");
                    evapText.setText("Evapotranspiration: Network error");
                });
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // Parse the JSON response
                String json = response.body().string();
                parseOpenMeteoJson(json);
            }
        });
    }

    // Extract soil moisture data from JSON response
    private void parseOpenMeteoJson(String json) {
        try {
            JSONObject obj = new JSONObject(json);
            JSONObject hourly = obj.getJSONObject("hourly");

            // Get arrays of data values
            JSONArray moisture = hourly.getJSONArray("soil_moisture_0_1cm");
            JSONArray evap = hourly.getJSONArray("evapotranspiration");

            // Get first value from each array (current hour)
            double topSoil = moisture.getDouble(0);
            double evapRate = evap.getDouble(0);
            latestSoilMoisture = topSoil;
            latestEvapotranspiration = evapRate;

            // Update UI on main thread
            runOnUiThread(() -> {
                moistureText.setText("Soil Moisture: " + String.format("%.3f", topSoil) + " m³/m³");
                evapText.setText("Evapotranspiration: " + String.format("%.2f", evapRate) + " mm/day");
                publishSatelliteStateIfReady();
            });

        } catch (Exception e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                moistureText.setText("Soil Moisture: Parse error");
                evapText.setText("Evapotranspiration: Parse error");
            });
        }
    }

    // ---------------- NASA POWER (VEGETATION HEALTH) ----------------

    // Request vegetation health data from NASA POWER API
    private void fetchNasaPowerData(double lat, double lon) {
        // Get date range: last 7 days for recent data
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
        String endDate = dateFormat.format(cal.getTime());

        cal.add(Calendar.DAY_OF_YEAR, -7);
        String startDate = dateFormat.format(cal.getTime());

        // NASA POWER API URL - using GWETTOP (vegetation water content)
        String url = "https://power.larc.nasa.gov/api/temporal/daily/point?" +
                "parameters=GWETTOP,PRECTOTCORR&" +
                "community=AG&" +
                "longitude=" + lon + "&" +
                "latitude=" + lat + "&" +
                "start=" + startDate + "&" +
                "end=" + endDate + "&" +
                "format=JSON";

        Log.d("NASA_URL", url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        // Make network request
        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("NASA_ERROR", "Failed to fetch", e);
                runOnUiThread(() -> vegetationHealthText.setText("Vegetation Health: Network error"));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String json = response.body().string();
                Log.d("NASA_JSON", json);
                parseNasaPowerData(json);
            }
        });
    }

    // Calculate vegetation health from NASA data
    private void parseNasaPowerData(String json) {
        try {
            JSONObject obj = new JSONObject(json);

            // Navigate to the data section
            JSONObject parameters = obj.getJSONObject("properties").getJSONObject("parameter");

            // Get GWETTOP (soil wetness - indicator of vegetation health)
            JSONObject gwettop = parameters.getJSONObject("GWETTOP");

            // Get the most recent value
            double totalValue = 0;
            int count = 0;

            // Iterate through all dates and average the values
            Iterator<String> keys = gwettop.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                double value = gwettop.getDouble(key);
                if (value > -999) { // NASA uses -999 for missing data
                    totalValue += value;
                    count++;
                }
            }

            if (count > 0) {
                // Average the values (0-1 scale)
                double healthIndex = totalValue / count;

                double finalValue = healthIndex;
                latestVegetationHealth = finalValue;
                runOnUiThread(() -> {
                    vegetationHealthText.setText("Vegetation Health: " + String.format("%.3f", finalValue));

                    // Color code based on vegetation health
                    if (finalValue > 0.6) {
                        // Green = healthy vegetation
                        vegetationHealthText.setTextColor(Color.parseColor("#2E7D32"));
                    } else if (finalValue > 0.3) {
                        // Yellow = moderate vegetation
                        vegetationHealthText.setTextColor(Color.parseColor("#F9A825"));
                    } else {
                        // Red = sparse/stressed vegetation
                        vegetationHealthText.setTextColor(Color.parseColor("#C62828"));
                    }
                    publishSatelliteStateIfReady();
                });
            } else {
                runOnUiThread(() -> vegetationHealthText.setText("Vegetation Health: No data available"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("NASA_PARSE", "Parse error: " + e.getMessage());
            runOnUiThread(() -> vegetationHealthText.setText("Vegetation Health: Parse error"));
        }
    }

    private void publishSatelliteStateIfReady() {
        if (Double.isNaN(latestVegetationHealth)
                || Double.isNaN(latestSoilMoisture)
                || Double.isNaN(latestEvapotranspiration)) {
            return;
        }

        AcreAgentRepository.getInstance().updateSatellite(
                new AcreAgentRepository.SatelliteState(
                        latestRegion,
                        latestVegetationHealth,
                        latestSoilMoisture,
                        latestEvapotranspiration
                )
        );
    }

    // ---------------- PERMISSION CALLBACK ----------------

    // Handle user's response to permission request
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, get location
                fetchCurrentLocation();
            } else {
                // Permission denied, show message
                Toast.makeText(this, "Location permission denied. Enter coordinates manually.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

