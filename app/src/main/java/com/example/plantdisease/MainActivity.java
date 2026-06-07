package com.example.plantdisease;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.content.pm.PackageManager;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private ImageView imageView;
    private TextView resultText, diagnosisAgentText;
    private ProgressBar progressBar;
    private Button cameraBtn, galleryBtn, diagnosisAgentBtn;
    private Bitmap selectedBitmap;
    private DiseaseModel diseaseModel;
    private DiseaseModel.Prediction latestPrediction;
    private static final int PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Get user info from welcome screen
        Intent intent = getIntent();
        String userName = intent.getStringExtra("USER_NAME");
        String userRegion = intent.getStringExtra("USER_REGION");

        if (userName != null && userRegion != null) {
            // You can display it somewhere or use it later
            android.util.Log.i(TAG, "User: " + userName + " from " + userRegion);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.i(TAG, "MainActivity onCreate");

        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);
        diagnosisAgentText = findViewById(R.id.diagnosisAgentText);
        progressBar = findViewById(R.id.progressBar);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        diagnosisAgentBtn = findViewById(R.id.diagnosisAgentBtn);
        diagnosisAgentText.setVisibility(View.GONE);
        diagnosisAgentBtn.setVisibility(View.GONE);
        diagnosisAgentBtn.setOnClickListener(v -> {
            if (latestPrediction != null) {
                diagnosisAgentText.setText(buildDiseaseAgentAdvice(
                        latestPrediction.diseaseName,
                        latestPrediction.confidence
                ));
                diagnosisAgentText.setVisibility(View.VISIBLE);
            }
        });

        // Load model on background thread
        resultText.setText("Loading ML model...");
        new Thread(() -> {
            try {
                Log.i(TAG, "Loading DiseaseModel on thread...");
                diseaseModel = new DiseaseModel(MainActivity.this);
                runOnUiThread(() -> {
                    Toast.makeText(this, "✓ Model loaded!", Toast.LENGTH_LONG).show();
                    resultText.setText("Ready! Select an image to analyze");
                    Log.i(TAG, "Model loaded successfully");
                });
            } catch (Exception e) {
                Log.e(TAG, "Model loading failed", e);
                runOnUiThread(() -> {
                    String errorMsg = "❌ Model Error: " + e.getMessage();
                    resultText.setText(errorMsg);
                    Toast.makeText(MainActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        }).start();

        requestPermissions();

        cameraBtn.setOnClickListener(v -> openCamera());
        galleryBtn.setOnClickListener(v -> openGallery());
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this,
                        new String[]{
                                android.Manifest.permission.CAMERA,
                                android.Manifest.permission.READ_EXTERNAL_STORAGE
                        },
                        PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Permissions granted");
            } else {
                Log.w(TAG, "Permissions denied");
            }
        }
    }

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedBitmap = (Bitmap) result.getData().getExtras().get("data");
                            imageView.setImageBitmap(selectedBitmap);
                            identifyDisease();
                        }
                    });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri imageUri = result.getData().getData();
                            try {
                                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                                imageView.setImageBitmap(selectedBitmap);
                                identifyDisease();
                            } catch (IOException e) {
                                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(galleryIntent);
    }

    private void identifyDisease() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (diseaseModel == null) {
            Toast.makeText(this, "❌ Model not loaded yet", Toast.LENGTH_LONG).show();
            resultText.setText("❌ Model failed to load. Please restart app.");
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        resultText.setText("🔍 Analyzing plant...");
        diagnosisAgentText.setVisibility(View.GONE);
        diagnosisAgentBtn.setVisibility(View.GONE);
        Log.i(TAG, "Starting prediction...");

        new Thread(() -> {
            try {
                DiseaseModel.Prediction prediction = diseaseModel.predictDisease(selectedBitmap);

                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    if (prediction != null) {
                        latestPrediction = prediction;
                        resultText.setText(
                                "Disease: " + prediction.diseaseName + "\n\n" +
                                        "Confidence: " + String.format("%.2f%%", prediction.confidence * 100)
                        );
                        diagnosisAgentBtn.setVisibility(View.VISIBLE);
                        AcreAgentRepository.getInstance().updatePlant(
                                new AcreAgentRepository.PlantState(
                                        prediction.diseaseName,
                                        prediction.confidence
                                )
                        );
                        Log.i(TAG, "Prediction successful");
                    } else {
                        resultText.setText("❌ Prediction failed");
                        Toast.makeText(MainActivity.this, "Could not analyze image", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Prediction exception", e);
                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    resultText.setText("❌ Error: " + e.getMessage());
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (diseaseModel != null) {
            diseaseModel.close();
        }
    }

    private String buildDiseaseAgentAdvice(String diseaseName, float confidence) {
        String normalized = diseaseName == null ? "" : diseaseName.toLowerCase();
        String confidenceLabel = confidence >= 0.80f ? "High confidence"
                : confidence >= 0.55f ? "Moderate confidence"
                : "Low confidence";
        boolean healthy = normalized.contains("healthy");
        boolean blight = normalized.contains("blight");
        boolean rust = normalized.contains("rust");
        boolean mildew = normalized.contains("mildew") || normalized.contains("powdery");
        boolean spot = normalized.contains("spot") || normalized.contains("scab");

        StringBuilder advice = new StringBuilder();
        advice.append("Diagnosis Agent\n\n");
        advice.append("Primary read: ").append(confidenceLabel)
                .append(" model match for ").append(diseaseName).append(".\n\n");

        if (healthy) {
            advice.append("Risk level: Low.\n\n");
            advice.append("Agent reasoning: Healthy predictions usually mean the visible leaf pattern does not match the disease classes strongly. That does not rule out early stress, root issues, nutrient problems, or pests outside the image.\n\n");
            advice.append("Next checks: Compare older and newer leaves, check undersides for pests, and re-scan if symptoms spread.\n\n");
            advice.append("Action: Continue normal care and avoid unnecessary treatment.");
            return advice.toString();
        }

        advice.append("Risk level: ").append(confidence >= 0.80f ? "High" : "Medium")
                .append(". Treat this as a decision-support signal, not a final lab diagnosis.\n\n");
        advice.append("Agent reasoning: The model found a visual pattern that resembles a known disease class. A good field diagnosis should now check whether the symptom pattern, spread, weather, and watering conditions agree.\n\n");

        if (blight) {
            advice.append("Likely pattern: Blights often spread from lower or older leaves, especially when foliage stays wet.\n\n");
            advice.append("What to inspect: Brown lesions, yellow halos, rapid spread after rain, and leaves touching soil.\n\n");
            advice.append("Action: Remove badly affected leaves, improve airflow, avoid overhead watering, mulch against splash, and sanitize tools.");
        } else if (rust) {
            advice.append("Likely pattern: Rust is often strongest on undersides and can move quickly in humid conditions.\n\n");
            advice.append("What to inspect: Orange or brown powdery spots, leaf yellowing, and nearby infected plants.\n\n");
            advice.append("Action: Remove infected material, reduce leaf wetness, and separate heavily affected plants.");
        } else if (mildew) {
            advice.append("Likely pattern: Mildew is favored by dense canopy, humidity, and weak airflow.\n\n");
            advice.append("What to inspect: White powder, crowded leaves, shaded zones, and recurring moisture.\n\n");
            advice.append("Action: Improve spacing, prune for airflow, and avoid late-day watering.");
        } else if (spot) {
            advice.append("Likely pattern: Leaf spots can come from fungal, bacterial, or environmental stress, so spread pattern matters.\n\n");
            advice.append("What to inspect: Expanding spots, leaf drop, splash patterns, and whether many plants show the same pattern.\n\n");
            advice.append("Action: Remove infected leaves, mulch to reduce soil splash, and monitor spread over 48 hours.");
        } else {
            advice.append("Likely pattern: The model sees disease-like visual stress, but field context is needed.\n\n");
            advice.append("What to inspect: Whether symptoms are spreading, localized, or linked to water, heat, nutrient, or pest stress.\n\n");
            advice.append("Action: Take another image in good light, compare multiple leaves, and use the Acre Agent for combined field context.");
        }

        advice.append("\n\nEscalate if: symptoms spread fast, stems/fruit are affected, many plants show the same pattern, or confidence is high across repeated scans.");
        if (confidence < 0.55f) {
            advice.append("\n\nUncertainty: Confidence is low, so re-scan with a clearer image before taking major action.");
        }
        return advice.toString();
    }
}
