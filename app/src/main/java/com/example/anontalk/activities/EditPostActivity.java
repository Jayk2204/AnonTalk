package com.example.anontalk.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.anontalk.R;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;

public class EditPostActivity extends AppCompatActivity {

    EditText etEditPost;
    Button btnUpdate;

    String postId;
    String oldText;

    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        etEditPost = findViewById(R.id.etEditPost);
        btnUpdate = findViewById(R.id.btnUpdatePost);

        db = FirebaseFirestore.getInstance();

        // 🔁 Get data from intent
        postId = getIntent().getStringExtra("postId");
        oldText = getIntent().getStringExtra("text");

        if (postId == null) {
            Toast.makeText(this, "Invalid post", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etEditPost.setText(oldText);
        etEditPost.setSelection(etEditPost.getText().length());

        btnUpdate.setOnClickListener(v -> updatePost());
    }

    // 🔥 UPDATE POST IN FIRESTORE
    private void updatePost() {
        String newText = etEditPost.getText().toString().trim();

        if (newText.isEmpty()) {
            Toast.makeText(this, "Post cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (newText.equals(oldText)) {
            Toast.makeText(this, "No changes made", Toast.LENGTH_SHORT).show();
            return;
        }

        HashMap<String, Object> map = new HashMap<>();
        map.put("text", newText);

        db.collection("posts")
                .document(postId)
                .update(map)
                .addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Post updated", Toast.LENGTH_SHORT).show();
                    finish(); // go back to feed
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
    }
}
