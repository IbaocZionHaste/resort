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

import com.example.resort.R;  // Your app’s R class

import java.util.List;

public class InformationAdapter extends RecyclerView.Adapter<InformationAdapter.ViewHolder> {
    private final List<String> dataList;

    /** Constructor: pass in the list of strings to display */
    public InformationAdapter(List<String> dataList) {
        this.dataList = dataList;  /// Store the data list :contentReference[oaicite:3]{index=3}
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        /// Inflate the custom item layout (must exist at res/layout/item_message.xml) :contentReference[oaicite:4]{index=4}
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String item = dataList.get(position);
        holder.messageText.setText(item);  /// Bind the string to the TextView :contentReference[oaicite:5]{index=5}

        /// Enable text selection so user can long-press and select :contentReference[oaicite:6]{index=6}
        holder.messageText.setTextIsSelectable(true);
        holder.messageText.setTextColor(Color.BLACK);
        holder.messageText.setTextSize(13);

        /// Copy to clipboard on click using ClipboardManager & ClipData :contentReference[oaicite:7]{index=7} :contentReference[oaicite:8]{index=8}
        holder.messageText.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager)
                    v.getContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Copied Text", item);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(v.getContext(),
                    "Text copied to clipboard",
                    Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return dataList.size();
    }

    /** ViewHolder: holds reference to your item’s TextView */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView messageText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            /// Bind the TextView in item_message.xml: must match android:id="@+id/messageText" :contentReference[oaicite:10]{index=10}
            messageText = itemView.findViewById(R.id.messageText);
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
