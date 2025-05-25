package com.example.myapplication;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AssistantVirtuel extends AppCompatActivity {
    private EditText editMessage;
    private TextView txtResponse;
    private Button btnSend;

    private final String API_KEY = "AIzaSyDnRLBC0m_xnDOT0fwzAeBQLmFBnOKiHKY"; // Replace with your Gemini API key
    private final OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_virtuel);

        editMessage = findViewById(R.id.prompt);
        txtResponse = findViewById(R.id.geminianswer);
        btnSend = findViewById(R.id.btnSend);

        btnSend.setOnClickListener(v -> {
            String userMessage = editMessage.getText().toString();
            if (!userMessage.isEmpty()) {
                sendMessageToGemini(userMessage);
            }
        });
    }

    private void sendMessageToGemini(String message) {
        // Initialisation du client HTTP
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();
        try {
            JSONArray contents = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", message);
            JSONObject contentItem = new JSONObject();
            contentItem.put("parts", new JSONArray().put(part));
            contents.put(contentItem);
            json.put("contents", contents);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Construction du corps JSON à envoyer à l'API
        RequestBody body = RequestBody.create(
                json.toString(),
                MediaType.parse("application/json"));

        String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + API_KEY;

        // Création de la requête http
        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        // Exécution de la requête dans un thread séparé***
        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    // Traitement de la réponse
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    String text = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    runOnUiThread(() -> txtResponse.setText(text));
                } else {
                    runOnUiThread(() -> txtResponse.setText("Erreur : " + response.message()));
                }
            } catch (IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> txtResponse.setText("Erreur : " + e.getMessage()));
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }
}
