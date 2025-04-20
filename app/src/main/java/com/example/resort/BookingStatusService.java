package com.example.resort;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.Set;

public class BookingStatusService extends Service {
    private static final String TAG = "BookingStatusService";
    private static final String CHANNEL_ID = "booking_channel";

    private Handler handler;
    private Runnable pollTask;
    private final Set<Integer> shownNotifications = new HashSet<>();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service onCreate – initializing");
        handler = new Handler(Looper.getMainLooper());
        startForegroundServiceWithNotification();
        listenForNotifications();
    }

    private void startForegroundServiceWithNotification() {
        // Create channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Booking Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("Notifications for booking status");
            NotificationManager mgr = getSystemService(NotificationManager.class);
            if (mgr != null) mgr.createNotificationChannel(ch);
        }

        // Build persistent notification
        Intent stop = new Intent(this, BookingStatusService.class)
                .setAction("STOP_FOREGROUND_SERVICE");
        PendingIntent pi = PendingIntent.getService(
                this, 0, stop,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification notify = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Island Front View Booking")
                .setContentText("Service running…")
                .setSmallIcon(R.drawable.ic_profile_notification)
                .addAction(R.drawable.ic_launcher_foreground, "Stop", pi)
                .setOngoing(true)
                .setAutoCancel(false)
                .build();

        startForeground(1, notify);
    }

    private void listenForNotifications() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Log.w(TAG, "No user—cannot poll bookings.");
            stopSelf();
            return;
        }

        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid())
                .child("MyBooking");

        pollTask = new Runnable() {
            boolean done = false;

            @Override
            public void run() {
                if (done) return;

                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        for (DataSnapshot bSnap : snap.getChildren()) {
                            /// 1) Booking review
                            String review = bSnap.child("bookingReview")
                                    .child("statusReview")
                                    .getValue(String.class);
                            if ("Approved".equalsIgnoreCase(review)) {
                                showLocalNotification(
                                        "Booking Approved",
                                        "Proceed to payment!",
                                        100
                                );
                                // keep done=false so we can catch later final approval
                                break;
                            } else if ("Declined".equalsIgnoreCase(review)) {
                                done = true;  /// stop after decline
                                showLocalNotification(
                                        "Booking Declined",
                                        "Booking has been reviewed. Please proceed to the payment by clicking the Pay Now button.",
                                        101
                                );
                                break;
                            }

                            /// 2) Payment transaction
                            if (bSnap.child("paymentTransaction").exists()) {
                                String payStatus = bSnap.child("paymentTransaction")
                                        .child("paymentStatus")
                                        .getValue(String.class);
                                String finalStatus = bSnap.child("paymentTransaction")
                                        .child("finalStatus")
                                        .getValue(String.class);

                                if ("Refund".equalsIgnoreCase(payStatus)) {
                                    done = true;  /// stop after refund
                                    showLocalNotification(
                                            "Payment Refunded",
                                            "Your payment has been reversed and refunded by the admin.",
                                            102
                                    );
                                    break;
                                } else if ("Approved".equalsIgnoreCase(payStatus)) {
                                    showLocalNotification(
                                            "Payment Approved",
                                            "Payment transaction has been approved. Please wait for final approval.",
                                            103
                                    );
                                    break;
                                } else if ("Approved".equalsIgnoreCase(finalStatus)) {
                                    done = true;  /// stop after final approval
                                    showLocalNotification(
                                            "Final Approval",
                                            "Congratulations! Your booking has been approved.",
                                            104
                                    );
                                    break;
                                }
                            }
                        }

                        if (done) {
                            handler.removeCallbacks(pollTask);
                            stopForeground(true);
                            stopSelf();
                        } else {
                            handler.postDelayed(pollTask, 1000);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e(TAG, "Polling error", error.toException());
                        handler.postDelayed(pollTask, 1000);
                    }
                });
            }
        };

        handler.post(pollTask);
    }

    private void showLocalNotification(String title, String message, int id) {
        // 1) Skip if already shown
        if (shownNotifications.contains(id)) return;
        shownNotifications.add(id);

        NotificationManager mgr = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);
        if (mgr == null) return;

        // 2) (Optional) skip if still active in status bar
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (StatusBarNotification sbn : mgr.getActiveNotifications()) {
                if (sbn.getId() == id) return;
            }
        }

        // 3) Ensure channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "Booking Updates",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            ch.setDescription("Booking status alerts");
            mgr.createNotificationChannel(ch);
        }

        // 4) Build & fire
        Notification notify = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_profile_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setOngoing(false)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .build();

        mgr.notify(id, notify);
        Log.d(TAG, "Notified: " + title);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && "STOP_FOREGROUND_SERVICE".equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopForeground(true);
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

///Fix Current 2 Original
//package com.example.resort;
//
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.media.RingtoneManager;
//import android.os.Build;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Looper;
//import android.service.notification.StatusBarNotification;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.HashSet;
//import java.util.Set;
//
//public class BookingStatusService extends Service {
//    private static final String TAG = "BookingStatusService";
//    private static final String CHANNEL_ID = "booking_channel";
//
//    private Handler handler;
//    private Runnable pollTask;
//    private final Set<Integer> shownNotifications = new HashSet<>();
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        Log.d(TAG, "Service onCreate – initializing");
//        handler = new Handler(Looper.getMainLooper());
//        startForegroundServiceWithNotification();
//        listenForNotifications();
//    }
//
//    private void startForegroundServiceWithNotification() {
//        // Create channel
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel ch = new NotificationChannel(
//                    CHANNEL_ID,
//                    "Booking Updates",
//                    NotificationManager.IMPORTANCE_DEFAULT
//            );
//            ch.setDescription("Notifications for booking status");
//            NotificationManager mgr = getSystemService(NotificationManager.class);
//            if (mgr != null) mgr.createNotificationChannel(ch);
//        }
//
//        // Build persistent notification
//        Intent stop = new Intent(this, BookingStatusService.class)
//                .setAction("STOP_FOREGROUND_SERVICE");
//        PendingIntent pi = PendingIntent.getService(
//                this, 0, stop,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//        Notification notify = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("Island Front View Booking")
//                .setContentText("Service running…")
//                .setSmallIcon(R.drawable.ic_profile_notification)
//                .addAction(R.drawable.ic_launcher_foreground, "Stop", pi)
//                .setOngoing(true)
//                .setAutoCancel(false)
//                .build();
//
//        startForeground(1, notify);
//    }
//
//    private void listenForNotifications() {
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        if (user == null) {
//            Log.w(TAG, "No user—cannot poll bookings.");
//            stopSelf();
//            return;
//        }
//
//        DatabaseReference ref = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(user.getUid())
//                .child("MyBooking");
//
//        pollTask = new Runnable() {
//            boolean done = false;
//
//            @Override
//            public void run() {
//                if (done) return;
//
//                ref.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snap) {
//                        for (DataSnapshot bSnap : snap.getChildren()) {
//                            /// 1) Booking review
//                            String review = bSnap.child("bookingReview")
//                                    .child("statusReview")
//                                    .getValue(String.class);
//                            if ("Approved".equalsIgnoreCase(review)) {
//                                showLocalNotification(
//                                        "Booking Approved",
//                                        "Proceed to payment!",
//                                        100
//                                );
//                                // keep done=false so we can catch later final approval
//                                break;
//                            } else if ("Declined".equalsIgnoreCase(review)) {
//                                done = true;  /// stop after decline
//                                showLocalNotification(
//                                        "Booking Declined",
//                                        "Booking has been reviewed. Please proceed to the payment by clicking the Pay Now button.",
//                                        101
//                                );
//                                break;
//                            }
//
//                            /// 2) Payment transaction
//                            if (bSnap.child("paymentTransaction").exists()) {
//                                String payStatus = bSnap.child("paymentTransaction")
//                                        .child("paymentStatus")
//                                        .getValue(String.class);
//                                String finalStatus = bSnap.child("paymentTransaction")
//                                        .child("finalStatus")
//                                        .getValue(String.class);
//
//                                if ("Refund".equalsIgnoreCase(payStatus)) {
//                                    done = true;  /// stop after refund
//                                    showLocalNotification(
//                                            "Payment Refunded",
//                                            "Your payment has been reversed and refunded by the admin.",
//                                            102
//                                    );
//                                    break;
//                                } else if ("Approved".equalsIgnoreCase(payStatus)) {
//                                    showLocalNotification(
//                                            "Payment Approved",
//                                            "Payment transaction has been approved. Please wait for final approval.",
//                                            103
//                                    );
//                                    break;
//                                } else if ("Approved".equalsIgnoreCase(finalStatus)) {
//                                    done = true;  /// stop after final approval
//                                    showLocalNotification(
//                                            "Final Approval",
//                                            "Congratulations! Your booking has been approved.",
//                                            104
//                                    );
//                                    break;
//                                }
//                            }
//                        }
//
//                        if (done) {
//                            handler.removeCallbacks(pollTask);
//                            stopForeground(true);
//                            stopSelf();
//                        } else {
//                            handler.postDelayed(pollTask, 1000);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e(TAG, "Polling error", error.toException());
//                        handler.postDelayed(pollTask, 1000);
//                    }
//                });
//            }
//        };
//
//        handler.post(pollTask);
//    }
//
//    private void showLocalNotification(String title, String message, int id) {
//        // 1) Skip if already shown
//        if (shownNotifications.contains(id)) return;
//        shownNotifications.add(id);
//
//        NotificationManager mgr = (NotificationManager)
//                getSystemService(Context.NOTIFICATION_SERVICE);
//        if (mgr == null) return;
//
//        // 2) (Optional) skip if still active in status bar
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            for (StatusBarNotification sbn : mgr.getActiveNotifications()) {
//                if (sbn.getId() == id) return;
//            }
//        }
//
//        // 3) Ensure channel
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel ch = new NotificationChannel(
//                    CHANNEL_ID,
//                    "Booking Updates",
//                    NotificationManager.IMPORTANCE_DEFAULT
//            );
//            ch.setDescription("Booking status alerts");
//            mgr.createNotificationChannel(ch);
//        }
//
//        // 4) Build & fire
//        Notification notify = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_profile_notification)
//                .setContentTitle(title)
//                .setContentText(message)
//                .setAutoCancel(true)
//                .setOngoing(false)
//                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
//                .build();
//
//        mgr.notify(id, notify);
//        Log.d(TAG, "Notified: " + title);
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (intent != null && "STOP_FOREGROUND_SERVICE".equals(intent.getAction())) {
//            stopForeground(true);
//            stopSelf();
//            return START_NOT_STICKY;
//        }
//        return START_STICKY;
//    }
//
//    @Override
//    public void onDestroy() {
//        stopForeground(true);
//        super.onDestroy();
//        Log.d(TAG, "Service destroyed");
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//}




///No Current User 1 Not Use
//package com.example.resort;
//
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.media.RingtoneManager;
//import android.os.Build;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Looper;
//import android.service.notification.StatusBarNotification;
//import android.util.Log;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//public class BookingStatusService extends Service {
//    private static final String TAG = "BookingStatusService";
//    private static final String CHANNEL_ID = "booking_channel";
//
//    private Handler handler;
//    private Runnable pollTask;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        Log.d(TAG, "Service onCreate – initializing");
//        handler = new Handler(Looper.getMainLooper());
//        startForegroundServiceWithNotification();
//        listenForNotifications();
//    }
//
//    private void startForegroundServiceWithNotification() {
//        // Create channel
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel ch = new NotificationChannel(
//                    CHANNEL_ID,
//                    "Booking Updates",
//                    NotificationManager.IMPORTANCE_DEFAULT
//            );
//            ch.setDescription("Notifications for booking status");
//            NotificationManager mgr = getSystemService(NotificationManager.class);
//            if (mgr != null) mgr.createNotificationChannel(ch);
//        }
//
//        // Build persistent notification
//        Intent stop = new Intent(this, BookingStatusService.class)
//                .setAction("STOP_FOREGROUND_SERVICE");
//        PendingIntent pi = PendingIntent.getService(
//                this, 0, stop,
//                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
//        );
//
//
//        Notification notify = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("Island Front View Booking")
//                .setContentText("Service running…")
//                .setSmallIcon(R.drawable.ic_profile_notification)
//                .addAction(R.drawable.ic_launcher_foreground, "Stop", pi)
//                .setOngoing(true)
//                .setAutoCancel(false)
//                .build();
//
//        startForeground(1, notify);
//    }
//
//    private void listenForNotifications() {
//        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
//        if (user == null) {
//            Log.w(TAG, "No user—cannot poll bookings.");
//            stopSelf();
//            return;
//        }
//
//        DatabaseReference ref = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(user.getUid())
//                .child("MyBooking");
//
//        pollTask = new Runnable() {
//            boolean done = false;
//
//            @Override
//            public void run() {
//                if (done) return;
//
//                ref.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snap) {
//                        for (DataSnapshot bSnap : snap.getChildren()) {
//                            // 1) Booking review
//                            String review = bSnap.child("bookingReview")
//                                    .child("statusReview")
//                                    .getValue(String.class);
//                            if ("Approved".equalsIgnoreCase(review)) {
//                                ///done = true;
//                                showLocalNotification(
//                                        "Booking Approved",
//                                        "Proceed to payment!",
//                                        100
//                                );
//                                break;
//                            } else if ("Declined".equalsIgnoreCase(review)) {
//                                done = true;
//                                showLocalNotification(
//                                        "Booking Declined",
//                                        "Booking has been Reviewed. Please proceed to the payment by clicking the Pay Now button.",
//                                        101
//                                );
//                                break;
//                            }
//
//                            // 2) Payment transaction
//                            if (bSnap.child("paymentTransaction").exists()) {
//                                String payStatus = bSnap.child("paymentTransaction")
//                                        .child("paymentStatus")
//                                        .getValue(String.class);
//                                String finalStatus = bSnap.child("paymentTransaction")
//                                        .child("finalStatus")
//                                        .getValue(String.class);
//
//                                if ("Refund".equalsIgnoreCase(payStatus)) {
//                                    done = true;
//                                    showLocalNotification(
//                                            "Payment Refunded",
//                                            "Your payment has been reversed and refunded by the admin.",
//                                            102
//                                    );
//                                    break;
//                                } else if ("Approved".equalsIgnoreCase(payStatus)) {
//                                    ///done = true;
//                                    showLocalNotification(
//                                            "Payment Approved",
//                                            "Payment transaction has been Approved. Please wait for final approval.",
//                                            103
//                                    );
//                                    break;
//                                } else if ("Approved".equalsIgnoreCase(finalStatus)) {
//                                    done = true;
//                                    showLocalNotification(
//                                            "Final Approval",
//                                            "Congratulations! Your Booking has been Approved.",
//                                            104
//                                    );
//                                    break;
//                                }
//                            }
//                        }
//
//                        if (done) {
//                            handler.removeCallbacks(pollTask);
//                            stopForeground(true);
//                            stopSelf();
//                        } else {
//                            handler.postDelayed(pollTask, 1000);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e(TAG, "Polling error", error.toException());
//                        // retry on error
//                        handler.postDelayed(pollTask, 1000);
//                    }
//                });
//            }
//        };
//
//        handler.post(pollTask);
//    }
//
//
//    private void showLocalNotification(String title, String message, int id) {
//        NotificationManager mgr = (NotificationManager)
//                getSystemService(Context.NOTIFICATION_SERVICE);
//        if (mgr == null) return;
//
//        // avoid duplicates
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            for (StatusBarNotification sbn : mgr.getActiveNotifications()) {
//                if (sbn.getId() == id) return;
//            }
//        }
//
//        // ensure channel
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel ch = new NotificationChannel(
//                    CHANNEL_ID, "Booking Updates", NotificationManager.IMPORTANCE_DEFAULT);
//            ch.setDescription("Booking status alerts");
//            mgr.createNotificationChannel(ch);
//        }
//
//        Notification notify = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_profile_notification)
//                .setContentTitle(title)
//                .setContentText(message)
//                .setAutoCancel(true)
//                .setOngoing(false)
//                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
//                .build();
//
//        mgr.notify(id, notify);
//        Log.d(TAG, "Notified: " + title);
//    }
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        if (intent != null && "STOP_FOREGROUND_SERVICE".equals(intent.getAction())) {
//            stopForeground(true);
//            stopSelf();
//            return START_NOT_STICKY;
//        }
//        return START_STICKY;
//    }
//
//    @Override
//    public void onDestroy() {
//        stopForeground(true);
//        super.onDestroy();
//        Log.d(TAG, "Service destroyed");
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null;
//    }
//}


///No Current Many Bug
//package com.example.resort;
//import android.app.Notification;
//import android.app.NotificationChannel;
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.Context;
//import android.content.Intent;
//import android.media.AudioAttributes;
//import android.media.RingtoneManager;
//import android.os.Build;
//import android.os.Handler;
//import android.os.IBinder;
//import android.os.Looper;
//import android.service.notification.StatusBarNotification;
//import android.text.Html;
//import android.util.Log;
//import android.view.View;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.core.app.NotificationCompat;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Map;
//
//public class BookingStatusService extends Service {
//    private static final String TAG = "BookingStatusService";
//    private static final String CHANNEL_ID = "booking_channel";
//
//    /// Flags to ensure that we only process once for each event, as in your code.
//    private boolean approvalProcessed = false;
//    private boolean paymentApprovedProcessed = false;
//    private boolean finalProcessed = false;
//    private boolean declineProcessed = false;
//    private boolean paymentDeclineProcessed = false;
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        Log.d(TAG, "Service onCreate – starting foreground service and initializing listeners");
//        startEnhancedForegroundService();
//        listenForBookingUpdates();
//    }
//
//    /**
//     * This method sets up a persistent notification that cannot easily be dismissed
//     * and starts the service in the foreground.
//     *
//     *
//     */
//    private void startEnhancedForegroundService() {
//        /// Create or update the notification channel for Android O and above.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    "Booking Notifications",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            channel.setDescription("Channel for booking notifications");
//            NotificationManager manager = getSystemService(NotificationManager.class);
//            if (manager != null) {
//                manager.createNotificationChannel(channel);
//            }
//        }
//
//        /// Prepare an intent to stop the foreground service.
//        Intent stopIntent = new Intent(this, BookingStatusService.class);
//        stopIntent.setAction("STOP_FOREGROUND_SERVICE");
//        PendingIntent stopPendingIntent = PendingIntent.getService(
//                this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
//
//        /// Build the persistent notification with the stop action.
//        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setContentTitle("Island Front View Beach Resort Booking App")
//                .setContentText("Running...")
//                .setSmallIcon(R.drawable.ic_profile_notification)
//                .setOngoing(true)
//                .setAutoCancel(false)
//                .addAction(R.drawable.ic_launcher_foreground, "Stop", stopPendingIntent)
//                .build();
//
//        /// Start the service in the foreground with the persistent notification.
//        startForeground(1, notification);
//        Log.d(TAG, "Foreground notification started with ID: 1");
//    }
//
//    private void listenForBookingUpdates() {
//        /// Your existing code to subscribe to Firebase updates.
//        /// Consider adding logging inside each listener callback to monitor service activity.
//        listenForApproval();
//        listenForPaymentTransactionApproval();
//        FinalForApproval();
//    }
//
//    /// Booking Review Approval
//    private void listenForApproval() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            Log.w(TAG, "Current user is null; cannot listen for booking approval.");
//            return;
//        }
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
//                        boolean shouldStopPolling = false;
//                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                            String statusReview = bookingSnapshot.child("bookingReview")
//                                    .child("statusReview")
//                                    .getValue(String.class);
//                            if (statusReview != null) {
//                                if (statusReview.equalsIgnoreCase("Approved") && !approvalProcessed) {
//                                    approvalProcessed = true;
//                                    showLocalNotification("Booking has been Reviewed",
//                                            "Proceed to payment!", 2);
//                                    clearNotification(4);
//                                    clearNotification(5);
//                                    clearNotification(6);
//                                    clearNotification(7);
//                                    Log.d(TAG, "Booking approved; notification shown");
//                                    shouldStopPolling = true;
//                                    break;
//                                } else if (statusReview.equalsIgnoreCase("Declined") && !declineProcessed) {
//                                    declineProcessed = true;
//                                    showLocalNotification("Booking Declined!",
//                                            "Your booking has been declined.", 5);
//                                    stopForeground(true);
//                                    stopSelf();
//                                    Log.d(TAG, "Booking declined; notification shown");
//                                    shouldStopPolling = true;
//                                    break;
//                                }
//                            }
//                        }
//                        if (!shouldStopPolling) {
//                            handler.postDelayed(pollTask[0], 1000);
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Log.e(TAG, "Error reading booking data", error.toException());
//                    }
//                });
//            }
//        };
//        handler.post(pollTask[0]);
//    }
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
//                                    clearNotification(2);
//                                    showLocalNotification("Payment Approved",
//                                            "Payment has been approved. Awaiting final approval.", 3);
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
//                                            showLocalNotification("Payment Declined!",
//                                                    "Your payment has been reversed and refunded by the admin.", 6);
//                                            clearNotification(2);
//                                            stopForeground(true);
//                                            stopSelf();
//                                            Log.d(TAG, "Booking refund; notification shown");
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
//
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
//                        // If no booking data, stop polling after delay.
//                        if (!snapshot.hasChildren()) {
//                            stopPolling();
//                            new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                            }, 1000);
//                            return;
//                        }
//                        // Process each booking entry.
//                        for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                            String finalStatus = bookingSnapshot.child("paymentTransaction")
//                                    .child("finalStatus")
//                                    .getValue(String.class);
//                            // If Approved: clear UI, copy review, and then stop the foreground service.
//                            if (finalStatus != null && finalStatus.equalsIgnoreCase("Approved") && !finalProcessed) {
//                                clearNotification(3);
//                                showLocalNotification("Congratulations!", "Your booking has been finally approved.", 4);
//                                finalProcessed = true;
//
//                                // Stop the polling to 'hide' the listener.
//                                stopPolling();
//                                Log.d(TAG, "Final approval received. Processing reviews and stopping service.");
//
//                                // Process MyReview to MyReviewDone copy:
//                                DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                                        .getReference("users")
//                                        .child(userId)
//                                        .child("MyReview");
//                                DatabaseReference myReviewDoneRef = FirebaseDatabase.getInstance()
//                                        .getReference("users")
//                                        .child(userId)
//                                        .child("MyReviewDone");
//
//                                myReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                                    @Override
//                                    public void onDataChange(@NonNull DataSnapshot reviewSnapshot) {
//                                        if (reviewSnapshot.exists()) {
//                                            for (DataSnapshot review : reviewSnapshot.getChildren()) {
//                                                @SuppressWarnings("unchecked")
//                                                Map<String, Object> reviewData = (Map<String, Object>) review.getValue();
//                                                if (reviewData != null) {
//                                                    reviewData.remove("statusReview");
//                                                    DatabaseReference newReviewRef = myReviewDoneRef.push();
//                                                    newReviewRef.setValue(reviewData)
//                                                            .addOnCompleteListener(task -> {
//                                                                if (task.isSuccessful()) {
//                                                                    Log.d("CopyReview", "Review copied to MyReviewDone.");
//                                                                } else {
//                                                                    Log.e("CopyReview", "Failed to copy review", task.getException());
//                                                                }
//                                                            });
//                                                }
//                                            }
//                                            // After copying reviews, delete original data.
//                                            myReviewRef.removeValue()
//                                                    .addOnCompleteListener(deleteTask -> {
//                                                        if (deleteTask.isSuccessful()) {
//                                                            Log.d("DeleteReview", "Original MyReview data deleted.");
//                                                        } else {
//                                                            Log.e("DeleteReview", "Failed to delete MyReview.", deleteTask.getException());
//                                                        }
//                                                    });
//                                        } else {
//                                            Log.d("Review", "No MyReview data to process.");
//                                        }
//                                    }
//
//                                    @Override
//                                    public void onCancelled(@NonNull DatabaseError error) {
//                                        Log.e("Review", "Error reading MyReview data", error.toException());
//                                    }
//                                });
//
//                                // OPTIONAL: After processing, delay a bit then stop the service.
//                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                    Log.d(TAG, "Stopping foreground service due to final approval.");
//                                    // Stop the foreground service and then stop the service itself.
//                                    stopForeground(true);
//                                    stopSelf();
//                                }, 1000);
//
//                                break; // Exit loop after processing one booking.
//                            }
//                            // If Failed, similar handling could be applied.
//                            else if (finalStatus != null && finalStatus.equalsIgnoreCase("Failed") && !finalProcessed) {
//                                finalProcessed = true;
//                                showLocalNotification("Booking Failed", "Your booking has failed. Please try again.", 7);
//                                stopPolling();
//                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
//                                    Log.d(TAG, "Stopping foreground service due to booking failure.");
//                                    stopForeground(true);
//                                    stopSelf();
//                                }, 1000);
//                                break;
//                            }
//                        }
//                    }
//
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
//     * Builds and displays a local notification.
//     */
//    private void showLocalNotification(String title, String message, int notificationId) {
//        NotificationManager notificationManager =
//                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE); ///This code not replace the old notification
//
//        // Check if notification already exists (avoid duplicates)
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && notificationManager != null) {
//            StatusBarNotification[] activeNotifications = notificationManager.getActiveNotifications();
//            for (StatusBarNotification sbn : activeNotifications) {
//                if (sbn.getId() == notificationId) {
//                    Log.d(TAG, "Notification with ID " + notificationId + " is already active.");
//                    return;
//                }
//            }
//        }
//        // Recreate notification channel on Android O and above if necessary.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID,
//                    "Booking Notifications",
//                    NotificationManager.IMPORTANCE_DEFAULT);
//            channel.setDescription("Channel for booking notifications");
//            if (notificationManager != null) {
//                notificationManager.createNotificationChannel(channel);
//            }
//        }
//        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
//                .setSmallIcon(R.drawable.ic_profile_notification)
//                .setContentTitle(title)
//                .setContentText(message)
//                .setAutoCancel(true)
//                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
//                /// Ensuring it remains persistent if desired
//                .setOngoing(true);
//
//        // Post notification.
//        if (notificationManager != null) {
//            notificationManager.notify(notificationId, builder.build());
//            Log.d(TAG, "Local notification shown: " + title);
//        }
//    }
//
//    private void clearNotification(int notificationId) {
//        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//        if (notificationManager != null) {
//            notificationManager.cancel(notificationId); /// Removes only the specified notification
//            ///notificationManager.cancelAll(); /// Clears all active notifications
//        }
//    }
//
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, int startId) {
//        Log.d(TAG, "onStartCommand invoked. Service is active.");
//
//        // Check if the intent has an action to stop the foreground service.
//        if (intent != null && "STOP_FOREGROUND_SERVICE".equals(intent.getAction())) {
//            Log.d(TAG, "Stop action received. Stopping the foreground service.");
//            // Remove the persistent notification and stop the service.
//            stopForeground(true);
//            stopSelf();
//            return START_NOT_STICKY;
//        }
//
//        /// Returning START_STICKY ensures that if the service is terminated, the system
//        /// will try to re-create it as soon as possible.
//        return START_STICKY;
//    }
//
//    @Override
//    public void onDestroy() {
//        Log.d(TAG, "onDestroy called – cleaning up service and resources");
//        stopForeground(true);
//        /// Optionally, you could schedule a restart of the service here if needed using an Alarm/WorkManager,
//        /// but START_STICKY should do the job in most cases.
//        super.onDestroy();
//    }
//
//    @Nullable
//    @Override
//    public IBinder onBind(Intent intent) {
//        return null; // No binding for this service.
//    }
//}
//
//
//
//
