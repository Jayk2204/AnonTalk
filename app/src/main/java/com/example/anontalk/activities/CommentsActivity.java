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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CommentsActivity extends AppCompatActivity {

    private EditText etComment;
    private Button btnSend;
    private RecyclerView rvComments;

    private String postId;
    private FirebaseFirestore db;
    private List<CommentModel> list;
    private CommentAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        etComment = findViewById(R.id.etComment);
        btnSend = findViewById(R.id.btnSend);
        rvComments = findViewById(R.id.rvComments);

        rvComments.setLayoutManager(new LinearLayoutManager(this));

        list = new ArrayList<>();
        adapter = new CommentAdapter(list);
        rvComments.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        postId = getIntent().getStringExtra("postId");

        loadComments();

        btnSend.setOnClickListener(v -> sendComment());
    }

    private void sendComment() {
        String text = etComment.getText().toString().trim();
        if (text.isEmpty()) return;

        Map<String, Object> map = new HashMap<>();
        map.put("text", text);
        map.put("userId", FirebaseAuth.getInstance().getUid());
        map.put("timestamp", System.currentTimeMillis());

        db.collection("posts").document(postId)
                .collection("comments")
                .add(map);

        etComment.setText("");
    }

    private void loadComments() {
        db.collection("posts").document(postId)
                .collection("comments")
                .addSnapshotListener((value, error) -> {
                    if (value == null) return;
                    list.clear();
                    for (var doc : value.getDocuments()) {
                        CommentModel c = doc.toObject(CommentModel.class);
                        if (c != null) list.add(c);
                    }
                    adapter.notifyDataSetChanged();
                });
    }
}
