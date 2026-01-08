package com.example.anontalk.adapters;

import android.animation.ObjectAnimator;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<PostModel> list;
    private FirebaseFirestore db;
    private String uid;

    public PostAdapter(List<PostModel> list) {
        this.list = list;
        db = FirebaseFirestore.getInstance();
        uid = FirebaseAuth.getInstance().getUid();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {

        PostModel post = list.get(position);

        holder.tvText.setText(post.getText());
        holder.tvCategory.setText(post.getCategory());
        holder.tvLikes.setText(String.valueOf(post.getLikes()));

        // 🔥 LIKE BUTTON CLICK
        holder.btnLike.setOnClickListener(v -> {
            toggleLike(post.getId(), holder, post);
        });

        holder.itemView.setOnLongClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CommentsActivity.class);
            intent.putExtra("postId", post.getId());
            v.getContext().startActivity(intent);
            return true;
        });
        holder.btnComment.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CommentsActivity.class);
            intent.putExtra("postId", post.getId());
            v.getContext().startActivity(intent);
        });



        // ✨ DOUBLE TAP TO LIKE (GEN-Z STYLE)
        holder.itemView.setOnClickListener(v -> {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 0.95f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 0.95f, 1f);
            scaleX.setDuration(120);
            scaleY.setDuration(120);
            scaleX.start();
            scaleY.start();
        });
        FirebaseFirestore.getInstance()
                .collection("posts")
                .document(post.getId())
                .collection("comments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    holder.tvComments.setText(String.valueOf(count));
                });

    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ❤️ Like / Unlike Logic
    private void toggleLike(String postId, PostViewHolder holder, PostModel post) {

        if (postId == null) {
            return; // 🚫 Prevent crash
        }

        DocumentReference likeRef = db.collection("posts")
                .document(postId)
                .collection("likes")
                .document(uid);

        likeRef.get().addOnSuccessListener(snapshot -> {

            if (snapshot.exists()) {
                // 👎 Unlike
                likeRef.delete();
                db.collection("posts").document(postId)
                        .update("likes", FieldValue.increment(-1));

                holder.tvLikes.setText("❤️ " + (post.getLikes() - 1));
            } else {
                // 👍 Like
                likeRef.set(new HashMap<>());
                db.collection("posts").document(postId)
                        .update("likes", FieldValue.increment(1));

                holder.tvLikes.setText("❤️ " + (post.getLikes() + 1));
            }
        });
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView tvText,tvComments, tvCategory, tvLikes;
        ImageView btnLike,btnComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            tvText = itemView.findViewById(R.id.tvText);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            tvComments = itemView.findViewById(R.id.tvComments); // 🔥 THIS WAS NULl


        }
    }
}
