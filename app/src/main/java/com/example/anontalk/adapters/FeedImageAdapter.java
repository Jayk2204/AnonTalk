package com.example.anontalk.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.anontalk.R;

import java.util.List;

public class FeedImageAdapter extends RecyclerView.Adapter<FeedImageAdapter.ImageViewHolder> {

    Context context;
    List<String> imageList;   // 🔥 IMGBB URLs

    public FeedImageAdapter(Context context, List<String> imageList) {
        this.context = context;
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String imageUrl = imageList.get(position);

        Glide.with(context)
                .load(imageUrl)
                .into(holder.imageView);

        // Hide remove button in feed
        holder.btnRemove.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView, btnRemove;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imgPreview);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
