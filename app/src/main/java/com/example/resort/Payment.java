package com.example.resort;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.review.data.YourAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class Payment extends AppCompatActivity {

    private EditText firstName, lastName, reference, phoneNumber, amount;
    private CheckBox checkBoxGcash, checkBoxPalawan;
    private Button submitButton, backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_payment);


        /// Adjust layout for system insets.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /// Initialize views.
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        reference = findViewById(R.id.reference);
        phoneNumber = findViewById(R.id.phoneNumber);
        amount = findViewById(R.id.amount);
        checkBoxGcash = findViewById(R.id.checkBoxGcash);
        checkBoxPalawan = findViewById(R.id.checkBoxPalawan);
        submitButton = findViewById(R.id.submit);
        backButton = findViewById(R.id.back2);

        ///This code is one only check the checkbox
        checkBoxGcash.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkBoxPalawan.setChecked(false);
            }
        });

        checkBoxPalawan.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkBoxGcash.setChecked(false);
            }
        });

        ///Payment Information
        ImageView messageIcon = findViewById(R.id.messageIcon);
        TextView badge = findViewById(R.id.badge); // Your badge TextView (Optional if you want to show a count)
        messageIcon.setOnClickListener(view -> {
            // Inflate the custom layout for the dialog
            View dialogView = LayoutInflater.from(Payment.this).inflate(R.layout.dialog_recyclerview, null);

            // Set up RecyclerView
            RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
            recyclerView.setLayoutManager(new LinearLayoutManager(Payment.this));

            // Create the adapter and attach immediately (empty for now)
            List<String> dataList = new ArrayList<>();
            YourAdapter adapter = new YourAdapter(dataList);
            recyclerView.setAdapter(adapter);

            // Fetch data from Firebase Realtime Database
            FirebaseDatabase database = FirebaseDatabase.getInstance();
            DatabaseReference myRef = database.getReference("paymentdescription"); // path to your data

            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    // Check if 'description' exists
                    if (dataSnapshot.exists() && dataSnapshot.child("description").getValue() != null) {
                        String message = dataSnapshot.child("description").getValue(String.class);
                        if (message != null && !message.isEmpty()) {
                            dataList.add(message);  // Add the description to the list
                            adapter.notifyDataSetChanged();

                            // Optionally, you can add a badge count if needed (e.g., 1 if there's a description)
                            badge.setText("1");  // Badge will show 1 since there’s only one description
                            badge.setVisibility(View.VISIBLE); // Show badge
                        }
                    } else {
                        dataList.add("No info available.");
                        adapter.notifyDataSetChanged();
                        badge.setVisibility(View.GONE); // Hide badge if no description is found
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    dataList.add("Error loading no info.");
                    adapter.notifyDataSetChanged();
                    badge.setVisibility(View.GONE); // Hide badge in case of an error
                }
            });

            /// Build and show the dialog
            AlertDialog dialog = new AlertDialog.Builder(Payment.this, R.style.CustomDialog)
                    .setView(dialogView)
                    .create();

            /// Optional: Make it cancelable with background click
            dialog.setCanceledOnTouchOutside(true);
            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.show();

        });


        /// Fetch the current user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(Payment.this, "User not logged in!", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();

        // Directly reference the user's "MyBooking" node.
        DatabaseReference myBookingRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("MyBooking");

        // Submit payment data.
        submitButton.setOnClickListener(v -> {
            if (validateFields()) {
                // Retrieve field values.
                String fName = firstName.getText().toString().trim();
                String lName = lastName.getText().toString().trim();
                String refNo = reference.getText().toString().trim();
                String phone = phoneNumber.getText().toString().trim();
                String amt = amount.getText().toString().trim();
                String paymentMethodValue;
                if (checkBoxGcash.isChecked()) {
                    paymentMethodValue = "Gcash";
                } else if (checkBoxPalawan.isChecked()) {
                    paymentMethodValue = "Palawan";
                } else {
                    paymentMethodValue = "";
                }

                // Fetch the booking directly from "MyBooking" (assuming there’s one active booking)
                String finalPaymentMethodValue = paymentMethodValue;
                myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Get the first booking from MyBooking, assuming there is only one active booking.
                            String bookingId = snapshot.getChildren().iterator().next().getKey();

                            if (bookingId == null) {
                                Toast.makeText(Payment.this, "No booking found for current user!", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            // Proceed to update the payment method data.
                            DatabaseReference bookingRef = FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(userId)
                                    .child("MyBooking")
                                    .child(bookingId);
                            DatabaseReference paymentMethodRef = bookingRef.child("paymentMethod");

                            /// Create current date/time string.
                            String currentDateTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
                            /// Build paymentMethod data.
                            Map<String, Object> paymentData = new HashMap<>();
                            paymentData.put("Payment", finalPaymentMethodValue);
                            paymentData.put("Firstname", fName);
                            paymentData.put("Lastname", lName);
                            paymentData.put("Phone", phone);
                            paymentData.put("Reference", refNo);
                            paymentData.put("Amount", amt);
                            paymentData.put("Date", currentDateTime);
                            paymentData.put("Status", "Done");  // Set status to "Done"

                            /// Update the paymentMethod node.
                            paymentMethodRef.updateChildren(paymentData)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            // Append the same date into the paymentTransaction node.
                                            DatabaseReference paymentTransactionRef = bookingRef.child("paymentTransaction");
                                            Map<String, Object> transactionData = new HashMap<>();
                                            transactionData.put("PaymentDate", currentDateTime);
                                            paymentTransactionRef.updateChildren(transactionData)
                                                    .addOnCompleteListener(task1 -> {
                                                        if (task1.isSuccessful()) {
                                                            Log.d("PaymentTransaction", "Payment date appended successfully.");

                                                            /// Create a new top-level node "paymentSent" and add a payment message with date.
                                                            DatabaseReference paymentSentRef = FirebaseDatabase.getInstance()
                                                                    .getReference("paymentSent");
                                                            Map<String, Object> paymentSentData = new HashMap<>();
                                                            paymentSentData.put("message", "Payment sent by " + fName + " " + lName);
                                                            paymentSentData.put("date", currentDateTime);
                                                            paymentSentRef.push().setValue(paymentSentData);

                                                            /// *** Update SharedPreferences for persistent UI state ***
                                                            /// This ensures that in your Booking Status activity the progress (3) and payment submission time persist.
                                                            SharedPreferences prefs = getSharedPreferences("BookingPref_" + userId, MODE_PRIVATE);
                                                            prefs.edit().putBoolean("paymentSubmitted", true)
                                                                    .putInt("bookingProgress", 3)
                                                                    .apply();

                                                            // Clear all fields and checkboxes after successful payment
                                                            firstName.setText("");
                                                            lastName.setText("");
                                                            reference.setText("");
                                                            phoneNumber.setText("");
                                                            amount.setText("");
                                                            checkBoxGcash.setChecked(false);
                                                            checkBoxPalawan.setChecked(false);

                                                            Toast.makeText(Payment.this, "Payment submitted successfully!", Toast.LENGTH_SHORT).show();
                                                            Intent intent = new Intent(Payment.this, BookingStatus.class);
                                                            intent.putExtra("paymentSubmitted", true);
                                                            finish();

                                                        } else {
                                                            Log.e("PaymentTransaction", "Failed to append payment date to paymentTransaction.");
                                                        }
                                                    });
                                        } else {
                                            Toast.makeText(Payment.this, "Failed to update payment info", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        } else {
                            Toast.makeText(Payment.this, "No booking found for current user!", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(Payment.this, "Failed to fetch booking info", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Toast.makeText(Payment.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            }
        });

        // Go back to the previous screen
        backButton.setOnClickListener(v -> onBackPressed());
    }

    private boolean validateFields() {
        return !TextUtils.isEmpty(firstName.getText().toString().trim())
                && !TextUtils.isEmpty(lastName.getText().toString().trim())
                && !TextUtils.isEmpty(reference.getText().toString().trim())
                && !TextUtils.isEmpty(phoneNumber.getText().toString().trim())
                && !TextUtils.isEmpty(amount.getText().toString().trim())
                && (checkBoxGcash.isChecked() || checkBoxPalawan.isChecked());
    }
}



///Current User
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.app.AlertDialog;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.Button;
//import android.widget.CheckBox;
//import android.widget.EditText;
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
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.review.data.YourAdapter;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
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
//import java.util.List;
//import java.util.Locale;
//import java.util.Map;
//import java.util.Objects;
//
//public class Payment extends AppCompatActivity {
//
//    private EditText firstName, lastName, reference, phoneNumber, amount;
//    private CheckBox checkBoxGcash, checkBoxPalawan;
//    private Button submitButton, backButton;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_payment);
//
//        /// Adjust layout for system insets.
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Initialize views.
//        firstName = findViewById(R.id.firstName);
//        lastName = findViewById(R.id.lastName);
//        reference = findViewById(R.id.reference);
//        phoneNumber = findViewById(R.id.phoneNumber);
//        amount = findViewById(R.id.amount);
//        checkBoxGcash = findViewById(R.id.checkBoxGcash);
//        checkBoxPalawan = findViewById(R.id.checkBoxPalawan);
//        submitButton = findViewById(R.id.submit);
//        backButton = findViewById(R.id.back2);
//
//        ///This code is one only check the checkbox
//        checkBoxGcash.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                checkBoxPalawan.setChecked(false);
//            }
//        });
//
//        checkBoxPalawan.setOnCheckedChangeListener((buttonView, isChecked) -> {
//            if (isChecked) {
//                checkBoxGcash.setChecked(false);
//            }
//        });
//
//
//        ///Payment Information
//        ImageView messageIcon = findViewById(R.id.messageIcon);
//        TextView badge = findViewById(R.id.badge); // Your badge TextView (Optional if you want to show a count)
//        messageIcon.setOnClickListener(view -> {
//            // Inflate the custom layout for the dialog
//            View dialogView = LayoutInflater.from(Payment.this).inflate(R.layout.dialog_recyclerview, null);
//
//            // Set up RecyclerView
//            RecyclerView recyclerView = dialogView.findViewById(R.id.dialogRecyclerView);
//            recyclerView.setLayoutManager(new LinearLayoutManager(Payment.this));
//
//            // Create the adapter and attach immediately (empty for now)
//            List<String> dataList = new ArrayList<>();
//            YourAdapter adapter = new YourAdapter(dataList);
//            recyclerView.setAdapter(adapter);
//
//            // Fetch data from Firebase Realtime Database
//            FirebaseDatabase database = FirebaseDatabase.getInstance();
//            DatabaseReference myRef = database.getReference("paymentdescription"); // path to your data
//
//            myRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                    // Check if 'description' exists
//                    if (dataSnapshot.exists() && dataSnapshot.child("description").getValue() != null) {
//                        String message = dataSnapshot.child("description").getValue(String.class);
//                        if (message != null && !message.isEmpty()) {
//                            dataList.add(message);  // Add the description to the list
//                            adapter.notifyDataSetChanged();
//
//                            // Optionally, you can add a badge count if needed (e.g., 1 if there's a description)
//                            badge.setText("1");  // Badge will show 1 since there’s only one description
//                            badge.setVisibility(View.VISIBLE); // Show badge
//                        }
//                    } else {
//                        dataList.add("No refund info available.");
//                        adapter.notifyDataSetChanged();
//                        badge.setVisibility(View.GONE); // Hide badge if no description is found
//                    }
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError databaseError) {
//                    dataList.add("Error loading refund info.");
//                    adapter.notifyDataSetChanged();
//                    badge.setVisibility(View.GONE); // Hide badge in case of an error
//                }
//            });
//
//            /// Build and show the dialog
//            AlertDialog dialog = new AlertDialog.Builder(Payment.this, R.style.CustomDialog)
//                    .setView(dialogView)
//                    .create();
//
//           /// Optional: Make it cancelable with background click
//            dialog.setCanceledOnTouchOutside(true);
//            Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//            dialog.show();
//
//        });
//
//
//
//
//
//        // Fetch the current user
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            Toast.makeText(Payment.this, "User not logged in!", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        String userId = currentUser.getUid();
//
//        // Directly reference the user's "MyBooking" node.
//        DatabaseReference myBookingRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyBooking");
//
//        // Submit payment data.
//        submitButton.setOnClickListener(v -> {
//            if (validateFields()) {
//                // Retrieve field values.
//                String fName = firstName.getText().toString().trim();
//                String lName = lastName.getText().toString().trim();
//                String refNo = reference.getText().toString().trim();
//                String phone = phoneNumber.getText().toString().trim();
//                String amt = amount.getText().toString().trim();
//                String paymentMethodValue;
//                if (checkBoxGcash.isChecked()) {
//                    paymentMethodValue = "Gcash";
//                } else if (checkBoxPalawan.isChecked()) {
//                    paymentMethodValue = "Palawan";
//                } else {
//                    paymentMethodValue = "";
//                }
//
//                // Fetch the booking directly from "MyBooking" (assuming there’s one active booking)
//                String finalPaymentMethodValue = paymentMethodValue;
//                myBookingRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        if (snapshot.exists()) {
//                            // Get the first booking from MyBooking, assuming there is only one active booking.
//                            String bookingId = snapshot.getChildren().iterator().next().getKey();
//
//                            if (bookingId == null) {
//                                Toast.makeText(Payment.this, "No booking found for current user!", Toast.LENGTH_SHORT).show();
//                                return;
//                            }
//
//                            // Proceed to update the payment method data.
//                            DatabaseReference bookingRef = FirebaseDatabase.getInstance()
//                                    .getReference("users")
//                                    .child(userId)
//                                    .child("MyBooking")
//                                    .child(bookingId);
//                            DatabaseReference paymentMethodRef = bookingRef.child("paymentMethod");
//
//                            /// Create current date/time string.
//                            String currentDateTime = new SimpleDateFormat("yyyy-MM-dd hh:mm a", Locale.getDefault()).format(new Date());
//                            /// Build paymentMethod data.
//                            Map<String, Object> paymentData = new HashMap<>();
//                            paymentData.put("Payment", finalPaymentMethodValue);
//                            paymentData.put("Firstname", fName);
//                            paymentData.put("Lastname", lName);
//                            paymentData.put("Phone", phone);
//                            paymentData.put("Reference", refNo);
//                            paymentData.put("Amount", amt);
//                            paymentData.put("Date", currentDateTime);
//                            paymentData.put("Status", "Done");  // Set status to "Done"
//
//                            /// Update the paymentMethod node.
//                            paymentMethodRef.updateChildren(paymentData)
//                                    .addOnCompleteListener(task -> {
//                                        if (task.isSuccessful()) {
//                                            // Append the same date into the paymentTransaction node.
//                                            DatabaseReference paymentTransactionRef = bookingRef.child("paymentTransaction");
//                                            Map<String, Object> transactionData = new HashMap<>();
//                                            transactionData.put("PaymentDate", currentDateTime);
//                                            paymentTransactionRef.updateChildren(transactionData)
//                                                    .addOnCompleteListener(task1 -> {
//                                                        if (task1.isSuccessful()) {
//                                                            Log.d("PaymentTransaction", "Payment date appended successfully.");
//
//                                                            /// Create a new top-level node "paymentSent" and add a payment message with date.
//                                                            DatabaseReference paymentSentRef = FirebaseDatabase.getInstance()
//                                                                    .getReference("paymentSent");
//                                                            Map<String, Object> paymentSentData = new HashMap<>();
//                                                            paymentSentData.put("message", "Payment sent by " + fName + " " + lName);
//                                                            paymentSentData.put("date", currentDateTime);
//                                                            paymentSentRef.push().setValue(paymentSentData);
//
//                                                            /// *** Update SharedPreferences for persistent UI state ***
//                                                            /// This ensures that in your Booking Status activity the progress (3) and payment submission time persist.
//                                                            SharedPreferences prefs = getSharedPreferences("BookingPref_" + userId, MODE_PRIVATE);
//                                                            prefs.edit().putBoolean("paymentSubmitted", true)
//                                                                    .putInt("bookingProgress", 3)
//                                                                    .apply();
//
//                                                            // Clear all fields and checkboxes after successful payment
//                                                            firstName.setText("");
//                                                            lastName.setText("");
//                                                            reference.setText("");
//                                                            phoneNumber.setText("");
//                                                            amount.setText("");
//                                                            checkBoxGcash.setChecked(false);
//                                                            checkBoxPalawan.setChecked(false);
//
//                                                            Toast.makeText(Payment.this, "Payment submitted successfully!", Toast.LENGTH_SHORT).show();
//                                                            Intent intent = new Intent(Payment.this, BookingStatus.class);
//                                                            intent.putExtra("paymentSubmitted", true);
//                                                            finish();
//
//                                                        } else {
//                                                            Log.e("PaymentTransaction", "Failed to append payment date to paymentTransaction.");
//                                                        }
//                                                    });
//                                        } else {
//                                            Toast.makeText(Payment.this, "Failed to update payment info", Toast.LENGTH_SHORT).show();
//                                        }
//                                    });
//                        } else {
//                            Toast.makeText(Payment.this, "No booking found for current user!", Toast.LENGTH_SHORT).show();
//                        }
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        Toast.makeText(Payment.this, "Failed to fetch booking info", Toast.LENGTH_SHORT).show();
//                    }
//                });
//            } else {
//                Toast.makeText(Payment.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // Go back to the previous screen
//        backButton.setOnClickListener(v -> onBackPressed());
//    }
//
//    private boolean validateFields() {
//        return !TextUtils.isEmpty(firstName.getText().toString().trim())
//                && !TextUtils.isEmpty(lastName.getText().toString().trim())
//                && !TextUtils.isEmpty(reference.getText().toString().trim())
//                && !TextUtils.isEmpty(phoneNumber.getText().toString().trim())
//                && !TextUtils.isEmpty(amount.getText().toString().trim())
//                && (checkBoxGcash.isChecked() || checkBoxPalawan.isChecked());
//    }
//}
