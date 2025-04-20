package com.example.resort;

import static android.app.Service.START_STICKY;
import static android.content.ContentValues.TAG;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;


import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;


import android.Manifest;

public class BookingStatus extends AppCompatActivity {

    /// Progress tracker: 0 = no progress, 1 = booking submitted, 2 = reviewed, 3 = payment submitted,
    /// 4 = payment transaction approved, 5 = final approval.
    private int progress = 0;
    /// Passed from BookingReceipt.
    //private String bookingId;
    /// Prevents double data store in Firebase
    private boolean bookingMoved = false;

    ///private boolean bookingCancelDecline = false;
    private TextView dot1, dot2, dot3, dot4, dot5;
    private View line1_2, line2_3, line3_4, line4_5;
    private Button payNowButton, cancelButton;
    /// Message containers for dots 1 to 5.
    private FrameLayout messageFramedot1, messageFramedot2, messageFramedot3, messageFramedot4, messageFramedot5;
    private TextView messageText, messageText2, paymentMessageText, messageText4, messageText5;
    /// SharedPreferences for persisting booking state.
    private SharedPreferences prefs;
    /// Flags for one-time processing.
    private boolean approvalProcessed = false;
    private boolean paymentApprovedProcessed = false;
    private boolean finalProcessed = false;

    /// For decline handling.
    private boolean declineProcessed = false;
    private boolean paymentDeclineProcessed = false;
    private boolean bookingSubmittedProcessed = false;

    ///private static final int PAYMENT_REQUEST_CODE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking_status);


        // Get current user and set up user-specific SharedPreferences.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            finish();
            return;
        }
        String userId = currentUser.getUid();
        prefs = getSharedPreferences("BookingPref_" + userId, MODE_PRIVATE);

        // If booking is submitted from previous activity, save state in SharedPreferences.
        boolean bookingSubmittedIntent = getIntent().getBooleanExtra("bookingSubmitted", false);
        if (bookingSubmittedIntent) {
            prefs.edit().putBoolean("bookingSubmitted", true).apply();
            progress = 1;
            prefs.edit().putInt("bookingProgress", progress).apply();
        }

        boolean bookingPayIntent = getIntent().getBooleanExtra("paymentSubmitted", false);
        if (bookingPayIntent) {
            prefs.edit().putBoolean("paymentSubmitted", true).apply();
            progress = 3;
            prefs.edit().putInt("bookingProgress", progress).apply();
        }



        /// Adjust layout for system insets.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });


        /// Initialize UI elements.
        dot1 = findViewById(R.id.dot1);
        dot2 = findViewById(R.id.dot2);
        dot3 = findViewById(R.id.dot3);
        dot4 = findViewById(R.id.dot4);
        dot5 = findViewById(R.id.dot5);

        line1_2 = findViewById(R.id.line1);
        line2_3 = findViewById(R.id.line2);
        line3_4 = findViewById(R.id.line3);
        line4_5 = findViewById(R.id.line4);

        payNowButton = findViewById(R.id.button);
        cancelButton = findViewById(R.id.button2);
        Button backButton = findViewById(R.id.back2);

        messageFramedot1 = findViewById(R.id.messageFramedot1);
        messageText = findViewById(R.id.messageText);
        messageFramedot2 = findViewById(R.id.messageFramedot2);
        messageText2 = findViewById(R.id.messageText2);
        messageFramedot3 = findViewById(R.id.messageFramedot3);
        paymentMessageText = findViewById(R.id.messageText3);
        messageFramedot4 = findViewById(R.id.messageFramedot4);
        messageText4 = findViewById(R.id.messageText4);
        messageFramedot5 = findViewById(R.id.messageFramedot5);
        messageText5 = findViewById(R.id.messageText5);

        // Initially hide message texts.
        messageText.setVisibility(View.GONE);
        messageText2.setVisibility(View.GONE);
        paymentMessageText.setVisibility(View.GONE);
        messageText4.setVisibility(View.GONE);
        messageText5.setVisibility(View.GONE);

        updateDots();

        /// Restore persisted booking state.
        if (prefs.contains("bookingSubmitted") && prefs.getBoolean("bookingSubmitted", false)) {
            progress = 1;
            updateDots();
            showSubmissionMessage();
        }
        if (prefs.contains("paymentSubmitted") && prefs.getBoolean("paymentSubmitted", false)) {
            progress = Math.max(progress, 3);
            updateDots();
            if (paymentMessageText.getVisibility() != View.VISIBLE) {
                showPaymentSubmittedMessage();
            }
        }

        if (prefs.contains("paymentApproved") && prefs.getBoolean("paymentApproved", false)) {
            progress = 4;
            updateDots();
            if (messageText4.getVisibility() != View.VISIBLE) {
                showDot4Message();
            }
        }

        if (prefs.contains("reviewApproved") && prefs.getBoolean("reviewApproved", false)) {
            progress = 2;
            updateDots();
            if (messageText2.getVisibility() != View.VISIBLE) {
                showApprovalMessage();
            }
        }

        if (prefs.contains("finalApproved") && prefs.getBoolean("finalApproved", false)) {
            progress = 5;
            updateDots();
            if (messageText5.getVisibility() != View.VISIBLE) {
                showDot5Message();
            }
        }


        ///Pay now Button
        payNowButton.setOnClickListener(v -> {
            if (progress < 2) {
                Toast.makeText(BookingStatus.this, "No booking or step not done, cannot proceed.", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent paymentIntent = new Intent(BookingStatus.this, Payment.class);
            startActivity(paymentIntent);
        });

        backButton.setOnClickListener(v -> onBackPressed());
        cancelButton.setOnClickListener(view -> cancelBooking());

        /// Set up Firebase listeners for dynamic updates.
        listenForPaymentMethodStatus();
        listenForMyBooking();
        listenForApproval();
        listenForPaymentTransactionApproval();
        FinalForApproval();
    }


    /// Update dots and lines based on current progress.
    private void updateDots() {
        dot1.setBackgroundResource(progress >= 1 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
        dot2.setBackgroundResource(progress >= 2 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
        dot3.setBackgroundResource(progress >= 3 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
        dot4.setBackgroundResource(progress >= 4 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
        dot5.setBackgroundResource(progress >= 5 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);

        line1_2.setBackgroundResource(progress >= 1 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
        line2_3.setBackgroundResource(progress >= 2 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
        line3_4.setBackgroundResource(progress >= 3 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
        line4_5.setBackgroundResource(progress >= 4 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);

        if (progress == 5) {
            payNowButton.setEnabled(false);
        }
        ///Save updated progress.
        prefs.edit().putInt("bookingProgress", progress).apply();
    }



    /**
     * Cancels a booking by:
     * - Appending a cancellation timestamp to each booking's review.
     * - Moving data from "MyBooking" to "MyHistory".
     * - Updating the review status in "MyReview" to "Cancelled" (without removing it).
     * - Updating the UI with a cancellation message.
     * - Disabling the cancel button.
     * - Creating a new top-level "cancelBooking" node with cancellation details.
     */

    private void cancelBooking() {
        if (progress < 1) {
            Toast.makeText(BookingStatus.this, "No booking in the field, cannot cancel.", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        final String userId = currentUser.getUid();

        // References for booking data.
        final DatabaseReference myBookingRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("MyBooking");
        final DatabaseReference myHistoryRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("MyHistory");

        myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Update each booking's review status to "Cancelled".
                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                    // Update the booking review status.
                    bookingSnapshot.getRef()
                            .child("bookingReview")
                            .child("statusReview")
                            .setValue("Cancelled");

                    // If a paymentTransaction node exists, update its status and set PaymentDate.
                    if (bookingSnapshot.hasChild("paymentTransaction")) {
                        bookingSnapshot.getRef()
                                .child("paymentTransaction")
                                .child("paymentStatus")
                                .setValue("Cancelled");

                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                        String paymentDate = sdf.format(new Date());
                        bookingSnapshot.getRef()
                                .child("paymentTransaction")
                                .child("PaymentDate")
                                .setValue(paymentDate);
                    }
                }

                // Move booking data to MyHistory.
                myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot bookingSnapshot) {
                        for (DataSnapshot child : bookingSnapshot.getChildren()) {
                            Object bookingData = child.getValue();
                            myHistoryRef.push().setValue(bookingData);
                        }
                        // Remove all bookings from MyBooking.
                        myBookingRef.removeValue();

                        DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(userId)
                                .child("MyReview");

                        myReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot reviewSnapshot) {
                                boolean hasPending = false;

                                for (DataSnapshot review : reviewSnapshot.getChildren()) {
                                    String status = review.child("statusReview").getValue(String.class);
                                    if ("Pending".equalsIgnoreCase(status)) {
                                        hasPending = true;
                                        break;
                                    }
                                }

                                if (hasPending) {
                                    // Delete the whole MyReview node
                                    myReviewRef.removeValue().addOnSuccessListener(aVoid ->
                                            Log.d("BookingStatus", "MyReview deleted because it had a Pending status.")
                                    );
                                } else {
                                    // Otherwise, mark all as Cancelled
                                    for (DataSnapshot review : reviewSnapshot.getChildren()) {
                                        review.getRef().child("statusReview").setValue("Cancelled");
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                Log.e("BookingStatus", "Review fetch error: " + error.getMessage());
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error if needed.
                    }
                });

                // Display cancellation message.
                String cancelTime = getCurrentTime();
                String cancellationMessage = "&quot;Booking has been Cancelled.&quot;<br>";
                String redTime = String.format("<font color='#FF0000'>%s</font>", cancelTime);
                messageFramedot1.setVisibility(View.VISIBLE);
                messageText.setVisibility(View.VISIBLE);
                messageText.setText(Html.fromHtml(cancellationMessage + redTime));
                sendNotificationToFirebase(messageText.getText().toString(), "cancel");

                // Fetch user details to build the cancellation string.
                DatabaseReference userRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(userId);
                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                        String firstName = userSnapshot.child("firstName").getValue(String.class);
                        String lastName = userSnapshot.child("lastName").getValue(String.class);

                        String bookingCancelledBy = "Booking cancelled by " + firstName + " " + lastName;

                        // Create a new top-level cancellation record.
                        DatabaseReference cancelBookingRef = FirebaseDatabase.getInstance().getReference("cancelBooking");
                        Map<String, Object> cancelData = new HashMap<>();
                        cancelData.put("message", bookingCancelledBy);
                        cancelData.put("date", cancelTime);
                        ///cancelBookingRef.push().setValue(cancelData);


                        DatabaseReference newReqRef = cancelBookingRef.push();
                        newReqRef.setValue(cancelData)
                                .addOnSuccessListener(aVoid -> {
                                    /// Simplified Telegram message without technical IDs
                                    String telegramMsg = "🔔 New Cancel Request 🔔\n"
                                            + "👤 Name: " + firstName + " " + lastName + "\n"
                                            + "📅 Date: " + cancelTime + "\n"
                                            + "❌ Status: " + "Cancel";

                                    sendTelegramNotification(telegramMsg);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e("Firebase", "BookingRequest failed", e);
                                });
                    }


                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("BookingStatus", "User data fetch failed: " + error.getMessage());
                    }
                });

                // Reset state after a delay.
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    /// Stop the foreground service
                    Intent stopIntent = new Intent(BookingStatus.this, BookingStatusService.class);
                    stopService(stopIntent);
                    Log.d("BookingStatus", "Foreground service stopped after cancellation.");

                    progress = 0;
                    updateDots();
                    clearBookingMessageUI();
                    clearBookingPreferences();
                }, 1000);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e("BookingStatus", "Cancellation failed: " + error.getMessage());
            }
        });
    }

    ///Telegram Api don't touch this
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

    /// Message view if the booking is submit Not use
    private void showSubmissionMessage() {
        messageFramedot1.setVisibility(View.VISIBLE);
        messageText.setVisibility(View.VISIBLE);
        String currentTime = getCurrentTime();
        String submissionMessage = "&quot;Booking has been Submitted. Please wait for it to be reviewed by admin.&quot;<br>";
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime );
        messageText.setText(Html.fromHtml(submissionMessage + redTime));
        sendNotificationToFirebase(messageText.getText().toString(), "dot1");
        clearNotification(2);
        clearNotification(3);
        clearNotification(4);
        clearNotification(5);
        clearNotification(6);
        clearNotification(7);
    }

    ///  Message view if the booking is payment Not use
    private void showPaymentSubmittedMessage() {
        messageFramedot3.setVisibility(View.VISIBLE);
        paymentMessageText.setVisibility(View.VISIBLE);
        String currentTime = getCurrentTime();
        String paymentMessage = "&quot;Payment has been Submitted. Please wait for admin review.&quot;<br>";
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
        paymentMessageText.setText(Html.fromHtml(paymentMessage + redTime));
        // Disable payment and cancel buttons
        payNowButton.setEnabled(false);
        payNowButton.setClickable(false);
        payNowButton.setAlpha(0.5f);
        //Cancel
        cancelButton.setEnabled(false);
        cancelButton.setClickable(false);
        cancelButton.setAlpha(0.5f);
        sendNotificationToFirebase(paymentMessageText.getText().toString(), "dot3");

    }

    private void showApprovalMessage() {
        messageFramedot2.setVisibility(View.VISIBLE);
        messageText2.setVisibility(View.VISIBLE);
        String currentTime = getCurrentTime();
        String approvalMessage = "&quot;Booking has been Reviewed. Please proceed to the payment by clicking the Pay Now button.&quot;<br>";
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
        String fullMessage = approvalMessage + redTime;
        messageText2.setText(Html.fromHtml(fullMessage));
        prefs.edit().putString("bookingStatus", "reviewApproved").apply();
        sendNotificationToFirebase(messageText2.getText().toString(), "dot2");

        // Disable the cancel button.
        cancelButton.setEnabled(false);
        cancelButton.setClickable(false);
        cancelButton.setAlpha(0.5f);

    }


    private void showDot4Message() {
        messageFramedot4.setVisibility(View.VISIBLE);
        messageText4.setVisibility(View.VISIBLE);
        String currentTime = getCurrentTime();
        String msg = "&quot;Payment transaction has been Approved. Please wait for final approval.&quot;<br>";
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
        String fullMessage = msg + redTime;
        messageText4.setText(Html.fromHtml(fullMessage));
        prefs.edit().putString("bookingStatus", "paymentApproved").apply();
        sendNotificationToFirebase(messageText4.getText().toString(), "dot4");
    }

    private void showDot5Message() {
        messageFramedot5.setVisibility(View.VISIBLE);
        messageText5.setVisibility(View.VISIBLE);
        String msg = "&quot;Congratulations! Your Booking has been Approved.&quot;<br>";
        String currentTime = getCurrentTime();
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
        String fullMessage = msg + redTime;
        messageText5.setText(Html.fromHtml(fullMessage));
        prefs.edit().putString("bookingStatus", "finalApproved").apply();
        sendNotificationToFirebase(messageText5.getText().toString(), "dot5");
    }


    /// Booking submitted
    private void listenForMyBooking() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();
        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("MyBooking");

        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable[] pollTask = new Runnable[1];
        pollTask[0] = new Runnable() {
            @Override
            public void run() {
                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // If booking exists and hasn't been processed yet, process it.
                        if (snapshot.exists() && !bookingSubmittedProcessed) {
                            bookingSubmittedProcessed = true;
                            progress = 1;
                            updateDots();
                            showSubmissionMessage();
                        }
                        // Only re-poll if the booking hasn't been processed.
                        if (!bookingSubmittedProcessed) {
                            handler.postDelayed(pollTask[0], 1000);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("BookingCheck", "Error reading MyBooking data", error.toException());
                    }
                });
            }
        };

        handler.post(pollTask[0]);
    }


    /// Payment Submitted
    private boolean bookingPayProcessed = false;
    private DatabaseReference bookingRef;

    private void listenForPaymentMethodStatus() {
        // Get the current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;  // Exit if no user is logged in
        String userId = currentUser.getUid();

        // Ensure that bookingRef is only initialized once
        if (bookingRef == null) {
            bookingRef = FirebaseDatabase.getInstance()
                    .getReference("users").child(userId).child("MyBooking");

            final Handler handler = new Handler(Looper.getMainLooper());
            final Runnable[] pollTask = new Runnable[1];
            pollTask[0] = new Runnable() {
                @Override
                public void run() {
                    // Use a listener to check the payment status
                    bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            // Check if MyBooking exists for the current user
                            if (snapshot.exists() && !bookingPayProcessed) {
                                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                                    // Access the payment status
                                    String paymentStatus = bookingSnapshot.child("paymentMethod")
                                            .child("Status")
                                            .getValue(String.class);

                                    // If paymentStatus is "Done", update progress to 3 and show the message
                                    // Inside the loop that checks each bookingSnapshot
                                    if (paymentStatus != null && paymentStatus.equalsIgnoreCase("Done")) {
                                        progress = Math.max(progress, 3);
                                        bookingPayProcessed = false;
                                        updateDots();
                                        showPaymentSubmittedMessage();
                                        break;
                                    }
                                }
                            } else {
                                Log.d("BookingCheck", "MyBooking does not exist for the user.");
                            }

                            // Only re-poll if the payment hasn't been processed yet.
                            if (!bookingPayProcessed) {
                                handler.postDelayed(pollTask[0], 1000);  // Poll every second
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("BookingCheck", "Error reading MyBooking data", error.toException());
                        }
                    });
                }
            };

            handler.post(pollTask[0]);  // Start polling
        }
    }



    ///Booking Review Admin
    private void listenForApproval() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("MyBooking");

        // Create a Handler to schedule polling every second
        final Handler handler = new Handler(Looper.getMainLooper());
        final Runnable[] pollTask = new Runnable[1];
        pollTask[0] = new Runnable() {
            @Override
            public void run() {
                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                            String statusReview = bookingSnapshot.child("bookingReview")
                                    .child("statusReview")
                                    .getValue(String.class);
                            if (statusReview != null) {
                                if (statusReview.equalsIgnoreCase("Approved") && !approvalProcessed) {
                                    approvalProcessed = true;
                                    progress = 2;
                                    updateDots();
                                    showApprovalMessage();
                                } else if (statusReview.equalsIgnoreCase("Declined") && !declineProcessed) {
                                    declineProcessed = true;
                                    DataSnapshot paymentSnap = bookingSnapshot.child("paymentTransaction");
                                    if (paymentSnap.exists()) {
                                        String currentPaymentStatus = paymentSnap.child("paymentStatus")
                                                .getValue(String.class);
                                        if (currentPaymentStatus == null ||
                                                !currentPaymentStatus.equalsIgnoreCase("Declined")) {
                                            bookingSnapshot.child("paymentTransaction")
                                                    .getRef()
                                                    .child("paymentStatus")
                                                    .setValue("Declined");

                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                                            String paymentDate = sdf.format(new Date());
                                            bookingSnapshot.child("paymentTransaction")
                                                    .getRef()
                                                    .child("PaymentDate")
                                                    .setValue(paymentDate);

                                            messageFramedot2.setVisibility(View.VISIBLE);
                                            messageText2.setVisibility(View.VISIBLE);
                                            String currentTime = getCurrentTime();
                                            String msg = "\"Sorry, your booking has been declined by the admin.\"<br>";
                                            String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
                                            messageText2.setText(Html.fromHtml(msg + redTime));
                                            sendNotificationToFirebase(messageText2.getText().toString(), "BookingDecline");
                                            moveAllBookingsToHistory();
                                            clearBookingMessageUI();
                                            clearBookingPreferences();

                                            DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
                                                    .getReference("users")
                                                    .child(userId)
                                                    .child("MyReview");
                                            myReviewRef.removeValue().addOnCompleteListener(task -> {
                                                if (task.isSuccessful()) {
                                                    Log.d("DeleteReview", "MyReview node deleted successfully.");
                                                } else {
                                                    Log.e("DeleteReview", "Failed to delete MyReview node.", task.getException());
                                                }
                                            });
                                        }
                                    }
                                }
                            }
                        }
                        // Continue polling every second
                        handler.postDelayed(pollTask[0], 1000);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("BookingCheck", "Error reading MyBooking data", error.toException());
                        // Continue polling even on error
                        handler.postDelayed(pollTask[0], 1000);
                    }
                });
            }
        };

        // Start polling every second
        handler.post(pollTask[0]);
    }


    ///Payment Admin Approval
    private void listenForPaymentTransactionApproval() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("MyBooking");

        final Handler handler = new Handler(Looper.getMainLooper());
        Runnable pollTask = new Runnable() {
            @Override
            public void run() {
                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                            if (bookingSnapshot.child("paymentTransaction").exists()) {
                                String paymentStatus = bookingSnapshot.child("paymentTransaction")
                                        .child("paymentStatus")
                                        .getValue(String.class);
                                // When paymentStatus is Approved, update progress to 4
                                if (paymentStatus != null &&
                                        paymentStatus.equalsIgnoreCase("Approved") &&
                                        !paymentApprovedProcessed) {
                                    paymentApprovedProcessed = true;
                                    progress = Math.max(progress, 4);  // Set progress directly to 4
                                    updateDots();  // Ensure this method updates your progress UI elements
                                    showDot4Message();
                                }
                                // Handle Refund branch if needed.
                                else if (paymentStatus != null &&
                                        paymentStatus.equalsIgnoreCase("Refund") &&
                                        !paymentDeclineProcessed) {
                                    paymentDeclineProcessed = true;
                                    // Process refund logic as you already have.
                                    DataSnapshot paymentSnap = bookingSnapshot.child("bookingReview");
                                    if (paymentSnap.exists()) {
                                        String currentPaymentStatus = paymentSnap.child("statusReview")
                                                .getValue(String.class);
                                        if (currentPaymentStatus == null ||
                                                !currentPaymentStatus.equalsIgnoreCase("Refund")) {
                                            bookingSnapshot.child("bookingReview")
                                                    .getRef()
                                                    .child("statusReview")
                                                    .setValue("Refund");

                                            // Update UI for refund branch.
                                            messageFramedot2.setVisibility(View.VISIBLE);
                                            messageText2.setVisibility(View.VISIBLE);
                                            String currentTime = getCurrentTime();
                                            String msg = "&quot;Your payment has been reversed and refunded by the admin.&quot;<br>";
                                            String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
                                            messageText2.setText(Html.fromHtml(msg + redTime));
                                            sendNotificationToFirebase(messageText2.getText().toString(), "PaymentRefunded");
                                            moveAllBookingsToHistory();
                                            clearBookingMessageUI();
                                            clearBookingPreferences();


                                            // Delete the MyReview node after refund.
                                            DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
                                                    .getReference("users")
                                                    .child(userId)
                                                    .child("MyReview");
                                            myReviewRef.removeValue().addOnCompleteListener(deleteTask -> {
                                                if (deleteTask.isSuccessful()) {
                                                    Log.d("DeleteReview", "MyReview deleted after refund.");
                                                } else {
                                                    Log.e("DeleteReview", "Failed to delete MyReview after refund.", deleteTask.getException());
                                                }
                                            });
                                        }
                                    }
                                    // Optionally, you might want to stop further polling here.
                                    return;
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Optionally log error here.
                    }
                });
                // Poll every second.
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(pollTask);
    }



    /// Final Approved by Admin
    private Handler pollingHandler;
    private Runnable pollTask;

    private void FinalForApproval() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("MyBooking");

        pollingHandler = new Handler(Looper.getMainLooper());
        pollTask = new Runnable() {
            @Override
            public void run() {
                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // Kung no booking data, stop polling and clear the UI after delay.
                        if (!snapshot.hasChildren()) {
                            stopPolling();
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                clearBookingMessageUI();
                                clearBookingPreferences();
                                moveAllBookingsToHistory();
                            }, 1000);
                            return;
                        }
                        /// Process each booking entry.
                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                            String finalStatus = bookingSnapshot.child("paymentTransaction")
                                    .child("finalStatus")
                                    .getValue(String.class);
                            /// if Approved, stop polling and execute clear functions.
                            if (finalStatus != null && finalStatus.equalsIgnoreCase("Approved") && !finalProcessed) {
                                finalProcessed = true;
                                progress = Math.max(progress, 5);
                                updateDots();
                                showDot5Message();
                                stopPolling();


                                ///This code the my review is change to my review done after the booking is done
                                DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(userId)
                                        .child("MyReview");

                                DatabaseReference myReviewDoneRef = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(userId)
                                        .child("MyReviewDone");

                                myReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot reviewSnapshot) {
                                        if (reviewSnapshot.exists()) {
                                            Map<String, Object> approvedReviews = new HashMap<>();

                                            for (DataSnapshot review : reviewSnapshot.getChildren()) {
                                                //noinspection unchecked
                                                Map<String, Object> reviewData = (Map<String, Object>) review.getValue();
                                                if (reviewData != null) {
                                                    reviewData.remove("statusReview");

                                                    /// Generate a unique key for each review
                                                    DatabaseReference newReviewRef = myReviewDoneRef.push();
                                                    newReviewRef.setValue(reviewData)
                                                            .addOnCompleteListener(task -> {
                                                                if (task.isSuccessful()) {
                                                                    Log.d("CopyReview", "Review successfully added to MyReviewDone.");
                                                                } else {
                                                                    Log.e("CopyReview", "Failed to add review to MyReviewDone", task.getException());
                                                                }
                                                            });
                                                }
                                            }

                                            /// After copying, delete the original MyReview data
                                            myReviewRef.removeValue()
                                                    .addOnCompleteListener(deleteTask -> {
                                                        if (deleteTask.isSuccessful()) {
                                                            Log.d("DeleteReview", "Original MyReview deleted after copy.");
                                                        } else {
                                                            Log.e("DeleteReview", "Failed to delete MyReview.", deleteTask.getException());
                                                        }
                                                    });
                                        } else {
                                            Log.d("Review", "No MyReview data to copy.");
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("Review", "Error reading MyReview data", error.toException());
                                    }
                                });

                                ///Delay 1 minute
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    clearBookingMessageUI();
                                    clearBookingPreferences();
                                    moveAllBookingsToHistory();
                                }, 1000);
                                break; // Exit loop after processing one booking.
                            }
                            // if Failed, stop polling and execute clear functions.
                            else if (finalStatus != null && finalStatus.equalsIgnoreCase("Failed") && !finalProcessed) {
                                finalProcessed = true;
                                progress = 0;
                                updateDots();
                                stopPolling();
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    clearBookingMessageUI();
                                    clearBookingPreferences();
                                    moveAllBookingsToHistory();
                                }, 1000);
                                break;
                            }
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Error reading booking data", error.toException());
                    }
                });
                /// Schedule next poll every second.
                pollingHandler.postDelayed(this, 1000);
            }
        };
        pollingHandler.post(pollTask);
    }

    /// Function to stop the polling
    private void stopPolling() {
        if (pollingHandler != null && pollTask != null) {
            pollingHandler.removeCallbacks(pollTask);
            Log.d(TAG, "Polling stopped.");
        }
    }


    /**
     * Sends notification data to Firebase under the current user's notifications node.
     */
    private void sendNotificationToFirebase(String message, String bookingId) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || bookingId == null) return;
        String userId = currentUser.getUid();
        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("notifications")
                .child(bookingId);

        // Check if a notification already exists and whether it's been read.
        notificationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isRead = snapshot.child("read").getValue(Boolean.class);
                // Only update the notification if it is not marked as read.
                if (isRead != null && isRead) {
                    // The notification is already read—do not overwrite it.
                    return;
                }
                // Build notification data without altering the 'read' flag if it exists.
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("message", message);
                notificationData.put("timestamp", getCurrentTime());
                notificationData.put("expired", false);
                notificationRef.setValue(notificationData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Optionally log or handle the error.
            }
        });
    }


    private void clearNotification(int notificationId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId); /// Removes only the specified notification
            ///notificationManager.cancelAll(); // Clears all active notifications
        }
    }

    /**
     * Moves all booking data from "MyBooking" to "MyHistory".
     */
    private void moveAllBookingsToHistory() {
        if (bookingMoved) return;
        bookingMoved = true;

        FirebaseUser  currentUser  = FirebaseAuth.getInstance().getCurrentUser ();
        if (currentUser  == null) return;
        String userId = currentUser .getUid();
        DatabaseReference myBookingRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("MyBooking");
        DatabaseReference myHistoryRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("MyHistory");

        myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                    Object bookingData = bookingSnapshot.getValue();

                    /// Assuming bookingData has a unique identifier, e.g., bookingId
                    String bookingId = bookingSnapshot.getKey(); // or extract a unique field from bookingData

                    /// Check if the booking already exists in MyHistory
                    assert bookingId != null;
                    myHistoryRef.child(bookingId).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot historySnapshot) {
                            if (!historySnapshot.exists()) {
                                /// If it doesn't exist, add it to MyHistory
                                myHistoryRef.child(bookingId).setValue(bookingData);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Toast.makeText(BookingStatus.this, "Failed to check history for booking.", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                // After processing all bookings, remove them from MyBooking
                myBookingRef.removeValue();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(BookingStatus.this, "Failed to move bookings to history.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Clears the booking message UI.
     */
    private void clearBookingMessageUI() {
        messageText.setText("");
        messageText2.setText("");
        paymentMessageText.setText("");
        messageText4.setText("");
        messageText5.setText("");

        dot1.setBackgroundResource(R.drawable.drawable_dot_clear);
        dot2.setBackgroundResource(R.drawable.drawable_dot_clear);
        dot3.setBackgroundResource(R.drawable.drawable_dot_clear);
        dot4.setBackgroundResource(R.drawable.drawable_dot_clear);
        dot5.setBackgroundResource(R.drawable.drawable_dot_clear);

        line1_2.setBackgroundResource(R.drawable.drawable_dot_clear);
        line2_3.setBackgroundResource(R.drawable.drawable_dot_clear);
        line3_4.setBackgroundResource(R.drawable.drawable_dot_clear);
        line4_5.setBackgroundResource(R.drawable.drawable_dot_clear);
    }

    /**
     * Clears booking-related keys from SharedPreferences.
     */
    private void clearBookingPreferences() {
        prefs.edit().remove("bookingSubmitted")
                .remove("paymentSubmitted")
                .remove("paymentSubmittedTime")
                .remove("paymentApproved")
                .remove("reviewApproved")
                .remove("submissionTime")
                .remove("bookingComplete")
                .remove("bookingProgress")
                .remove("finalApproved")
                .remove("bookingId")
                .clear()
                .apply();
    }


    /// Get the current time.
    private String getCurrentTime() {
        return new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
    }

}




///Fix Current
//package com.example.resort;
//
//import static android.app.Service.START_STICKY;
//import static android.content.ContentValues.TAG;
//
//
//import android.annotation.SuppressLint;
//import android.app.ActivityManager;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.content.pm.PackageManager;
//import android.media.AudioAttributes;
//import android.media.RingtoneManager;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Looper;
//import android.service.notification.StatusBarNotification;
//import android.telephony.SmsManager;
//import android.text.Html;
//import android.util.Log;
//import android.util.Pair;
//import android.view.View;
//import android.widget.Button;
//import android.widget.FrameLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.ActivityCompat;
//import androidx.core.app.NotificationCompat;
//import androidx.core.content.ContextCompat;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.google.android.gms.tasks.OnCompleteListener;
//import com.google.android.gms.tasks.Task;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.*;
//
//import java.io.InputStream;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.net.URLEncoder;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Scanner;
//import java.util.Set;
//
//import com.google.firebase.messaging.FirebaseMessaging;
//import com.google.firebase.messaging.FirebaseMessagingService;
//import com.google.firebase.messaging.RemoteMessage;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//
//import com.android.volley.AuthFailureError;
//import com.android.volley.Request;
//import com.android.volley.toolbox.JsonObjectRequest;
//import com.android.volley.toolbox.Volley;
//
//
//import android.Manifest;
//
//public class BookingStatus extends AppCompatActivity {
//
//    /// Progress tracker: 0 = no progress, 1 = booking submitted, 2 = reviewed, 3 = payment submitted,
//    /// 4 = payment transaction approved, 5 = final approval.
//    private int progress = 0;
//    /// Passed from BookingReceipt.
//    //private String bookingId;
//    /// Prevents double data store in Firebase
//    private boolean bookingMoved = false;
//
//    ///private boolean bookingCancelDecline = false;
//    private TextView dot1, dot2, dot3, dot4, dot5;
//    private View line1_2, line2_3, line3_4, line4_5;
//    private Button payNowButton, cancelButton;
//    /// Message containers for dots 1 to 5.
//    private FrameLayout messageFramedot1, messageFramedot2, messageFramedot3, messageFramedot4, messageFramedot5;
//    private TextView messageText, messageText2, paymentMessageText, messageText4, messageText5;
//    /// SharedPreferences for persisting booking state.
//    private SharedPreferences prefs;
//    /// Flags for one-time processing.
//    private boolean approvalProcessed = false;
//    private boolean paymentApprovedProcessed = false;
//    private boolean finalProcessed = false;
//
//    /// For decline handling.
//    private boolean declineProcessed = false;
//    private boolean paymentDeclineProcessed = false;
//    private boolean bookingSubmittedProcessed = false;
//
//    ///private static final int PAYMENT_REQUEST_CODE = 1001;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_booking_status);
//
//
//        // Get current user and set up user-specific SharedPreferences.
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            finish();
//            return;
//        }
//        String userId = currentUser.getUid();
//        prefs = getSharedPreferences("BookingPref_" + userId, MODE_PRIVATE);
//
//        // If booking is submitted from previous activity, save state in SharedPreferences.
//        boolean bookingSubmittedIntent = getIntent().getBooleanExtra("bookingSubmitted", false);
//        if (bookingSubmittedIntent) {
//            prefs.edit().putBoolean("bookingSubmitted", true).apply();
//            progress = 1;
//            prefs.edit().putInt("bookingProgress", progress).apply();
//        }
//
//        boolean bookingPayIntent = getIntent().getBooleanExtra("paymentSubmitted", false);
//        if (bookingPayIntent) {
//            prefs.edit().putBoolean("paymentSubmitted", true).apply();
//            progress = 3;
//            prefs.edit().putInt("bookingProgress", progress).apply();
//        }
//
//
//
//        /// Adjust layout for system insets.
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
//                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
//                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
//                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
//            return insets;
//        });
//
//
//        /// Initialize UI elements.
//        dot1 = findViewById(R.id.dot1);
//        dot2 = findViewById(R.id.dot2);
//        dot3 = findViewById(R.id.dot3);
//        dot4 = findViewById(R.id.dot4);
//        dot5 = findViewById(R.id.dot5);
//
//        line1_2 = findViewById(R.id.line1);
//        line2_3 = findViewById(R.id.line2);
//        line3_4 = findViewById(R.id.line3);
//        line4_5 = findViewById(R.id.line4);
//
//        payNowButton = findViewById(R.id.button);
//        cancelButton = findViewById(R.id.button2);
//        Button backButton = findViewById(R.id.back2);
//
//        messageFramedot1 = findViewById(R.id.messageFramedot1);
//        messageText = findViewById(R.id.messageText);
//        messageFramedot2 = findViewById(R.id.messageFramedot2);
//        messageText2 = findViewById(R.id.messageText2);
//        messageFramedot3 = findViewById(R.id.messageFramedot3);
//        paymentMessageText = findViewById(R.id.messageText3);
//        messageFramedot4 = findViewById(R.id.messageFramedot4);
//        messageText4 = findViewById(R.id.messageText4);
//        messageFramedot5 = findViewById(R.id.messageFramedot5);
//        messageText5 = findViewById(R.id.messageText5);
//
//        // Initially hide message texts.
//        messageText.setVisibility(View.GONE);
//        messageText2.setVisibility(View.GONE);
//        paymentMessageText.setVisibility(View.GONE);
//        messageText4.setVisibility(View.GONE);
//        messageText5.setVisibility(View.GONE);
//
//        updateDots();
//
//        /// Restore persisted booking state.
//        if (prefs.contains("bookingSubmitted") && prefs.getBoolean("bookingSubmitted", false)) {
//            progress = 1;
//            updateDots();
//            showSubmissionMessage();
//        }
//        if (prefs.contains("paymentSubmitted") && prefs.getBoolean("paymentSubmitted", false)) {
//            progress = Math.max(progress, 3);
//            updateDots();
//            if (paymentMessageText.getVisibility() != View.VISIBLE) {
//                showPaymentSubmittedMessage();
//            }
//        }
//
//        if (prefs.contains("paymentApproved") && prefs.getBoolean("paymentApproved", false)) {
//            progress = 4;
//            updateDots();
//            if (messageText4.getVisibility() != View.VISIBLE) {
//                showDot4Message();
//            }
//        }
//
//        if (prefs.contains("reviewApproved") && prefs.getBoolean("reviewApproved", false)) {
//            progress = 2;
//            updateDots();
//            if (messageText2.getVisibility() != View.VISIBLE) {
//                showApprovalMessage();
//            }
//        }
//
//        if (prefs.contains("finalApproved") && prefs.getBoolean("finalApproved", false)) {
//            progress = 5;
//            updateDots();
//            if (messageText5.getVisibility() != View.VISIBLE) {
//                showDot5Message();
//            }
//        }
//
//
//        ///Pay now Button
//        payNowButton.setOnClickListener(v -> {
//            if (progress < 2) {
//                Toast.makeText(BookingStatus.this, "No booking or step not done, cannot proceed.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            Intent paymentIntent = new Intent(BookingStatus.this, Payment.class);
//            startActivity(paymentIntent);
//        });
//
//        backButton.setOnClickListener(v -> onBackPressed());
//        cancelButton.setOnClickListener(view -> cancelBooking());
//
//        /// Set up Firebase listeners for dynamic updates.
//        listenForPaymentMethodStatus();
//        listenForMyBooking();
//        listenForApproval();
//        listenForPaymentTransactionApproval();
//        FinalForApproval();
//    }
//
//
//    /// Update dots and lines based on current progress.
//    private void updateDots() {
//        dot1.setBackgroundResource(progress >= 1 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
//        dot2.setBackgroundResource(progress >= 2 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
//        dot3.setBackgroundResource(progress >= 3 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
//        dot4.setBackgroundResource(progress >= 4 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
//        dot5.setBackgroundResource(progress >= 5 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
//
//        line1_2.setBackgroundResource(progress >= 1 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
//        line2_3.setBackgroundResource(progress >= 2 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
//        line3_4.setBackgroundResource(progress >= 3 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
//        line4_5.setBackgroundResource(progress >= 4 ? R.drawable.drawable_dot_color : R.drawable.drawable_dot_clear);
//
//        if (progress == 5) {
//            payNowButton.setEnabled(false);
//        }
//        ///Save updated progress.
//        prefs.edit().putInt("bookingProgress", progress).apply();
//    }
//
//
//
//    /**
//     * Cancels a booking by:
//     * - Appending a cancellation timestamp to each booking's review.
//     * - Moving data from "MyBooking" to "MyHistory".
//     * - Updating the review status in "MyReview" to "Cancelled" (without removing it).
//     * - Updating the UI with a cancellation message.
//     * - Disabling the cancel button.
//     * - Creating a new top-level "cancelBooking" node with cancellation details.
//     */
//
//    private void cancelBooking() {
//        if (progress < 1) {
//            Toast.makeText(BookingStatus.this, "No booking in the field, cannot cancel.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        final String userId = currentUser.getUid();
//
//        // References for booking data.
//        final DatabaseReference myBookingRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyBooking");
//        final DatabaseReference myHistoryRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyHistory");
//
//        myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                // Update each booking's review status to "Cancelled".
//                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                    // Update the booking review status.
//                    bookingSnapshot.getRef()
//                            .child("bookingReview")
//                            .child("statusReview")
//                            .setValue("Cancelled");
//
//                    // If a paymentTransaction node exists, update its status and set PaymentDate.
//                    if (bookingSnapshot.hasChild("paymentTransaction")) {
//                        bookingSnapshot.getRef()
//                                .child("paymentTransaction")
//                                .child("paymentStatus")
//                                .setValue("Cancelled");
//
//                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
//                        String paymentDate = sdf.format(new Date());
//                        bookingSnapshot.getRef()
//                                .child("paymentTransaction")
//                                .child("PaymentDate")
//                                .setValue(paymentDate);
//                    }
//                }
//
//                // Move booking data to MyHistory.
//                myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot bookingSnapshot) {
//                        for (DataSnapshot child : bookingSnapshot.getChildren()) {
//                            Object bookingData = child.getValue();
//                            myHistoryRef.push().setValue(bookingData);
//                        }
//                        // Remove all bookings from MyBooking.
//                        myBookingRef.removeValue();
//
//                        DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                                .getReference("users")
//                                .child(userId)
//                                .child("MyReview");
//
//                        myReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(@NonNull DataSnapshot reviewSnapshot) {
//                                boolean hasPending = false;
//
//                                for (DataSnapshot review : reviewSnapshot.getChildren()) {
//                                    String status = review.child("statusReview").getValue(String.class);
//                                    if ("Pending".equalsIgnoreCase(status)) {
//                                        hasPending = true;
//                                        break;
//                                    }
//                                }
//
//                                if (hasPending) {
//                                    // Delete the whole MyReview node
//                                    myReviewRef.removeValue().addOnSuccessListener(aVoid ->
//                                            Log.d("BookingStatus", "MyReview deleted because it had a Pending status.")
//                                    );
//                                } else {
//                                    // Otherwise, mark all as Cancelled
//                                    for (DataSnapshot review : reviewSnapshot.getChildren()) {
//                                        review.getRef().child("statusReview").setValue("Cancelled");
//                                    }
//                                }
//                            }
//
//                            @Override
//                            public void onCancelled(@NonNull DatabaseError error) {
//                                Log.e("BookingStatus", "Review fetch error: " + error.getMessage());
//                            }
//                        });
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        // Handle error if needed.
//                    }
//                });
//
//                // Display cancellation message.
//                String cancelTime = getCurrentTime();
//                String cancellationMessage = "&quot;Booking has been Cancelled.&quot;<br>";
//                String redTime = String.format("<font color='#FF0000'>%s</font>", cancelTime);
//                messageFramedot1.setVisibility(View.VISIBLE);
//                messageText.setVisibility(View.VISIBLE);
//                messageText.setText(Html.fromHtml(cancellationMessage + redTime));
//                sendNotificationToFirebase(messageText.getText().toString(), "cancel");
//
//                // Fetch user details to build the cancellation string.
//                DatabaseReference userRef = FirebaseDatabase.getInstance()
//                        .getReference("users")
//                        .child(userId);
//                userRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
//                        String firstName = userSnapshot.child("firstName").getValue(String.class);
//                        String lastName = userSnapshot.child("lastName").getValue(String.class);
//
//                        String bookingCancelledBy = "Booking cancelled by " + firstName + " " + lastName;
//
//                        // Create a new top-level cancellation record.
//                        DatabaseReference cancelBookingRef = FirebaseDatabase.getInstance().getReference("cancelBooking");
//                        Map<String, Object> cancelData = new HashMap<>();
//                        cancelData.put("message", bookingCancelledBy);
//                        cancelData.put("date", cancelTime);
//                        ///cancelBookingRef.push().setValue(cancelData);
//
//
//                        DatabaseReference newReqRef = cancelBookingRef.push();
//                        newReqRef.setValue(cancelData)
//                                .addOnSuccessListener(aVoid -> {
//                                    /// Simplified Telegram message without technical IDs
//                                    String telegramMsg = "🔔 New Cancel Request 🔔\n"
//                                            + "👤 Name: " + firstName + " " + lastName + "\n"
//                                            + "📅 Date: " + cancelTime + "\n"
//                                            + "❌ Status: " + "Cancel";
//
//                                    sendTelegramNotification(telegramMsg);
//                                })
//                                .addOnFailureListener(e -> {
//                                    Log.e("Firebase", "BookingRequest failed", e);
//                                });
//                    }
//
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e("BookingStatus", "User data fetch failed: " + error.getMessage());
//                    }
//                });
//
//                // Reset state after a delay.
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                    /// Stop the foreground service
//                    Intent stopIntent = new Intent(BookingStatus.this, BookingStatusService.class);
//                    stopService(stopIntent);
//                    Log.d("BookingStatus", "Foreground service stopped after cancellation.");
//
//                    progress = 0;
//                    updateDots();
//                    clearBookingMessageUI();
//                    clearBookingPreferences();
//                }, 1000);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e("BookingStatus", "Cancellation failed: " + error.getMessage());
//            }
//        });
//    }
//
//    ///Telegram Api don't touch this
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
//    /// Message view if the booking is submit Not use
//    private void showSubmissionMessage() {
//        messageFramedot1.setVisibility(View.VISIBLE);
//        messageText.setVisibility(View.VISIBLE);
//        String currentTime = getCurrentTime();
//        String submissionMessage = "&quot;Booking has been Submitted. Please wait for it to be reviewed by admin.&quot;<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime );
//        messageText.setText(Html.fromHtml(submissionMessage + redTime));
//        sendNotificationToFirebase(messageText.getText().toString(), "dot1");
//        clearNotification(2);
//        clearNotification(3);
//        clearNotification(4);
//        clearNotification(5);
//        clearNotification(6);
//        clearNotification(7);
//    }
//
//    ///  Message view if the booking is payment Not use
//    private void showPaymentSubmittedMessage() {
//        messageFramedot3.setVisibility(View.VISIBLE);
//        paymentMessageText.setVisibility(View.VISIBLE);
//        String currentTime = getCurrentTime();
//        String paymentMessage = "&quot;Payment has been Submitted. Please wait for admin review.&quot;<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        paymentMessageText.setText(Html.fromHtml(paymentMessage + redTime));
//        // Disable payment and cancel buttons
//        payNowButton.setEnabled(false);
//        payNowButton.setClickable(false);
//        payNowButton.setAlpha(0.5f);
//        //Cancel
//        cancelButton.setEnabled(false);
//        cancelButton.setClickable(false);
//        cancelButton.setAlpha(0.5f);
//        sendNotificationToFirebase(paymentMessageText.getText().toString(), "dot3");
//
//    }
//
//    private void showApprovalMessage() {
//        messageFramedot2.setVisibility(View.VISIBLE);
//        messageText2.setVisibility(View.VISIBLE);
//        String currentTime = getCurrentTime();
//        String approvalMessage = "&quot;Booking has been Reviewed. Please proceed to the payment by clicking the Pay Now button.&quot;<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        String fullMessage = approvalMessage + redTime;
//        messageText2.setText(Html.fromHtml(fullMessage));
//        prefs.edit().putString("bookingStatus", "reviewApproved").apply();
//        sendNotificationToFirebase(messageText2.getText().toString(), "dot2");
//
//        // Disable the cancel button.
//        cancelButton.setEnabled(false);
//        cancelButton.setClickable(false);
//        cancelButton.setAlpha(0.5f);
//
//    }
//
//
//    private void showDot4Message() {
//        messageFramedot4.setVisibility(View.VISIBLE);
//        messageText4.setVisibility(View.VISIBLE);
//        String currentTime = getCurrentTime();
//        String msg = "&quot;Payment transaction has been Approved. Please wait for final approval.&quot;<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        String fullMessage = msg + redTime;
//        messageText4.setText(Html.fromHtml(fullMessage));
//        prefs.edit().putString("bookingStatus", "paymentApproved").apply();
//        sendNotificationToFirebase(messageText4.getText().toString(), "dot4");
//    }
//
//    private void showDot5Message() {
//        messageFramedot5.setVisibility(View.VISIBLE);
//        messageText5.setVisibility(View.VISIBLE);
//        String msg = "&quot;Congratulations! Your Booking has been Approved.&quot;<br>";
//        String currentTime = getCurrentTime();
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        String fullMessage = msg + redTime;
//        messageText5.setText(Html.fromHtml(fullMessage));
//        prefs.edit().putString("bookingStatus", "finalApproved").apply();
//        sendNotificationToFirebase(messageText5.getText().toString(), "dot5");
//    }
//
//
//    /// Booking submitted
//    private void listenForMyBooking() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//
//        String userId = currentUser.getUid();
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyBooking");
//
//        final Handler handler = new Handler(Looper.getMainLooper());
//        final Runnable[] pollTask = new Runnable[1];
//        pollTask[0] = new Runnable() {
//            @Override
//            public void run() {
//                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        // If booking exists and hasn't been processed yet, process it.
//                        if (snapshot.exists() && !bookingSubmittedProcessed) {
//                            bookingSubmittedProcessed = true;
//                            progress = 1;
//                            updateDots();
//                            showSubmissionMessage();
//                        }
//                        // Only re-poll if the booking hasn't been processed.
//                        if (!bookingSubmittedProcessed) {
//                            handler.postDelayed(pollTask[0], 1000);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e("BookingCheck", "Error reading MyBooking data", error.toException());
//                    }
//                });
//            }
//        };
//
//        handler.post(pollTask[0]);
//    }
//
//
//    /// Payment Submitted
//    private boolean bookingPayProcessed = false;
//    private DatabaseReference bookingRef;
//
//    private void listenForPaymentMethodStatus() {
//        // Get the current user
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;  // Exit if no user is logged in
//        String userId = currentUser.getUid();
//
//        // Ensure that bookingRef is only initialized once
//        if (bookingRef == null) {
//            bookingRef = FirebaseDatabase.getInstance()
//                    .getReference("users").child(userId).child("MyBooking");
//
//            final Handler handler = new Handler(Looper.getMainLooper());
//            final Runnable[] pollTask = new Runnable[1];
//            pollTask[0] = new Runnable() {
//                @Override
//                public void run() {
//                    // Use a listener to check the payment status
//                    bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            // Check if MyBooking exists for the current user
//                            if (snapshot.exists() && !bookingPayProcessed) {
//                                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                                    // Access the payment status
//                                    String paymentStatus = bookingSnapshot.child("paymentMethod")
//                                            .child("Status")
//                                            .getValue(String.class);
//
//                                    // If paymentStatus is "Done", update progress to 3 and show the message
//                                    // Inside the loop that checks each bookingSnapshot
//                                    if (paymentStatus != null && paymentStatus.equalsIgnoreCase("Done")) {
//                                        progress = Math.max(progress, 3);
//                                        bookingPayProcessed = true;
//                                        updateDots();
//                                        showPaymentSubmittedMessage();
//                                        break;
//                                    }
//                                }
//                            } else {
//                                Log.d("BookingCheck", "MyBooking does not exist for the user.");
//                            }
//
//                            // Only re-poll if the payment hasn't been processed yet.
//                            if (!bookingPayProcessed) {
//                                handler.postDelayed(pollTask[0], 1000);  // Poll every second
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            Log.e("BookingCheck", "Error reading MyBooking data", error.toException());
//                        }
//                    });
//                }
//            };
//
//            handler.post(pollTask[0]);  // Start polling
//        }
//    }
//
//
//
////    private void listenForApproval() {
////        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
////        if (currentUser == null) return;
////        String userId = currentUser.getUid();
////        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
////                .getReference("users").child(userId).child("MyBooking");
////
////        // Create a Handler to schedule polling every second
////        final Handler handler = new Handler(Looper.getMainLooper());
////        // Use an array to hold the Runnable reference
////        final Runnable[] pollTask = new Runnable[1];
////        pollTask[0] = new Runnable() {
////            @Override
////            public void run() {
////                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
////                    @Override
////                    public void onDataChange(@NonNull DataSnapshot snapshot) {
////                        boolean shouldStopPolling = false;
////                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
////                            String statusReview = bookingSnapshot.child("bookingReview")
////                                    .child("statusReview")
////                                    .getValue(String.class);
////                            if (statusReview != null) {
////                                if (statusReview.equalsIgnoreCase("Approved") && !approvalProcessed) {
////                                    approvalProcessed = true;
////                                    progress = 2;
////                                    updateDots();
////                                    showApprovalMessage();
////                                    /// Stop polling
////                                    shouldStopPolling = true;
////                                    break;
////                                } else if (statusReview.equalsIgnoreCase("Declined") && !declineProcessed) {
////                                    declineProcessed = true;
////                                    DataSnapshot paymentSnap = bookingSnapshot.child("paymentTransaction");
////                                    if (paymentSnap.exists()) {
////                                        String currentPaymentStatus = paymentSnap.child("paymentStatus")
////                                                .getValue(String.class);
////                                        if (currentPaymentStatus == null ||
////                                                !currentPaymentStatus.equalsIgnoreCase("Declined")) {
////                                            // Update paymentStatus to "Declined"
////                                            bookingSnapshot.child("paymentTransaction")
////                                                    .getRef()
////                                                    .child("paymentStatus")
////                                                    .setValue("Declined");
////
////                                            // Append PaymentDate with the current formatted date
////                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
////                                            String paymentDate = sdf.format(new Date());
////                                            bookingSnapshot.child("paymentTransaction")
////                                                    .getRef()
////                                                    .child("PaymentDate")
////                                                    .setValue(paymentDate);
////
////                                            // Update UI for decline branch
////                                            messageFramedot2.setVisibility(View.VISIBLE);
////                                            messageText2.setVisibility(View.VISIBLE);
////                                            String currentTime = getCurrentTime();
////                                            String msg = "&quot;Sorry, your booking has been declined by the admin.&quot;<br>";
////                                            String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
////                                            messageText2.setText(Html.fromHtml(msg + redTime));
////                                            ///showLocalNotification("Booking Declined!",
////                                            ///"Sorry, your booking has been declined by the admin.", 4);
////                                            sendNotificationToFirebase(messageText2.getText().toString(), "BookingDecline");
////                                            moveAllBookingsToHistory();
////                                            clearBookingMessageUI();
////                                            clearBookingPreferences();
////
////                                            // Delete the MyReview node after processing decline
////                                            DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
////                                                    .getReference("users")
////                                                    .child(userId)
////                                                    .child("MyReview");
////                                            myReviewRef.removeValue().addOnCompleteListener(task -> {
////                                                if (task.isSuccessful()) {
////                                                    Log.d("DeleteReview", "MyReview node deleted successfully.");
////                                                } else {
////                                                    Log.e("DeleteReview", "Failed to delete MyReview node.", task.getException());
////                                                }
////                                            });
////                                        }
////                                    }
////                                    // Stop polling after processing decline
////                                    shouldStopPolling = true;
////                                    break;
////                                }
////                            }
////                        }
////                        // Only reschedule if no update was processed
////                        if (!shouldStopPolling) {
////                            handler.postDelayed(pollTask[0], 1000);
////                        }
////                    }
////                    @Override
////                    public void onCancelled(@NonNull DatabaseError error) {
////                        Log.e("BookingCheck", "Error reading MyBooking data", error.toException());
////                    }
////                });
////            }
////        };
////
////        /// Start polling every second
////        handler.post(pollTask[0]);
////    }
//
//    ///Booking Review Admin
//    private void listenForApproval() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyBooking");
//
//        // Create a Handler to schedule polling every second
//        final Handler handler = new Handler(Looper.getMainLooper());
//        final Runnable[] pollTask = new Runnable[1];
//        pollTask[0] = new Runnable() {
//            @Override
//            public void run() {
//                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                            String statusReview = bookingSnapshot.child("bookingReview")
//                                    .child("statusReview")
//                                    .getValue(String.class);
//                            if (statusReview != null) {
//                                if (statusReview.equalsIgnoreCase("Approved") && !approvalProcessed) {
//                                    approvalProcessed = true;
//                                    progress = 2;
//                                    updateDots();
//                                    showApprovalMessage();
//                                } else if (statusReview.equalsIgnoreCase("Declined") && !declineProcessed) {
//                                    declineProcessed = true;
//                                    DataSnapshot paymentSnap = bookingSnapshot.child("paymentTransaction");
//                                    if (paymentSnap.exists()) {
//                                        String currentPaymentStatus = paymentSnap.child("paymentStatus")
//                                                .getValue(String.class);
//                                        if (currentPaymentStatus == null ||
//                                                !currentPaymentStatus.equalsIgnoreCase("Declined")) {
//                                            bookingSnapshot.child("paymentTransaction")
//                                                    .getRef()
//                                                    .child("paymentStatus")
//                                                    .setValue("Declined");
//
//                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
//                                            String paymentDate = sdf.format(new Date());
//                                            bookingSnapshot.child("paymentTransaction")
//                                                    .getRef()
//                                                    .child("PaymentDate")
//                                                    .setValue(paymentDate);
//
//                                            messageFramedot2.setVisibility(View.VISIBLE);
//                                            messageText2.setVisibility(View.VISIBLE);
//                                            String currentTime = getCurrentTime();
//                                            String msg = "\"Sorry, your booking has been declined by the admin.\"<br>";
//                                            String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//                                            messageText2.setText(Html.fromHtml(msg + redTime));
//                                            sendNotificationToFirebase(messageText2.getText().toString(), "BookingDecline");
//                                            moveAllBookingsToHistory();
//                                            clearBookingMessageUI();
//                                            clearBookingPreferences();
//
//                                            DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                                                    .getReference("users")
//                                                    .child(userId)
//                                                    .child("MyReview");
//                                            myReviewRef.removeValue().addOnCompleteListener(task -> {
//                                                if (task.isSuccessful()) {
//                                                    Log.d("DeleteReview", "MyReview node deleted successfully.");
//                                                } else {
//                                                    Log.e("DeleteReview", "Failed to delete MyReview node.", task.getException());
//                                                }
//                                            });
//                                        }
//                                    }
//                                }
//                            }
//                        }
//                        // Continue polling every second
//                        handler.postDelayed(pollTask[0], 1000);
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e("BookingCheck", "Error reading MyBooking data", error.toException());
//                        // Continue polling even on error
//                        handler.postDelayed(pollTask[0], 1000);
//                    }
//                });
//            }
//        };
//
//        // Start polling every second
//        handler.post(pollTask[0]);
//    }
//
//
//    ///Payment Admin Approval
//    private void listenForPaymentTransactionApproval() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyBooking");
//
//        final Handler handler = new Handler(Looper.getMainLooper());
//        Runnable pollTask = new Runnable() {
//            @Override
//            public void run() {
//                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                            if (bookingSnapshot.child("paymentTransaction").exists()) {
//                                String paymentStatus = bookingSnapshot.child("paymentTransaction")
//                                        .child("paymentStatus")
//                                        .getValue(String.class);
//                                // When paymentStatus is Approved, update progress to 4
//                                if (paymentStatus != null &&
//                                        paymentStatus.equalsIgnoreCase("Approved") &&
//                                        !paymentApprovedProcessed) {
//                                    paymentApprovedProcessed = true;
//                                    progress = Math.max(progress, 4);  // Set progress directly to 4
//                                    updateDots();  // Ensure this method updates your progress UI elements
//                                    showDot4Message();
//                                }
//                                // Handle Refund branch if needed.
//                                else if (paymentStatus != null &&
//                                        paymentStatus.equalsIgnoreCase("Refund") &&
//                                        !paymentDeclineProcessed) {
//                                    paymentDeclineProcessed = true;
//                                    // Process refund logic as you already have.
//                                    DataSnapshot paymentSnap = bookingSnapshot.child("bookingReview");
//                                    if (paymentSnap.exists()) {
//                                        String currentPaymentStatus = paymentSnap.child("statusReview")
//                                                .getValue(String.class);
//                                        if (currentPaymentStatus == null ||
//                                                !currentPaymentStatus.equalsIgnoreCase("Refund")) {
//                                            bookingSnapshot.child("bookingReview")
//                                                    .getRef()
//                                                    .child("statusReview")
//                                                    .setValue("Refund");
//
//                                            // Update UI for refund branch.
//                                            messageFramedot2.setVisibility(View.VISIBLE);
//                                            messageText2.setVisibility(View.VISIBLE);
//                                            String currentTime = getCurrentTime();
//                                            String msg = "&quot;Your payment has been reversed and refunded by the admin.&quot;<br>";
//                                            String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//                                            messageText2.setText(Html.fromHtml(msg + redTime));
//                                            sendNotificationToFirebase(messageText2.getText().toString(), "PaymentRefunded");
//                                            moveAllBookingsToHistory();
//                                            clearBookingMessageUI();
//                                            clearBookingPreferences();
//
//
//                                            // Delete the MyReview node after refund.
//                                            DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                                                    .getReference("users")
//                                                    .child(userId)
//                                                    .child("MyReview");
//                                            myReviewRef.removeValue().addOnCompleteListener(deleteTask -> {
//                                                if (deleteTask.isSuccessful()) {
//                                                    Log.d("DeleteReview", "MyReview deleted after refund.");
//                                                } else {
//                                                    Log.e("DeleteReview", "Failed to delete MyReview after refund.", deleteTask.getException());
//                                                }
//                                            });
//                                        }
//                                    }
//                                    // Optionally, you might want to stop further polling here.
//                                    return;
//                                }
//                            }
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        // Optionally log error here.
//                    }
//                });
//                // Poll every second.
//                handler.postDelayed(this, 1000);
//            }
//        };
//        handler.post(pollTask);
//    }
//
//
//
//    /// Final Approved by Admin
//    private Handler pollingHandler;
//    private Runnable pollTask;
//
//    private void FinalForApproval() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyBooking");
//
//        pollingHandler = new Handler(Looper.getMainLooper());
//        pollTask = new Runnable() {
//            @Override
//            public void run() {
//                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        // Kung no booking data, stop polling and clear the UI after delay.
//                        if (!snapshot.hasChildren()) {
//                            stopPolling();
//                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                clearBookingMessageUI();
//                                clearBookingPreferences();
//                                moveAllBookingsToHistory();
//                                ///clearNotification(1);
//                                ///clearNotification(2);
//                            }, 1000);
//                            return;
//                        }
//                        /// Process each booking entry.
//                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                            String finalStatus = bookingSnapshot.child("paymentTransaction")
//                                    .child("finalStatus")
//                                    .getValue(String.class);
//                            /// if Approved, stop polling and execute clear functions.
//                            if (finalStatus != null && finalStatus.equalsIgnoreCase("Approved") && !finalProcessed) {
//                                finalProcessed = true;
//                                progress = Math.max(progress, 5);
//                                updateDots();
//                                ///showLocalNotification("Congratulations!", "Your booking has been finally approved.", 3);
//                                showDot5Message();
//                                stopPolling();
//
//
//                                ///This code the my review is change to my review done after the booking is done
//                                DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                                        .getReference("users")
//                                        .child(userId)
//                                        .child("MyReview");
//
//                                DatabaseReference myReviewDoneRef = FirebaseDatabase.getInstance()
//                                        .getReference("users")
//                                        .child(userId)
//                                        .child("MyReviewDone");
//
//                                myReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                                    @Override
//                                    public void onDataChange(@NonNull DataSnapshot reviewSnapshot) {
//                                        if (reviewSnapshot.exists()) {
//                                            Map<String, Object> approvedReviews = new HashMap<>();
//
//                                            for (DataSnapshot review : reviewSnapshot.getChildren()) {
//                                                //noinspection unchecked
//                                                Map<String, Object> reviewData = (Map<String, Object>) review.getValue();
//                                                if (reviewData != null) {
//                                                    reviewData.remove("statusReview");
//
//                                                    /// Generate a unique key for each review
//                                                    DatabaseReference newReviewRef = myReviewDoneRef.push();
//                                                    newReviewRef.setValue(reviewData)
//                                                            .addOnCompleteListener(task -> {
//                                                                if (task.isSuccessful()) {
//                                                                    Log.d("CopyReview", "Review successfully added to MyReviewDone.");
//                                                                } else {
//                                                                    Log.e("CopyReview", "Failed to add review to MyReviewDone", task.getException());
//                                                                }
//                                                            });
//                                                }
//                                            }
//
//                                            /// After copying, delete the original MyReview data
//                                            myReviewRef.removeValue()
//                                                    .addOnCompleteListener(deleteTask -> {
//                                                        if (deleteTask.isSuccessful()) {
//                                                            Log.d("DeleteReview", "Original MyReview deleted after copy.");
//                                                        } else {
//                                                            Log.e("DeleteReview", "Failed to delete MyReview.", deleteTask.getException());
//                                                        }
//                                                    });
//                                        } else {
//                                            Log.d("Review", "No MyReview data to copy.");
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onCancelled(@NonNull DatabaseError error) {
//                                        Log.e("Review", "Error reading MyReview data", error.toException());
//                                    }
//                                });
//
//                                ///Delay 1 minute
//                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                    clearBookingMessageUI();
//                                    clearBookingPreferences();
//                                    moveAllBookingsToHistory();
//                                    ///clearNotification(1);
//                                    ///clearNotification(2);
//                                }, 1000);
//                                break; // Exit loop after processing one booking.
//                            }
//                            // if Failed, stop polling and execute clear functions.
//                            else if (finalStatus != null && finalStatus.equalsIgnoreCase("Failed") && !finalProcessed) {
//                                finalProcessed = true;
//                                progress = 0;
//                                updateDots();
//                                ///showLocalNotification("Booking Failed", "Your booking has failed. Please try again.", 6);
//                                stopPolling();
//                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                    clearBookingMessageUI();
//                                    clearBookingPreferences();
//                                    moveAllBookingsToHistory();
//                                    ///clearNotification(1);
//                                    ///clearNotification(2);
//                                }, 1000);
//                                break;
//                            }
//                        }
//                    }
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e(TAG, "Error reading booking data", error.toException());
//                    }
//                });
//                // Schedule next poll every second.
//                pollingHandler.postDelayed(this, 1000);
//            }
//        };
//        pollingHandler.post(pollTask);
//    }
//
//    /// Function to stop the polling
//    private void stopPolling() {
//        if (pollingHandler != null && pollTask != null) {
//            pollingHandler.removeCallbacks(pollTask);
//            Log.d(TAG, "Polling stopped.");
//        }
//    }
//
//
//    /**
//     * Sends notification data to Firebase under the current user's notifications node.
//     */
//    private void sendNotificationToFirebase(String message, String bookingId) {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null || bookingId == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("notifications")
//                .child(bookingId);
//
//        // Check if a notification already exists and whether it's been read.
//        notificationRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                Boolean isRead = snapshot.child("read").getValue(Boolean.class);
//                // Only update the notification if it is not marked as read.
//                if (isRead != null && isRead) {
//                    // The notification is already read—do not overwrite it.
//                    return;
//                }
//                // Build notification data without altering the 'read' flag if it exists.
//                Map<String, Object> notificationData = new HashMap<>();
//                notificationData.put("message", message);
//                notificationData.put("timestamp", getCurrentTime());
//                notificationData.put("expired", false);
//                notificationRef.setValue(notificationData);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                // Optionally log or handle the error.
//            }
//        });
//    }
//
//    ///Show device notification not use this code
////    @SuppressLint("ObsoleteSdkInt")
////    private void showLocalNotification(String title, String message, int notificationId) {
////        // Get the NotificationManager service.
////        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
////
////        // Check if a notification with the same ID is already active to avoid duplicate notifications.
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
////            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
////            for (StatusBarNotification sbn : activeNotifications) {
////                if (sbn.getId() == notificationId) {
////                    // Already active; do not post a duplicate.
////                    return;
////                }
////            }
////        }
////
////        // For Android Oreo and above, create a notification channel.
////        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
////            NotificationChannel channel = new NotificationChannel(
////                    "booking_channel",
////                    "Booking Notifications",
////                    NotificationManager.IMPORTANCE_DEFAULT);
////            channel.setDescription("Channel for booking notifications");
////            channel.enableLights(true);
////            channel.enableVibration(true);
////            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
////                    new AudioAttributes.Builder()
////                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
////                            .build());
////            notificationManager.createNotificationChannel(channel);
////        }
////
////        // Build the notification.
////        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "booking_channel")
////                .setSmallIcon(R.drawable.ic_profile_notification) // Ensure this icon exists.
////                .setContentTitle(title)
////                .setContentText(message)
////                .setAutoCancel(true)
////                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
////
////        // Post the notification.
////        notificationManager.notify(notificationId, builder.build());
////    }
////
////
//
//
//    private void clearNotification(int notificationId) {
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        if (notificationManager != null) {
//            notificationManager.cancel(notificationId); // Removes only the specified notification
//            //notificationManager.cancelAll(); // Clears all active notifications
//        }
//    }
//
//
////    /**
////     * Moves all booking data from "MyBooking" to "MyHistory".
////     */
////    private void moveAllBookingsToHistory() {
////        if (bookingMoved) return;
////        bookingMoved = true;
////
////        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
////        if (currentUser == null) return;
////        String userId = currentUser.getUid();
////        DatabaseReference myBookingRef = FirebaseDatabase.getInstance()
////                .getReference("users")
////                .child(userId)
////                .child("MyBooking");
////        DatabaseReference myHistoryRef = FirebaseDatabase.getInstance()
////                .getReference("users")
////                .child(userId)
////                .child("MyHistory");
////
////        myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
////            @Override
////            public void onDataChange(@NonNull DataSnapshot snapshot) {
////                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
////                    Object bookingData = bookingSnapshot.getValue();
////                    myHistoryRef.push().setValue(bookingData);
////                }
////                myBookingRef.removeValue();
////            }
////
////            @Override
////            public void onCancelled(@NonNull DatabaseError error) {
////                Toast.makeText(BookingStatus.this, "Failed to move bookings to history.", Toast.LENGTH_SHORT).show();
////            }
////        });
////    }
////
//
//    /**
//     * Moves all booking data from "MyBooking" to "MyHistory".
//     */
//    private void moveAllBookingsToHistory() {
//        if (bookingMoved) return;
//        bookingMoved = true;
//
//        FirebaseUser  currentUser  = FirebaseAuth.getInstance().getCurrentUser ();
//        if (currentUser  == null) return;
//        String userId = currentUser .getUid();
//        DatabaseReference myBookingRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyBooking");
//        DatabaseReference myHistoryRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyHistory");
//
//        myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                    Object bookingData = bookingSnapshot.getValue();
//
//                    /// Assuming bookingData has a unique identifier, e.g., bookingId
//                    String bookingId = bookingSnapshot.getKey(); // or extract a unique field from bookingData
//
//                    /// Check if the booking already exists in MyHistory
//                    assert bookingId != null;
//                    myHistoryRef.child(bookingId).addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot historySnapshot) {
//                            if (!historySnapshot.exists()) {
//                                /// If it doesn't exist, add it to MyHistory
//                                myHistoryRef.child(bookingId).setValue(bookingData);
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            Toast.makeText(BookingStatus.this, "Failed to check history for booking.", Toast.LENGTH_SHORT).show();
//                        }
//                    });
//                }
//                // After processing all bookings, remove them from MyBooking
//                myBookingRef.removeValue();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(BookingStatus.this, "Failed to move bookings to history.", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    /**
//     * Clears the booking message UI.
//     */
//    private void clearBookingMessageUI() {
//        messageText.setText("");
//        messageText2.setText("");
//        paymentMessageText.setText("");
//        messageText4.setText("");
//        messageText5.setText("");
//
//        dot1.setBackgroundResource(R.drawable.drawable_dot_clear);
//        dot2.setBackgroundResource(R.drawable.drawable_dot_clear);
//        dot3.setBackgroundResource(R.drawable.drawable_dot_clear);
//        dot4.setBackgroundResource(R.drawable.drawable_dot_clear);
//        dot5.setBackgroundResource(R.drawable.drawable_dot_clear);
//
//        line1_2.setBackgroundResource(R.drawable.drawable_dot_clear);
//        line2_3.setBackgroundResource(R.drawable.drawable_dot_clear);
//        line3_4.setBackgroundResource(R.drawable.drawable_dot_clear);
//        line4_5.setBackgroundResource(R.drawable.drawable_dot_clear);
//    }
//
//    /**
//     * Clears booking-related keys from SharedPreferences.
//     */
//    private void clearBookingPreferences() {
//        prefs.edit().remove("bookingSubmitted")
//                .remove("paymentSubmitted")
//                .remove("paymentSubmittedTime")
//                .remove("paymentApproved")
//                .remove("reviewApproved")
//                .remove("submissionTime")
//                .remove("bookingComplete")
//                .remove("bookingProgress")
//                .remove("finalApproved")
//                .remove("bookingId")
//                .clear()
//                .apply();
//    }
//
//
//    /// Get the current time.
//    private String getCurrentTime() {
//        return new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//    }
//
//}
//
