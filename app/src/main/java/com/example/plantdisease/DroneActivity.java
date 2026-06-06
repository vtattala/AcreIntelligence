package com.example.plantdisease;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DroneActivity extends AppCompatActivity {

    private static final String STREAM_URL = "http://192.168.68.121:8080/stream";
    private static final String HEALTH_URL = "http://192.168.68.121:8080/health";

    private ImageView feedView;
    private TextView statusText;
    private Button connectBtn;
    private ExecutorService executor;
    private Handler mainHandler;
    private AtomicBoolean streaming = new AtomicBoolean(false);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_drone);

        feedView = findViewById(R.id.drone_feed);
        statusText = findViewById(R.id.drone_status);
        connectBtn = findViewById(R.id.btn_connect_drone);
        mainHandler = new Handler(Looper.getMainLooper());
        executor = Executors.newFixedThreadPool(2);

        connectBtn.setOnClickListener(v -> {
            if (!streaming.get()) {
                startStream();
                connectBtn.setText("Disconnect");
            } else {
                stopStream();
                connectBtn.setText("Connect to Drone");
            }
        });
    }

    private void startStream() {
        streaming.set(true);
        setStatus("⏳ Connecting to drone...");

        executor.submit(() -> {
            try {
                // Check if Pi is reachable
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

                    // Detect JPEG end marker 0xFF 0xD9
                    if (prev == 0xFF && b == 0xD9) {
                        byte[] jpegData = jpegBuffer.toByteArray();

                        // Find JPEG start marker 0xFF 0xD8
                        int start = -1;
                        for (int i = 0; i < jpegData.length - 1; i++) {
                            if ((jpegData[i] & 0xFF) == 0xFF && (jpegData[i + 1] & 0xFF) == 0xD8) {
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

    private void stopStream() {
        streaming.set(false);
        setStatus("⚪ Disconnected");
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