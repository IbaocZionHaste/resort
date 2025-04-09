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
        Button next4 = view.findViewById(R.id.history);
        Button logoutButton = view.findViewById(R.id.button11);

        loadProfileData();
        profileImage.setOnClickListener(v -> openImagePicker());


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
                    // Retrieve booking review status.
                    String statusReview = bookingSnapshot.child("bookingReview")
                            .child("statusReview")
                            .getValue(String.class);

                    // Skip declined bookings.
                    if (statusReview != null && statusReview.equalsIgnoreCase("Declined")) {
                        continue;
                    }

                    // Booking submission exists: progress at least 1.
                    calculatedProgress = Math.max(calculatedProgress, 1);

                    // If booking review is approved, upgrade progress to at least 2.
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

/// --- Update only if there's an actual improvement ---
//                if (calculatedProgress > progress) {
//                    progress = calculatedProgress;
//                    updateDots();
//                    prefs.edit().putInt("bookingProgress", progress).apply();
//                    if (progress == 5) {
//                        prefs.edit().putBoolean("bookingComplete", true).apply();
//                        clearBookingPreferences();
//                    }
//                }

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
                //.remove("paymentSubmittedTime")
                .remove("paymentApproved")
                .remove("reviewApproved")
                //.remove("submissionTime")
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
        // Remove the Firebase listener for booking progress updates.
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

    private void loadProfileData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                    if (imageUrl != null && !imageUrl.equals("default") && !imageUrl.isEmpty()) {
                        Picasso.get()
                                .load(imageUrl)
                                .placeholder(R.drawable.profile)
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.profile);
                    }
                    String username = dataSnapshot.child("username").getValue(String.class);
                    String phoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
                    if (username != null) {
                        usernameTextView.setText(username);
                    }
                    if (phoneNumber != null) {
                        phoneNumberTextView.setText(phoneNumber);
                    }
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLogoutConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        // Inflate the custom layout
        View customView = getLayoutInflater().inflate(R.layout.custom_logout_dialog, null);
        builder.setView(customView);

        // Create the dialog and make it non-cancelable
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        // Get references to the UI elements in the custom layout
        Button btnCancel = customView.findViewById(R.id.btnCancel);
        Button btnExit = customView.findViewById(R.id.btnExit);

        // Set click listener for Cancel button: dismiss the dialog.
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Set click listener for Exit button: perform logout and dismiss the dialog.
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
                    // For notifications with forceShow true, always count if they are not read.
                    if (forceShow != null && forceShow) {
                        if (read == null || !read) {
                            notificationCount++;
                        }
                    } else {
                        // For non-approved notifications, simply count if not read.
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




///Payment submission bumps progress to 3.
//                    if (bookingSnapshot.hasChild("paymentSubmitted")) {
//                        calculatedProgress = Math.max(calculatedProgress, 3);
//                    }


/// THIS DATA FOR THE SHORTCUT PROGRESS IN THE BOOKING STATUS (Profile Update)
//        // Read the saved booking progress from SharedPreferences.
//
//        int storedProgress = prefs.getInt("bookingProgress", 0);
//        progress = prefs.getBoolean("bookingComplete", false) ? 5 : storedProgress;
//        updateDots();
//
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
//        // Attach a persistent Firebase listener to the user's "MyBooking" node.
//        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
//        DatabaseReference bookingRef = userRef.child("MyBooking");
//        bookingProgressListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                // Calculate progress based on valid (non-declined) bookings.
//                int calculatedProgress = 0;
//
//                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                    // Retrieve booking review status.
//                    String statusReview = bookingSnapshot.child("bookingReview")
//                            .child("statusReview")
//                            .getValue(String.class);
//
//                    // If this booking is declined, skip it entirely.
//                    if (statusReview != null && statusReview.equalsIgnoreCase("Declined")) {
//                        continue;
//                    }
//
//                    // Booking submission exists: set progress to at least 1.
//                    calculatedProgress = Math.max(calculatedProgress, 1);
//
//                    // If booking review is approved, upgrade progress to at least 2.
//                    if (statusReview != null && statusReview.equalsIgnoreCase("Approved")) {
//                        calculatedProgress = Math.max(calculatedProgress, 2);
//                    }
//
//                    // Payment submission bumps progress to 3.
//                    if (bookingSnapshot.hasChild("paymentSubmitted")) {
//                        calculatedProgress = Math.max(calculatedProgress, 3);
//                    }
//
//                    // Check paymentTransaction for approval states.
//                    if (bookingSnapshot.child("paymentTransaction").exists()) {
//                        // If paymentStatus is approved, consider that progress 4.
//                        String paymentStatus = bookingSnapshot.child("paymentTransaction")
//                                .child("paymentStatus")
//                                .getValue(String.class);
//                        if (paymentStatus != null && paymentStatus.equalsIgnoreCase("Approved")) {
//                            calculatedProgress = Math.max(calculatedProgress, 4);
//                        }
//                    }
//
//                    // Final payment approval locks progress at 5.
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
//                // Ensure we never lower progress: store the highest achieved value.
//                int storedProgress = prefs.getInt("bookingProgress", 0);
//                int newProgress = Math.max(storedProgress, calculatedProgress);
//
//                // Lock in the final state if progress reaches 5.
//                if (newProgress == 5) {
//                    prefs.edit().putBoolean("bookingComplete", true).apply();
//                }
//                // Only update SharedPreferences if there's an improvement.
//                if (newProgress != storedProgress) {
//                    prefs.edit().putInt("bookingProgress", newProgress).apply();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Log.e(TAG, "Error reading booking data", error.toException());
//            }
//        };
//        bookingRef.addValueEventListener(bookingProgressListener);
//        return view;
//    }

//No Get Current User
//package com.example.resort;
//
//import android.app.AlertDialog;
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
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
//    // Progress tracking variables.
//    private int progress = 0;
//    private TextView dot1, dot2, dot3, dot4, dot5;
//    private View line1_2, line2_3, line3_4, line4_5;
//    private SharedPreferences prefs;
//    private SharedPreferences.OnSharedPreferenceChangeListener progressListener;
//
//    // Firebase listener for booking progress changes.
//    private ValueEventListener bookingProgressListener;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.profile_fragment, container, false);
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
//        ImageView notificationBtn = view.findViewById(R.id.notification);
//        notificationBtn.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Notification.class);
//            startActivity(intent);
//            markAllNotificationsAsRead();
//        });
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
//
//        profileImage.setOnClickListener(v -> openImagePicker());
//
//        next3.setOnClickListener(v -> startActivity(new Intent(getActivity(), AboutUs.class)));
//        next.setOnClickListener(v -> startActivity(new Intent(getActivity(), Account.class)));
//        next1.setOnClickListener(v -> startActivity(new Intent(getActivity(), BookingStatus.class)));
//        next2.setOnClickListener(v -> startActivity(new Intent(getActivity(), Feedback.class)));
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
//        prefs = requireActivity().getSharedPreferences("BookingPrefs", Context.MODE_PRIVATE);
//        progress = prefs.getInt("bookingProgress", 0);
//        if (prefs.getBoolean("bookingComplete", false)) {
//            progress = 5;
//        }
//        updateDots();
//
//
//        // Listen for changes in SharedPreferences (for when BookingStatus updates progress)
//        progressListener = (sharedPreferences, key) -> {
//            if ("bookingProgress".equals(key)) {
//                progress = sharedPreferences.getInt("bookingProgress", 0);
//                updateDots();
//            }
//        };
//        prefs.registerOnSharedPreferenceChangeListener(progressListener);
//
//        // Add a persistent Firebase listener for booking progress updates.
//        DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(currentUser.getUid())
//                .child("MyBooking");
//
//        bookingProgressListener = new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                int newProgress = 0;
//                // Loop through each booking to determine the highest progress achieved.
//                for (DataSnapshot bookingSnapshot : snapshot.getChildren()) {
//                    // Booking submitted always counts as progress >= 1.
//                    newProgress = Math.max(newProgress, 1);
//
//                    // Check booking review approval.
//                    String statusReview = bookingSnapshot.child("bookingReview")
//                            .child("statusReview")
//                            .getValue(String.class);
//                    if (statusReview != null && statusReview.equalsIgnoreCase("Approved")) {
//                        newProgress = Math.max(newProgress, 2);
//                    }
//
//                    // Check for payment submission.
//                    if (bookingSnapshot.hasChild("paymentSubmitted")) {
//                        newProgress = Math.max(newProgress, 3);
//                    }
//
//                    // Check for payment transaction approval.
//                    if (bookingSnapshot.child("paymentTransaction").exists()) {
//                        String paymentStatus = bookingSnapshot.child("paymentTransaction")
//                                .child("paymentStatus")
//                                .getValue(String.class);
//                        if (paymentStatus != null && paymentStatus.equalsIgnoreCase("Approved")) {
//                            newProgress = Math.max(newProgress, 4);
//                        }
//                    }
//
//                    // If payment is approved, simulate final approval.
//                    // You can modify this logic based on your exact requirements.
//                    if (newProgress >= 4) {
//                        newProgress = 5;
//                    }
//                }
//
//                // If progress changed, update the UI and shared preferences.
//                if (newProgress != progress) {
//                    progress = newProgress;
//                    updateDots();
//                    prefs.edit().putInt("bookingProgress", progress).apply();
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                // Handle potential errors.
//            }
//        };
//        bookingRef.addValueEventListener(bookingProgressListener);
//
//        return view;
//    }
//
//    private void updateDots() {
//        if (progress >= 1) dot1.setBackgroundResource(R.drawable.drawable_dot_color);
//        else dot1.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 2) dot2.setBackgroundResource(R.drawable.drawable_dot_color);
//        else dot2.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 3) dot3.setBackgroundResource(R.drawable.drawable_dot_color);
//        else dot3.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 4) dot4.setBackgroundResource(R.drawable.drawable_dot_color);
//        else dot4.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 5) dot5.setBackgroundResource(R.drawable.drawable_dot_color);
//        else dot5.setBackgroundResource(R.drawable.drawable_dot_clear);
//
//        if (progress >= 1) line1_2.setBackgroundResource(R.drawable.drawable_dot_color);
//        else line1_2.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 2) line2_3.setBackgroundResource(R.drawable.drawable_dot_color);
//        else line2_3.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 3) line3_4.setBackgroundResource(R.drawable.drawable_dot_color);
//        else line3_4.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 4) line4_5.setBackgroundResource(R.drawable.drawable_dot_color);
//        else line4_5.setBackgroundResource(R.drawable.drawable_dot_clear);
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        prefs.unregisterOnSharedPreferenceChangeListener(progressListener);
//        // Remove the Firebase listener for booking progress updates.
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
//    private void loadProfileData() {
//        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
//                    if (imageUrl != null && !imageUrl.equals("default") && !imageUrl.isEmpty()) {
//                        Picasso.get()
//                                .load(imageUrl)
//                                .placeholder(R.drawable.profile)
//                                .into(profileImage);
//                    } else {
//                        profileImage.setImageResource(R.drawable.profile);
//                    }
//                    String username = dataSnapshot.child("username").getValue(String.class);
//                    String phoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
//                    if (username != null) {
//                        usernameTextView.setText(username);
//                    }
//                    if (phoneNumber != null) {
//                        phoneNumberTextView.setText(phoneNumber);
//                    }
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void showLogoutConfirmationDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setTitle("Logout");
//        builder.setMessage("Do you want to exit?");
//        builder.setPositiveButton("Exit", (dialog, which) -> logoutUser());
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
//        builder.setIcon(android.R.drawable.ic_dialog_alert);
//        builder.show();
//    }
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
//                );
//    }
//
//    private void redirectToLoginScreen() {
//        Intent intent = new Intent(getActivity(), Login.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);
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
//                    if (read == null || !read) {
//                        notificationCount++;
//                    }
//                }
//                updateBadgeCount(notificationCount);
//            }
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
//
//







//NOT REALTIME UPDATE IF THE USER EXIT THE PROGRESS NOT WORK
//package com.example.resort;
//
//import android.app.AlertDialog;
//import android.app.Activity;
//import android.content.Context;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
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
//public class ProfileFragment extends Fragment {
//
//    private static final int PICK_IMAGE_REQUEST = 1;
//    private ImageView profileImage;
//    private TextView usernameTextView, phoneNumberTextView, badgeCount;
//    private DatabaseReference userRef;
//    private FirebaseAuth mAuth;
//    private StorageReference storageRef;
//
//    // Progress tracking variables.
//    private int progress = 0;
//    private TextView dot1, dot2, dot3, dot4, dot5;
//    private View line1_2, line2_3, line3_4, line4_5;
//    private SharedPreferences prefs;
//    private SharedPreferences.OnSharedPreferenceChangeListener progressListener;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.profile_fragment, container, false);
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
//        ImageView notificationBtn = view.findViewById(R.id.notification);
//        notificationBtn.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Notification.class);
//            startActivity(intent);
//            markAllNotificationsAsRead();
//        });
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
//
//        profileImage.setOnClickListener(v -> openImagePicker());
//
//        next3.setOnClickListener(v -> startActivity(new Intent(getActivity(), AboutUs.class)));
//        next.setOnClickListener(v -> startActivity(new Intent(getActivity(), Account.class)));
//        next1.setOnClickListener(v -> startActivity(new Intent(getActivity(), BookingStatus.class)));
//        next2.setOnClickListener(v -> startActivity(new Intent(getActivity(), Feedback.class)));
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
//        // Get the shared booking progress.
//        prefs = getActivity().getSharedPreferences("BookingPrefs", getActivity().MODE_PRIVATE);
//        progress = prefs.getInt("bookingProgress", 0);
//        updateDots();
//
//        // Listen for changes in booking progress.
//        progressListener = (sharedPreferences, key) -> {
//            if ("bookingProgress".equals(key)) {
//                progress = sharedPreferences.getInt("bookingProgress", 0);
//                updateDots();
//            }
//        };
//        prefs.registerOnSharedPreferenceChangeListener(progressListener);
//
//        return view;
//    }
//
//    private void updateDots() {
//        if (progress >= 1) dot1.setBackgroundResource(R.drawable.drawable_dot_color);
//        else dot1.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 2) dot2.setBackgroundResource(R.drawable.drawable_dot_color);
//        else dot2.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 3) dot3.setBackgroundResource(R.drawable.drawable_dot_color);
//        else dot3.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 4) dot4.setBackgroundResource(R.drawable.drawable_dot_color);
//        else dot4.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 5) dot5.setBackgroundResource(R.drawable.drawable_dot_color);
//        else dot5.setBackgroundResource(R.drawable.drawable_dot_clear);
//
//        if (progress >= 1) line1_2.setBackgroundResource(R.drawable.drawable_dot_color);
//        else line1_2.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 2) line2_3.setBackgroundResource(R.drawable.drawable_dot_color);
//        else line2_3.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 3) line3_4.setBackgroundResource(R.drawable.drawable_dot_color);
//        else line3_4.setBackgroundResource(R.drawable.drawable_dot_clear);
//        if (progress >= 4) line4_5.setBackgroundResource(R.drawable.drawable_dot_color);
//        else line4_5.setBackgroundResource(R.drawable.drawable_dot_clear);
//    }
//
//    @Override
//    public void onDestroyView() {
//        super.onDestroyView();
//        prefs.unregisterOnSharedPreferenceChangeListener(progressListener);
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
//    private void loadProfileData() {
//        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
//                    if (imageUrl != null && !imageUrl.equals("default") && !imageUrl.isEmpty()) {
//                        Picasso.get()
//                                .load(imageUrl)
//                                .placeholder(R.drawable.profile)
//                                .into(profileImage);
//                    } else {
//                        profileImage.setImageResource(R.drawable.profile);
//                    }
//                    String username = dataSnapshot.child("username").getValue(String.class);
//                    String phoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
//                    if (username != null) {
//                        usernameTextView.setText(username);
//                    }
//                    if (phoneNumber != null) {
//                        phoneNumberTextView.setText(phoneNumber);
//                    }
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void showLogoutConfirmationDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setTitle("Logout");
//        builder.setMessage("Do you want to exit?");
//        builder.setPositiveButton("Exit", (dialog, which) -> logoutUser());
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
//        builder.setIcon(android.R.drawable.ic_dialog_alert);
//        builder.show();
//    }
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
//                );
//    }
//
//    private void redirectToLoginScreen() {
//        Intent intent = new Intent(getActivity(), Login.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);
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
//                    if (read == null || !read) {
//                        notificationCount++;
//                    }
//                }
//                updateBadgeCount(notificationCount);
//            }
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
//}
//


//THIS NO FUNCTION OF THE PROGRESS IN THE BOOKING
//package com.example.resort;
//
//import android.app.AlertDialog;
//import android.app.Activity;
//import android.content.Intent;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
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
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
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
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.profile_fragment, container, false);
//
//        // Initialize your badge TextView and start listening for notifications from Firebase.
//        badgeCount = view.findViewById(R.id.badge_count);
//        listenForFirebaseNotifications();
//
//        // Initialize Firebase Auth and get current user
//        mAuth = FirebaseAuth.getInstance();
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if (currentUser == null) {
//            // Optionally, redirect to login if user is null.
//            return view;
//        }
//
//        // Notification button click listener
//        ImageView notificationBtn = view.findViewById(R.id.notification);
//        notificationBtn.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Notification.class);
//            startActivity(intent);
//
//            // Mark all notifications as read.
//            markAllNotificationsAsRead();
//        });
//
//        // Initialize Database reference for the current user
//        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
//
//        // Initialize Firebase Storage reference in "profile_images" folder
//        storageRef = FirebaseStorage.getInstance().getReference("profile_images")
//                .child(currentUser.getUid() + ".jpg");
//
//        // Initialize views
//        profileImage = view.findViewById(R.id.imageView);
//        usernameTextView = view.findViewById(R.id.textView14);
//        phoneNumberTextView = view.findViewById(R.id.textView13);
//
//        // Buttons for navigation and logout
//        Button next = view.findViewById(R.id.button7);
//        Button next1 = view.findViewById(R.id.button6);
//        Button next2 = view.findViewById(R.id.button9);
//        Button next3 = view.findViewById(R.id.button10);
//        Button logoutButton = view.findViewById(R.id.button11);
//
//        // Load profile data (image, username, phone number)
//        loadProfileData();
//
//        // On clicking the profile image, open the gallery for a new image
//        profileImage.setOnClickListener(v -> openImagePicker());
//
//        // Other navigation button listeners remain unchanged
//        next3.setOnClickListener(v -> startActivity(new Intent(getActivity(), AboutUs.class)));
//        next.setOnClickListener(v -> startActivity(new Intent(getActivity(), Account.class)));
//        next1.setOnClickListener(v -> startActivity(new Intent(getActivity(), BookingStatus.class)));
//        next2.setOnClickListener(v -> startActivity(new Intent(getActivity(), Feedback.class)));
//
//        // Logout button click listener
//        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
//
//        return view;
//    }
//
//
//
//    // Open gallery to pick an image
//    private void openImagePicker() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        //noinspection deprecation
//        startActivityForResult(intent, PICK_IMAGE_REQUEST);
//    }
//
//    /** @noinspection deprecation*/
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
//            Uri imageUri = data.getData();
//            if (imageUri != null) {
//                // Upload the selected image to Firebase Storage
//                uploadImageToFirebaseStorage(imageUri);
//            }
//        }
//    }
//
//    // Upload image to Firebase Storage and update the user's imageUrl in the database
//    private void uploadImageToFirebaseStorage(Uri imageUri) {
//        storageRef.putFile(imageUri)
//                .addOnSuccessListener(taskSnapshot ->
//                        storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
//                            String downloadUrl = uri.toString();
//                            // Save the download URL to the Realtime Database
//                            userRef.child("imageUrl").setValue(downloadUrl)
//                                    .addOnSuccessListener(aVoid -> {
//                                        Toast.makeText(getActivity(), "Image updated!", Toast.LENGTH_SHORT).show();
//                                        // Load the image using Picasso for immediate display
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
//    // Load profile data (image, username, phone number) from Firebase
//    private void loadProfileData() {
//        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    // Retrieve and display profile image using Picasso
//                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
//                    if (imageUrl != null && !imageUrl.equals("default") && !imageUrl.isEmpty()) {
//                        Picasso.get()
//                                .load(imageUrl)
//                                .placeholder(R.drawable.profile) // placeholder until image loads
//                                .into(profileImage);
//                    } else {
//                        profileImage.setImageResource(R.drawable.profile);
//                    }
//
//                    // Retrieve and set username and phone number
//                    String username = dataSnapshot.child("username").getValue(String.class);
//                    String phoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
//                    if (username != null) {
//                        usernameTextView.setText(username);
//                    }
//                    if (phoneNumber != null) {
//                        phoneNumberTextView.setText(phoneNumber);
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    // Display a logout confirmation dialog
//    private void showLogoutConfirmationDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        builder.setTitle("Logout");
//        builder.setMessage("Do you want to exit?");
//        builder.setPositiveButton("Exit", (dialog, which) -> logoutUser());
//        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());
//        builder.setIcon(android.R.drawable.ic_dialog_alert);
//        builder.show();
//    }
//
//    // Log out the user and update the login status in the database
//    private void logoutUser() {
//        userRef.child("isOnline").setValue(false)
//                .addOnSuccessListener(aVoid -> {
//                    mAuth.signOut();
//                    Toast.makeText(getActivity(), "Logged out successfully", Toast.LENGTH_SHORT).show();
//                    redirectToLoginScreen();
//                })
//                .addOnFailureListener(e ->
//                        Toast.makeText(getActivity(), "Failed to update login status", Toast.LENGTH_SHORT).show()
//                );
//    }
//
//
//    // Redirect the user to the login screen
//    private void redirectToLoginScreen() {
//        Intent intent = new Intent(getActivity(), Login.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        startActivity(intent);
//        requireActivity().finish();
//    }
//
//
//    /**
//     * Listen for new messages from booking status in Firebase.
//     * Assumes each booking snapshot may have a "newMessage" field.
//     */
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
//                    if (read == null || !read) {  // count if not read
//                        notificationCount++;
//                    }
//                }
//                updateBadgeCount(notificationCount);
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                // Handle errors as needed.
//            }
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
//            public void onCancelled(@NonNull DatabaseError error) {
//                // Handle errors if necessary.
//            }
//        });
//    }
//}

//package com.example.resort;
//
//import android.app.Activity;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Base64;
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
//import com.bumptech.glide.Glide;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//
//public class ProfileFragment extends Fragment {
//
//    private static final int PICK_IMAGE_REQUEST = 1;
//    private ImageView profileImage;
//    private TextView usernameTextView, phoneNumberTextView;
//    private DatabaseReference userRef;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.profile_fragment, container, false);
//
//        // Initialize Firebase
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
//
//        // Initialize views
//        profileImage = view.findViewById(R.id.imageView);
//        usernameTextView = view.findViewById(R.id.textView14); // Add this ID in your XML
//        phoneNumberTextView = view.findViewById(R.id.textView13); // Add this ID in your XML
//        Button next = view.findViewById(R.id.button7);
//        Button next1 = view.findViewById(R.id.button6);
//        Button next2 = view.findViewById(R.id.button9);
//        Button next3 = view.findViewById(R.id.button10);
//
//        // Load profile data (image, username, phone number)
//        loadProfileData();
//
//        // Click to update profile image
//        profileImage.setOnClickListener(v -> openImagePicker());
//
//        // Other buttons (unchanged)
//        next3.setOnClickListener(v -> startActivity(new Intent(getActivity(), AboutUs.class)));
//        next.setOnClickListener(v -> startActivity(new Intent(getActivity(), Account.class)));
//        next1.setOnClickListener(v -> startActivity(new Intent(getActivity(), BookingStatus.class)));
//        next2.setOnClickListener(v -> startActivity(new Intent(getActivity(), Feedback.class)));
//
//        return view;
//    }
//
//    // Open gallery to pick image
//    private void openImagePicker() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(intent, PICK_IMAGE_REQUEST);
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PICK_IMAGE_REQUEST) {
//            getActivity();
//            if (resultCode == Activity.RESULT_OK && data != null) {
//                Uri imageUri = data.getData();
//                try {
//                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
//                    String base64Image = convertBitmapToBase64(bitmap);
//                    saveImageToFirebase(base64Image); // Save to Firebase
//                    Glide.with(this).load(bitmap).into(profileImage); // Show image immediately
//                } catch (IOException e) {
//                    Toast.makeText(getActivity(), "Failed to load image", Toast.LENGTH_SHORT).show();
//                }
//            }
//        }
//    }
//
//    // Convert image to Base64 (for Firebase storage)
//    private String convertBitmapToBase64(Bitmap bitmap) {
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
//        byte[] byteArray = byteArrayOutputStream.toByteArray();
//        return Base64.encodeToString(byteArray, Base64.DEFAULT);
//    }
//
//    // Save Base64 image to Firebase
//    private void saveImageToFirebase(String base64Image) {
//        userRef.child("imageUrl").setValue(base64Image)
//                .addOnSuccessListener(aVoid -> Toast.makeText(getActivity(), "Image updated!", Toast.LENGTH_SHORT).show())
//                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Update failed", Toast.LENGTH_SHORT).show());
//    }
//
//    // Load profile data (image, username, phone number) from Firebase
//    private void loadProfileData() {
//        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    // Load profile image
//                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
//                    if (imageUrl != null && !imageUrl.equals("default")) {
//                        byte[] decodedBytes = Base64.decode(imageUrl, Base64.DEFAULT);
//                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
//                        Glide.with(ProfileFragment.this).load(decodedBitmap).into(profileImage);
//                    } else {
//                        Glide.with(ProfileFragment.this).load(R.drawable.ic_profile_about).into(profileImage);
//                    }
//
//                    // Load username and phone number
//                    String username = dataSnapshot.child("username").getValue(String.class);
//                    String phoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
//
//                    if (username != null) {
//                        usernameTextView.setText(username);
//                    }
//                    if (phoneNumber != null) {
//                        phoneNumberTextView.setText(phoneNumber);
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//}
//

//private void loadProfileData() {
//    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                                               @Override
//                                               public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                                                   if (dataSnapshot.exists()) {
//                                                       // Retrieve and display profile image using Picasso
//                                                       String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
//                                                       if (imageUrl != null && !imageUrl.equals("default")) {
//                                                           Picasso.get().load(imageUrl).into(profileImage);
//                                                       } else {
//                                                           profileImage.setImageResource(R.drawable.avatar);
//                                                       }
//
//                                                       // Retrieve and set username and phone number
//                                                       String username = dataSnapshot.child("username").getValue(String.class);
//                                                       String phoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
//                                                       if (username != null) {
//                                                           usernameTextView.setText(username);
//                                                       }
//                                                       if (phoneNumber != null) {
//                                                           phoneNumberTextView.setText(phoneNumber);
//                                                       }
//                                                   }
//                                               }
//package com.example.resort;
//
//import android.app.AlertDialog;
//import android.app.Activity;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.os.Bundle;
//import android.provider.MediaStore;
//import android.util.Base64;
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
//import com.bumptech.glide.Glide;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.io.ByteArrayOutputStream;
//import java.io.IOException;
//import java.util.Objects;
//
//public class ProfileFragment extends Fragment {
//
//    private static final int PICK_IMAGE_REQUEST = 1;
//    private ImageView profileImage;
//    private TextView usernameTextView, phoneNumberTextView;
//    private DatabaseReference userRef;
//    private FirebaseAuth mAuth;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.profile_fragment, container, false);
//
//        // Initialize Firebase
//        mAuth = FirebaseAuth.getInstance();
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
//
//        // Initialize views
//        profileImage = view.findViewById(R.id.imageView);
//        usernameTextView = view.findViewById(R.id.textView14); // Add this ID in your XML
//        phoneNumberTextView = view.findViewById(R.id.textView13); // Add this ID in your XML
//        Button next = view.findViewById(R.id.button7);
//        Button next1 = view.findViewById(R.id.button6);
//        Button next2 = view.findViewById(R.id.button9);
//        Button next3 = view.findViewById(R.id.button10);
//        Button logoutButton = view.findViewById(R.id.button11); // Add this ID in your XML
//
//        // Load profile data (image, username, phone number)
//        loadProfileData();
//
//        // Click to update profile image
//        profileImage.setOnClickListener(v -> openImagePicker());
//
//        // Other buttons (unchanged)
//        next3.setOnClickListener(v -> startActivity(new Intent(getActivity(), AboutUs.class)));
//        next.setOnClickListener(v -> startActivity(new Intent(getActivity(), Account.class)));
//        next1.setOnClickListener(v -> startActivity(new Intent(getActivity(), BookingStatus.class)));
//        next2.setOnClickListener(v -> startActivity(new Intent(getActivity(), Feedback.class)));
//
//        // Logout button click listener
//        logoutButton.setOnClickListener(v -> showLogoutConfirmationDialog());
//
//        return view;
//    }
//
//    // Open gallery to pick image
//    private void openImagePicker() {
//        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        startActivityForResult(intent, PICK_IMAGE_REQUEST);
//    }
//
//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == PICK_IMAGE_REQUEST) {
//            getActivity();
//            if (resultCode == Activity.RESULT_OK && data != null) {
//                Uri imageUri = data.getData();
//                try {
//                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
//                    String base64Image = convertBitmapToBase64(bitmap);
//                    saveImageToFirebase(base64Image); // Save to Firebase
//                    Glide.with(this).load(bitmap).into(profileImage); // Show image immediately
//                } catch (IOException e) {
//                    Toast.makeText(getActivity(), "Failed to load image", Toast.LENGTH_SHORT).show();
//                }
//            }
//        }
//    }
//
//    // Convert image to Base64 (for Firebase storage)
//    private String convertBitmapToBase64(Bitmap bitmap) {
//        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
//        byte[] byteArray = byteArrayOutputStream.toByteArray();
//        return Base64.encodeToString(byteArray, Base64.DEFAULT);
//    }
//
//    // Save Base64 image to Firebase
//    private void saveImageToFirebase(String base64Image) {
//        userRef.child("imageUrl").setValue(base64Image)
//                .addOnSuccessListener(aVoid -> Toast.makeText(getActivity(), "Image updated!", Toast.LENGTH_SHORT).show())
//                .addOnFailureListener(e -> Toast.makeText(getActivity(), "Update failed", Toast.LENGTH_SHORT).show());
//    }
//
//    // Load profile data (image, username, phone number) from Firebase
//    private void loadProfileData() {
//        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                if (dataSnapshot.exists()) {
//                    // Load profile image
//                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
//                    if (imageUrl != null && !imageUrl.equals("default")) {
//                        byte[] decodedBytes = Base64.decode(imageUrl, Base64.DEFAULT);
//                        Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
//                        Glide.with(ProfileFragment.this).load(decodedBitmap).into(profileImage);
//                    } else {
//                        Glide.with(ProfileFragment.this).load(R.drawable.ic_profile_about).into(profileImage);
//                    }
//
//                    // Load username and phone number
//                    String username = dataSnapshot.child("username").getValue(String.class);
//                    String phoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
//
//                    if (username != null) {
//                        usernameTextView.setText(username);
//                    }
//                    if (phoneNumber != null) {
//                        phoneNumberTextView.setText(phoneNumber);
//                    }
//                }
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void showLogoutConfirmationDialog() {
//        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
//        // Inflate the custom layout
//        LayoutInflater inflater = requireActivity().getLayoutInflater();
//        View dialogView = inflater.inflate(R.layout.dialog_logout, null);
//        builder.setView(dialogView);
//
//        // Create the AlertDialog instance
//        AlertDialog dialog = builder.create();
//
//        // Set up the buttons with click listeners
//        Button exitButton = dialogView.findViewById(R.id.exitButton);
//        Button cancelButton = dialogView.findViewById(R.id.cancelButton);
//
//        exitButton.setOnClickListener(v -> {
//            logoutUser();
//            dialog.dismiss();
//        });
//
//        cancelButton.setOnClickListener(v -> dialog.dismiss());
//
//        // Show the dialog
//        dialog.show();
//    }
//
//    // Log out the user
//    private void logoutUser() {
//        mAuth.signOut(); // Sign out from Firebase
//        Toast.makeText(getActivity(), "Logged out successfully", Toast.LENGTH_SHORT).show();
//        redirectToLoginScreen();
//    }
//
//
//    // Redirect to the login screen
//    private void redirectToLoginScreen() {
//        Intent intent = new Intent(getActivity(), Login.class);
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK); // Clear the back stack
//        startActivity(intent);
//        requireActivity().finish(); // Close the current activity
//    }
//}


//package com.example.resort;
//
//import android.content.Intent;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.ImageView;
//
//import androidx.fragment.app.Fragment;
//
//public class ProfileFragment extends Fragment {
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        View view = inflater.inflate(R.layout.profile_fragment, container, false);
//
//
//        // Set up Button references and their click listeners
//        Button next = view.findViewById(R.id.button7); // Ensure this ID matches your XML
//        Button next1 = view.findViewById(R.id.button6); // Ensure this ID matches your XML
//        Button next2 = view.findViewById(R.id.button9);
//        Button next3 = view.findViewById(R.id.button10); // Ensure this ID matches your XML
//
//
//        // Set up click listener for Account button
//        next3.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Start Account activity
//                Intent intent = new Intent(getActivity(), AboutUs.class);
//                startActivity(intent);
//            }
//        });
//
//        // Set up click listener for Account button
//        next.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Start Account activity
//                Intent intent = new Intent(getActivity(), Account.class);
//                startActivity(intent);
//            }
//        });
//
//        // Set up click listener for Booking Status button
//        next1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Start BookingStatus activity
//                Intent intent = new Intent(getActivity(), BookingStatus.class);
//                startActivity(intent);
//            }
//        });
//
//        // Set up click listener for Feedback button
//        next2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Start Feedback activity
//                Intent intent = new Intent(getActivity(), Feedback.class);
//                startActivity(intent);
//            }
//        });
//
//        return view; // Return the inflated view
//    }
//}


//package com.example.resort;
//
//import android.content.Intent;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//
//import androidx.fragment.app.Fragment;
//
//public class ProfileFragment extends Fragment {
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        View view = inflater.inflate(R.layout.profile_fragment, container, false);
//
//
//
//        // Correctly get the Button references from the inflated view
//        Button next = view.findViewById(R.id.button7); // Ensure this ID matches your XML
//        Button next1 = view.findViewById(R.id.button6); // Ensure this ID matches your XML
//        Button next2 = view.findViewById(R.id.button9); // Ensure this ID matches your XML
//
//        // Set up click listener for Account button
//        next.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Start Account activity using the fragment's activity context
//                Intent intent = new Intent(getActivity(), Account.class);
//                startActivity(intent);
//            }
//        });
//
//        // Set up click listener for Booking Status button
//        next1.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Start BookingStatus activity using the fragment's activity context
//                Intent intent = new Intent(getActivity(), BookingStatus.class);
//                startActivity(intent);
//            }
//        });
//
//        // Set up click listener for Feedback button
//        next2.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Start Feedback activity using the fragment's activity context
//                Intent intent = new Intent(getActivity(), Feedback.class);
//                startActivity(intent);
//            }
//        });
//
//        return view; // Return the inflated view
//    }
//}


//package com.example.resort;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import androidx.fragment.app.Fragment;
//
//public class ProfileFragment extends Fragment {
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        return inflater.inflate(R.layout.profile_fragment, container, false);
//    }
//}

///This no blink if click
//        next3.setOnClickListener(v -> startActivity(new Intent(getActivity(), AboutUs.class)));
//        next.setOnClickListener(v -> startActivity(new Intent(getActivity(), Account.class)));
//        next1.setOnClickListener(v -> startActivity(new Intent(getActivity(), BookingStatus.class)));
//        next2.setOnClickListener(v -> startActivity(new Intent(getActivity(), Feedback.class)));