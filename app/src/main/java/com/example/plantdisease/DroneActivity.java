package com.example.plantdisease;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DroneActivity extends AppCompatActivity {

    private static final String STREAM_URL  = "http://192.168.68.121:8080/stream";
    private static final String HEALTH_URL  = "http://192.168.68.121:8080/health";
    private static final String THERMAL_URL = "http://192.168.68.121:8080/thermal";

    private ImageView feedView;
    private TextView statusText;
    private Button connectBtn, thermalBtn;
    private ThermalOverlayView thermalOverlay;
    private EditText inputTempMin, inputTempMax;
    private ExecutorService executor;
    private Handler mainHandler;
    private AtomicBoolean streaming = new AtomicBoolean(false);
    private AtomicBoolean thermalOn = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone);

        feedView       = findViewById(R.id.drone_feed);
        statusText     = findViewById(R.id.drone_status);
        connectBtn     = findViewById(R.id.btn_connect_drone);
        thermalBtn     = findViewById(R.id.btn_thermal);
        thermalOverlay = findViewById(R.id.thermal_overlay);
        inputTempMin   = findViewById(R.id.input_temp_min);
        inputTempMax   = findViewById(R.id.input_temp_max);
        mainHandler    = new Handler(Looper.getMainLooper());
        executor       = Executors.newFixedThreadPool(3);

        // Apply custom temperature range
        findViewById(R.id.btn_apply_temp).setOnClickListener(v -> {
            try {
                float min = Float.parseFloat(inputTempMin.getText().toString());
                float max = Float.parseFloat(inputTempMax.getText().toString());
                if (min < max) {
                    thermalOverlay.setOptimalRange(min, max);
                }
            } catch (NumberFormatException e) {
                // Invalid input, ignore
            }
        });

        connectBtn.setOnClickListener(v -> {
            if (!streaming.get()) {
                startStream();
                connectBtn.setText("Disconnect");
            } else {
                stopStream();
                connectBtn.setText("Connect to Drone");
            }
        });

        thermalBtn.setOnClickListener(v -> {
            if (!thermalOn.get()) {
                thermalOn.set(true);
                thermalOverlay.setVisibility(android.view.View.VISIBLE);
                thermalBtn.setText("Thermal On");
                thermalBtn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#22C55E")));
                startThermalPolling();
            } else {
                thermalOn.set(false);
                thermalOverlay.setVisibility(android.view.View.GONE);
                thermalBtn.setText("Thermal Off");
                thermalBtn.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(
                                android.graphics.Color.parseColor("#134D2E")));
            }
        });
    }

    private void startStream() {
        streaming.set(true);
        setStatus("⏳ Connecting to drone...");

        executor.submit(() -> {
            try {
                URL healthUrl = new URL(HEALTH_URL);
                HttpURLConnection healthConn = (HttpURLConnection) healthUrl.openConnection();
                healthConn.setConnectTimeout(3000);
                healthConn.connect();
                if (healthConn.getResponseCode() != 200) {
                    setStatus("❌ Pi not reachable. Is hotspot on?");
                    streaming.set(false);
                    return;
                }
                healthConn.disconnect();

                setStatus("🟢 Drone feed live");

                URL url = new URL(STREAM_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(10000);
                conn.connect();

                InputStream inputStream = conn.getInputStream();
                ByteArrayOutputStream jpegBuffer = new ByteArrayOutputStream();
                int b;
                int prev = -1;

                while (streaming.get()) {
                    b = inputStream.read();
                    if (b == -1) break;
                    jpegBuffer.write(b);
                    if (prev == 0xFF && b == 0xD9) {
                        byte[] jpegData = jpegBuffer.toByteArray();
                        int start = -1;
                        for (int i = 0; i < jpegData.length - 1; i++) {
                            if ((jpegData[i] & 0xFF) == 0xFF && (jpegData[i+1] & 0xFF) == 0xD8) {
                                start = i;
                                break;
                            }
                        }
                        if (start >= 0) {
                            final byte[] frame = Arrays.copyOfRange(jpegData, start, jpegData.length);
                            Bitmap bmp = BitmapFactory.decodeByteArray(frame, 0, frame.length);
                            if (bmp != null) {
                                mainHandler.post(() -> feedView.setImageBitmap(bmp));
                            }
                        }
                        jpegBuffer.reset();
                    }
                    prev = b;
                }
                conn.disconnect();

            } catch (Exception e) {
                setStatus("❌ Connection failed: " + e.getMessage());
                streaming.set(false);
                mainHandler.post(() -> connectBtn.setText("Connect to Drone"));
            }
        });
    }

    private void startThermalPolling() {
        executor.submit(() -> {
            while (thermalOn.get()) {
                try {
                    URL url = new URL(THERMAL_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(5000);
                    conn.connect();

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    conn.disconnect();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray rows = json.getJSONArray("temps");
                    float min = (float) json.getDouble("min");
                    float max = (float) json.getDouble("max");

                    float[][] data = new float[24][32];
                    for (int r = 0; r < 24; r++) {
                        JSONArray row = rows.getJSONArray(r);
                        for (int c = 0; c < 32; c++) {
                            data[r][c] = (float) row.getDouble(c);
                        }
                    }

                    mainHandler.post(() -> thermalOverlay.updateThermal(data, min, max));
                    Thread.sleep(500);

                } catch (Exception e) {
                    try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                }
            }
        });
    }

    private void stopStream() {
        streaming.set(false);
        thermalOn.set(false);
        setStatus("⚪ Disconnected");
        mainHandler.post(() -> thermalOverlay.setVisibility(android.view.View.GONE));
    }

    private void setStatus(String msg) {
        mainHandler.post(() -> statusText.setText(msg));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopStream();
        executor.shutdownNow();
    }
}