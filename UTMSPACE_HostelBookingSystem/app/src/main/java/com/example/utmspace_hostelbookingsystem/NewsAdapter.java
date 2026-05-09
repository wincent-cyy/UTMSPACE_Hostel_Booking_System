package com.example.utmspace_hostelbookingsystem;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NewsAdapter extends RecyclerView.Adapter<NewsAdapter.NewsViewHolder> {

    private List<Integer> imageList; // List of drawable IDs

    public NewsAdapter(List<Integer> imageList) {
        this.imageList = imageList;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Reuse your item_news_banner layout
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news_banner, parent, false);
        return new NewsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder holder, int position) {
        // Set the image resource directly
        holder.ivBannerImage.setImageResource(imageList.get(position));

        // If you don't want text, you can hide the TextViews here
        // holder.tvNewsDescription.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return imageList.size();
    }

    static class NewsViewHolder extends RecyclerView.ViewHolder {
        ImageView ivBannerImage;
        NewsViewHolder(@NonNull View itemView) {
            super(itemView);
            ivBannerImage = itemView.findViewById(R.id.ivBannerImage);
        }
    }
}