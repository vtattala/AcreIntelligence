package com.example.plantdisease;

import android.os.Bundle;
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

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ChatActivity extends AppCompatActivity {

    // UI
    LinearLayout chatContainer;
    EditText messageInput;
    Button sendButton;
    ScrollView chatScroll;

    // 🔐 YOUR BACKEND (NOT OPENAI)
    private static final String SERVER_URL =
            "https://chatgpt-backend-m3jh.onrender.com/chat";

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chatbot);

        chatContainer = findViewById(R.id.chatContainer);
        messageInput = findViewById(R.id.messageInput);
        sendButton = findViewById(R.id.sendButton);
        chatScroll = findViewById(R.id.chatScroll);

        sendButton.setOnClickListener(v -> {
            String userText = messageInput.getText().toString().trim();
            if (userText.isEmpty()) return;

            addMessage(userText, true);
            messageInput.setText("");

            sendMessageToAI(userText);
        });
    }

    /**
     * Sends message to YOUR SERVER, which talks to ChatGPT
     */
    private void sendMessageToAI(String userMessage) {
        try {
            JSONObject json = new JSONObject();
            json.put("message", userMessage);

            RequestBody body = RequestBody.create(
                    json.toString(),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .post(body)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() ->
                            addMessage("❌ Error connecting to server.", false)
                    );
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        runOnUiThread(() ->
                                addMessage("❌ Server error.", false)
                        );
                        return;
                    }

                    try {
                        JSONObject res =
                                new JSONObject(response.body().string());

                        String reply = res.getString("reply");

                        runOnUiThread(() ->
                                addMessage(reply, false)
                        );

                    } catch (Exception e) {
                        runOnUiThread(() ->
                                addMessage("❌ Invalid response.", false)
                        );
                    }
                }
            });

        } catch (Exception e) {
            addMessage("❌ Request failed.", false);
        }
    }

    /**
     * Adds a chat bubble
     */
    private void addMessage(String text, boolean isUser) {
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

        chatScroll.post(() ->
                chatScroll.fullScroll(View.FOCUS_DOWN)
        );
    }
}


//package com.example.plantdisease;
//
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.view.Gravity;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.ScrollView;
//import android.widget.TextView;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.android.material.card.MaterialCardView;
//
//public class ChatActivity extends AppCompatActivity {
//
//    // UI
//    LinearLayout chatContainer;
//    EditText messageInput;
//    Button sendButton;
//    ScrollView chatScroll;
//
//    // 🔹 PLACEHOLDER AI CONFIG
//    private static final String API_KEY = "PASTE_YOUR_API_KEY_HERE";
//    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
//    private static final String MODEL_NAME = "gpt-4.1-mini"; // placeholder
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.chatbot);
//
//        chatContainer = findViewById(R.id.chatContainer);
//        messageInput = findViewById(R.id.messageInput);
//        sendButton = findViewById(R.id.sendButton);
//        chatScroll = findViewById(R.id.chatScroll);
//
//        sendButton.setOnClickListener(v -> {
//            String userText = messageInput.getText().toString().trim();
//            if (userText.isEmpty()) return;
//
//            addMessage(userText, true);
//            messageInput.setText("");
//
//            // 🔹 SEND MESSAGE TO "AI"
//            sendMessageToAI(userText);
//        });
//    }
//
//    /**
//     * 🔹 PLACEHOLDER METHOD
//     * This simulates an AI call.
//     * Later, you will replace the inside with a real OpenAI HTTP request.
//     */
//    private void sendMessageToAI(String userMessage) {
//
//        // Simulate network delay (like a real API call)
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//
//            // 🔹 FAKE AI RESPONSE (replace later)
//            String fakeAIResponse =
//                    "🤖 (Sample AI Reply)\n\n" +
//                            "You asked: \"" + userMessage + "\"\n\n" +
//                            "This is where OpenAI's response will appear.";
//
//            addMessage(fakeAIResponse, false);
//
//        }, 1200); // 1.2 seconds delay
//    }
//
//    /**
//     * Adds a chat bubble to the screen
//     */
//    private void addMessage(String text, boolean isUser) {
//        MaterialCardView card = new MaterialCardView(this);
//        card.setRadius(40);
//        card.setCardElevation(4);
//        card.setUseCompatPadding(true);
//
//        LinearLayout.LayoutParams params =
//                new LinearLayout.LayoutParams(
//                        LinearLayout.LayoutParams.WRAP_CONTENT,
//                        LinearLayout.LayoutParams.WRAP_CONTENT
//                );
//        params.bottomMargin = 16;
//
//        if (isUser) {
//            params.gravity = Gravity.END;
//            card.setCardBackgroundColor(0xFF4CAF50); // green
//        } else {
//            params.gravity = Gravity.START;
//            card.setCardBackgroundColor(0xFF2196F3); // blue
//        }
//
//        card.setLayoutParams(params);
//
//        TextView textView = new TextView(this);
//        textView.setText(text);
//        textView.setTextSize(16);
//        textView.setTextColor(0xFFFFFFFF);
//        textView.setPadding(32, 20, 32, 20);
//
//        card.addView(textView);
//        chatContainer.addView(card);
//
//        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
//    }
//}