package com.example.resort;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class Login extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private CheckBox checkBoxRememberMe;
    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private SharedPreferences sharedPreferences;
    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PASSWORD = "password";
    private static final String KEY_REMEMBER_ME = "rememberMe";
    private ProgressDialog progressDialog; // For showing a loading indicator

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Initialize Firebase Auth and Database
        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // Reference the UI elements
        etEmail = findViewById(R.id.editTextTextEmailAddress);
        etPassword = findViewById(R.id.editTextTextPassword2);
        Button buttonLogin = findViewById(R.id.button);
        TextView textView4 = findViewById(R.id.textView4);
        TextView textViewForgotPassword = findViewById(R.id.textView8);
        checkBoxRememberMe = findViewById(R.id.termsCheckBox);

        // NEW: Reference Google and Facebook login buttons (make sure these IDs exist in your layout)
        //ImageButton buttonGoogle = findViewById(R.id.button3);
        //ImageButton buttonFacebook = findViewById(R.id.button2);

        // Initialize the progress dialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging in...");
        progressDialog.setCancelable(false);

        // Check if "Remember Me" was checked previously
        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
        if (rememberMe) {
            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
            etEmail.setText(savedEmail);
            etPassword.setText(savedPassword);
            checkBoxRememberMe.setChecked(true);
        }

        ///This code is eye to hide and unhide the password view
        etPassword.setOnTouchListener(new View.OnTouchListener() {
            boolean isPasswordVisible = false;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                final int DRAWABLE_RIGHT = 2;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    /// Check if the touch event was within the bounds of the right drawable
                    if (etPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
                        int drawableWidth = etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
                        if (event.getRawX() >= (etPassword.getRight() - drawableWidth)) {
                            if (isPasswordVisible) {
                                // Hide password and reset eye icon color (black)
                                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                                etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                        etPassword.getCompoundDrawables()[0],
                                        etPassword.getCompoundDrawables()[1],
                                        tintDrawable(etPassword.getCompoundDrawables()[DRAWABLE_RIGHT], 0xFF000000),
                                        etPassword.getCompoundDrawables()[3]
                                );
                                isPasswordVisible = false;
                            } else {
                                // Show password and tint the eye icon red
                                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                                etPassword.setCompoundDrawablesWithIntrinsicBounds(
                                        etPassword.getCompoundDrawables()[0],
                                        etPassword.getCompoundDrawables()[1],
                                        tintDrawable(etPassword.getCompoundDrawables()[DRAWABLE_RIGHT], 0xFFFF0000),
                                        etPassword.getCompoundDrawables()[3]
                                );
                                isPasswordVisible = true;
                            }
                            // Move cursor to end
                            etPassword.setSelection(etPassword.getText().length());
                            return true;
                        }
                    }
                }
                return false;
            }
        });

        // Set click listener for the "Login" button
        buttonLogin.setOnClickListener(v -> loginUser());

        // Set click listener for the "Sign Up" text
        textView4.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, SignUpNext.class);
            startActivity(intent);
            finish();
        });

        // Set click listener for the "Forgot Password" text
        textViewForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, VerificationEmail
                    .class);
            startActivity(intent);
        });

        /// NEW: Set click listeners for Google and Facebook buttons
//        buttonGoogle.setOnClickListener(v ->
//                Toast.makeText(Login.this, "Google login is not available now", Toast.LENGTH_SHORT).show()
//        );
//
//        buttonFacebook.setOnClickListener(v ->
//                Toast.makeText(Login.this, "Facebook login is not available now", Toast.LENGTH_SHORT).show()
//        );


    }

    ///Icon drawable color tint
    private Drawable tintDrawable(Drawable drawable, int color) {
        if (drawable != null) {
            Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
            DrawableCompat.setTint(wrappedDrawable, color);
            return wrappedDrawable;
        }
        return null;
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        // Validate fields
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress dialog while logging in
        progressDialog.show();

        // Sign in the user with Firebase Authentication
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        // Save email and password if "Remember Me" is checked
                        if (checkBoxRememberMe.isChecked()) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(KEY_EMAIL, email);
                            editor.putString(KEY_PASSWORD, password);
                            editor.putBoolean(KEY_REMEMBER_ME, true);
                            editor.apply();
                        } else {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.remove(KEY_EMAIL);
                            editor.remove(KEY_PASSWORD);
                            editor.putBoolean(KEY_REMEMBER_ME, false);
                            editor.apply();
                        }

                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            // Check if user is banned by fetching user data from Firebase
                            mDatabase.child("users").child(user.getUid())
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            String userStatus = dataSnapshot.child("status").getValue(String.class);
                                            if (userStatus != null && userStatus.equalsIgnoreCase("banned")) {
                                                // Show alert dialog if user is banned
                                                new AlertDialog.Builder(Login.this)
                                                        .setTitle("Account Suspended")
                                                        .setMessage("You are banned because suspicious activity")
                                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                                            @Override
                                                            public void onClick(DialogInterface dialog, int which) {
                                                                mAuth.signOut();
                                                            }
                                                        })
                                                        .setCancelable(false)
                                                        .show();
                                            } else {
                                                // Set user online status
                                                mDatabase.child("users").child(user.getUid()).child("isOnline").setValue(true);

                                                // Proceed with login flow (phone verification check or redirect)
                                                String phoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
                                                Boolean phoneVerified = dataSnapshot.child("phoneVerified").getValue(Boolean.class);
                                                boolean isVerified = (phoneVerified != null) && phoneVerified;

                                                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                                                    if (isVerified) {
                                                        Intent intent = new Intent(Login.this, BottomNavigation.class);
                                                        startActivity(intent);
                                                        overridePendingTransition(0, 0); /// Instant transition (blink effect)
                                                        finish();
                                                    } else {
                                                        Intent intent = new Intent(Login.this, VerificationCode.class);
                                                        intent.putExtra("phoneNumber", phoneNumber);
                                                        startActivity(intent);
                                                        overridePendingTransition(0, 0); /// Instant transition (blink effect)
                                                        ///finish();
                                                    }
                                                } else {
                                                    Intent intent = new Intent(Login.this, BottomNavigation.class);
                                                    startActivity(intent);
                                                    overridePendingTransition(0, 0); /// Instant transition (blink effect)
                                                    finish();
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            Toast.makeText(Login.this, "Database error", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        Toast.makeText(Login.this, "Login failed: " +
                                Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

}

///No Current User
//package com.example.resort;
//
//import android.app.AlertDialog;
//import android.app.ProgressDialog;
//import android.content.DialogInterface;
//import android.content.Intent;
//import android.content.SharedPreferences;
//import android.graphics.drawable.ColorDrawable;
//import android.graphics.drawable.Drawable;
//import android.os.Bundle;
//import android.text.InputType;
//import android.text.TextUtils;
//import android.view.MotionEvent;
//import android.view.View;
//import android.widget.Button;
//import android.widget.CheckBox;
//import android.widget.EditText;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.ActionBar;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.drawable.DrawableCompat;
//import androidx.core.view.ViewCompat;
//import androidx.core.graphics.Insets;
//import androidx.core.view.WindowCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.core.widget.NestedScrollView;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.Objects;
//
//public class Login extends AppCompatActivity {
//
//    private EditText etEmail, etPassword;
//    private CheckBox checkBoxRememberMe;
//    private FirebaseAuth mAuth;
//    private DatabaseReference mDatabase;
//    private SharedPreferences sharedPreferences;
//    private static final String PREFS_NAME = "LoginPrefs";
//    private static final String KEY_EMAIL = "email";
//    private static final String KEY_PASSWORD = "password";
//    private static final String KEY_REMEMBER_ME = "rememberMe";
//    private ProgressDialog progressDialog; // For showing a loading indicator
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_login);
//
//        // Initialize Firebase Auth and Database
//        mAuth = FirebaseAuth.getInstance();
//        mDatabase = FirebaseDatabase.getInstance().getReference();
//
//        // Initialize SharedPreferences
//        sharedPreferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
//
//        // Reference the UI elements
//        etEmail = findViewById(R.id.editTextTextEmailAddress);
//        etPassword = findViewById(R.id.editTextTextPassword2);
//        Button buttonLogin = findViewById(R.id.button);
//        TextView textView4 = findViewById(R.id.textView4);
//        TextView textViewForgotPassword = findViewById(R.id.textView8);
//        checkBoxRememberMe = findViewById(R.id.termsCheckBox);
//
//        // NEW: Reference Google and Facebook login buttons (make sure these IDs exist in your layout)
//        ImageButton buttonGoogle = findViewById(R.id.button3);
//        ImageButton buttonFacebook = findViewById(R.id.button2);
//
//        // Initialize the progress dialog
//        progressDialog = new ProgressDialog(this);
//        progressDialog.setMessage("Logging in...");
//        progressDialog.setCancelable(false);
//
//        // Check if "Remember Me" was checked previously
//        boolean rememberMe = sharedPreferences.getBoolean(KEY_REMEMBER_ME, false);
//        if (rememberMe) {
//            String savedEmail = sharedPreferences.getString(KEY_EMAIL, "");
//            String savedPassword = sharedPreferences.getString(KEY_PASSWORD, "");
//            etEmail.setText(savedEmail);
//            etPassword.setText(savedPassword);
//            checkBoxRememberMe.setChecked(true);
//        }
//
//        ///This code is eye to hide and unhide the password view
//        etPassword.setOnTouchListener(new View.OnTouchListener() {
//            boolean isPasswordVisible = false;
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                final int DRAWABLE_RIGHT = 2;
//                if (event.getAction() == MotionEvent.ACTION_UP) {
//                    /// Check if the touch event was within the bounds of the right drawable
//                    if (etPassword.getCompoundDrawables()[DRAWABLE_RIGHT] != null) {
//                        int drawableWidth = etPassword.getCompoundDrawables()[DRAWABLE_RIGHT].getBounds().width();
//                        if (event.getRawX() >= (etPassword.getRight() - drawableWidth)) {
//                            if (isPasswordVisible) {
//                                // Hide password and reset eye icon color (black)
//                                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
//                                etPassword.setCompoundDrawablesWithIntrinsicBounds(
//                                        etPassword.getCompoundDrawables()[0],
//                                        etPassword.getCompoundDrawables()[1],
//                                        tintDrawable(etPassword.getCompoundDrawables()[DRAWABLE_RIGHT], 0xFF000000),
//                                        etPassword.getCompoundDrawables()[3]
//                                );
//                                isPasswordVisible = false;
//                            } else {
//                                // Show password and tint the eye icon red
//                                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
//                                etPassword.setCompoundDrawablesWithIntrinsicBounds(
//                                        etPassword.getCompoundDrawables()[0],
//                                        etPassword.getCompoundDrawables()[1],
//                                        tintDrawable(etPassword.getCompoundDrawables()[DRAWABLE_RIGHT], 0xFFFF0000),
//                                        etPassword.getCompoundDrawables()[3]
//                                );
//                                isPasswordVisible = true;
//                            }
//                            // Move cursor to end
//                            etPassword.setSelection(etPassword.getText().length());
//                            return true;
//                        }
//                    }
//                }
//                return false;
//            }
//        });
//
//        // Set click listener for the "Login" button
//        buttonLogin.setOnClickListener(v -> loginUser());
//
//        // Set click listener for the "Sign Up" text
//        textView4.setOnClickListener(v -> {
//            Intent intent = new Intent(Login.this, SignUpNext.class);
//            startActivity(intent);
//            finish();
//        });
//
//        // Set click listener for the "Forgot Password" text
//        textViewForgotPassword.setOnClickListener(v -> {
//            Intent intent = new Intent(Login.this, VerificationEmail
//                    .class);
//            startActivity(intent);
//        });
//
//        // NEW: Set click listeners for Google and Facebook buttons
//        buttonGoogle.setOnClickListener(v ->
//                Toast.makeText(Login.this, "Google login is not available now", Toast.LENGTH_SHORT).show()
//        );
//
//        buttonFacebook.setOnClickListener(v ->
//                Toast.makeText(Login.this, "Facebook login is not available now", Toast.LENGTH_SHORT).show()
//        );
//
//
//    }
//
//    ///Icon drawable color tint
//    private Drawable tintDrawable(Drawable drawable, int color) {
//        if (drawable != null) {
//            Drawable wrappedDrawable = DrawableCompat.wrap(drawable);
//            DrawableCompat.setTint(wrappedDrawable, color);
//            return wrappedDrawable;
//        }
//        return null;
//    }
//
//    private void loginUser() {
//        String email = etEmail.getText().toString().trim();
//        String password = etPassword.getText().toString().trim();
//
//        // Validate fields
//        if (TextUtils.isEmpty(email)) {
//            Toast.makeText(this, "Please enter your email.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (TextUtils.isEmpty(password)) {
//            Toast.makeText(this, "Please enter your password.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Show progress dialog while logging in
//        progressDialog.show();
//
//        // Sign in the user with Firebase Authentication
//        mAuth.signInWithEmailAndPassword(email, password)
//                .addOnCompleteListener(task -> {
//                    progressDialog.dismiss();
//                    if (task.isSuccessful()) {
//                        // Save email and password if "Remember Me" is checked
//                        if (checkBoxRememberMe.isChecked()) {
//                            SharedPreferences.Editor editor = sharedPreferences.edit();
//                            editor.putString(KEY_EMAIL, email);
//                            editor.putString(KEY_PASSWORD, password);
//                            editor.putBoolean(KEY_REMEMBER_ME, true);
//                            editor.apply();
//                        } else {
//                            SharedPreferences.Editor editor = sharedPreferences.edit();
//                            editor.remove(KEY_EMAIL);
//                            editor.remove(KEY_PASSWORD);
//                            editor.putBoolean(KEY_REMEMBER_ME, false);
//                            editor.apply();
//                        }
//
//                        FirebaseUser user = mAuth.getCurrentUser();
//                        if (user != null) {
//                            // Check if user is banned by fetching user data from Firebase
//                            mDatabase.child("users").child(user.getUid())
//                                    .addListenerForSingleValueEvent(new ValueEventListener() {
//                                        @Override
//                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                                            String userStatus = dataSnapshot.child("status").getValue(String.class);
//                                            if (userStatus != null && userStatus.equalsIgnoreCase("banned")) {
//                                                // Show alert dialog if user is banned
//                                                new AlertDialog.Builder(Login.this)
//                                                        .setTitle("Account Suspended")
//                                                        .setMessage("You are banned because suspicious activity")
//                                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                                                            @Override
//                                                            public void onClick(DialogInterface dialog, int which) {
//                                                                mAuth.signOut();
//                                                            }
//                                                        })
//                                                        .setCancelable(false)
//                                                        .show();
//                                            } else {
//                                                // Set user online status
//                                                mDatabase.child("users").child(user.getUid()).child("isOnline").setValue(true);
//
//                                                // Proceed with login flow (phone verification check or redirect)
//                                                String phoneNumber = dataSnapshot.child("phoneNumber").getValue(String.class);
//                                                Boolean phoneVerified = dataSnapshot.child("phoneVerified").getValue(Boolean.class);
//                                                boolean isVerified = (phoneVerified != null) && phoneVerified;
//
//                                                if (phoneNumber != null && !phoneNumber.isEmpty()) {
//                                                    if (isVerified) {
//                                                        Intent intent = new Intent(Login.this, BottomNavigation.class);
//                                                        startActivity(intent);
//                                                        overridePendingTransition(0, 0); // Instant transition (blink effect)
//                                                        finish();
//                                                    } else {
//                                                        Intent intent = new Intent(Login.this, VerificationCode.class);
//                                                        intent.putExtra("phoneNumber", phoneNumber);
//                                                        startActivity(intent);
//                                                        overridePendingTransition(0, 0); // Instant transition (blink effect)
//                                                        ///finish();
//                                                    }
//                                                } else {
//                                                    Intent intent = new Intent(Login.this, BottomNavigation.class);
//                                                    startActivity(intent);
//                                                    overridePendingTransition(0, 0); // Instant transition (blink effect)
//                                                    finish();
//                                                }
//                                            }
//                                        }
//
//                                        @Override
//                                        public void onCancelled(@NonNull DatabaseError error) {
//                                            Toast.makeText(Login.this, "Database error", Toast.LENGTH_SHORT).show();
//                                        }
//                                    });
//                        }
//                    } else {
//                        Toast.makeText(Login.this, "Login failed: " +
//                                Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//    }
//
//}
