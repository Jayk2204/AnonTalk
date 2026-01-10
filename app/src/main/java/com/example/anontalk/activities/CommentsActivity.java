package com.example.anontalk.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anontalk.R;
import com.example.anontalk.adapters.CommentAdapter;
import com.example.anontalk.models.CommentModel;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    EditText etComment;
    Button btnSend;

    FirebaseFirestore db;
    List<CommentModel> commentList = new ArrayList<>();
    CommentAdapter adapter;

    String postId;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_comments);

        postId = getIntent().getStringExtra("postId");

        recyclerView = findViewById(R.id.recyclerViewComments);
        etComment = findViewById(R.id.etComment);
        btnSend = findViewById(R.id.btnSend);

        db = FirebaseFirestore.getInstance();

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommentAdapter(this, commentList);
        recyclerView.setAdapter(adapter);

        loadComments();

        btnSend.setOnClickListener(v -> sendComment());
    }

    private void loadComments() {
        db.collection("Posts").document(postId).collection("Comments")
                .addSnapshotListener((value, error) -> {
                    commentList.clear();
                    for (var doc : value.getDocuments()) {
                        CommentModel model = doc.toObject(CommentModel.class);
                        commentList.add(model);
                    }
                    adapter.notifyDataSetChanged();
                });
    }

    private void sendComment() {
        String comment = etComment.getText().toString().trim();
        if (comment.isEmpty()) return;

        HashMap<String, Object> map = new HashMap<>();
        map.put("text", comment);
        map.put("timestamp", System.currentTimeMillis());

        db.collection("Posts").document(postId)
                .collection("Comments")
                .add(map)
                .addOnSuccessListener(unused -> {
                    etComment.setText("");
                    Toast.makeText(this, "Comment added", Toast.LENGTH_SHORT).show();
                });
    }
}
