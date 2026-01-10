package com.example.anontalk.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anontalk.R;
import com.example.anontalk.activities.CommentsActivity;
import com.example.anontalk.models.PostModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    Context context;
    List<PostModel> postList;
    FirebaseFirestore db;
    FirebaseAuth auth;

    public PostAdapter(Context context, List<PostModel> postList) {
        this.context = context;
        this.postList = postList;
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {

        PostModel model = postList.get(position);

        holder.tvPostText.setText(model.getText());
        holder.tvComments.setText(String.valueOf(model.getCommentCount()));
        holder.tvTime.setText(getTimeAgo(model.getTimestamp()));

        String postId = model.getPostId();
        String userId = auth.getCurrentUser().getUid();

        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(userId);

        // 🔁 REALTIME LIKE COUNT UPDATE
        postRef.addSnapshotListener((snapshot, error) -> {
            if (snapshot != null && snapshot.exists()) {
                Long likes = snapshot.getLong("likeCount");
                if (likes == null) likes = 0L; // 🔥 IMPORTANT
                holder.tvLikes.setText(String.valueOf(likes));
            }
        });

        // ❤️ CHECK IF USER ALREADY LIKED
        likeRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
            } else {
                holder.btnLike.setImageResource(R.drawable.ic_like);
            }
        });

        // ❤️ LIKE BUTTON CLICK
        holder.btnLike.setOnClickListener(v -> toggleLike(postRef, likeRef, holder));

        // ❤️ DOUBLE TAP TO LIKE
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            private long lastClickTime = 0;

            @Override
            public void onClick(View v) {
                long clickTime = System.currentTimeMillis();
                if (clickTime - lastClickTime < 300) {
                    toggleLike(postRef, likeRef, holder);
                    animateHeart(holder.btnLike);
                }
                lastClickTime = clickTime;
            }
        });

        // 💬 COMMENTS CLICK
        holder.btnComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("postId", postId);
            context.startActivity(intent);
        });

        // 🔁 AUTO UPDATE COMMENT COUNT
        postRef.collection("comments")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        holder.tvComments.setText(String.valueOf(value.size()));
                    }
                });
    }

    // ❤️ LIKE TOGGLE (ONE USER = ONE LIKE)
    private void toggleLike(DocumentReference postRef, DocumentReference likeRef, PostViewHolder holder) {

        db.runTransaction(transaction -> {

            DocumentSnapshot postSnapshot = transaction.get(postRef);
            DocumentSnapshot likeSnapshot = transaction.get(likeRef);

            long likeCount = postSnapshot.getLong("likeCount") == null
                    ? 0
                    : postSnapshot.getLong("likeCount");

            if (likeSnapshot.exists()) {
                // ❌ UNLIKE
                transaction.delete(likeRef);
                transaction.update(postRef, "likeCount", likeCount - 1);
            } else {
                // ❤️ LIKE
                transaction.set(likeRef, new HashMap<>());
                transaction.update(postRef, "likeCount", likeCount + 1);
            }
            return null;

        }).addOnSuccessListener(unused -> {
            animateHeart(holder.btnLike);
        });
    }

    // ❤️ HEART POP ANIMATION
    private void animateHeart(ImageView heart) {
        heart.animate()
                .scaleX(1.5f).scaleY(1.5f)
                .setDuration(150)
                .withEndAction(() ->
                        heart.animate().scaleX(1f).scaleY(1f).setDuration(150));
    }

    // ⏱ TIME AGO FORMAT
    private String getTimeAgo(long time) {
        long now = System.currentTimeMillis();
        long diff = now - time;

        if (diff < 60000) return "Just now";
        if (diff < 3600000) return (diff / 60000) + " min ago";
        if (diff < 86400000) return (diff / 3600000) + " hr ago";
        return (diff / 86400000) + " days ago";
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView tvPostText, tvLikes, tvComments, tvTime;
        ImageView btnLike, btnComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            tvPostText = itemView.findViewById(R.id.tvPostText);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvComments = itemView.findViewById(R.id.tvComments);
            tvTime = itemView.findViewById(R.id.tvTime);

            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
        }
    }
}
