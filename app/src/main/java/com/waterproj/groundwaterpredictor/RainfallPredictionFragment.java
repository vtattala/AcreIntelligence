package com.waterproj.groundwaterpredictor;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.plantdisease.R;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.google.android.material.textfield.TextInputEditText;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RainfallPredictionFragment extends Fragment {

    private final DecimalFormat percentFormat = new DecimalFormat("0%");
    private final DecimalFormat rainFormat = new DecimalFormat("0.0");
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    private final List<RainfallLocationOption> locations = Arrays.asList(
            new RainfallLocationOption("Stockholm", 59.3293, 18.0686),
            new RainfallLocationOption("Uppsala", 59.8586, 17.6389),
            new RainfallLocationOption("Vasteras", 59.6099, 16.5448),
            new RainfallLocationOption("Norrkoping", 58.5877, 16.1924),
            new RainfallLocationOption("Sodertalje", 59.1955, 17.6253)
    );

    public RainfallPredictionFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rainfall_prediction, container, false);

        MaterialAutoCompleteTextView locationDropdown = view.findViewById(R.id.locationDropdown);
        TextView latValue = view.findViewById(R.id.latValue);
        TextView lonValue = view.findViewById(R.id.lonValue);
        TextInputEditText horizonInput = view.findViewById(R.id.horizonInput);
        TextInputEditText dateInput = view.findViewById(R.id.dateInput);
        Button predictButton = view.findViewById(R.id.predictButton);
        ProgressBar loadingBar = view.findViewById(R.id.loadingBar);
        MaterialCardView resultCard = view.findViewById(R.id.resultCard);
        TextView summaryText = view.findViewById(R.id.summaryText);
        TextView chanceText = view.findViewById(R.id.chanceText);
        TextView amountText = view.findViewById(R.id.amountText);
        TextView riskText = view.findViewById(R.id.riskText);
        TextView helperText = view.findViewById(R.id.helperText);
        TextView versionText = view.findViewById(R.id.versionText);
        new WaterAgentPanelController(view).bind();

        resultCard.setVisibility(View.INVISIBLE);

        ArrayAdapter<RainfallLocationOption> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                locations
        );
        locationDropdown.setAdapter(adapter);
        locationDropdown.setText(locations.get(0).name, false);
        latValue.setText(String.valueOf(locations.get(0).lat));
        lonValue.setText(String.valueOf(locations.get(0).lon));
        horizonInput.setText("72");

        Calendar calendar = Calendar.getInstance();
        dateInput.setText(dateFormat.format(calendar.getTime()));

        locationDropdown.setOnItemClickListener((parent, v, position, id) -> {
            RainfallLocationOption selected = locations.get(position);
            latValue.setText(String.valueOf(selected.lat));
            lonValue.setText(String.valueOf(selected.lon));
            animateSmallCard(resultCard);
        });

        dateInput.setOnClickListener(v -> showDatePicker(dateInput));
        dateInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                showDatePicker(dateInput);
            }
        });

        predictButton.setOnClickListener(v -> {
            RainfallLocationOption selectedLocation = null;
            for (RainfallLocationOption option : locations) {
                if (option.name.equals(locationDropdown.getText().toString().trim())) {
                    selectedLocation = option;
                    break;
                }
            }

            Integer horizon = null;
            try {
                horizon = Integer.parseInt(String.valueOf(horizonInput.getText()).trim());
            } catch (Exception ignored) {
            }

            String date = String.valueOf(dateInput.getText()).trim();

            if (selectedLocation == null || horizon == null || date.isBlank()) {
                Toast.makeText(requireContext(), "Choose a location, date, and horizon.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isDateWithinForecastWindow(date)) {
                summaryText.setText("Pick a closer date.");
                chanceText.setText("This weather source usually supports forecasts up to about 16 days ahead.");
                amountText.setText("");
                riskText.setText("");
                helperText.setText("Choose today or a date in the next 16 days.");
                versionText.setText("");
                showResultCard(resultCard);
                return;
            }

            setLoading(true, predictButton, loadingBar);
            RainfallLocationOption finalSelectedLocation = selectedLocation;

            RainfallRequest request = new RainfallRequest(
                    finalSelectedLocation.name,
                    finalSelectedLocation.lat,
                    finalSelectedLocation.lon,
                    horizon,
                    date
            );

            RainfallClient.getApiService().predictRainfall(request).enqueue(new Callback<RainfallResponse>() {
                @Override
                public void onResponse(@NonNull Call<RainfallResponse> call,
                                       @NonNull Response<RainfallResponse> response) {
                    setLoading(false, predictButton, loadingBar);

                    if (response.isSuccessful() && response.body() != null) {
                        RainfallResponse body = response.body();
                        double rainChance = Math.max(0.0, Math.min(1.0, body.getRain_probability_24h()));
                        double extremeRisk = Math.max(0.0, Math.min(1.0, body.getExtreme_rain_risk()));

                        summaryText.setText(buildSummary(rainChance, body.getRainfall_mm_p50_24h()));
                        chanceText.setText("Chance of rain: " + percentFormat.format(rainChance));
                        amountText.setText("Likely rainfall: " + rainFormat.format(body.getRainfall_mm_p50_24h()) + " mm");
                        riskText.setText("Heavy rain risk: " + percentFormat.format(extremeRisk));
                        helperText.setText(buildHelperLine(rainChance, body.getRainfall_mm_p90_24h()));
                        versionText.setText("Model: " + body.getModel_version());
                        WaterAgentRepository.getInstance().updateRainfall(
                                new WaterAgentRepository.RainfallState(
                                        finalSelectedLocation.name,
                                        rainChance,
                                        body.getRainfall_mm_p50_24h(),
                                        body.getRainfall_mm_p90_24h(),
                                        extremeRisk
                                )
                        );

                        showResultCard(resultCard);
                    } else {
                        summaryText.setText("We could not get a prediction.");
                        chanceText.setText("Try a date closer to today.");
                        amountText.setText("");
                        riskText.setText("");
                        helperText.setText("Also make sure your backend URL is correct.");
                        versionText.setText("HTTP " + response.code());
                        showResultCard(resultCard);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<RainfallResponse> call, @NonNull Throwable t) {
                    setLoading(false, predictButton, loadingBar);
                    summaryText.setText("Connection problem.");
                    chanceText.setText("Please check the internet or backend URL.");
                    amountText.setText("");
                    riskText.setText("");
                    helperText.setText("If the backend is sleeping, wait a few seconds and try again.");
                    versionText.setText(t.getMessage() != null ? t.getMessage() : "Unknown error");
                    showResultCard(resultCard);
                }
            });
        });

        return view;
    }

    private void showDatePicker(TextInputEditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog picker = new DatePickerDialog(
                requireContext(),
                (datePicker, year, month, dayOfMonth) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, dayOfMonth);
                    target.setText(dateFormat.format(selected.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        picker.show();
    }

    private boolean isDateWithinForecastWindow(String dateText) {
        try {
            Calendar selectedDate = Calendar.getInstance();
            selectedDate.setTime(dateFormat.parse(dateText));

            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar maxDate = (Calendar) today.clone();
            maxDate.add(Calendar.DAY_OF_YEAR, 16);

            return !selectedDate.before(today) && !selectedDate.after(maxDate);
        } catch (Exception e) {
            return false;
        }
    }

    private void setLoading(boolean isLoading, Button button, ProgressBar loadingBar) {
        button.setEnabled(!isLoading);
        button.setText(isLoading ? "Loading..." : "Predict Rainfall");
        loadingBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showResultCard(MaterialCardView card) {
        if (card.getVisibility() != View.VISIBLE) {
            card.setAlpha(0f);
            card.setTranslationY(40f);
            card.setVisibility(View.VISIBLE);
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(260)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        } else {
            animateSmallCard(card);
        }
    }

    private void animateSmallCard(View card) {
        card.animate()
                .scaleX(1.01f)
                .scaleY(1.01f)
                .setDuration(120)
                .withEndAction(() ->
                        card.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
                )
                .start();
    }

    private String buildSummary(double rainChance, double medianRainMm) {
        if (rainChance >= 0.75 && medianRainMm >= 5) {
            return "Rain looks very likely, and it may be fairly noticeable.";
        } else if (rainChance >= 0.75) {
            return "Rain looks likely, but it may stay light.";
        } else if (rainChance >= 0.45 && medianRainMm >= 2) {
            return "There is a fair chance of some rain.";
        } else if (rainChance >= 0.45) {
            return "A little rain is possible.";
        } else {
            return "Rain does not look very likely right now.";
        }
    }

    private String buildHelperLine(double rainChance, double upperRainMm) {
        if (rainChance >= 0.75) {
            return "Bringing a jacket or umbrella would be smart.";
        } else if (upperRainMm >= 5) {
            return "There is a small chance of a heavier burst of rain.";
        } else {
            return "This looks mild overall.";
        }
    }
}
