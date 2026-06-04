package com.example.plantdisease;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;

import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatDebug"; // Filter by this in Logcat

    LinearLayout chatContainer;
    EditText messageInput;
    Button sendButton;
    ScrollView chatScroll;

    private static final String SERVER_URL =
            "https://chatgpt-backend-m3jh.onrender.com/chat";

    // ✅ FIXED: Added timeouts for Render cold start
    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot);

        chatContainer = findViewById(R.id.chatContainer);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatScroll = findViewById(R.id.chatScroll);

        Log.d(TAG, "✅ ChatActivity created successfully");
        Log.d(TAG, "🌐 Server URL: " + SERVER_URL);

        sendButton.setOnClickListener(v -> {
            String userText = messageInput.getText().toString().trim();
            Log.d(TAG, "📤 Send button clicked. Message: " + userText);

            if (userText.isEmpty()) {
                Log.d(TAG, "⚠️ Empty message, ignoring");
                return;
            }

            addMessage(userText, true);
            messageInput.setText("");
            sendMessageToAI(userText);
        });
    }

    private void sendMessageToAI(String userMessage) {
        Log.d(TAG, "🚀 Sending message to AI: " + userMessage);

        try {
            JSONObject json = new JSONObject();
            json.put("message", userMessage);
            String jsonString = json.toString();

            Log.d(TAG, "📦 Request body: " + jsonString);

            RequestBody body = RequestBody.create(
                    jsonString,
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .post(body)
                    .build();

            Log.d(TAG, "📡 Making HTTP POST to: " + SERVER_URL);

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e(TAG, "❌ Network FAILED: " + e.getMessage());
                    Log.e(TAG, "❌ Failure cause: " + e.getCause());
                    runOnUiThread(() ->
                            addMessage("❌ Connection failed: " + e.getMessage(), false)
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    int statusCode = response.code();
                    String responseBody = response.body().string(); // Read ONCE

                    Log.d(TAG, "📥 Response received!");
                    Log.d(TAG, "📊 Status code: " + statusCode);
                    Log.d(TAG, "📄 Raw response body: " + responseBody);

                    if (!response.isSuccessful()) {
                        Log.e(TAG, "❌ Server returned error " + statusCode);
                        Log.e(TAG, "❌ Error body: " + responseBody);
                        runOnUiThread(() ->
                                addMessage("❌ Server error " + statusCode + ":\n" + responseBody, false)
                        );
                        return;
                    }

                    try {
                        Log.d(TAG, "🔍 Parsing JSON response...");
                        JSONObject res = new JSONObject(responseBody);

                        // Log all keys in the response
                        Log.d(TAG, "🔑 JSON keys: " + res.keys().toString());

                        if (!res.has("reply")) {
                            Log.e(TAG, "❌ No 'reply' key found in response!");
                            Log.e(TAG, "❌ Full response was: " + responseBody);
                            runOnUiThread(() ->
                                    addMessage("❌ Missing 'reply' in response:\n" + responseBody, false)
                            );
                            return;
                        }

                        String reply = res.getString("reply");
                        Log.d(TAG, "✅ Got reply: " + reply);

                        runOnUiThread(() -> addMessage(reply, false));

                    } catch (Exception e) {
                        Log.e(TAG, "❌ JSON parse error: " + e.getMessage());
                        Log.e(TAG, "❌ Raw body was: " + responseBody);
                        runOnUiThread(() ->
                                addMessage("❌ Parse error: " + e.getMessage() + "\nRaw: " + responseBody, false)
                        );
                    }
                }
            });

        } catch (Exception e) {
            Log.e(TAG, "❌ Request setup failed: " + e.getMessage());
            runOnUiThread(() -> addMessage("❌ Request failed: " + e.getMessage(), false));
        }
    }

    private void addMessage(String text, boolean isUser) {
        Log.d(TAG, "💬 Adding message [" + (isUser ? "USER" : "AI") + "]: " + text);

        MaterialCardView card = new MaterialCardView(this);
        card.setRadius(40);
        card.setCardElevation(4);
        card.setUseCompatPadding(true);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
        params.bottomMargin = 16;

        if (isUser) {
            params.gravity = Gravity.END;
            card.setCardBackgroundColor(0xFF4CAF50);
        } else {
            params.gravity = Gravity.START;
            card.setCardBackgroundColor(0xFF2196F3);
        }

        card.setLayoutParams(params);

        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(16);
        textView.setTextColor(0xFFFFFFFF);
        textView.setPadding(32, 20, 32, 20);

        card.addView(textView);
        chatContainer.addView(card);

        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
    }
}