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

import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final List<PostModel> list;
    private final FirebaseFirestore db;
    private final String uid;

    // ✅ SINGLE-CONSTRUCTOR (MATCHES FeedFragment)
    public PostAdapter(List<PostModel> list) {
        this.list = list;
        this.db = FirebaseFirestore.getInstance();
        this.uid = FirebaseAuth.getInstance().getUid();
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

        // ❤️ LIKE BUTTON
        holder.btnLike.setOnClickListener(v -> {
            toggleLike(post, holder);
        });

        // 💬 COMMENT BUTTON
        holder.btnComment.setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CommentsActivity.class);
            intent.putExtra("postId", post.getId());
            v.getContext().startActivity(intent);
        });

        // 💬 LONG PRESS TO OPEN COMMENTS
        holder.itemView.setOnLongClickListener(v -> {
            Intent intent = new Intent(v.getContext(), CommentsActivity.class);
            intent.putExtra("postId", post.getId());
            v.getContext().startActivity(intent);
            return true;
        });

        // ✨ SMALL TAP ANIMATION
        holder.itemView.setOnClickListener(v -> {
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(v, "scaleX", 0.96f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(v, "scaleY", 0.96f, 1f);
            scaleX.setDuration(120);
            scaleY.setDuration(120);
            scaleX.start();
            scaleY.start();
        });

        // 🔢 LOAD COMMENT COUNT
        if (post.getId() != null) {
            db.collection("posts")
                    .document(post.getId())
                    .collection("comments")
                    .get()
                    .addOnSuccessListener(snapshots ->
                            holder.tvComments.setText(String.valueOf(snapshots.size()))
                    );
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    // ❤️ LIKE / UNLIKE LOGIC (SAFE)
    private void toggleLike(PostModel post, PostViewHolder holder) {

        if (post.getId() == null || uid == null) return;

        DocumentReference likeRef = db.collection("posts")
                .document(post.getId())
                .collection("likes")
                .document(uid);

        likeRef.get().addOnSuccessListener(snapshot -> {

            if (snapshot.exists()) {
                // 👎 UNLIKE
                likeRef.delete();
                db.collection("posts")
                        .document(post.getId())
                        .update("likes", FieldValue.increment(-1));

                holder.tvLikes.setText(String.valueOf(post.getLikes() - 1));
            } else {
                // 👍 LIKE
                likeRef.set(new Object());
                db.collection("posts")
                        .document(post.getId())
                        .update("likes", FieldValue.increment(1));

                holder.tvLikes.setText(String.valueOf(post.getLikes() + 1));
            }
        });
    }

    // 🔹 VIEW HOLDER
    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView tvText, tvComments, tvCategory, tvLikes;
        ImageView btnLike, btnComment;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            tvText = itemView.findViewById(R.id.tvText);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvComments = itemView.findViewById(R.id.tvComments);

            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
        }
    }
}
