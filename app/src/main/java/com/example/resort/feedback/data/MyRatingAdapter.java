package com.example.resort.feedback.data;


import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.R;

import java.util.List;

public class MyRatingAdapter extends RecyclerView.Adapter<MyRatingAdapter.MyRatingViewHolder> {

    private final List<RatingItem> ratingList;

    public MyRatingAdapter(List<RatingItem> ratingList) {
        this.ratingList = ratingList;
    }

    @NonNull
    @Override
    public MyRatingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rating, parent, false);
        return new MyRatingViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MyRatingViewHolder holder, int position) {
        RatingItem item = ratingList.get(position);
        holder.nameTextView.setText('"' + item.getItemName() + '"');
        holder.categoryTextView.setText(item.getCategory());
        ///holder.commentTextView.setText(item.getComment());
        holder.dateTextView.setText(item.getDate());
        holder.ratingBar.setRating(item.getRate());

        /// Get the comment
        String comment = item.getComment();
        if (comment != null && !comment.trim().isEmpty()) {
            /// Enclose the comment in double quotes if it's not empty
            holder.commentTextView.setText("\"" + comment + "\"");
        } else {
            /// Set the TextView to an empty string if there's no comment
            holder.commentTextView.setText("");
        }
    }


    @Override
    public int getItemCount() {
        return ratingList.size();
    }

    static class MyRatingViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, categoryTextView, commentTextView, dateTextView;
        RatingBar ratingBar;

        public MyRatingViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.itemName);
            categoryTextView = itemView.findViewById(R.id.itemCategory);
            commentTextView = itemView.findViewById(R.id.itemComment);
            dateTextView = itemView.findViewById(R.id.itemDate);
            ratingBar = itemView.findViewById(R.id.itemRatingBar);
        }
    }
}

///Fix Current
//package com.example.resort.feedback.data;
//
//
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
//public class MyRatingAdapter extends RecyclerView.Adapter<MyRatingAdapter.MyRatingViewHolder> {
//
//    private final List<RatingItem> ratingList;
//
//    public MyRatingAdapter(List<RatingItem> ratingList) {
//        this.ratingList = ratingList;
//    }
//
//    @NonNull
//    @Override
//    public MyRatingViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_rating, parent, false);
//        return new MyRatingViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(MyRatingViewHolder holder, int position) {
//        RatingItem item = ratingList.get(position);
//        holder.nameTextView.setText(item.getItemName());
//        holder.categoryTextView.setText(item.getCategory());
//        ///holder.commentTextView.setText(item.getComment());
//        holder.dateTextView.setText(item.getDate());
//        holder.ratingBar.setRating(item.getRate());
//
//        /// Get the comment
//        String comment = item.getComment();
//        if (comment != null && !comment.trim().isEmpty()) {
//            /// Enclose the comment in double quotes if it's not empty
//            holder.commentTextView.setText("\"" + comment + "\"");
//        } else {
//            /// Set the TextView to an empty string if there's no comment
//            holder.commentTextView.setText("");
//        }
//    }
//
//
//    @Override
//    public int getItemCount() {
//        return ratingList.size();
//    }
//
//    static class MyRatingViewHolder extends RecyclerView.ViewHolder {
//        TextView nameTextView, categoryTextView, commentTextView, dateTextView;
//        RatingBar ratingBar;
//
//        public MyRatingViewHolder(View itemView) {
//            super(itemView);
//            nameTextView = itemView.findViewById(R.id.itemName);
//            categoryTextView = itemView.findViewById(R.id.itemCategory);
//            commentTextView = itemView.findViewById(R.id.itemComment);
//            dateTextView = itemView.findViewById(R.id.itemDate);
//            ratingBar = itemView.findViewById(R.id.itemRatingBar);
//        }
//    }
//}
