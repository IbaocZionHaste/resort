package com.example.resort;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.function.Consumer;

public class Comment extends AppCompatActivity {
    private EditText recentEditText, commentEditText;
    private TextView messageTextView;
    private RatingBar ratingBar5, ratingBar4, ratingBar3, ratingBar2, ratingBar1;

    private final List<DataSnapshot> bookingSnapshots = new ArrayList<>();
    private int selectedBookingIndex = -1;
    private int selectedRating = 0;
    private boolean isSubmitting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_comment);

        findViewById(R.id.back2).setOnClickListener(v -> finish());

        ratingBar5 = findViewById(R.id.ratingBar5);
        ratingBar4 = findViewById(R.id.ratingBar4);
        ratingBar3 = findViewById(R.id.ratingBar3);
        ratingBar2 = findViewById(R.id.ratingBar2);
        ratingBar1 = findViewById(R.id.ratingBar1);
        setupRateButtons();

        recentEditText = findViewById(R.id.Recent);
        recentEditText.setText("Recent Booking");
        recentEditText.setFocusable(false);
        recentEditText.setOnClickListener(v -> showBookingSelectionDialog());

        messageTextView = findViewById(R.id.textView2);
        commentEditText = findViewById(R.id.Comment);

        fetchBookings();

        findViewById(R.id.Submit).setOnClickListener(v -> submitRating());
    }

    private void setupRateButtons() {
        int yellow = getResources().getColor(R.color.yellow);
        int grey   = getResources().getColor(R.color.grey);
        findViewById(R.id.rate5).setOnClickListener(v -> selectRating(5, ratingBar5, yellow, grey));
        findViewById(R.id.rate4).setOnClickListener(v -> selectRating(4, ratingBar4, yellow, grey));
        findViewById(R.id.rate3).setOnClickListener(v -> selectRating(3, ratingBar3, yellow, grey));
        findViewById(R.id.rate2).setOnClickListener(v -> selectRating(2, ratingBar2, yellow, grey));
        findViewById(R.id.rate1).setOnClickListener(v -> selectRating(1, ratingBar1, yellow, grey));
    }

    private void selectRating(int stars, RatingBar bar, int selColor, int defColor) {
        selectedRating = stars;
        ratingBar5.setRating(0); ratingBar4.setRating(0);
        ratingBar3.setRating(0); ratingBar2.setRating(0);
        ratingBar1.setRating(0);
        ratingBar5.setProgressTintList(ColorStateList.valueOf(defColor));
        ratingBar4.setProgressTintList(ColorStateList.valueOf(defColor));
        ratingBar3.setProgressTintList(ColorStateList.valueOf(defColor));
        ratingBar2.setProgressTintList(ColorStateList.valueOf(defColor));
        ratingBar1.setProgressTintList(ColorStateList.valueOf(defColor));
        bar.setRating(stars);
        bar.setProgressTintList(ColorStateList.valueOf(selColor));
    }

    private void fetchBookings() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("MyReviewDone");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snapshot) {
                bookingSnapshots.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    bookingSnapshots.add(snap);
                }
            }
            @Override public void onCancelled(DatabaseError error) {
                Toast.makeText(Comment.this, "Error fetching bookings", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBookingSelectionDialog() {
        if (bookingSnapshots.isEmpty()) {
            Toast.makeText(this, "No bookings available", Toast.LENGTH_SHORT).show();
            return;
        }
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_booking_selection, null);
        ListView listView       = dialogView.findViewById(R.id.bookingListView);
        Button btnViewDetails   = dialogView.findViewById(R.id.btnViewDetails);
        Button btnCancel        = dialogView.findViewById(R.id.btnCancel);

        String[] labels = new String[bookingSnapshots.size()];
        for (int i = 0; i < bookingSnapshots.size(); i++) {
            labels[i] = "Booking " + (i + 1);
        }
        final int[] sel = {-1};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.item_booking_label, labels);
        listView.setAdapter(adapter);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        listView.setOnItemClickListener((p, v, pos, id) -> sel[0] = pos);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView).create();

        btnViewDetails.setOnClickListener(v -> {
            if (sel[0] >= 0) {
                showBookingDetailsDialog(sel[0]);
                dialog.dismiss();
            } else {
                Toast.makeText(this, "Please select a booking", Toast.LENGTH_SHORT).show();
            }
        });
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.show();
    }

    private void showBookingDetailsDialog(int index) {
        DataSnapshot booking = bookingSnapshots.get(index);
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_booking_details, null);
        LinearLayout container = dialogView.findViewById(R.id.itemContainer);
        Button okButton = dialogView.findViewById(R.id.btnOk);

        populateDetails(booking, container);

        AlertDialog alertDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        okButton.setOnClickListener(v -> {
            okButton.setEnabled(false);
            selectedBookingIndex = index;
            recentEditText.setText("Booking " + (index + 1));
            alertDialog.dismiss();
        });

        alertDialog.show();
        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
    }

    private void populateDetails(DataSnapshot booking, LinearLayout container) {
        container.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        DataSnapshot acc  = booking.child("accommodations");
        DataSnapshot food = booking.child("foodAndDrinks");
        DataSnapshot pkg  = booking.child("package");

        if (acc.exists())  for (DataSnapshot item : acc.getChildren())  inflateItem(item, inflater, container);
        if (food.exists()) for (DataSnapshot item : food.getChildren()) inflateItem(item, inflater, container);
        if (pkg.exists())  inflateItem(pkg, inflater, container);
    }

    private void inflateItem(DataSnapshot itemSnap, LayoutInflater inflater, LinearLayout container) {
        String category = itemSnap.child("category").getValue(String.class);
        String name     = itemSnap.child("name").getValue(String.class);
        Long   qty      = itemSnap.child("quantity").getValue(Long.class);
        Double price    = itemSnap.child("price").getValue(Double.class);

        if (category != null && name != null && qty != null && price != null) {
            View row = inflater.inflate(R.layout.item_row, container, false);

            ((TextView) row.findViewById(R.id.tvCategory)).setText(category);
            ((TextView) row.findViewById(R.id.tvProductName)).setText(name);
            ((TextView) row.findViewById(R.id.tvQty)).setText(String.valueOf(qty));
            ((TextView) row.findViewById(R.id.tvPrice))
                    .setText(String.format("₱%.2f", price));

            container.addView(row);
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        return net != null && net.isConnected();
    }

    private void submitRating() {
        if (isSubmitting) return;
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedBookingIndex < 0) {
            Toast.makeText(this, "Please select a booking first", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

//        String commentText = commentEditText.getText().toString().trim();
//        if (commentText.isEmpty()) {
//            Toast.makeText(this, "Please add a comment", Toast.LENGTH_SHORT).show();
//            return;
//        }

        /// 1) Get and trim the user’s comment
        String commentText = commentEditText.getText().toString().trim();
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Please add a comment", Toast.LENGTH_SHORT).show();
            return;
        }
        /// 2) **New:** Enforce 360-character limit (including spaces)
        if (commentText.length() > 360) {
            Toast.makeText(this,
                    "Comment cannot exceed 360 characters",
                    Toast.LENGTH_SHORT
            ).show();
            return;
        }

        // Prevent double submits
        isSubmitting = true;
        Button submitBtn = findViewById(R.id.Submit);
        submitBtn.setEnabled(false);

        // Get userId and booking snapshot
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DataSnapshot bookingSnap = bookingSnapshots.get(selectedBookingIndex);

        // 1) Gather all item names
        List<String> names = new ArrayList<>();
        DataSnapshot accSection  = bookingSnap.child("accommodations");
        DataSnapshot foodSection = bookingSnap.child("foodAndDrinks");
        DataSnapshot pkgSection  = bookingSnap.child("package");

        if (accSection.exists()) {
            for (DataSnapshot item : accSection.getChildren()) {
                String n = item.child("name").getValue(String.class);
                if (n != null) names.add(n);
            }
        }
        if (foodSection.exists()) {
            for (DataSnapshot item : foodSection.getChildren()) {
                String n = item.child("name").getValue(String.class);
                if (n != null) names.add(n);
            }
        }
        if (pkgSection.exists()) {
            String n = pkgSection.child("name").getValue(String.class);
            if (n != null) names.add(n);
        }

        // 2) Join names or fallback
        final String allNames = names.isEmpty()
                ? "No Items"
                : TextUtils.join(", ", names);

        // 3) Category always "Accommodation"
        final String allCategories = "Accommodation";

        // 4) Fetch username then build & push record
        DatabaseReference userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("username");

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(DataSnapshot snap) {
                String username = snap.getValue(String.class);
                if (username == null || username.isEmpty()) {
                    username = "Unknown User";
                }

                Map<String, Object> record = new HashMap<>();
                record.put("user",     username);
                record.put("category", allCategories);
                record.put("itemName", allNames);
                record.put("rate",     selectedRating);
                record.put("comment",  commentText);

                SimpleDateFormat sdf = new SimpleDateFormat(
                        "yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
                sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
                record.put("date", sdf.format(new Date()));

                DatabaseReference ratingRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(userId)
                        .child("MyRating");

                ratingRef.push().setValue(record, (error, ref) -> {
                    // Restore UI state
                    isSubmitting = false;
                    submitBtn.setEnabled(true);

                    if (error == null) {
                        Toast.makeText(Comment.this,
                                "Rating submitted", Toast.LENGTH_SHORT).show();

                        // Remove from MyReviewDone
                        String bookingKey = bookingSnapshots
                                .get(selectedBookingIndex).getKey();
                        FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(userId)
                                .child("MyReviewDone")
                                .child(bookingKey)
                                .removeValue();

                        bookingSnapshots.remove(selectedBookingIndex);
                        recentEditText.setText("Tap to select booking");
                        commentEditText.setText("");
                        selectRating(0, ratingBar1,
                                getResources().getColor(R.color.yellow),
                                getResources().getColor(R.color.grey));
                    } else {
                        Toast.makeText(Comment.this,
                                "Failed to submit rating", Toast.LENGTH_SHORT).show();
                    }
                });
            }
            @Override public void onCancelled(DatabaseError error) {
                isSubmitting = false;
                submitBtn.setEnabled(true);
                Toast.makeText(Comment.this,
                        "Unable to get user info", Toast.LENGTH_SHORT).show();
            }
        });
    }
}




///Not Use
//package com.example.resort;
//
//import android.content.Context;
//import android.content.res.ColorStateList;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.ListView;
//import android.widget.RatingBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.HashMap;
//import java.util.TimeZone;
//
//public class Comment extends AppCompatActivity {
//    private EditText recentEditText, commentEditText;
//    private TextView messageTextView;
//    private RatingBar ratingBar5, ratingBar4, ratingBar3, ratingBar2, ratingBar1;
//
//    // List of booking snapshots fetched from Firebase
//    private final List<DataSnapshot> bookingSnapshots = new ArrayList<>();
//    private int selectedBookingIndex = -1;
//    private int selectedRating = 0;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_comment);
//
//        // Back button
//        findViewById(R.id.back2).setOnClickListener(v -> finish());
//
//        // RatingBars
//        ratingBar5 = findViewById(R.id.ratingBar5);
//        ratingBar4 = findViewById(R.id.ratingBar4);
//        ratingBar3 = findViewById(R.id.ratingBar3);
//        ratingBar2 = findViewById(R.id.ratingBar2);
//        ratingBar1 = findViewById(R.id.ratingBar1);
//
//        // Rate buttons
//        setupRateButtons();
//
//        // Recent booking selector
//        recentEditText = findViewById(R.id.Recent);
//        recentEditText.setText("Tap to select booking");
//        recentEditText.setFocusable(false);
//        recentEditText.setOnClickListener(v -> showBookingSelectionDialog());
//
//        // Message prompt & comment field
//        messageTextView = findViewById(R.id.textView2);
//        commentEditText = findViewById(R.id.Comment);
//
//        // Fetch bookings
//        fetchBookings();
//
//        // Submit rating
//        findViewById(R.id.Submit).setOnClickListener(v -> submitRating());
//    }
//
//    private void setupRateButtons() {
//        int yellow = getResources().getColor(R.color.yellow);
//        int grey   = getResources().getColor(R.color.grey);
//
//        findViewById(R.id.rate5).setOnClickListener(v -> selectRating(5, ratingBar5, yellow, grey));
//        findViewById(R.id.rate4).setOnClickListener(v -> selectRating(4, ratingBar4, yellow, grey));
//        findViewById(R.id.rate3).setOnClickListener(v -> selectRating(3, ratingBar3, yellow, grey));
//        findViewById(R.id.rate2).setOnClickListener(v -> selectRating(2, ratingBar2, yellow, grey));
//        findViewById(R.id.rate1).setOnClickListener(v -> selectRating(1, ratingBar1, yellow, grey));
//    }
//
//    private void selectRating(int stars, RatingBar bar, int selColor, int defColor) {
//        selectedRating = stars;
//        ratingBar5.setRating(0);
//        ratingBar4.setRating(0);
//        ratingBar3.setRating(0);
//        ratingBar2.setRating(0);
//        ratingBar1.setRating(0);
//        ratingBar5.setProgressTintList(ColorStateList.valueOf(defColor));
//        ratingBar4.setProgressTintList(ColorStateList.valueOf(defColor));
//        ratingBar3.setProgressTintList(ColorStateList.valueOf(defColor));
//        ratingBar2.setProgressTintList(ColorStateList.valueOf(defColor));
//        ratingBar1.setProgressTintList(ColorStateList.valueOf(defColor));
//        bar.setRating(stars);
//        bar.setProgressTintList(ColorStateList.valueOf(selColor));
//    }
//
//    private void fetchBookings() {
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference ref = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyReviewDone");
//        ref.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                bookingSnapshots.clear();
//                for (DataSnapshot snap : snapshot.getChildren()) {
//                    bookingSnapshots.add(snap);
//                }
//            }
//            @Override
//            public void onCancelled(DatabaseError error) {
//                Toast.makeText(Comment.this, "Error fetching bookings", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
////    private void showBookingSelectionDialog() {
////        if (bookingSnapshots.isEmpty()) {
////            Toast.makeText(this, "No bookings available", Toast.LENGTH_SHORT).show();
////            return;
////        }
////        String[] labels = new String[bookingSnapshots.size()];
////        for (int i = 0; i < bookingSnapshots.size(); i++) {
////            labels[i] = "Booking " + (i+1);
////        }
////        final int[] selectedIndex = {-1};
////        new AlertDialog.Builder(this)
////                .setTitle("Select Booking")
////                .setSingleChoiceItems(labels, -1, (dialog, which) -> selectedIndex[0] = which)
////                .setPositiveButton("View Details", (dialog, which) -> {
////                    if (selectedIndex[0] >= 0) {
////                        showBookingDetailsDialog(selectedIndex[0]);
////                    } else {
////                        Toast.makeText(this, "Please select a booking", Toast.LENGTH_SHORT).show();
////                    }
////                })
////                .setNegativeButton("Cancel", null)
////                .show();
////    }
//
//    private void showBookingSelectionDialog() {
//        if (bookingSnapshots.isEmpty()) {
//            Toast.makeText(this, "No bookings available", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_booking_selection, null);
//        ListView listView = dialogView.findViewById(R.id.bookingListView);
//        Button btnViewDetails = dialogView.findViewById(R.id.btnViewDetails);
//        Button btnCancel = dialogView.findViewById(R.id.btnCancel);
//
//        String[] labels = new String[bookingSnapshots.size()];
//        for (int i = 0; i < bookingSnapshots.size(); i++) {
//            labels[i] = "Booking " + (i + 1);
//        }
//
//        final int[] selectedIndex = {-1};
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_booking_label, labels);
//        listView.setAdapter(adapter);
//        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
//        listView.setOnItemClickListener((parent, view, position, id) -> selectedIndex[0] = position);
//
//        AlertDialog dialog = new AlertDialog.Builder(this)
//                .setView(dialogView)
//                .create();
//
//        btnViewDetails.setOnClickListener(v -> {
//            if (selectedIndex[0] >= 0) {
//                showBookingDetailsDialog(selectedIndex[0]);
//                dialog.dismiss();
//            } else {
//                Toast.makeText(this, "Please select a booking", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        btnCancel.setOnClickListener(v -> dialog.dismiss());
//
//        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        dialog.show();
//    }
//
//
//
//    private void showBookingDetailsDialog(int index) {
//        DataSnapshot booking = bookingSnapshots.get(index);
//
//        View dialogView = LayoutInflater.from(this)
//                .inflate(R.layout.dialog_booking_details, null);
//        LinearLayout container = dialogView.findViewById(R.id.itemContainer);
//        Button okButton = dialogView.findViewById(R.id.btnOk);
//
//        populateDetails(booking, container);
//
//        AlertDialog alertDialog = new AlertDialog.Builder(this)
//                .setView(dialogView)
//                .setCancelable(false) // prevent outside dismiss
//                .create();
//
//        okButton.setOnClickListener(v -> {
//            // Prevent double tap or network fail chaos
//            okButton.setEnabled(false); // Disable button after click
//
//            selectedBookingIndex = index;
//            recentEditText.setText("Booking " + (index + 1));
//
//            alertDialog.dismiss(); // Close the dialog safely
//        });
//
//        alertDialog.show();
//        alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
//    }
//
//
//    private void populateDetails(DataSnapshot booking, LinearLayout container) {
//        container.removeAllViews();
//        LayoutInflater inflater = LayoutInflater.from(this);
//        // Accommodations
//        DataSnapshot acc = booking.child("accommodations");
//        if (acc.exists()) for (DataSnapshot item : acc.getChildren()) inflateItem(item, inflater, container);
//        // Food & Drinks
//        DataSnapshot food = booking.child("foodAndDrinks");
//        if (food.exists()) for (DataSnapshot item : food.getChildren()) inflateItem(item, inflater, container);
//        // Package
//        DataSnapshot pkg = booking.child("package");
//        if (pkg.exists()) inflateItem(pkg, inflater, container);
//    }
//
//    private void inflateItem(DataSnapshot itemSnap, LayoutInflater inflater, LinearLayout container) {
//        String name  = itemSnap.child("name").getValue(String.class);
//        Long qty     = itemSnap.child("quantity").getValue(Long.class);
//        Double price = itemSnap.child("price").getValue(Double.class);
//        if (name != null && qty != null && price != null) {
//            View row = inflater.inflate(R.layout.item_row, container, false);
//            ((TextView) row.findViewById(R.id.tvProductName)).setText(name);
//            ((TextView) row.findViewById(R.id.tvQty)).setText(String.valueOf(qty));
//            ((TextView) row.findViewById(R.id.tvPrice))
//                    .setText(String.format("₱%.2f", price));
//            container.addView(row);
//        }
//    }
//
//    /// at top of class
//    private boolean isSubmitting = false;
//
//    /// helper to check connectivity
//    private boolean isNetworkAvailable() {
//        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo net = cm.getActiveNetworkInfo();
//        return net != null && net.isConnected();
//    }
//
//   /// in Comment.java, replace submitRating() with:
//    private void submitRating() {
//        if (isSubmitting) return;
//        if (!isNetworkAvailable()) {
//            Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (selectedBookingIndex < 0) {
//            Toast.makeText(this, "Please select a booking first", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (selectedRating == 0) {
//            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        String commentText = commentEditText.getText().toString().trim();
//        if (commentText.isEmpty()) {
//            Toast.makeText(this, "Please add a comment", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        isSubmitting = true;
//        Button submitBtn = findViewById(R.id.Submit);
//        submitBtn.setEnabled(false);
//
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        String bookingKey = bookingSnapshots.get(selectedBookingIndex).getKey();
//        Map<String, Object> record = new HashMap<>();
//        record.put("user", userId);
//        record.put("bookingKey", bookingKey);
//        record.put("rate", selectedRating);
//        record.put("comment", commentText);
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
//        sdf.setTimeZone(TimeZone.getTimeZone("Asia/Manila"));
//        record.put("date", sdf.format(new Date()));
//
//        DatabaseReference ratingRef = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyRating");
//        ratingRef.push().setValue(record, (error, ref) -> {
//            isSubmitting = false;
//            submitBtn.setEnabled(true);
//            if (error == null) {
//                Toast.makeText(Comment.this, "Rating submitted", Toast.LENGTH_SHORT).show();
//                FirebaseDatabase.getInstance()
//                        .getReference("users").child(userId)
//                        .child("MyReviewDone").child(bookingKey)
//                        .removeValue();
//                bookingSnapshots.remove(selectedBookingIndex);
//                recentEditText.setText("Tap to select booking");
//                commentEditText.setText("");
//                selectRating(0, ratingBar1,
//                        getResources().getColor(R.color.yellow),
//                        getResources().getColor(R.color.grey));
//            } else {
//                Toast.makeText(Comment.this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//}
//



///Original
//package com.example.resort;
//
//import android.content.res.ColorStateList;
//import android.os.Bundle;
//import android.view.Gravity;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.RatingBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.content.ContextCompat;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Map;
//
//public class Comment extends AppCompatActivity {
//
//    // List to store review items with extra metadata.
//    private final ArrayList<ReviewItem> reviewItemsList = new ArrayList<>();
//    private EditText recentEditText;
//
//    // Displays the rating prompt.
//    private TextView messageTextView;
//
//    // For user's comment.
//    private EditText commentEditText;
//
//    // These variables will hold the selected rating value and review details.
//    private int selectedRating = 0;
//    private String selectedReviewCategory = "";
//
//    // Tracks which item was selected.
//    private int selectedReviewIndex = -1;
//
//    // RatingBar references for resetting later.
//    private RatingBar ratingBar5, ratingBar4, ratingBar3, ratingBar2, ratingBar1;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_comment);
//
//
//        Button back = findViewById(R.id.back2);
//        back.setOnClickListener(v -> finish());
//
//        // Initialize RatingBars.
//        ratingBar5 = findViewById(R.id.ratingBar5);
//        ratingBar4 = findViewById(R.id.ratingBar4);
//        ratingBar3 = findViewById(R.id.ratingBar3);
//        ratingBar2 = findViewById(R.id.ratingBar2);
//        ratingBar1 = findViewById(R.id.ratingBar1);
//
//        // Initialize Rate buttons.
//        Button btnRate5 = findViewById(R.id.rate5);
//        Button btnRate4 = findViewById(R.id.rate4);
//        Button btnRate3 = findViewById(R.id.rate3);
//        Button btnRate2 = findViewById(R.id.rate2);
//        Button btnRate1 = findViewById(R.id.rate1);
//
//        int yellowColor = ContextCompat.getColor(this, R.color.yellow);
//        int defaultColor = ContextCompat.getColor(this, R.color.grey);
//
//        // Set up rate button click listeners.
//        btnRate5.setOnClickListener(v -> {
//            ratingBar5.setRating(5);
//            ratingBar4.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 5;
//        });
//
//        btnRate4.setOnClickListener(v -> {
//            ratingBar4.setRating(4);
//            ratingBar5.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 4;
//        });
//
//        btnRate3.setOnClickListener(v -> {
//            ratingBar3.setRating(3);
//            ratingBar5.setRating(0);
//            ratingBar4.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 3;
//        });
//
//        btnRate2.setOnClickListener(v -> {
//            ratingBar2.setRating(2);
//            ratingBar5.setRating(0);
//            ratingBar4.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 2;
//        });
//
//        btnRate1.setOnClickListener(v -> {
//            ratingBar1.setRating(1);
//            ratingBar5.setRating(0);
//            ratingBar4.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 1;
//        });
//
//        // Initialize Recent field.
//        recentEditText = findViewById(R.id.Recent);
//        recentEditText.setText("Recent Booking");
//        recentEditText.setFocusable(false);
//
//        // Initialize rating prompt and comment field.
//        messageTextView = findViewById(R.id.textView2);
//        commentEditText = findViewById(R.id.Comment);
//
//        // Fetch review items from Firebase.
//        fetchReviewNames();
//
//        // Submit button setup.
//        Button submitButton = findViewById(R.id.Submit);
//        submitButton.setOnClickListener(v -> submitRating());
//    }
//
//    /**
//     * Fetch review items from the current user's "MyReview" node.
//     * Processes nodes "accommodations", "foodAndDrinks", and "package".
//     * For list nodes, each item’s key is stored; for "package", we assume a single object.
//     */
//
//
//    private void fetchReviewNames() {
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyReviewDone");
//
//        myReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                reviewItemsList.clear();
//                // Iterate through each review node.
//                for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
//                    String reviewKey = reviewSnapshot.getKey();
//                    // Process "accommodations".
//                    DataSnapshot accommodationsSnapshot = reviewSnapshot.child("accommodations");
//                    if (accommodationsSnapshot.exists()) {
//                        for (DataSnapshot itemSnapshot : accommodationsSnapshot.getChildren()) {
//                            String name = itemSnapshot.child("name").getValue(String.class);
//                            String category = itemSnapshot.child("category").getValue(String.class);
//                            if (name != null && category != null) {
//                                String itemKey = itemSnapshot.getKey();
//                                reviewItemsList.add(new ReviewItem(name, category, reviewKey, "accommodations", itemKey));
//                            }
//                        }
//                    }
//                    // Process "foodAndDrinks".
//                    DataSnapshot foodSnapshot = reviewSnapshot.child("foodAndDrinks");
//                    if (foodSnapshot.exists()) {
//                        for (DataSnapshot itemSnapshot : foodSnapshot.getChildren()) {
//                            String name = itemSnapshot.child("name").getValue(String.class);
//                            String category = itemSnapshot.child("category").getValue(String.class);
//                            if (name != null && category != null) {
//                                String itemKey = itemSnapshot.getKey();
//                                reviewItemsList.add(new ReviewItem(name, category, reviewKey, "foodAndDrinks", itemKey));
//                            }
//                        }
//                    }
//                    // Process "package" as a single object.
//                    DataSnapshot packageSnapshot = reviewSnapshot.child("package");
//                    if (packageSnapshot.exists()) {
//                        String name = packageSnapshot.child("name").getValue(String.class);
//                        String category = packageSnapshot.child("category").getValue(String.class);
//                        if (name != null && category != null) {
//                            // For package, we use "package" as the node type and key.
//                            reviewItemsList.add(new ReviewItem(name, category, reviewKey, "package", "package"));
//                        }
//                    }
//                }
//                setupRecentField();
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                Toast.makeText(Comment.this, "Error fetching data", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    /**
//     * Set up the Recent field so that tapping it shows a selection dialog with review items.
//     * When an item is selected, update the field text, store its index and category,
//     * and update the rating prompt accordingly.
//     */
//    private void setupRecentField() {
//        recentEditText.setOnClickListener(v -> {
//            if (!reviewItemsList.isEmpty()) {
//                CharSequence[] namesArray = new CharSequence[reviewItemsList.size()];
//                for (int i = 0; i < reviewItemsList.size(); i++) {
//                    namesArray[i] = reviewItemsList.get(i).name;
//                }
//
//                new AlertDialog.Builder(Comment.this)
//                        .setTitle("Tap to select review")
//                        .setItems(namesArray, (dialog, which) -> {
//                            ReviewItem selectedItem = reviewItemsList.get(which);
//                            recentEditText.setText(selectedItem.name);
//                            selectedReviewCategory = selectedItem.category.toLowerCase();
//                            selectedReviewIndex = which;
//
//                            // Update rating prompt based on category.
//                            switch (selectedReviewCategory) {
//                                case "boat":
//                                case "cottage":
//                                    messageTextView.setText("\"Please rate our accommodation\"");
//                                    break;
//                                case "food":
//                                case "dessert":
//                                case "beverage":
//                                case "alcohol":
//                                    messageTextView.setText("\"Please rate our Food & Drink\"");
//                                    break;
//                                case "package":
//                                    messageTextView.setText("\"Please rate our package\"");
//                                    break;
//                                default:
//                                    messageTextView.setText("\"Please rate our service\"");
//                                    break;
//                            }
//                        })
//                        .show();
//            } else {
//                Toast.makeText(Comment.this, "No review items available", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    /**
//     * Submits the rating record to the current user's "MyRating" node,
//     * removes the rated review item permanently from Firebase, and resets the UI.
//     */
//    private void submitRating() {
//        if (recentEditText.getText().toString().equals("Tap to select review") || selectedReviewCategory.isEmpty()) {
//            Toast.makeText(this, "Please select a review item", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (selectedRating == 0) {
//            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String commentText = commentEditText.getText().toString().trim();
//        /// Add condition to check if the comment field is empty
//        if (commentText.isEmpty()) {
//            Toast.makeText(this, "Please add a comment", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        /// Get the current user's ID and reference to their data
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
//
//        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                ///Inside the onDataChange() method of userRef.child("username").addListenerForSingleValueEvent(...)
//                String username = snapshot.getValue(String.class);
//                if (username == null || username.isEmpty()) {
//                    username = "Unknown User";
//                }
//
//                Map<String, Object> ratingRecord = new HashMap<>();
//                ratingRecord.put("user", username);
//                ratingRecord.put("rate", selectedRating);
//                ratingRecord.put("comment", commentText);
//                ratingRecord.put("category", selectedReviewCategory);
//
//                // New field: include the item name.
//                if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
//                    ratingRecord.put("itemName", reviewItemsList.get(selectedReviewIndex).name);
//                }
//
//                // Format date with AM/PM in Philippine time.
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
//                sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Manila"));
//                String currentDate = sdf.format(new Date());
//                ratingRecord.put("date", currentDate);
//                // Then push the ratingRecord to Firebase as before.
//
//                DatabaseReference ratingRef = FirebaseDatabase.getInstance()
//                        .getReference("users")
//                        .child(userId)
//                        .child("MyRating");
//                ratingRef.push().setValue(ratingRecord, (error, ref) -> {
//                    if (error == null) {
//                        Toast.makeText(Comment.this, "Rating submitted successfully", Toast.LENGTH_SHORT).show();
//
//                        // Remove the rated review item permanently from Firebase.
//                        if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
//                            ReviewItem item = reviewItemsList.get(selectedReviewIndex);
//                            DatabaseReference itemRef;
//                            if (item.nodeType.equals("package")) {
//                                itemRef = FirebaseDatabase.getInstance()
//                                        .getReference("users")
//                                        .child(userId)
//                                        .child("MyReviewDone")
//                                        .child(item.parentReviewKey)
//                                        .child("package");
//                            } else {
//                                itemRef = FirebaseDatabase.getInstance()
//                                        .getReference("users")
//                                        .child(userId)
//                                        .child("MyReviewDone")
//                                        .child(item.parentReviewKey)
//                                        .child(item.nodeType)
//                                        .child(item.itemKey);
//                            }
//                            itemRef.removeValue();
//                        }
//
//                        /// Remove the item from the in-memory list.
//                        if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
//                            reviewItemsList.remove(selectedReviewIndex);
//                        }
//
//                        /// Reset the UI fields.
//                        recentEditText.setText("Tap to select review");
//                        selectedReviewCategory = "";
//                        selectedReviewIndex = -1;
//                        selectedRating = 0;
//                        commentEditText.setText("");
//
//                        /// Reset all RatingBars to zero.
//                        ratingBar5.setRating(0);
//                        ratingBar4.setRating(0);
//                        ratingBar3.setRating(0);
//                        ratingBar2.setRating(0);
//                        ratingBar1.setRating(0);
//                    } else {
//                        Toast.makeText(Comment.this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                Toast.makeText(Comment.this, "Failed to retrieve user info", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    //Inner class to hold review item data along with Firebase metadata.
//    private static class ReviewItem {
//        String name;
//        String category;
//        String parentReviewKey; // The key of the review node in MyReview.
//        String nodeType; // "accommodations", "foodAndDrinks", or "package"
//        String itemKey;  // The key of the item (for list nodes) or "package" for a package.
//
//        ReviewItem(String name, String category, String parentReviewKey, String nodeType, String itemKey) {
//            this.name = name;
//            this.category = category;
//            this.parentReviewKey = parentReviewKey;
//            this.nodeType = nodeType;
//            this.itemKey = itemKey;
//        }
//    }
//}


///No Current User
//package com.example.resort;
//
//import android.content.res.ColorStateList;
//import android.os.Bundle;
//import android.view.Gravity;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.RatingBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.content.ContextCompat;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Map;
//
//public class Comment extends AppCompatActivity {
//
//    // List to store review items with extra metadata.
//    private final ArrayList<ReviewItem> reviewItemsList = new ArrayList<>();
//    private EditText recentEditText;
//
//    // Displays the rating prompt.
//    private TextView messageTextView;
//
//    // For user's comment.
//    private EditText commentEditText;
//
//    // These variables will hold the selected rating value and review details.
//    private int selectedRating = 0;
//    private String selectedReviewCategory = "";
//
//    // Tracks which item was selected.
//    private int selectedReviewIndex = -1;
//
//    // RatingBar references for resetting later.
//    private RatingBar ratingBar5, ratingBar4, ratingBar3, ratingBar2, ratingBar1;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_comment);
//
//
//        Button back = findViewById(R.id.back2);
//        back.setOnClickListener(v -> finish());
//
//        // Initialize RatingBars.
//        ratingBar5 = findViewById(R.id.ratingBar5);
//        ratingBar4 = findViewById(R.id.ratingBar4);
//        ratingBar3 = findViewById(R.id.ratingBar3);
//        ratingBar2 = findViewById(R.id.ratingBar2);
//        ratingBar1 = findViewById(R.id.ratingBar1);
//
//        // Initialize Rate buttons.
//        Button btnRate5 = findViewById(R.id.rate5);
//        Button btnRate4 = findViewById(R.id.rate4);
//        Button btnRate3 = findViewById(R.id.rate3);
//        Button btnRate2 = findViewById(R.id.rate2);
//        Button btnRate1 = findViewById(R.id.rate1);
//
//        int yellowColor = ContextCompat.getColor(this, R.color.yellow);
//        int defaultColor = ContextCompat.getColor(this, R.color.grey);
//
//        // Set up rate button click listeners.
//        btnRate5.setOnClickListener(v -> {
//            ratingBar5.setRating(5);
//            ratingBar4.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 5;
//        });
//
//        btnRate4.setOnClickListener(v -> {
//            ratingBar4.setRating(4);
//            ratingBar5.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 4;
//        });
//
//        btnRate3.setOnClickListener(v -> {
//            ratingBar3.setRating(3);
//            ratingBar5.setRating(0);
//            ratingBar4.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 3;
//        });
//
//        btnRate2.setOnClickListener(v -> {
//            ratingBar2.setRating(2);
//            ratingBar5.setRating(0);
//            ratingBar4.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 2;
//        });
//
//        btnRate1.setOnClickListener(v -> {
//            ratingBar1.setRating(1);
//            ratingBar5.setRating(0);
//            ratingBar4.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 1;
//        });
//
//        // Initialize Recent field.
//        recentEditText = findViewById(R.id.Recent);
//        recentEditText.setText("Recent Booking");
//        recentEditText.setFocusable(false);
//
//        // Initialize rating prompt and comment field.
//        messageTextView = findViewById(R.id.textView2);
//        commentEditText = findViewById(R.id.Comment);
//
//        // Fetch review items from Firebase.
//        fetchReviewNames();
//
//        // Submit button setup.
//        Button submitButton = findViewById(R.id.Submit);
//        submitButton.setOnClickListener(v -> submitRating());
//    }
//
//    /**
//     * Fetch review items from the current user's "MyReview" node.
//     * Processes nodes "accommodations", "foodAndDrinks", and "package".
//     * For list nodes, each item’s key is stored; for "package", we assume a single object.
//     */
//
//
//    private void fetchReviewNames() {
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyReviewDone");
//
//        myReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                reviewItemsList.clear();
//                // Iterate through each review node.
//                for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
//                    String reviewKey = reviewSnapshot.getKey();
//                    // Process "accommodations".
//                    DataSnapshot accommodationsSnapshot = reviewSnapshot.child("accommodations");
//                    if (accommodationsSnapshot.exists()) {
//                        for (DataSnapshot itemSnapshot : accommodationsSnapshot.getChildren()) {
//                            String name = itemSnapshot.child("name").getValue(String.class);
//                            String category = itemSnapshot.child("category").getValue(String.class);
//                            if (name != null && category != null) {
//                                String itemKey = itemSnapshot.getKey();
//                                reviewItemsList.add(new ReviewItem(name, category, reviewKey, "accommodations", itemKey));
//                            }
//                        }
//                    }
//                    // Process "foodAndDrinks".
//                    DataSnapshot foodSnapshot = reviewSnapshot.child("foodAndDrinks");
//                    if (foodSnapshot.exists()) {
//                        for (DataSnapshot itemSnapshot : foodSnapshot.getChildren()) {
//                            String name = itemSnapshot.child("name").getValue(String.class);
//                            String category = itemSnapshot.child("category").getValue(String.class);
//                            if (name != null && category != null) {
//                                String itemKey = itemSnapshot.getKey();
//                                reviewItemsList.add(new ReviewItem(name, category, reviewKey, "foodAndDrinks", itemKey));
//                            }
//                        }
//                    }
//                    // Process "package" as a single object.
//                    DataSnapshot packageSnapshot = reviewSnapshot.child("package");
//                    if (packageSnapshot.exists()) {
//                        String name = packageSnapshot.child("name").getValue(String.class);
//                        String category = packageSnapshot.child("category").getValue(String.class);
//                        if (name != null && category != null) {
//                            // For package, we use "package" as the node type and key.
//                            reviewItemsList.add(new ReviewItem(name, category, reviewKey, "package", "package"));
//                        }
//                    }
//                }
//                setupRecentField();
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                Toast.makeText(Comment.this, "Error fetching data", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    /**
//     * Set up the Recent field so that tapping it shows a selection dialog with review items.
//     * When an item is selected, update the field text, store its index and category,
//     * and update the rating prompt accordingly.
//     */
//    private void setupRecentField() {
//        recentEditText.setOnClickListener(v -> {
//            if (!reviewItemsList.isEmpty()) {
//                CharSequence[] namesArray = new CharSequence[reviewItemsList.size()];
//                for (int i = 0; i < reviewItemsList.size(); i++) {
//                    namesArray[i] = reviewItemsList.get(i).name;
//                }
//
//                new AlertDialog.Builder(Comment.this)
//                        .setTitle("Tap to select review")
//                        .setItems(namesArray, (dialog, which) -> {
//                            ReviewItem selectedItem = reviewItemsList.get(which);
//                            recentEditText.setText(selectedItem.name);
//                            selectedReviewCategory = selectedItem.category.toLowerCase();
//                            selectedReviewIndex = which;
//
//                            // Update rating prompt based on category.
//                            switch (selectedReviewCategory) {
//                                case "boat":
//                                case "cottage":
//                                    messageTextView.setText("\"Please rate our accommodation\"");
//                                    break;
//                                case "food":
//                                case "dessert":
//                                case "beverage":
//                                case "alcohol":
//                                    messageTextView.setText("Please rate our Food & Drink");
//                                    break;
//                                case "package":
//                                    messageTextView.setText("Please rate our package");
//                                    break;
//                                default:
//                                    messageTextView.setText("Please rate our service");
//                                    break;
//                            }
//                        })
//                        .show();
//            } else {
//                Toast.makeText(Comment.this, "No review items available", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    /**
//     * Submits the rating record to the current user's "MyRating" node,
//     * removes the rated review item permanently from Firebase, and resets the UI.
//     */
//    private void submitRating() {
//        if (recentEditText.getText().toString().equals("Tap to select review") || selectedReviewCategory.isEmpty()) {
//            Toast.makeText(this, "Please select a review item", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (selectedRating == 0) {
//            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String commentText = commentEditText.getText().toString().trim();
//        /// Add condition to check if the comment field is empty
//        if (commentText.isEmpty()) {
//            Toast.makeText(this, "Please add a comment", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        /// Get the current user's ID and reference to their data
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
//
//        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                ///Inside the onDataChange() method of userRef.child("username").addListenerForSingleValueEvent(...)
//                String username = snapshot.getValue(String.class);
//                if (username == null || username.isEmpty()) {
//                    username = "Unknown User";
//                }

//                Map<String, Object> ratingRecord = new HashMap<>();
//                ratingRecord.put("user", username);
//                ratingRecord.put("rate", selectedRating);
//                ratingRecord.put("comment", commentText);
//                ratingRecord.put("category", selectedReviewCategory);
//
//                // New field: include the item name.
//                if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
//                    ratingRecord.put("itemName", reviewItemsList.get(selectedReviewIndex).name);
//                }
//
//                // Format date with AM/PM in Philippine time.
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
//                sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Manila"));
//                String currentDate = sdf.format(new Date());
//                ratingRecord.put("date", currentDate);
//                // Then push the ratingRecord to Firebase as before.
//
//                DatabaseReference ratingRef = FirebaseDatabase.getInstance()
//                        .getReference("users")
//                        .child(userId)
//                        .child("MyRating");
//                ratingRef.push().setValue(ratingRecord, (error, ref) -> {
//                    if (error == null) {
//                        Toast.makeText(Comment.this, "Rating submitted successfully", Toast.LENGTH_SHORT).show();
//
//                        // Remove the rated review item permanently from Firebase.
//                        if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
//                            ReviewItem item = reviewItemsList.get(selectedReviewIndex);
//                            DatabaseReference itemRef;
//                            if (item.nodeType.equals("package")) {
//                                itemRef = FirebaseDatabase.getInstance()
//                                        .getReference("users")
//                                        .child(userId)
//                                        .child("MyReviewDone")
//                                        .child(item.parentReviewKey)
//                                        .child("package");
//                            } else {
//                                itemRef = FirebaseDatabase.getInstance()
//                                        .getReference("users")
//                                        .child(userId)
//                                        .child("MyReviewDone")
//                                        .child(item.parentReviewKey)
//                                        .child(item.nodeType)
//                                        .child(item.itemKey);
//                            }
//                            itemRef.removeValue();
//                        }
//
//                        /// Remove the item from the in-memory list.
//                        if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
//                            reviewItemsList.remove(selectedReviewIndex);
//                        }
//
//                        /// Reset the UI fields.
//                        recentEditText.setText("Tap to select review");
//                        selectedReviewCategory = "";
//                        selectedReviewIndex = -1;
//                        selectedRating = 0;
//                        commentEditText.setText("");
//
//                        /// Reset all RatingBars to zero.
//                        ratingBar5.setRating(0);
//                        ratingBar4.setRating(0);
//                        ratingBar3.setRating(0);
//                        ratingBar2.setRating(0);
//                        ratingBar1.setRating(0);
//                    } else {
//                        Toast.makeText(Comment.this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                Toast.makeText(Comment.this, "Failed to retrieve user info", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    //Inner class to hold review item data along with Firebase metadata.
//    private static class ReviewItem {
//        String name;
//        String category;
//        String parentReviewKey; // The key of the review node in MyReview.
//        String nodeType; // "accommodations", "foodAndDrinks", or "package"
//        String itemKey;  // The key of the item (for list nodes) or "package" for a package.
//
//        ReviewItem(String name, String category, String parentReviewKey, String nodeType, String itemKey) {
//            this.name = name;
//            this.category = category;
//            this.parentReviewKey = parentReviewKey;
//            this.nodeType = nodeType;
//            this.itemKey = itemKey;
//        }
//    }
//}
//
