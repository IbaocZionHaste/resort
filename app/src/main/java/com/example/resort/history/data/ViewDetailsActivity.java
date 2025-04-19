package com.example.resort.history.data;


import android.graphics.Color;
import android.text.TextUtils;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.resort.R;
import com.example.resort.history.data.BookingData.PaymentTransaction;
import com.example.resort.history.data.BookingData.BookingReview;
import com.example.resort.history.data.BookingData.PaymentMethod;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

public class ViewDetailsActivity extends AppCompatActivity {
    private TextView tvDate;
    private TextView tvEmail;
    private TextView tvName;
    private TextView tvPhone;
    private TextView tvRefNo;
    private TextView tvStatusReview;
    private LinearLayout layoutOrderItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_details);

        tvDate = findViewById(R.id.tvDate);
        tvEmail = findViewById(R.id.tvEmail);
        tvName = findViewById(R.id.tvName);
        tvPhone = findViewById(R.id.tvPhone);
        tvRefNo = findViewById(R.id.tvRefNo);
        tvStatusReview = findViewById(R.id.tvStatusReview);
        layoutOrderItems = findViewById(R.id.layoutOrderItems);

        // Initialize the arrow ImageView
        ImageView arrow = findViewById(R.id.arrow);

        // Set OnClickListener for the arrow ImageView to handle back press
        arrow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Navigate back to the previous activity
                onBackPressed();
            }
        });

        // Retrieve passed booking data as JSON and convert it back.
        String json = getIntent().getStringExtra("bookingData");
        BookingData booking = new Gson().fromJson(json, BookingData.class);

        if (booking != null && booking.getBookingReview() != null) {
            BookingReview review = booking.getBookingReview();

            tvDate.setText("Booking Date: " + review.bookingDate);
            tvEmail.setText("Email: " + review.email);
            tvName.setText("Name: " + review.name);
            tvPhone.setText("Phone: " + review.phone);
            tvRefNo.setText("Reference: " + review.refNo);
            tvStatusReview.setText("Status Review: " + review.statusReview);

            // Format Order Items
            if (review.orderItems != null) {
                // For each category, create a header and then list each item.
                addOrderItemsSection("Accommodations", review.orderItems.get("accommodations"));
                addOrderItemsSection("Food & Drinks", review.orderItems.get("foodAndDrinks"));
                addOrderItemsSection("Package", review.orderItems.get("package"));
            }
        }


        if (booking != null && booking.getPaymentTransaction() != null) {
            PaymentTransaction transaction = booking.getPaymentTransaction();

            ///TextView tvPaymentDate= findViewById(R.id.tvPaymentDate);
            TextView tvDownPayment = findViewById(R.id.tvDownPayment);
            TextView tvAmountTotal = findViewById(R.id.tvAmountTotal);

            tvDownPayment.setText("Down Payment: " + transaction.downPayment);
            tvAmountTotal.setText("Total Amount: " + transaction.amount);
//            String paymentDate = transaction.PaymentDate;
//            if (paymentDate == null || paymentDate.trim().isEmpty()) {
//                tvPaymentDate.setText("Payment Date: N/A");
//            } else {
//                tvPaymentDate.setText("Payment Date: " + paymentDate);
//            }

        }

        if (booking != null && booking.getPaymentMethod() != null) {
            PaymentMethod pm = booking.getPaymentMethod();

            // Set each field's text with the respective payment details
            TextView tvPayment = findViewById(R.id.tvPayment);
            TextView tvAmount = findViewById(R.id.tvAmount);
            TextView tvDates = findViewById(R.id.tvDates);
            TextView tvFirstname = findViewById(R.id.tvFirstname);
            TextView tvLastname = findViewById(R.id.tvLastname);
            TextView tvPhones = findViewById(R.id.tvPhones);
            TextView tvReference = findViewById(R.id.tvReference);

            TextView tvStatus = findViewById(R.id.tvStatus);

            tvPayment.setText("Payment: " + pm.Payment);
            tvAmount.setText("Amount: " + pm.Amount);
            ///tvDates.setText("Date: " + pm.Date);
            tvDates.setText("Date: " + (TextUtils.isEmpty(pm.Date) ? "N/A" : pm.Date));
            tvFirstname.setText("Firstname: " + pm.Firstname);
            tvLastname.setText("Lastname: " + pm.Lastname);
            tvPhones.setText("Phone: " + pm.Phone);
            tvReference.setText("Reference: " + pm.Reference);

            tvStatus.setText("Status: " + pm.Status);
        }
    }

    private void addOrderItemsSection(String sectionTitle, Object data) {
        if (data == null) {
            return;
        }
        LayoutInflater inflater = LayoutInflater.from(this);

        // Add header with black text color and margin
        TextView header = new TextView(this);
        header.setText(sectionTitle);
        header.setTextSize(16);  // Set text size
        header.setTypeface(header.getTypeface(), Typeface.BOLD);  // Set text style to bold
        header.setTextColor(Color.BLACK);  // Set text color to black
        header.setPadding(0, 16, 0, 8);  // Optional padding for the header

        // Set margin for header
        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        headerParams.setMargins(0, 16, 0, 8);  // Adjust margins as needed
        header.setLayoutParams(headerParams);

        layoutOrderItems.addView(header);

        // Check if data is a List (for accommodations, foodAndDrinks)
        if (data instanceof List) {
            List list = (List) data;
            for (Object obj : list) {
                if (obj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> item = (Map<String, Object>) obj;
                    View itemView = inflater.inflate(R.layout.order_item, layoutOrderItems, false);
                    TextView tvItemName = itemView.findViewById(R.id.tvItemName);
                    TextView tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
                    TextView tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
                    TextView tvItemQty = itemView.findViewById(R.id.tvItemQty);

                    tvItemName.setText("" + item.get("name"));
                    tvItemCategory.setText("" + item.get("category"));
                    tvItemPrice.setText("" + item.get("price"));

                    // Get the quantity and remove ".0" if it's an integer, and show as "Qty: X"
                    Object quantityObj = item.get("quantity");
                    if (quantityObj instanceof Double) {
                        Double quantity = (Double) quantityObj;
                        if (quantity == quantity.intValue()) {
                            // If the quantity is an integer value, display it as an integer without ".0"
                            tvItemQty.setText("Qty: " + quantity.intValue());
                        } else {
                            // Otherwise, display the quantity with decimal
                            tvItemQty.setText("Qty: " + quantity);
                        }
                    } else {
                        // If it's not a Double, assume it's an integer and display as "Qty: X"
                        tvItemQty.setText("Qty: " + quantityObj);
                    }

                    layoutOrderItems.addView(itemView);
                }
            }
        }
        // Otherwise, if it's a Map (for package, usually a single object)
        else if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> item = (Map<String, Object>) data;
            View itemView = inflater.inflate(R.layout.order_item, layoutOrderItems, false);
            TextView tvItemName = itemView.findViewById(R.id.tvItemName);
            TextView tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
            TextView tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
            TextView tvItemQty = itemView.findViewById(R.id.tvItemQty);

            tvItemName.setText("" + item.get("name"));
            tvItemCategory.setText("" + item.get("category"));
            tvItemPrice.setText("" + item.get("price"));

            // Get the quantity and remove ".0" if it's an integer, and show as "Qty: X"
            Object quantityObj = item.get("quantity");
            if (quantityObj instanceof Double) {
                Double quantity = (Double) quantityObj;
                if (quantity == quantity.intValue()) {
                    // If the quantity is an integer value, display it as an integer without ".0"
                    tvItemQty.setText("Qty: " + quantity.intValue());
                } else {
                    // Otherwise, display the quantity with decimal
                    tvItemQty.setText("Qty: " + quantity);
                }
            } else {
                // If it's not a Double, assume it's an integer and display as "Qty: X"
                tvItemQty.setText("Qty: " + quantityObj);
            }

            layoutOrderItems.addView(itemView);
        }
    }

}



///Fix Current
//package com.example.resort.history.data;
//
//
//import android.graphics.Color;
//import android.text.TextUtils;
//import android.graphics.Typeface;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.example.resort.R;
//import com.example.resort.history.data.BookingData.PaymentTransaction;
//import com.example.resort.history.data.BookingData.BookingReview;
//import com.example.resort.history.data.BookingData.PaymentMethod;
//import com.google.gson.Gson;
//
//import java.util.List;
//import java.util.Map;
//
//public class ViewDetailsActivity extends AppCompatActivity {
//    private TextView tvDate;
//    private TextView tvEmail;
//    private TextView tvName;
//    private TextView tvPhone;
//    private TextView tvRefNo;
//    private TextView tvStatusReview;
//    private LinearLayout layoutOrderItems;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        EdgeToEdge.enable(this);
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_view_details);
//
//        tvDate = findViewById(R.id.tvDate);
//        tvEmail = findViewById(R.id.tvEmail);
//        tvName = findViewById(R.id.tvName);
//        tvPhone = findViewById(R.id.tvPhone);
//        tvRefNo = findViewById(R.id.tvRefNo);
//        tvStatusReview = findViewById(R.id.tvStatusReview);
//        layoutOrderItems = findViewById(R.id.layoutOrderItems);
//
//        // Initialize the arrow ImageView
//        ImageView arrow = findViewById(R.id.arrow);
//
//        // Set OnClickListener for the arrow ImageView to handle back press
//        arrow.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Navigate back to the previous activity
//                onBackPressed();
//            }
//        });
//
//        // Retrieve passed booking data as JSON and convert it back.
//        String json = getIntent().getStringExtra("bookingData");
//        BookingData booking = new Gson().fromJson(json, BookingData.class);
//
//        if (booking != null && booking.getBookingReview() != null) {
//            BookingReview review = booking.getBookingReview();
//
//            tvDate.setText("Booking Date: " + review.bookingDate);
//            tvEmail.setText("Email: " + review.email);
//            tvName.setText("Name: " + review.name);
//            tvPhone.setText("Phone: " + review.phone);
//            tvRefNo.setText("Reference: " + review.refNo);
//            tvStatusReview.setText("Status Review: " + review.statusReview);
//
//            // Format Order Items
//            if (review.orderItems != null) {
//                // For each category, create a header and then list each item.
//                addOrderItemsSection("Accommodations", review.orderItems.get("accommodations"));
//                addOrderItemsSection("Food & Drinks", review.orderItems.get("foodAndDrinks"));
//                addOrderItemsSection("Package", review.orderItems.get("package"));
//            }
//        }
//
//
//        if (booking != null && booking.getPaymentTransaction() != null) {
//            PaymentTransaction transaction = booking.getPaymentTransaction();
//
//            ///TextView tvPaymentDate= findViewById(R.id.tvPaymentDate);
//            TextView tvDownPayment = findViewById(R.id.tvDownPayment);
//            TextView tvAmountTotal = findViewById(R.id.tvAmountTotal);
//
//            tvDownPayment.setText("Down Payment: " + transaction.downPayment);
//            tvAmountTotal.setText("Total Amount: " + transaction.amount);
////            String paymentDate = transaction.PaymentDate;
////            if (paymentDate == null || paymentDate.trim().isEmpty()) {
////                tvPaymentDate.setText("Payment Date: N/A");
////            } else {
////                tvPaymentDate.setText("Payment Date: " + paymentDate);
////            }
//
//        }
//
//        if (booking != null && booking.getPaymentMethod() != null) {
//            PaymentMethod pm = booking.getPaymentMethod();
//
//            // Set each field's text with the respective payment details
//            TextView tvPayment = findViewById(R.id.tvPayment);
//            TextView tvAmount = findViewById(R.id.tvAmount);
//            TextView tvDates = findViewById(R.id.tvDates);
//            TextView tvFirstname = findViewById(R.id.tvFirstname);
//            TextView tvLastname = findViewById(R.id.tvLastname);
//            TextView tvPhones = findViewById(R.id.tvPhones);
//            TextView tvReference = findViewById(R.id.tvReference);
//
//            TextView tvStatus = findViewById(R.id.tvStatus);
//
//            tvPayment.setText("Payment: " + pm.Payment);
//            tvAmount.setText("Amount: " + pm.Amount);
//            ///tvDates.setText("Date: " + pm.Date);
//            tvDates.setText("Date: " + (TextUtils.isEmpty(pm.Date) ? "N/A" : pm.Date));
//            tvFirstname.setText("Firstname: " + pm.Firstname);
//            tvLastname.setText("Lastname: " + pm.Lastname);
//            tvPhones.setText("Phone: " + pm.Phone);
//            tvReference.setText("Reference: " + pm.Reference);
//
//            tvStatus.setText("Status: " + pm.Status);
//        }
//    }
//
//    private void addOrderItemsSection(String sectionTitle, Object data) {
//        if (data == null) {
//            return;
//        }
//        LayoutInflater inflater = LayoutInflater.from(this);
//
//        // Add header with black text color and margin
//        TextView header = new TextView(this);
//        header.setText(sectionTitle);
//        header.setTextSize(16);  // Set text size
//        header.setTypeface(header.getTypeface(), Typeface.BOLD);  // Set text style to bold
//        header.setTextColor(Color.BLACK);  // Set text color to black
//        header.setPadding(0, 16, 0, 8);  // Optional padding for the header
//
//        // Set margin for header
//        LinearLayout.LayoutParams headerParams = new LinearLayout.LayoutParams(
//                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
//        headerParams.setMargins(0, 16, 0, 8);  // Adjust margins as needed
//        header.setLayoutParams(headerParams);
//
//        layoutOrderItems.addView(header);
//
//        // Check if data is a List (for accommodations, foodAndDrinks)
//        if (data instanceof List) {
//            List list = (List) data;
//            for (Object obj : list) {
//                if (obj instanceof Map) {
//                    @SuppressWarnings("unchecked")
//                    Map<String, Object> item = (Map<String, Object>) obj;
//                    View itemView = inflater.inflate(R.layout.order_item, layoutOrderItems, false);
//                    TextView tvItemName = itemView.findViewById(R.id.tvItemName);
//                    TextView tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
//                    TextView tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
//                    TextView tvItemQty = itemView.findViewById(R.id.tvItemQty);
//
//                    tvItemName.setText("" + item.get("name"));
//                    tvItemCategory.setText("" + item.get("category"));
//                    tvItemPrice.setText("" + item.get("price"));
//
//                    // Get the quantity and remove ".0" if it's an integer, and show as "Qty: X"
//                    Object quantityObj = item.get("quantity");
//                    if (quantityObj instanceof Double) {
//                        Double quantity = (Double) quantityObj;
//                        if (quantity == quantity.intValue()) {
//                            // If the quantity is an integer value, display it as an integer without ".0"
//                            tvItemQty.setText("Qty: " + quantity.intValue());
//                        } else {
//                            // Otherwise, display the quantity with decimal
//                            tvItemQty.setText("Qty: " + quantity);
//                        }
//                    } else {
//                        // If it's not a Double, assume it's an integer and display as "Qty: X"
//                        tvItemQty.setText("Qty: " + quantityObj);
//                    }
//
//                    layoutOrderItems.addView(itemView);
//                }
//            }
//        }
//        // Otherwise, if it's a Map (for package, usually a single object)
//        else if (data instanceof Map) {
//            @SuppressWarnings("unchecked")
//            Map<String, Object> item = (Map<String, Object>) data;
//            View itemView = inflater.inflate(R.layout.order_item, layoutOrderItems, false);
//            TextView tvItemName = itemView.findViewById(R.id.tvItemName);
//            TextView tvItemCategory = itemView.findViewById(R.id.tvItemCategory);
//            TextView tvItemPrice = itemView.findViewById(R.id.tvItemPrice);
//            TextView tvItemQty = itemView.findViewById(R.id.tvItemQty);
//
//            tvItemName.setText("" + item.get("name"));
//            tvItemCategory.setText("" + item.get("category"));
//            tvItemPrice.setText("" + item.get("price"));
//
//            // Get the quantity and remove ".0" if it's an integer, and show as "Qty: X"
//            Object quantityObj = item.get("quantity");
//            if (quantityObj instanceof Double) {
//                Double quantity = (Double) quantityObj;
//                if (quantity == quantity.intValue()) {
//                    // If the quantity is an integer value, display it as an integer without ".0"
//                    tvItemQty.setText("Qty: " + quantity.intValue());
//                } else {
//                    // Otherwise, display the quantity with decimal
//                    tvItemQty.setText("Qty: " + quantity);
//                }
//            } else {
//                // If it's not a Double, assume it's an integer and display as "Qty: X"
//                tvItemQty.setText("Qty: " + quantityObj);
//            }
//
//            layoutOrderItems.addView(itemView);
//        }
//    }
//
//}
//
