package com.example.resort.notification.data;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.R;

import java.util.List;
import java.util.Map;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    // The notification data is represented as a List of Maps.
    // Each map can have keys like "message" and "timestamp".
    private final List<Map<String, Object>> notifications;

    public NotificationAdapter(List<Map<String, Object>> notifications) {
        this.notifications = notifications;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Map<String, Object> notification = notifications.get(position);
        String message = (String) notification.get("message");
        String timestamp = (String) notification.get("timestamp");

        if (message != null) {
            // Remove any trailing date and time in the format " YYYY-MM-DD HH:MM AM/PM"
            message = message.replaceAll("\\s*\\d{4}-\\d{2}-\\d{2}\\s+\\d{1,2}:\\d{2}\\s*(AM|PM)$", "");
        }

        holder.tvMessage.setText(Html.fromHtml(message));
        holder.tvTimestamp.setText(timestamp);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTimestamp;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
    }
}
