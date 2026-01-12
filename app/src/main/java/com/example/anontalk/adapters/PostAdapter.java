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
    // 🗳 POLL BINDING
    // ==============================
    private void bindPoll(PollViewHolder holder, PollModel poll) {

        holder.tvQuestion.setText(poll.getQuestion());

        String pollId = poll.getPollId();
        String uid = auth.getCurrentUser().getUid();

        boolean isExpired = Timestamp.now().compareTo(poll.getExpiresAt()) > 0;
        if (isExpired) {
            holder.tvStatus.setVisibility(View.VISIBLE);
            holder.tvStatus.setText("Poll ended");
        } else {
            holder.tvStatus.setVisibility(View.GONE);
        }

        // 🔁 REALTIME LISTENER FOR POLL (to get updated totalVotes)
        db.collection("polls")
                .document(pollId)
                .addSnapshotListener((pollSnap, error) -> {

                    if (pollSnap == null || !pollSnap.exists()) return;
                    Long totalVotesObj = pollSnap.getLong("totalVotes");
                    if (totalVotesObj == null) totalVotesObj = 0L;

// 🔒 Make it final for lambda usage
                    final long totalVotes = totalVotesObj;


                    holder.tvTotalVotes.setText(totalVotes + " votes");

                    // 🔎 CHECK USER VOTE
                    db.collection("polls")
                            .document(pollId)
                            .collection("votes")
                            .document(uid)
                            .get()
                            .addOnSuccessListener(voteSnap -> {

                                String votedOptionId = null;
                                if (voteSnap.exists()) {
                                    votedOptionId = voteSnap.getString("optionId");
                                }

                                final String finalVotedOptionId = votedOptionId;

                                // 🔁 LOAD OPTIONS (REALTIME)
                                db.collection("polls")
                                        .document(pollId)
                                        .collection("options")
                                        .addSnapshotListener((value, error2) -> {

                                            if (value == null) return;

                                            holder.optionsContainer.removeAllViews();

                                            for (DocumentSnapshot doc : value) {

                                                String optionId = doc.getId();
                                                String text = doc.getString("text");
                                                Long votes = doc.getLong("votes");
                                                if (votes == null) votes = 0L;

                                                View optView = LayoutInflater.from(context)
                                                        .inflate(R.layout.item_poll_option, holder.optionsContainer, false);

                                                TextView tvOpt = optView.findViewById(R.id.tvOptionText);
                                                TextView tvVotes = optView.findViewById(R.id.tvVotes);
                                                TextView tvPercentage = optView.findViewById(R.id.tvPercentage);
                                                TextView tvSelected = optView.findViewById(R.id.tvSelected);
                                                ProgressBar progressBar = optView.findViewById(R.id.progressBar);

                                                tvOpt.setText(text);
                                                tvVotes.setText(votes + " votes");

                                                int percent = totalVotes > 0
                                                        ? (int) ((votes * 100) / totalVotes)
                                                        : 0;

                                                tvPercentage.setText(percent + "%");

                                                // 🎯 Animate progress bar correctly
                                                progressBar.setProgress(0);
                                                progressBar.setMax(100);
                                                progressBar.setProgress(percent);

                                                // ✅ Highlight voted option
                                                if (finalVotedOptionId != null && finalVotedOptionId.equals(optionId)) {
                                                    tvSelected.setVisibility(View.VISIBLE);
                                                    optView.setBackgroundResource(R.drawable.poll_selected_bg);
                                                } else {
                                                    tvSelected.setVisibility(View.GONE);
                                                    optView.setBackgroundResource(R.drawable.poll_option_bg);
                                                }

                                                // 🛑 Disable voting if expired or already voted
                                                if (!isExpired && finalVotedOptionId == null) {
                                                    optView.setOnClickListener(v ->
                                                            voteOnPoll(pollId, optionId)
                                                    );
                                                }

                                                holder.optionsContainer.addView(optView);
                                            }
                                        });
                            });
                });
    }



    // ==============================
    // 🔐 ONE USER = ONE VOTE
    // ==============================
    private void voteOnPoll(String pollId, String optionId) {

        String uid = auth.getCurrentUser().getUid();

        DocumentReference voteRef = db.collection("polls")
                .document(pollId)
                .collection("votes")
                .document(uid);

        voteRef.get().addOnSuccessListener(snapshot -> {
            if (snapshot.exists()) {
                Toast.makeText(context, "You already voted", Toast.LENGTH_SHORT).show();
            } else {

                Map<String, Object> voteMap = new HashMap<>();
                voteMap.put("optionId", optionId);
                voteMap.put("votedAt", Timestamp.now());

                voteRef.set(voteMap);

                DocumentReference optionRef = db.collection("polls")
                        .document(pollId)
                        .collection("options")
                        .document(optionId);

                db.runTransaction(transaction -> {

                    Long votes = transaction.get(optionRef).getLong("votes");
                    if (votes == null) votes = 0L;
                    transaction.update(optionRef, "votes", votes + 1);

                    DocumentReference pollRef = db.collection("polls").document(pollId);
                    Long totalVotes = transaction.get(pollRef).getLong("totalVotes");
                    if (totalVotes == null) totalVotes = 0L;
                    transaction.update(pollRef, "totalVotes", totalVotes + 1);

                    return null;
                });

                Toast.makeText(context, "Vote submitted", Toast.LENGTH_SHORT).show();
            }
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

        public PollViewHolder(@NonNull View itemView) {
            super(itemView);

            tvQuestion = itemView.findViewById(R.id.tvPollQuestion);
            tvTotalVotes = itemView.findViewById(R.id.tvTotalVotes);
            tvStatus = itemView.findViewById(R.id.tvPollStatus);
            optionsContainer = itemView.findViewById(R.id.optionsContainer);
        }
    }
}
