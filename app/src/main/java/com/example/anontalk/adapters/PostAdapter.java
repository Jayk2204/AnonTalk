package com.example.anontalk.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anontalk.R;
import com.example.anontalk.activities.CommentsActivity;
import com.example.anontalk.activities.EditPostActivity;
import com.example.anontalk.models.FeedItem;
import com.example.anontalk.models.PollModel;
import com.example.anontalk.models.PostModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PostAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_POST = 0;
    private static final int TYPE_POLL = 1;

    Context context;
    List<FeedItem> feedList;
    FirebaseFirestore db;
    FirebaseAuth auth;

    public PostAdapter(Context context, List<FeedItem> feedList) {
        this.context = context;
        this.feedList = feedList;
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public int getItemViewType(int position) {
        if (feedList.get(position).getType().equals("POLL")) {
            return TYPE_POLL;
        }
        return TYPE_POST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_POLL) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_poll, parent, false);
            return new PollViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
            return new PostViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        FeedItem item = feedList.get(position);

        if (holder instanceof PostViewHolder) {
            bindPost((PostViewHolder) holder, item.getPost(), position);
        } else if (holder instanceof PollViewHolder) {
            bindPoll((PollViewHolder) holder, item.getPoll());
        }
    }

    // ==============================
    // 📌 POST BINDING (UNCHANGED)
    // ==============================
    private void bindPost(PostViewHolder holder, PostModel model, int position) {

        holder.tvPostText.setText(model.getText());
        holder.tvTime.setText(getTimeAgo(model.getTimestamp()));

        String postId = model.getPostId();

        if (auth.getCurrentUser() == null) return;
        String currentUserId = auth.getCurrentUser().getUid();

        DocumentReference postRef = db.collection("posts").document(postId);
        DocumentReference likeRef = postRef.collection("likes").document(currentUserId);

        holder.tvLikes.setText(String.valueOf(model.getLikeCount()));
        holder.tvComments.setText(String.valueOf(model.getCommentCount()));

        // 🖼 IMAGES
        if (model.getImages() != null && !model.getImages().isEmpty()) {

            holder.rvImages.setVisibility(View.VISIBLE);

            FeedImageAdapter imageAdapter =
                    new FeedImageAdapter(context, model.getImages());

            holder.rvImages.setLayoutManager(
                    new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));

            holder.rvImages.setAdapter(imageAdapter);

        } else {
            holder.rvImages.setVisibility(View.GONE);
        }

        // ❤️ CHECK LIKE
        likeRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
            } else {
                holder.btnLike.setImageResource(R.drawable.ic_like);
            }
        });

        // ❤️ LIKE CLICK
        holder.btnLike.setOnClickListener(v -> toggleLike(postRef, likeRef, holder, position));

        // 💬 COMMENTS
        holder.btnComment.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommentsActivity.class);
            intent.putExtra("postId", postId);
            context.startActivity(intent);
        });

        // 🗑️ EDIT / DELETE
        holder.itemView.setOnLongClickListener(v -> {

            String ownerId = model.getUserId();
            if (ownerId == null || !ownerId.equals(currentUserId)) return true;

            String[] options = {"Edit", "Delete"};

            new AlertDialog.Builder(context)
                    .setTitle("Post Options")
                    .setItems(options, (dialog, which) -> {

                        if (which == 0) {
                            Intent intent = new Intent(context, EditPostActivity.class);
                            intent.putExtra("postId", model.getPostId());
                            intent.putExtra("text", model.getText());
                            context.startActivity(intent);

                        } else {
                            deletePost(model.getPostId(), holder.getAdapterPosition());
                        }
                    })
                    .show();

            return true;
        });

        // 🔁 REALTIME COMMENTS
        postRef.collection("comments")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        holder.tvComments.setText(String.valueOf(value.size()));
                    }
                });

        // 🔁 REALTIME LIKES
        postRef.addSnapshotListener((snapshot, error) -> {
            if (snapshot != null && snapshot.exists()) {
                Long likes = snapshot.getLong("likeCount");
                if (likes == null) likes = 0L;
                holder.tvLikes.setText(String.valueOf(likes));
            }
        });
    }

    // ==============================
    // 🗳 POLL BINDING (FIXED, LOGIC SAME)
    // ==============================
    private void bindPoll(PollViewHolder holder, PollModel poll) {

        holder.tvQuestion.setText(poll.getQuestion());

        String pollId = poll.getPollId();
        String uid = auth.getCurrentUser().getUid();

        boolean isExpired = Timestamp.now().compareTo(poll.getExpiresAt()) > 0;
        holder.tvStatus.setVisibility(isExpired ? View.VISIBLE : View.GONE);
        holder.tvStatus.setText("Poll ended");

        DocumentReference pollRef = db.collection("polls").document(pollId);
        DocumentReference voteRef = pollRef.collection("votes").document(uid);

        // -----------------------------
        // 🔁 LISTEN: POLL TOTAL VOTES
        // -----------------------------
        pollRef.addSnapshotListener((pollSnap, e) -> {
            if (pollSnap == null || !pollSnap.exists()) return;

            Long totalVotesObj = pollSnap.getLong("totalVotes");
            if (totalVotesObj == null) totalVotesObj = 0L;

            holder.currentTotalVotes = totalVotesObj; // 🔥 REALTIME STORE
            holder.tvTotalVotes.setText(totalVotesObj + " votes");
        });

        // -----------------------------
        // 🔁 LISTEN: USER VOTE
        // -----------------------------
        voteRef.addSnapshotListener((voteSnap, e) -> {

            String votedOptionId = null;
            if (voteSnap != null && voteSnap.exists()) {
                votedOptionId = voteSnap.getString("optionId");
            }

            final String finalVotedOptionId = votedOptionId;

            // -----------------------------
            // 🔁 LISTEN: OPTIONS
            // -----------------------------
            pollRef.collection("options")
                    .addSnapshotListener((optionsSnap, error2) -> {

                        if (optionsSnap == null) return;

                        holder.optionsContainer.removeAllViews();

                        for (DocumentSnapshot doc : optionsSnap) {

                            String optionId = doc.getId();
                            String text = doc.getString("text");
                            Long votesObj = doc.getLong("votes");
                            if (votesObj == null) votesObj = 0L;
                            long votes = votesObj;

                            View optView = LayoutInflater.from(context)
                                    .inflate(R.layout.item_poll_option, holder.optionsContainer, false);

                            TextView tvOpt = optView.findViewById(R.id.tvOptionText);
                            TextView tvVotes = optView.findViewById(R.id.tvVotes);
                            TextView tvPercentage = optView.findViewById(R.id.tvPercentage);
                            TextView tvSelected = optView.findViewById(R.id.tvSelected);
                            ProgressBar progressBar = optView.findViewById(R.id.progressBar);

                            tvOpt.setText(text);
                            tvVotes.setText(votes + " votes");

                            // 🔥 FIX: model ke old data ke bajay realtime value
                            long totalVotes = holder.currentTotalVotes;
                            if (totalVotes == 0) {
                                totalVotes = votes; // avoid divide by zero UI glitch
                            }

                            int percent = totalVotes > 0
                                    ? (int) ((votes * 100) / totalVotes)
                                    : 0;

                            tvPercentage.setText(percent + "%");
                            progressBar.setMax(100);
                            progressBar.setProgress(percent);

                            // ✅ Highlight selected option
                            if (finalVotedOptionId != null && finalVotedOptionId.equals(optionId)) {
                                tvSelected.setVisibility(View.VISIBLE);
                                optView.setBackgroundResource(R.drawable.poll_selected_bg);
                            } else {
                                tvSelected.setVisibility(View.GONE);
                                optView.setBackgroundResource(R.drawable.poll_option_bg);
                            }

                            // 🗳 Click to vote
                            if (!isExpired) {
                                optView.setOnClickListener(v ->
                                        voteOnPoll(pollId, optionId)
                                );
                            }


                            holder.optionsContainer.addView(optView);
                        }
                    });
        });
    }

    // ==============================
// 🔁 ALLOW CHANGE VOTE (FIXED)
// ==============================
    private void voteOnPoll(String pollId, String optionId) {

        String uid = auth.getCurrentUser().getUid();

        DocumentReference pollRef = db.collection("polls").document(pollId);
        DocumentReference voteRef = pollRef
                .collection("votes")
                .document(uid);

        voteRef.get().addOnSuccessListener(snapshot -> {

            db.runTransaction(transaction -> {

                // 🔹 READ ALL REQUIRED DOCUMENTS FIRST

                DocumentSnapshot pollSnap = transaction.get(pollRef);
                Long totalVotes = pollSnap.getLong("totalVotes");
                if (totalVotes == null) totalVotes = 0L;

                String oldOptionId = null;
                if (snapshot.exists()) {
                    oldOptionId = snapshot.getString("optionId");
                }

                // If user taps the same option again → do nothing
                if (oldOptionId != null && oldOptionId.equals(optionId)) {
                    return null;
                }

                DocumentReference newOptionRef = pollRef
                        .collection("options")
                        .document(optionId);

                DocumentSnapshot newOptionSnap = transaction.get(newOptionRef);
                Long newVotes = newOptionSnap.getLong("votes");
                if (newVotes == null) newVotes = 0L;

                DocumentSnapshot oldOptionSnap = null;
                DocumentReference oldOptionRef = null;
                Long oldVotes = 0L;

                if (oldOptionId != null) {
                    oldOptionRef = pollRef
                            .collection("options")
                            .document(oldOptionId);
                    oldOptionSnap = transaction.get(oldOptionRef);
                    oldVotes = oldOptionSnap.getLong("votes");
                    if (oldVotes == null) oldVotes = 0L;
                }

                // 🔹 NOW DO ALL WRITES

                if (oldOptionId != null) {
                    // 🔽 Decrease old option
                    transaction.update(oldOptionRef, "votes", Math.max(oldVotes - 1, 0));
                } else {
                    // 🆕 First time vote → increase totalVotes
                    transaction.update(pollRef, "totalVotes", totalVotes + 1);
                }

                // 🔼 Increase new option
                transaction.update(newOptionRef, "votes", newVotes + 1);

                // 🧾 Save / update user vote
                Map<String, Object> voteMap = new HashMap<>();
                voteMap.put("optionId", optionId);
                voteMap.put("votedAt", Timestamp.now());
                transaction.set(voteRef, voteMap);

                return null;

            }).addOnSuccessListener(unused ->
                    Toast.makeText(context, "Vote updated", Toast.LENGTH_SHORT).show()
            ).addOnFailureListener(e ->
                    Toast.makeText(context, "Vote failed: " + e.getMessage(), Toast.LENGTH_LONG).show()
            );
        });
    }



    // ==============================
    // ❤️ LIKE TOGGLE
    // ==============================
    private void toggleLike(DocumentReference postRef, DocumentReference likeRef,
                            PostViewHolder holder, int position) {

        db.runTransaction(transaction -> {

            DocumentSnapshot postSnapshot = transaction.get(postRef);
            DocumentSnapshot likeSnapshot = transaction.get(likeRef);

            long likeCount = postSnapshot.contains("likeCount")
                    ? postSnapshot.getLong("likeCount")
                    : 0;

            if (likeSnapshot.exists()) {
                transaction.delete(likeRef);
                transaction.update(postRef, "likeCount", Math.max(likeCount - 1, 0));
            } else {
                transaction.set(likeRef, new HashMap<>());
                transaction.update(postRef, "likeCount", likeCount + 1);
            }
            return null;

        }).addOnSuccessListener(unused -> notifyItemChanged(position));
    }

    // 🗑 DELETE
    private void deletePost(String postId, int position) {

        db.collection("posts")
                .document(postId)
                .delete()
                .addOnSuccessListener(unused -> {
                    feedList.remove(position);
                    notifyItemRemoved(position);
                    Toast.makeText(context, "Post deleted", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(context, "Delete failed", Toast.LENGTH_SHORT).show()
                );
    }

    // ⏱ TIME FORMAT
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
        return feedList.size();
    }

    // ==============================
    // 🧩 VIEW HOLDERS
    // ==============================
    static class PostViewHolder extends RecyclerView.ViewHolder {

        TextView tvPostText, tvLikes, tvComments, tvTime;
        ImageView btnLike, btnComment;
        RecyclerView rvImages;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);

            tvPostText = itemView.findViewById(R.id.tvPostText);
            tvLikes = itemView.findViewById(R.id.tvLikes);
            tvComments = itemView.findViewById(R.id.tvComments);
            tvTime = itemView.findViewById(R.id.tvTime);

            btnLike = itemView.findViewById(R.id.btnLike);
            btnComment = itemView.findViewById(R.id.btnComment);
            rvImages = itemView.findViewById(R.id.rvPostImages);
        }
    }

    static class PollViewHolder extends RecyclerView.ViewHolder {

        TextView tvQuestion, tvTotalVotes, tvStatus;
        LinearLayout optionsContainer;

        long currentTotalVotes = 0; // 🔥 REALTIME STORAGE

        public PollViewHolder(@NonNull View itemView) {
            super(itemView);

            tvQuestion = itemView.findViewById(R.id.tvPollQuestion);
            tvTotalVotes = itemView.findViewById(R.id.tvTotalVotes);
            tvStatus = itemView.findViewById(R.id.tvPollStatus);
            optionsContainer = itemView.findViewById(R.id.optionsContainer);
        }
    }
}
