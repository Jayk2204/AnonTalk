package com.example.anontalk.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.anontalk.R;
import com.example.anontalk.models.CommentModel;

import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.Holder> {

    private List<CommentModel> list;

    public CommentAdapter(List<CommentModel> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.tvComment.setText(list.get(position).getText());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView tvComment;
        public Holder(@NonNull View itemView) {
            super(itemView);
            tvComment = itemView.findViewById(R.id.tvComment);
        }
    }
}
