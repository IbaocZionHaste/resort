package com.example.resort;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.core.view.ViewCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SignUp extends AppCompatActivity {

    private EditText etEmail, etUsername, etPassword, etConfirmPassword;
    private CheckBox termsCheckBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_up);

        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();

        etEmail = findViewById(R.id.editTextTextEmailAddress);
        etUsername = findViewById(R.id.editTextText6);
        etPassword = findViewById(R.id.editTextTextPassword3);
        etConfirmPassword = findViewById(R.id.editTextTextPassword2);
        Button btnSignUp = findViewById(R.id.button);
        termsCheckBox = findViewById(R.id.termsCheckBox);
        /// Apply the toggle functionality on both fields
        setupPasswordToggle(etPassword);
        setupPasswordToggle(etConfirmPassword);

        TextView textView4 = findViewById(R.id.textView4);
        textView4.setOnClickListener(v -> {
            Intent intent = new Intent(SignUp.this, Login.class);
            startActivity(intent);
            finish();
        });

        /// Block entry if phone not verified
        boolean phoneVerified = getIntent().getBooleanExtra("phoneVerified", false);
        if (!phoneVerified) {
            Toast.makeText(this, "Please verify your phone first.", Toast.LENGTH_LONG).show();
            Intent backIntent = new Intent(this, SignUpNext.class);
            if (getIntent().getExtras() != null) {
                backIntent.putExtras(getIntent().getExtras());
            }
            startActivity(backIntent);
            finish();
            return;
        }


        btnSignUp.setOnClickListener(v -> registerUser());

        ///CheckBox Term
        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showAgreementDialog();
            }
        });

    }

    private void setupPasswordToggle(final EditText passwordField) {
        final int DRAWABLE_END = 2; /// Compound drawable right

        passwordField.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    // Check if the touch is within the bounds of the end drawable (eye icon)
                    if (event.getRawX() >= (passwordField.getRight() -
                            passwordField.getCompoundDrawables()[DRAWABLE_END].getBounds().width())) {

                        if (passwordField.getTransformationMethod() instanceof PasswordTransformationMethod) {
                            // Show password
                            passwordField.setTransformationMethod(HideReturnsTransformationMethod.getInstance());

                            // Change eye icon tint to red (assuming your eye drawable supports tinting)
                            Drawable eyeDrawable = passwordField.getCompoundDrawables()[DRAWABLE_END];
                            if (eyeDrawable != null) {
                                eyeDrawable.setColorFilter(Color.RED, PorterDuff.Mode.SRC_IN);
                                // Reset the compound drawables (to force a redraw)
                                passwordField.setCompoundDrawablesWithIntrinsicBounds(
                                        passwordField.getCompoundDrawables()[0],
                                        passwordField.getCompoundDrawables()[1],
                                        eyeDrawable,
                                        passwordField.getCompoundDrawables()[3]);
                            }
                        } else {
                            /// Hide password
                            passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());

                            /// Clear the color filter or reset to default color
                            Drawable eyeDrawable = passwordField.getCompoundDrawables()[DRAWABLE_END];
                            if (eyeDrawable != null) {
                                eyeDrawable.clearColorFilter();
                                passwordField.setCompoundDrawablesWithIntrinsicBounds(
                                        passwordField.getCompoundDrawables()[0],
                                        passwordField.getCompoundDrawables()[1],
                                        eyeDrawable,
                                        passwordField.getCompoundDrawables()[3]);
                            }
                        }
                        /// Move cursor to the end so the user experience remains natural
                        passwordField.setSelection(passwordField.getText().length());
                        return true;
                    }
                }
                return false;
            }
        });
    }


private void registerUser() {
    String email = etEmail.getText().toString().trim();
    String username = etUsername.getText().toString().trim();
    String password = etPassword.getText().toString().trim();
    String confirmPassword = etConfirmPassword.getText().toString().trim();

    if (TextUtils.isEmpty(email) || TextUtils.isEmpty(username) ||
            TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
        Toast.makeText(SignUp.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
        return;
    }


    if (!isValidEmail(email)) {
        Toast.makeText(SignUp.this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
        return;
    }

    /// Ensure email is a gmail.com address
    if (!email.toLowerCase(Locale.getDefault()).endsWith("@gmail.com")) {
        Toast.makeText(SignUp.this, "Please use a @gmail.com email address.", Toast.LENGTH_SHORT).show();
        return;
    }

    /// Ensure password is at least 6 characters
    if (password.length() < 6) {
        Toast.makeText(SignUp.this, "Password must be at least 6 characters.", Toast.LENGTH_SHORT).show();
        return;
    }

    if (!password.equals(confirmPassword)) {
        Toast.makeText(SignUp.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
        return;
    }

    if (!termsCheckBox.isChecked()) {
        Toast.makeText(SignUp.this, "You must agree to the terms and conditions.", Toast.LENGTH_SHORT).show();
        return;
    }

    ProgressDialog progressDialog = new ProgressDialog(this);
    progressDialog.setMessage("Registering...");
    progressDialog.setCancelable(false);
    progressDialog.show();

    FirebaseAuth mAuth = FirebaseAuth.getInstance();
    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
    String currentUid = mAuth.getCurrentUser() != null
            ? mAuth.getCurrentUser().getUid()
            : null;

    /// First check if username already exists under "users"
    mDatabase.child("users").orderByChild("username").equalTo(username)
            .get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    progressDialog.dismiss();
                    Log.e("SignUp", "Error checking username", task.getException());
                    Toast.makeText(SignUp.this,
                            "Error checking username: " + Objects.requireNonNull(task.getException()).getMessage(),
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                DataSnapshot snapshot = task.getResult();
                Log.d("SignUp", "Username check completed, exists=" + snapshot.exists());

                if (snapshot.exists()) {
                    boolean onlyCurrentUser = true;
                    for (DataSnapshot userSnap : snapshot.getChildren()) {
                        String foundUid = userSnap.getKey();
                        Log.d("SignUp", "Found UID: " + foundUid);
                        if (currentUid == null || !foundUid.equals(currentUid)) {
                            onlyCurrentUser = false;
                            break;
                        }
                    }
                    if (!onlyCurrentUser) {
                        progressDialog.dismiss();
                        Log.i("SignUp", "Username already taken by another user");
                        Toast.makeText(SignUp.this,
                                "Sorry, username is already taken.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Log.d("SignUp", "Username belongs to current user, proceeding");
                }

                /// Username is unique (or only on this user) → proceed with registration
                Log.d("SignUp", "Proceeding with registration for username: " + username);
                mAuth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task1 -> {
                            progressDialog.dismiss();
                            if (!task1.isSuccessful()) {
                                Log.e("SignUp", "Registration failed", task1.getException());
                                Toast.makeText(SignUp.this,
                                        "Registration failed: " + Objects.requireNonNull(task1.getException()).getMessage(),
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }

                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                Log.d("SignUp", "User created with UID: " + user.getUid());
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(username)
                                        .build();
                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileTask -> {
                                            if (!profileTask.isSuccessful()) {
                                                Log.w("SignUp", "Profile update failed", profileTask.getException());
                                            }
                                            Log.d("SignUp", "Profile updated, preparing user data");

                                            /// Retrieve extras
                                            Bundle extras = getIntent().getExtras();
                                            String lastName = extras != null ? extras.getString("lastName") : "";
                                            String firstName = extras != null ? extras.getString("firstName") : "";
                                            String middleInitial = extras != null ? extras.getString("middleInitial") : "";
                                            String barangay = extras != null ? extras.getString("barangay") : "";
                                            String municipality = extras != null ? extras.getString("municipality") : "";
                                            String province = extras != null ? extras.getString("province") : "";
                                            String street = extras != null ? extras.getString("street") : "";
                                            String age = extras != null ? extras.getString("age") : "";
                                            String phoneNumber = extras != null ? extras.getString("phoneNumber") : "";
                                            String gender = extras != null ? extras.getString("gender") : "";
                                            boolean phoneVerified = extras != null && extras.getBoolean("phoneVerified");

                                            String registrationDate = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault())
                                                    .format(new Date());

                                            /// Populate userData map
                                            Map<String, Object> userData = new HashMap<>();
                                            userData.put("firstName", firstName);
                                            userData.put("lastName", lastName);
                                            userData.put("middleInitial", middleInitial);
                                            userData.put("barangay", barangay);
                                            userData.put("municipality", municipality);
                                            userData.put("province", province);
                                            userData.put("street", street);
                                            userData.put("age", age);
                                            userData.put("phoneNumber", phoneNumber);
                                            userData.put("phoneVerified", phoneVerified);
                                            userData.put("gender", gender);
                                            userData.put("email", email);
                                            userData.put("username", username);
                                            userData.put("imageUrl", "default_image_url");
                                            userData.put("registrationDate", registrationDate);
                                            userData.put("isOnline", true);

                                            Log.d("SignUp", "Writing user data to DB");
                                            mDatabase.child("users").child(user.getUid())
                                                    .setValue(userData)
                                                    .addOnCompleteListener(dbTask -> {
                                                        if (dbTask.isSuccessful()) {
                                                            Log.d("SignUp", "User data saved successfully");
                                                            Toast.makeText(SignUp.this,
                                                                    "Registration successful!",
                                                                    Toast.LENGTH_SHORT).show();
                                                            startActivity(new Intent(SignUp.this, Login.class));
                                                            finish();
                                                        } else {
                                                            Log.e("SignUp", "Failed to save user data", dbTask.getException());
                                                            Toast.makeText(SignUp.this,
                                                                    "Failed to save user data.",
                                                                    Toast.LENGTH_SHORT).show();
                                                        }
                                                    });
                                        });
                            }
                        });
            });
}



    ///Agreement Data
    private void showAgreementDialog() {
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_terms, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(SignUp.this);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();


        ///Connect buttons
        Button btnAgree = dialogView.findViewById(R.id.btnAgree);
        Button btnCancel = dialogView.findViewById(R.id.btnCancel);

        btnAgree.setOnClickListener(v -> {
            termsCheckBox.setOnCheckedChangeListener(null);
            termsCheckBox.setChecked(true);
            addTermsListenerBack();
            alertDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> {
            termsCheckBox.setOnCheckedChangeListener(null);
            termsCheckBox.setChecked(false);
            addTermsListenerBack();
            alertDialog.dismiss();
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void addTermsListenerBack() {
        termsCheckBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                showAgreementDialog();
            }
        });
    }

    /// Method to reset all input fields
    private void resetFields() {
        etEmail.setText("");
        etUsername.setText("");
        etPassword.setText("");
        etConfirmPassword.setText("");
        termsCheckBox.setChecked(false);
    }

    /// Method to navigate to LoginActivity
    private void navigateToLoginActivity() {
        Intent intent = new Intent(SignUp.this, Login.class);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }

    /// Method to validate email format
    private boolean isValidEmail(String email) {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    /// Method to show progress dialog
    private void showProgressDialog() {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Registering...");
        progressDialog.setCancelable(false);
        progressDialog.show();
    }
}


///No Current User
//package com.example.resort;
//
//import android.app.ProgressDialog;
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.widget.Button;
//import android.widget.CheckBox;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//import java.util.Locale;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.view.ViewCompat;
//import androidx.core.graphics.Insets;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.auth.UserProfileChangeRequest;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//
//import java.text.SimpleDateFormat;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//
//public class SignUp extends AppCompatActivity {
//
//    private FirebaseAuth mAuth;
//    private DatabaseReference mDatabase;
//
//    private EditText etEmail, etUsername, etPassword, etConfirmPassword;
//    private CheckBox termsCheckBox;
//    private ProgressDialog progressDialog;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_sign_up);
//
//        mAuth = FirebaseAuth.getInstance();
//        mDatabase = FirebaseDatabase.getInstance().getReference();
//
//        etEmail = findViewById(R.id.editTextTextEmailAddress);
//        etUsername = findViewById(R.id.editTextText6);
//        etPassword = findViewById(R.id.editTextTextPassword3);
//        etConfirmPassword = findViewById(R.id.editTextTextPassword2);
//        Button btnSignUp = findViewById(R.id.button);
//        termsCheckBox = findViewById(R.id.termsCheckBox);
//
//        TextView textView4 = findViewById(R.id.textView4);
//        textView4.setOnClickListener(v -> {
//            Intent intent = new Intent(SignUp.this, Login.class);
//            startActivity(intent);
//            finish();
//        });
//
/////        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
////            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
////            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
////            return insets;
////        });
//
//        btnSignUp.setOnClickListener(v -> registerUser());
//    }
//
//    private void registerUser() {
//        String email = etEmail.getText().toString().trim();
//        String username = etUsername.getText().toString().trim();
//        String password = etPassword.getText().toString().trim();
//        String confirmPassword = etConfirmPassword.getText().toString().trim();
//
//        // Validate fields
//        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(username) ||
//                TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
//            Toast.makeText(SignUp.this, "Please fill in all fields.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (!isValidEmail(email)) {
//            Toast.makeText(SignUp.this, "Please enter a valid email address.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (!password.equals(confirmPassword)) {
//            Toast.makeText(SignUp.this, "Passwords do not match.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        if (!termsCheckBox.isChecked()) {
//            Toast.makeText(SignUp.this, "You must agree to the terms and conditions.", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        // Show progress dialog
//        showProgressDialog();
//
//        // Register new user with Firebase Authentication
//        mAuth.createUserWithEmailAndPassword(email, password)
//                .addOnCompleteListener(task -> {
//                    progressDialog.dismiss();
//                    if (task.isSuccessful()) {
//                        FirebaseUser user = mAuth.getCurrentUser();
//                        if (user != null) {
//                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
//                                    .setDisplayName(username)
//                                    .build();
//                            user.updateProfile(profileUpdates)
//                                    .addOnCompleteListener(profileTask -> {
//                                        if (profileTask.isSuccessful()) {
//                                            // Retrieve extra data (optional)
//                                            Bundle extras = getIntent().getExtras();
//                                            String lastName = extras != null ? extras.getString("lastName") : "";
//                                            String firstName = extras != null ? extras.getString("firstName") : "";
//                                            String middleInitial = extras != null ? extras.getString("middleInitial") : "";
//                                            String barangay = extras != null ? extras.getString("barangay") : "";
//                                            String municipality = extras != null ? extras.getString("municipality") : "";
//                                            String province = extras != null ? extras.getString("province") : "";
//                                            String street = extras != null ? extras.getString("street") : "";
//                                            String age = extras != null ? extras.getString("age") : "";
//                                            String phoneNumber = extras != null ? extras.getString("phoneNumber") : "";
//                                            String gender = extras != null ? extras.getString("gender") : "";
//
//                                            // Get the current date in MM-dd-yyyy format.
//                                            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd-yyyy", Locale.getDefault());
//                                            String registrationDate = sdf.format(new Date());
//
//                                            Map<String, Object> userData = new HashMap<>();
//                                            userData.put("firstName", firstName);
//                                            userData.put("lastName", lastName);
//                                            userData.put("middleInitial", middleInitial);
//                                            userData.put("barangay", barangay);
//                                            userData.put("municipality", municipality);
//                                            userData.put("province", province);
//                                            userData.put("street", street);
//                                            userData.put("age", age);
//                                            userData.put("phoneNumber", phoneNumber);
//                                            userData.put("gender", gender);
//                                            userData.put("email", email);
//                                            userData.put("username", username);
//                                            userData.put("imageUrl", "default_image_url"); /// Default image
//                                            /// Added registrationDate field.
//                                            userData.put("registrationDate", registrationDate);
//
//                                            mDatabase.child("users").child(user.getUid())
//                                                    .setValue(userData)
//                                                    .addOnSuccessListener(aVoid -> {
//                                                        Toast.makeText(SignUp.this, "Registration complete!", Toast.LENGTH_SHORT).show();
//                                                        /// After registration, always navigate to Login Activity
//                                                        navigateToLoginActivity();
//                                                        resetFields(); /// Reset all fields
//                                                    })
//                                                    .addOnFailureListener(e -> {
//                                                        Toast.makeText(SignUp.this, "Data saving failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                                                    });
//                                        } else {
//                                            Toast.makeText(SignUp.this, "Profile update failed: " + profileTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
//                                        }
//                                    });
//                        }
//                    } else {
//                        Toast.makeText(SignUp.this, "Registration failed: " +
//                                Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
//                    }
//                });
//
//    }
//
//    // Method to reset all input fields
//    private void resetFields() {
//        etEmail.setText("");
//        etUsername.setText("");
//        etPassword.setText("");
//        etConfirmPassword.setText("");
//        termsCheckBox.setChecked(false);
//    }
//
//    /// Method to navigate to LoginActivity
//    private void navigateToLoginActivity() {
//        Intent intent = new Intent(SignUp.this, Login.class);
//        startActivity(intent);
//        overridePendingTransition(0, 0);
//        finish();
//    }
//
//    /// Method to validate email format
//    private boolean isValidEmail(String email) {
//        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
//    }
//
//    /// Method to show progress dialog
//    private void showProgressDialog() {
//        progressDialog = new ProgressDialog(this);
//        progressDialog.setMessage("Registering...");
//        progressDialog.setCancelable(false);
//        progressDialog.show();
//    }
//}
//
//
