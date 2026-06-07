package com.waterproj.groundwaterpredictor;

import android.content.res.AssetFileDescriptor;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.plantdisease.R;
import com.google.android.material.button.MaterialButton;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Locale;

public class SoilMoisturePanelController {
    private static final String MODEL_FILE = "soil_moisture_model.tflite";
    private static final String REAL_SEED_FILE = "soil_moisture_real_seed.bin";
    private static final int REGION_SEED_COUNT = 2;

    private final Fragment fragment;
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
    private float[] realSeedValues = new float[0];
    private int seedElementsPerRegion = 0;

    public SoilMoisturePanelController(Fragment fragment) {
        this.fragment = fragment;
    }

    public void bind(View view) {
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
                fragment.requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                RegionMapper.getSupportedRegions()
        ));
        profileSpinner.setAdapter(new ArrayAdapter<>(
                fragment.requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                new String[]{"Real SoMo processed tensor"}
        ));

        recentRainSeekBar.setMax(100);
        irrigationSeekBar.setMax(100);
        recentRainSeekBar.setProgress(45);
        irrigationSeekBar.setProgress(25);
        profileSpinner.setEnabled(false);
        recentRainSeekBar.setEnabled(false);
        irrigationSeekBar.setEnabled(false);
        updateSliderLabels();

        recentRainSeekBar.setOnSeekBarChangeListener(simpleSeekListener());
        irrigationSeekBar.setOnSeekBarChangeListener(simpleSeekListener());
        runButton.setOnClickListener(v -> runSoilPrediction());

        try {
            loadRealSeedValues();
            interpreter = new Interpreter(loadModelFile());
            modelShapeText.setText("Real SoMo seed loaded: " + describeTensorShapes());
        } catch (Exception exception) {
            showError("Could not load soil moisture model: " + exception.getMessage());
            runButton.setEnabled(false);
        }
    }

    public void close() {
        if (interpreter != null) {
            interpreter.close();
            interpreter = null;
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
        if (!recentRainSeekBar.isEnabled()) {
            recentRainValue.setText("Using the latest processed SoMo sequence from water_proj.");
            irrigationValue.setText("Scenario sliders are disabled in real-data mode.");
            return;
        }
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
        int regionIndex = region.contains("Michigan") ? 1 : 0;
        int regionOffset = regionIndex * seedElementsPerRegion;

        for (int i = 0; i < inputElements; i++) {
            int seedIndex = regionOffset + (i % seedElementsPerRegion);
            inputBuffer.putFloat(realSeedValues[seedIndex]);
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
        String status = classifyMoisture(average);
        summaryText.setText("Soil Moisture: " + status);
        averageText.setText(String.format(Locale.US, "Average moisture index: %.3f", average));
        rangeText.setText(String.format(Locale.US, "Predicted range: %.3f to %.3f", min, max));
        guidanceText.setText(buildGuidance(average));
        WaterAgentRepository.getInstance().updateSoil(
                new WaterAgentRepository.SoilState(
                        String.valueOf(regionSpinner.getSelectedItem()),
                        status,
                        average,
                        min,
                        max
                )
        );
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
            return "Surface moisture is low. That can reduce near-term recharge unless rain or irrigation reaches below the root zone.";
        }
        if (average > 0.66f) {
            return "The surface layer is wet. Recharge potential is better if soils keep draining downward instead of running off.";
        }
        return "Surface moisture looks balanced. Compare this with the aquifer heatmap to judge short-term recharge support.";
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
        AssetFileDescriptor descriptor = fragment.requireContext().getAssets().openFd(MODEL_FILE);
        FileInputStream inputStream = new FileInputStream(descriptor.getFileDescriptor());
        FileChannel channel = inputStream.getChannel();
        return channel.map(FileChannel.MapMode.READ_ONLY, descriptor.getStartOffset(), descriptor.getDeclaredLength());
    }

    private void loadRealSeedValues() throws IOException {
        byte[] bytes;
        try (InputStream inputStream = fragment.requireContext().getAssets().open(REAL_SEED_FILE)) {
            bytes = new byte[inputStream.available()];
            int offset = 0;
            while (offset < bytes.length) {
                int read = inputStream.read(bytes, offset, bytes.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        realSeedValues = new float[bytes.length / 4];
        for (int i = 0; i < realSeedValues.length; i++) {
            realSeedValues[i] = buffer.getFloat();
        }

        if (realSeedValues.length < REGION_SEED_COUNT || realSeedValues.length % REGION_SEED_COUNT != 0) {
            throw new IOException("Unexpected real soil seed length: " + realSeedValues.length);
        }
        seedElementsPerRegion = realSeedValues.length / REGION_SEED_COUNT;
    }

    private void showError(String message) {
        errorText.setVisibility(View.VISIBLE);
        errorText.setText(message == null ? "Unknown soil moisture error." : message);
    }
}
