package com.example.resort;

import static android.content.ContentValues.TAG;

import android.app.AlertDialog;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.squareup.picasso.Picasso;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.Objects;

public class ProfileFragment extends Fragment {

    private static final int PICK_IMAGE_REQUEST = 1;
    private ImageView profileImage;
    private TextView usernameTextView, phoneNumberTextView, badgeCount;
    private DatabaseReference userRef;
    private FirebaseAuth mAuth;
    private StorageReference storageRef;

    /// Progress tracking variables.
    private int progress = 0;
    private TextView dot1, dot2, dot3, dot4, dot5;
    private View line1_2, line2_3, line3_4, line4_5;
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener progressListener;

    /// Firebase listener for booking progress changes.
    private ValueEventListener bookingProgressListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.profile_fragment, container, false);
        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());

        badgeCount = view.findViewById(R.id.badge_count);
        listenForFirebaseNotifications();

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            return view;
        }

        /// Use the current user's UID to create a user-specific SharedPreferences file.
        prefs = requireActivity().getSharedPreferences("BookingPref_" + currentUser.getUid(), Context.MODE_PRIVATE);
        ImageView notificationBtn = view.findViewById(R.id.notification);
        notificationBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Notification.class);
            startActivity(intent);
            markAllNotificationsAsRead();
        });


        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        storageRef = FirebaseStorage.getInstance().getReference("profile_images")
                .child(currentUser.getUid() + ".jpg");

        profileImage = view.findViewById(R.id.imageView);
        usernameTextView = view.findViewById(R.id.textView14);
        phoneNumberTextView = view.findViewById(R.id.textView13);

        Button next = view.findViewById(R.id.button7);
        Button next1 = view.findViewById(R.id.button6);
        Button next2 = view.findViewById(R.id.button9);
        Button next3 = view.findViewById(R.id.button10);
        Button logoutButton = view.findViewById(R.id.button11);

        loadProfileData();
        profileImage.setOnClickListener(v -> openImagePicker());

        /// ====== Added Custom Dialog for Facebook ======
        ImageView messageIcon = view.findViewById(R.id.message);
        messageIcon.setOnClickListener(v -> {
            // Inflate custom dialog layout
            LayoutInflater dlgInflater = LayoutInflater.from(getContext());
            View dialogView = dlgInflater.inflate(R.layout.custom_facebook, null);

            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setView(dialogView)
                    .setCancelable(true)
                    .create();

            // Buttons inside custom layout
            Button btnProceed = dialogView.findViewById(R.id.button_proceed);
            Button btnCancel = dialogView.findViewById(R.id.button_cancel);

            btnProceed.setOnClickListener(b -> {
                String fbUrl = "https://www.facebook.com/profile.php?id=100063953257035";
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fbUrl)));
                dialog.dismiss();
            });
            btnCancel.setOnClickListener(b -> dialog.dismiss());

            dialog.show();
        });
        /// ====== End Custom Dialog ======


        next3.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), AboutUs.class));
            requireActivity().overridePendingTransition(0, 0);
        });
        next.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), Account.class));
            requireActivity().overridePendingTransition(0, 0);
        });
        next1.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), BookingStatus.class));
            requireActivity().overridePendingTransition(0, 0);
        });
        next2.setOnClickListener(v -> {
            startActivity(new Intent(getActivity(), Feedback.class));
            requireActivity().overridePendingTransition(0, 0);
        });


        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());

        // Initialize progress dots and lines (ensure these IDs exist in profile_fragment.xml)
        dot1 = view.findViewById(R.id.dot1);
        dot2 = view.findViewById(R.id.dot2);
        dot3 = view.findViewById(R.id.dot3);
        dot4 = view.findViewById(R.id.dot4);
        dot5 = view.findViewById(R.id.dot5);
        line1_2 = view.findViewById(R.id.line1);
        line2_3 = view.findViewById(R.id.line2);
        line3_4 = view.findViewById(R.id.line3);
        line4_5 = view.findViewById(R.id.line4);


        // Read the saved booking progress from SharedPreferences.
        int storedProgress = prefs.getInt("bookingProgress", 0);
        progress = prefs.getBoolean("bookingComplete", false) ? 5 : storedProgress;
        updateDots();

        // Listen for changes in SharedPreferences to update the progress UI.
        progressListener = (sharedPreferences, key) -> {
            if ("bookingProgress".equals(key)) {
                int newStoredProgress = prefs.getBoolean("bookingComplete", false) ? 5 : sharedPreferences.getInt("bookingProgress", 0);
                // Only update UI if there’s an actual change to avoid blinking.
                if (newStoredProgress != progress) {
                    progress = newStoredProgress;
                    updateDots();
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(progressListener);

        /// --- Firebase listener to update booking progress directly ---
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        DatabaseReference bookingRef = userRef.child("MyBooking");
        bookingProgressListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int calculatedProgress = 0;

                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
                    /// Retrieve booking review status.
                    String statusReview = bookingSnapshot.child("bookingReview")
                            .child("statusReview")
                            .getValue(String.class);

                    /// Skip declined bookings.
                    if (statusReview != null && statusReview.equalsIgnoreCase("Declined")) {
                        ///continue;  ///this not use
                        calculatedProgress = Math.max(calculatedProgress, 2);
                    }

                    /// Booking submission exists: progress at least 1.
                    calculatedProgress = Math.max(calculatedProgress, 1);

                    /// If booking review is approved, upgrade progress to at least 2.
                    if (statusReview != null && statusReview.equalsIgnoreCase("Approved")) {
                        calculatedProgress = Math.max(calculatedProgress, 2);
                    }


                    // Check if paymentMethod Status is "Done" and set progress to 3.
                    String paymentMethodStatus = bookingSnapshot.child("paymentMethod")
                            .child("Status")
                            .getValue(String.class);
                    if (paymentMethodStatus != null && paymentMethodStatus.equalsIgnoreCase("Done")) {
                        calculatedProgress = Math.max(calculatedProgress, 3);
                    }


                    // Payment transaction approval sets progress to 4.
                    if (bookingSnapshot.child("paymentTransaction").exists()) {
                        String paymentStatus = bookingSnapshot.child("paymentTransaction")
                                .child("paymentStatus")
                                .getValue(String.class);
                        if (paymentStatus != null) {
                            if (paymentStatus.equalsIgnoreCase("Approved")) {
                                calculatedProgress = 4;
                            } else if (paymentStatus.equalsIgnoreCase("Refund")) {
                                calculatedProgress = 0;
                            }
                        }
                    }

                    /// Final payment approval locks progress at 5.
                    if (bookingSnapshot.child("paymentTransaction").exists()) {
                        String finalStatus = bookingSnapshot.child("paymentTransaction")
                                .child("finalStatus")
                                .getValue(String.class);
                        if (finalStatus != null && finalStatus.equalsIgnoreCase("Approved")) {
                            calculatedProgress = 5;
                        }
                    }
                }


                /// Always update the UI and SharedPreferences if there's a change in progress
                if (calculatedProgress != progress) {
                    progress = calculatedProgress;
                    updateDots();
                    prefs.edit().putInt("bookingProgress", progress).apply();

                    if (progress == 5) {
                        prefs.edit().putBoolean("bookingComplete", true).apply();
                        clearBookingPreferences();
                    } else {
                        prefs.edit().putBoolean("bookingComplete", false).apply();
                    }
                }

            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Error reading booking data", error.toException());
            }
        };
        bookingRef.addValueEventListener(bookingProgressListener);
        return view;
    }



    private void clearBookingPreferences() {
        prefs.edit().clear().apply();
        prefs.edit().remove("bookingSubmitted")
                .remove("bookingProgress")
                .remove("bookingComplete")
                .remove("paymentSubmitted")
                .remove("paymentSubmittedTime")
                .remove("paymentApproved")
                .remove("reviewApproved")
                .remove("submissionTime")
                .remove("finalApproved")
                .remove("bookingId")
                .clear()
                .apply();
    }

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
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        prefs.unregisterOnSharedPreferenceChangeListener(progressListener);
        /// Remove the Firebase listener for booking progress updates.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null && bookingProgressListener != null) {
            DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("MyBooking");
            bookingRef.removeEventListener(bookingProgressListener);
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            if (imageUri != null) {
                uploadImageToFirebaseStorage(imageUri);
            }
        }
    }


    private void uploadImageToFirebaseStorage(Uri imageUri) {
        storageRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot ->
                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                            String downloadUrl = uri.toString();
                            userRef.child("imageUrl").setValue(downloadUrl)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(getActivity(), "Image updated!", Toast.LENGTH_SHORT).show();
                                        Picasso.get().load(downloadUrl).into(profileImage);
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(getActivity(), "Failed to update image URL", Toast.LENGTH_SHORT).show()
                                    );
                        }).addOnFailureListener(e ->
                                Toast.makeText(getActivity(), "Failed to get download URL", Toast.LENGTH_SHORT).show()
                        )
                )
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }


    /// Load profile data including username , number and profile image from Firebase Storage
    private String cachedImageUrl = null;
    private String cachedUsername = null;
    private String cachedPhoneNumber = null;
    private ValueEventListener profileListener;

    private void loadProfileData() {
        // Attach persistent listener only once
        if (profileListener == null) {
            profileListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Retrieve new data from Firebase
                        String newImageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                        String newUsername = dataSnapshot.child("username").getValue(String.class);
                        String newPhoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);

                        // Update profile image if the URL has changed
                        if (!Objects.equals(newImageUrl, cachedImageUrl)) {
                            cachedImageUrl = newImageUrl;
                            if (newImageUrl != null && !newImageUrl.equals("default") && !newImageUrl.isEmpty()) {
                                Picasso.get()
                                        .load(newImageUrl)
                                        .placeholder(R.drawable.profile)
                                        .into(profileImage);
                            } else {
                                profileImage.setImageResource(R.drawable.profile);
                            }
                        }

                        // Update username if it has changed
                        if (!Objects.equals(newUsername, cachedUsername)) {
                            cachedUsername = newUsername;
                            if (newUsername != null) {
                                usernameTextView.setText(newUsername);
                            }
                        }

                        // Update phone number if it has changed
                        if (!Objects.equals(newPhoneNumber, cachedPhoneNumber)) {
                            cachedPhoneNumber = newPhoneNumber;
                            if (newPhoneNumber != null) {
                                phoneNumberTextView.setText(newPhoneNumber);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Use getActivity() if in a Fragment; otherwise use 'this' or the Activity context.
                    Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
                }
            };

            // Attach the listener persistently so it monitors for changes.
            userRef.addValueEventListener(profileListener);
        }
    }



    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        /// Inflate the custom layout
        View customView = getLayoutInflater().inflate(R.layout.custom_logout_dialog, null);
        builder.setView(customView);

        /// Create the dialog and make it non-cancelable
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        /// Get references to the UI elements in the custom layout
        Button btnCancel = customView.findViewById(R.id.btnCancel);
        Button btnExit = customView.findViewById(R.id.btnExit);

        /// Set click listener for Cancel button: dismiss the dialog.
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        /// Set click listener for Exit button: perform logout and dismiss the dialog.
        btnExit.setOnClickListener(v -> {
            logoutUser();  // Your logoutUser() method should handle logout actions.
            dialog.dismiss();
        });

        // Show the dialog.
        dialog.show();
    }


    private void logoutUser() {
        userRef.child("isOnline").setValue(false)
                .addOnSuccessListener(aVoid -> {
                    mAuth.signOut();
                    Toast.makeText(getActivity(), "Logged out successfully", Toast.LENGTH_SHORT).show();
                    redirectToLoginScreen();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getActivity(), "Failed to update login status", Toast.LENGTH_SHORT).show()
                );
    }

    private void redirectToLoginScreen() {
        Intent intent = new Intent(getActivity(), Login.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().overridePendingTransition(0, 0);
        requireActivity().finish();
    }

    private void listenForFirebaseNotifications() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("notifications");

        notificationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int notificationCount = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean read = child.child("read").getValue(Boolean.class);
                    Boolean forceShow = child.child("forceShow").getValue(Boolean.class);
                    /// For notifications with forceShow true, always count if they are not read.
                    if (forceShow != null && forceShow) {
                        if (read == null || !read) {
                            notificationCount++;
                        }
                    } else {
                        /// For non-approved notifications, simply count if not read.
                        if (read == null || !read) {
                            notificationCount++;
                        }
                    }
                }
                updateBadgeCount(notificationCount);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void updateBadgeCount(int count) {
        if (count > 0) {
            badgeCount.setText(String.valueOf(count));
            badgeCount.setVisibility(View.VISIBLE);
        } else {
            badgeCount.setVisibility(View.GONE);
        }
    }

    private void markAllNotificationsAsRead() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("notifications");
        notificationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    child.getRef().child("read").setValue(true);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

}

///No Current
//package com.example.resort;
//
//import static android.content.ContentValues.TAG;
//
//import android.app.AlertDialog;
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.net.Uri;
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Looper;
//import android.preference.PreferenceManager;
//import android.provider.MediaStore;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//
//import com.squareup.picasso.Picasso;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.*;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//
//import java.util.Objects;
//
//public class ProfileFragment extends Fragment {
//
//    private static final int PICK_IMAGE_REQUEST = 1;
//    private ImageView profileImage;
//    private TextView usernameTextView, phoneNumberTextView, badgeCount;
//    private DatabaseReference userRef;
//    private FirebaseAuth mAuth;
//    private StorageReference storageRef;
//
//    /// Progress tracking variables.
//    private int progress = 0;
//    private TextView dot1, dot2, dot3, dot4, dot5;
//    private View line1_2, line2_3, line3_4, line4_5;
//    private SharedPreferences prefs;
//    private SharedPreferences.OnSharedPreferenceChangeListener progressListener;
//
//    /// Firebase listener for booking progress changes.
//    private ValueEventListener bookingProgressListener;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.profile_fragment, container, false);
//        prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
//
//        badgeCount = view.findViewById(R.id.badge_count);
//        listenForFirebaseNotifications();
//
//        mAuth = FirebaseAuth.getInstance();
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if (currentUser == null) {
//            return view;
//        }
//
//        /// Use the current user's UID to create a user-specific SharedPreferences file.
//        prefs = requireActivity().getSharedPreferences("BookingPref_" + currentUser.getUid(), Context.MODE_PRIVATE);
//        ImageView notificationBtn = view.findViewById(R.id.notification);
//        notificationBtn.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Notification.class);
//            startActivity(intent);
//            markAllNotificationsAsRead();
//        });
//
//
//        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
//        storageRef = FirebaseStorage.getInstance().getReference("profile_images")
//                .child(currentUser.getUid() + ".jpg");
//
//        profileImage = view.findViewById(R.id.imageView);
//        usernameTextView = view.findViewById(R.id.textView14);
//        phoneNumberTextView = view.findViewById(R.id.textView13);
//
//        Button next = view.findViewById(R.id.button7);
//        Button next1 = view.findViewById(R.id.button6);
//        Button next2 = view.findViewById(R.id.button9);
//        Button next3 = view.findViewById(R.id.button10);
//        Button logoutButton = view.findViewById(R.id.button11);
//
//        loadProfileData();
//        profileImage.setOnClickListener(v -> openImagePicker());
//
//        /// ====== Added Custom Dialog for Facebook ======
//        ImageView messageIcon = view.findViewById(R.id.message);
//        messageIcon.setOnClickListener(v -> {
//            // Inflate custom dialog layout
//            LayoutInflater dlgInflater = LayoutInflater.from(getContext());
//            View dialogView = dlgInflater.inflate(R.layout.custom_facebook, null);
//
//            AlertDialog dialog = new AlertDialog.Builder(requireContext())
//                    .setView(dialogView)
//                    .setCancelable(true)
//                    .create();
//
//            // Buttons inside custom layout
//            Button btnProceed = dialogView.findViewById(R.id.button_proceed);
//            Button btnCancel = dialogView.findViewById(R.id.button_cancel);
//
//            btnProceed.setOnClickListener(b -> {
//                String fbUrl = "https://www.facebook.com/profile.php?id=100063953257035";
//                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(fbUrl)));
//                dialog.dismiss();
//            });
//            btnCancel.setOnClickListener(b -> dialog.dismiss());
//
//            dialog.show();
//        });
//        /// ====== End Custom Dialog ======
//
//
//        next3.setOnClickListener(v -> {
//            startActivity(new Intent(getActivity(), AboutUs.class));
//            requireActivity().overridePendingTransition(0, 0);
//        });
//        next.setOnClickListener(v -> {
//            startActivity(new Intent(getActivity(), Account.class));
//            requireActivity().overridePendingTransition(0, 0);
//        });
//        next1.setOnClickListener(v -> {
//            startActivity(new Intent(getActivity(), BookingStatus.class));
//            requireActivity().overridePendingTransition(0, 0);
//        });
//        next2.setOnClickListener(v -> {
//            startActivity(new Intent(getActivity(), Feedback.class));
//            requireActivity().overridePendingTransition(0, 0);
//        });
//
//
//        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
//
//        // Initialize progress dots and lines (ensure these IDs exist in profile_fragment.xml)
//        dot1 = view.findViewById(R.id.dot1);
//        dot2 = view.findViewById(R.id.dot2);
//        dot3 = view.findViewById(R.id.dot3);
//        dot4 = view.findViewById(R.id.dot4);
//        dot5 = view.findViewById(R.id.dot5);
//        line1_2 = view.findViewById(R.id.line1);
//        line2_3 = view.findViewById(R.id.line2);
//        line3_4 = view.findViewById(R.id.line3);
//        line4_5 = view.findViewById(R.id.line4);
//
//
//        // Read the saved booking progress from SharedPreferences.
//        int storedProgress = prefs.getInt("bookingProgress", 0);
//        progress = prefs.getBoolean("bookingComplete", false) ? 5 : storedProgress;
//        updateDots();
//
//        // Listen for changes in SharedPreferences to update the progress UI.
//        progressListener = (sharedPreferences, key) -> {
//            if ("bookingProgress".equals(key)) {
//                int newStoredProgress = prefs.getBoolean("bookingComplete", false) ? 5 : sharedPreferences.getInt("bookingProgress", 0);
//                // Only update UI if there’s an actual change to avoid blinking.
//                if (newStoredProgress != progress) {
//                    progress = newStoredProgress;
//                    updateDots();
//                }
//            }
//        };
//        prefs.registerOnSharedPreferenceChangeListener(progressListener);
//
//        /// --- Firebase listener to update booking progress directly ---
//        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
//        DatabaseReference bookingRef = userRef.child("MyBooking");
//        bookingProgressListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                int calculatedProgress = 0;
//
//                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                    // Retrieve booking review status.
//                    String statusReview = bookingSnapshot.child("bookingReview")
//                            .child("statusReview")
//                            .getValue(String.class);
//
//                    // Skip declined bookings.
//                    if (statusReview != null && statusReview.equalsIgnoreCase("Declined")) {
//                        continue;
//                    }
//
//                    // Booking submission exists: progress at least 1.
//                    calculatedProgress = Math.max(calculatedProgress, 1);
//
//                    // If booking review is approved, upgrade progress to at least 2.
//                    if (statusReview != null && statusReview.equalsIgnoreCase("Approved")) {
//                        calculatedProgress = Math.max(calculatedProgress, 2);
//                    }
//
//
//                    // Check if paymentMethod Status is "Done" and set progress to 3.
//                    String paymentMethodStatus = bookingSnapshot.child("paymentMethod")
//                            .child("Status")
//                            .getValue(String.class);
//                    if (paymentMethodStatus != null && paymentMethodStatus.equalsIgnoreCase("Done")) {
//                        calculatedProgress = Math.max(calculatedProgress, 3);
//                    }
//
//
//                    // Payment transaction approval sets progress to 4.
//                    if (bookingSnapshot.child("paymentTransaction").exists()) {
//                        String paymentStatus = bookingSnapshot.child("paymentTransaction")
//                                .child("paymentStatus")
//                                .getValue(String.class);
//                        if (paymentStatus != null) {
//                            if (paymentStatus.equalsIgnoreCase("Approved")) {
//                                calculatedProgress = 4;
//                            } else if (paymentStatus.equalsIgnoreCase("Refund")) {
//                                calculatedProgress = 0;
//                            }
//                        }
//                    }
//
//                    /// Final payment approval locks progress at 5.
//                    if (bookingSnapshot.child("paymentTransaction").exists()) {
//                        String finalStatus = bookingSnapshot.child("paymentTransaction")
//                                .child("finalStatus")
//                                .getValue(String.class);
//                        if (finalStatus != null && finalStatus.equalsIgnoreCase("Approved")) {
//                            calculatedProgress = 5;
//                        }
//                    }
//                }
//
//
//                /// Always update the UI and SharedPreferences if there's a change in progress
//                if (calculatedProgress != progress) {
//                    progress = calculatedProgress;
//                    updateDots();
//                    prefs.edit().putInt("bookingProgress", progress).apply();
//
//                    if (progress == 5) {
//                        prefs.edit().putBoolean("bookingComplete", true).apply();
//                        clearBookingPreferences();
//                    } else {
//                        prefs.edit().putBoolean("bookingComplete", false).apply();
//                    }
//                }
//
//            }
//
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e(TAG, "Error reading booking data", error.toException());
//            }
//        };
//        bookingRef.addValueEventListener(bookingProgressListener);
//        return view;
//    }
//
//
//
//    private void clearBookingPreferences() {
//        prefs.edit().clear().apply();
//        prefs.edit().remove("bookingSubmitted")
//                .remove("bookingProgress")
//                .remove("bookingComplete")
//                .remove("paymentSubmitted")
//                .remove("paymentSubmittedTime")
//                .remove("paymentApproved")
//                .remove("reviewApproved")
//                .remove("submissionTime")
//                .remove("finalApproved")
//                .remove("bookingId")
//                .clear()
//                .apply();
//    }
//
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
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        prefs.unregisterOnSharedPreferenceChangeListener(progressListener);
//        /// Remove the Firebase listener for booking progress updates.
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if (currentUser != null && bookingProgressListener != null) {
//            DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                    .getReference("users")
//                    .child(currentUser.getUid())
//                    .child("MyBooking");
//            bookingRef.removeEventListener(bookingProgressListener);
//        }
//    }
//
//    private void openImagePicker() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(intent, PICK_IMAGE_REQUEST);
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
//            Uri imageUri = data.getData();
//            if (imageUri != null) {
//                uploadImageToFirebaseStorage(imageUri);
//            }
//        }
//    }
//
//
//    private void uploadImageToFirebaseStorage(Uri imageUri) {
//        storageRef.putFile(imageUri)
//                .addOnSuccessListener(taskSnapshot ->
//                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
//                            String downloadUrl = uri.toString();
//                            userRef.child("imageUrl").setValue(downloadUrl)
//                                    .addOnSuccessListener(aVoid -> {
//                                        Toast.makeText(getActivity(), "Image updated!", Toast.LENGTH_SHORT).show();
//                                        Picasso.get().load(downloadUrl).into(profileImage);
//                                    })
//                                    .addOnFailureListener(e ->
//                                            Toast.makeText(getActivity(), "Failed to update image URL", Toast.LENGTH_SHORT).show()
//                                    );
//                        }).addOnFailureListener(e ->
//                                Toast.makeText(getActivity(), "Failed to get download URL", Toast.LENGTH_SHORT).show()
//                        )
//                )
//                .addOnFailureListener(e ->
//                        Toast.makeText(getActivity(), "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
//                );
//    }
//
//
//    /// Load profile data including username , number and profile image from Firebase Storage
//    private String cachedImageUrl = null;
//    private String cachedUsername = null;
//    private String cachedPhoneNumber = null;
//    private ValueEventListener profileListener;
//
//    private void loadProfileData() {
//        // Attach persistent listener only once
//        if (profileListener == null) {
//            profileListener = new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                    if (dataSnapshot.exists()) {
//                        // Retrieve new data from Firebase
//                        String newImageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
//                        String newUsername = dataSnapshot.child("username").getValue(String.class);
//                        String newPhoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
//
//                        // Update profile image if the URL has changed
//                        if (!Objects.equals(newImageUrl, cachedImageUrl)) {
//                            cachedImageUrl = newImageUrl;
//                            if (newImageUrl != null && !newImageUrl.equals("default") && !newImageUrl.isEmpty()) {
//                                Picasso.get()
//                                        .load(newImageUrl)
//                                        .placeholder(R.drawable.profile)
//                                        .into(profileImage);
//                            } else {
//                                profileImage.setImageResource(R.drawable.profile);
//                            }
//                        }
//
//                        // Update username if it has changed
//                        if (!Objects.equals(newUsername, cachedUsername)) {
//                            cachedUsername = newUsername;
//                            if (newUsername != null) {
//                                usernameTextView.setText(newUsername);
//                            }
//                        }
//
//                        // Update phone number if it has changed
//                        if (!Objects.equals(newPhoneNumber, cachedPhoneNumber)) {
//                            cachedPhoneNumber = newPhoneNumber;
//                            if (newPhoneNumber != null) {
//                                phoneNumberTextView.setText(newPhoneNumber);
//                            }
//                        }
//                    }
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError databaseError) {
//                    // Use getActivity() if in a Fragment; otherwise use 'this' or the Activity context.
//                    Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
//                }
//            };
//
//            // Attach the listener persistently so it monitors for changes.
//            userRef.addValueEventListener(profileListener);
//        }
//    }
//
//
//
//    private void showLogoutConfirmationDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        /// Inflate the custom layout
//        View customView = getLayoutInflater().inflate(R.layout.custom_logout_dialog, null);
//        builder.setView(customView);
//
//        /// Create the dialog and make it non-cancelable
//        AlertDialog dialog = builder.create();
//        dialog.setCancelable(false);
//
//        /// Get references to the UI elements in the custom layout
//        Button btnCancel = customView.findViewById(R.id.btnCancel);
//        Button btnExit = customView.findViewById(R.id.btnExit);
//
//        /// Set click listener for Cancel button: dismiss the dialog.
//        btnCancel.setOnClickListener(v -> dialog.dismiss());
//
//        /// Set click listener for Exit button: perform logout and dismiss the dialog.
//        btnExit.setOnClickListener(v -> {
//            logoutUser();  // Your logoutUser() method should handle logout actions.
//            dialog.dismiss();
//        });
//
//        // Show the dialog.
//        dialog.show();
//    }
//
//
//    private void logoutUser() {
//        userRef.child("isOnline").setValue(false)
//                .addOnSuccessListener(aVoid -> {
//                    mAuth.signOut();
//                    Toast.makeText(getActivity(), "Logged out successfully", Toast.LENGTH_SHORT).show();
//                    redirectToLoginScreen();
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(getActivity(), "Failed to update login status", Toast.LENGTH_SHORT).show()
//         );
//    }
//
//    private void redirectToLoginScreen() {
//        Intent intent = new Intent(getActivity(), Login.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);
//        requireActivity().overridePendingTransition(0, 0);
//        requireActivity().finish();
//    }
//
//    private void listenForFirebaseNotifications() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("notifications");
//
//        notificationRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                int notificationCount = 0;
//                for (DataSnapshot child : snapshot.getChildren()) {
//                    Boolean read = child.child("read").getValue(Boolean.class);
//                    Boolean forceShow = child.child("forceShow").getValue(Boolean.class);
//                    /// For notifications with forceShow true, always count if they are not read.
//                    if (forceShow != null && forceShow) {
//                        if (read == null || !read) {
//                            notificationCount++;
//                        }
//                    } else {
//                        /// For non-approved notifications, simply count if not read.
//                        if (read == null || !read) {
//                            notificationCount++;
//                        }
//                    }
//                }
//                updateBadgeCount(notificationCount);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) { }
//        });
//    }
//
//    private void updateBadgeCount(int count) {
//        if (count > 0) {
//            badgeCount.setText(String.valueOf(count));
//            badgeCount.setVisibility(View.VISIBLE);
//        } else {
//            badgeCount.setVisibility(View.GONE);
//        }
//    }
//
//    private void markAllNotificationsAsRead() {
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
//                    child.getRef().child("read").setValue(true);
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) { }
//        });
//    }
//
//}
