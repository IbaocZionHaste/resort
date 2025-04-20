///Payment Information
package com.example.resort.review.data;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class InformationAdapter extends RecyclerView.Adapter<InformationAdapter.ViewHolder> {
    private final List<String> dataList;

    public InformationAdapter(List<String> dataList) {
        this.dataList = dataList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_1, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = dataList.get(position);
        holder.textView.setText('"' + item + '"');

        /// Enable text selection for copying
        holder.textView.setTextIsSelectable(true);
        holder.textView.setTextColor(Color.BLACK);
        holder.textView.setTextSize(16);

        /// Set click listener to copy the text when clicked
        holder.textView.setOnClickListener(v -> {
            // Get the selected text and copy it to the clipboard
            ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", item);
            clipboard.setPrimaryClip(clip);

            /// Optionally, show a toast to notify the user
            Toast.makeText(v.getContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = itemView.findViewById(android.R.id.text1);
        }
    }
}




/// Fix Current
//package com.example.resort.review.data;
//
//import android.content.ClipData;
//import android.content.ClipboardManager;
//import android.content.Context;
//import android.graphics.Color;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import java.util.List;
//
//public class InformationAdapter extends RecyclerView.Adapter<InformationAdapter.ViewHolder> {
//    private final List<String> dataList;
//
//    public InformationAdapter(List<String> dataList) {
//        this.dataList = dataList;
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(parent.getContext())
//                .inflate(android.R.layout.simple_list_item_1, parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        String item = dataList.get(position);
//        holder.textView.setText('"' + item + '"');
//
//        /// Enable text selection for copying
//        holder.textView.setTextIsSelectable(true);
//        holder.textView.setTextColor(Color.BLACK);
//        holder.textView.setTextSize(16);
//
//        /// Set click listener to copy the text when clicked
//        holder.textView.setOnClickListener(v -> {
//            // Get the selected text and copy it to the clipboard
//            ClipboardManager clipboard = (ClipboardManager) v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
//            ClipData clip = ClipData.newPlainText("Copied Text", item);
//            clipboard.setPrimaryClip(clip);
//
//            /// Optionally, show a toast to notify the user
//            Toast.makeText(v.getContext(), "Text copied to clipboard", Toast.LENGTH_SHORT).show();
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return dataList.size();
//    }
//
//    public static class ViewHolder extends RecyclerView.ViewHolder {
//        TextView textView;
//
//        public ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            textView = itemView.findViewById(android.R.id.text1);
//        }
//    }
//}
//
