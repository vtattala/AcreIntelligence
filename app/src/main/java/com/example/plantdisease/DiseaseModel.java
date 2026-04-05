package com.example.plantdisease;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class DiseaseModel {
    private Interpreter tflite;
    private List<String> diseaseLabels;
    private static final int IMG_SIZE = 224;
    private static final int NUM_THREADS = 4;
    private static final String TAG = "DiseaseModel";

    public static class Prediction {
        public String diseaseName;
        public float confidence;

        public Prediction(String name, float conf) {
            diseaseName = name;
            confidence = conf;
        }
    }

    public DiseaseModel(Context context) throws Exception {
        try {
            Log.i(TAG, "Starting model initialization...");

            // Load disease labels first
            diseaseLabels = loadLabels(context);
            Log.i(TAG, "Labels loaded: " + diseaseLabels.size() + " classes");

            // Load TFLite model
            Log.i(TAG, "Loading TFLite model from assets...");
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context, "plant_disease_model.tflite");
            Log.i(TAG, "Model buffer loaded, size: " + tfliteModel.capacity() + " bytes");

            // Create interpreter
            Interpreter.Options options = new Interpreter.Options();
            options.setNumThreads(NUM_THREADS);

            Log.i(TAG, "Creating interpreter...");
            tflite = new Interpreter(tfliteModel, options);
            Log.i(TAG, "✓ Model loaded successfully!");

        } catch (Exception e) {
            Log.e(TAG, "FATAL ERROR loading model", e);
            tflite = null;
            diseaseLabels = null;
            throw new Exception("Model initialization failed: " + e.getMessage(), e);
        }
    }

    private List<String> loadLabels(Context context) throws IOException {
        List<String> labels = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(context.getAssets().open("disease_labels.json"))
            );
            StringBuilder jsonString = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonString.append(line);
            }
            reader.close();

            Gson gson = new Gson();
            JsonObject jsonObject = gson.fromJson(jsonString.toString(), JsonObject.class);
            JsonObject classesObject = jsonObject.getAsJsonObject("classes"); // get the dictionary

            // Convert keys to a list and sort numerically
            List<Integer> keys = new ArrayList<>();
            for (String key : classesObject.keySet()) {
                keys.add(Integer.parseInt(key));
            }
            keys.sort(Integer::compareTo);

            // Add class names to labels list in ID order
            for (Integer key : keys) {
                String className = classesObject.get(key.toString()).getAsString();
                labels.add(className); // now labels[0] corresponds to class ID 100
            }

            Log.i(TAG, "Successfully loaded " + labels.size() + " disease labels");
        } catch (Exception e) {
            Log.e(TAG, "Error loading labels: " + e.getMessage(), e);
            throw new IOException("Failed to load disease labels", e);
        }

        return labels;
    }

    public Prediction predictDisease(Bitmap bitmap) {
        if (bitmap == null || tflite == null || diseaseLabels == null) {
            Log.e(TAG, "Prediction failed: bitmap=" + (bitmap != null) +
                    ", tflite=" + (tflite != null) +
                    ", labels=" + (diseaseLabels != null));
            return null;
        }

        try {
            Log.i(TAG, "Starting prediction...");

            // Process image
            TensorImage image = new TensorImage();
            image.load(bitmap);

            ImageProcessor imageProcessor = new ImageProcessor.Builder()
                    .add(new ResizeOp(IMG_SIZE, IMG_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                    .add(new NormalizeOp(0f, 255f))
                    .build();

            TensorImage processedImage = imageProcessor.process(image);
            Log.i(TAG, "Image processed");

            // Run inference
            float[][] output = new float[1][diseaseLabels.size()];
            tflite.run(processedImage.getBuffer(), output);
            Log.i(TAG, "Inference complete");

            // Find best prediction
            float maxConfidence = 0;
            int maxIndex = 0;
            for (int i = 0; i < output[0].length; i++) {
                if (output[0][i] > maxConfidence) {
                    maxConfidence = output[0][i];
                    maxIndex = i;
                }
            }

            Log.i(TAG, "Prediction: " + diseaseLabels.get(maxIndex) +
                    " (" + (maxConfidence * 100) + "%)");

            return new Prediction(diseaseLabels.get(maxIndex), maxConfidence);

        } catch (Exception e) {
            Log.e(TAG, "Prediction error: " + e.getMessage(), e);
            return null;
        }
    }

    public void close() {
        if (tflite != null) {
            tflite.close();
            Log.i(TAG, "Model closed");
        }
    }
}