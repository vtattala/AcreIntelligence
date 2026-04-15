package com.example.plantdisease;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class RegionalGuideActivity extends AppCompatActivity {

    private static final String TAG = "RegionalGuide";
    private static final String WEATHER_API_KEY = "1cb1a88483037ec05c04e9eee9bd5521";

    private EditText locationInput;
    private Button searchBtn, backBtn;
    private TextView resultText;
    private ImageView mapView;
    private ProgressBar progressBar;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_regional_guide);

        locationInput = findViewById(R.id.locationInput);
        searchBtn = findViewById(R.id.searchBtn);
        backBtn = findViewById(R.id.backBtn);
        resultText = findViewById(R.id.resultText);
        mapView = findViewById(R.id.mapView);
        progressBar = findViewById(R.id.progressBar);
        scrollView = findViewById(R.id.scrollView);

        searchBtn.setOnClickListener(v -> searchRegion());
        backBtn.setOnClickListener(v -> finish());
    }

    private void searchRegion() {
        String location = locationInput.getText().toString().trim();

        if (location.isEmpty()) {
            Toast.makeText(this, "Enter a city or region", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        resultText.setText("🌍 Analyzing agricultural conditions for " + location + "...");

        new Thread(() -> {
            RegionalData data = fetchRegionalData(location);

            runOnUiThread(() -> {
                progressBar.setVisibility(ProgressBar.GONE);
                if (data != null) {
                    displayRegionalInfo(data);
                    loadMapImage(data.latitude, data.longitude);
                } else {
                    resultText.setText("❌ Location not found. Try a major city or region.");
                }
            });
        }).start();
    }

    private RegionalData fetchRegionalData(String location) {
        try {
            String encodedLocation = location.replace(" ", "%20");
            String weatherUrl =
                    "https://api.openweathermap.org/data/2.5/weather?q=" +
                            encodedLocation +
                            "&appid=" + WEATHER_API_KEY +
                            "&units=metric";

            String response = makeRequest(weatherUrl);
            if (response == null) return null;

            JsonObject json = new Gson().fromJson(response, JsonObject.class);

            RegionalData data = new RegionalData();
            data.location = json.get("name").getAsString();
            data.country = json.getAsJsonObject("sys").get("country").getAsString();

            JsonObject coord = json.getAsJsonObject("coord");
            data.latitude = coord.get("lat").getAsDouble();
            data.longitude = coord.get("lon").getAsDouble();

            JsonObject main = json.getAsJsonObject("main");
            data.temperature = main.get("temp").getAsDouble();
            data.humidity = main.get("humidity").getAsInt();

            JsonArray weatherArray = json.getAsJsonArray("weather");
            data.weatherDescription =
                    weatherArray.get(0).getAsJsonObject().get("description").getAsString();

            determineClimateAndPlants(data);
            return data;

        } catch (Exception e) {
            Log.e(TAG, "Weather API error", e);
            return null;
        }
    }

    private void determineClimateAndPlants(RegionalData data) {
        double lat = Math.abs(data.latitude);

        if (lat < 23.5) {
            data.climateZone = "Tropical";
            data.idealVegetables = "Tomatoes, Peppers, Eggplant, Okra";
            data.idealGrains = "Rice, Corn, Millet";
            data.idealFruits = "Mango, Banana, Papaya";
            data.wateringFrequency = "Daily";
            data.growingSeason = "Year‑round";
        } else if (lat < 35) {
            data.climateZone = "Subtropical";
            data.idealVegetables = "Tomatoes, Cucumbers, Squash";
            data.idealGrains = "Wheat, Corn";
            data.idealFruits = "Citrus, Grapes";
            data.wateringFrequency = "Every 2‑3 days";
            data.growingSeason = "March‑October";
        } else if (lat < 50) {
            data.climateZone = "Temperate";
            data.idealVegetables = "Potatoes, Cabbage, Lettuce";
            data.idealGrains = "Wheat, Barley";
            data.idealFruits = "Apples, Berries";
            data.wateringFrequency = "Every 3‑5 days";
            data.growingSeason = "April‑September";
        } else {
            data.climateZone = "Cold";
            data.idealVegetables = "Kale, Root vegetables";
            data.idealGrains = "Barley, Rye";
            data.idealFruits = "Berries";
            data.wateringFrequency = "Every 4‑7 days";
            data.growingSeason = "May‑August";
        }

        if (data.temperature < 10) {
            data.currentAdvice = "⚠️ Too cold for planting.";
        } else if (data.temperature > 30) {
            data.currentAdvice = "☀️ High heat — increase irrigation.";
        } else {
            data.currentAdvice = "✅ Good growing conditions.";
        }
    }

    private String makeRequest(String urlString) {
        try {
            HttpURLConnection conn =
                    (HttpURLConnection) new URL(urlString).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() != 200) return null;

            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            return response.toString();

        } catch (Exception e) {
            Log.e(TAG, "Request error", e);
            return null;
        }
    }

    private void loadMapImage(double latitude, double longitude) {
        new Thread(() -> {
            try {
                int zoom = 12;

                double x = Math.floor((longitude + 180) / 360 * (1 << zoom));
                double y = Math.floor(
                        (1 - Math.log(Math.tan(Math.toRadians(latitude)) +
                                1 / Math.cos(Math.toRadians(latitude))) / Math.PI) / 2 * (1 << zoom)
                );

                String mapUrl = String.format(Locale.US,
                        "https://services.arcgisonline.com/ArcGIS/rest/services/" +
                                "World_Imagery/MapServer/tile/%d/%d/%d",
                        zoom, (int) y, (int) x
                );

                Log.i(TAG, "Loading satellite tile: " + mapUrl);

                HttpURLConnection conn =
                        (HttpURLConnection) new URL(mapUrl).openConnection();
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);

                if (conn.getResponseCode() == 200) {
                    android.graphics.Bitmap bitmap =
                            android.graphics.BitmapFactory.decodeStream(conn.getInputStream());

                    runOnUiThread(() -> {
                        mapView.setImageBitmap(bitmap);
                        mapView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    });
                }

                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Satellite map error", e);
            }
        }).start();
    }

    private void displayRegionalInfo(RegionalData data) {
        StringBuilder result = new StringBuilder();

        result.append("📍 ").append(data.location.toUpperCase())
                .append(", ").append(data.country).append("\n\n");

        result.append("🌡️ Temperature: ")
                .append(String.format("%.1f°C", data.temperature)).append("\n");
        result.append("💧 Humidity: ").append(data.humidity).append("%\n");
        result.append("🌦️ Weather: ").append(capitalize(data.weatherDescription)).append("\n\n");

        result.append("🌍 Climate Zone: ").append(data.climateZone).append("\n");
        result.append("📅 Growing Season: ").append(data.growingSeason).append("\n\n");

        result.append("🥬 Vegetables: ").append(data.idealVegetables).append("\n\n");
        result.append("🌾 Grains: ").append(data.idealGrains).append("\n\n");
        result.append("🍎 Fruits: ").append(data.idealFruits).append("\n\n");

        result.append("💧 Watering: ").append(data.wateringFrequency).append("\n\n");
        result.append("📌 Advice: ").append(data.currentAdvice).append("\n\n");

        result.append("Updated: ")
                .append(new SimpleDateFormat("MMM dd, HH:mm", Locale.US).format(new Date()));

        resultText.setText(result.toString());
        scrollView.smoothScrollTo(0, 0);
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    private static class RegionalData {
        String location;
        String country;
        double latitude;
        double longitude;
        double temperature;
        int humidity;
        String weatherDescription;
        String climateZone;
        String idealVegetables;
        String idealGrains;
        String idealFruits;
        String wateringFrequency;
        String growingSeason;
        String currentAdvice;
    }
}
