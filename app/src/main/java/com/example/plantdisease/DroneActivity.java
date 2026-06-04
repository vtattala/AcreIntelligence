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
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class DroneActivity extends AppCompatActivity {

    private static final String STREAM_URL = "http://192.168.43.100:8080/stream";
    private static final String HEALTH_URL  = "http://192.168.43.100:8080/health";

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

        feedView    = findViewById(R.id.drone_feed);
        statusText  = findViewById(R.id.drone_status);
        connectBtn  = findViewById(R.id.btn_connect_drone);
        mainHandler = new Handler(Looper.getMainLooper());
        executor    = Executors.newFixedThreadPool(2);

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
                conn.connect();

                InputStream inputStream = new BufferedInputStream(conn.getInputStream());
                ByteArrayOutputStream frameBuffer = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int bytesRead;
                boolean inFrame = false;
                boolean headersDone = false;

                while (streaming.get() && (bytesRead = inputStream.read(buf)) != -1) {
                    String chunk = new String(buf, 0, bytesRead);

                    if (chunk.contains("--frame")) {
                        if (inFrame && frameBuffer.size() > 0) {
                            byte[] jpegData = frameBuffer.toByteArray();
                            Bitmap bmp = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);
                            if (bmp != null) {
                                mainHandler.post(() -> feedView.setImageBitmap(bmp));
                            }
                        }
                        frameBuffer.reset();
                        inFrame = true;
                        headersDone = false;
                        continue;
                    }

                    if (inFrame) {
                        if (!headersDone && chunk.contains("\r\n\r\n")) {
                            headersDone = true;
                            int dataStart = chunk.indexOf("\r\n\r\n") + 4;
                            if (dataStart < bytesRead) {
                                frameBuffer.write(buf, dataStart, bytesRead - dataStart);
                            }
                        } else if (headersDone) {
                            frameBuffer.write(buf, 0, bytesRead);
                        }
                    }
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