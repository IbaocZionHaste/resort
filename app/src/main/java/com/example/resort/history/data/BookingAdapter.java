package com.example.resort.history.data;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.R;
import com.example.resort.history.data.BookingData;
import com.google.gson.Gson;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {
    private Context context;
    private List<BookingData> bookingList;

    public BookingAdapter(Context context, List<BookingData> bookingList) {
        this.context = context;
        this.bookingList = bookingList;
    }

    @NonNull
    @Override
    public BookingAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BookingAdapter.ViewHolder holder, int position) {
        BookingData booking = bookingList.get(position);

        // Display the booking date
        if (booking.getBookingReview() != null && booking.getBookingReview().bookingDate != null) {
            holder.tvBookingDate.setText(booking.getBookingReview().bookingDate); // Set booking date
        } else {
            holder.tvBookingDate.setText(""); // Set it empty if no booking date
        }

        // Display the payment method and set the text color to blue
        if (booking.getPaymentMethod() != null) {
            holder.tvPaymentMethod.setText("Payment Method: " + booking.getPaymentMethod().Payment);
            holder.tvPaymentMethod.setTextColor(Color.BLACK); // Set color to blue
        } else {
            holder.tvPaymentMethod.setText("");
        }

/// Display the refNo from bookingReview
//        if (booking.getBookingReview() != null && booking.getBookingReview().refNo != null) {
//            holder.tvRefNo.setText("Reference: " + booking.getBookingReview().refNo);
//        } else {
//            holder.tvRefNo.setText(""); // Set empty if no reference number is available
//        }


        // Display the refNo (without "Reference:") in red color
        if (booking.getBookingReview() != null && booking.getBookingReview().refNo != null) {
            holder.tvRefNo.setText("Ref.No" + booking.getBookingReview().refNo);
            holder.tvRefNo.setTextColor(context.getResources().getColor(R.color.black));
        } else {
            holder.tvRefNo.setText(""); /// Set empty if no reference number is available
        }

        // Check the payment transaction status and update the status and color
        if (booking.getPaymentTransaction() != null) {
            String paymentStatus = booking.getPaymentTransaction().paymentStatus;
            holder.tvStatus.setText("Status: " + paymentStatus);

            // Set the color and status text based on the payment status
            switch (paymentStatus) {
                case "approved":
                    holder.tvStatus.setText("Status: Completed");
                    holder.tvStatus.setTextColor(Color.GREEN); // Green for Approved
                    break;
                case "Cancelled":
                    holder.tvStatus.setText("Status: Cancelled");
                    holder.tvStatus.setTextColor(Color.RED); // Red for Cancelled
                    break;
                case "Declined":
                    holder.tvStatus.setText("Status: Declined");
                    holder.tvStatus.setTextColor(Color.YELLOW); // Yellow for Declined
                    break;
                case "refund":
                    holder.tvStatus.setText("Status: Refunded");
                    holder.tvStatus.setTextColor(Color.YELLOW); // Blue for Refunded
                    break;
                default:
                    holder.tvStatus.setText("Status: Unknown");
                    holder.tvStatus.setTextColor(Color.GRAY); // Default color for unknown status
                    break;
            }
        } else {
            holder.tvStatus.setText("");
        }


        // When "View Details" is clicked, open the detail activity
        holder.btnViewDetails.setOnClickListener(v -> {
            Intent intent = new Intent(context, ViewDetailsActivity.class);
            String bookingJson = new Gson().toJson(booking);
            intent.putExtra("bookingData", bookingJson);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return bookingList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvBookingDate, tvPaymentMethod, tvStatus, tvRefNo;
        Button btnViewDetails;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvBookingDate = itemView.findViewById(R.id.tvBookingDate);
            tvPaymentMethod = itemView.findViewById(R.id.tvPaymentMethod);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvRefNo = itemView.findViewById(R.id.tvRefNo);
            btnViewDetails = itemView.findViewById(R.id.btnViewDetails);
        }
    }
}
