package com.example.myapplication;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;

public class Splash extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash); // Add splash screen layout

        // Handler to delay the transition to the Signin activity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                // Transition to the Signin activity
                Intent intent = new Intent(Splash.this, Signin.class);
                startActivity(intent);
                finish(); // Close the SplashActivity
            }
        }, 3000); // 3-second delay
    }
}
