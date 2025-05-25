package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.*;

public class Register extends AppCompatActivity {

    private EditText etName, etEmail, etPassword, confpassword;
    private FirebaseAuth mAuth;
    private TextView tologin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        confpassword = findViewById(R.id.et_confirm_password);
        tologin = findViewById(R.id.to_login);

        Button btnRegister = findViewById(R.id.btn_register);
        btnRegister.setOnClickListener(view -> registerUser());

        tologin.setOnClickListener(v -> {
            Intent intent = new Intent(Register.this, Signin.class);
            startActivity(intent);
        });
    }

    private void registerUser() {

        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmed = confpassword.getText().toString().trim();

        if (email.isEmpty() || name.isEmpty() || password.isEmpty() || confirmed.isEmpty()) {
            Toast.makeText(this, "Remplissez tous les champs", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!confirmed.equals(password)) {
            Toast.makeText(this, "Les mots de passe ne correspondent pas", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Set the user's display name
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(name)
                                    .build();

                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(profileTask -> {
                                        if (profileTask.isSuccessful()) {
                                            // Send email verification
                                            user.sendEmailVerification()
                                                    .addOnCompleteListener(emailTask -> {
                                                        if (emailTask.isSuccessful()) {
                                                            Toast.makeText(this, "Inscription réussie ! Vérifiez votre email.", Toast.LENGTH_LONG).show();
                                                            mAuth.signOut(); // Sign out to prevent login without verification
                                                            startActivity(new Intent(Register.this, Signin.class));
                                                            finish();
                                                        } else {
                                                            Toast.makeText(this, "Erreur lors de l'envoi de l'email de vérification.", Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(this, "Erreur : " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
