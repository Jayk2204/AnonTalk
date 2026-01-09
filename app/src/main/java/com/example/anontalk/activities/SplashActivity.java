package com.example.anontalk.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.anontalk.R;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView icon = findViewById(R.id.imgSplashIcon);

        // Story icons (order matters)
        int[] icons = {
                R.drawable.ic_talk,          // Talk
                R.drawable.ic_conffesion,    // Confession
                R.drawable.ic_dark_humor,    // Dark humor
                R.drawable.ic_problem,       // Real-life problems
                R.drawable.ic_splash_logo    // Final AnonTalk logo
        };

        Animation anim = AnimationUtils.loadAnimation(this, R.anim.fade_scale);
        Handler handler = new Handler(Looper.getMainLooper());

        long stepDelay = 400; // ms between each symbol

        for (int i = 0; i < icons.length; i++) {
            int index = i;
            handler.postDelayed(() -> {
                icon.setImageResource(icons[index]);
                icon.setAlpha(1f);
                icon.startAnimation(anim);
            }, i * stepDelay);
        }

        // Move to Auth after full story
        handler.postDelayed(() -> {
            startActivity(new Intent(
                    SplashActivity.this,
                    AuthActivity.class
            ));
            finish();
        }, icons.length * stepDelay + 200);
    }
}
