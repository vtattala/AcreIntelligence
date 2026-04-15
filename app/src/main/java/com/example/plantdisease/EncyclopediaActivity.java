package com.example.plantdisease;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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

public class EncyclopediaActivity extends AppCompatActivity {

    private static final String TAG = "Encyclopedia";
    private static final String TREFLE_API_KEY = "usr-cOa8wIEJtVxA5a2ny-l7maXCHTlcPiGjT9TnBlMMfvI"; // REPLACE WITH YOUR KEY

    private EditText searchInput;
    private Button searchBtn, backBtn;
    private TextView resultText;
    private ImageView plantImage;
    private ProgressBar progressBar;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encyclopedia);

        searchInput = findViewById(R.id.searchInput);
        searchBtn = findViewById(R.id.searchBtn);
        backBtn = findViewById(R.id.backBtn);
        resultText = findViewById(R.id.resultText);
        plantImage = findViewById(R.id.plantImage);
        progressBar = findViewById(R.id.progressBar);
        scrollView = findViewById(R.id.scrollView);

        searchBtn.setOnClickListener(v -> searchPlant());
        backBtn.setOnClickListener(v -> finish());
    }

    private void searchPlant() {
        String query = searchInput.getText().toString().trim();

        if (query.isEmpty()) {
            Toast.makeText(this, "Please enter a plant name", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        resultText.setText("🔍 Searching 400,000+ plant species...");
        plantImage.setImageDrawable(null);

        // Search in background thread
        new Thread(() -> {
            try {
                PlantData data = searchTrefleAPI(query);

                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    if (data != null) {
                        displayPlantInfo(data);
                        if (data.imageUrl != null && !data.imageUrl.isEmpty()) {
                            loadImage(data.imageUrl);
                        }
                    } else {
                        resultText.setText("❌ Plant not found\n\n" +
                                "Try:\n" +
                                "• Common names (Tomato, Rose, Basil)\n" +
                                "• Scientific names (Rosa damascena)\n" +
                                "• Check spelling\n" +
                                "• Use more specific terms");
                    }
                });

            } catch (Exception e) {
                Log.e(TAG, "Search error", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    resultText.setText("❌ Error: " + e.getMessage() +
                            "\n\nCheck internet connection");
                });
            }
        }).start();
    }

    private PlantData searchTrefleAPI(String query) {
        try {
            // URL encode the query
            String encodedQuery = query.replace(" ", "%20");
            String urlString = "https://trefle.io/api/v1/plants/search?token=" +
                    TREFLE_API_KEY + "&q=" + encodedQuery;

            Log.i(TAG, "Searching: " + urlString);

            // Make API request
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);

            int responseCode = conn.getResponseCode();
            Log.i(TAG, "Response code: " + responseCode);

            if (responseCode != 200) {
                Log.e(TAG, "API returned error code: " + responseCode);
                return null;
            }

            // Read response
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream())
            );
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Parse JSON
            JsonObject json = new Gson().fromJson(response.toString(), JsonObject.class);
            JsonArray data = json.getAsJsonArray("data");

            if (data == null || data.size() == 0) {
                Log.i(TAG, "No results found");
                return null;
            }

            // Get first result
            JsonObject plant = data.get(0).getAsJsonObject();

            PlantData result = new PlantData();
            result.commonName = getJsonString(plant, "common_name");
            result.scientificName = getJsonString(plant, "scientific_name");
            result.family = getJsonString(plant, "family");
            result.genus = getJsonString(plant, "genus");
            result.year = getJsonString(plant, "year");
            result.author = getJsonString(plant, "author");
            result.imageUrl = getJsonString(plant, "image_url");

            // Get additional details if available
            if (plant.has("family_common_name") && !plant.get("family_common_name").isJsonNull()) {
                result.familyCommon = plant.get("family_common_name").getAsString();
            }

            Log.i(TAG, "Found: " + result.commonName);
            return result;

        } catch (Exception e) {
            Log.e(TAG, "API Error", e);
            return null;
        }
    }

    private String getJsonString(JsonObject obj, String key) {
        try {
            if (obj.has(key) && !obj.get(key).isJsonNull()) {
                return obj.get(key).getAsString();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting " + key, e);
        }
        return "Not available";
    }

    private void displayPlantInfo(PlantData data) {
        StringBuilder result = new StringBuilder();

        result.append("🌿 ").append(data.commonName.toUpperCase()).append("\n");
        result.append("═══════════════════════════════\n\n");

        result.append("📋 Scientific Name:\n");
        result.append(data.scientificName);
        if (!data.author.equals("Not available")) {
            result.append(" ").append(data.author);
        }
        if (!data.year.equals("Not available")) {
            result.append(" (").append(data.year).append(")");
        }
        result.append("\n\n");

        if (!data.family.equals("Not available")) {
            result.append("👨‍👩‍👧 Family:\n");
            result.append(data.family);
            if (!data.familyCommon.equals("Not available")) {
                result.append(" (").append(data.familyCommon).append(")");
            }
            result.append("\n\n");
        }

        if (!data.genus.equals("Not available")) {
            result.append("🧬 Genus:\n");
            result.append(data.genus).append("\n\n");
        }

        result.append("─────────────────────────\n");
        result.append("📚 Source: Trefle Global Plant Database\n");
        result.append("🌍 Database: 400,000+ species");

        resultText.setText(result.toString());
        scrollView.smoothScrollTo(0, 0);
    }

    private void loadImage(String imageUrl) {
        new Thread(() -> {
            try {
                URL url = new URL(imageUrl);
                Bitmap bitmap = BitmapFactory.decodeStream(
                        url.openConnection().getInputStream()
                );

                runOnUiThread(() -> {
                    if (bitmap != null) {
                        plantImage.setImageBitmap(bitmap);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Image load error", e);
            }
        }).start();
    }

    private static class PlantData {
        String commonName = "Unknown Plant";
        String scientificName = "Not available";
        String family = "Not available";
        String familyCommon = "Not available";
        String genus = "Not available";
        String year = "Not available";
        String author = "Not available";
        String imageUrl = "";
    }
}
