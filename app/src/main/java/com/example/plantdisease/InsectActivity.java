package com.example.plantdisease;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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
import java.io.IOException;

public class InsectActivity extends AppCompatActivity {

    private static final String TAG = "InsectActivity";
    private ImageView imageView;
    private TextView resultText, insectAgentText;
    private ProgressBar progressBar;
    private Button cameraBtn, galleryBtn, backBtn, insectAgentBtn;
    private Bitmap selectedBitmap;
    private InsectModel insectModel;
    private InsectModel.InsectPrediction latestPrediction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insect);

        Log.i(TAG, "InsectActivity onCreate");

        imageView = findViewById(R.id.imageView);
        resultText = findViewById(R.id.resultText);
        insectAgentText = findViewById(R.id.insectAgentText);
        progressBar = findViewById(R.id.progressBar);
        cameraBtn = findViewById(R.id.cameraBtn);
        galleryBtn = findViewById(R.id.galleryBtn);
        backBtn = findViewById(R.id.backBtn);
        insectAgentBtn = findViewById(R.id.insectAgentBtn);
        insectAgentText.setVisibility(View.GONE);
        insectAgentBtn.setVisibility(View.GONE);
        insectAgentBtn.setOnClickListener(v -> {
            if (latestPrediction != null) {
                insectAgentText.setText(buildInsectAgentAdvice(
                        latestPrediction.insectName,
                        latestPrediction.confidence,
                        latestPrediction.fact1,
                        latestPrediction.fact2
                ));
                insectAgentText.setVisibility(View.VISIBLE);
            }
        });

        // Load model
        resultText.setText("Loading insect model...");
        new Thread(() -> {
            try {
                Log.i(TAG, "Loading InsectModel...");
                insectModel = new InsectModel(InsectActivity.this);
                runOnUiThread(() -> {
                    Toast.makeText(this, "✓ Insect model loaded!", Toast.LENGTH_LONG).show();
                    resultText.setText("Ready! Capture or select an insect image");
                    Log.i(TAG, "Model loaded successfully");
                });
            } catch (Exception e) {
                Log.e(TAG, "Model loading failed", e);
                runOnUiThread(() -> {
                    String errorMsg = "❌ Model Error: " + e.getMessage();
                    resultText.setText(errorMsg);
                    Toast.makeText(InsectActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        }).start();

        cameraBtn.setOnClickListener(v -> openCamera());
        galleryBtn.setOnClickListener(v -> openGallery());
        backBtn.setOnClickListener(v -> finish());
    }

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            selectedBitmap = (Bitmap) result.getData().getExtras().get("data");
                            imageView.setImageBitmap(selectedBitmap);
                            identifyInsect();
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
                                identifyInsect();
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

    private void identifyInsect() {
        if (selectedBitmap == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        if (insectModel == null) {
            Toast.makeText(this, "❌ Model not loaded yet", Toast.LENGTH_LONG).show();
            resultText.setText("❌ Model failed to load. Please restart.");
            return;
        }

        progressBar.setVisibility(ProgressBar.VISIBLE);
        resultText.setText("🔍 Identifying insect...");
        insectAgentText.setVisibility(View.GONE);
        insectAgentBtn.setVisibility(View.GONE);
        Log.i(TAG, "Starting prediction...");

        new Thread(() -> {
            try {
                InsectModel.InsectPrediction prediction = insectModel.predictInsect(selectedBitmap);

                runOnUiThread(() -> {
                    progressBar.setVisibility(ProgressBar.GONE);
                    if (prediction != null) {
                        latestPrediction = prediction;
                        String result = "Insect: " + prediction.insectName + "\n\n" +
                                "Confidence: " + String.format("%.2f%%", prediction.confidence * 100) + "\n\n" +
                                "Impact on Crops:\n" +
                                "• " + prediction.fact1 + "\n" +
                                "• " + prediction.fact2;
                        resultText.setText(result);
                        insectAgentBtn.setVisibility(View.VISIBLE);
                        AcreAgentRepository.getInstance().updateInsect(
                                new AcreAgentRepository.InsectState(
                                        prediction.insectName,
                                        prediction.confidence,
                                        prediction.fact1,
                                        prediction.fact2
                                )
                        );
                        Log.i(TAG, "Prediction successful");
                    } else {
                        resultText.setText("❌ Prediction failed");
                        Toast.makeText(InsectActivity.this, "Could not analyze image", Toast.LENGTH_SHORT).show();
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
        if (insectModel != null) {
            insectModel.close();
        }
    }

    private String buildInsectAgentAdvice(String insectName, float confidence, String fact1, String fact2) {
        String normalized = insectName == null ? "" : insectName.toLowerCase();
        String confidenceLabel = confidence >= 0.80f ? "High confidence"
                : confidence >= 0.55f ? "Moderate confidence"
                : "Low confidence";
        boolean aphid = normalized.contains("aphid");
        boolean beetle = normalized.contains("beetle");
        boolean caterpillar = normalized.contains("caterpillar") || normalized.contains("worm");
        boolean mite = normalized.contains("mite");

        StringBuilder advice = new StringBuilder();
        advice.append("Pest Agent\n\n");
        advice.append("Primary read: ").append(confidenceLabel)
                .append(" model match for ").append(insectName).append(".\n\n");
        advice.append("Model-provided crop impact: ").append(fact1).append(" ").append(fact2).append("\n\n");
        advice.append("Risk level: ").append(confidence >= 0.80f ? "High" : confidence >= 0.55f ? "Medium" : "Uncertain")
                .append(". Confirm with field scouting before treatment.\n\n");
        advice.append("Agent reasoning: A pest diagnosis is useful only if the insect and the field damage match. The next step is to connect the insect ID with feeding signs, population size, and crop stage.\n\n");

        if (aphid) {
            advice.append("Likely pattern: Aphids cluster on tender growth and can cause curling, honeydew, and virus spread.\n\n");
            advice.append("What to inspect: Leaf undersides, sticky honeydew, curling leaves, ants, and clusters on new growth.\n\n");
            advice.append("Action: Wash off small populations, protect beneficial insects, and monitor spread.");
        } else if (beetle) {
            advice.append("Likely pattern: Beetle damage often appears as chewing, skeletonizing, or edge feeding.\n\n");
            advice.append("What to inspect: Chewed leaf edges, holes, larvae, and field-edge concentration.\n\n");
            advice.append("Action: Scout nearby plants, remove visible pests if practical, and track damage rate.");
        } else if (caterpillar) {
            advice.append("Likely pattern: Caterpillars can cause rapid defoliation when populations build.\n\n");
            advice.append("What to inspect: Fresh chewing, frass, rolled leaves, and larvae hidden in the canopy.\n\n");
            advice.append("Action: Remove larvae where possible and prioritize young plants or high-value rows.");
        } else if (mite) {
            advice.append("Likely pattern: Mites flare during hot, dry stress and may be hard to see without close inspection.\n\n");
            advice.append("What to inspect: Fine stippling, webbing, dusty leaf surfaces, and hot dry zones.\n\n");
            advice.append("Action: Reduce plant stress, avoid unnecessary broad sprays, and recheck after irrigation.");
        } else {
            advice.append("What to inspect: Feeding damage, pest count per plant, and whether damage is spreading.\n\n");
            advice.append("Action: Re-scan if confidence is low and use the Acre Agent to combine pest, disease, and water stress.");
        }

        advice.append("\n\nEscalate if: pest counts are rising, damage reaches new growth, multiple rows are affected, or young crops are losing leaf area.");
        if (confidence < 0.55f) {
            advice.append("\n\nUncertainty: Confidence is low, so capture a closer, brighter image before acting.");
        }
        return advice.toString();
    }
}
