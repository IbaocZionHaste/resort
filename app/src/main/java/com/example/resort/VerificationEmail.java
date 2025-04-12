package com.example.resort;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;

public class VerificationEmail extends AppCompatActivity {

    private EditText emailEditText;
    private TextView resendButton;
    private int resendAttempts = 0; /// Counter for resending

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verification_email);

        /// Initialize views
        emailEditText = findViewById(R.id.email);
        Button submitButton = findViewById(R.id.button);
        resendButton = findViewById(R.id.resend);

        Button back = findViewById(R.id.back);
        back.setOnClickListener(v -> onBackPressed());

        /// Submit button to send reset email
        submitButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();

            if (email.isEmpty()) {
                emailEditText.setError("Please enter your email address.");
                return;
            }

            sendPasswordResetEmail(email);
        });

        /// Resend button functionality with limit
        resendButton.setOnClickListener(v -> {
            if (resendAttempts < 3) {
                String email = emailEditText.getText().toString().trim();

                if (email.isEmpty()) {
                    emailEditText.setError("Please enter your email address.");
                    return;
                }

                resendAttempts++; /// Increase count
                sendPasswordResetEmail(email);

                if (resendAttempts >= 3) {
                    resendButton.setEnabled(false); /// Disable button after 3 attempts
                    Toast.makeText(getApplicationContext(), "Resend limit reached!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getApplicationContext(), "Resend attempt " + resendAttempts + "/3", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendPasswordResetEmail(String email) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        /// Clear the email field upon successful submission
                        emailEditText.setText("");
                        Toast.makeText(getApplicationContext(), "Password reset email sent!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}


///No Current User
//package com.example.resort;
//
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.auth.FirebaseAuth;
//
//import java.util.Objects;
//
//public class VerificationEmail extends AppCompatActivity {
//
//    private EditText emailEditText;
//    private TextView resendButton;
//    private int resendAttempts = 0; /// Counter for resending
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_verification_email);
//
//        /// Initialize views
//        emailEditText = findViewById(R.id.email);
//        Button submitButton = findViewById(R.id.button);
//        resendButton = findViewById(R.id.resend);
//
//        Button back = findViewById(R.id.back);
//        back.setOnClickListener(v -> onBackPressed());
//
//        /// Submit button to send reset email
//        submitButton.setOnClickListener(v -> {
//            String email = emailEditText.getText().toString().trim();
//
//            if (email.isEmpty()) {
//                emailEditText.setError("Please enter your email address.");
//                return;
//            }
//
//            sendPasswordResetEmail(email);
//        });
//
//        /// Resend button functionality with limit
//        resendButton.setOnClickListener(v -> {
//            if (resendAttempts < 3) {
//                String email = emailEditText.getText().toString().trim();
//
//                if (email.isEmpty()) {
//                    emailEditText.setError("Please enter your email address.");
//                    return;
//                }
//
//                resendAttempts++; /// Increase count
//                sendPasswordResetEmail(email);
//
//                if (resendAttempts >= 3) {
//                    resendButton.setEnabled(false); /// Disable button after 3 attempts
//                    Toast.makeText(getApplicationContext(), "Resend limit reached!", Toast.LENGTH_LONG).show();
//                } else {
//                    Toast.makeText(getApplicationContext(), "Resend attempt " + resendAttempts + "/3", Toast.LENGTH_SHORT).show();
//                }
//            }
//        });
//    }
//
//    private void sendPasswordResetEmail(String email) {
//        FirebaseAuth auth = FirebaseAuth.getInstance();
//        auth.sendPasswordResetEmail(email)
//                .addOnCompleteListener(task -> {
//                    if (task.isSuccessful()) {
//                        /// Clear the email field upon successful submission
//                        emailEditText.setText("");
//                        Toast.makeText(getApplicationContext(), "Password reset email sent!", Toast.LENGTH_LONG).show();
//                    } else {
//                        Toast.makeText(getApplicationContext(), "Error: " + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_LONG).show();
//                    }
//                });
//    }
//}
