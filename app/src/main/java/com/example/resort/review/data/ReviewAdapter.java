package com.example.resort.review.data;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.R;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {

    private final List<Review> reviews;

    public ReviewAdapter(List<Review> reviews) {
        this.reviews = reviews;
    }

    // ViewHolder class that holds item layout views
    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
        public TextView tvUsername, tvComment, tvDate;
        public RatingBar ratingBar;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvComment = itemView.findViewById(R.id.tvComment);
            tvDate = itemView.findViewById(R.id.tvDate);
            ratingBar = itemView.findViewById(R.id.ratingBar5);
        }
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.tvUsername.setText(review.getUser());
        holder.tvComment.setText('"' + review.getComment() + '"');
        holder.tvDate.setText(review.getDate());
        /// Convert int to float for RatingBar
        holder.ratingBar.setRating((float) review.getRate());
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    // Update method to refresh the list
    @SuppressLint("NotifyDataSetChanged")
    public void updateReviews(List<Review> newReviews) {
        reviews.clear();
        reviews.addAll(newReviews);
        notifyDataSetChanged();
    }
}


///Fix Current
//package com.example.resort.review.data;
//
//import android.annotation.SuppressLint;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.RatingBar;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.R;
//
//import java.util.List;
//
//public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
//
//    private final List<Review> reviews;
//
//    public ReviewAdapter(List<Review> reviews) {
//        this.reviews = reviews;
//    }
//
//    // ViewHolder class that holds item layout views
//    public static class ReviewViewHolder extends RecyclerView.ViewHolder {
//        public TextView tvUsername, tvComment, tvDate;
//        public RatingBar ratingBar;
//
//        public ReviewViewHolder(@NonNull View itemView) {
//            super(itemView);
//            tvUsername = itemView.findViewById(R.id.tvUsername);
//            tvComment = itemView.findViewById(R.id.tvComment);
//            tvDate = itemView.findViewById(R.id.tvDate);
//            ratingBar = itemView.findViewById(R.id.ratingBar5);
//        }
//    }
//
//    @NonNull
//    @Override
//    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(R.layout.item_review, parent, false);
//        return new ReviewViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
//        Review review = reviews.get(position);
//        holder.tvUsername.setText(review.getUser());
//        holder.tvComment.setText('"' + review.getComment() + '"');
//        holder.tvDate.setText(review.getDate());
//        /// Convert int to float for RatingBar
//        holder.ratingBar.setRating((float) review.getRate());
//    }
//
//    @Override
//    public int getItemCount() {
//        return reviews.size();
//    }
//
//    // Update method to refresh the list
//    @SuppressLint("NotifyDataSetChanged")
//    public void updateReviews(List<Review> newReviews) {
//        reviews.clear();
//        reviews.addAll(newReviews);
//        notifyDataSetChanged();
//    }
//}
