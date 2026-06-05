package com.waterproj.groundwaterpredictor;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.plantdisease.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;

import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GroundwaterPredictionFragment extends Fragment {
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
    private TextView statusText;
    private TextView resultRegionText;
    private TextView summaryText;
    private TextView trendText;
    private TextView heatmapPlaceholderText;
    private SoilMoistureHeatmapView heatmapView;
    private TextView errorText;
    private ProgressBar loadingIndicator;
    private MaterialButton runPredictionButton;
    private View resultCard;

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
        statusText = view.findViewById(R.id.statusTextView);
        resultRegionText = view.findViewById(R.id.resultRegionTextView);
        summaryText = view.findViewById(R.id.summaryTextView);
        trendText = view.findViewById(R.id.trendTextView);
        heatmapPlaceholderText = view.findViewById(R.id.heatmapPlaceholderTextView);
        heatmapView = view.findViewById(R.id.groundwaterHeatmapView);
        errorText = view.findViewById(R.id.errorTextView);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        runPredictionButton = view.findViewById(R.id.runPredictionButton);
        resultCard = view.findViewById(R.id.resultCard);

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

        resultCard.setVisibility(View.GONE);
        loadingIndicator.setVisibility(View.GONE);
        errorText.setVisibility(View.GONE);
    }

    private void runPrediction() {
        String selectedRegion = String.valueOf(regionSpinner.getSelectedItem());
        String selectedTimeRange = String.valueOf(timeRangeSpinner.getSelectedItem());
        String selectedStartDate = startDateInput.getText().toString().trim();
        String selectedEndDate = endDateInput.getText().toString().trim();

        if (selectedRegion.isEmpty() || selectedTimeRange.isEmpty()
                || selectedStartDate.isEmpty() || selectedEndDate.isEmpty()) {
            Toast.makeText(requireContext(), "Complete the region, time range, and date inputs first.", Toast.LENGTH_SHORT).show();
            return;
        }

        setLoadingState(true, selectedRegion);

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
        statusText.setText(String.format(Locale.US, "Prediction ready for %s.", resolvedRegion));
        resultRegionText.setText("Region: " + resolvedRegion);
        summaryText.setText("Groundwater Level: " + response.getGroundwater_level_status());
        trendText.setText(response.getTrend_summary());

        List<List<Double>> heatmap = response.getHeatmap();
        if (heatmap != null && !heatmap.isEmpty()) {
            renderHeatmap(heatmap);
        } else {
            heatmapView.setHeatmap(null, 1, 1);
            heatmapPlaceholderText.setText(
                    "No heatmap data was returned for this prediction."
            );
        }
    }

    private void renderHeatmap(List<List<Double>> heatmap) {
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
        float min = Float.MAX_VALUE;
        float max = -Float.MAX_VALUE;

        for (int rowIndex = 0; rowIndex < rows; rowIndex++) {
            List<Double> row = heatmap.get(rowIndex);
            for (int columnIndex = 0; columnIndex < columns; columnIndex++) {
                double rawValue = 0.0;
                if (row != null && columnIndex < row.size() && row.get(columnIndex) != null) {
                    rawValue = row.get(columnIndex);
                }

                float value = (float) rawValue;
                values[rowIndex * columns + columnIndex] = value;
                min = Math.min(min, value);
                max = Math.max(max, value);
            }
        }

        heatmapView.setHeatmap(values, rows, columns);
        heatmapPlaceholderText.setText(String.format(
                Locale.US,
                "Heatmap rendered: %d x %d grid, values %.3f to %.3f.",
                rows,
                columns,
                min,
                max
        ));
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
}
