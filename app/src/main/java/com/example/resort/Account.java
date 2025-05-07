package com.example.resort;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Account extends AppCompatActivity {

    private EditText etFirstName, etLastName, etMiddleName, etStreet, etBarangay,
            etMunicipality, etProvince, etAge, etGender, etUsername, etEmail, etPhone;
    private SharedPreferences sharedPreferences;
    private static final String PREF_NAME = "AccountPrefs";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_account);

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);

        // Initialize Firebase Auth and Database
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        // Reference the UI elements
        etFirstName = findViewById(R.id.editTextText18);
        etLastName = findViewById(R.id.editTextText8);
        etMiddleName = findViewById(R.id.editTextText19);
        etStreet = findViewById(R.id.editTextText20);
        etBarangay = findViewById(R.id.editTextText13);
        etMunicipality = findViewById(R.id.editTextText10);
        etProvince = findViewById(R.id.editTextText7);
        etAge = findViewById(R.id.editTextText);
        etGender = findViewById(R.id.editTextText4);
        ///etUsername = findViewById(R.id.editTextText5);
        etEmail = findViewById(R.id.editTextText2);
        etPhone = findViewById(R.id.editTextText9);
        Button back = findViewById(R.id.back2);

        back.setOnClickListener(v -> onBackPressed());

        /// Get the current user
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String currentUserId = currentUser.getUid();
            String cachedUserId = sharedPreferences.getString("userId", null);

            /// If cached data belongs to the current user, load it
            //noinspection StatementWithEmptyBody
            if (cachedUserId != null && cachedUserId.equals(currentUserId) && loadCachedData()) {
                /// Cached data loaded successfully.
            } else {
                /// Cached data is missing or belongs to a different user.
                /// Clear cached data.
                SharedPreferences.Editor clearEditor = sharedPreferences.edit();
                clearEditor.clear();
                clearEditor.apply();

                /// Retrieve from Firebase
                mDatabase.child("users").child(currentUserId)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                if (snapshot.exists()) {
                                    // Populate the EditText fields with user data
                                    String firstName = snapshot.child("firstName").getValue(String.class);
                                    String lastName = snapshot.child("lastName").getValue(String.class);
                                    String middleInitial = snapshot.child("middleInitial").getValue(String.class);
                                    String street = snapshot.child("street").getValue(String.class);
                                    String barangay = snapshot.child("barangay").getValue(String.class);
                                    String municipality = snapshot.child("municipality").getValue(String.class);
                                    String province = snapshot.child("province").getValue(String.class);
                                    String age = snapshot.child("age").getValue(String.class);
                                    String gender = snapshot.child("gender").getValue(String.class);
                                    ///String username = snapshot.child("username").getValue(String.class);
                                    String email = snapshot.child("email").getValue(String.class);
                                    String phone = snapshot.child("phoneNumber").getValue(String.class);

                                    etFirstName.setText(firstName);
                                    etLastName.setText(lastName);
                                    etMiddleName.setText(middleInitial);
                                    etStreet.setText(street);
                                    etBarangay.setText(barangay);
                                    etMunicipality.setText(municipality);
                                    etProvince.setText(province);
                                    etAge.setText(age);
                                    etGender.setText(gender);
                                    ///etUsername.setText(username);
                                    etEmail.setText(email);
                                    etPhone.setText(phone);

                                    /// Cache the data in SharedPreferences along with the current user id
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("userId", currentUserId);
                                    editor.putString("firstName", firstName);
                                    editor.putString("lastName", lastName);
                                    editor.putString("middleInitial", middleInitial);
                                    editor.putString("street", street);
                                    editor.putString("barangay", barangay);
                                    editor.putString("municipality", municipality);
                                    editor.putString("province", province);
                                    editor.putString("age", age);
                                    editor.putString("gender", gender);
                                    ///editor.putString("username", username);
                                    editor.putString("email", email);
                                    editor.putString("phoneNumber", phone);
                                    editor.apply();
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                // Handle potential errors here.
                            }
                        });
            }
        }
    }

    /**
     * Loads account data from SharedPreferences.
     *
     * @return true if data was found and loaded; false otherwise.
     */
    private boolean loadCachedData() {
        boolean dataFound = false;

        String firstName = sharedPreferences.getString("firstName", null);
        if (firstName != null) {
            dataFound = true;
            etFirstName.setText(firstName);
            etLastName.setText(sharedPreferences.getString("lastName", ""));
            etMiddleName.setText(sharedPreferences.getString("middleInitial", ""));
            etStreet.setText(sharedPreferences.getString("street", ""));
            etBarangay.setText(sharedPreferences.getString("barangay", ""));
            etMunicipality.setText(sharedPreferences.getString("municipality", ""));
            etProvince.setText(sharedPreferences.getString("province", ""));
            etAge.setText(sharedPreferences.getString("age", ""));
            etGender.setText(sharedPreferences.getString("gender", ""));
            ///etUsername.setText(sharedPreferences.getString("username", ""));
            etEmail.setText(sharedPreferences.getString("email", ""));
            etPhone.setText(sharedPreferences.getString("phoneNumber", ""));
        }
        return dataFound;
    }
}




///No Current User
//package com.example.resort;
//
//import android.content.SharedPreferences;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//public class Account extends AppCompatActivity {
//
//    private EditText etFirstName, etLastName, etMiddleName, etStreet, etBarangay,
//            etMunicipality, etProvince, etAge, etGender, etUsername, etEmail, etPhone;
//    private SharedPreferences sharedPreferences;
//    private static final String PREF_NAME = "AccountPrefs";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_account);
//
//        // Initialize SharedPreferences
//        sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
//
//        // Initialize Firebase Auth and Database
//        FirebaseAuth mAuth = FirebaseAuth.getInstance();
//        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
//
//        // Reference the UI elements
//        etFirstName = findViewById(R.id.editTextText18);
//        etLastName = findViewById(R.id.editTextText8);
//        etMiddleName = findViewById(R.id.editTextText19);
//        etStreet = findViewById(R.id.editTextText20);
//        etBarangay = findViewById(R.id.editTextText13);
//        etMunicipality = findViewById(R.id.editTextText10);
//        etProvince = findViewById(R.id.editTextText7);
//        etAge = findViewById(R.id.editTextText);
//        etGender = findViewById(R.id.editTextText4);
//        etUsername = findViewById(R.id.editTextText5);
//        etEmail = findViewById(R.id.editTextText2);
//        etPhone = findViewById(R.id.editTextText9);
//        Button back = findViewById(R.id.back2);
//
//        back.setOnClickListener(v -> onBackPressed());
//
//        // Get the current user
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//        if (currentUser != null) {
//            String currentUserId = currentUser.getUid();
//            String cachedUserId = sharedPreferences.getString("userId", null);
//
//            // If cached data belongs to the current user, load it
//            if (cachedUserId != null && cachedUserId.equals(currentUserId) && loadCachedData()) {
//                // Cached data loaded successfully.
//            } else {
//                // Cached data is missing or belongs to a different user.
//                // Clear cached data.
//                SharedPreferences.Editor clearEditor = sharedPreferences.edit();
//                clearEditor.clear();
//                clearEditor.apply();
//
//                // Retrieve from Firebase
//                mDatabase.child("users").child(currentUserId)
//                        .addListenerForSingleValueEvent(new ValueEventListener() {
//                            @Override
//                            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                                if (snapshot.exists()) {
//                                    // Populate the EditText fields with user data
//                                    String firstName = snapshot.child("firstName").getValue(String.class);
//                                    String lastName = snapshot.child("lastName").getValue(String.class);
//                                    String middleInitial = snapshot.child("middleInitial").getValue(String.class);
//                                    String street = snapshot.child("street").getValue(String.class);
//                                    String barangay = snapshot.child("barangay").getValue(String.class);
//                                    String municipality = snapshot.child("municipality").getValue(String.class);
//                                    String province = snapshot.child("province").getValue(String.class);
//                                    String age = snapshot.child("age").getValue(String.class);
//                                    String gender = snapshot.child("gender").getValue(String.class);
//                                    String username = snapshot.child("username").getValue(String.class);
//                                    String email = snapshot.child("email").getValue(String.class);
//                                    String phone = snapshot.child("phoneNumber").getValue(String.class);
//
//                                    etFirstName.setText(firstName);
//                                    etLastName.setText(lastName);
//                                    etMiddleName.setText(middleInitial);
//                                    etStreet.setText(street);
//                                    etBarangay.setText(barangay);
//                                    etMunicipality.setText(municipality);
//                                    etProvince.setText(province);
//                                    etAge.setText(age);
//                                    etGender.setText(gender);
//                                    etUsername.setText(username);
//                                    etEmail.setText(email);
//                                    etPhone.setText(phone);
//
//                                    // Cache the data in SharedPreferences along with the current user id
//                                    SharedPreferences.Editor editor = sharedPreferences.edit();
//                                    editor.putString("userId", currentUserId);
//                                    editor.putString("firstName", firstName);
//                                    editor.putString("lastName", lastName);
//                                    editor.putString("middleInitial", middleInitial);
//                                    editor.putString("street", street);
//                                    editor.putString("barangay", barangay);
//                                    editor.putString("municipality", municipality);
//                                    editor.putString("province", province);
//                                    editor.putString("age", age);
//                                    editor.putString("gender", gender);
//                                    editor.putString("username", username);
//                                    editor.putString("email", email);
//                                    editor.putString("phoneNumber", phone);
//                                    editor.apply();
//                                }
//                            }
//
//                            @Override
//                            public void onCancelled(@NonNull DatabaseError error) {
//                                // Handle potential errors here.
//                            }
//                        });
//            }
//        }
//    }
//
//    /**
//     * Loads account data from SharedPreferences.
//     *
//     * @return true if data was found and loaded; false otherwise.
//     */
//    private boolean loadCachedData() {
//        boolean dataFound = false;
//
//        String firstName = sharedPreferences.getString("firstName", null);
//        if (firstName != null) {
//            dataFound = true;
//            etFirstName.setText(firstName);
//            etLastName.setText(sharedPreferences.getString("lastName", ""));
//            etMiddleName.setText(sharedPreferences.getString("middleInitial", ""));
//            etStreet.setText(sharedPreferences.getString("street", ""));
//            etBarangay.setText(sharedPreferences.getString("barangay", ""));
//            etMunicipality.setText(sharedPreferences.getString("municipality", ""));
//            etProvince.setText(sharedPreferences.getString("province", ""));
//            etAge.setText(sharedPreferences.getString("age", ""));
//            etGender.setText(sharedPreferences.getString("gender", ""));
//            etUsername.setText(sharedPreferences.getString("username", ""));
//            etEmail.setText(sharedPreferences.getString("email", ""));
//            etPhone.setText(sharedPreferences.getString("phoneNumber", ""));
//        }
//        return dataFound;
//    }
//}
//
//
