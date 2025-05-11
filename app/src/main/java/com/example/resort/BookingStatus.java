package com.example.resort;

import static android.app.Service.START_STICKY;
import static android.content.ContentValues.TAG;


import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.pdf.PdfDocument;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.MediaStore;
import android.service.notification.StatusBarNotification;
import android.telephony.SmsManager;
import android.text.Html;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationChannelCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
import java.util.function.Consumer;

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
    private Button payNowButton, cancelButton, refreshButton;
    /// Message containers for dots 1 to 5.
    private FrameLayout messageFramedot1, messageFramedot2, messageFramedot3, messageFramedot4, messageFramedot5;
    private TextView messageText, messageText2, paymentMessageText, messageText4, messageText5, viewDetails,  downloadDetails;
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

        /// If booking is submitted from previous activity, save state in SharedPreferences.
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
        refreshButton = findViewById(R.id.refresh);

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
        viewDetails = findViewById(R.id.View);
        downloadDetails = findViewById(R.id.Download);

        // Initially hide message texts.
        messageText.setVisibility(View.GONE);
        messageText2.setVisibility(View.GONE);
        refreshButton .setVisibility(View.GONE);
        paymentMessageText.setVisibility(View.GONE);
        messageText4.setVisibility(View.GONE);
        messageText5.setVisibility(View.GONE);
        viewDetails.setVisibility(View.GONE);
        downloadDetails.setVisibility(View.GONE);

        viewDetails.setEnabled(true);
        viewDetails.setClickable(true);
        downloadDetails.setEnabled(true);
        downloadDetails.setClickable(true);

        viewDetails.setOnClickListener(v -> {
            Log.d("BookingStatus", "⚡ View Details clicked!");
            fetchAndShowPaymentDetails(v.getContext());
        });

        downloadDetails.setOnClickListener(v -> {
            Log.d("BookingStatus", "⚡ View Details clicked!");
            fetchAndShowDownloadDetails(v.getContext());
        });
        viewDetails.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });

        downloadDetails.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(100).start();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false;
        });


        updateDots();

        /// Restore persisted booking state.
        if (prefs.contains("bookingSubmitted") && prefs.getBoolean("bookingSubmitted", false)) {
            progress = 1;
            updateDots();
            showSubmissionMessage();
        }
        if (prefs.contains("paymentSubmitted") && prefs.getBoolean("paymentSubmitted", false)) {
            progress = 3;
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


        ///Refresh Data
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Refresh", "Reloading data...");
                refreshData(); /// Call your listener functions again
            }
        });


        ///Pay Now Button
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

    ///Refresh Data
    private void refreshData() {
        forceProcessDeclinesAndRefunds();
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

    ///Force move if decline and refund is stock up
    private void forceProcessDeclinesAndRefunds() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(u.getUid())
                .child("MyBooking");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snap) {
                for (DataSnapshot b : snap.getChildren()) {
                    String review = b.child("bookingReview")
                            .child("statusReview")
                            .getValue(String.class);
                    String pay = b.child("paymentTransaction")
                            .child("paymentStatus")
                            .getValue(String.class);
                    String finalStatus = b.child("paymentTransaction")
                            .child("finalStatus")
                            .getValue(String.class);

                    // Trigger actions if Declined, Refunded, or Approved
                    if ("Declined".equalsIgnoreCase(review)
                            || "Refund".equalsIgnoreCase(pay)
                            || "Approved".equalsIgnoreCase(finalStatus)) {

                        moveAllBookingsToHistory();
                        clearBookingMessageUI();
                        clearBookingPreferences();
                        break;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError e) { }
        });
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

    /// Message view if the booking is submit by user
    private void showSubmissionMessage() {
        messageFramedot1.setVisibility(View.VISIBLE);
        messageText.setVisibility(View.VISIBLE);
        String currentTime = getCurrentTime();
        String submissionMessage = "&quot;Booking has been Submitted. Please wait for it to be reviewed by admin.&quot;<br>";
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime );
        messageText.setText(Html.fromHtml(submissionMessage + redTime));
        sendNotificationToFirebase(messageText.getText().toString(), "dot1");
        clearNotification(100);
        clearNotification(101);
        clearNotification(102);
        clearNotification(103);
        clearNotification(104);
        clearNotification(105);
        clearNotification(106);

    }

    ///  Message view if the booking is payment submitted
    private void showPaymentSubmittedMessage() {
        messageFramedot3.setVisibility(View.VISIBLE);
        paymentMessageText.setVisibility(View.VISIBLE);
        viewDetails.setVisibility(View.VISIBLE);
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
        downloadDetails.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.VISIBLE);
        String msg = "&quot;Congratulations! Your Booking has been Approved. Click the refresh now!&quot;<br>";
        String currentTime = getCurrentTime();
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
        String fullMessage = msg + redTime;
        messageText5.setText(Html.fromHtml(fullMessage));
        prefs.edit().putString("bookingStatus", "finalApproved").apply();
        sendNotificationToFirebase(messageText5.getText().toString(), "dot5");

    }


    ///Booking Review Decline
    private void showDecline() {
        /// Update UI for decline branch
        messageFramedot2.setVisibility(View.VISIBLE);
        messageText2.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.VISIBLE);
        String currentTime = getCurrentTime();
        String msg = "&quot;Sorry, your booking has been declined. Click the refresh now!&quot;<br>";
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
        messageText2.setText(Html.fromHtml(msg + redTime));
        sendNotificationToFirebase(messageText2.getText().toString(), "BookingDecline");


        ///moveAllBookingsToHistory();
        ///clearBookingMessageUI();
        ///clearBookingPreferences();

    }



    ///Payment Refunded
    private void showRefund() {
        /// Update UI for refund branch.
        messageFramedot4.setVisibility(View.VISIBLE);
        messageText4.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.VISIBLE);
        String currentTime = getCurrentTime();
        String msg = "&quot;Sorry, your payment has been refunded. Click the refresh now!&quot;<br>";
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
        messageText4.setText(Html.fromHtml(msg + redTime));
        sendNotificationToFirebase(messageText4.getText().toString(), "PaymentRefunded");


        ///moveAllBookingsToHistory();
        ///clearBookingMessageUI();
        ///clearBookingPreferences();

    }

    ///Payment Refunded 2
    private void showRefundMessage() {
        messageFramedot2.setVisibility(View.VISIBLE);
        messageText2.setVisibility(View.VISIBLE);
        refreshButton.setVisibility(View.VISIBLE);
        String currentTime = getCurrentTime();
        String approvalMessage = "&quot;Booking has been Reviewed. Please proceed to the payment by clicking the Pay Now button.&quot;<br>";
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
        String fullMessage = approvalMessage + redTime;
        messageText2.setText(Html.fromHtml(fullMessage));
        prefs.edit().putString("bookingStatus", "reviewApproved").apply();

        /// Disable the cancel button.
        cancelButton.setEnabled(false);
        cancelButton.setClickable(false);
        cancelButton.setAlpha(0.5f);
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
    private Handler handler;
    private boolean bookingPayProcessed = false;
    private DatabaseReference bookingRef;

    private void listenForPaymentMethodStatus() {
        /// Get the current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();

        /// Ensure that bookingRef is only initialized once
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
                            /// Check if MyBooking exists for the current user
                            if (snapshot.exists() && !bookingPayProcessed) {
                                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                                    /// Access the payment status
                                    String paymentStatus = bookingSnapshot.child("paymentMethod")
                                            .child("Status")
                                            .getValue(String.class);

                                    /// If paymentStatus is "Done", update progress to 3 and show the message
                                    /// Inside the loop that checks each bookingSnapshot
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
              /// Listen for sign-out or user change so we can clean up
            FirebaseAuth.getInstance().addAuthStateListener(auth -> {
                FirebaseUser newUser = auth.getCurrentUser();
                /// If the user has signed out or switched
                if (newUser == null || !newUser.getUid().equals(userId)) {
                    stopPaymentStatusPolling();
                }
            });

        }
    }
    private void stopPaymentStatusPolling() {
        if (handler != null && pollTask != null) {
            handler.removeCallbacks(pollTask);
        }
        bookingPayProcessed = false;
    }



    ///Booking Review Admin
    private void listenForApproval() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                .getReference("users").child(userId).child("MyBooking");

        /// Create a Handler to schedule polling every second
        final Handler handler = new Handler(Looper.getMainLooper());
        /// Use an array to hold the Runnable reference
        final Runnable[] pollTask = new Runnable[1];
        pollTask[0] = new Runnable() {
            @Override
            public void run() {
                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean shouldStopPolling = false;
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
                                    /// Stop polling
                                    shouldStopPolling = true;
                                    break;
                                } else if (statusReview.equalsIgnoreCase("Declined") && !declineProcessed) {
                                    declineProcessed = true;
                                    progress = 2; ///New Data
                                    updateDots();
                                    showDecline(); ///Message


                                    DataSnapshot paymentSnap = bookingSnapshot.child("paymentTransaction");
                                    if (paymentSnap.exists()) {
                                        String currentPaymentStatus = paymentSnap.child("paymentStatus")
                                                .getValue(String.class);
                                        if (currentPaymentStatus == null ||
                                                !currentPaymentStatus.equalsIgnoreCase("Declined")) {
                                            /// Update paymentStatus to "Declined"
                                            bookingSnapshot.child("paymentTransaction")
                                                    .getRef()
                                                    .child("paymentStatus")
                                                    .setValue("Declined");

                                            /// Append PaymentDate with the current formatted date
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                                            String paymentDate = sdf.format(new Date());
                                            bookingSnapshot.child("paymentTransaction")
                                                    .getRef()
                                                    .child("PaymentDate")
                                                    .setValue(paymentDate);


                                            /// Delete the MyReview node after processing decline
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
                                    /// Stop polling after processing decline
                                    shouldStopPolling = true;
                                    break;
                                }
                            }
                        }
                        /// Only reschedule if no update was processed
                        if (!shouldStopPolling) {
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

        /// Start polling every second
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
                                    progress = Math.max(progress, 4);
                                    updateDots();
                                    showDot4Message();
                                }
                                // Handle Refund branch if needed.
                                else if (paymentStatus != null &&
                                        paymentStatus.equalsIgnoreCase("Refund") &&
                                        !paymentDeclineProcessed) {
                                    paymentDeclineProcessed = true;
                                    showRefund(); ///Message
                                    showRefundMessage(); ///Message Refund 2
                                    progress = Math.max(progress, 4);

                                    /// Process refund logic as you already have.
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
                        /// Kung no booking data, stop polling and clear the UI after delay.
                        if (!snapshot.hasChildren()) {
                            stopPolling();
                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                ///clearBookingMessageUI();
                                ///clearBookingPreferences();
                                ///moveAllBookingsToHistory();
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

                                /// Stop the foreground service
                                Intent stopIntent = new Intent(BookingStatus.this, BookingStatusService.class);
                                stopService(stopIntent);
                                Log.d("BookingStatus", "Foreground service stopped.");

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
                                        if (!reviewSnapshot.exists()) {
                                            Log.d("Review", "No MyReview data to copy.");
                                            return;
                                        }
                                        for (DataSnapshot review : reviewSnapshot.getChildren()) {
                                            String originalKey = review.getKey();
                                            if (originalKey == null) {
                                                /// No key? Skip it—can’t map it reliably
                                                Log.w("CopyReview", "Encountered a review with null key; skipping.");
                                                continue;
                                            }

                                            //noinspection unchecked
                                            Map<String, Object> reviewData = (Map<String, Object>) review.getValue();
                                            if (reviewData == null) {
                                                Log.w("CopyReview", "Review data for key " + originalKey + " is null; skipping.");
                                                continue;
                                            }

                                            /// Drop the status flag
                                            reviewData.remove("statusReview");

                                            /// Use the same key to overwrite if it already exists
                                            myReviewDoneRef.child(originalKey)
                                                    .setValue(reviewData)
                                                    .addOnCompleteListener(task -> {
                                                        if (task.isSuccessful()) {
                                                            Log.d("CopyReview", "Review " + originalKey + " added/updated in MyReviewDone.");
                                                        } else {
                                                            Log.e("CopyReview", "Failed to add review " + originalKey,
                                                                    task.getException());
                                                        }
                                                    });
                                        }

                                        /// Finally, delete the whole original node in one go
                                        myReviewRef.removeValue()
                                                .addOnCompleteListener(deleteTask -> {
                                                    if (deleteTask.isSuccessful()) {
                                                        Log.d("DeleteReview", "Original MyReview deleted after copy.");
                                                    } else {
                                                        Log.e("DeleteReview", "Failed to delete MyReview.",
                                                                deleteTask.getException());
                                                    }
                                                });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        Log.e("Review", "Error reading MyReview data", error.toException());
                                    }
                                });


                                ///Delay 1 second
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    ///clearBookingMessageUI();
                                    ///clearBookingPreferences();
                                    ///moveAllBookingsToHistory();
                                }, 1000);
                                break; /// Exit loop after processing one booking.
                            }
                            /// if Failed, stop polling and execute clear functions.
                            else if (finalStatus != null && finalStatus.equalsIgnoreCase("Refund") && !finalProcessed) {
                                finalProcessed = true;
                                progress = 0;
                                updateDots();
                                stopPolling();
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    ///clearBookingMessageUI();
                                    ///clearBookingPreferences();
                                    ///moveAllBookingsToHistory();
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

        /// Check if a notification already exists and whether it's been read.
        notificationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Boolean isRead = snapshot.child("read").getValue(Boolean.class);
                /// Only update the notification if it is not marked as read.
                if (isRead != null && isRead) {
                    /// The notification is already read—do not overwrite it.
                    return;
                }
                /// Build notification data without altering the 'read' flag if it exists.
                Map<String, Object> notificationData = new HashMap<>();
                notificationData.put("message", message);
                notificationData.put("timestamp", getCurrentTime());
                notificationData.put("expired", false);
                notificationRef.setValue(notificationData);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                /// Optionally log or handle the error.
            }
        });
    }


    private void clearNotification(int notificationId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId); /// Removes only the specified notification
            ///notificationManager.cancelAll(); /// Clears all active notifications
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
                /// After processing all bookings, remove them from MyBooking
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
        viewDetails.setVisibility(View.GONE);
        downloadDetails.setVisibility(View.GONE);
        refreshButton .setVisibility(View.GONE);

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
        prefs.edit().clear().apply();
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

    private void fetchAndShowPaymentDetails(Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = user.getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("MyBooking");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(context, "No booking data found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                    DataSnapshot paymentMethodSnapshot = bookingSnapshot.child("paymentMethod");

                    String firstname = paymentMethodSnapshot.child("Firstname").getValue(String.class);
                    String lastname = paymentMethodSnapshot.child("Lastname").getValue(String.class);
                    String payment = paymentMethodSnapshot.child("Payment").getValue(String.class);
                    Long amount = paymentMethodSnapshot.child("Amount").getValue(Long.class);
                    Long balance = paymentMethodSnapshot.child("Balance").getValue(Long.class);
                    String date = paymentMethodSnapshot.child("Date").getValue(String.class);
                    String reference = paymentMethodSnapshot.child("Reference").getValue(String.class);
                    String status = paymentMethodSnapshot.child("Status").getValue(String.class);
                    Long total = paymentMethodSnapshot.child("total").getValue(Long.class);

                    /// Create the dialog builder
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    LayoutInflater inflater = LayoutInflater.from(context);
                    View view = inflater.inflate(R.layout.payment_details, null);

                    /// Set all TextViews with the data
                    ((TextView) view.findViewById(R.id.tvName)).setText(firstname + " " + lastname);
                    ((TextView) view.findViewById(R.id.tvPaymentMethod)).setText(payment);
                    ((TextView) view.findViewById(R.id.tvAmount)).setText("₱" + (amount != null ? amount : 0));
                    ((TextView) view.findViewById(R.id.tvBalance)).setText("₱" + (balance != null ? balance : 0));
                    ((TextView) view.findViewById(R.id.tvTotal)).setText("₱" + (total != null ? total : 0));
                    ((TextView) view.findViewById(R.id.tvDate)).setText(date);
                    ((TextView) view.findViewById(R.id.tvPhone)).setText(paymentMethodSnapshot.child("Phone").getValue(String.class));
                    ((TextView) view.findViewById(R.id.tvReference)).setText(reference);

                    /// Change the button text to "Okay"
                    Button CancelButton = view.findViewById(R.id.btnCancelPayment);
                    CancelButton.setText("Cancel");

                    /// Create and show the dialog
                    AlertDialog dialog = builder.setView(view).create();
                    dialog.show();

                    /// Set a click listener on the "Okay" button to dismiss the dialog
                    CancelButton.setOnClickListener(v -> {
                        if (dialog.isShowing()) {
                            dialog.dismiss();
                        }
                    });

                     // --- ADD THIS DOWNLOAD BLOCK ---
                    Button btnDownload = view.findViewById(R.id.btnDownloadPayment);
                    btnDownload.setOnClickListener(v -> {
                        // Hide action buttons
                        btnDownload.setVisibility(View.GONE);
                        CancelButton.setVisibility(View.GONE);

                        view.post(() -> {
                            downloadAsPDF(view, dialog, success -> {
                                if (!success) {
                                    btnDownload.setVisibility(View.VISIBLE);
                                    CancelButton.setVisibility(View.VISIBLE);
                                }
                            });
                        });
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }




   ///Receipt Data
    public void fetchAndShowDownloadDetails(final Context context) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        final String userId = user.getUid();

        DatabaseReference profileRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId);

        profileRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot profileSnap) {
                String fName = profileSnap.child("firstName").getValue(String.class);
                String lName = profileSnap.child("lastName").getValue(String.class);
                String mi = profileSnap.child("middleInitial").getValue(String.class);
                String street = profileSnap.child("street").getValue(String.class);
                String barangay = profileSnap.child("barangay").getValue(String.class);
                String municipality = profileSnap.child("municipality").getValue(String.class);
                String province = profileSnap.child("province").getValue(String.class);

                DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(userId)
                        .child("MyBooking");

                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot bookingSnap) {
                        if (!bookingSnap.exists()) {
                            Toast.makeText(context, "No booking data found.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        for (DataSnapshot booking : bookingSnap.getChildren()) {
                            DataSnapshot review = booking.child("bookingReview");
                            DataSnapshot orderItems = review.child("orderItems");

                            String fullName = fName + " " + mi + ". " + lName;
                            String address = street + "\n" + barangay + ", " + municipality + ", " + province;
                            String refNo = review.child("refNo").getValue(String.class);
                            String bDate = review.child("bookingDate").getValue(String.class);
                            Double amount = review.child("amount").getValue(Double.class);

                            LayoutInflater inflater = LayoutInflater.from(context);
                            View dialogV = inflater.inflate(R.layout.download_details, null);

                            ((TextView) dialogV.findViewById(R.id.tvCustomerName)).setText(fullName);
                            ((TextView) dialogV.findViewById(R.id.tvAddress)).setText(address);
                            ((TextView) dialogV.findViewById(R.id.tvReceiptNo)).setText("Ref: " + refNo);
                            ((TextView) dialogV.findViewById(R.id.tvDate)).setText(bDate);
                            ((TextView) dialogV.findViewById(R.id.tvTotal)).setText("Total: ₱" + amount);

                            LinearLayout itemContainer = dialogV.findViewById(R.id.itemContainer);

                            // Accommodations
                            DataSnapshot accommodations = orderItems.child("accommodations");
                            if (accommodations.exists()) {
                                for (DataSnapshot itemSnap : accommodations.getChildren()) {
                                    View row = inflater.inflate(R.layout.item_row, itemContainer, false);
                                    String name = itemSnap.child("name").getValue(String.class);
                                    Long qty = itemSnap.child("quantity").getValue(Long.class);
                                    Double price = itemSnap.child("price").getValue(Double.class);
                                    if (name != null && qty != null && price != null) {
                                        ((TextView) row.findViewById(R.id.tvProductName)).setText(name);
                                        ((TextView) row.findViewById(R.id.tvQty)).setText(String.valueOf(qty));
                                        ((TextView) row.findViewById(R.id.tvPrice)).setText(String.format("₱%.2f", price));
                                        itemContainer.addView(row);
                                    }
                                }
                            }

                            /// Food and Drinks
                            DataSnapshot foodAndDrinks = orderItems.child("foodAndDrinks");
                            if (foodAndDrinks.exists()) {
                                for (DataSnapshot itemSnap : foodAndDrinks.getChildren()) {
                                    View row = inflater.inflate(R.layout.item_row, itemContainer, false);
                                    String name = itemSnap.child("name").getValue(String.class);
                                    Long qty = itemSnap.child("quantity").getValue(Long.class);
                                    Double price = itemSnap.child("price").getValue(Double.class);
                                    if (name != null && qty != null && price != null) {
                                        ((TextView) row.findViewById(R.id.tvProductName)).setText(name);
                                        ((TextView) row.findViewById(R.id.tvQty)).setText(String.valueOf(qty));
                                        ((TextView) row.findViewById(R.id.tvPrice)).setText(String.format("₱%.2f", price));
                                        itemContainer.addView(row);
                                    }
                                }
                            }

                            // Packages
                            DataSnapshot pkgNode = orderItems.child("package");
                            if (pkgNode.exists()) {
                                /// Case A: single object (your current structure)
                                if (pkgNode.hasChild("name")) {
                                    String id    = pkgNode.getKey();
                                    String name  = pkgNode.child("name").getValue(String.class);
                                    Long   qty   = pkgNode.child("quantity").getValue(Long.class);
                                    Double price = pkgNode.child("price").getValue(Double.class);
                                    if (name != null && qty != null && price != null) {
                                        View row = inflater.inflate(R.layout.item_row, itemContainer, false);
                                        row.setTag(id);
                                        ((TextView) row.findViewById(R.id.tvProductName)).setText(name);
                                        ((TextView) row.findViewById(R.id.tvQty)).setText(String.valueOf(qty));
                                        ((TextView) row.findViewById(R.id.tvPrice))
                                                .setText(String.format("₱%.2f", price));
                                        itemContainer.addView(row);
                                    }
                                }
                                /// Case B: list of packages under push-IDs
                                else {
                                    for (DataSnapshot itemSnap : pkgNode.getChildren()) {
                                        String id    = itemSnap.getKey();
                                        String name  = itemSnap.child("name").getValue(String.class);
                                        Long   qty   = itemSnap.child("quantity").getValue(Long.class);
                                        Double price = itemSnap.child("price").getValue(Double.class);
                                        if (name != null && qty != null && price != null) {
                                            View row = inflater.inflate(R.layout.item_row, itemContainer, false);
                                            row.setTag(id);
                                            ((TextView) row.findViewById(R.id.tvProductName)).setText(name);
                                            ((TextView) row.findViewById(R.id.tvQty)).setText(String.valueOf(qty));
                                            ((TextView) row.findViewById(R.id.tvPrice))
                                                    .setText(String.format("₱%.2f", price));
                                            itemContainer.addView(row);
                                        }
                                    }
                                }
                            }


                            /// Show the dialog
                            AlertDialog dialog = new AlertDialog.Builder(context)
                                    .setView(dialogV)
                                    .setCancelable(false)
                                    .create();

                            dialog.show();

                            if (dialog.getWindow() != null) {
                                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.WHITE));
                            }

                            Button btnDownload = dialogV.findViewById(R.id.btnDownload);
                            Button btnCancel = dialogV.findViewById(R.id.btnCancel);

                            btnDownload.setOnClickListener(v -> {
                                // Hide the buttons before rendering to PDF
                                btnDownload.setVisibility(View.GONE);
                                btnCancel.setVisibility(View.GONE);

                                // Use dialogV instead of contentView
                                dialogV.post(() -> {
                                    downloadAsPDF(dialogV, dialog, success -> {
                                        if (!success) {
                                            // Show the buttons again if the download failed
                                            btnDownload.setVisibility(View.VISIBLE);
                                            btnCancel.setVisibility(View.VISIBLE);
                                        }
                                        // No need to show buttons again if successful since dialog is dismissed
                                    });
                                });
                            });


                            btnCancel.setOnClickListener(v -> dialog.dismiss());

                            break; /// Only show the first booking
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(context, "Booking load failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(context, "Profile load failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * Renders `contentView` to a PDF, writes it into
     * getExternalFilesDir(DIRECTORY_DOWNLOADS).
     *
     * @param contentView the inflated booking‐details layout
     * @param dialogToDismiss the dialog to dismiss after saving
     * @param onDone callback that receives true if saved successfully
     */

        public void downloadAsPDF(View contentView, AlertDialog dialogToDismiss, Consumer<Boolean> onDone) {
        // 0) Force software layer so everything (images, shapes) draws into the Canvas
        contentView.setLayerType(View.LAYER_TYPE_SOFTWARE, null); // <<<====

        // 1) Measure/layout with actual width (use screen width or parent width)
        int targetWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
        int specW = View.MeasureSpec.makeMeasureSpec(targetWidth, View.MeasureSpec.EXACTLY); // <<<====
        int specH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        contentView.measure(specW, specH);
        contentView.layout(0, 0, contentView.getMeasuredWidth(), contentView.getMeasuredHeight());

        // 2) Render to Bitmap
        Bitmap bitmap = Bitmap.createBitmap(
                contentView.getMeasuredWidth(),
                contentView.getMeasuredHeight(),
                Bitmap.Config.ARGB_8888
        );
        Canvas canvas = new Canvas(bitmap);
        contentView.draw(canvas);

        // 3) Build PDF, splitting into pages if needed
        PdfDocument pdf = new PdfDocument();
        int pageHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
        int pageCount = (int) Math.ceil((float) bitmap.getHeight() / pageHeight);

        for (int i = 0; i < pageCount; i++) {
            int offsetY = i * pageHeight;
            PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
                    bitmap.getWidth(),
                    Math.min(pageHeight, bitmap.getHeight() - offsetY),
                    i + 1
            ).create();
            PdfDocument.Page page = pdf.startPage(pageInfo);

            // draw the slice of the bitmap onto this page
            page.getCanvas().drawBitmap(
                    bitmap,
                    new Rect(0, offsetY, bitmap.getWidth(), offsetY + pageInfo.getPageHeight()),
                    new Rect(0, 0, pageInfo.getPageWidth(), pageInfo.getPageHeight()),
                    null
            );
            pdf.finishPage(page);
        }

        /// --- 4) save to the public Downloads directory
        File downloads;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For Android 10+ use MediaStore
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, "booking_" + System.currentTimeMillis() + ".pdf");
            values.put(MediaStore.Downloads.MIME_TYPE, "application/pdf");
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

            Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                Toast.makeText(this, "Cannot create file", Toast.LENGTH_SHORT).show();
                onDone.accept(false);
                pdf.close();
                return;
            }

            try (OutputStream os = getContentResolver().openOutputStream(uri)) {
                pdf.writeTo(os);
                Toast.makeText(this, "Saved to Downloads", Toast.LENGTH_SHORT).show();
                if (dialogToDismiss != null && dialogToDismiss.isShowing()) {
                    dialogToDismiss.dismiss();
                }
                onDone.accept(true);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                onDone.accept(false);
            } finally {
                pdf.close();
            }
        } else {
            // Below Android 10: direct file write
            downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            if (!downloads.exists() && !downloads.mkdirs()) {
                Toast.makeText(this, "Cannot access Downloads", Toast.LENGTH_SHORT).show();
                onDone.accept(false);
                pdf.close();
                return;
            }

            File outFile = new File(downloads, "booking_" + System.currentTimeMillis() + ".pdf");
            try (FileOutputStream fos = new FileOutputStream(outFile)) {
                pdf.writeTo(fos);
                Toast.makeText(this, "Saved to Downloads: " + outFile.getName(), Toast.LENGTH_SHORT).show();
                if (dialogToDismiss != null && dialogToDismiss.isShowing()) {
                    dialogToDismiss.dismiss();
                }
                onDone.accept(true);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Save failed", Toast.LENGTH_SHORT).show();
                onDone.accept(false);
            } finally {
                pdf.close();
            }
        }
    }

}



//    public void downloadAsPDF(View contentView, AlertDialog dialogToDismiss, Consumer<Boolean> onDone) {
//        // --- 1) ensure the view has a size by measuring+laying out
//        int specW = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
//        int specH = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
//        contentView.measure(specW, specH);
//        contentView.layout(0, 0, contentView.getMeasuredWidth(), contentView.getMeasuredHeight());
//
//        // --- 2) render the view to a bitmap
//        Bitmap bitmap = Bitmap.createBitmap(
//                contentView.getMeasuredWidth(),
//                contentView.getMeasuredHeight(),
//                Bitmap.Config.ARGB_8888
//        );
//        Canvas canvas = new Canvas(bitmap);
//        contentView.draw(canvas);
//
//        // --- 3) build the PDF
//        PdfDocument pdf = new PdfDocument();
//        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(
//                bitmap.getWidth(), bitmap.getHeight(), 1
//        ).create();
//        PdfDocument.Page page = pdf.startPage(pageInfo);
//        page.getCanvas().drawBitmap(bitmap, 0, 0, null);
//        pdf.finishPage(page);

///Fix Current
//package com.example.resort;
//import static android.app.Service.START_STICKY;
//import static android.content.ContentValues.TAG;
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
//    private Button payNowButton, cancelButton, refreshButton;
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
//        refreshButton = findViewById(R.id.refresh);
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
//        ///Refresh Data
//
//        refreshButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Log.d("Refresh", "Reloading data...");
//                refreshData(); /// Call your listener functions again
//            }
//        });
//
//
//
//        ///Pay Now Button
//        payNowButton.setOnClickListener(v -> {
//            if (progress < 2) {
//                Toast.makeText(BookingStatus.this, "No booking or step not done, cannot proceed.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            Intent paymentIntent = new Intent(BookingStatus.this, Payment.class);
//            startActivity(paymentIntent);
//        });
//        backButton.setOnClickListener(v -> onBackPressed());
//        cancelButton.setOnClickListener(view -> cancelBooking());
//        /// Set up Firebase listeners for dynamic updates.
//        listenForPaymentMethodStatus();
//        listenForMyBooking();
//        listenForApproval();
//        listenForPaymentTransactionApproval();
//        FinalForApproval();
//    }
//
//
//    ///Refresh Data
//    private void refreshData() {
//        forceProcessDeclinesAndRefunds();
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
//    ///Force move if decline and refund is stock up
//    private void forceProcessDeclinesAndRefunds() {
//        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
//        if (u == null) return;
//        DatabaseReference ref = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(u.getUid())
//                .child("MyBooking");
//
//        ref.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snap) {
//                for (DataSnapshot b : snap.getChildren()) {
//                    String review = b.child("bookingReview")
//                            .child("statusReview")
//                            .getValue(String.class);
//                    String pay    = b.child("paymentTransaction")
//                            .child("paymentStatus")
//                            .getValue(String.class);
//
//                    if ("Declined".equalsIgnoreCase(review)
//                            || "Refund".equalsIgnoreCase(pay)) {
//                        moveAllBookingsToHistory();
//                        clearBookingMessageUI();
//                        clearBookingPreferences();
//                        break;
//                    }
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError e) { }
//        });
//    }
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
//        clearNotification(100);
//        clearNotification(101);
//        clearNotification(102);
//        clearNotification(103);
//        clearNotification(104);
//
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
//
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
//
//    }
//
//    private void showDecline() {
//        /// Update UI for decline branch
//        messageFramedot2.setVisibility(View.VISIBLE);
//        messageText2.setVisibility(View.VISIBLE);
//        String currentTime = getCurrentTime();
//        String msg = "&quot;Sorry, your booking has been declined by the admin. Click the refresh now!&quot;<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        messageText2.setText(Html.fromHtml(msg + redTime));
//        sendNotificationToFirebase(messageText2.getText().toString(), "BookingDecline");
//
//        ///moveAllBookingsToHistory();
//        ///clearBookingMessageUI();
//        ///clearBookingPreferences();
//
//    }
//
//    private void showRefund() {
//        /// Update UI for refund branch.
//        messageFramedot4.setVisibility(View.VISIBLE);
//        messageText4.setVisibility(View.VISIBLE);
//        String currentTime = getCurrentTime();
//        String msg = "&quot;Your payment has been reversed and refunded by the admin. Click the refresh now!&quot;<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        messageText4.setText(Html.fromHtml(msg + redTime));
//        sendNotificationToFirebase(messageText4.getText().toString(), "PaymentRefunded");
//
//        ///moveAllBookingsToHistory();
//        ///clearBookingMessageUI();
//        ///clearBookingPreferences();
//
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
//                                        bookingPayProcessed = false;
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
//        // Use an array to hold the Runnable reference
//        final Runnable[] pollTask = new Runnable[1];
//        pollTask[0] = new Runnable() {
//            @Override
//            public void run() {
//                bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        boolean shouldStopPolling = false;
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
//                                    /// Stop polling
//                                    shouldStopPolling = true;
//                                    break;
//                                } else if (statusReview.equalsIgnoreCase("Declined") && !declineProcessed) {
//                                    declineProcessed = true;
//                                    showDecline(); ///Message
//                                    DataSnapshot paymentSnap = bookingSnapshot.child("paymentTransaction");
//                                    if (paymentSnap.exists()) {
//                                        String currentPaymentStatus = paymentSnap.child("paymentStatus")
//                                                .getValue(String.class);
//                                        if (currentPaymentStatus == null ||
//                                                !currentPaymentStatus.equalsIgnoreCase("Declined")) {
//                                            // Update paymentStatus to "Declined"
//                                            bookingSnapshot.child("paymentTransaction")
//                                                    .getRef()
//                                                    .child("paymentStatus")
//                                                    .setValue("Declined");
//
//                                            // Append PaymentDate with the current formatted date
//                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
//                                            String paymentDate = sdf.format(new Date());
//                                            bookingSnapshot.child("paymentTransaction")
//                                                    .getRef()
//                                                    .child("PaymentDate")
//                                                    .setValue(paymentDate);
//
//
//
//                                            /// Stop the foreground service
//                                            Intent stopIntent = new Intent(BookingStatus.this, BookingStatusService.class);
//                                            stopService(stopIntent);
//                                            Log.d("BookingStatus", "Foreground service stopped.");
//
//                                            // Delete the MyReview node after processing decline
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
//                                    // Stop polling after processing decline
//                                    shouldStopPolling = true;
//                                    break;
//                                }
//                            }
//                        }
//                        // Only reschedule if no update was processed
//                        if (!shouldStopPolling) {
//                            handler.postDelayed(pollTask[0], 1000);
//                        }
//                    }
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e("BookingCheck", "Error reading MyBooking data", error.toException());
//                    }
//                });
//            }
//        };
//
//        /// Start polling every second
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
//                                    showRefund(); ///Message
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
//
//                                            /// Stop the foreground service
//                                            Intent stopIntent = new Intent(BookingStatus.this, BookingStatusService.class);
//                                            stopService(stopIntent);
//                                            Log.d("BookingStatus", "Foreground service stopped.");
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
//                                showDot5Message();
//                                stopPolling();
//
//                                /// Stop the foreground service
//                                Intent stopIntent = new Intent(BookingStatus.this, BookingStatusService.class);
//                                stopService(stopIntent);
//                                Log.d("BookingStatus", "Foreground service stopped.");
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
//                                }, 1000);
//                                break; // Exit loop after processing one booking.
//                            }
//                            // if Failed, stop polling and execute clear functions.
//                            else if (finalStatus != null && finalStatus.equalsIgnoreCase("Failed") && !finalProcessed) {
//                                finalProcessed = true;
//                                progress = 0;
//                                updateDots();
//                                stopPolling();
//                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                    clearBookingMessageUI();
//                                    clearBookingPreferences();
//                                    moveAllBookingsToHistory();
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
//                /// Schedule next poll every second.
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
//        /// Check if a notification already exists and whether it's been read.
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
//
//    private void clearNotification(int notificationId) {
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        if (notificationManager != null) {
//            notificationManager.cancel(notificationId); /// Removes only the specified notification
//            ///notificationManager.cancelAll(); // Clears all active notifications
//        }
//    }
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
//        prefs.edit().clear().apply();
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
