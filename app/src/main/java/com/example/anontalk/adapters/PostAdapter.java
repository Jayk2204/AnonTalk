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
        return feedList.get(position).getType().equals("POLL")
                ? TYPE_POLL : TYPE_POST;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        if (viewType == TYPE_POLL) {
            View v = LayoutInflater.from(context)
                    .inflate(R.layout.item_poll, parent, false);
            return new PollViewHolder(v);
        } else {
            View v = LayoutInflater.from(context)
                    .inflate(R.layout.item_post, parent, false);
            return new PostViewHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder, int position) {

        FeedItem item = feedList.get(position);

        if (holder instanceof PostViewHolder) {
            bindPost((PostViewHolder) holder, item.getPost(), position);
        } else {
            bindPoll((PollViewHolder) holder, item.getPoll());
        }
    }

    // ==============================
    // 📌 POST BINDING (FIXED HEADER)
    // ==============================
    private void bindPost(PostViewHolder holder, PostModel model, int position) {

        holder.tvPostText.setText(model.getText());

        String timeAgo = getTimeAgo(model.getTimestamp());
        holder.tvHeader.setText("Anonymous · " + timeAgo);

        String postId = model.getPostId();
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();

        DocumentReference postRef =
                db.collection("posts").document(postId);
        DocumentReference likeRef =
                postRef.collection("likes").document(uid);

        holder.tvLikes.setText(String.valueOf(model.getLikeCount()));
        holder.tvComments.setText(String.valueOf(model.getCommentCount()));

        if (model.getImages() != null && !model.getImages().isEmpty()) {
            holder.rvImages.setVisibility(View.VISIBLE);
            holder.rvImages.setLayoutManager(
                    new LinearLayoutManager(
                            context,
                            LinearLayoutManager.HORIZONTAL,
                            false
                    ));
            holder.rvImages.setAdapter(
                    new FeedImageAdapter(context, model.getImages()));
        } else {
            holder.rvImages.setVisibility(View.GONE);
        }

        likeRef.get().addOnSuccessListener(doc ->
                holder.btnLike.setImageResource(
                        doc.exists()
                                ? R.drawable.ic_heart_filled
                                : R.drawable.ic_like));

        holder.btnLike.setOnClickListener(v ->
                toggleLike(postRef, likeRef, holder, position));

        holder.btnComment.setOnClickListener(v -> {
            Intent i = new Intent(context, CommentsActivity.class);
            i.putExtra("postId", postId);
            context.startActivity(i);
        });

        holder.itemView.setOnLongClickListener(v -> {

            if (!uid.equals(model.getUserId())) return true;

            String[] options = {"Edit", "Delete"};

            new AlertDialog.Builder(context)
                    .setItems(options, (d, w) -> {
                        if (w == 0) {
                            Intent i =
                                    new Intent(context,
                                            EditPostActivity.class);
                            i.putExtra("postId", postId);
                            i.putExtra("text", model.getText());
                            context.startActivity(i);
                        } else {
                            deletePost(postId,
                                    holder.getAdapterPosition());
                        }
                    }).show();
            return true;
        });

        postRef.collection("comments")
                .addSnapshotListener((v, e) -> {
                    if (v != null)
                        holder.tvComments
                                .setText(String.valueOf(v.size()));
                });

        postRef.addSnapshotListener((s, e) -> {
            if (s != null && s.exists()) {
                Long likes = s.getLong("likeCount");
                holder.tvLikes.setText(
                        String.valueOf(likes == null ? 0 : likes));
            }

            holder.itemView.setOnClickListener(v -> {
                Intent i = new Intent(context, CommentsActivity.class);
                i.putExtra("postId", postId);
                context.startActivity(i);
            });

        });
    }

    // ==============================
    // 🗳 POLL BINDING (UNCHANGED LOGIC)
    // ==============================
    private void bindPoll(PollViewHolder holder, PollModel poll) {

        holder.tvQuestion.setText(poll.getQuestion());
        holder.optionsContainer.removeAllViews();

        boolean expired =
                Timestamp.now().compareTo(poll.getExpiresAt()) > 0;

        holder.tvStatus.setVisibility(expired ? View.VISIBLE : View.GONE);
        holder.tvStatus.setText("Poll ended");
        holder.optionsContainer.setAlpha(expired ? 0.6f : 1f);

        String pollId = poll.getPollId();
        if (auth.getCurrentUser() == null) return;
        String uid = auth.getCurrentUser().getUid();


        DocumentReference pollRef =
                db.collection("polls").document(pollId);
        DocumentReference voteRef =
                pollRef.collection("votes").document(uid);

        pollRef.addSnapshotListener((snap, e) -> {
            if (snap == null) return;
            Long total = snap.getLong("totalVotes");
            holder.currentTotalVotes = total == null ? 0 : total;
            holder.tvTotalVotes.setText(
                    holder.currentTotalVotes + " votes");
        });

        voteRef.get().addOnSuccessListener(voteSnap -> {

            String votedId =
                    voteSnap != null
                            ? voteSnap.getString("optionId")
                            : null;

            pollRef.collection("options").get()
                    .addOnSuccessListener(opts -> {

                        for (DocumentSnapshot d : opts) {

                            String optionId = d.getId();
                            String text = d.getString("text");
                            Long votesObj = d.getLong("votes");
                            long votes = votesObj == null ? 0 : votesObj;

                            View opt =
                                    LayoutInflater.from(context)
                                            .inflate(
                                                    R.layout.item_poll_option,
                                                    holder.optionsContainer,
                                                    false);

                            TextView tvOpt =
                                    opt.findViewById(R.id.tvOptionText);
                            TextView tvVotes =
                                    opt.findViewById(R.id.tvVotes);
                            TextView tvPct =
                                    opt.findViewById(R.id.tvPercentage);
                            TextView tvSel =
                                    opt.findViewById(R.id.tvSelected);
                            ProgressBar pb =
                                    opt.findViewById(R.id.progressBar);

                            tvOpt.setText(text);
                            tvVotes.setText(votes + " votes");

                            long total = holder.currentTotalVotes;
                            int pct = total > 0
                                    ? (int) ((votes * 100) / total)
                                    : 0;

                            tvPct.setText(pct + "%");

                            pb.setProgress(0);
                            pb.animate()
                                    .setDuration(250)
                                    .withEndAction(() ->
                                            pb.setProgress(pct))
                                    .start();

                            if (optionId.equals(votedId)) {
                                tvSel.setVisibility(View.VISIBLE);
                                opt.setBackgroundResource(
                                        R.drawable.poll_selected_bg);
                            }

                            if (!expired) {
                                opt.setOnClickListener(v ->
                                        voteOnPoll(pollId, optionId));
                            }

                            holder.optionsContainer.addView(opt);
                        }
                    });
        });
    }

    // ==============================
    // 🔁 VOTE LOGIC (UNCHANGED)
    // ==============================
    private void voteOnPoll(String pollId, String optionId) {

        String uid = auth.getCurrentUser().getUid();
        DocumentReference pollRef =
                db.collection("polls").document(pollId);
        DocumentReference voteRef =
                pollRef.collection("votes").document(uid);

        voteRef.get().addOnSuccessListener(snapshot -> {

            db.runTransaction(tx -> {

                DocumentSnapshot pollSnap = tx.get(pollRef);
                Long total = pollSnap.getLong("totalVotes");
                if (total == null) total = 0L;

                String oldOpt =
                        snapshot.exists()
                                ? snapshot.getString("optionId")
                                : null;

                if (optionId.equals(oldOpt)) return null;

                DocumentReference newRef =
                        pollRef.collection("options")
                                .document(optionId);

                DocumentSnapshot newSnap = tx.get(newRef);
                Long newVotes = newSnap.getLong("votes");
                if (newVotes == null) newVotes = 0L;

                if (oldOpt != null) {
                    DocumentReference oldRef =
                            pollRef.collection("options")
                                    .document(oldOpt);
                    DocumentSnapshot oldSnap = tx.get(oldRef);
                    Long oldVotes = oldSnap.getLong("votes");
                    tx.update(oldRef,
                            "votes",
                            Math.max((oldVotes == null ? 0 : oldVotes) - 1, 0));
                } else {
                    tx.update(pollRef,
                            "totalVotes", total + 1);
                }

                tx.update(newRef,
                        "votes", newVotes + 1);

                Map<String, Object> map = new HashMap<>();
                map.put("optionId", optionId);
                map.put("votedAt", Timestamp.now());
                tx.set(voteRef, map);

                return null;
            });
        });
    }

    // ==============================
    // ❤️ LIKE LOGIC (UNCHANGED)
    // ==============================
    private void toggleLike(
            DocumentReference postRef,
            DocumentReference likeRef,
            PostViewHolder holder,
            int position) {

        db.runTransaction(tx -> {

            DocumentSnapshot postSnap = tx.get(postRef);
            long count =
                    postSnap.contains("likeCount")
                            ? postSnap.getLong("likeCount")
                            : 0;

            if (tx.get(likeRef).exists()) {
                tx.delete(likeRef);
                tx.update(postRef,
                        "likeCount",
                        Math.max(count - 1, 0));
            } else {
                tx.set(likeRef, new HashMap<>());
                tx.update(postRef,
                        "likeCount", count + 1);
            }
            return null;

        }).addOnSuccessListener(v ->
                notifyItemChanged(position));
    }

    private void deletePost(String postId, int pos) {
        db.collection("posts")
                .document(postId)
                .delete()
                .addOnSuccessListener(v -> {
                    feedList.remove(pos);
                    notifyItemRemoved(pos);
                    Toast.makeText(context,
                            "Post deleted",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private String getTimeAgo(long t) {
        long d = System.currentTimeMillis() - t;
        if (d < 60000) return "Just now";
        if (d < 3600000) return (d / 60000) + " min ago";
        if (d < 86400000) return (d / 3600000) + " hr ago";
        return (d / 86400000) + " days ago";
    }

    @Override
    public int getItemCount() {
        return feedList.size();
    }

    // ==============================
    // VIEW HOLDERS
    // ==============================
    static class PostViewHolder extends RecyclerView.ViewHolder {
        TextView tvPostText, tvLikes, tvComments, tvHeader;
        ImageView btnLike, btnComment;
        RecyclerView rvImages;

        PostViewHolder(View v) {
            super(v);
            tvPostText = v.findViewById(R.id.tvPostText);
            tvLikes = v.findViewById(R.id.tvLikes);
            tvComments = v.findViewById(R.id.tvComments);
            tvHeader = v.findViewById(R.id.tvHeader);
            btnLike = v.findViewById(R.id.btnLike);
            btnComment = v.findViewById(R.id.btnComment);
            rvImages = v.findViewById(R.id.rvPostImages);
        }
    }

    static class PollViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuestion, tvTotalVotes, tvStatus;
        LinearLayout optionsContainer;
        long currentTotalVotes = 0;

        PollViewHolder(View v) {
            super(v);
            tvQuestion = v.findViewById(R.id.tvPollQuestion);
            tvTotalVotes = v.findViewById(R.id.tvTotalVotes);
            tvStatus = v.findViewById(R.id.tvPollStatus);
            optionsContainer = v.findViewById(R.id.optionsContainer);
        }
    }
}
