package com.example.resort;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VerificationCode extends AppCompatActivity {

    private TextView timerTextView, resend;
    private EditText code1, code2, code3, code4, code5, code6;
    /** @noinspection deprecation*/
    private ProgressDialog progressDialog;

    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private FirebaseAuth mAuth;
    private CountDownTimer countDownTimer;
    // 1 minute (60000 milliseconds)
    private static final long COUNTDOWN_TIME = 60000;

    private boolean isTimerRunning = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification_code);

        mAuth = FirebaseAuth.getInstance();

        // NOTE: In production mode, we are not using test mode settings.
        // Firebase automatically handles the reCAPTCHA (or SafetyNet) challenge when needed.

        // Bind views
        resend = findViewById(R.id.resend);
        TextView numberView = findViewById(R.id.number);
        timerTextView = findViewById(R.id.time);
        code1 = findViewById(R.id.code1);
        code2 = findViewById(R.id.code2);
        code3 = findViewById(R.id.code3);
        code4 = findViewById(R.id.code4);
        code5 = findViewById(R.id.code5);
        code6 = findViewById(R.id.code6);
        Button submit = findViewById(R.id.button);
        Button backButton = findViewById(R.id.back);

        setupOTPInputs();

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Verifying...");
        progressDialog.setCancelable(false);

        // Initially disable resend button and set text to "Resend"
        resend.setEnabled(false);
        resend.setText("");

        // Get phone number from intent
        String phoneNumber = getIntent().getStringExtra("phoneNumber");
        // Example: if user input is a local Philippine number, make sure to format it as +63XXXTENTACION
        if (phoneNumber != null && !phoneNumber.isEmpty()) {
            numberView.setText(phoneNumber);
            startPhoneNumberVerification(phoneNumber);
        } else {
            Toast.makeText(this, "Phone number not available.", Toast.LENGTH_SHORT).show();
        }

        // Resend button click listener
        resend.setOnClickListener(v -> {
            if (isTimerRunning) {
                Toast.makeText(VerificationCode.this, "Please wait for cooldown", Toast.LENGTH_SHORT).show();
            } else if (phoneNumber != null && resendToken != null) {
                resend.setEnabled(false);
                resend.setText("Resend");
                resendVerificationCode(phoneNumber, resendToken);
            } else {
                Toast.makeText(VerificationCode.this, "Unable to resend code.", Toast.LENGTH_SHORT).show();
            }
        });

        // Submit button click listener for manual code entry
        submit.setOnClickListener(v -> {
            String code = getCodeFromEditTexts();
            if (!TextUtils.isEmpty(code) && code.length() == 6) {
                progressDialog.show();
                verifyPhoneNumberWithCode(verificationId, code);
            } else {
                Toast.makeText(VerificationCode.this, "Please enter the 6-digit code.", Toast.LENGTH_SHORT).show();
            }
        });

        // Back button click listener
        backButton.setOnClickListener(v -> finish());
    }

    private void startCountDownTimer() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        isTimerRunning = true;
        countDownTimer = new CountDownTimer(COUNTDOWN_TIME, 1000) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                // Display only seconds remaining (e.g., "60s", "59s")
                long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
                timerTextView.setText(secondsLeft + "s");
            }
            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {
                timerTextView.setText("0s");
                isTimerRunning = false;
                // Change button text to "Retry" after cooldown
                resend.setEnabled(true);
                resend.setText("Resend");
            }
        }.start();
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber) // ensure phone number is in international format, e.g., +63XXXXXXXXXX for Philippines
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
               ///this not use because the autofill code remove
                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                    String code = credential.getSmsCode();
                    if (code != null && code.length() == 6) {
                        //autoFillCode(code);
                        verifyPhoneNumberWithCode(verificationId, code);
                    }
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    progressDialog.dismiss();
                    Toast.makeText(VerificationCode.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
                @SuppressLint("SetTextI18n")
                @Override
                public void onCodeSent(@NonNull String verifyId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = verifyId;
                    resendToken = token;
                    Toast.makeText(VerificationCode.this, "Verification code sent.", Toast.LENGTH_SHORT).show();
                    startCountDownTimer();
                    // While timer is running, disable the button with text "Resend"
                    resend.setEnabled(false);
                    resend.setText("Wait");
                }
            };

    private void resendVerificationCode(String phoneNumber, PhoneAuthProvider.ForceResendingToken token) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .setForceResendingToken(token)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
        Toast.makeText(this, "Resending code...", Toast.LENGTH_SHORT).show();
    }

    private void verifyPhoneNumberWithCode(String verificationId, String code) {
        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid());

            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DataSnapshot snapshot = task.getResult();
                    Boolean existingVerification = snapshot.child("phoneVerified").getValue(Boolean.class);

                    // Only update if not already verified
                    if (existingVerification == null || !existingVerification) {
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("phoneVerified", true);

                        String phoneNumber = getIntent().getStringExtra("phoneNumber");
                        if (phoneNumber != null) {
                            updates.put("phoneNumber", phoneNumber);
                        }

                        userRef.updateChildren(updates)
                                .addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        Intent intent = new Intent(VerificationCode.this, BottomNavigation.class);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(VerificationCode.this, "Database update failed: "
                                                + Objects.requireNonNull(updateTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // Already verified: Proceed directly
                        progressDialog.dismiss();
                        Intent intent = new Intent(VerificationCode.this, BottomNavigation.class);
                        startActivity(intent);
                        finish();
                    }
                }
            });

        } else {
            // Handle non-logged in users (shouldn't happen in normal flow)
            mAuth.signInWithCredential(credential)
                    .addOnCompleteListener(task -> {
                        progressDialog.dismiss();
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = mAuth.getCurrentUser();
                            if (firebaseUser != null) {
                                DatabaseReference userRef = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(firebaseUser.getUid());

                                Map<String, Object> updates = new HashMap<>();
                                updates.put("phoneVerified", true);
                                String phoneNumber = getIntent().getStringExtra("phoneNumber");
                                if (phoneNumber != null && !phoneNumber.isEmpty()) {
                                    updates.put("phoneNumber", phoneNumber);
                                }

                                userRef.updateChildren(updates)
                                        .addOnCompleteListener(updateTask -> {
                                            if (updateTask.isSuccessful()) {
                                                Intent intent = new Intent(VerificationCode.this, BottomNavigation.class);
                                                startActivity(intent);
                                                finish();
                                            } else {
                                                Toast.makeText(VerificationCode.this, "Database update failed: "
                                                        + Objects.requireNonNull(updateTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            }
                        } else {
                            Toast.makeText(VerificationCode.this, "Verification error: "
                                    + Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private String getCodeFromEditTexts() {
        return code1.getText().toString().trim() +
                code2.getText().toString().trim() +
                code3.getText().toString().trim() +
                code4.getText().toString().trim() +
                code5.getText().toString().trim() +
                code6.getText().toString().trim();
    }

//    private void autoFillCode(String code) {
//        if (code.length() == 6) {
//            code1.setText(String.valueOf(code.charAt(0)));
//            code2.setText(String.valueOf(code.charAt(1)));
//            code3.setText(String.valueOf(code.charAt(2)));
//            code4.setText(String.valueOf(code.charAt(3)));
//            code5.setText(String.valueOf(code.charAt(4)));
//            code6.setText(String.valueOf(code.charAt(5)));
//        }
//    }



    /// Setup OTP EditTexts so that once a digit is entered, focus moves to the next field.
    private void setupOTPInputs() {
        code1.addTextChangedListener(new OTPTextWatcher(code1, code2));
        code2.addTextChangedListener(new OTPTextWatcher(code2, code3));
        code3.addTextChangedListener(new OTPTextWatcher(code3, code4));
        code4.addTextChangedListener(new OTPTextWatcher(code4, code5));
        code5.addTextChangedListener(new OTPTextWatcher(code5, code6));
        code6.addTextChangedListener(new OTPTextWatcher(code6, null));
    }

    /// Custom TextWatcher class for OTP fields
    private static class OTPTextWatcher implements TextWatcher {
        private final EditText nextView;

        public OTPTextWatcher(EditText ignoredCurrentView, EditText nextView) {
            this.nextView = nextView;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // No action needed here
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
            // No action needed here
        }
    }
}



