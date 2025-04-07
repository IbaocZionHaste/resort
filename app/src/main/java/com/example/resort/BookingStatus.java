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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    // Message containers for dots 1 to 5.
    private FrameLayout messageFramedot1, messageFramedot2, messageFramedot3, messageFramedot4, messageFramedot5;
    private TextView messageText, messageText2, paymentMessageText, messageText4, messageText5;
    // SharedPreferences for persisting booking state.
    private SharedPreferences prefs;
    // Flags for one-time processing.
    private boolean approvalProcessed = false;
    private boolean paymentApprovedProcessed = false;
    private boolean finalProcessed = false;

    // For decline handling.
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




        // Adjust layout for system insets.
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

        // Restore persisted booking state.
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

        // Set up Firebase listeners for dynamic updates.
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
                String cancellationMessage = "Booking has been Cancelled.<br>";
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
                        cancelData.put("cancelledBy", bookingCancelledBy);
                        cancelData.put("userId", userId);
                        cancelData.put("cancelTime", cancelTime);
                        cancelBookingRef.push().setValue(cancelData);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("BookingStatus", "User data fetch failed: " + error.getMessage());
                    }
                });

                // Reset state after a delay.
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
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



    /// Message view if the booking is submit Not use
    private void showSubmissionMessage() {
        messageFramedot1.setVisibility(View.VISIBLE);
        messageText.setVisibility(View.VISIBLE);
        String currentTime = getCurrentTime();
        String submissionMessage = "Booking has been Submitted. Please wait for it to be reviewed by admin.<br>";
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime );
        messageText.setText(Html.fromHtml(submissionMessage + redTime));
        sendNotificationToFirebase(messageText.getText().toString(), "dot1");
        clearNotification(3);
        clearNotification(4);
        clearNotification(5);
    }

    ///  Message view if the booking is payment Not use
    private void showPaymentSubmittedMessage() {
            messageFramedot3.setVisibility(View.VISIBLE);
            paymentMessageText.setVisibility(View.VISIBLE);
            String currentTime = getCurrentTime();
            String paymentMessage = "Payment has been Submitted. Please wait for admin review.<br>";
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
        String approvalMessage = "Booking has been Reviewed. Please proceed to the payment by clicking the Pay Now button.<br>";
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
        String msg = "Payment transaction has been Approved. Please wait for final approval.<br>";
        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
        String fullMessage = msg + redTime;
        messageText4.setText(Html.fromHtml(fullMessage));
        prefs.edit().putString("bookingStatus", "paymentApproved").apply();
        sendNotificationToFirebase(messageText4.getText().toString(), "dot4");
    }


    private void showDot5Message() {
        messageFramedot5.setVisibility(View.VISIBLE);
        messageText5.setVisibility(View.VISIBLE);
        String msg = "Congratulations! Your Booking has been Approved.<br>";
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
        // Use an array to hold the Runnable reference
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
                                    showLocalNotification("Booking has been Reviewed",
                                            "Please proceed to the payment by clicking the Pay Now button!", 1);
                                    // Stop polling
                                    shouldStopPolling = true;
                                    break;
                                } else if (statusReview.equalsIgnoreCase("Declined") && !declineProcessed) {
                                    declineProcessed = true;
                                    DataSnapshot paymentSnap = bookingSnapshot.child("paymentTransaction");
                                    if (paymentSnap.exists()) {
                                        String currentPaymentStatus = paymentSnap.child("paymentStatus")
                                                .getValue(String.class);
                                        if (currentPaymentStatus == null ||
                                                !currentPaymentStatus.equalsIgnoreCase("Declined")) {
                                            // Update paymentStatus to "Declined"
                                            bookingSnapshot.child("paymentTransaction")
                                                    .getRef()
                                                    .child("paymentStatus")
                                                    .setValue("Declined");

                                            // Append PaymentDate with the current formatted date
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault());
                                            String paymentDate = sdf.format(new Date());
                                            bookingSnapshot.child("paymentTransaction")
                                                    .getRef()
                                                    .child("PaymentDate")
                                                    .setValue(paymentDate);

                                            // Update UI for decline branch
                                            messageFramedot2.setVisibility(View.VISIBLE);
                                            messageText2.setVisibility(View.VISIBLE);
                                            String currentTime = getCurrentTime();
                                            String msg = "Sorry, your booking has been declined by the admin. <br>";
                                            String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
                                            messageText2.setText(Html.fromHtml(msg + redTime));
                                            showLocalNotification("Booking Declined!",
                                                    "Sorry, your booking has been declined by the admin.", 4);
                                            sendNotificationToFirebase(messageText2.getText().toString(), "BookingDecline");
                                            moveAllBookingsToHistory();
                                            clearBookingMessageUI();
                                            clearBookingPreferences();

                                            // Delete the MyReview node after processing decline
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
                                    // Stop polling after processing decline
                                    shouldStopPolling = true;
                                    break;
                                }
                            }
                        }
                        // Only reschedule if no update was processed
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
                                    showLocalNotification("Payment Approved",
                                            "Payment has been approved. Awaiting final approval.", 2);
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
                                            String msg = "Your payment has been reversed and refunded by the admin. <br>";
                                            String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
                                            messageText2.setText(Html.fromHtml(msg + redTime));
                                            showLocalNotification("Payment Declined!",
                                                    "Your payment has been reversed and refunded by the admin.", 5);
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

                                clearNotification(1);
                                clearNotification(2);
                            }, 1000);
                            return;
                        }
                        // Process each booking entry.
                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                            String finalStatus = bookingSnapshot.child("paymentTransaction")
                                    .child("finalStatus")
                                    .getValue(String.class);
                            // if Approved, stop polling and execute clear functions.
                            if (finalStatus != null && finalStatus.equalsIgnoreCase("Approved") && !finalProcessed) {
                                finalProcessed = true;
                                progress = Math.max(progress, 5);
                                updateDots();
                                showLocalNotification("Congratulations!", "Your booking has been finally approved.", 3);
                                showDot5Message();
                                stopPolling();



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
                                                    reviewData.put("statusReview", "Approved");

                                                    // Generate a unique key for each review
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

                                            // After copying, delete the original MyReview data
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



                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    clearBookingMessageUI();
                                    clearBookingPreferences();
                                    moveAllBookingsToHistory();
                                    clearNotification(1);
                                    clearNotification(2);
                                }, 1000);
                                break; // Exit loop after processing one booking.
                            }
                            // if Failed, stop polling and execute clear functions.
                            else if (finalStatus != null && finalStatus.equalsIgnoreCase("Failed") && !finalProcessed) {
                                finalProcessed = true;
                                progress = 0;
                                updateDots();
                                showLocalNotification("Booking Failed", "Your booking has failed. Please try again.", 6);
                                stopPolling();
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    clearBookingMessageUI();
                                    clearBookingPreferences();
                                    moveAllBookingsToHistory();
                                    clearNotification(1);
                                    clearNotification(2);
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
                // Schedule next poll every second.
                pollingHandler.postDelayed(this, 1000);
            }
        };
        pollingHandler.post(pollTask);
    }

    // Function to stop the polling
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


    ///Show device notification
    @SuppressLint("ObsoleteSdkInt")
    private void showLocalNotification(String title, String message, int notificationId) {
        // Get the NotificationManager service.
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Check if a notification with the same ID is already active to avoid duplicate notifications.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
            for (StatusBarNotification sbn : activeNotifications) {
                if (sbn.getId() == notificationId) {
                    // Already active; do not post a duplicate.
                    return;
                }
            }
        }

        // For Android Oreo and above, create a notification channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "booking_channel",
                    "Booking Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Channel for booking notifications");
            channel.enableLights(true);
            channel.enableVibration(true);
            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                            .build());
            notificationManager.createNotificationChannel(channel);
        }

        // Build the notification.
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "booking_channel")
                .setSmallIcon(R.drawable.ic_profile_notification) // Ensure this icon exists.
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));

        // Post the notification.
        notificationManager.notify(notificationId, builder.build());
    }


    private void clearNotification(int notificationId) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(notificationId); // Removes only the specified notification
            //notificationManager.cancelAll(); // Clears all active notifications
        }
    }

    /**
     * Moves all booking data from "MyBooking" to "MyHistory".
     */
    private void moveAllBookingsToHistory() {
        if (bookingMoved) return;
        bookingMoved = true;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();
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
                    myHistoryRef.push().setValue(bookingData);
                }
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




///This is original
//    private void showSubmissionMessage() {
//        messageFramedot1.setVisibility(View.VISIBLE);
//        messageText.setVisibility(View.VISIBLE);
//        String storedTime = prefs.getString("submissionTime", "");
//        if (storedTime.isEmpty()) {
//            storedTime = getCurrentTime();
//            prefs.edit().putString("submissionTime", storedTime).apply();
//        }
//        String submissionMessage = "Booking has been Submitted. Please wait for it to be reviewed by admin.<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", storedTime);
//        messageText.setText(Html.fromHtml(submissionMessage + redTime));
//        sendNotificationToFirebase(messageText.getText().toString(), "dot1");
//        clearNotification(3);
//        clearNotification(4);
//        clearNotification(5);
//
//    }



///Pogress refs for payment
//
//        if (prefs.contains("paymentSubmitted") && prefs.getBoolean("paymentSubmitted", false)) {
//            progress = Math.max(progress, 3);
//            updateDots();
//            String paymentTime = prefs.getString("paymentSubmittedTime", "");
//            showPaymentSubmittedMessage(paymentTime);
//        }


///This the payment progress once submit the payment
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PAYMENT_REQUEST_CODE && resultCode == RESULT_OK) {
//            String paymentTime = data.getStringExtra("paymentSubmittedTime");
//            progress = 3;
//            updateDots();
//            showPaymentSubmittedMessage(paymentTime);
//            prefs.edit().putBoolean("paymentSubmitted", true)
//                    .putString("paymentSubmittedTime", paymentTime)
//                    .apply();
//        }
//    }



///Use the already defined currentUser
//        currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            userId = currentUser.getUid();
//            SharedPreferences prefs = getSharedPreferences("BookingPref_" + userId, MODE_PRIVATE);
//
//            // Try retrieving bookingId from the Intent.
//            bookingId = getIntent().getStringExtra("bookingId");
//            if (bookingId == null) {
//                // Fallback to SharedPreferences.
//                bookingId = prefs.getString("bookingId", null);
//            }
//
//            if (bookingId == null) {
//                Log.d("BookingStatus", "Booking not found!");
//            }
//        } else {
//            Toast.makeText(BookingStatus.this, "User not logged in", Toast.LENGTH_SHORT).show();
//        }


//        payNowButton.setOnClickListener(v -> {
//            if (progress < 2) {
//                Toast.makeText(BookingStatus.this, "No booking in the field, cannot proceeding.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            if (bookingId == null) {
//                Log.d("BookingStatus", "Booking not found!");
//                return;
//            }
//            Intent paymentIntent = new Intent(BookingStatus.this, Payment.class);
//            paymentIntent.putExtra("bookingId", bookingId);
//            startActivityForResult(paymentIntent, PAYMENT_REQUEST_CODE);
//        });
//        backButton.setOnClickListener(v -> onBackPressed());
//        cancelButton.setOnClickListener(view -> cancelBooking());
//    }



//
//        ///Booking Submitted
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(currentUser.getUid())
//                .child("MyBooking");
//
//        bookingRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                if (snapshot.exists()) {
//                    // Booking data exists, update progress and display submission message
//                    progress = 1;
//                    updateDots();
//                    showSubmissionMessage();
//
//                    // Retrieve booking review status.
//                    String statusReview = snapshot.child("bookingReview")
//                            .child("statusReview")
//                            .getValue(String.class);
//
//                    // If booking review is approved, upgrade progress to 2.
//                    if ("Approved".equalsIgnoreCase(statusReview)) {
//                        progress = 2;
//                        updateDots();
//                        showApprovalMessage();
//                    }
//                } else {
//                    /// No booking data exists, reset progress
//                    progress = 0;
//                    updateDots();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                // Handle possible errors
//                Log.e("BookingStatus", "Error reading booking data", error.toException());
//            }
//        });
//
//


///this code delete all current user book
// Function to clear booking data from Firebase for current user.
//    private void clearCurrentUserBookingData() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyBooking");
//
//        bookingRef.removeValue().addOnCompleteListener(task -> {
//            if (task.isSuccessful()) {
//                Log.d(TAG, "Booking data cleared successfully.");
//            } else {
//                Log.e(TAG, "Failed to clear booking data", task.getException());
//            }
//        });
//    }

///this code no polling stop.
//
//    //Final Approved by Admin
//    private void FinalForApproval() {
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
//                        // If no booking data exists, schedule a clear after 20 seconds.
//                        if (!snapshot.hasChildren()) {
//                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                clearBookingMessageUI();
//                                clearBookingPreferences();
//                                moveAllBookingsToHistory();
//                            }, 1000);
//                            return;
//                        }
//                        // Iterate through each booking
//                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                            String finalStatus = bookingSnapshot.child("paymentTransaction")
//                                    .child("finalStatus")
//                                    .getValue(String.class);
//                            // Process "Approved" bookings
//                            if (finalStatus != null && finalStatus.equalsIgnoreCase("Approved") && !finalProcessed) {
//                                finalProcessed = true;
//                                progress = Math.max(progress, 5);
//                                updateDots();
//                                showLocalNotification("Congratulations!", "Your booking has been finally approved.", 3);
//                                showDot5Message();
//                                // Clear UI and move booking after 20 seconds delay.
//                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                    clearBookingMessageUI();
//                                    clearBookingPreferences();
//                                    moveAllBookingsToHistory();
//                                }, 1000);
//                                // Once we process one booking, exit the loop.
//                                break;
//                            }
//                            //Process "Failed" bookings (or any other finalStatus you define)
//                            else if (finalStatus != null && finalStatus.equalsIgnoreCase("Failed") && !finalProcessed) {
//                                finalProcessed = true;
//                                // Optionally, adjust progress value for a failed state.
//                                progress = 0;
//                                updateDots();
//                                showLocalNotification("Booking Failed", "Your booking has failed. Please try again.", 6);
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
//                // Poll every second.
//                handler.postDelayed(this, 1000);
//            }
//        };
//        handler.post(pollTask);
//    }

// Declare these as class-level variables

///working but have many bug
//package com.example.resort;
//
//import static android.content.ContentValues.TAG;
//
//import android.annotation.SuppressLint;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.media.AudioAttributes;
//import android.media.RingtoneManager;
//import android.os.Build;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.preference.PreferenceManager;
//import android.service.notification.StatusBarNotification;
//import android.text.Html;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.FrameLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.app.NotificationCompat;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.*;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Map;
//
//public class BookingStatus extends AppCompatActivity {
//
//    // Progress tracker: 0 = no progress, 1 = booking submitted, 2 = reviewed, 3 = payment submitted,
//    // 4 = payment transaction approved, 5 = final approval.
//    private int progress = 0;
//    private String bookingId;  // Passed from BookingReceipt.
//
//    private boolean bookingMoved = false; // Prevents double data store in Firebase
//    private TextView dot1, dot2, dot3, dot4, dot5;
//    private View line1_2, line2_3, line3_4, line4_5;
//    private Button payNowButton, cancelButton;
//
//    // Message containers for dots 1 to 5.
//    private FrameLayout messageFramedot1, messageFramedot2, messageFramedot3, messageFramedot4, messageFramedot5;
//    private TextView messageText, messageText2, paymentMessageText, messageText4, messageText5;
//
//    // SharedPreferences for persisting booking state.
//    private SharedPreferences prefs;
//
//    // Flags for one-time processing.
//    private boolean approvalProcessed = false;
//    private boolean paymentApprovedProcessed = false;
//    private static final int PAYMENT_REQUEST_CODE = 1001;
//
//    //private boolean approvalProcessed = false;
//    //private boolean paymentApprovedProcessed = false;
//    private boolean finalProcessed = false;
//
//    private boolean declineProcessed = false;
//    private boolean paymentDeclineProcessed = false;
//
//    // Firebase listener references.
//    private ValueEventListener approvalListener;
//    private ValueEventListener paymentListener;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_booking_status);
//
//        // Get current user and set up user-specific SharedPreferences.
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            // Handle not-logged-in state (e.g., redirect to login)
//            finish();
//            return;
//        }
//        String userId = currentUser.getUid();
//        prefs = getSharedPreferences("BookingPrefs_" + userId,  Context.MODE_PRIVATE);
//
//        // If booking is submitted from previous activity, save state in SharedPreferences.
//        boolean bookingSubmittedIntent = getIntent().getBooleanExtra("bookingSubmitted", false);
//        if (bookingSubmittedIntent) {
//            prefs.edit().putBoolean("bookingSubmitted", true).apply();
//            progress = 1;
//            prefs.edit().putInt("bookingProgress", progress).apply();
//        }
//
//
//
//        // Adjust layout for system insets.
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
//                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
//                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
//                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
//            return insets;
//        });
//
//        // Initialize UI elements.
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
//        // Restore persisted booking state.
//        if (prefs.contains("bookingSubmitted") && prefs.getBoolean("bookingSubmitted", false)) {
//            progress = 1;
//            updateDots();
//            showSubmissionMessage();
//        }
//        if (prefs.contains("paymentSubmitted") && prefs.getBoolean("paymentSubmitted", false)) {
//            progress = Math.max(progress, 3);
//            updateDots();
//            String paymentTime = prefs.getString("paymentSubmittedTime", "");
//            showPaymentSubmittedMessage(paymentTime);
//        }
//
//        if (prefs.contains("paymentApproved") && prefs.getBoolean("paymentApproved", false)) {
//            progress = 4;
//            updateDots();
//            showDot4Message();
//        }
//
//        if (prefs.contains("reviewApproved") && prefs.getBoolean("reviewApproved", false)) {
//            progress = 2;
//            updateDots();
//            showApprovalMessage();
//        }
//
//        if (prefs.contains("finalApproved") && prefs.getBoolean("finalApproved", false)) {
//            progress = 5;
//            updateDots();
//            showDot5Message();
//        }
//
//        // Set up Firebase listeners for dynamic updates.
//        listenForApproval();
//        listenForPaymentTransactionApproval();
//        FinalForApproval();
//
//
//        // Use the already defined currentUser
//        currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            userId = currentUser.getUid();
//            SharedPreferences prefs = getSharedPreferences("BookingPref_" + userId,  Context.MODE_PRIVATE);
//
//            // Try retrieving bookingId from the Intent.
//            bookingId = getIntent().getStringExtra("bookingId");
//            if (bookingId == null) {
//                // Fallback to SharedPreferences.
//                bookingId = prefs.getString("bookingId", null);
//            }
//
//            if (bookingId == null) {
//                Toast.makeText(BookingStatus.this, "Booking not found!", Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            Toast.makeText(BookingStatus.this, "User not logged in", Toast.LENGTH_SHORT).show();
//        }
//
//        // Set up button actions.
//        payNowButton.setOnClickListener(v -> {
//            if (progress < 2) {
//                Toast.makeText(BookingStatus.this, "Please complete the previous step before proceeding.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            if (bookingId == null) {
//                Toast.makeText(BookingStatus.this, "Booking not found!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            Intent paymentIntent = new Intent(BookingStatus.this, Payment.class);
//            paymentIntent.putExtra("bookingId", bookingId);
//            startActivityForResult(paymentIntent, PAYMENT_REQUEST_CODE);
//        });
//
//
//        backButton.setOnClickListener(v -> onBackPressed());
//
//        // Set up the cancel button listener.
//        cancelButton.setOnClickListener(view -> cancelBooking());
//    }
//
//    // Update dots and lines based on current progress.
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
//        // Save updated progress.
//        prefs.edit().putInt("bookingProgress", progress).apply();
//    }
//
//    /**
//     * Cancels a booking by:
//     * - Appending a cancellation timestamp to each booking's review.
//     * - Moving data from "MyBooking" to "MyCancel".
//     * - Removing "MyBooking" and "MyReview" data.
//     * - Updating the UI with a cancellation message.
//     * - Disabling the cancel button.
//     */
//    private void cancelBooking() {
//        if (progress < 1) {
//            Toast.makeText(BookingStatus.this, "No booking in the field, cannot cancel.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//
//        // Get references to the relevant Firebase nodes.
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
//                // Set booking review status and payment transaction status to "Cancelled" for each booking.
//                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                    // Log and update booking review status.
//                    Log.d("BookingStatus", "Updating bookingReview/statusReview for booking key: " + bookingSnapshot.getKey());
//                    bookingSnapshot.getRef()
//                            .child("bookingReview")
//                            .child("statusReview")
//                            .setValue("Cancelled");
//
//                    // Check if paymentTransaction node exists
//                    if (bookingSnapshot.hasChild("paymentTransaction")) {
//                        Log.d("BookingStatus", "PaymentTransaction exists for booking key: " + bookingSnapshot.getKey() +
//                                " Value: " + bookingSnapshot.child("paymentTransaction").getValue());
//                        bookingSnapshot.getRef()
//                                .child("paymentTransaction")
//                                .child("paymentStatus")
//                                .setValue("Cancelled");
//                    } else {
//                        Log.d("BookingStatus", "PaymentTransaction node does NOT exist for booking key: " + bookingSnapshot.getKey());
//                    }
//                }
//
//                // Then proceed with moving the data to MyHistory.
//                myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot bookingSnapshot) {
//                        for (DataSnapshot child : bookingSnapshot.getChildren()) {
//                            Object bookingData = child.getValue();
//                            myHistoryRef.push().setValue(bookingData);
//                        }
//                        // Remove all bookings.
//                        myBookingRef.removeValue();
//
//                        // Remove the "MyReview" node.
//                        DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                                .getReference("users")
//                                .child(userId)
//                                .child("MyReview");
//                        myReviewRef.removeValue();
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                    }
//                });
//
//                // Display cancellation message.
//                String cancelTime = getCurrentTime();
//                String cancellationMessage = "Booking has been Cancelled.<br>";
//                String redTime = String.format("<font color='#FF0000'>%s</font>", cancelTime);
//                messageFramedot1.setVisibility(View.VISIBLE);
//                messageText.setVisibility(View.VISIBLE);
//                messageText.setText(Html.fromHtml(cancellationMessage + redTime));
//                sendNotificationToFirebase(messageText.getText().toString(), "cancel");
//
//
//
//                // Reset state after a delay.
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
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
//                // Additional error handling can be added here.
//            }
//        });
//
//    }
//
//    private void showSubmissionMessage() {
//        messageFramedot1.setVisibility(View.VISIBLE);
//        messageText.setVisibility(View.VISIBLE);
//        String storedTime = prefs.getString("submissionTime", "");
//        if (storedTime.isEmpty()) {
//            storedTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//            prefs.edit().putString("submissionTime", storedTime).apply();
//        }
//        String submissionMessage = "Booking has been Submitted. Please wait for it to be reviewed by admin.<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", storedTime);
//        messageText.setText(Html.fromHtml(submissionMessage + redTime));
//        sendNotificationToFirebase(messageText.getText().toString(), "dot1");
//    }
//
//    private void showApprovalMessage() {
//        messageFramedot2.setVisibility(View.VISIBLE);
//        messageText2.setVisibility(View.VISIBLE);
//        if (progress < 2) {
//            progress = 2;
//            updateDots();
//        }
//        String currentTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//        String approvalMessage = "Booking has been Reviewed. Please proceed to the payment by clicking the Pay Now button.<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        messageText2.setText(Html.fromHtml(approvalMessage + redTime));
//        sendNotificationToFirebase(messageText2.getText().toString(), "dot2");
//        // Disable the cancel button.
//        cancelButton.setEnabled(false);
//        cancelButton.setClickable(false);
//        cancelButton.setAlpha(0.5f);
//    }
//
//    private void showPaymentSubmittedMessage(String submissionTime) {
//        messageFramedot3.setVisibility(View.VISIBLE);
//        paymentMessageText.setVisibility(View.VISIBLE);
//        String paymentMessage = "Payment has been Submitted. Please wait for admin review.<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", submissionTime);
//        paymentMessageText.setText(Html.fromHtml(paymentMessage + redTime));
//        // Disable the pay now button.
//        payNowButton.setEnabled(false);
//        payNowButton.setClickable(false);
//        payNowButton.setAlpha(0.5f);
//        // Disable the cancel button as well.
//        cancelButton.setEnabled(false);
//        cancelButton.setClickable(false);
//        cancelButton.setAlpha(0.5f);
//        sendNotificationToFirebase(paymentMessageText.getText().toString(), "dot3");
//    }
//
//    private void showDot4Message() {
//        messageFramedot4.setVisibility(View.VISIBLE);
//        messageText4.setVisibility(View.VISIBLE);
//        String currentTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//        String msg = "Payment transaction has been Approved. Please wait for final approval.<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        messageText4.setText(Html.fromHtml(msg + redTime));
//        sendNotificationToFirebase(messageText4.getText().toString(), "dot4");
//
//    }
//
//    private void showDot5Message() {
//        messageFramedot5.setVisibility(View.VISIBLE);
//        messageText5.setVisibility(View.VISIBLE);
//        String msg = "Congratulations! Your Booking has been Approved.<br>";
//        String currentTime = getCurrentTime();
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        String fullMessage = msg + redTime;
//        messageText5.setText(Html.fromHtml(fullMessage));
//        sendNotificationToFirebase(messageText5.getText().toString(), "dot5");
//    }
//
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PAYMENT_REQUEST_CODE && resultCode == RESULT_OK) {
//            String paymentTime = data.getStringExtra("paymentSubmittedTime");
//            progress = 3;
//            updateDots();
//            showPaymentSubmittedMessage(paymentTime);
//            prefs.edit().putBoolean("paymentSubmitted", true)
//                    .putString("paymentSubmittedTime", paymentTime)
//                    .apply();
//        }
//    }
//
//    //Booking Review Admin
//    private void listenForApproval() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyBooking");
//
//        // Create a Handler to schedule polling every second
//        final Handler handler = new Handler(Looper.getMainLooper());
//        Runnable pollTask = new Runnable() {
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
//                                    showApprovalMessage();
//                                    showLocalNotification("Booking has been Reviewed", "Please proceed to the payment by clicking the Pay Now button!", 1);
//
//                                    // Optionally, you can break if no further processing is needed
//                                } else if (statusReview.equalsIgnoreCase("Declined") && !declineProcessed) {
//                                    declineProcessed = true;
//                                    // Immediately process the decline branch—no delay
//                                    DataSnapshot paymentSnap = bookingSnapshot.child("paymentTransaction");
//                                    if (paymentSnap.exists()) {
//                                        String currentPaymentStatus = paymentSnap.child("paymentStatus").getValue(String.class);
//                                        if (currentPaymentStatus == null || !currentPaymentStatus.equalsIgnoreCase("Declined")) {
//                                            // Update paymentStatus to "Declined"
//                                            bookingSnapshot.child("paymentTransaction")
//                                                    .getRef()
//                                                    .child("paymentStatus")
//                                                    .setValue("Declined");
//
//                                            // For the decline branch
//                                            messageFramedot2.setVisibility(View.VISIBLE);
//                                            messageText2.setVisibility(View.VISIBLE);
//                                            String currentTime = getCurrentTime();
//                                            String msg = "Your booking has been declined by the admin. <br>";
//                                            String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//                                            messageText2.setText(Html.fromHtml(msg + redTime));
//                                            showLocalNotification("Booking Declined!", "Your booking has been declined by the admin.", 4);
//                                            sendNotificationToFirebase(messageText2.getText().toString(), "BookingDecline");
//                                            moveAllBookingsToHistory();
//                                            clearBookingMessageUI();
//                                            clearBookingPreferences();
//                                        }
//                                    }
//                                    // Exit after processing decline to avoid further iterations
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
//                // Schedule the next poll after 1 second (1000 milliseconds)
//                handler.postDelayed(this, 1000);
//            }
//        };
//
//        // Start polling every second
//        handler.post(pollTask);
//    }

//
//    //Payment Admin Approval
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
//                                // When paymentStatus is Approved, update dot4 (progress = 4)
//                                if (paymentStatus != null
//                                        && paymentStatus.equalsIgnoreCase("Approved")
//                                        && !paymentApprovedProcessed) {
//                                    paymentApprovedProcessed = true;
//                                    progress = Math.max(progress, 4);
//                                    updateDots();
//                                    showDot4Message();
//                                    showLocalNotification("Payment Approved", "Payment has been approved. Awaiting final approval.", 2);
//                                }
//                                // Handle payment declined if needed.
//                                else if (paymentStatus != null
//                                        && paymentStatus.equalsIgnoreCase("Declined")
//                                        && !paymentDeclineProcessed) {
//                                    paymentDeclineProcessed = true;
//                                    // Immediately process the decline branch—no delay
//                                    DataSnapshot paymentSnap = bookingSnapshot.child("bookingReview");
//                                    if (paymentSnap.exists()) {
//                                        String currentPaymentStatus = paymentSnap.child("statusReview").getValue(String.class);
//                                        if (currentPaymentStatus == null || !currentPaymentStatus.equalsIgnoreCase("Declined")) {
//                                            // Update statusReview to "Declined"
//                                            bookingSnapshot.child("bookingReview")
//                                                    .getRef()
//                                                    .child("statusReview")
//                                                    .setValue("Declined");
//
//
//                                            // For the decline branch
//                                            messageFramedot2.setVisibility(View.VISIBLE);
//                                            messageText2.setVisibility(View.VISIBLE);
//                                            String currentTime = getCurrentTime();
//                                            String msg = "Your payment has been declined by the admin. <br>";
//                                            String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//                                            messageText2.setText(Html.fromHtml(msg + redTime));
//                                            showLocalNotification("Payment Declined!", "Your Payment has been declined by the admin.", 5);
//                                            sendNotificationToFirebase(messageText2.getText().toString(), "PaymentDecline");
//                                            moveAllBookingsToHistory();
//                                            clearBookingMessageUI();
//                                            clearBookingPreferences();
//                                        }
//                                    }
//                                    // Exit after processing decline to avoid further iterations
//                                    return;
//                                }
//                            }
//                        }
//                    }
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) { }
//                });
//                handler.postDelayed(this, 1000); // Poll every second.
//            }
//        };
//        handler.post(pollTask);
//    }
//
//
//
//    //Final Approved by Admin
//    private void FinalForApproval() {
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
//                        // If no booking data exists, schedule a clear after 20 seconds.
//                        if (!snapshot.hasChildren()) {
//                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                clearBookingMessageUI();
//                                clearBookingPreferences();
//                                moveAllBookingsToHistory();
//                            }, 20000);
//                            return;
//                        }
//                        // Iterate through each booking
//                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                            String finalStatus = bookingSnapshot.child("paymentTransaction")
//                                    .child("finalStatus")
//                                    .getValue(String.class);
//                            // Process "Approved" bookings
//                            if (finalStatus != null && finalStatus.equalsIgnoreCase("Approved") && !finalProcessed) {
//                                finalProcessed = true;
//                                progress = Math.max(progress, 5);
//                                updateDots();
//                                showLocalNotification("Congratulations!", "Your booking has been finally approved.", 3);
//                                showDot5Message();
//                                // Clear UI and move booking after 20 seconds delay.
//                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                    clearBookingMessageUI();
//                                    clearBookingPreferences();
//                                    moveAllBookingsToHistory();
//                                }, 20000);
//                                // Once we process one booking, exit the loop.
//                                break;
//                            }
//                            //Process "Failed" bookings (or any other finalStatus you define)
//                            else if (finalStatus != null && finalStatus.equalsIgnoreCase("Failed") && !finalProcessed) {
//                                finalProcessed = true;
//                                // Optionally, adjust progress value for a failed state.
//                                progress = 0;
//                                updateDots();
//                                showLocalNotification("Booking Failed", "Your booking has failed. Please try again.", 6);
//                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                    clearBookingMessageUI();
//                                    clearBookingPreferences();
//                                    moveAllBookingsToHistory();
//                                }, 20000);
//                                break;
//                            }
//                        }
//                    }
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e(TAG, "Error reading booking data", error.toException());
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
//
//
//    /**
//     * Moves all booking data from "MyBooking" to "MyHistory".
//     */
//    private void moveAllBookingsToHistory() {
//        if (bookingMoved) return;
//        bookingMoved = true;
//
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
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
//                    myHistoryRef.push().setValue(bookingData);
//                }
//                myBookingRef.removeValue();
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(BookingStatus.this, "Failed to move bookings to history.", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//
//    ///Notification show in the device
//    @SuppressLint("ObsoleteSdkInt")
//    private void showLocalNotification(String title, String message, int notificationId) {
//        // Get the NotificationManager service.
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // Check if a notification with the same ID is already active to avoid duplicate notifications.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
//            for (StatusBarNotification sbn : activeNotifications) {
//                if (sbn.getId() == notificationId) {
//                    // Already active; do not post a duplicate.
//                    return;
//                }
//            }
//        }
//
//        // For Android Oreo and above, create a notification channel.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    "booking_channel",
//                    "Booking Notifications",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            channel.setDescription("Channel for booking notifications");
//            channel.enableLights(true);
//            channel.enableVibration(true);
//            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
//                    new AudioAttributes.Builder()
//                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                            .build());
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // Build the notification.
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "booking_channel")
//                .setSmallIcon(R.drawable.ic_profile_notification) // Ensure this icon exists.
//                .setContentTitle(title)
//                .setContentText(message)
//                .setAutoCancel(true)
//                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
//
//        // Post the notification.
//        notificationManager.notify(notificationId, builder.build());
//    }
//
//
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
//                .remove("paymentApprovedTime")
//                .remove("submissionTime")
//                .remove("bookingComplete")
//                .remove("bookingProgress")
//                .remove("reviewApproved")
//                .remove("finalApproved")
//                .remove("bookingId")
//                .apply();
//    }
//
//    // Get the current time.
//    private String getCurrentTime() {
//        return new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//    }
//}













//    @SuppressLint("ObsoleteSdkInt")
//    private void showLocalNotification(String title, String message, int notificationId) {
//        // Get the NotificationManager service.
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // For Android Oreo and above, create a notification channel.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    "booking_channel",
//                    "Booking Notifications",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            channel.setDescription("Channel for booking notifications");
//            channel.enableLights(true);
//            channel.enableVibration(true);
//            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
//                    new AudioAttributes.Builder()
//                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                            .build());
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // Build the notification.
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "booking_channel")
//                .setSmallIcon(R.drawable.ic_profile_notification) // Ensure this icon exists.
//                .setContentTitle(title)
//                .setContentText(message)
//                .setAutoCancel(true)
//                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
//
//        // Post the notification.
//        notificationManager.notify(notificationId, builder.build());
//    }
//private void showApprovalMessage() {
//    messageFramedot2.setVisibility(View.VISIBLE);
//    messageText2.setVisibility(View.VISIBLE);
//    if (progress < 2) {
//        progress = 2;
//        updateDots();
//    }
//    String currentTime = getCurrentTime();
//    String approvalMessage = "Booking has been Reviewed. Please proceed to the payment by clicking the Pay Now button.<br>";
//    String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//    messageText2.setText(Html.fromHtml(approvalMessage + redTime));
//    sendNotificationToFirebase(messageText2.getText().toString(), "dot2");
//}
//
//private void showDot4Message() {
//    messageFramedot4.setVisibility(View.VISIBLE);
//    messageText4.setVisibility(View.VISIBLE);
//    String currentTime = getCurrentTime();
//    String msg = "Payment transaction has been Approved. Please wait for final approval.<br>";
//    String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//    messageText4.setText(Html.fromHtml(msg + redTime));
//    sendNotificationToFirebase(messageText4.getText().toString(), "dot4");
//}
//
//private void showDot5Message() {
//    messageFramedot5.setVisibility(View.VISIBLE);
//    messageText5.setVisibility(View.VISIBLE);
//    String msg = "Congratulations! Your Booking has been Approved.<br>";
//    String currentTime = getCurrentTime();
//    String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//    messageText5.setText(Html.fromHtml(msg + redTime));
//    sendNotificationToFirebase(messageText5.getText().toString(), "dot5");
//}








//
//private void showApprovalMessage() {
//    messageFramedot2.setVisibility(View.VISIBLE);
//    messageText2.setVisibility(View.VISIBLE);
//    if (progress < 2) {
//        progress = 2;
//        updateDots();
//    }
//    String currentTime = getCurrentTime();
//    String approvalMessage = "Booking has been Reviewed. Please proceed to the payment by clicking the Pay Now button.<br>";
//    String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//    String fullMessage = approvalMessage + redTime;
//    messageText2.setText(Html.fromHtml(fullMessage));
//    sendNotificationToFirebase(messageText2.getText().toString(), "dot2");
//
//    // Store the approval message in SharedPreferences
//    SharedPreferences prefs = getSharedPreferences("BookingMessages", MODE_PRIVATE);
//    SharedPreferences.Editor editor = prefs.edit();
//    editor.putString("ApprovalMessage", messageText2.getText().toString());
//    editor.apply();
//}
//
//private void showDot4Message() {
//    messageFramedot4.setVisibility(View.VISIBLE);
//    messageText4.setVisibility(View.VISIBLE);
//    String currentTime = getCurrentTime();
//    String msg = "Payment transaction has been Approved. Please wait for final approval.<br>";
//    String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//    String fullMessage = msg + redTime;
//    messageText4.setText(Html.fromHtml(fullMessage));
//    sendNotificationToFirebase(messageText4.getText().toString(), "dot4");
//
//    // Store the payment approval message in SharedPreferences
//    SharedPreferences prefs = getSharedPreferences("BookingMessages", MODE_PRIVATE);
//    SharedPreferences.Editor editor = prefs.edit();
//    editor.putString("PaymentApprovalMessage", messageText4.getText().toString());
//    editor.apply();
//}
//
//private void showDot5Message() {
//    messageFramedot5.setVisibility(View.VISIBLE);
//    messageText5.setVisibility(View.VISIBLE);
//    String msg = "Congratulations! Your Booking has been Approved.<br>";
//    String currentTime = getCurrentTime();
//    String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//    String fullMessage = msg + redTime;
//    messageText5.setText(Html.fromHtml(fullMessage));
//    sendNotificationToFirebase(messageText5.getText().toString(), "dot5");
//
//    // Store the final approval message in SharedPreferences
//    SharedPreferences prefs = getSharedPreferences("BookingMessages", MODE_PRIVATE);
//    SharedPreferences.Editor editor = prefs.edit();
//    editor.putString("FinalApprovalMessage", messageText5.getText().toString());
//    editor.apply();
//}


//    @SuppressLint("ObsoleteSdkInt")
//    private void showLocalNotification(String title, String message, int notificationId) {
//        // Get the NotificationManager service.
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//        // For Android Oreo and above, create a notification channel.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    "booking_channel",
//                    "Booking Notifications",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            channel.setDescription("Channel for booking notifications");
//            channel.enableLights(true);
//            channel.enableVibration(true);
//            channel.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
//                    new AudioAttributes.Builder()
//                            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
//                            .build());
//            notificationManager.createNotificationChannel(channel);
//        }
//
//        // Build the notification.
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "booking_channel")
//                .setSmallIcon(R.drawable.ic_profile_notification) // Ensure this icon exists.
//                .setContentTitle(title)
//                .setContentText(message)
//                .setAutoCancel(true)
//                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
//
//        // Post the notification.
//        notificationManager.notify(notificationId, builder.build());
//    }

//
//    private static final String PREFS_NAME = "notification_prefs";
//    private static final String KEY_NOTIFIED_IDS = "notified_ids";
//
//
//    private void clearNotifiedIds() {
//        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//        prefs.edit().remove(KEY_NOTIFIED_IDS).apply();
//    }








//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // Remove Firebase listeners to prevent memory leaks.
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            String userId = currentUser.getUid();
//            DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                    .getReference("users").child(userId).child("MyBooking");
//            if (approvalListener != null) {
//                bookingRef.removeEventListener(approvalListener);
//            }
//            if (paymentListener != null) {
//                bookingRef.removeEventListener(paymentListener);
//            }
//        }
//    }


//    private void cancelNotification() {
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.cancel(1);
//        // Declare this at the class level:
//        boolean notificationShown = false;
//    }
























////This code is no decline function
//package com.example.resort;
//
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.text.Html;
//import android.util.Log;
//import android.view.View;
//import android.widget.Button;
//import android.widget.FrameLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.*;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Map;
//
//public class BookingStatus extends AppCompatActivity {
//
//    // Progress tracker: 0 = no progress, 1 = booking submitted, 2 = reviewed, 3 = payment submitted,
//    // 4 = payment transaction approved, 5 = final approval.
//    private int progress = 0;
//    private String bookingId;  // Passed from BookingReceipt.
//
//    private boolean bookingMoved = false; // Prevents double data store in Firebase
//    private TextView dot1, dot2, dot3, dot4, dot5;
//    private View line1_2, line2_3, line3_4, line4_5;
//    private Button payNowButton, cancelButton;
//
//    // Message containers for dots 1 to 5.
//    private FrameLayout messageFramedot1, messageFramedot2, messageFramedot3, messageFramedot4, messageFramedot5;
//    private TextView messageText, messageText2, paymentMessageText, messageText4, messageText5;
//
//    // SharedPreferences for persisting booking state.
//    private SharedPreferences prefs;
//
//    // Flags for one-time processing.
//    private boolean approvalProcessed = false;
//    private boolean paymentApprovedProcessed = false;
//    private static final int PAYMENT_REQUEST_CODE = 1001;
//
//    // Firebase listener references.
//    private ValueEventListener approvalListener;
//    private ValueEventListener paymentListener;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_booking_status);
//
//        // Get current user and set up user-specific SharedPreferences.
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            // Handle not-logged-in state (e.g., redirect to login)
//            finish();
//            return;
//        }
//        String userId = currentUser.getUid();
//        prefs = getSharedPreferences("BookingPrefs_" + userId, MODE_PRIVATE);
//
//        // If booking is submitted from previous activity, save state in SharedPreferences.
//        boolean bookingSubmittedIntent = getIntent().getBooleanExtra("bookingSubmitted", false);
//        if (bookingSubmittedIntent) {
//            prefs.edit().putBoolean("bookingSubmitted", true).apply();
//            progress = 1;
//            prefs.edit().putInt("bookingProgress", progress).apply();
//        }
//
//        // Adjust layout for system insets.
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
//                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
//                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
//                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
//            return insets;
//        });
//
//        // Initialize UI elements.
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
//        // Restore persisted booking state.
//        if (prefs.contains("bookingSubmitted") && prefs.getBoolean("bookingSubmitted", false)) {
//            progress = 1;
//            updateDots();
//            showSubmissionMessage();
//        }
//        if (prefs.contains("paymentSubmitted") && prefs.getBoolean("paymentSubmitted", false)) {
//            progress = Math.max(progress, 3);
//            updateDots();
//            String paymentTime = prefs.getString("paymentSubmittedTime", "");
//            showPaymentSubmittedMessage(paymentTime);
//        }
//        if (prefs.contains("paymentApproved") && prefs.getBoolean("paymentApproved", false)) {
//            progress = 5;
//            updateDots();
//            String finalApprovalTime = prefs.getString("paymentApprovedTime", "");
//            showDot4Message();
//            showDot5Message(finalApprovalTime);
//        }
//
//        // Query Firebase immediately to update state based on current data.
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyBooking");
//        bookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                    // Check booking review approval.
//                    String statusReview = bookingSnapshot.child("bookingReview")
//                            .child("statusReview")
//                            .getValue(String.class);
//                    if (statusReview != null && statusReview.equalsIgnoreCase("Approved")) {
//                        progress = Math.max(progress, 2);
//                        prefs.edit().putInt("bookingProgress", progress).apply();
//                        showApprovalMessage();
//                    }
//                    // Check payment transaction approval.
//                    if (bookingSnapshot.child("paymentTransaction").exists()) {
//                        String paymentStatus = bookingSnapshot.child("paymentTransaction")
//                                .child("paymentStatus")
//                                .getValue(String.class);
//                        if (paymentStatus != null && paymentStatus.equalsIgnoreCase("Approved")) {
//                            progress = Math.max(progress, 4);
//                            prefs.edit().putInt("bookingProgress", progress).apply();
//                            showDot4Message();
//                            // Simulate final approval right away.
//                            String approvalTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//                            progress = 5;
//                            updateDots();
//                            showDot5Message(approvalTime);
//                            prefs.edit().putBoolean("paymentApproved", true)
//                                    .putString("paymentApprovedTime", approvalTime)
//                                    .apply();
//                        }
//                    }
//                }
//                updateDots(); // Refresh the UI dots with the latest progress.
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e("BookingStatus", "Error fetching booking data: " + error.getMessage());
//            }
//        });
//
//        // Set up Firebase listeners for dynamic updates.
//        listenForApproval();
//        listenForPaymentTransactionApproval();
//
//
//        // Use the already defined currentUser
//        currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            userId = currentUser.getUid();
//            SharedPreferences prefs = getSharedPreferences("BookingPref_" + userId, MODE_PRIVATE);
//
//            // Try retrieving bookingId from the Intent.
//            bookingId = getIntent().getStringExtra("bookingId");
//            if (bookingId == null) {
//                // Fallback to SharedPreferences.
//                bookingId = prefs.getString("bookingId", null);
//            }
//
//            if (bookingId == null) {
//                Toast.makeText(BookingStatus.this, "Booking not found!", Toast.LENGTH_SHORT).show();
//            }
//        } else {
//            Toast.makeText(BookingStatus.this, "User not logged in", Toast.LENGTH_SHORT).show();
//        }
//
//        // Set up button actions.
//        payNowButton.setOnClickListener(v -> {
//            if (progress < 2) {
//                Toast.makeText(BookingStatus.this, "Please complete the previous step before proceeding.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            if (bookingId == null) {
//                Toast.makeText(BookingStatus.this, "Booking not found!", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            Intent paymentIntent = new Intent(BookingStatus.this, Payment.class);
//            paymentIntent.putExtra("bookingId", bookingId);
//            startActivityForResult(paymentIntent, PAYMENT_REQUEST_CODE);
//        });
//
//
//        backButton.setOnClickListener(v -> onBackPressed());
//
//        // Set up the cancel button listener.
//        cancelButton.setOnClickListener(view -> cancelBooking());
//    }
//
//    // Update dots and lines based on current progress.
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
//        // Save updated progress.
//        prefs.edit().putInt("bookingProgress", progress).apply();
//    }
//
//    /**
//     * Cancels a booking by:
//     * - Appending a cancellation timestamp to each booking's review.
//     * - Moving data from "MyBooking" to "MyCancel".
//     * - Removing "MyBooking" and "MyReview" data.
//     * - Updating the UI with a cancellation message.
//     * - Disabling the cancel button.
//     */
//    private void cancelBooking() {
//        if (progress < 1) {
//            Toast.makeText(BookingStatus.this, "No booking in the field, cannot cancel.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//
//        // Get references to the relevant Firebase nodes.
//        DatabaseReference myBookingRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyBooking");
//        DatabaseReference myCancelRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyCancel");
//
//        // Fetch booking data.
//        myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                // Build the cancellation timestamp.
//                String cancelTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//                String cancelValue = "Cancelled at " + cancelTime;
//
//                // Append the "UserCancel" node to each booking's review.
//                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                    bookingSnapshot.getRef()
//                            .child("bookingReview")
//                            .child("UserCancel")
//                            .setValue(cancelValue);
//                }
//
//                // Move the booking data from "MyBooking" to "MyCancel".
//                myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot bookingSnapshot) {
//                        for (DataSnapshot child : bookingSnapshot.getChildren()) {
//                            Object bookingData = child.getValue();
//                            myCancelRef.push().setValue(bookingData);
//                        }
//                        // Remove all bookings.
//                        myBookingRef.removeValue();
//
//                        // Remove the "MyReview" node.
//                        DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                                .getReference("users")
//                                .child(userId)
//                                .child("MyReview");
//                        myReviewRef.removeValue();
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) { }
//                });
//
//                // Prepare and display the cancellation UI message.
//                String cancellationMessage = "Booking has been Cancelled in Booking Review.<br>";
//                String redTime = String.format("<font color='#FF0000'>%s</font>", cancelTime);
//                if (progress >= 1) {
//                    messageFramedot1.setVisibility(View.VISIBLE);
//                    messageText.setVisibility(View.VISIBLE);
//                    messageText.setText(Html.fromHtml(cancellationMessage + redTime));
//                    sendNotificationToFirebase(messageText.getText().toString(), "dot6");
//                }
//
//                // Reset state after a delay.
//                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                    progress = 0;
//                    updateDots();
//                    clearBookingMessageUI();
//                    clearBookingPreferences();
//                }, 5000);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(BookingStatus.this, "Cancellation failed: " + error.getMessage(), Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void showSubmissionMessage() {
//        messageFramedot1.setVisibility(View.VISIBLE);
//        messageText.setVisibility(View.VISIBLE);
//        String storedTime = prefs.getString("submissionTime", "");
//        if (storedTime.isEmpty()) {
//            storedTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//            prefs.edit().putString("submissionTime", storedTime).apply();
//        }
//        String submissionMessage = "Booking has been Submitted. Please wait for it to be reviewed by admin.<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", storedTime);
//        messageText.setText(Html.fromHtml(submissionMessage + redTime));
//        sendNotificationToFirebase(messageText.getText().toString(), "dot1");
//    }
//
//    private void showApprovalMessage() {
//        messageFramedot2.setVisibility(View.VISIBLE);
//        messageText2.setVisibility(View.VISIBLE);
//        if (progress < 2) {
//            progress = 2;
//            updateDots();
//        }
//        String currentTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//        String approvalMessage = "Booking has been Reviewed. Please proceed to the payment by clicking the Pay Now button.<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        messageText2.setText(Html.fromHtml(approvalMessage + redTime));
//        sendNotificationToFirebase(messageText2.getText().toString(), "dot2");
//        // Disable the cancel button.
//        cancelButton.setEnabled(false);
//        cancelButton.setClickable(false);
//        cancelButton.setAlpha(0.5f);
//    }
//
//    private void showPaymentSubmittedMessage(String submissionTime) {
//        messageFramedot3.setVisibility(View.VISIBLE);
//        paymentMessageText.setVisibility(View.VISIBLE);
//        String paymentMessage = "Payment has been Submitted. Please wait for admin review.<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", submissionTime);
//        paymentMessageText.setText(Html.fromHtml(paymentMessage + redTime));
//        // Disable the pay now button.
//        payNowButton.setEnabled(false);
//        payNowButton.setClickable(false);
//        payNowButton.setAlpha(0.5f);
//        // Disable the cancel button as well.
//        cancelButton.setEnabled(false);
//        cancelButton.setClickable(false);
//        cancelButton.setAlpha(0.5f);
//        sendNotificationToFirebase(paymentMessageText.getText().toString(), "dot3");
//    }
//
//    private void showDot4Message() {
//        messageFramedot4.setVisibility(View.VISIBLE);
//        messageText4.setVisibility(View.VISIBLE);
//        String currentTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//        String msg = "Payment transaction has been Approved. Please wait for final approval.<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", currentTime);
//        messageText4.setText(Html.fromHtml(msg + redTime));
//        sendNotificationToFirebase(messageText4.getText().toString(), "dot4");
//    }
//
//    private void showDot5Message(String approvalTime) {
//        messageFramedot5.setVisibility(View.VISIBLE);
//        messageText5.setVisibility(View.VISIBLE);
//        String msg = "Congratulations! Your Booking has been Approved.<br>";
//        String redTime = String.format("<font color='#FF0000'>%s</font>", approvalTime);
//        messageText5.setText(Html.fromHtml(msg + redTime));
//        sendNotificationToFirebase(messageText5.getText().toString(), "dot5");
//
//        // After final approval, clear UI and preferences after a delay.
//        new Handler(Looper.getMainLooper()).postDelayed(() -> {
//            expireFirebaseNotifications();
//            moveAllBookingsToHistory();
//            clearBookingMessageUI();
//            clearBookingPreferences();
//        }, 5000);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PAYMENT_REQUEST_CODE && resultCode == RESULT_OK) {
//            String paymentTime = data.getStringExtra("paymentSubmittedTime");
//            progress = 3;
//            updateDots();
//            showPaymentSubmittedMessage(paymentTime);
//            prefs.edit().putBoolean("paymentSubmitted", true)
//                    .putString("paymentSubmittedTime", paymentTime)
//                    .apply();
//        }
//    }
//
//    private void listenForApproval() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyBooking");
//
//        approvalListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                    String statusReview = bookingSnapshot.child("bookingReview")
//                            .child("statusReview")
//                            .getValue(String.class);
//                    if (statusReview != null && statusReview.equalsIgnoreCase("Approved") && !approvalProcessed) {
//                        approvalProcessed = true;
//                        showApprovalMessage();
//                        break;
//                    }
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) { }
//        };
//        bookingRef.addValueEventListener(approvalListener);
//    }
//
//    private void listenForPaymentTransactionApproval() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users").child(userId).child("MyBooking");
//
//        paymentListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                    if (bookingSnapshot.child("paymentTransaction").exists()) {
//                        String paymentStatus = bookingSnapshot.child("paymentTransaction")
//                                .child("paymentStatus")
//                                .getValue(String.class);
//                        if (paymentStatus != null && paymentStatus.equalsIgnoreCase("Approved") && !paymentApprovedProcessed) {
//                            paymentApprovedProcessed = true;
//
//                            // Immediately update progress to 4 and show message4.
//                            progress = 4;
//                            updateDots();
//                            showDot4Message();
//
//                            // Delay final approval (message5 and dot5) for 5 seconds.
//                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                progress = 5;
//                                updateDots();  // Now dot5 turns active after the delay.
//                                String approvalTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault())
//                                        .format(new Date());
//                                showDot5Message(approvalTime);  // Display final approval message.
//
//                                // Save the final approval state.
//                                prefs.edit().putBoolean("paymentApproved", true)
//                                        .putString("paymentApprovedTime", approvalTime)
//                                        .putBoolean("bookingComplete", true)
//                                        .putInt("bookingProgress", progress)
//                                        .apply();
//
//                                // Delay cleanup after showing message5 by an additional 5 seconds.
//                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                    expireFirebaseNotifications();
//                                    moveAllBookingsToHistory();
//                                    clearBookingMessageUI();
//                                    prefs.edit().remove("bookingSubmitted")
//                                            .remove("paymentSubmitted")
//                                            .remove("paymentSubmittedTime")
//                                            .remove("paymentApproved")
//                                            .remove("paymentApprovedTime")
//                                            .remove("bookingComplete")
//                                            .remove("bookingProgress")
//                                            .remove("bookingId")
//                                            .apply();
//                                }, 5000);
//
//                            }, 5000);
//
//                            break;
//                        }
//                    }
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) { }
//        };
//        bookingRef.addValueEventListener(paymentListener);
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
//        Map<String, Object> notificationData = new HashMap<>();
//        notificationData.put("message", message);
//        notificationData.put("timestamp", new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date()));
//        notificationData.put("expired", false);
//        notificationRef.setValue(notificationData);
//    }
//
//    /**
//     * Marks all notifications in Firebase as expired.
//     */
//    private void expireFirebaseNotifications() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("notifications");
//        notificationRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot child : snapshot.getChildren()) {
//                    child.getRef().child("expired").setValue(true);
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) { }
//        });
//    }
//
//    /**
//     * Moves all booking data from "MyBooking" to "MyHistory".
//     */
//    private void moveAllBookingsToHistory() {
//        if (bookingMoved) return;
//        bookingMoved = true;
//
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
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
//                    myHistoryRef.push().setValue(bookingData);
//                }
//                myBookingRef.removeValue();
//            }
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
//                .remove("paymentApprovedTime")
//                .remove("submissionTime")
//                .remove("bookingComplete")
//                .remove("bookingProgress")
//                //.remove("bookingId")
//                .apply();
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // Remove Firebase listeners to prevent memory leaks.
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            String userId = currentUser.getUid();
//            DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                    .getReference("users").child(userId).child("MyBooking");
//            if (approvalListener != null) {
//                bookingRef.removeEventListener(approvalListener);
//            }
//            if (paymentListener != null) {
//                bookingRef.removeEventListener(paymentListener);
//            }
//        }
//    }
//}
