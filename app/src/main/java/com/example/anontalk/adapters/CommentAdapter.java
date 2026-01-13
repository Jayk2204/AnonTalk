package com.example.anontalk.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anontalk.R;
import com.example.anontalk.activities.CommentsActivity;
import com.example.anontalk.models.CommentModel;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private Context context;
    private List<CommentModel> commentList;
    private String myUid;

    // ✅ CONSTRUCTOR (SAFE)
    public CommentAdapter(Context context, List<CommentModel> commentList) {
        this.context = context;
        this.commentList = commentList;
        this.myUid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {

        CommentModel model = commentList.get(position);

        // 🧠 HEADER (X-style)
        String timeAgo = getTimeAgo(model.getTimestamp());
        holder.tvHeader.setText("Anonymous · " + timeAgo);

        // 💬 COMMENT TEXT
        holder.tvComment.setText(model.getText());

        // 🏷 EDITED LABEL
        holder.tvEdited.setVisibility(model.isEdited()
                ? View.VISIBLE
                : View.GONE);

        // 🛑 LONG PRESS → EDIT / DELETE (OWNER ONLY)
        holder.itemView.setOnLongClickListener(v -> {

            if (!model.getUserId().equals(myUid)) return true;

            String[] options = {"Edit", "Delete"};

            new AlertDialog.Builder(context)
                    .setTitle("Comment Options")
                    .setItems(options, (dialog, which) -> {

                        if (which == 0) {
                            showEditDialog(model);
                        } else {
                            if (context instanceof CommentsActivity) {
                                ((CommentsActivity) context)
                                        .deleteComment(model.getCommentId());
                            }
                        }
                    })
                    .show();

            return true;
        });
    }

    // ✏️ EDIT COMMENT DIALOG (UNCHANGED LOGIC)
    private void showEditDialog(CommentModel model) {

        final EditText input = new EditText(context);
        input.setText(model.getText());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(context)
                .setTitle("Edit Comment")
                .setView(input)
                .setPositiveButton("Update", (dialog, which) -> {

                    String newText = input.getText().toString().trim();
                    if (!newText.isEmpty()
                            && context instanceof CommentsActivity) {

                        ((CommentsActivity) context)
                                .editComment(model.getCommentId(), newText);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    // ==============================
    // VIEW HOLDER (UPDATED FOR X-STYLE)
    // ==============================
    static class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvHeader, tvComment, tvEdited;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);

            tvHeader = itemView.findViewById(R.id.tvHeader);   // NEW
            tvComment = itemView.findViewById(R.id.tvComment);
            tvEdited = itemView.findViewById(R.id.tvEdited);
        }
    }

    // ==============================
    // ⏱ TIME FORMAT (SAFE)
    // ==============================
    private String getTimeAgo(long time) {

        long diff = System.currentTimeMillis() - time;

        if (diff < 60000) return "Just now";
        if (diff < 3600000) return (diff / 60000) + "m";
        if (diff < 86400000) return (diff / 3600000) + "h";
        return (diff / 86400000) + "d";
    }
}
