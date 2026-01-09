package com.example.anontalk.activities;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;

import com.example.anontalk.R;

public class TermsActivity extends AppCompatActivity {

    private CheckBox cbTerms;
    private CheckBox cbPrivacy;
    private Button btnAccept;
    private TextView tvPrivacyLink;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_terms);

        // Bind views
        cbTerms = findViewById(R.id.cbAccept);          // Terms checkbox
        cbPrivacy = findViewById(R.id.cbPrivacyRead);  // Privacy checkbox
        btnAccept = findViewById(R.id.btnAccept);
        tvPrivacyLink = findViewById(R.id.tvPrivacyLink);

        // Button disabled by default
        btnAccept.setEnabled(false);

        // Common listener for both checkboxes
        CompoundButton.OnCheckedChangeListener checkListener =
                (buttonView, isChecked) -> {
                    btnAccept.setEnabled(
                            cbTerms.isChecked() && cbPrivacy.isChecked()
                    );
                };

        cbTerms.setOnCheckedChangeListener(checkListener);
        cbPrivacy.setOnCheckedChangeListener(checkListener);

        // Open Privacy Policy screen
        tvPrivacyLink.setOnClickListener(v -> {
            startActivity(new Intent(
                    TermsActivity.this,
                    PrivacyPolicyActivity.class
            ));
        });

        // Accept both Terms + Privacy
        btnAccept.setOnClickListener(v -> {
            SharedPreferences prefs =
                    getSharedPreferences("APP_PREFS", MODE_PRIVATE);

            prefs.edit()
                    .putBoolean("TERMS_ACCEPTED", true)
                    .apply();

            startActivity(new Intent(
                    TermsActivity.this,
                    AuthActivity.class
            ));
            finish();
        });

        // 🚫 Block back button & gesture
        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        finishAffinity();
                    }
                });
    }
}
