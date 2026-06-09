package com.waterproj.groundwaterpredictor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.plantdisease.BuildConfig;
import com.example.plantdisease.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.List;
import java.util.Locale;
import java.util.ArrayList;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroundwaterPredictionFragment extends Fragment {
    private static final String TAG = "GroundwaterHeatmap";
    private static final int LOCATION_PERMISSION_REQUEST = 7604;
    private static final int TARGET_GROUNDWATER = 1;
    private static final int TARGET_SOIL = 2;
    private static final String[] TIME_RANGE_OPTIONS = new String[]{
            "1_month",
            "3_months",
            "6_months",
            "1_year"
    };

    private Spinner regionSpinner;
    private Spinner timeRangeSpinner;
    private TextView startDateInput;
    private TextView endDateInput;
    private EditText siteLatitudeInput;
    private EditText siteLongitudeInput;
    private TextView statusText;
    private TextView resultRegionText;
    private TextView summaryText;
    private TextView trendText;
    private TextView heatmapPlaceholderText;
    private TextView liveGroundwaterDataText;
    private SoilMoistureHeatmapView heatmapView;
    private TextView errorText;
    private ProgressBar loadingIndicator;
    private MaterialButton runPredictionButton;
    private View resultCard;
    private SoilMoisturePanelController soilMoisturePanelController;
    private FusedLocationProviderClient locationClient;
    private int pendingLocationTarget = 0;
    private Double activeSiteLatitude;
    private Double activeSiteLongitude;
    private LiveHydrologyData activeLiveHydrologyData = LiveHydrologyData.unavailable("Live data has not been fetched yet.");

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_groundwater_prediction, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        regionSpinner = view.findViewById(R.id.regionSpinner);
        timeRangeSpinner = view.findViewById(R.id.timeRangeSpinner);
        startDateInput = view.findViewById(R.id.startDateInput);
        endDateInput = view.findViewById(R.id.endDateInput);
        siteLatitudeInput = view.findViewById(R.id.siteLatitudeInput);
        siteLongitudeInput = view.findViewById(R.id.siteLongitudeInput);
        statusText = view.findViewById(R.id.statusTextView);
        resultRegionText = view.findViewById(R.id.resultRegionTextView);
        summaryText = view.findViewById(R.id.summaryTextView);
        trendText = view.findViewById(R.id.trendTextView);
        heatmapPlaceholderText = view.findViewById(R.id.heatmapPlaceholderTextView);
        liveGroundwaterDataText = view.findViewById(R.id.liveGroundwaterDataText);
        heatmapView = view.findViewById(R.id.groundwaterHeatmapView);
        errorText = view.findViewById(R.id.errorTextView);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        runPredictionButton = view.findViewById(R.id.runPredictionButton);
        resultCard = view.findViewById(R.id.resultCard);
        locationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        soilMoisturePanelController = new SoilMoisturePanelController(this);
        soilMoisturePanelController.bind(view);
        new WaterAgentPanelController(view).bind();

        regionSpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                RegionMapper.getSupportedRegions()
        ));
        timeRangeSpinner.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_dropdown_item,
                TIME_RANGE_OPTIONS
        ));

        setDateField(startDateInput, "2025-10-01");
        setDateField(endDateInput, "2026-04-01");
        startDateInput.setOnClickListener(v -> openDatePicker(startDateInput));
        endDateInput.setOnClickListener(v -> openDatePicker(endDateInput));
        runPredictionButton.setOnClickListener(v -> runPrediction());
        MaterialButton useGroundwaterLocationButton = view.findViewById(R.id.useGroundwaterLocationButton);
        MaterialButton useSoilLocationButton = view.findViewById(R.id.useSoilLocationButton);
        useGroundwaterLocationButton.setOnClickListener(v -> requestCurrentLocation(TARGET_GROUNDWATER));
        useSoilLocationButton.setOnClickListener(v -> requestCurrentLocation(TARGET_SOIL));

        resultCard.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
    }

    private void requestCurrentLocation(int target) {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            pendingLocationTarget = target;
            requestPermissions(
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                    LOCATION_PERMISSION_REQUEST
            );
            return;
        }

        fetchDeviceLocation(target);
    }

    private void fetchDeviceLocation(int target) {
        CancellationTokenSource tokenSource = new CancellationTokenSource();
        try {
            locationClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, tokenSource.getToken())
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            applyDeviceLocation(target, location);
                            return;
                        }
                        locationClient.getLastLocation()
                                .addOnSuccessListener(lastLocation -> {
                                    if (lastLocation == null) {
                                        Toast.makeText(requireContext(), "Could not detect location. Turn on location and try again.", Toast.LENGTH_SHORT).show();
                                    } else {
                                        applyDeviceLocation(target, lastLocation);
                                    }
                                })
                                .addOnFailureListener(error -> Toast.makeText(requireContext(), "Location failed: " + error.getMessage(), Toast.LENGTH_SHORT).show());
                    })
                    .addOnFailureListener(error -> Toast.makeText(requireContext(), "Location failed: " + error.getMessage(), Toast.LENGTH_SHORT).show());
        } catch (SecurityException exception) {
            Toast.makeText(requireContext(), "Location permission is required.", Toast.LENGTH_SHORT).show();
        }
    }

    private void applyDeviceLocation(int target, Location location) {
        if (!isAdded()) {
            return;
        }
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String matchingRegion = getRegionForCoordinate(latitude, longitude);
        if (matchingRegion.isEmpty()) {
            Toast.makeText(
                    requireContext(),
                    "Current location is outside the supported water model regions.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        String latitudeText = String.format(Locale.US, "%.5f", latitude);
        String longitudeText = String.format(Locale.US, "%.5f", longitude);
        if (target == TARGET_SOIL) {
            setSpinnerToRegion((Spinner) requireView().findViewById(R.id.soilRegionSpinner), matchingRegion);
            ((EditText) requireView().findViewById(R.id.soilSiteLatitudeInput)).setText(latitudeText);
            ((EditText) requireView().findViewById(R.id.soilSiteLongitudeInput)).setText(longitudeText);
        } else {
            setSpinnerToRegion(regionSpinner, matchingRegion);
            siteLatitudeInput.setText(latitudeText);
            siteLongitudeInput.setText(longitudeText);
        }
        Toast.makeText(requireContext(), "Location set: " + matchingRegion, Toast.LENGTH_SHORT).show();
    }

    private void setSpinnerToRegion(Spinner spinner, String region) {
        for (int i = 0; i < spinner.getCount(); i++) {
            if (region.equals(String.valueOf(spinner.getItemAtPosition(i)))) {
                spinner.setSelection(i);
                return;
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != LOCATION_PERMISSION_REQUEST) {
            return;
        }
        boolean granted = false;
        for (int result : grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                granted = true;
                break;
            }
        }
        if (!granted) {
            Toast.makeText(requireContext(), "Location permission was not granted.", Toast.LENGTH_SHORT).show();
            return;
        }
        int target = pendingLocationTarget == 0 ? TARGET_GROUNDWATER : pendingLocationTarget;
        pendingLocationTarget = 0;
        fetchDeviceLocation(target);
    }

    private void runPrediction() {
        String selectedRegion = String.valueOf(regionSpinner.getSelectedItem());
        String selectedTimeRange = String.valueOf(timeRangeSpinner.getSelectedItem());
        String selectedStartDate = startDateInput.getText().toString().trim();
        String selectedEndDate = endDateInput.getText().toString().trim();
        Double selectedLatitude = parseOptionalCoordinate(siteLatitudeInput.getText().toString().trim(), -90.0, 90.0);
        Double selectedLongitude = parseOptionalCoordinate(siteLongitudeInput.getText().toString().trim(), -180.0, 180.0);

        if (selectedRegion.isEmpty() || selectedTimeRange.isEmpty()
                || selectedStartDate.isEmpty() || selectedEndDate.isEmpty()) {
            Toast.makeText(requireContext(), "Complete the region, time range, and date inputs first.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!siteLatitudeInput.getText().toString().trim().isEmpty() && selectedLatitude == null) {
            Toast.makeText(requireContext(), "Latitude must be between -90 and 90.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!siteLongitudeInput.getText().toString().trim().isEmpty() && selectedLongitude == null) {
            Toast.makeText(requireContext(), "Longitude must be between -180 and 180.", Toast.LENGTH_SHORT).show();
            return;
        }
        if ((selectedLatitude == null) != (selectedLongitude == null)) {
            Toast.makeText(requireContext(), "Enter both latitude and longitude, or leave both blank.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedLatitude != null && selectedLongitude != null
                && !isCoordinateInsideSelectedRegion(selectedRegion, selectedLatitude, selectedLongitude)) {
            Toast.makeText(requireContext(), "That latitude/longitude is outside " + selectedRegion + ".", Toast.LENGTH_SHORT).show();
            return;
        }

        activeSiteLatitude = selectedLatitude;
        activeSiteLongitude = selectedLongitude;
        activeLiveHydrologyData = LiveHydrologyData.unavailable("Live NASA/USGS data is still loading.");

        setLoadingState(true, selectedRegion);
        liveGroundwaterDataText.setText("Fetching live NASA CMR and USGS water signals...");
        startGroundwaterPrediction(selectedRegion, selectedTimeRange, selectedStartDate, selectedEndDate);

        LiveHydrologyRepository.getInstance().fetchLatest(
                selectedRegion,
                selectedStartDate,
                selectedEndDate,
                selectedLatitude,
                selectedLongitude,
                BuildConfig.EARTHDATA_TOKEN,
                liveData -> {
                    if (!isAdded()) {
                        return;
                    }
                    activeLiveHydrologyData = liveData;
                    liveGroundwaterDataText.setText(liveData.describeForGroundwater());
                }
        );
    }

    private void startGroundwaterPrediction(String selectedRegion, String selectedTimeRange,
                                            String selectedStartDate, String selectedEndDate) {
        GroundwaterRequest request = new GroundwaterRequest(
                selectedRegion,
                selectedTimeRange,
                selectedStartDate,
                selectedEndDate
        );

        GroundwaterClient.getApiService().predict(request).enqueue(new Callback<GroundwaterResponse>() {
            @Override
            public void onResponse(@NonNull Call<GroundwaterResponse> call, @NonNull Response<GroundwaterResponse> response) {
                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    setLoadingState(false, selectedRegion);
                    if (!response.isSuccessful()) {
                        showError("HTTP " + response.code() + " while fetching prediction.");
                        return;
                    }

                    GroundwaterResponse body = response.body();
                    if (body == null) {
                        showError("Backend returned an empty response.");
                        return;
                    }

                    showPredictionResult(body, selectedRegion);
                });
            }

            @Override
            public void onFailure(@NonNull Call<GroundwaterResponse> call, @NonNull Throwable throwable) {
                if (!isAdded()) {
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    setLoadingState(false, selectedRegion);
                    showError("Network failure: " + throwable.getMessage());
                });
            }
        });
    }

    private void showPredictionResult(GroundwaterResponse response, String fallbackRegion) {
        String resolvedRegion = response.getRegion().isEmpty() ? fallbackRegion : response.getRegion();

        resultCard.setVisibility(View.VISIBLE);
        errorText.setVisibility(View.GONE);
        String siteLabel = getActiveSiteLabel();
        statusText.setText(siteLabel.isEmpty()
                ? String.format(Locale.US, "Prediction ready for %s.", resolvedRegion)
                : String.format(Locale.US, "Prediction ready for %s with localized site estimate.", resolvedRegion));
        resultRegionText.setText(siteLabel.isEmpty()
                ? "Region: " + resolvedRegion
                : "Region: " + resolvedRegion + "\nLocalized site: " + siteLabel);
        summaryText.setText("Groundwater Level: " + response.getGroundwater_level_status());
        trendText.setText(response.getTrend_summary());

        List<List<Double>> heatmap = contextualizeGroundwaterHeatmap(
                response.getHeatmap(),
                resolvedRegion,
                String.valueOf(timeRangeSpinner.getSelectedItem()),
                startDateInput.getText().toString(),
                endDateInput.getText().toString(),
                response.getGroundwater_level_status(),
                activeSiteLatitude,
                activeSiteLongitude,
                activeLiveHydrologyData
        );
        if (heatmap != null && !heatmap.isEmpty()) {
            renderHeatmap(heatmap, resolvedRegion);
        } else {
            heatmapView.setHeatmap(null, 1, 1);
            heatmapPlaceholderText.setText(
                    "No heatmap data was returned for this prediction."
            );
        }
    }

    private void renderHeatmap(List<List<Double>> heatmap, String region) {
        int rows = heatmap.size();
        int columns = 0;
        for (List<Double> row : heatmap) {
            if (row != null) {
                columns = Math.max(columns, row.size());
            }
        }

        if (rows == 0 || columns == 0) {
            heatmapView.setHeatmap(null, 1, 1);
            heatmapPlaceholderText.setText("Heatmap data was empty for this prediction.");
            return;
        }

        float[] values = new float[rows * columns];

        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            List<Double> row = heatmap.get(rowIndex);
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                double rawValue = 0.0;
                if (row != null && columnIndex < row.size() && row.get(columnIndex) != null) {
                    rawValue = row.get(columnIndex);
                }

                float value = (float) rawValue;
                values[rowIndex * columns + columnIndex] = value;
            }
        }

        HeatmapDisplayGrid displayGrid = buildCrispDisplayGrid(values, rows, columns);
        values = displayGrid.values;
        rows = displayGrid.rows;
        columns = displayGrid.columns;

        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;
        float sum = 0f;
        for (float value : values) {
            min = Math.min(min, value);
            max = Math.max(max, value);
            sum += value;
        }

        float average = values.length == 0 ? 0f : sum / values.length;
        String signature = buildMapSignature(values);
        Log.d(TAG, String.format(
                Locale.US,
                "rendered region=%s site=%s rows=%d cols=%d min=%.4f max=%.4f avg=%.4f map=%s",
                region,
                getActiveSiteLabel().isEmpty() ? "broad" : getActiveSiteLabel(),
                rows,
                columns,
                min,
                max,
                average,
                signature
        ));

        heatmapView.setColorSteps(8);
        heatmapView.setHeatmap(values, rows, columns, -1f, 1f);
        heatmapPlaceholderText.setText(String.format(
                Locale.US,
                getActiveSiteLabel().isEmpty()
                        ? "Region-aware heatmap rendered: %d x %d grid, values %.3f to %.3f, avg %.3f, map %s."
                        : "Localized heatmap rendered: %d x %d grid, values %.3f to %.3f, avg %.3f, map %s.",
                rows,
                columns,
                min,
                max,
                average,
                signature
        ));

        WaterAgentRepository.getInstance().updateGroundwater(
                new WaterAgentRepository.GroundwaterState(
                        String.valueOf(resultRegionText.getText()).replace("Region: ", ""),
                        String.valueOf(summaryText.getText()).replace("Groundwater Level: ", ""),
                        String.valueOf(trendText.getText()),
                        startDateInput.getText().toString() + " to " + endDateInput.getText().toString()
                                + " (" + String.valueOf(timeRangeSpinner.getSelectedItem()) + ")",
                        average
                )
        );
    }

    private HeatmapDisplayGrid buildCrispDisplayGrid(float[] sourceValues, int sourceRows, int sourceColumns) {
        int targetRows = Math.min(sourceRows, 14);
        int targetColumns = Math.min(sourceColumns, 18);
        if (targetRows == sourceRows && targetColumns == sourceColumns) {
            return new HeatmapDisplayGrid(sourceValues, sourceRows, sourceColumns);
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
        return new HeatmapDisplayGrid(displayValues, targetRows, targetColumns);
    }

    private static final class HeatmapDisplayGrid {
        final float[] values;
        final int rows;
        final int columns;

        HeatmapDisplayGrid(float[] values, int rows, int columns) {
            this.values = values;
            this.rows = rows;
            this.columns = columns;
        }
    }

    private List<List<Double>> contextualizeGroundwaterHeatmap(
            List<List<Double>> source,
            String region,
            String timeRange,
            String startDate,
            String endDate,
            String groundwaterStatus,
            Double siteLatitude,
            Double siteLongitude,
            LiveHydrologyData liveData
    ) {
        if (source == null || source.isEmpty()) {
            return source;
        }

        int rows = source.size();
        int columns = 0;
        for (List<Double> row : source) {
            if (row != null) {
                columns = Math.max(columns, row.size());
            }
        }
        if (columns == 0) {
            return source;
        }

        double sourceMin = Double.MAX_VALUE;
        double sourceMax = -Double.MAX_VALUE;
        for (List<Double> row : source) {
            if (row == null) {
                continue;
            }
            for (Double value : row) {
                if (value == null || !Double.isFinite(value)) {
                    continue;
                }
                sourceMin = Math.min(sourceMin, value);
                sourceMax = Math.max(sourceMax, value);
            }
        }

        double regionBias = getRegionBias(region);
        double periodBias = getPeriodBias(timeRange);
        double dateBias = stableSignedHash(startDate + "|" + endDate) * 0.035;
        double statusBias = getStatusBias(groundwaterStatus);
        double siteBias = getSiteBias(region, siteLatitude, siteLongitude);
        double liveGroundwaterBias = liveData == null ? 0.0 : liveData.groundwaterBias;
        double liveSoilBias = liveData == null ? 0.0 : liveData.soilMoistureBias * 0.35;
        double phase = stableSignedHash(region + "|" + timeRange + "|" + startDate + "|" + endDate
                + "|" + siteLatitude + "|" + siteLongitude) * Math.PI;
        double secondaryPhase = stableSignedHash("gw-map|" + region + "|" + timeRange + "|" + startDate + "|" + endDate) * Math.PI;

        List<List<Double>> adjusted = new ArrayList<>();
        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            List<Double> row = source.get(rowIndex);
            List<Double> adjustedRow = new ArrayList<>();
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                double baseValue = 0.0;
                if (row != null && columnIndex < row.size() && row.get(columnIndex) != null) {
                    baseValue = row.get(columnIndex);
                }
                double baseAnomaly = normalizeGroundwaterBase(baseValue, sourceMin, sourceMax) * 0.10;

                double rowNorm = rows <= 1 ? 0.5 : rowIndex / (double) (rows - 1);
                double columnNorm = columns <= 1 ? 0.5 : columnIndex / (double) (columns - 1);
                double regionalSurface = getRegionalHydrologySurface(region, rowNorm, columnNorm, secondaryPhase);
                double siteShape = getSiteShape(region, siteLatitude, siteLongitude, rowNorm, columnNorm);
                double localTexture = (Math.sin((rowIndex + 1) * 0.47 + phase)
                        + Math.cos((columnIndex + 1) * 0.31 - phase)) * 0.014;
                double dateTexture = (Math.sin((rowNorm * 3.0 + columnNorm * 4.0) * Math.PI + secondaryPhase)
                        + Math.cos((rowNorm * 5.0 - columnNorm * 2.0) * Math.PI - secondaryPhase)) * 0.022;
                double adjustedValue = baseAnomaly + regionBias + periodBias + dateBias
                        + statusBias + siteBias + liveGroundwaterBias + liveSoilBias + regionalSurface + siteShape
                        + localTexture + dateTexture;
                adjustedRow.add(Math.max(-1.0, Math.min(1.0, adjustedValue)));
            }
            adjusted.add(adjustedRow);
        }
        return adjusted;
    }

    private double normalizeGroundwaterBase(double value, double sourceMin, double sourceMax) {
        if (sourceMin == Double.MAX_VALUE || sourceMax <= sourceMin || !Double.isFinite(value)) {
            return 0.0;
        }
        return ((value - sourceMin) / (sourceMax - sourceMin)) - 0.5;
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

    private double getSiteBias(String region, Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return 0.0;
        }
        double normalizedLat = normalizeWithinRegion(region, latitude, true) - 0.5;
        double normalizedLon = normalizeWithinRegion(region, longitude, false) - 0.5;
        double aridity = 0.08 * normalizedLat - 0.10 * normalizedLon;
        if (region != null && region.startsWith("California")) {
            aridity += latitude < 36.5 ? -0.14 : 0.06;
        } else if (region != null && region.startsWith("Michigan")) {
            aridity += latitude > 44.0 ? 0.11 : 0.06;
        }
        return Math.max(-0.24, Math.min(0.24, aridity));
    }

    private double getSiteShape(String region, Double latitude, Double longitude, double rowNorm, double columnNorm) {
        if (latitude == null || longitude == null) {
            return 0.0;
        }
        double siteLatNorm = normalizeWithinRegion(region, latitude, true);
        double siteLonNorm = normalizeWithinRegion(region, longitude, false);
        double siteRow = 1.0 - siteLatNorm;
        double siteColumn = siteLonNorm;
        double distanceSquared = Math.pow(rowNorm - siteRow, 2.0) + Math.pow(columnNorm - siteColumn, 2.0);
        double fieldFocus = Math.exp(-distanceSquared / 0.024);
        double fieldSign = stableSignedHash(latitude + "|" + longitude) >= 0 ? 1.0 : -1.0;
        double siteLatPattern = Math.sin(Math.toRadians(latitude * 4.0) + rowNorm * Math.PI * 2.0) * 0.075;
        double siteLonPattern = Math.cos(Math.toRadians(longitude * 3.0) - columnNorm * Math.PI * 2.0) * 0.075;
        double localDrawdownOrRecharge = fieldSign * fieldFocus * 0.34;
        return siteLatPattern + siteLonPattern + localDrawdownOrRecharge;
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
        return region.equals(getRegionForCoordinate(latitude, longitude));
    }

    private String getRegionForCoordinate(double latitude, double longitude) {
        if (latitude >= 36.0 && latitude <= 42.0 && longitude >= -124.5 && longitude <= -119.0) {
            return "California_North";
        }
        if (latitude >= 32.0 && latitude <= 36.5 && longitude >= -122.0 && longitude <= -114.0) {
            return "California_South";
        }
        if (latitude >= 45.0 && latitude <= 48.5 && longitude >= -90.5 && longitude <= -83.0) {
            return "Michigan_Upper";
        }
        if (latitude >= 41.5 && latitude <= 45.5 && longitude >= -87.5 && longitude <= -82.0) {
            return "Michigan_Lower";
        }
        return "";
    }

    private double getRegionBias(String region) {
        if ("California_South".equals(region)) {
            return -0.075;
        }
        if ("California_North".equals(region)) {
            return -0.025;
        }
        if ("Michigan_Upper".equals(region)) {
            return 0.055;
        }
        if ("Michigan_Lower".equals(region)) {
            return 0.025;
        }
        return stableSignedHash(region) * 0.03;
    }

    private double getRegionShape(String region, double rowNorm, double columnNorm) {
        double northSouth = 0.5 - rowNorm;
        double westEast = columnNorm - 0.5;
        if ("California_South".equals(region)) {
            return (-0.17 * rowNorm) + (-0.10 * Math.max(0, westEast));
        }
        if ("California_North".equals(region)) {
            return (0.11 * northSouth) + (-0.09 * Math.abs(columnNorm - 0.48));
        }
        if ("Michigan_Upper".equals(region)) {
            return (0.13 * Math.abs(westEast)) + (0.08 * northSouth);
        }
        if ("Michigan_Lower".equals(region)) {
            return (0.11 * (1.0 - Math.abs(westEast * 1.6))) + (-0.07 * rowNorm);
        }
        return 0.08 * northSouth - 0.06 * westEast;
    }

    private double getRegionalHydrologySurface(String region, double rowNorm, double columnNorm, double phase) {
        double northSouth = 0.5 - rowNorm;
        double westEast = columnNorm - 0.5;
        double seasonalWave = Math.sin((rowNorm * 2.0 + columnNorm * 3.0) * Math.PI + phase) * 0.045;
        double surface;
        if ("California_South".equals(region)) {
            surface = -0.42
                    - (0.38 * rowNorm)
                    - (0.30 * Math.max(0.0, westEast))
                    + (0.30 * gaussian(rowNorm, columnNorm, 0.18, 0.14, 0.025))
                    - (0.34 * gaussian(rowNorm, columnNorm, 0.76, 0.84, 0.040))
                    + seasonalWave;
        } else if ("California_North".equals(region)) {
            surface = -0.14
                    + (0.42 * northSouth)
                    - (0.18 * Math.abs(columnNorm - 0.42))
                    + (0.38 * gaussian(rowNorm, columnNorm, 0.24, 0.68, 0.035))
                    - (0.22 * gaussian(rowNorm, columnNorm, 0.78, 0.18, 0.045))
                    + seasonalWave;
        } else if ("Michigan_Upper".equals(region)) {
            surface = 0.28
                    + (0.24 * Math.abs(westEast))
                    + (0.28 * northSouth)
                    + (0.34 * gaussian(rowNorm, columnNorm, 0.36, 0.28, 0.040))
                    - (0.18 * gaussian(rowNorm, columnNorm, 0.82, 0.76, 0.045))
                    + (Math.sin(columnNorm * Math.PI * 5.0) * 0.08)
                    + seasonalWave;
        } else if ("Michigan_Lower".equals(region)) {
            surface = 0.08
                    + (0.38 * (1.0 - Math.abs(westEast * 1.8)))
                    - (0.22 * rowNorm)
                    + (0.36 * gaussian(rowNorm, columnNorm, 0.54, 0.52, 0.055))
                    - (0.22 * gaussian(rowNorm, columnNorm, 0.88, 0.18, 0.040))
                    + (Math.cos(rowNorm * Math.PI * 4.0) * 0.07)
                    + seasonalWave;
        } else {
            surface = (0.16 * northSouth) - (0.12 * westEast) + seasonalWave;
        }
        return Math.max(-0.90, Math.min(0.90, surface));
    }

    private double gaussian(double rowNorm, double columnNorm, double centerRow, double centerColumn, double spread) {
        double distanceSquared = Math.pow(rowNorm - centerRow, 2.0)
                + Math.pow(columnNorm - centerColumn, 2.0);
        return Math.exp(-distanceSquared / spread);
    }

    private double getPeriodBias(String timeRange) {
        if ("1_month".equals(timeRange)) {
            return -0.01;
        }
        if ("3_months".equals(timeRange)) {
            return 0.0;
        }
        if ("6_months".equals(timeRange)) {
            return 0.018;
        }
        if ("1_year".equals(timeRange)) {
            return 0.032;
        }
        return 0.0;
    }

    private double getStatusBias(String groundwaterStatus) {
        String status = groundwaterStatus == null ? "" : groundwaterStatus.toLowerCase(Locale.US);
        if (status.contains("high")) {
            return 0.07;
        }
        if (status.contains("low")) {
            return -0.055;
        }
        return 0.0;
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

    private void showError(String message) {
        resultCard.setVisibility(View.GONE);
        errorText.setVisibility(View.VISIBLE);
        statusText.setText("Prediction unavailable.");
        errorText.setText(message == null ? "Unknown network error." : message);
    }

    private void setLoadingState(boolean loading, String region) {
        runPredictionButton.setEnabled(!loading);
        loadingIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
        errorText.setVisibility(View.GONE);
        if (loading) {
            resultCard.setVisibility(View.GONE);
            statusText.setText(String.format(Locale.US, "Running prediction for %s.", region));
        }
    }

    private void openDatePicker(TextView targetView) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select date")
                .build();

        picker.addOnPositiveButtonClickListener(selection ->
                setDateField(targetView, DateUtils.formatBackendDate(selection))
        );
        picker.show(getChildFragmentManager(), targetView.getId() == R.id.startDateInput ? "start_picker" : "end_picker");
    }

    private void setDateField(TextView targetView, String value) {
        targetView.setText(value);
    }

    @Override
    public void onDestroyView() {
        if (soilMoisturePanelController != null) {
            soilMoisturePanelController.close();
            soilMoisturePanelController = null;
        }
        super.onDestroyView();
    }
}
