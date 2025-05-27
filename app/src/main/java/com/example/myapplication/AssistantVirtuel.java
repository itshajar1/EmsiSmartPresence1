package com.example.myapplication;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class AssistantVirtuel extends AppCompatActivity {
    private EditText editMessage;
    private View btnSend;
    private LinearLayout chatContainer;
    private ScrollView scrollView;
    private DrawerLayout drawerLayout;
    private LinearLayout drawerContent;
    private ImageView menuIcon;
    private TextView chatTitle;

    private final String API_KEY = "AIzaSyDnRLBC0m_xnDOT0fwzAeBQLmFBnOKiHKY"; // Replace with your actual API key
    private final OkHttpClient client = new OkHttpClient();
    private final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private SharedPreferences sharedPreferences;
    private String currentChatId;
    private List<ChatHistory> chatHistoryList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_virtuel);

        initializeViews();
        setupMenuFunctionality();
        loadChatHistory();
        startNewChat();

        btnSend.setOnClickListener(v -> {
            String userMessage = editMessage.getText().toString().trim();
            if (!userMessage.isEmpty()) {
                addMessage(userMessage, true);
                editMessage.setText("");
                sendMessageToGemini(userMessage);
                saveChatHistory();
            }
        });
    }

    private void initializeViews() {
        editMessage = findViewById(R.id.prompt);
        btnSend = findViewById(R.id.btnSend);
        chatContainer = findViewById(R.id.chatContainer);
        scrollView = findViewById(R.id.scrollView);
        drawerLayout = findViewById(R.id.drawerLayout);
        drawerContent = findViewById(R.id.drawerContent);
        menuIcon = findViewById(R.id.menuIcon);
        chatTitle = findViewById(R.id.chatTitle);

        sharedPreferences = getSharedPreferences("ChatHistory", Context.MODE_PRIVATE);
        chatHistoryList = new ArrayList<>();
    }

    private void setupMenuFunctionality() {
        menuIcon.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                updateHistoryDrawer();
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
    }

    private void startNewChat() {
        currentChatId = "chat_" + System.currentTimeMillis();
        chatTitle.setText("Nouvelle discussion");
        chatContainer.removeAllViews();
        addMessage("Bonjour ! Comment puis-je vous aider aujourd’hui ?", false);
    }

    private void addMessage(String message, boolean isUser) {
        // Create main container for the message
        LinearLayout messageContainer = new LinearLayout(this);
        messageContainer.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        containerParams.setMargins(0, 10, 0, 10);
        messageContainer.setLayoutParams(containerParams);

        // Create message bubble
        TextView textView = new TextView(this);
        textView.setText(message);
        textView.setTextSize(16);
        textView.setPadding(16, 12, 16, 12);
        textView.setBackgroundResource(isUser ? R.drawable.user_bubble : R.drawable.edittext_background);
        textView.setTextColor(isUser ? Color.WHITE : Color.BLACK);

        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        textParams.gravity = isUser ? Gravity.END : Gravity.START;
        textView.setLayoutParams(textParams);

        // Create action buttons container
        LinearLayout actionContainer = new LinearLayout(this);
        actionContainer.setOrientation(LinearLayout.HORIZONTAL);
        actionContainer.setPadding(8, 4, 8, 0);

        LinearLayout.LayoutParams actionContainerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        actionContainerParams.gravity = isUser ? Gravity.END : Gravity.START;
        actionContainer.setLayoutParams(actionContainerParams);

        // Create Copy button
        TextView copyButton = new TextView(this);
        copyButton.setText("📋 Copy");
        copyButton.setTextSize(11);
        copyButton.setPadding(16, 6, 16, 6);
        copyButton.setBackgroundColor(Color.TRANSPARENT);
        copyButton.setTextColor(Color.parseColor("#666666"));
        copyButton.setClickable(true);
        copyButton.setFocusable(true);

        // Add ripple effect
        copyButton.setBackground(createRippleDrawable());

        LinearLayout.LayoutParams copyParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        copyParams.setMargins(0, 0, 16, 0);
        copyButton.setLayoutParams(copyParams);

        copyButton.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText("message", message));
            Toast.makeText(this, "Copied!", Toast.LENGTH_SHORT).show();

            // Visual feedback
            copyButton.setText("✓ Copied");
            copyButton.setTextColor(Color.parseColor("#4CAF50"));
            copyButton.postDelayed(() -> {
                copyButton.setText("📋 Copy");
                copyButton.setTextColor(Color.parseColor("#666666"));
            }, 1500);
        });

        // Create Edit button
        TextView editButton = new TextView(this);
        editButton.setText("✏️ Edit");
        editButton.setTextSize(11);
        editButton.setPadding(16, 6, 16, 6);
        editButton.setBackgroundColor(Color.TRANSPARENT);
        editButton.setTextColor(Color.parseColor("#666666"));
        editButton.setClickable(true);
        editButton.setFocusable(true);

        // Add ripple effect
        editButton.setBackground(createRippleDrawable());

        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        editButton.setLayoutParams(editParams);

        editButton.setOnClickListener(v -> showEditDialog(textView, message));

        // Add buttons to action container
        actionContainer.addView(copyButton);
        actionContainer.addView(editButton);

        // Add views to message container
        messageContainer.addView(textView);
        messageContainer.addView(actionContainer);

        // Add message container to chat
        chatContainer.addView(messageContainer);
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
    }

    private android.graphics.drawable.Drawable createRippleDrawable() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            android.content.res.ColorStateList colorStateList = android.content.res.ColorStateList.valueOf(Color.parseColor("#20000000"));
            return new android.graphics.drawable.RippleDrawable(colorStateList, null, null);
        } else {
            // Fallback for older versions
            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setColor(Color.TRANSPARENT);
            drawable.setCornerRadius(12);
            return drawable;
        }
    }

    private void showEditDialog(TextView textView, String originalMessage) {
        EditText input = new EditText(this);
        input.setText(originalMessage);
        input.setTextSize(16);
        input.setPadding(16, 16, 16, 16);

        new AlertDialog.Builder(this)
                .setTitle("Modifier")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newText = input.getText().toString().trim();
                    if (!newText.isEmpty()) {
                        textView.setText(newText);
                        Toast.makeText(this, "Message modifié!", Toast.LENGTH_SHORT).show();
                        saveChatHistory();
                    }
                })
                .setNegativeButton("Annuler", null)
                .show();
    }

    private void updateHistoryDrawer() {
        drawerContent.removeAllViews();

        // Add header
        TextView header = new TextView(this);
        header.setText("💬 Historique");
        header.setTextSize(18);
        header.setTextColor(Color.BLACK);
        header.setPadding(16, 20, 16, 20);
        header.setGravity(Gravity.CENTER);
        header.setBackgroundColor(Color.parseColor("#F5F5F5"));
        drawerContent.addView(header);

        // Add "New Chat" button
        TextView newChatBtn = new TextView(this);
        newChatBtn.setText("➕ Nouvelle discussion");
        newChatBtn.setTextSize(16);
        newChatBtn.setPadding(20, 16, 20, 16);
        newChatBtn.setTextColor(Color.parseColor("#6200EE"));
        newChatBtn.setBackgroundColor(Color.WHITE);
        newChatBtn.setClickable(true);

        newChatBtn.setOnClickListener(v -> {
            startNewChat();
            drawerLayout.closeDrawer(GravityCompat.START);
        });
        drawerContent.addView(newChatBtn);

        // Add separator
        View separator = new View(this);
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2);
        separator.setLayoutParams(sepParams);
        separator.setBackgroundColor(Color.parseColor("#E0E0E0"));
        drawerContent.addView(separator);

        // Add chat history items
        for (int i = chatHistoryList.size() - 1; i >= 0; i--) {
            ChatHistory chat = chatHistoryList.get(i);
            addHistoryItem(chat);
        }

        if (chatHistoryList.isEmpty()) {
            TextView emptyMsg = new TextView(this);
            emptyMsg.setText("No previous chats");
            emptyMsg.setTextSize(14);
            emptyMsg.setPadding(20, 30, 20, 30);
            emptyMsg.setTextColor(Color.GRAY);
            emptyMsg.setGravity(Gravity.CENTER);
            drawerContent.addView(emptyMsg);
        }
    }

    private void addHistoryItem(ChatHistory chat) {
        LinearLayout historyItem = new LinearLayout(this);
        historyItem.setOrientation(LinearLayout.VERTICAL);
        historyItem.setPadding(20, 12, 20, 12);
        historyItem.setBackgroundColor(Color.WHITE);
        historyItem.setClickable(true);

        TextView titleView = new TextView(this);
        titleView.setText(chat.title);
        titleView.setTextSize(15);
        titleView.setTextColor(Color.BLACK);
        titleView.setMaxLines(1);

        TextView dateView = new TextView(this);
        dateView.setText(chat.date);
        dateView.setTextSize(12);
        dateView.setTextColor(Color.GRAY);

        historyItem.addView(titleView);
        historyItem.addView(dateView);

        historyItem.setOnClickListener(v -> {
            loadChat(chat);
            drawerLayout.closeDrawer(GravityCompat.START);
        });

        drawerContent.addView(historyItem);

        // Add separator
        View separator = new View(this);
        LinearLayout.LayoutParams sepParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1);
        separator.setLayoutParams(sepParams);
        separator.setBackgroundColor(Color.parseColor("#F0F0F0"));
        drawerContent.addView(separator);
    }

    private void saveChatHistory() {
        if (chatContainer.getChildCount() <= 1) return; // Don't save if only greeting message

        // Extract first user message as title
        String title = "New Chat";
        for (int i = 0; i < chatContainer.getChildCount(); i++) {
            View child = chatContainer.getChildAt(i);
            if (child instanceof LinearLayout) {
                LinearLayout messageContainer = (LinearLayout) child;
                if (messageContainer.getChildCount() > 0) {
                    View firstChild = messageContainer.getChildAt(0);
                    if (firstChild instanceof TextView) {
                        TextView textView = (TextView) firstChild;
                        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) textView.getLayoutParams();
                        if (params.gravity == Gravity.END) { // User message
                            title = textView.getText().toString();
                            if (title.length() > 30) {
                                title = title.substring(0, 30) + "...";
                            }
                            break;
                        }
                    }
                }
            }
        }

        String date = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(new Date());

        ChatHistory chatHistory = new ChatHistory(currentChatId, title, date, "");

        // Update existing or add new
        boolean found = false;
        for (int i = 0; i < chatHistoryList.size(); i++) {
            if (chatHistoryList.get(i).id.equals(currentChatId)) {
                chatHistoryList.set(i, chatHistory);
                found = true;
                break;
            }
        }
        if (!found) {
            chatHistoryList.add(chatHistory);
        }

        chatTitle.setText(title);
        saveChatHistoryToPrefs();
    }

    private void saveChatHistoryToPrefs() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        JSONArray jsonArray = new JSONArray();

        for (ChatHistory chat : chatHistoryList) {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("id", chat.id);
                jsonObject.put("title", chat.title);
                jsonObject.put("date", chat.date);
                jsonArray.put(jsonObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        editor.putString("chat_history", jsonArray.toString());
        editor.apply();
    }

    private void loadChatHistory() {
        String historyJson = sharedPreferences.getString("chat_history", "[]");
        try {
            JSONArray jsonArray = new JSONArray(historyJson);
            chatHistoryList.clear();

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                ChatHistory chat = new ChatHistory(
                        jsonObject.getString("id"),
                        jsonObject.getString("title"),
                        jsonObject.getString("date"),
                        ""
                );
                chatHistoryList.add(chat);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void loadChat(ChatHistory chat) {
        currentChatId = chat.id;
        chatTitle.setText(chat.title);
        chatContainer.removeAllViews();
        addMessage("Chat loaded: " + chat.title, false);
    }

    private void sendMessageToGemini(String message) {
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

        RequestBody body = RequestBody.create(json.toString(), JSON);
        String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .build();

        new Thread(() -> {
            try {
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String responseData = response.body().string();
                    JSONObject jsonResponse = new JSONObject(responseData);
                    JSONArray candidates = jsonResponse.getJSONArray("candidates");
                    String text = candidates.getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    runOnUiThread(() -> {
                        addMessage(text, false);
                        saveChatHistory();
                    });
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No details";
                    runOnUiThread(() -> Toast.makeText(this, "Error: " + errorBody, Toast.LENGTH_LONG).show());
                }
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                runOnUiThread(() -> addMessage("Error: " + e.getMessage(), false));
            }
        }).start();
    }

    // Inner class for chat history
    private static class ChatHistory {
        String id;
        String title;
        String date;
        String content;

        ChatHistory(String id, String title, String date, String content) {
            this.id = id;
            this.title = title;
            this.date = date;
            this.content = content;
        }
    }
}