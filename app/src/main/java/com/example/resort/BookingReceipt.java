package com.example.resort;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.credentials.Credential;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.se.omapi.Session;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.android.volley.Response;
import com.example.resort.addcart.data.CartItem;
import com.example.resort.addcart.data.CartManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Request;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Call;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;

import okhttp3.OkHttpClient;


public class BookingReceipt extends AppCompatActivity {

    private TextView detailBooking;
    private TextView totalPaymentText;
    private TextView downPaymentText;
    private TextView nameTextView;
    private TextView phoneTextView;
    private TextView emailTextView;
    private TextView messageTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking_receipt);

        // Adjust layout for system insets.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back arrow functionality.
        ImageView back = findViewById(R.id.arrow);
        back.setOnClickListener(v -> onBackPressed());

        // Initialize receipt-related TextViews.
        detailBooking = findViewById(R.id.DetailBooking);
        totalPaymentText = findViewById(R.id.Totalpayment);
        downPaymentText = findViewById(R.id.Downpayment);
        TextView dateTextView = findViewById(R.id.date); // For booking date/time info

        // Initialize user details TextViews.
        nameTextView = findViewById(R.id.name);
        phoneTextView = findViewById(R.id.phone);
        emailTextView = findViewById(R.id.email);

        // Initialize the thank you message TextView.
        messageTextView = findViewById(R.id.message);

        // Set a bold monospace typeface for the receipt details.
        detailBooking.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);

        // Retrieve and display the formatted booking info passed from Booking activity.
        String bookingDate = getIntent().getStringExtra("BOOKING_DATE");
        if (bookingDate != null && !bookingDate.isEmpty()) {
            dateTextView.setText(bookingDate);
        } else {
            dateTextView.setText("Date info not available");
        }

        /// Load user details from Firebase.
        loadUserDetails();

        /// Load the cart details (receipt).
        loadCartDetails();

        /// Set up the SaveReceipt (print) button.

        //setupSaveReceiptFunction();

        /// Set up the submit button to show the custom confirmation dialog.
        Button submitButton = findViewById(R.id.submit);
        submitButton.setOnClickListener(v -> submitBookingToFirebase());
    }

    /// --------------------- Receipt & UI Helper Methods ---------------------
//    private void setupSaveReceiptFunction() {
//        ImageButton btnSaveReceipt = findViewById(R.id.SaveReceipt);
//        btnSaveReceipt.setOnClickListener(v -> {
//            // Inflate the custom layout
//            LayoutInflater inflater = LayoutInflater.from(BookingReceipt.this);
//            View dialogView = inflater.inflate(R.layout.custom_alert_dialog, null);
//
//            // Build the AlertDialog using the custom layout
//            AlertDialog.Builder builder = new AlertDialog.Builder(BookingReceipt.this)
//                    .setView(dialogView);
//            final AlertDialog dialog = builder.create();
//
//            // Get references to buttons in the custom layout
//            Button btnSave = dialogView.findViewById(R.id.btnSave);
//            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
//
//            // Save button functionality (using your existing logic)
//            btnSave.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    final Button submitButton = findViewById(R.id.submit);
//
//                    // Hide buttons temporarily.
//                    btnSaveReceipt.setVisibility(View.INVISIBLE);
//                    submitButton.setVisibility(View.INVISIBLE);
//
//                    btnSaveReceipt.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            // --- Temporarily update UI for the saved receipt ---
//                            final ImageView arrowView = findViewById(R.id.arrow);
//                            final int originalArrowVisibility = arrowView.getVisibility();
//                            arrowView.setVisibility(View.INVISIBLE);
//
//                            final String originalMessage = messageTextView.getText().toString();
//                            final Drawable[] originalDrawables = messageTextView.getCompoundDrawables();
//                            messageTextView.setText("Thank you for purchasing Sir/Ma'am.");
//                            messageTextView.setCompoundDrawables(null, null, null, null);
//
//                            View contentView = findViewById(android.R.id.content);
//                            Bitmap screenshot = getScreenShot(contentView);
//
//                            // --- Restore UI elements ---
//                            messageTextView.setText(originalMessage);
//                            messageTextView.setCompoundDrawables(
//                                    originalDrawables[0],
//                                    originalDrawables[1],
//                                    originalDrawables[2],
//                                    originalDrawables[3]);
//                            arrowView.setVisibility(originalArrowVisibility);
//
//                            btnSaveReceipt.setVisibility(View.VISIBLE);
//                            btnSaveReceipt.setEnabled(true);
//                            submitButton.setVisibility(View.VISIBLE);
//                            submitButton.setEnabled(true);
//
//                            boolean isSaved = saveReceiptImage(screenshot);
//                            if (isSaved) {
//                                Toast.makeText(BookingReceipt.this, "Receipt saved to gallery", Toast.LENGTH_SHORT).show();
//                            } else {
//                                Toast.makeText(BookingReceipt.this, "Error saving receipt", Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    }, 200); // 200ms delay for UI update
//
//                    dialog.dismiss();
//                }
//            });
//
//            // Cancel button simply dismisses the dialog
//            btnCancel.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    dialog.dismiss();
//                }
//            });
//
//            // Show the custom dialog
//            dialog.show();
//        });
//    }



    /// Helper method to capture a screenshot of a given view.
    private Bitmap getScreenShot(View view) {
        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        view.draw(canvas);
        return bitmap;
    }

    /// Helper method to save the screenshot image to the device's gallery.
    private boolean saveReceiptImage(Bitmap bitmap) {
        try {
            String savedImageURL = MediaStore.Images.Media.insertImage(
                    getContentResolver(),
                    bitmap,
                    "Receipt_" + System.currentTimeMillis(),
                    "Receipt image"
            );
            return savedImageURL != null;
        } catch (Exception e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return false;
        }
    }

    /// --------------------- Firebase Data Methods ---------------------

    private void loadUserDetails() {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            String userId = currentUser.getUid();
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
            mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String firstName = snapshot.child("firstName").getValue(String.class);
                        String middleName = snapshot.child("middleInitial").getValue(String.class);
                        String lastName = snapshot.child("lastName").getValue(String.class);
                        String phone = snapshot.child("phoneNumber").getValue(String.class);
                        String email = snapshot.child("email").getValue(String.class);

                        String middleInitial = "";
                        if (middleName != null && !middleName.isEmpty()) {
                            middleInitial = middleName.substring(0, 1).toUpperCase();
                        }

                        String fullName;
                        if ((lastName == null || lastName.isEmpty()) &&
                                firstName != null && !firstName.isEmpty() &&
                                !middleInitial.isEmpty()) {
                            fullName = firstName + " the " + middleInitial + ".";
                        } else {
                            StringBuilder builder = new StringBuilder();
                            if (firstName != null && !firstName.isEmpty()) { builder.append(firstName); }
                            if (!middleInitial.isEmpty()) { builder.append(" ").append(middleInitial).append("."); }
                            if (lastName != null && !lastName.isEmpty()) { builder.append(" ").append(lastName); }
                            fullName = builder.toString().trim();
                        }

                        nameTextView.setText("Name: " + (fullName.isEmpty() ? "N/A" : fullName));
                        phoneTextView.setText("Phone: " + (phone != null ? phone : "N/A"));
                        emailTextView.setText("Email: " + (email != null ? email : "N/A"));
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e("BookingReceipt", "Failed to load user details: " + error.getMessage());
                }
            });
        } else {
            nameTextView.setText("Name: Guest");
            phoneTextView.setText("Phone: N/A");
            emailTextView.setText("Email: N/A");
        }
    }


    @SuppressLint("SetTextI18n")
    private void loadCartDetails() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(BookingReceipt.this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        List<CartItem> cartItems = CartManager.getInstance(BookingReceipt.this, userId).getCartItems();

        Log.d("BookingReceipt", "Total Cart Items: " + cartItems.size());
        for (CartItem item : cartItems) {
            Log.d("BookingReceipt", "Item: " + item.getName() + ", Category: " + item.getCategory() + ", Quantity: " + item.getQuantity());
        }

        List<CartItem> accommodations = new ArrayList<>();
        List<CartItem> foodAndDrinks = new ArrayList<>();
        List<CartItem> packages = new ArrayList<>();

        for (CartItem item : cartItems) {
            String category = item.getCategory().toLowerCase();
            switch (category) {
                case "accommodation":
                case "boat":
                case "cottage":
                case "room":
                    accommodations.add(item);
                    break;
                case "food":
                case "dessert":
                case "beverage":
                case "alcohol":
                    foodAndDrinks.add(item);
                    break;
                case "package":
                    packages.add(item);
                    break;
                default:
                    Log.w("BookingReceipt", "Uncategorized Item: " + item.getName());
            }
        }

        StringBuilder receipt = new StringBuilder();
        String formatLine = "%-26s %10s\n";

        if (!accommodations.isEmpty()) {
            receipt.append("Accommodation\n");
            for (CartItem item : accommodations) {
                String itemName = item.getName();
                if (item.getQuantity() > 1) {
                    itemName += " (X" + item.getQuantity() + ")";
                }
                String price = "₱" + String.format(Locale.US, "%.2f", item.getPrice() * item.getQuantity());
                receipt.append(String.format(Locale.US, formatLine, itemName, price));
            }
            receipt.append("\n");
        }

        if (!foodAndDrinks.isEmpty()) {
            receipt.append("Food/Dessert/Beverage/Alcohol\n");
            for (CartItem item : foodAndDrinks) {
                String itemName = item.getName();
                if (item.getQuantity() > 1) {
                    itemName += " (X" + item.getQuantity() + ")";
                }
                String price = "₱" + String.format(Locale.US, "%.2f", item.getPrice() * item.getQuantity());
                receipt.append(String.format(Locale.US, formatLine, itemName, price));
            }
            receipt.append("\n");
        }

        if (!packages.isEmpty()) {
            receipt.append("Package\n");
            for (CartItem item : packages) {
                String itemName = item.getName();
                if (item.getQuantity() > 1) {
                    itemName += " (X" + item.getQuantity() + ")";
                }
                String price = "₱" + String.format(Locale.US, "%.2f", item.getPrice() * item.getQuantity());
                receipt.append(String.format(Locale.US, formatLine, itemName, price));
            }
            receipt.append("\n");
        }

        detailBooking.setText(receipt.toString());

        double total = 0.0;
        for (CartItem item : cartItems) {
            total += item.getPrice() * item.getQuantity();
        }
        double downPayment = total * 0.50;

        totalPaymentText.setText(String.format(Locale.US, "₱%.2f", total));
        downPaymentText.setText(String.format(Locale.US, "₱%.2f", downPayment));
    }

    /// --------------------- Firebase Booking Submission Methods ---------------------

    /// Shows the custom confirmation dialog using your layout (dialog_submission.xml)
    private void submitBookingToFirebase() {
        AlertDialog.Builder builder = new AlertDialog.Builder(BookingReceipt.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_submission, null);
        builder.setView(dialogView);

        AlertDialog alertDialog = builder.create();
        Button submissionButton = dialogView.findViewById(R.id.Submission);
        submissionButton.setOnClickListener(v -> {
            sendBookingData();
            alertDialog.dismiss();
        });

        alertDialog.show();
    }


    ///Send Booking Data
    private void sendBookingData() {
        // Retrieve user details from TextViews.
        String name = nameTextView.getText().toString().replace("Name: ", "").trim();
        String phone = phoneTextView.getText().toString().replace("Phone: ", "").trim();
        String email = emailTextView.getText().toString().replace("Email: ", "").trim();

        // Retrieve booking date.
        TextView dateTextView = findViewById(R.id.date);
        String bookingDate = dateTextView.getText().toString().trim();

        // Set default statuses.
        String statusReview = "Pending";
        String paymentStatus = "Pending";

        // Generate a unique booking reference.
        String referenceNo = generateRandomReference();

        // Calculate total amount from cart items.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(BookingReceipt.this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        List<CartItem> cartItems = CartManager.getInstance(BookingReceipt.this, userId).getCartItems();
        double totalAmount = cartItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();


        // Categorize cart items.
        List<CartItem> accommodations = new ArrayList<>();
        List<CartItem> foodAndDrinks = new ArrayList<>();
        List<CartItem> packages = new ArrayList<>();
        for (CartItem item : cartItems) {
            String category = item.getCategory().toLowerCase();
            switch (category) {
                case "accommodation":
                case "boat":
                case "cottage":
                case "room":
                    accommodations.add(item);
                    break;
                case "food":
                case "dessert":
                case "beverage":
                case "alcohol":
                    foodAndDrinks.add(item);
                    break;
                case "package":
                    packages.add(item);
                    break;
                default:
                    Log.w("BookingReceipt", "Uncategorized Item: " + item.getName());
            }
        }
        ///Date
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
        String formattedDate = sdf.format(new Date());

        /// Build bookingReview map.
        Map<String, Object> bookingReview = new HashMap<>();
        bookingReview.put("statusReview", statusReview);
        bookingReview.put("name", name);
        bookingReview.put("bookingDate", bookingDate);
        bookingReview.put("refNo", referenceNo);
        bookingReview.put("email", email);
        bookingReview.put("phone", phone);
        ///New Data Add
        bookingReview.put("downPayment", totalAmount * 0.50);
        bookingReview.put("amount", totalAmount);

        Map<String, Object> orderItems = new HashMap<>();
        orderItems.put("accommodations", cartItemsToMapList(accommodations));
        orderItems.put("foodAndDrinks", cartItemsToMapList(foodAndDrinks));
        if (!packages.isEmpty()) {
            orderItems.put("package", cartItemToMap(packages.get(0)));
        }
        bookingReview.put("orderItems", orderItems);

        /// Build paymentTransaction map.
        Map<String, Object> paymentTransaction = new HashMap<>();
        paymentTransaction.put("paymentStatus", paymentStatus);
        paymentTransaction.put("finalStatus", paymentStatus);
        paymentTransaction.put("name", name);
        paymentTransaction.put("refNo", referenceNo);
        paymentTransaction.put("amount", totalAmount);
        paymentTransaction.put("PaymentDate", formattedDate);
        paymentTransaction.put("downPayment", totalAmount * 0.50);

        /// Build paymentMethod map with default values ("N/A").
        Map<String, Object> paymentMethod = new HashMap<>();
        paymentMethod.put("Payment", "N/A");
        paymentMethod.put("Reference", "N/A");
        paymentMethod.put("Firstname", "N/A");
        paymentMethod.put("Lastname", "N/A");
        paymentMethod.put("Phone", "N/A");
        paymentMethod.put("Amount", "N/A");
        paymentMethod.put("Status", "N/A");


        /// Combine the booking data.
        Map<String, Object> bookingData = new HashMap<>();
        bookingData.put("bookingReview", bookingReview);
        bookingData.put("paymentTransaction", paymentTransaction);
        bookingData.put("paymentMethod", paymentMethod);


        SharedPreferences preferences = getSharedPreferences("BookingPref_" + userId, MODE_PRIVATE);
        /// Prevent a new booking if any booking state is active.
        if (preferences.getBoolean("bookingSubmitted", false) ||
                preferences.getBoolean("paymentSubmitted", false) ||
                preferences.getBoolean("paymentApproved", false) ||
                preferences.getBoolean("reviewApproved", false) ||
                preferences.getBoolean("finalApproved", false)) {
            Toast.makeText(BookingReceipt.this, "Sorry, your booking is still in progress.", Toast.LENGTH_SHORT).show();
            return;
        }


        /// Update userId from the current user.
        userId = currentUser.getUid();
        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("MyBooking");

        /// Check if any booking data exists in MyBooking.
        String finalUserId = userId;
        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // If any booking exists, abort the submission.
                    Toast.makeText(BookingReceipt.this,
                            "Sorry, you already have an active booking.", Toast.LENGTH_SHORT).show();
                } else {
                    /// No pending booking exists; proceed with submission.
                    String bookingId = bookingRef.push().getKey();
                    if (bookingId != null) {
                        bookingRef.child(bookingId).setValue(bookingData)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        Toast.makeText(BookingReceipt.this, "Booking submitted successfully", Toast.LENGTH_SHORT).show();


                                        /// ******************* New Code Start *******************
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                                        String formattedDate = sdf.format(new Date());

                                        DatabaseReference bookingRequestRef = FirebaseDatabase.getInstance()
                                                .getReference("bookingRequest");

                                        Map<String, Object> bookingRequest = new HashMap<>();
                                        bookingRequest.put("message", "Booking request by " + name);
                                        bookingRequest.put("date", formattedDate);
                                        ///bookingRequestRef.push().setValue( bookingRequest);

                                        DatabaseReference newReqRef = bookingRequestRef.push();
                                        newReqRef.setValue(bookingRequest)
                                                .addOnSuccessListener(aVoid -> {
                                                    /// Simplified Telegram message without technical IDs
                                                    String telegramMsg = "🔔 New Booking Request 🔔\n"
                                                            + "👤 Name: " + name + "\n"
                                                            + "📅 Date: " + formattedDate + "\n"
                                                            + "⏳ Status: " + "Pending";

                                                    sendTelegramNotification(telegramMsg);
                                                })
                                                .addOnFailureListener(e -> {
                                                    Log.e("Firebase", "BookingRequest failed", e);
                                                });

                                        /// ******************* New Code End *******************

                                        /// Clean up previous notifications.
                                        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
                                                .getReference("users")
                                                .child(finalUserId)
                                                .child("notifications");
                                        notificationRef.removeValue();

                                        /// Copy orderItems to MyReview node with a default statusReview of "Pending".
                                        Object orderItemsObj = bookingReview.get("orderItems");
                                        if (orderItemsObj instanceof Map) {
                                            /// Cast orderItemsObj to a Map
                                            @SuppressWarnings("unchecked")
                                            Map<String, Object> orderItemsMap = (Map<String, Object>) orderItemsObj;

                                            /// Add default statusReview
                                            orderItemsMap.put("statusReview", "Pending");

                                            /// Get a reference to the MyReview node
                                            DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
                                                    .getReference("users")
                                                    .child(finalUserId)
                                                    .child("MyReview");

                                            /// Generate a unique key for the new review
                                            String reviewId = myReviewRef.push().getKey();
                                            if (reviewId != null) {
                                                /// Set the value of the new review to orderItemsMap
                                                myReviewRef.child(reviewId).setValue(orderItemsMap);
                                            }
                                        }

                                        /// Append available date to booked items.
                                        if (orderItemsObj instanceof Map) {
                                            ///noinspection unchecked
                                            updateBookedItemsStatus((Map<String, Object>) orderItemsObj, bookingDate);
                                        }

                                        /// Permanently delete cart items.
                                        CartManager.getInstance(BookingReceipt.this, finalUserId).clearCartItems();

                                        /// Save booking state persistently.
                                        SharedPreferences preferences = getSharedPreferences("BookingPref_" + finalUserId,  MODE_PRIVATE);
                                        SharedPreferences.Editor editor = preferences.edit();
                                        editor.putBoolean("bookingSubmitted", true);
                                        editor.apply();

                                        /// ******************* Start Foreground Service *******************
                                        /// This will start your BookingStatusService as a foreground service.
                                        Intent serviceIntent = new Intent(BookingReceipt.this, BookingStatusService.class);
                                        androidx.core.content.ContextCompat.startForegroundService(BookingReceipt.this, serviceIntent);
                                        Log.d("BookingReceipt", "Foreground service started");
                                        /// ******************* End Foreground Service *******************


                                        // Navigate to BookingStatus, which reads the saved state.
                                        Intent intent = new Intent(BookingReceipt.this, BookingStatus.class);
                                        intent.putExtra("bookingSubmitted", true);
                                        startActivity(intent);
                                        overridePendingTransition(0, 0);
                                        finish();
                                    } else {
                                        Toast.makeText(BookingReceipt.this, "Error submitting booking", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        Toast.makeText(BookingReceipt.this, "Error generating booking ID", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BookingReceipt.this, "Error checking booking status", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void sendTelegramNotification(String message) {
        new Thread(() -> {
            try {
                String botToken = "7263113934:AAHIz9CRO-7zgvkK_75b9BCFcaN3lrRXGqo";
                String chatId = "7259957866";

                String urlString = "https://api.telegram.org/bot" + botToken
                        + "/sendMessage?chat_id=" + chatId
                        + "&text=" + URLEncoder.encode(message, "UTF-8");

                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                /// Log the full URL (for debugging)
                Log.d("Telegram", "Request URL: " + urlString);

                int responseCode = conn.getResponseCode();
                if (responseCode == 200) {
                    Log.i("Telegram", "Message sent successfully");
                } else {
                    /// Read error response
                    InputStream errorStream = conn.getErrorStream();
                    if (errorStream != null) {
                        String error = new Scanner(errorStream).useDelimiter("\\A").next();
                        Log.e("Telegram", "API Error: " + error);
                    }
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e("Telegram", "Error: ", e);
            }
        }).start();
    }

    /// --------------------- Helper Methods ---------------------

    /// Generates a unique reference number.
    private String generateRandomReference() {
        return "REF" + System.currentTimeMillis();
    }

    /// Converts a single CartItem to a Map.
    private Map<String, Object> cartItemToMap(CartItem item) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", item.getName());
        map.put("category", item.getCategory());
        map.put("price", item.getPrice()* item.getQuantity());
        map.put("quantity", item.getQuantity());
        /// NEW: Add totalPrice
        //map.put("totalPrice", item.getPrice() * item.getQuantity());
        return map;
    }

    /// Converts a list of CartItem objects to a list of Maps.
    private List<Map<String, Object>> cartItemsToMapList(List<CartItem> items) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (CartItem item : items) {
            list.add(cartItemToMap(item));
        }
        return list;
    }

    /// --------------- New Methods for Updating Booked Items Available Date ---------------

    /**
     * Updates all booked items by appending a new available date to the "availableDates" list.
     * It processes both accommodations and package items by looking up products using their name.
     *
     * @param orderItems  The map containing order details.
     * @param bookingDate The booking date provided by the user.
     */
    private void updateBookedItemsStatus(Map<String, Object> orderItems, String bookingDate) {
        // Process accommodations (e.g., Boat, Cottage)
        Object accommodationsObj = orderItems.get("accommodations");
        if (accommodationsObj instanceof List<?>) {
            List<?> accommodations = (List<?>) accommodationsObj;
            for (Object obj : accommodations) {
                if (obj instanceof Map<?, ?>) {
                    Map<?, ?> itemMap = (Map<?, ?>) obj;
                    String itemName = (String) itemMap.get("name");
                    appendAvailableDate(itemName, bookingDate);
                }
            }
        }
        // Process package item if available
        Object packageObj = orderItems.get("package");
        if (packageObj instanceof Map<?, ?>) {
            Map<?, ?> packageItem = (Map<?, ?>) packageObj;
            String itemName = (String) packageItem.get("name");
            appendAvailableDate(itemName, bookingDate);
        }
    }

    /**
     * Updates the booked item by setting its status to "Unavailable" and copying the
     * booking date directly into the "availableDate" field for the product identified by its name.
     *
     * @param itemName    the name of the product to update.
     * @param bookingDate the booking date provided by the user.
     */
    private void appendAvailableDate(String itemName, String bookingDate) {
        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products");
        productsRef.orderByChild("name").equalTo(itemName)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot child : snapshot.getChildren()) {
                            child.getRef().child("status").setValue("Unavailable");
                            /// Directly copy the bookingDate into the "availableDate" node
                            child.getRef().child("availableDate").setValue(bookingDate);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        /// Handle error if needed
                    }
                });
    }
}





///Fix Current
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.credentials.Credential;
//import android.graphics.Bitmap;
//import android.graphics.Canvas;
//import android.graphics.Typeface;
//import android.graphics.drawable.Drawable;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.se.omapi.Session;
//import android.util.Base64;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.android.volley.Response;
//import com.example.resort.addcart.data.CartItem;
//import com.example.resort.addcart.data.CartManager;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.squareup.picasso.Callback;
//import com.squareup.picasso.Request;
//import com.twilio.Twilio;
//import com.twilio.rest.api.v2010.account.Call;
//import com.twilio.rest.api.v2010.account.Message;
//import com.twilio.type.PhoneNumber;
//
//import java.io.BufferedReader;
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.io.OutputStream;
//import java.io.UnsupportedEncodingException;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Properties;
//import java.util.Scanner;
//
//import okhttp3.OkHttpClient;
//
//
//public class BookingReceipt extends AppCompatActivity {
//
//    private TextView detailBooking;
//    private TextView totalPaymentText;
//    private TextView downPaymentText;
//    private TextView nameTextView;
//    private TextView phoneTextView;
//    private TextView emailTextView;
//    private TextView messageTextView;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_booking_receipt);
//
//        // Adjust layout for system insets.
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Back arrow functionality.
//        ImageView back = findViewById(R.id.arrow);
//        back.setOnClickListener(v -> onBackPressed());
//
//        // Initialize receipt-related TextViews.
//        detailBooking = findViewById(R.id.DetailBooking);
//        totalPaymentText = findViewById(R.id.Totalpayment);
//        downPaymentText = findViewById(R.id.Downpayment);
//        TextView dateTextView = findViewById(R.id.date); // For booking date/time info
//
//        // Initialize user details TextViews.
//        nameTextView = findViewById(R.id.name);
//        phoneTextView = findViewById(R.id.phone);
//        emailTextView = findViewById(R.id.email);
//
//        // Initialize the thank you message TextView.
//        messageTextView = findViewById(R.id.message);
//
//        // Set a bold monospace typeface for the receipt details.
//        detailBooking.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
//
//        // Retrieve and display the formatted booking info passed from Booking activity.
//        String bookingDate = getIntent().getStringExtra("BOOKING_DATE");
//        if (bookingDate != null && !bookingDate.isEmpty()) {
//            dateTextView.setText(bookingDate);
//        } else {
//            dateTextView.setText("Date info not available");
//        }
//
//        /// Load user details from Firebase.
//        loadUserDetails();
//
//        /// Load the cart details (receipt).
//        loadCartDetails();
//
//        /// Set up the SaveReceipt (print) button.
//        setupSaveReceiptFunction();
//
//        /// Set up the submit button to show the custom confirmation dialog.
//        Button submitButton = findViewById(R.id.submit);
//        submitButton.setOnClickListener(v -> submitBookingToFirebase());
//    }
//
//    /// --------------------- Receipt & UI Helper Methods ---------------------
//    private void setupSaveReceiptFunction() {
//        ImageButton btnSaveReceipt = findViewById(R.id.SaveReceipt);
//        btnSaveReceipt.setOnClickListener(v -> {
//            // Inflate the custom layout
//            LayoutInflater inflater = LayoutInflater.from(BookingReceipt.this);
//            View dialogView = inflater.inflate(R.layout.custom_alert_dialog, null);
//
//            // Build the AlertDialog using the custom layout
//            AlertDialog.Builder builder = new AlertDialog.Builder(BookingReceipt.this)
//                    .setView(dialogView);
//            final AlertDialog dialog = builder.create();
//
//            // Get references to buttons in the custom layout
//            Button btnSave = dialogView.findViewById(R.id.btnSave);
//            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
//
//            // Save button functionality (using your existing logic)
//            btnSave.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    final Button submitButton = findViewById(R.id.submit);
//
//                    // Hide buttons temporarily.
//                    btnSaveReceipt.setVisibility(View.INVISIBLE);
//                    submitButton.setVisibility(View.INVISIBLE);
//
//                    btnSaveReceipt.postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            // --- Temporarily update UI for the saved receipt ---
//                            final ImageView arrowView = findViewById(R.id.arrow);
//                            final int originalArrowVisibility = arrowView.getVisibility();
//                            arrowView.setVisibility(View.INVISIBLE);
//
//                            final String originalMessage = messageTextView.getText().toString();
//                            final Drawable[] originalDrawables = messageTextView.getCompoundDrawables();
//                            messageTextView.setText("Thank you for purchasing Sir/Ma'am.");
//                            messageTextView.setCompoundDrawables(null, null, null, null);
//
//                            View contentView = findViewById(android.R.id.content);
//                            Bitmap screenshot = getScreenShot(contentView);
//
//                            // --- Restore UI elements ---
//                            messageTextView.setText(originalMessage);
//                            messageTextView.setCompoundDrawables(
//                                    originalDrawables[0],
//                                    originalDrawables[1],
//                                    originalDrawables[2],
//                                    originalDrawables[3]);
//                            arrowView.setVisibility(originalArrowVisibility);
//
//                            btnSaveReceipt.setVisibility(View.VISIBLE);
//                            btnSaveReceipt.setEnabled(true);
//                            submitButton.setVisibility(View.VISIBLE);
//                            submitButton.setEnabled(true);
//
//                            boolean isSaved = saveReceiptImage(screenshot);
//                            if (isSaved) {
//                                Toast.makeText(BookingReceipt.this, "Receipt saved to gallery", Toast.LENGTH_SHORT).show();
//                            } else {
//                                Toast.makeText(BookingReceipt.this, "Error saving receipt", Toast.LENGTH_SHORT).show();
//                            }
//                        }
//                    }, 200); // 200ms delay for UI update
//
//                    dialog.dismiss();
//                }
//            });
//
//            // Cancel button simply dismisses the dialog
//            btnCancel.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    dialog.dismiss();
//                }
//            });
//
//            // Show the custom dialog
//            dialog.show();
//        });
//    }
//
//
//    /// Helper method to capture a screenshot of a given view.
//    private Bitmap getScreenShot(View view) {
//        Bitmap bitmap = Bitmap.createBitmap(view.getWidth(), view.getHeight(), Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        view.draw(canvas);
//        return bitmap;
//    }
//
//    /// Helper method to save the screenshot image to the device's gallery.
//    private boolean saveReceiptImage(Bitmap bitmap) {
//        try {
//            String savedImageURL = MediaStore.Images.Media.insertImage(
//                    getContentResolver(),
//                    bitmap,
//                    "Receipt_" + System.currentTimeMillis(),
//                    "Receipt image"
//            );
//            return savedImageURL != null;
//        } catch (Exception e) {
//            //noinspection CallToPrintStackTrace
//            e.printStackTrace();
//            return false;
//        }
//    }
//
//    /// --------------------- Firebase Data Methods ---------------------
//
//    private void loadUserDetails() {
//        FirebaseAuth mAuth = FirebaseAuth.getInstance();
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//
//        if (currentUser != null) {
//            String userId = currentUser.getUid();
//            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
//            mDatabase.child("users").child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot snapshot) {
//                    if (snapshot.exists()) {
//                        String firstName = snapshot.child("firstName").getValue(String.class);
//                        String middleName = snapshot.child("middleInitial").getValue(String.class);
//                        String lastName = snapshot.child("lastName").getValue(String.class);
//                        String phone = snapshot.child("phoneNumber").getValue(String.class);
//                        String email = snapshot.child("email").getValue(String.class);
//
//                        String middleInitial = "";
//                        if (middleName != null && !middleName.isEmpty()) {
//                            middleInitial = middleName.substring(0, 1).toUpperCase();
//                        }
//
//                        String fullName;
//                        if ((lastName == null || lastName.isEmpty()) &&
//                                firstName != null && !firstName.isEmpty() &&
//                                !middleInitial.isEmpty()) {
//                            fullName = firstName + " the " + middleInitial + ".";
//                        } else {
//                            StringBuilder builder = new StringBuilder();
//                            if (firstName != null && !firstName.isEmpty()) { builder.append(firstName); }
//                            if (!middleInitial.isEmpty()) { builder.append(" ").append(middleInitial).append("."); }
//                            if (lastName != null && !lastName.isEmpty()) { builder.append(" ").append(lastName); }
//                            fullName = builder.toString().trim();
//                        }
//
//                        nameTextView.setText("Name: " + (fullName.isEmpty() ? "N/A" : fullName));
//                        phoneTextView.setText("Phone: " + (phone != null ? phone : "N/A"));
//                        emailTextView.setText("Email: " + (email != null ? email : "N/A"));
//                    }
//                }
//
//                @Override
//                public void onCancelled(DatabaseError error) {
//                    Log.e("BookingReceipt", "Failed to load user details: " + error.getMessage());
//                }
//            });
//        } else {
//            nameTextView.setText("Name: Guest");
//            phoneTextView.setText("Phone: N/A");
//            emailTextView.setText("Email: N/A");
//        }
//    }
//
//
//    @SuppressLint("SetTextI18n")
//    private void loadCartDetails() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            Toast.makeText(BookingReceipt.this, "User not logged in", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        String userId = currentUser.getUid();
//        List<CartItem> cartItems = CartManager.getInstance(BookingReceipt.this, userId).getCartItems();
//
//        Log.d("BookingReceipt", "Total Cart Items: " + cartItems.size());
//        for (CartItem item : cartItems) {
//            Log.d("BookingReceipt", "Item: " + item.getName() + ", Category: " + item.getCategory() + ", Quantity: " + item.getQuantity());
//        }
//
//        List<CartItem> accommodations = new ArrayList<>();
//        List<CartItem> foodAndDrinks = new ArrayList<>();
//        List<CartItem> packages = new ArrayList<>();
//
//        for (CartItem item : cartItems) {
//            String category = item.getCategory().toLowerCase();
//            switch (category) {
//                case "accommodation":
//                case "boat":
//                case "cottage":
//                    accommodations.add(item);
//                    break;
//                case "food":
//                case "dessert":
//                case "beverage":
//                case "alcohol":
//                    foodAndDrinks.add(item);
//                    break;
//                case "package":
//                    packages.add(item);
//                    break;
//                default:
//                    Log.w("BookingReceipt", "Uncategorized Item: " + item.getName());
//            }
//        }
//
//        StringBuilder receipt = new StringBuilder();
//        String formatLine = "%-26s %10s\n";
//
//        if (!accommodations.isEmpty()) {
//            receipt.append("Accommodation\n");
//            for (CartItem item : accommodations) {
//                String itemName = item.getName();
//                if (item.getQuantity() > 1) {
//                    itemName += " (X" + item.getQuantity() + ")";
//                }
//                String price = "₱" + String.format(Locale.US, "%.2f", item.getPrice() * item.getQuantity());
//                receipt.append(String.format(Locale.US, formatLine, itemName, price));
//            }
//            receipt.append("\n");
//        }
//
//        if (!foodAndDrinks.isEmpty()) {
//            receipt.append("Food/Dessert/Beverage/Alcohol\n");
//            for (CartItem item : foodAndDrinks) {
//                String itemName = item.getName();
//                if (item.getQuantity() > 1) {
//                    itemName += " (X" + item.getQuantity() + ")";
//                }
//                String price = "₱" + String.format(Locale.US, "%.2f", item.getPrice() * item.getQuantity());
//                receipt.append(String.format(Locale.US, formatLine, itemName, price));
//            }
//            receipt.append("\n");
//        }
//
//        if (!packages.isEmpty()) {
//            receipt.append("Package\n");
//            for (CartItem item : packages) {
//                String itemName = item.getName();
//                if (item.getQuantity() > 1) {
//                    itemName += " (X" + item.getQuantity() + ")";
//                }
//                String price = "₱" + String.format(Locale.US, "%.2f", item.getPrice() * item.getQuantity());
//                receipt.append(String.format(Locale.US, formatLine, itemName, price));
//            }
//            receipt.append("\n");
//        }
//
//        detailBooking.setText(receipt.toString());
//
//        double total = 0.0;
//        for (CartItem item : cartItems) {
//            total += item.getPrice() * item.getQuantity();
//        }
//        double downPayment = total * 0.50;
//
//        totalPaymentText.setText(String.format(Locale.US, "₱%.2f", total));
//        downPaymentText.setText(String.format(Locale.US, "₱%.2f", downPayment));
//    }
//
//    /// --------------------- Firebase Booking Submission Methods ---------------------
//
//    /// Shows the custom confirmation dialog using your layout (dialog_submission.xml)
//    private void submitBookingToFirebase() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(BookingReceipt.this);
//        View dialogView = getLayoutInflater().inflate(R.layout.dialog_submission, null);
//        builder.setView(dialogView);
//
//        AlertDialog alertDialog = builder.create();
//        Button submissionButton = dialogView.findViewById(R.id.Submission);
//        submissionButton.setOnClickListener(v -> {
//            sendBookingData();
//            alertDialog.dismiss();
//        });
//
//        alertDialog.show();
//    }
//
//
//    ///Send Booking Data
//    private void sendBookingData() {
//        // Retrieve user details from TextViews.
//        String name = nameTextView.getText().toString().replace("Name: ", "").trim();
//        String phone = phoneTextView.getText().toString().replace("Phone: ", "").trim();
//        String email = emailTextView.getText().toString().replace("Email: ", "").trim();
//
//        // Retrieve booking date.
//        TextView dateTextView = findViewById(R.id.date);
//        String bookingDate = dateTextView.getText().toString().trim();
//
//        // Set default statuses.
//        String statusReview = "Pending";
//        String paymentStatus = "Pending";
//
//        // Generate a unique booking reference.
//        String referenceNo = generateRandomReference();
//
//        // Calculate total amount from cart items.
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            Toast.makeText(BookingReceipt.this, "User not logged in", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        String userId = currentUser.getUid();
//        List<CartItem> cartItems = CartManager.getInstance(BookingReceipt.this, userId).getCartItems();
//        double totalAmount = cartItems.stream().mapToDouble(item -> item.getPrice() * item.getQuantity()).sum();
//
//
//        // Categorize cart items.
//        List<CartItem> accommodations = new ArrayList<>();
//        List<CartItem> foodAndDrinks = new ArrayList<>();
//        List<CartItem> packages = new ArrayList<>();
//        for (CartItem item : cartItems) {
//            String category = item.getCategory().toLowerCase();
//            switch (category) {
//                case "accommodation":
//                case "boat":
//                case "cottage":
//                    accommodations.add(item);
//                    break;
//                case "food":
//                case "dessert":
//                case "beverage":
//                case "alcohol":
//                    foodAndDrinks.add(item);
//                    break;
//                case "package":
//                    packages.add(item);
//                    break;
//                default:
//                    Log.w("BookingReceipt", "Uncategorized Item: " + item.getName());
//            }
//        }
//        ///Date
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
//        String formattedDate = sdf.format(new Date());
//
//        /// Build bookingReview map.
//        Map<String, Object> bookingReview = new HashMap<>();
//        bookingReview.put("statusReview", statusReview);
//        bookingReview.put("name", name);
//        bookingReview.put("bookingDate", bookingDate);
//        bookingReview.put("refNo", referenceNo);
//        bookingReview.put("email", email);
//        bookingReview.put("phone", phone);
//        ///New Data Add
//        bookingReview.put("downPayment", totalAmount * 0.50);
//        bookingReview.put("amount", totalAmount);
//
//        Map<String, Object> orderItems = new HashMap<>();
//        orderItems.put("accommodations", cartItemsToMapList(accommodations));
//        orderItems.put("foodAndDrinks", cartItemsToMapList(foodAndDrinks));
//        if (!packages.isEmpty()) {
//            orderItems.put("package", cartItemToMap(packages.get(0)));
//        }
//        bookingReview.put("orderItems", orderItems);
//
//        /// Build paymentTransaction map.
//        Map<String, Object> paymentTransaction = new HashMap<>();
//        paymentTransaction.put("paymentStatus", paymentStatus);
//        paymentTransaction.put("finalStatus", paymentStatus);
//        paymentTransaction.put("name", name);
//        paymentTransaction.put("refNo", referenceNo);
//        paymentTransaction.put("amount", totalAmount);
//        paymentTransaction.put("PaymentDate", formattedDate);
//        paymentTransaction.put("downPayment", totalAmount * 0.50);
//
//        /// Build paymentMethod map with default values ("N/A").
//        Map<String, Object> paymentMethod = new HashMap<>();
//        paymentMethod.put("Payment", "N/A");
//        paymentMethod.put("Reference", "N/A");
//        paymentMethod.put("Firstname", "N/A");
//        paymentMethod.put("Lastname", "N/A");
//        paymentMethod.put("Phone", "N/A");
//        paymentMethod.put("Amount", "N/A");
//        paymentMethod.put("Status", "N/A");
//
//        /// Combine the booking data.
//        Map<String, Object> bookingData = new HashMap<>();
//        bookingData.put("bookingReview", bookingReview);
//        bookingData.put("paymentTransaction", paymentTransaction);
//        bookingData.put("paymentMethod", paymentMethod);
//
//
//        SharedPreferences preferences = getSharedPreferences("BookingPref_" + userId, MODE_PRIVATE);
//        /// Prevent a new booking if any booking state is active.
//        if (preferences.getBoolean("bookingSubmitted", false) ||
//                preferences.getBoolean("paymentSubmitted", false) ||
//                preferences.getBoolean("paymentApproved", false) ||
//                preferences.getBoolean("reviewApproved", false) ||
//                preferences.getBoolean("finalApproved", false)) {
//            Toast.makeText(BookingReceipt.this, "Sorry, your booking is still in progress.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//
//        /// Update userId from the current user.
//        userId = currentUser.getUid();
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyBooking");
//
//        /// Check if any booking data exists in MyBooking.
//        String finalUserId = userId;
//        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    // If any booking exists, abort the submission.
//                    Toast.makeText(BookingReceipt.this,
//                            "Sorry, you already have an active booking.", Toast.LENGTH_SHORT).show();
//                } else {
//                    /// No pending booking exists; proceed with submission.
//                    String bookingId = bookingRef.push().getKey();
//                    if (bookingId != null) {
//                        bookingRef.child(bookingId).setValue(bookingData)
//                                .addOnCompleteListener(task -> {
//                                    if (task.isSuccessful()) {
//                                        Toast.makeText(BookingReceipt.this, "Booking submitted successfully", Toast.LENGTH_SHORT).show();
//
//
//                                        /// ******************* New Code Start *******************
//                                        /// Create a new node at the top level called "bookingRequest"
//
////                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
////                                        String formattedDate = sdf.format(new Date());
////
////                                        DatabaseReference bookingRequestRef = FirebaseDatabase.getInstance()
////                                                .getReference("bookingRequest");
////
////                                        /// Build a new map for the booking request with a custom message and the date.
////                                        Map<String, Object> bookingRequest = new HashMap<>();
////                                        bookingRequest.put("message", "Booking request by " + name);
////                                        bookingRequest.put("date", formattedDate);
////
////                                        // Push the booking request to a new node.
////                                        bookingRequestRef.push().setValue(bookingRequest);
//
//
//                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
//                                        String formattedDate = sdf.format(new Date());
//
//                                        DatabaseReference bookingRequestRef = FirebaseDatabase.getInstance()
//                                                .getReference("bookingRequest");
//
//                                        Map<String, Object> bookingRequest = new HashMap<>();
//                                        bookingRequest.put("message", "Booking request by " + name);
//                                        bookingRequest.put("date", formattedDate);
//
//                                        DatabaseReference newReqRef = bookingRequestRef.push();
//                                        newReqRef.setValue(bookingRequest)
//                                                .addOnSuccessListener(aVoid -> {
//                                                    /// Simplified Telegram message without technical IDs
//                                                    String telegramMsg = "🔔 New Booking Request 🔔\n"
//                                                            + "👤 Name: " + name + "\n"
//                                                            + "📅 Date: " + formattedDate + "\n"
//                                                            + "⏳ Status: " + "Pending";
//
//                                                    sendTelegramNotification(telegramMsg);
//                                                })
//                                                .addOnFailureListener(e -> {
//                                                    Log.e("Firebase", "BookingRequest failed", e);
//                                                });
//
//                                        /// ******************* New Code End *******************
//
//                                        /// Clean up previous notifications.
//                                        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
//                                                .getReference("users")
//                                                .child(finalUserId)
//                                                .child("notifications");
//                                        notificationRef.removeValue();
//
//                                        /// Copy orderItems to MyReview node with a default statusReview of "Pending".
//                                        Object orderItemsObj = bookingReview.get("orderItems");
//                                        if (orderItemsObj instanceof Map) {
//                                            /// Cast orderItemsObj to a Map
//                                            @SuppressWarnings("unchecked")
//                                            Map<String, Object> orderItemsMap = (Map<String, Object>) orderItemsObj;
//
//                                            /// Add default statusReview
//                                            orderItemsMap.put("statusReview", "Pending");
//
//                                            /// Get a reference to the MyReview node
//                                            DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                                                    .getReference("users")
//                                                    .child(finalUserId)
//                                                    .child("MyReview");
//
//                                            /// Generate a unique key for the new review
//                                            String reviewId = myReviewRef.push().getKey();
//                                            if (reviewId != null) {
//                                                /// Set the value of the new review to orderItemsMap
//                                                myReviewRef.child(reviewId).setValue(orderItemsMap);
//                                            }
//                                        }
//
//                                        /// Append available date to booked items.
//                                        if (orderItemsObj instanceof Map) {
//                                            ///noinspection unchecked
//                                            updateBookedItemsStatus((Map<String, Object>) orderItemsObj, bookingDate);
//                                        }
//
//                                        /// Permanently delete cart items.
//                                        CartManager.getInstance(BookingReceipt.this, finalUserId).clearCartItems();
//
//                                        /// Save booking state persistently.
//                                        SharedPreferences preferences = getSharedPreferences("BookingPref_" + finalUserId,  MODE_PRIVATE);
//                                        SharedPreferences.Editor editor = preferences.edit();
//                                        editor.putBoolean("bookingSubmitted", true);
//                                        editor.apply();
//
//
//                                        /// ******************* Start Foreground Service *******************
//                                        /// This will start your BookingStatusService as a foreground service.
//                                        Intent serviceIntent = new Intent(BookingReceipt.this, BookingStatusService.class);
//                                        androidx.core.content.ContextCompat.startForegroundService(BookingReceipt.this, serviceIntent);
//                                        Log.d("BookingReceipt", "Foreground service started");
//                                        /// ******************* End Foreground Service *******************
//
//
//                                        // Navigate to BookingStatus, which reads the saved state.
//                                        Intent intent = new Intent(BookingReceipt.this, BookingStatus.class);
//                                        intent.putExtra("bookingSubmitted", true);
//                                        ///intent.putExtra("bookingId", bookingId);
//
//                                        startActivity(intent);
//                                        overridePendingTransition(0, 0);
//                                        finish();
//                                    } else {
//                                        Toast.makeText(BookingReceipt.this, "Error submitting booking", Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                    } else {
//                        Toast.makeText(BookingReceipt.this, "Error generating booking ID", Toast.LENGTH_SHORT).show();
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(BookingReceipt.this, "Error checking booking status", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void sendTelegramNotification(String message) {
//        new Thread(() -> {
//            try {
//                String botToken = "7263113934:AAHIz9CRO-7zgvkK_75b9BCFcaN3lrRXGqo";
//                String chatId = "7259957866";
//
//                String urlString = "https://api.telegram.org/bot" + botToken
//                        + "/sendMessage?chat_id=" + chatId
//                        + "&text=" + URLEncoder.encode(message, "UTF-8");
//
//                URL url = new URL(urlString);
//                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//                conn.setRequestMethod("GET");
//
//                /// Log the full URL (for debugging)
//                Log.d("Telegram", "Request URL: " + urlString);
//
//                int responseCode = conn.getResponseCode();
//                if (responseCode == 200) {
//                    Log.i("Telegram", "Message sent successfully");
//                } else {
//                    /// Read error response
//                    InputStream errorStream = conn.getErrorStream();
//                    if (errorStream != null) {
//                        String error = new Scanner(errorStream).useDelimiter("\\A").next();
//                        Log.e("Telegram", "API Error: " + error);
//                    }
//                }
//                conn.disconnect();
//            } catch (Exception e) {
//                Log.e("Telegram", "Error: ", e);
//            }
//        }).start();
//    }
//
//    /// --------------------- Helper Methods ---------------------
//
//    /// Generates a unique reference number.
//    private String generateRandomReference() {
//        return "REF" + System.currentTimeMillis();
//    }
//
//    /// Converts a single CartItem to a Map.
//    private Map<String, Object> cartItemToMap(CartItem item) {
//        Map<String, Object> map = new HashMap<>();
//        map.put("name", item.getName());
//        map.put("category", item.getCategory());
//        map.put("price", item.getPrice()* item.getQuantity());
//        map.put("quantity", item.getQuantity());
//        /// NEW: Add totalPrice
//        //map.put("totalPrice", item.getPrice() * item.getQuantity());
//        return map;
//    }
//
//    /// Converts a list of CartItem objects to a list of Maps.
//    private List<Map<String, Object>> cartItemsToMapList(List<CartItem> items) {
//        List<Map<String, Object>> list = new ArrayList<>();
//        for (CartItem item : items) {
//            list.add(cartItemToMap(item));
//        }
//        return list;
//    }
//
//    /// --------------- New Methods for Updating Booked Items Available Date ---------------
//
//    /**
//     * Updates all booked items by appending a new available date to the "availableDates" list.
//     * It processes both accommodations and package items by looking up products using their name.
//     *
//     * @param orderItems  The map containing order details.
//     * @param bookingDate The booking date provided by the user.
//     */
//    private void updateBookedItemsStatus(Map<String, Object> orderItems, String bookingDate) {
//        // Process accommodations (e.g., Boat, Cottage)
//        Object accommodationsObj = orderItems.get("accommodations");
//        if (accommodationsObj instanceof List<?>) {
//            List<?> accommodations = (List<?>) accommodationsObj;
//            for (Object obj : accommodations) {
//                if (obj instanceof Map<?, ?>) {
//                    Map<?, ?> itemMap = (Map<?, ?>) obj;
//                    String itemName = (String) itemMap.get("name");
//                    appendAvailableDate(itemName, bookingDate);
//                }
//            }
//        }
//        // Process package item if available
//        Object packageObj = orderItems.get("package");
//        if (packageObj instanceof Map<?, ?>) {
//            Map<?, ?> packageItem = (Map<?, ?>) packageObj;
//            String itemName = (String) packageItem.get("name");
//            appendAvailableDate(itemName, bookingDate);
//        }
//    }
//
//    /**
//     * Updates the booked item by setting its status to "Unavailable" and copying the
//     * booking date directly into the "availableDate" field for the product identified by its name.
//     *
//     * @param itemName    the name of the product to update.
//     * @param bookingDate the booking date provided by the user.
//     */
//    private void appendAvailableDate(String itemName, String bookingDate) {
//        DatabaseReference productsRef = FirebaseDatabase.getInstance().getReference("products");
//        productsRef.orderByChild("name").equalTo(itemName)
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        for (DataSnapshot child : snapshot.getChildren()) {
//                            child.getRef().child("status").setValue("Unavailable");
//                            /// Directly copy the bookingDate into the "availableDate" node
//                            child.getRef().child("availableDate").setValue(bookingDate);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        /// Handle error if needed
//                    }
//                });
//    }
//}
//
//
