package com.waterproj.groundwaterpredictor;

import android.content.res.AssetFileDescriptor;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.plantdisease.R;
import com.google.android.material.button.MaterialButton;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;

public class SoilMoisturePredictionFragment extends Fragment {
    private static final String MODEL_FILE = "soil_moisture_model.tflite";

    private Spinner regionSpinner;
    private Spinner profileSpinner;
    private SeekBar recentRainSeekBar;
    private SeekBar irrigationSeekBar;
    private TextView recentRainValue;
    private TextView irrigationValue;
    private TextView summaryText;
    private TextView averageText;
    private TextView rangeText;
    private TextView guidanceText;
    private TextView modelShapeText;
    private TextView errorText;
    private ProgressBar loadingBar;
    private MaterialButton runButton;
    private SoilMoistureHeatmapView heatmapView;
    private Interpreter interpreter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_soil_moisture_prediction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        regionSpinner = view.findViewById(R.id.soilRegionSpinner);
        profileSpinner = view.findViewById(R.id.soilProfileSpinner);
        recentRainSeekBar = view.findViewById(R.id.recentRainSeekBar);
        irrigationSeekBar = view.findViewById(R.id.irrigationSeekBar);
        recentRainValue = view.findViewById(R.id.recentRainValue);
        irrigationValue = view.findViewById(R.id.irrigationValue);
        summaryText = view.findViewById(R.id.soilSummaryText);
        averageText = view.findViewById(R.id.soilAverageText);
        rangeText = view.findViewById(R.id.soilRangeText);
        guidanceText = view.findViewById(R.id.soilGuidanceText);
        modelShapeText = view.findViewById(R.id.soilModelShapeText);
        errorText = view.findViewById(R.id.soilErrorText);
        loadingBar = view.findViewById(R.id.soilLoadingBar);
        runButton = view.findViewById(R.id.runSoilPredictionButton);
        heatmapView = view.findViewById(R.id.soilHeatmapView);

        regionSpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                RegionMapper.getSupportedRegions()
        ));
        profileSpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Balanced field", "Dry sandy soil", "Clay-heavy soil", "Recently irrigated"}
        ));

        recentRainSeekBar.setMax(100);
        irrigationSeekBar.setMax(100);
        recentRainSeekBar.setProgress(45);
        irrigationSeekBar.setProgress(25);
        updateSliderLabels();

        recentRainSeekBar.setOnSeekBarChangeListener(simpleSeekListener());
        irrigationSeekBar.setOnSeekBarChangeListener(simpleSeekListener());
        runButton.setOnClickListener(v -> runSoilPrediction());

        try {
            interpreter = new Interpreter(loadModelFile());
            modelShapeText.setText("Model ready: " + describeTensorShapes());
        } catch (Exception exception) {
            showError("Could not load soil moisture model: " + exception.getMessage());
            runButton.setEnabled(false);
        }
    }

    private SeekBar.OnSeekBarChangeListener simpleSeekListener() {
        return new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateSliderLabels();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        };
    }

    private void updateSliderLabels() {
        recentRainValue.setText(recentRainSeekBar.getProgress() + "%");
        irrigationValue.setText(irrigationSeekBar.getProgress() + "%");
    }

    private void runSoilPrediction() {
        if (interpreter == null) {
            showError("Soil moisture model is not loaded.");
            return;
        }

        loadingBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        runButton.setEnabled(false);

        try {
            int inputElements = interpreter.getInputTensor(0).numElements();
            int outputElements = interpreter.getOutputTensor(0).numElements();

            ByteBuffer inputBuffer = ByteBuffer.allocateDirect(inputElements * 4).order(ByteOrder.nativeOrder());
            fillInputTensor(inputBuffer, inputElements);

            ByteBuffer outputBuffer = ByteBuffer.allocateDirect(outputElements * 4).order(ByteOrder.nativeOrder());
            interpreter.run(inputBuffer, outputBuffer);
            outputBuffer.rewind();

            float[] output = new float[outputElements];
            for (int i = 0; i < output.length; i++) {
                output[i] = outputBuffer.getFloat();
            }

            showPrediction(output);
        } catch (Exception exception) {
            showError("Soil prediction failed: " + exception.getMessage());
        } finally {
            loadingBar.setVisibility(View.GONE);
            runButton.setEnabled(interpreter != null);
        }
    }

    private void fillInputTensor(ByteBuffer inputBuffer, int inputElements) {
        String region = String.valueOf(regionSpinner.getSelectedItem());
        String profile = String.valueOf(profileSpinner.getSelectedItem());
        float rain = recentRainSeekBar.getProgress() / 100f;
        float irrigation = irrigationSeekBar.getProgress() / 100f;
        float regionBias = region.contains("California") ? 0.42f : 0.54f;
        float profileBias = profile.contains("Dry") ? -0.18f
                : profile.contains("Clay") ? 0.08f
                : profile.contains("irrigated") ? 0.20f
                : 0.0f;

        for (int i = 0; i < inputElements; i++) {
            float wave = (float) Math.sin(i * 0.071f) * 0.06f;
            float gradient = (i % 32) / 31f * 0.08f;
            float value = regionBias + profileBias + (rain * 0.20f) + (irrigation * 0.24f) + wave + gradient;
            inputBuffer.putFloat(Math.max(0f, Math.min(1f, value)));
        }
        inputBuffer.rewind();
    }

    private void showPrediction(float[] output) {
        if (output.length == 0) {
            showError("The soil model returned no values.");
            return;
        }

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        float sum = 0f;
        for (float value : output) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }

        float average = sum / output.length;
        int[] outputShape = interpreter.getOutputTensor(0).shape();
        int rows = outputShape.length >= 3 ? Math.max(1, outputShape[outputShape.length - 3]) : 32;
        int columns = outputShape.length >= 2 ? Math.max(1, outputShape[outputShape.length - 2]) : Math.max(1, output.length / rows);

        heatmapView.setHeatmap(output, rows, columns);
        summaryText.setText("Soil Moisture: " + classifyMoisture(average));
        averageText.setText(String.format(Locale.US, "Average moisture index: %.3f", average));
        rangeText.setText(String.format(Locale.US, "Predicted range: %.3f to %.3f", min, max));
        guidanceText.setText(buildGuidance(average));
    }

    private String classifyMoisture(float average) {
        if (average < 0.33f) {
            return "Dry";
        }
        if (average > 0.66f) {
            return "Wet";
        }
        return "Moderate";
    }

    private String buildGuidance(float average) {
        if (average < 0.33f) {
            return "Moisture is low. Consider irrigation soon and watch shallow-root crops closely.";
        }
        if (average > 0.66f) {
            return "Soil is holding plenty of water. Delay extra irrigation and monitor drainage-sensitive areas.";
        }
        return "Moisture looks balanced. Keep current irrigation plans and recheck after rainfall changes.";
    }

    private String describeTensorShapes() {
        return "input " + shapeToString(interpreter.getInputTensor(0).shape())
                + ", output " + shapeToString(interpreter.getOutputTensor(0).shape());
    }

    private String shapeToString(int[] shape) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < shape.length; i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(shape[i]);
        }
        return builder.append("]").toString();
    }

    private MappedByteBuffer loadModelFile() throws IOException {
        AssetFileDescriptor descriptor = requireContext().getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(descriptor.getFileDescriptor());
        FileChannel channel = inputStream.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY, descriptor.getStartOffset(), descriptor.getDeclaredLength());
    }

    private void showError(String message) {
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(message == null ? "Unknown soil moisture error." : message);
    }

    @Override
    public void onDestroyView() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
        }
        super.onDestroyView();
    }
}
