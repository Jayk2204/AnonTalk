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

    // ✅ CONSTRUCTOR
    public CommentAdapter(Context context, List<CommentModel> commentList) {
        this.context = context;
        this.commentList = commentList;
        this.myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {

        CommentModel model = commentList.get(position);

        // 💬 SET COMMENT TEXT
        holder.tvComment.setText(model.getText());

        // 🏷 SHOW "(edited)" LABEL IF EDITED
        if (model.isEdited()) {
            holder.tvEdited.setVisibility(View.VISIBLE);
        } else {
            holder.tvEdited.setVisibility(View.GONE);
        }

        // 🛑 LONG PRESS → EDIT / DELETE (ONLY FOR OWNER)
        holder.itemView.setOnLongClickListener(v -> {

            if (!model.getUserId().equals(myUid)) return true; // not your comment

            String[] options = {"Edit", "Delete"};

            new AlertDialog.Builder(context)
                    .setTitle("Comment Options")
                    .setItems(options, (dialog, which) -> {

                        if (which == 0) {
                            // ✏️ EDIT
                            showEditDialog(model);
                        } else if (which == 1) {
                            // 🗑 DELETE
                            ((CommentsActivity) context)
                                    .deleteComment(model.getCommentId());
                        }

                    })
                    .show();

            return true;
        });
    }

    // ✏️ EDIT DIALOG
    private void showEditDialog(CommentModel model) {
        final EditText input = new EditText(context);
        input.setText(model.getText());
        input.setSelection(input.getText().length());

        new AlertDialog.Builder(context)
                .setTitle("Edit Comment")
                .setView(input)
                .setPositiveButton("Update", (dialog, which) -> {
                    String newText = input.getText().toString().trim();
                    if (!newText.isEmpty()) {
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

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        TextView tvComment, tvEdited;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvEdited = itemView.findViewById(R.id.tvEdited); // "(edited)" label
        }
    }
}
