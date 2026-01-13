package com.example.anontalk.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anontalk.R;
import com.example.anontalk.adapters.CommentAdapter;
import com.example.anontalk.models.CommentModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {

    RecyclerView rvComments;
    EditText etComment;
    ImageView btnSend, btnBack;

    FirebaseFirestore db;
    List<CommentModel> commentList = new ArrayList<>();
    CommentAdapter adapter;

    String postId;

    private static final long COMMENT_COOLDOWN_MS = 10_000; // 10 seconds
    private long lastCommentAt = 0;

    @Override
    protected void onCreate(@Nullable Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_comments);

        postId = getIntent().getStringExtra("postId");
        if (postId == null) {
            finish();
            return;
        }

        // 🔗 Views
        rvComments = findViewById(R.id.recyclerViewComments);
        etComment = findViewById(R.id.etComment);
        btnSend = findViewById(R.id.btnSend);
        btnBack = findViewById(R.id.btnBack);

        db = FirebaseFirestore.getInstance();

        // Recycler
        rvComments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CommentAdapter(this, commentList);
        rvComments.setAdapter(adapter);

        loadComments();

        btnSend.setOnClickListener(v -> sendComment());
        btnBack.setOnClickListener(v -> finish());
    }

    // 🔁 REAL-TIME LOAD
    private void loadComments() {
        db.collection("posts")
                .document(postId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null || value == null) return;

                    commentList.clear();
                    for (QueryDocumentSnapshot doc : value) {
                        CommentModel model = doc.toObject(CommentModel.class);
                        model.setCommentId(doc.getId());
                        commentList.add(model);
                    }

                    adapter.notifyDataSetChanged();
                    if (!commentList.isEmpty()) {
                        rvComments.scrollToPosition(commentList.size() - 1);
                    }
                });
    }

    // 🚫 ANTI-SPAM
    private boolean canSendComment() {
        long now = System.currentTimeMillis();
        if (now - lastCommentAt < COMMENT_COOLDOWN_MS) {
            long wait = (COMMENT_COOLDOWN_MS - (now - lastCommentAt)) / 1000;
            Toast.makeText(this,
                    "Please wait " + wait + "s before commenting again",
                    Toast.LENGTH_SHORT).show();
            return false;
        }
        lastCommentAt = now;
        return true;
    }

    // 💬 ADD COMMENT (ATOMIC)
    private void sendComment() {

        String comment = etComment.getText().toString().trim();
        if (comment.isEmpty()) return;
        if (!canSendComment()) return;

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference postRef =
                db.collection("posts").document(postId);
        DocumentReference commentRef =
                postRef.collection("comments").document();

        db.runTransaction(transaction -> {

            DocumentSnapshot postSnap = transaction.get(postRef);
            long count = postSnap.contains("commentCount")
                    ? postSnap.getLong("commentCount") : 0;

            HashMap<String, Object> map = new HashMap<>();
            map.put("text", comment);
            map.put("userId", userId);
            map.put("timestamp", FieldValue.serverTimestamp());
            map.put("edited", false);

            transaction.set(commentRef, map);
            transaction.update(postRef, "commentCount", count + 1);
            return null;

        }).addOnSuccessListener(unused -> {
            etComment.setText("");
        }).addOnFailureListener(e ->
                Toast.makeText(this,
                        "Failed to add comment",
                        Toast.LENGTH_SHORT).show()
        );
    }

    // 🗑 DELETE (OWNER ONLY)
    public void deleteComment(String commentId) {

        DocumentReference postRef =
                db.collection("posts").document(postId);
        DocumentReference commentRef =
                postRef.collection("comments").document(commentId);

        db.runTransaction(transaction -> {

            DocumentSnapshot postSnap = transaction.get(postRef);
            DocumentSnapshot commentSnap = transaction.get(commentRef);
            if (!commentSnap.exists()) return null;

            long count = postSnap.contains("commentCount")
                    ? postSnap.getLong("commentCount") : 0;

            transaction.delete(commentRef);
            transaction.update(postRef,
                    "commentCount", Math.max(count - 1, 0));
            return null;

        }).addOnSuccessListener(unused ->
                Toast.makeText(this,
                        "Comment deleted",
                        Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(this,
                        "Delete failed",
                        Toast.LENGTH_SHORT).show()
        );
    }

    // ✏️ EDIT (OWNER ONLY)
    public void editComment(String commentId, String newText) {

        if (newText == null || newText.trim().isEmpty()) return;
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;

        String myUid =
                FirebaseAuth.getInstance().getCurrentUser().getUid();

        DocumentReference commentRef =
                db.collection("posts")
                        .document(postId)
                        .collection("comments")
                        .document(commentId);

        db.runTransaction(transaction -> {

            DocumentSnapshot snap = transaction.get(commentRef);
            if (!snap.exists()) return null;

            String owner = snap.getString("userId");
            if (!myUid.equals(owner)) return null;

            HashMap<String, Object> updates = new HashMap<>();
            updates.put("text", newText);
            updates.put("edited", true);

            transaction.update(commentRef, updates);
            return null;

        }).addOnSuccessListener(unused ->
                Toast.makeText(this,
                        "Comment updated",
                        Toast.LENGTH_SHORT).show()
        ).addOnFailureListener(e ->
                Toast.makeText(this,
                        "Edit failed",
                        Toast.LENGTH_SHORT).show()
        );
    }
}
