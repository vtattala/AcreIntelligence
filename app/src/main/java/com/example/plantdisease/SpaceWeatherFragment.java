package com.example.plantdisease;

import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpaceWeatherFragment extends Fragment {
    private static final String ARG_COMPACT = "COMPACT";

    private TextView kpIndexValue, kpStatus, solarWindValue, solarWindStatus;
    private TextView f107Value, f107Status, geomagneticStatus, radiationStatus;
    private TextView cropImpact, satelliteImpact, aiSummary;

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .build();

    // store last values for AI prompt
    private double lastKpIndex = Double.NaN;
    private double lastEstimatedSpeed = Double.NaN;
    private double lastF107 = Double.NaN;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        boolean compact = getArguments() != null && getArguments().getBoolean(ARG_COMPACT, false);
        View view = inflater.inflate(compact ? R.layout.panel_space_weather : R.layout.fragment_space_weather, container, false);

        // Connect to XML views
        kpIndexValue = view.findViewById(R.id.kpIndexValue);
        kpStatus = view.findViewById(R.id.kpStatus);
        solarWindValue = view.findViewById(R.id.solarWindValue);
        solarWindStatus = view.findViewById(R.id.solarWindStatus);
        f107Value = view.findViewById(R.id.f107Value);
        f107Status = view.findViewById(R.id.f107Status);
        geomagneticStatus = view.findViewById(R.id.geomagneticStatus);
        radiationStatus = view.findViewById(R.id.radiationStatus);

        // Agriculture impact views
        cropImpact = view.findViewById(R.id.cropImpact);
        satelliteImpact = view.findViewById(R.id.satelliteImpact);

        // Space Weather Agent analysis view
        aiSummary = view.findViewById(R.id.aiSummary);

        // Refresh button
        view.findViewById(R.id.refreshBtn).setOnClickListener(v -> {
            kpIndexValue.setText("Loading...");
            solarWindValue.setText("Loading...");
            f107Value.setText("Loading...");
            cropImpact.setText("Analyzing crop impact...");
            satelliteImpact.setText("Analyzing satellite impact...");
            aiSummary.setText("Generating agent analysis...");
            fetchSpaceWeather();
        });

        // Fetch data on load
        fetchSpaceWeather();

        return view;
    }

    public static SpaceWeatherFragment compact() {
        SpaceWeatherFragment fragment = new SpaceWeatherFragment();
        Bundle args = new Bundle();
        args.putBoolean(ARG_COMPACT, true);
        fragment.setArguments(args);
        return fragment;
    }

    private void fetchSpaceWeather() {
        String spaceWeatherUrl = "https://services.swpc.noaa.gov/json/planetary_k_index_1m.json";
        Request request = new Request.Builder().url(spaceWeatherUrl).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SPACE_WEATHER", "Failed to fetch Kp index", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        kpIndexValue.setText("Error");
                        kpStatus.setText("Unable to fetch data: " + describeNetworkFailure(e));
                        cropImpact.setText("Unable to determine crop impact");
                        aiSummary.setText("Agent analysis unavailable.");
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : null;
                parseSpaceWeather(body);
            }
        });

        fetchSolarWind();
    }

    private void fetchSolarWind() {
        String url = "https://services.swpc.noaa.gov/json/rtsw/rtsw_mag_1m.json";
        Request request = new Request.Builder().url(url).build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("SOLAR_WIND", "Failed to fetch solar wind", e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        satelliteImpact.setText("Unable to determine satellite impact: " + describeNetworkFailure(e));
                        aiSummary.setText("Agent analysis unavailable.");
                    });
                }
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                String body = response.body() != null ? response.body().string() : null;
                parseSolarWind(body);
            }
        });
    }

    private void parseSpaceWeather(String json) {
        try {
            if (json == null || json.isEmpty()) {
                throw new Exception("Empty Kp JSON");
            }

            JSONArray array = new JSONArray(json);

            if (array.length() > 0 && getActivity() != null) {
                JSONObject latest = array.getJSONObject(array.length() - 1);
                double kpIndex = latest.optDouble("kp_index", Double.NaN);
                lastKpIndex = kpIndex;

                getActivity().runOnUiThread(() -> {
                    kpIndexValue.setText(Double.isNaN(kpIndex) ? "--" : String.format("%.1f", kpIndex));

                    // GEOMAGNETIC STATUS + COLORS
                    if (Double.isNaN(kpIndex)) {
                        kpStatus.setText("N/A");
                        kpStatus.setTextColor(Color.parseColor("#7B83A6"));
                        geomagneticStatus.setText("Data unavailable");
                        geomagneticStatus.setTextColor(Color.parseColor("#7B83A6"));
                        cropImpact.setText("Crop Impact: Data unavailable");
                        cropImpact.setTextColor(Color.parseColor("#7B83A6"));
                    } else if (kpIndex < 4) {
                        kpStatus.setText("Quiet");
                        kpStatus.setTextColor(Color.parseColor("#2E7D32"));
                        geomagneticStatus.setText("Normal geomagnetic conditions");
                        geomagneticStatus.setTextColor(Color.parseColor("#2E7D32"));

                        cropImpact.setText("Crop Impact: Satellite vegetation data is reliable today.");
                        cropImpact.setTextColor(Color.parseColor("#2E7D32"));

                    } else if (kpIndex < 6) {
                        kpStatus.setText("Unsettled");
                        kpStatus.setTextColor(Color.parseColor("#F9A825"));
                        geomagneticStatus.setText("Minor geomagnetic disturbance");
                        geomagneticStatus.setTextColor(Color.parseColor("#F9A825"));

                        cropImpact.setText("Crop Impact: NDVI and soil moisture readings may show slight noise.");
                        cropImpact.setTextColor(Color.parseColor("#F9A825"));

                    } else {
                        kpStatus.setText("Storm");
                        kpStatus.setTextColor(Color.parseColor("#C62828"));
                        geomagneticStatus.setText("Strong geomagnetic storm");
                        geomagneticStatus.setTextColor(Color.parseColor("#C62828"));

                        cropImpact.setText("Crop Impact: Strong storm — vegetation indices may be inaccurate.");
                        cropImpact.setTextColor(Color.parseColor("#C62828"));
                    }

                    // RADIATION IMPACT
                    if (!Double.isNaN(kpIndex) && kpIndex < 5) {
                        radiationStatus.setText("Normal radiation levels");
                        radiationStatus.setTextColor(Color.parseColor("#2E7D32"));
                    } else if (!Double.isNaN(kpIndex)) {
                        radiationStatus.setText("Elevated radiation — monitor crop stress");
                        radiationStatus.setTextColor(Color.parseColor("#D84315"));
                    }
                    publishSpaceWeatherState();
                });
            }
        } catch (Exception e) {
            Log.e("PARSE_KP", "Error parsing Kp JSON", e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> aiSummary.setText("Agent analysis unavailable."));
            }
        }
    }

    private void parseSolarWind(String json) {
        try {
            if (json == null || json.isEmpty()) {
                throw new Exception("Empty solar wind JSON");
            }

            JSONArray array = new JSONArray(json);

            if (array.length() > 0 && getActivity() != null) {
                JSONObject latest = array.getJSONObject(array.length() - 1);

                double bt = latest.optDouble("bt", 0.0);
                double estimatedSpeed = 300 + (bt * 50);
                double f107 = 70 + (bt * 10);

                // store for AI prompt
                lastEstimatedSpeed = estimatedSpeed;
                lastF107 = f107;

                getActivity().runOnUiThread(() -> {
                    solarWindValue.setText(String.format("%.0f km/s", estimatedSpeed));

                    // SOLAR WIND STATUS
                    if (estimatedSpeed < 400) {
                        solarWindStatus.setText("Slow");
                        solarWindStatus.setTextColor(Color.parseColor("#2E7D32"));

                        satelliteImpact.setText("Satellite Impact: Stable conditions for crop monitoring.");
                        satelliteImpact.setTextColor(Color.parseColor("#2E7D32"));

                    } else if (estimatedSpeed < 600) {
                        solarWindStatus.setText("Elevated");
                        solarWindStatus.setTextColor(Color.parseColor("#F9A825"));

                        satelliteImpact.setText("Satellite Impact: Minor interference possible in NDVI data.");
                        satelliteImpact.setTextColor(Color.parseColor("#F9A825"));

                    } else {
                        solarWindStatus.setText("High-Speed Stream");
                        solarWindStatus.setTextColor(Color.parseColor("#D84315"));

                        satelliteImpact.setText("Satellite Impact: Expect reduced accuracy in vegetation data.");
                        satelliteImpact.setTextColor(Color.parseColor("#D84315"));
                    }

                    // F10.7 SOLAR FLUX
                    f107Value.setText(String.format("%.0f sfu", f107));

                    if (f107 < 100) {
                        f107Status.setText("Low Solar Activity");
                        f107Status.setTextColor(Color.parseColor("#2E7D32"));
                    } else if (f107 < 150) {
                        f107Status.setText("Moderate Activity");
                        f107Status.setTextColor(Color.parseColor("#F9A825"));
                    } else {
                        f107Status.setText("High Solar Activity");
                        f107Status.setTextColor(Color.parseColor("#D84315"));
                    }

                    generateSpaceWeatherAgentSummary();
                    publishSpaceWeatherState();
                });
            }
        } catch (Exception e) {
            Log.e("PARSE_SOLAR", "Error parsing solar wind JSON", e);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> aiSummary.setText("Agent analysis unavailable."));
            }
        }
    }

    private void generateSpaceWeatherAgentSummary() {
        String risk = "Low";
        if (!Double.isNaN(lastKpIndex) && lastKpIndex >= 6) {
            risk = "High";
        } else if ((!Double.isNaN(lastKpIndex) && lastKpIndex >= 4)
                || (!Double.isNaN(lastEstimatedSpeed) && lastEstimatedSpeed >= 600)
                || (!Double.isNaN(lastF107) && lastF107 >= 150)) {
            risk = "Medium";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("Space Weather Agent\n\n");
        summary.append("Integrated space risk: ").append(risk).append(". ");
        summary.append("Kp ")
                .append(Double.isNaN(lastKpIndex) ? "N/A" : String.format("%.1f", lastKpIndex))
                .append(", solar wind ")
                .append(Double.isNaN(lastEstimatedSpeed) ? "N/A" : String.format("%.0f km/s", lastEstimatedSpeed))
                .append(", F10.7 ")
                .append(Double.isNaN(lastF107) ? "N/A" : String.format("%.0f sfu", lastF107))
                .append(".\n\n");

        if ("High".equals(risk)) {
            summary.append("Meaning for crops: direct crop damage is usually not the main concern. The bigger issue is that satellite-based vegetation, NDVI, and soil-moisture signals may be noisier during disturbed geomagnetic conditions.\n\n");
            summary.append("Action: If field data shows sudden stress today, verify with ground observation before making a major irrigation, disease, or fertilizer decision.");
        } else if ("Medium".equals(risk)) {
            summary.append("Meaning for crops: crop growth impact is likely limited, but remote sensing and GPS-linked field readings may have mild reliability issues.\n\n");
            summary.append("Action: Use satellite/agricultural data normally, but double-check unusual readings with a field walk or drone feed.");
        } else {
            summary.append("Meaning for crops: space weather is quiet enough that satellite vegetation and soil-moisture monitoring should be reasonably reliable.\n\n");
            summary.append("Action: Normal crop scouting and water decisions can rely more confidently on field and satellite data.");
        }

        summary.append("\n\nWhy this matters: space weather does not diagnose plant disease by itself, but it can affect the quality of satellite signals that the farm advisor uses to interpret crop health, water stress, and field variability.");
        aiSummary.setText(summary.toString());
    }

    private void publishSpaceWeatherState() {
        if (Double.isNaN(lastKpIndex) && Double.isNaN(lastEstimatedSpeed) && Double.isNaN(lastF107)) {
            return;
        }

        String risk = "Low";
        if (!Double.isNaN(lastKpIndex) && lastKpIndex >= 6) {
            risk = "High";
        } else if (!Double.isNaN(lastKpIndex) && lastKpIndex >= 4) {
            risk = "Medium";
        } else if (!Double.isNaN(lastEstimatedSpeed) && lastEstimatedSpeed >= 600) {
            risk = "Medium";
        }

        AcreAgentRepository.getInstance().updateSpaceWeather(
                new AcreAgentRepository.SpaceWeatherState(
                        risk,
                        Double.isNaN(lastKpIndex) ? -1.0 : lastKpIndex,
                        Double.isNaN(lastEstimatedSpeed) ? -1.0 : lastEstimatedSpeed,
                        Double.isNaN(lastF107) ? -1.0 : lastF107,
                        cropImpact == null ? "" : String.valueOf(cropImpact.getText()),
                        satelliteImpact == null ? "" : String.valueOf(satelliteImpact.getText())
                )
        );
    }

    private String describeNetworkFailure(IOException exception) {
        String message = exception.getMessage();
        String reason = message == null || message.isEmpty()
                ? exception.getClass().getSimpleName()
                : message;
        String network = describeActiveNetwork();
        return reason + " | " + network;
    }

    private String describeActiveNetwork() {
        if (getContext() == null) {
            return "network unknown";
        }
        ConnectivityManager manager = (ConnectivityManager)
                getContext().getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        if (manager == null) {
            return "network manager unavailable";
        }
        Network network = manager.getActiveNetwork();
        if (network == null) {
            return "no active network";
        }
        NetworkCapabilities capabilities = manager.getNetworkCapabilities(network);
        if (capabilities == null) {
            return "network has no capabilities";
        }
        boolean wifi = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
        boolean cellular = capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
        boolean internet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        boolean validated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        return "wifi=" + wifi + ", cellular=" + cellular
                + ", internet=" + internet + ", validated=" + validated;
    }
}

