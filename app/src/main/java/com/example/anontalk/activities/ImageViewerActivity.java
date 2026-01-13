package com.example.anontalk.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.anontalk.R;

public class ImageViewerActivity extends AppCompatActivity {

    ImageView imgFull;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
        );
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_image_viewer);

        imgFull = findViewById(R.id.imgFull);

        String imageUrl = getIntent().getStringExtra("image");

        Glide.with(this)
                .load(imageUrl)
                .into(imgFull);

        // Tap anywhere to close (X-style)
        imgFull.setOnClickListener(v -> finish());
    }
}
