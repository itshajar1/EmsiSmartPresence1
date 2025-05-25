package com.example.myapplication;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Home extends AppCompatActivity {

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home);

        mAuth = FirebaseAuth.getInstance();

        // Get current user
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            // Retrieve the user's display name and set it in the TextView
            String userName = currentUser.getDisplayName();
            if (userName != null) {
                TextView userNameTextView = findViewById(R.id.user_name);
                userNameTextView.setText(userName);
            } else {
                TextView userNameTextView = findViewById(R.id.user_name);
                userNameTextView.setText("No Name Set");
            }
        }

        // Apply font style to the username TextView
        TextView userNameTextView = findViewById(R.id.user_name);
        userNameTextView.setTypeface(Typeface.SANS_SERIF);

        // Set up all the clickable features/buttons with their actions
        setupFeatureButtons();
        setupChatbotButton();

    }

    private void setupFeatureButtons() {
        // Maps feature
        LinearLayout mapsButton = findViewById(R.id.map);
        mapsButton.setOnClickListener(v -> openMaps());

        // Documents feature
        LinearLayout documentsButton = findViewById(R.id.doc);
        documentsButton.setOnClickListener(v -> openDocuments());

        // Timetable feature
        LinearLayout timetableButton = findViewById(R.id.time);
        timetableButton.setOnClickListener(v -> openTimetable());

        // Rattrapages feature
        LinearLayout rattrapagesButton = findViewById(R.id.ratt);
        rattrapagesButton.setOnClickListener(v -> openRattrapages());

        // Absences feature
        LinearLayout absencesButton = findViewById(R.id.abs);
        absencesButton.setOnClickListener(v -> openAbsences());
    }
    private void setupChatbotButton() {
        Button btnChatbot = findViewById(R.id.btnChatbot);
        btnChatbot.setOnClickListener(v -> openChatbot());
    }
    private void openMaps() {
        Intent intent = new Intent(this, maps.class);
        startActivity(intent);
    }

    private void openDocuments() {
        Intent intent = new Intent(this, doc.class);
        startActivity(intent);
    }

    private void openTimetable() {
        Intent intent = new Intent(this, time.class);
        startActivity(intent);
    }

    private void openRattrapages() {
        Intent intent = new Intent(this, RattrapagesActivity.class);
        startActivity(intent);
    }

    private void openAbsences() {
        Intent intent = new Intent(this, abs.class);
        startActivity(intent);
    }
    private void openChatbot() {
        startActivity(new Intent(this, AssistantVirtuel.class)); // Remplace si ton activité a un autre nom
    }
}
