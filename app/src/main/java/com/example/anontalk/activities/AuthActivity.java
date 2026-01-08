package com.example.anontalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.anontalk.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AuthActivity extends AppCompatActivity {

    private Button btnAnon;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        btnAnon = findViewById(R.id.btnAnonLogin);
        progressBar = findViewById(R.id.progressAuth);

        auth = FirebaseAuth.getInstance();

        // If already logged in → go directly to Home
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(AuthActivity.this, HomeActivity.class));
            finish();
            return;
        }

        btnAnon.setOnClickListener(v -> signInAnonymously());
    }

    private void signInAnonymously() {
        progressBar.setVisibility(View.VISIBLE);
        btnAnon.setEnabled(false);

        auth.signInAnonymously()
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnAnon.setEnabled(true);

                    if (task.isSuccessful()) {
                        startActivity(new Intent(AuthActivity.this, HomeActivity.class));
                        finish();
                    } else {
                        String error = task.getException() != null ?
                                task.getException().getMessage() : "Unknown error";

                        Toast.makeText(AuthActivity.this,
                                "Login failed: " + error,
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

}
