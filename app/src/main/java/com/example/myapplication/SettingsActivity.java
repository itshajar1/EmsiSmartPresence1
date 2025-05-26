package com.example.myapplication;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private EditText etFirstName, etLastName;
    private Button btnUpdateProfile, btnUpdatePassword;
    private ImageButton btnBack;
    private TextView tvUserEmail, tvCreationDate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        initializeViews();
        loadCurrentUserData();
        setupClickListeners();
    }

    private void initializeViews() {
        etFirstName = findViewById(R.id.et_first_name);
        etLastName = findViewById(R.id.et_last_name);
        btnUpdateProfile = findViewById(R.id.btn_update_profile);
        btnUpdatePassword = findViewById(R.id.btn_update_password);
        btnBack = findViewById(R.id.btn_back);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvCreationDate = findViewById(R.id.tv_creation_date);
    }

    private void loadCurrentUserData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            // Load profile name
            String displayName = currentUser.getDisplayName();
            if (displayName != null && displayName.contains(" ")) {
                String[] names = displayName.split(" ", 2);
                etFirstName.setText(names[0]);
                etLastName.setText(names[1]);
            } else if (displayName != null) {
                etFirstName.setText(displayName);
            }

            // Load email
            tvUserEmail.setText(currentUser.getEmail());

            // Load creation date
            if (currentUser.getMetadata() != null) {
                long creationTimestamp = currentUser.getMetadata().getCreationTimestamp();
                String date = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(new Date(creationTimestamp));
                tvCreationDate.setText(date);
            }
        }
    }

    private void setupClickListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnUpdateProfile.setOnClickListener(v -> updateProfile());
        btnUpdatePassword.setOnClickListener(v -> showPasswordChangeDialog());
    }

    private void updateProfile() {
        String firstName = etFirstName.getText().toString().trim();
        String lastName = etLastName.getText().toString().trim();

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First name is required");
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String fullName = firstName + (TextUtils.isEmpty(lastName) ? "" : " " + lastName);

            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName)
                    .build();

            user.updateProfile(profileUpdates)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(SettingsActivity.this,
                                    "Profile updated successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(SettingsActivity.this,
                                    "Failed to update profile", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void showPasswordChangeDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_change_password, null);
        builder.setView(dialogView);
        builder.setTitle("Change Password");

        EditText etCurrentPassword = dialogView.findViewById(R.id.et_current_password);
        EditText etNewPassword = dialogView.findViewById(R.id.et_new_password);
        EditText etConfirmPassword = dialogView.findViewById(R.id.et_confirm_password);

        AlertDialog dialog = builder.create();

        dialogView.findViewById(R.id.btn_update).setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();
            String confirmPassword = etConfirmPassword.getText().toString().trim();

            if (validatePasswordInputs(currentPassword, newPassword, confirmPassword)) {
                updatePassword(currentPassword, newPassword);
                dialog.dismiss();
            }
        });

        dialogView.findViewById(R.id.btn_cancel).setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private boolean validatePasswordInputs(String currentPassword, String newPassword, String confirmPassword) {
        if (TextUtils.isEmpty(currentPassword)) {
            Toast.makeText(this, "Current password is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (TextUtils.isEmpty(newPassword)) {
            Toast.makeText(this, "New password is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (newPassword.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords don't match", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private void updatePassword(String currentPassword, String newPassword) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && user.getEmail() != null) {
            AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);

            user.reauthenticate(credential)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            user.updatePassword(newPassword)
                                    .addOnCompleteListener(updateTask -> {
                                        if (updateTask.isSuccessful()) {
                                            Toast.makeText(SettingsActivity.this,
                                                    "Password updated successfully", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(SettingsActivity.this,
                                                    "Failed to update password", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(SettingsActivity.this,
                                    "Current password is incorrect", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
}