package com.example.anontalk.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.anontalk.R;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class PostActivity extends AppCompatActivity {

    private TextInputEditText etConfession;
    private ChipGroup chipGroupCategory;
    private Button btnSubmit;
    private ProgressBar progressPost;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post);

        // Views
        etConfession = findViewById(R.id.etConfession);
        chipGroupCategory = findViewById(R.id.chipGroupCategory);
        btnSubmit = findViewById(R.id.btnSubmit);
        progressPost = findViewById(R.id.progressPost);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Submit click
        btnSubmit.setOnClickListener(v -> {
            // Small premium press animation
            v.animate().scaleX(0.96f).scaleY(0.96f).setDuration(80).withEndAction(() -> {
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start();
            }).start();

            submitConfession();
        });
    }

    private void submitConfession() {
        String confessionText = etConfession.getText() != null
                ? etConfession.getText().toString().trim()
                : "";

        if (confessionText.isEmpty()) {
            Toast.makeText(this, "Please write something...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Get selected chip
        int checkedId = chipGroupCategory.getCheckedChipId();
        if (checkedId == View.NO_ID) {
            Toast.makeText(this, "Please select a category", Toast.LENGTH_SHORT).show();
            return;
        }

        Chip selectedChip = findViewById(checkedId);
        String category = selectedChip.getText().toString();

        progressPost.setVisibility(View.VISIBLE);
        btnSubmit.setEnabled(false);

        Map<String, Object> post = new HashMap<>();
        post.put("userId", auth.getUid());
        post.put("text", confessionText);
        post.put("category", category);
        post.put("likes", 0);
        post.put("timestamp", FieldValue.serverTimestamp());

        db.collection("posts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    progressPost.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);

                    Toast.makeText(PostActivity.this,
                            "Confession posted anonymously 🔥",
                            Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    progressPost.setVisibility(View.GONE);
                    btnSubmit.setEnabled(true);

                    Toast.makeText(PostActivity.this,
                            "Failed: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }
}
