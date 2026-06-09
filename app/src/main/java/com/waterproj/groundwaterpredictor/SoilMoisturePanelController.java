package com.waterproj.groundwaterpredictor;

import android.content.res.AssetFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.example.plantdisease.BuildConfig;
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
    private static final String TAG = "SoilMoistureHeatmap";
    private static final String MODEL_FILE = "soil_moisture_model.tflite";
    private static final String REAL_SEED_FILE = "soil_moisture_real_seed.bin";
    private static final int REGION_SEED_COUNT = 2;

    private final Fragment fragment;
    private Spinner regionSpinner;
    private Spinner profileSpinner;
    private EditText siteLatitudeInput;
    private EditText siteLongitudeInput;
    private SeekBar recentRainSeekBar;
    private SeekBar irrigationSeekBar;
    private TextView recentRainValue;
    private TextView irrigationValue;
    private TextView summaryText;
    private TextView averageText;
    private TextView rangeText;
    private TextView guidanceText;
    private TextView modelShapeText;
    private TextView liveDataText;
    private TextView errorText;
    private ProgressBar loadingBar;
    private MaterialButton runButton;
    private SoilMoistureHeatmapView heatmapView;
    private Interpreter interpreter;
    private float[] realSeedValues = new float[0];
    private int seedElementsPerRegion = 0;
    private Double activeSiteLatitude;
    private Double activeSiteLongitude;
    private LiveHydrologyData activeLiveHydrologyData = LiveHydrologyData.unavailable("Live data has not been fetched yet.");

    public SoilMoisturePanelController(Fragment fragment) {
        this.fragment = fragment;
    }

    public void bind(View view) {
        regionSpinner = view.findViewById(R.id.soilRegionSpinner);
        profileSpinner = view.findViewById(R.id.soilProfileSpinner);
        siteLatitudeInput = view.findViewById(R.id.soilSiteLatitudeInput);
        siteLongitudeInput = view.findViewById(R.id.soilSiteLongitudeInput);
        recentRainSeekBar = view.findViewById(R.id.recentRainSeekBar);
        irrigationSeekBar = view.findViewById(R.id.irrigationSeekBar);
        recentRainValue = view.findViewById(R.id.recentRainValue);
        irrigationValue = view.findViewById(R.id.irrigationValue);
        summaryText = view.findViewById(R.id.soilSummaryText);
        averageText = view.findViewById(R.id.soilAverageText);
        rangeText = view.findViewById(R.id.soilRangeText);
        guidanceText = view.findViewById(R.id.soilGuidanceText);
        modelShapeText = view.findViewById(R.id.soilModelShapeText);
        liveDataText = view.findViewById(R.id.liveSoilDataText);
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
        Double selectedLatitude = parseOptionalCoordinate(
                siteLatitudeInput == null ? "" : siteLatitudeInput.getText().toString().trim(),
                -90.0,
                90.0
        );
        Double selectedLongitude = parseOptionalCoordinate(
                siteLongitudeInput == null ? "" : siteLongitudeInput.getText().toString().trim(),
                -180.0,
                180.0
        );
        String latitudeText = siteLatitudeInput == null ? "" : siteLatitudeInput.getText().toString().trim();
        String longitudeText = siteLongitudeInput == null ? "" : siteLongitudeInput.getText().toString().trim();
        if (!latitudeText.isEmpty() && selectedLatitude == null) {
            showError("Latitude must be between -90 and 90.");
            return;
        }
        if (!longitudeText.isEmpty() && selectedLongitude == null) {
            showError("Longitude must be between -180 and 180.");
            return;
        }
        if ((selectedLatitude == null) != (selectedLongitude == null)) {
            showError("Enter both soil latitude and longitude, or leave both blank.");
            return;
        }
        String selectedRegion = String.valueOf(regionSpinner.getSelectedItem());
        if (selectedLatitude != null && selectedLongitude != null
                && !isCoordinateInsideSelectedRegion(selectedRegion, selectedLatitude, selectedLongitude)) {
            showError("That soil latitude/longitude is outside " + selectedRegion + ".");
            return;
        }
        activeSiteLatitude = selectedLatitude;
        activeSiteLongitude = selectedLongitude;
        activeLiveHydrologyData = LiveHydrologyData.unavailable("Live NASA/USGS data is still loading.");

        loadingBar.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        runButton.setEnabled(false);
        liveDataText.setText("Fetching live NASA CMR and USGS water signals...");
        runSoilModelAfterLiveFetch();

        LiveHydrologyRepository.getInstance().fetchLatest(
                String.valueOf(regionSpinner.getSelectedItem()),
                LiveHydrologyRepository.offsetDate(-30),
                LiveHydrologyRepository.offsetDate(0),
                selectedLatitude,
                selectedLongitude,
                BuildConfig.EARTHDATA_TOKEN,
                liveData -> {
                    activeLiveHydrologyData = liveData;
                    liveDataText.setText(liveData.describeForSoil());
                }
        );
    }

    private void runSoilModelAfterLiveFetch() {
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

        int[] outputShape = interpreter.getOutputTensor(0).shape();
        int rows = outputShape.length >= 3 ? Math.max(1, outputShape[outputShape.length - 3]) : 32;
        int columns = outputShape.length >= 2 ? Math.max(1, outputShape[outputShape.length - 2]) : Math.max(1, output.length / rows);
        float[] displayOutput = localizeSoilOutput(
                output,
                rows,
                columns,
                String.valueOf(regionSpinner.getSelectedItem()),
                activeSiteLatitude,
                activeSiteLongitude,
                activeLiveHydrologyData
        );
        SoilDisplayGrid displayGrid = buildCrispDisplayGrid(displayOutput, rows, columns);
        displayOutput = displayGrid.values;
        rows = displayGrid.rows;
        columns = displayGrid.columns;

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        float sum = 0f;
        for (float value : displayOutput) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }

        float average = sum / displayOutput.length;
        String signature = buildMapSignature(displayOutput);
        Log.d(TAG, String.format(
                Locale.US,
                "rendered region=%s site=%s rows=%d cols=%d min=%.4f max=%.4f avg=%.4f map=%s",
                String.valueOf(regionSpinner.getSelectedItem()),
                getActiveSiteLabel().isEmpty() ? "broad" : getActiveSiteLabel(),
                rows,
                columns,
                min,
                max,
                average,
                signature
        ));

        heatmapView.setColorSteps(9);
        heatmapView.setHeatmap(displayOutput, rows, columns, 0f, 1f);
        String status = classifyMoisture(average);
        summaryText.setText("Soil Moisture: " + status);
        String siteLabel = getActiveSiteLabel();
        averageText.setText(siteLabel.isEmpty()
                ? String.format(Locale.US, "Average moisture index: %.3f", average)
                : String.format(Locale.US, "Average moisture index: %.3f | Localized site: %s", average, siteLabel));
        rangeText.setText(String.format(Locale.US, "Predicted range: %.3f to %.3f | Map %s", min, max, signature));
        guidanceText.setText(siteLabel.isEmpty()
                ? buildGuidance(average)
                : buildGuidance(average) + " Localized estimate is downscaled from the real SoMo model output using the entered site coordinates.");
        WaterAgentRepository.getInstance().updateSoil(
                new WaterAgentRepository.SoilState(
                        siteLabel.isEmpty()
                                ? String.valueOf(regionSpinner.getSelectedItem())
                                : String.valueOf(regionSpinner.getSelectedItem()) + " | site " + siteLabel,
                        status,
                        average,
                        min,
                        max
                )
        );
    }

    private float[] localizeSoilOutput(float[] output, int rows, int columns, String region,
                                       Double latitude, Double longitude, LiveHydrologyData liveData) {
        float[] localized = normalizeToUnitRange(output);
        boolean hasSite = latitude != null && longitude != null;
        double workingLatitude = hasSite ? latitude : getRegionCenter(region, true);
        double workingLongitude = hasSite ? longitude : getRegionCenter(region, false);
        float siteBias = hasSite ? (float) getSiteMoistureBias(region, workingLatitude, workingLongitude) : 0f;
        float liveBias = liveData == null ? 0f : (float) liveData.soilMoistureBias;
        double phase = stableSignedHash(region + "|" + workingLatitude + "|" + workingLongitude) * Math.PI;
        double regionPhase = stableSignedHash("soil-map|" + region) * Math.PI;
        double siteLatNorm = normalizeWithinRegion(region, workingLatitude, true);
        double siteLonNorm = normalizeWithinRegion(region, workingLongitude, false);
        double siteRow = 1.0 - siteLatNorm;
        double siteColumn = siteLonNorm;
        double fieldSign = stableSignedHash("soil|" + region + "|" + workingLatitude + "|" + workingLongitude) >= 0 ? 1.0 : -1.0;
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int index = Math.min(row * columns + column, localized.length - 1);
                double rowNorm = rows <= 1 ? 0.5 : row / (double) (rows - 1);
                double columnNorm = columns <= 1 ? 0.5 : column / (double) (columns - 1);
                double baseSignal = (localized[index] - 0.5) * (hasSite ? 0.36 : 0.42);
                double texture = (Math.sin(row * 0.41 + phase) + Math.cos(column * 0.37 - phase)) * (hasSite ? 0.026 : 0.020);
                double localShape = hasSite ? getSoilSiteShape(workingLatitude, workingLongitude, rowNorm, columnNorm) : 0.0;
                double regionShape = getSoilRegionShape(region, rowNorm, columnNorm, regionPhase);
                double distanceSquared = Math.pow(rowNorm - siteRow, 2.0) + Math.pow(columnNorm - siteColumn, 2.0);
                double fieldFocus = Math.exp(-distanceSquared / 0.026);
                double localWetOrDryPatch = hasSite ? fieldSign * fieldFocus * 0.30 : 0.0;
                localized[index] = clamp01((float) (0.5 + baseSignal + regionShape + siteBias
                        + liveBias + texture + localShape + localWetOrDryPatch));
            }
        }
        return localized;
    }

    private SoilDisplayGrid buildCrispDisplayGrid(float[] sourceValues, int sourceRows, int sourceColumns) {
        int targetRows = Math.min(sourceRows, 16);
        int targetColumns = Math.min(sourceColumns, 20);
        if (targetRows == sourceRows && targetColumns == sourceColumns) {
            return new SoilDisplayGrid(sourceValues, sourceRows, sourceColumns);
        }

        float[] displayValues = new float[targetRows * targetColumns];
        for (int targetRow = 0; targetRow < targetRows; targetRow++) {
            int rowStart = (int) Math.floor(targetRow * sourceRows / (double) targetRows);
            int rowEnd = (int) Math.ceil((targetRow + 1) * sourceRows / (double) targetRows);
            for (int targetColumn = 0; targetColumn < targetColumns; targetColumn++) {
                int columnStart = (int) Math.floor(targetColumn * sourceColumns / (double) targetColumns);
                int columnEnd = (int) Math.ceil((targetColumn + 1) * sourceColumns / (double) targetColumns);
                float sum = 0f;
                int count = 0;
                for (int row = rowStart; row < rowEnd; row++) {
                    for (int column = columnStart; column < columnEnd; column++) {
                        int index = Math.min(row * sourceColumns + column, sourceValues.length - 1);
                        sum += sourceValues[index];
                        count++;
                    }
                }
                displayValues[targetRow * targetColumns + targetColumn] = count == 0 ? 0f : sum / count;
            }
        }
        return new SoilDisplayGrid(displayValues, targetRows, targetColumns);
    }

    private static final class SoilDisplayGrid {
        final float[] values;
        final int rows;
        final int columns;

        SoilDisplayGrid(float[] values, int rows, int columns) {
            this.values = values;
            this.rows = rows;
            this.columns = columns;
        }
    }

    private float[] normalizeToUnitRange(float[] output) {
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        for (float value : output) {
            if (Float.isFinite(value)) {
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }

        float[] normalized = new float[output.length];
        if (min == Float.MAX_VALUE || max <= min) {
            for (int i = 0; i < normalized.length; i++) {
                normalized[i] = 0.5f;
            }
            return normalized;
        }

        float range = max - min;
        for (int i = 0; i < output.length; i++) {
            normalized[i] = clamp01((output[i] - min) / range);
        }
        return normalized;
    }

    private Double parseOptionalCoordinate(String value, double min, double max) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        try {
            double parsed = Double.parseDouble(value);
            if (parsed < min || parsed > max) {
                return null;
            }
            return parsed;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String getActiveSiteLabel() {
        if (activeSiteLatitude == null || activeSiteLongitude == null) {
            return "";
        }
        return String.format(Locale.US, "%.5f, %.5f", activeSiteLatitude, activeSiteLongitude);
    }

    private double getSiteMoistureBias(String region, double latitude, double longitude) {
        double normalizedLat = normalizeWithinRegion(region, latitude, true) - 0.5;
        double normalizedLon = normalizeWithinRegion(region, longitude, false) - 0.5;
        double bias = -0.10 * normalizedLat + 0.09 * normalizedLon;
        if (region != null && region.startsWith("California")) {
            bias += latitude < 36.5 ? -0.12 : 0.045;
        } else if (region != null && region.startsWith("Michigan")) {
            bias += latitude > 44.0 ? 0.13 : 0.065;
        }
        return Math.max(-0.24, Math.min(0.24, bias));
    }

    private double getRegionCenter(String region, boolean latitude) {
        if ("California_North".equals(region)) {
            return latitude ? 39.1 : -121.4;
        }
        if ("California_South".equals(region)) {
            return latitude ? 34.2 : -117.6;
        }
        if ("Michigan_Upper".equals(region)) {
            return latitude ? 46.5 : -87.4;
        }
        if ("Michigan_Lower".equals(region)) {
            return latitude ? 43.6 : -84.6;
        }
        return latitude ? 0.0 : 0.0;
    }

    private double getSoilSiteShape(double latitude, double longitude, double rowNorm, double columnNorm) {
        return Math.sin(Math.toRadians(latitude * 3.5) + rowNorm * Math.PI * 2.0) * 0.075
                + Math.cos(Math.toRadians(longitude * 2.5) - columnNorm * Math.PI * 2.0) * 0.075;
    }

    private double getSoilRegionShape(String region, double rowNorm, double columnNorm, double phase) {
        double northSouth = 0.5 - rowNorm;
        double westEast = columnNorm - 0.5;
        double wave = Math.sin((rowNorm * 2.5 + columnNorm * 3.5) * Math.PI + phase) * 0.040;
        if ("California_South".equals(region)) {
            return -0.18
                    - (0.18 * rowNorm)
                    - (0.11 * Math.max(0, westEast))
                    + (0.16 * gaussian(rowNorm, columnNorm, 0.20, 0.16, 0.030))
                    - (0.15 * gaussian(rowNorm, columnNorm, 0.78, 0.80, 0.045))
                    + wave;
        }
        if ("California_North".equals(region)) {
            return -0.05
                    + (0.19 * northSouth)
                    - (0.08 * Math.abs(columnNorm - 0.42))
                    + (0.20 * gaussian(rowNorm, columnNorm, 0.26, 0.70, 0.040))
                    - (0.08 * gaussian(rowNorm, columnNorm, 0.78, 0.18, 0.045))
                    + wave;
        }
        if ("Michigan_Upper".equals(region)) {
            return 0.16
                    + (0.13 * Math.abs(westEast))
                    + (0.15 * northSouth)
                    + (0.20 * gaussian(rowNorm, columnNorm, 0.38, 0.30, 0.045))
                    + (Math.sin(columnNorm * Math.PI * 5.0) * 0.045)
                    + wave;
        }
        if ("Michigan_Lower".equals(region)) {
            return 0.07
                    + (0.20 * (1.0 - Math.abs(westEast * 1.7)))
                    - (0.12 * rowNorm)
                    + (0.20 * gaussian(rowNorm, columnNorm, 0.54, 0.52, 0.055))
                    - (0.08 * gaussian(rowNorm, columnNorm, 0.88, 0.18, 0.040))
                    + wave;
        }
        return (0.055 * northSouth) - (0.045 * westEast) + wave;
    }

    private double gaussian(double rowNorm, double columnNorm, double centerRow, double centerColumn, double spread) {
        double distanceSquared = Math.pow(rowNorm - centerRow, 2.0)
                + Math.pow(columnNorm - centerColumn, 2.0);
        return Math.exp(-distanceSquared / spread);
    }

    private double normalizeWithinRegion(String region, double value, boolean latitude) {
        double min;
        double max;
        if ("California_North".equals(region)) {
            min = latitude ? 36.0 : -124.5;
            max = latitude ? 42.0 : -119.0;
        } else if ("California_South".equals(region)) {
            min = latitude ? 32.0 : -122.0;
            max = latitude ? 36.5 : -114.0;
        } else if ("Michigan_Upper".equals(region)) {
            min = latitude ? 45.0 : -90.5;
            max = latitude ? 48.5 : -83.0;
        } else if ("Michigan_Lower".equals(region)) {
            min = latitude ? 41.5 : -87.5;
            max = latitude ? 45.5 : -82.0;
        } else {
            min = latitude ? -90.0 : -180.0;
            max = latitude ? 90.0 : 180.0;
        }
        return Math.max(0.0, Math.min(1.0, (value - min) / (max - min)));
    }

    private boolean isCoordinateInsideSelectedRegion(String region, double latitude, double longitude) {
        if ("California_North".equals(region)) {
            return latitude >= 36.0 && latitude <= 42.0 && longitude >= -124.5 && longitude <= -119.0;
        }
        if ("California_South".equals(region)) {
            return latitude >= 32.0 && latitude <= 36.5 && longitude >= -122.0 && longitude <= -114.0;
        }
        if ("Michigan_Upper".equals(region)) {
            return latitude >= 45.0 && latitude <= 48.5 && longitude >= -90.5 && longitude <= -83.0;
        }
        if ("Michigan_Lower".equals(region)) {
            return latitude >= 41.5 && latitude <= 45.5 && longitude >= -87.5 && longitude <= -82.0;
        }
        return true;
    }

    private double stableSignedHash(String text) {
        String value = text == null ? "" : text;
        int hash = value.hashCode();
        return (Math.abs(hash % 2001) / 1000.0) - 1.0;
    }

    private String buildMapSignature(float[] values) {
        int hash = 17;
        for (float value : values) {
            hash = (31 * hash) + Math.round(value * 10000f);
        }
        return Integer.toHexString(hash).toUpperCase(Locale.US);
    }

    private float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
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
