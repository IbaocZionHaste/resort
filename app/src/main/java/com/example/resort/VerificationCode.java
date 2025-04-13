package com.example.resort;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class VerificationCode extends AppCompatActivity {

    private TextView timerTextView, resend, numberView;
    private EditText code1, code2, code3, code4, code5, code6;
    private ProgressDialog progressDialog;
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private FirebaseAuth mAuth;
    private CountDownTimer countDownTimer;
    private static final long COUNTDOWN_TIME = 120000;
    private boolean isTimerRunning = false;

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verification_code);

        mAuth = FirebaseAuth.getInstance();
        resend = findViewById(R.id.resend);
        timerTextView = findViewById(R.id.time);
        numberView = findViewById(R.id.number);
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

        resend.setEnabled(false);
        resend.setText("");

        String phoneNumber = getIntent().getStringExtra("phoneNumber");
        if (!TextUtils.isEmpty(phoneNumber)) {
            numberView.setText(phoneNumber);
            startPhoneNumberVerification(phoneNumber);
        } else {
            Toast.makeText(this, "Phone number not available.", Toast.LENGTH_SHORT).show();
        }

        resend.setOnClickListener(v -> {
            if (isTimerRunning) {
                Toast.makeText(VerificationCode.this, "Please wait for cooldown", Toast.LENGTH_SHORT).show();
            } else if (!TextUtils.isEmpty(phoneNumber) && resendToken != null) {
                resend.setEnabled(false);
                resend.setText("Resend");
                resendVerificationCode(phoneNumber, resendToken);
            } else {
                Toast.makeText(VerificationCode.this, "Unable to resend code.", Toast.LENGTH_SHORT).show();
            }
        });

        submit.setOnClickListener(v -> {
            String code = getCodeFromEditTexts();
            if (verificationId == null) {
                Toast.makeText(VerificationCode.this, "OTP code error", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!TextUtils.isEmpty(code) && code.length() == 6) {
                progressDialog.show();
                verifyPhoneNumberWithCode(verificationId, code);
            } else {
                Toast.makeText(VerificationCode.this, "Please enter the 6-digit code.", Toast.LENGTH_SHORT).show();
            }
        });

        backButton.setOnClickListener(v -> finish());
    }

    private void startCountDownTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = true;
        countDownTimer = new CountDownTimer(COUNTDOWN_TIME, 1000) {
            @SuppressLint("SetTextI18n")
            @Override
            public void onTick(long millisUntilFinished) {
                long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
                timerTextView.setText(secondsLeft + "s");
            }
            @SuppressLint("SetTextI18n")
            @Override
            public void onFinish() {
                timerTextView.setText("0s");
                isTimerRunning = false;
                resend.setEnabled(true);
                resend.setText("Resend");
            }
        }.start();
    }

    private void startPhoneNumberVerification(String phoneNumber) {
        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(this)
                .setCallbacks(mCallbacks)
                .build();
        PhoneAuthProvider.verifyPhoneNumber(options);
    }

    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                @Override
                public void onVerificationCompleted(PhoneAuthCredential credential) {
                    // Empty - no auto-processing
                }

                @Override
                public void onVerificationFailed(FirebaseException e) {
                    progressDialog.dismiss();
                    Toast.makeText(VerificationCode.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCodeSent(@NonNull String verifyId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = verifyId;
                    resendToken = token;
                    Toast.makeText(VerificationCode.this, "Verification code sent.", Toast.LENGTH_SHORT).show();
                    startCountDownTimer();
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
        progressDialog.show();

        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            DatabaseReference userRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid());
            userRef.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    DataSnapshot snapshot = task.getResult();
                    Boolean isVerified = snapshot.child("phoneVerified").getValue(Boolean.class);
                    if (isVerified != null && isVerified) {
                        progressDialog.dismiss();
                        // If already verified then show our custom alert dialog.
                        showCustomVerificationDialog();
                    } else {
                        mAuth.signInWithCredential(credential).addOnCompleteListener(credentialTask -> {
                            progressDialog.dismiss();
                            if (credentialTask.isSuccessful()) {
                                Map<String, Object> updates = new HashMap<>();
                                updates.put("phoneVerified", true);
                                String phone = getIntent().getStringExtra("phoneNumber");
                                if (!TextUtils.isEmpty(phone)) {
                                    updates.put("phoneNumber", phone);
                                }
                                userRef.updateChildren(updates).addOnCompleteListener(updateTask -> {
                                    if (updateTask.isSuccessful()) {
                                        /// Show custom alert dialog upon successful update.
                                        showCustomVerificationDialog();
                                    } else {
                                        Toast.makeText(VerificationCode.this, "Database update failed: " +
                                                Objects.requireNonNull(updateTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            } else {
                                Toast.makeText(VerificationCode.this, "Verification error: " +
                                        Objects.requireNonNull(credentialTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    progressDialog.dismiss();
                    Toast.makeText(VerificationCode.this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
                    if (firebaseUser != null) {
                        DatabaseReference userRef = FirebaseDatabase.getInstance()
                                .getReference("users")
                                .child(firebaseUser.getUid());
                        Map<String, Object> updates = new HashMap<>();
                        updates.put("phoneVerified", true);
                        String phone = getIntent().getStringExtra("phoneNumber");
                        if (!TextUtils.isEmpty(phone)) {
                            updates.put("phoneNumber", phone);
                        }
                        userRef.updateChildren(updates).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                // Clear back stack
                                Intent intent = new Intent(VerificationCode.this, BottomNavigation.class);
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(intent);
                                overridePendingTransition(0, 0);
                                finish();
                            } else {
                                Toast.makeText(VerificationCode.this, "Database update failed: " +
                                        Objects.requireNonNull(updateTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    Toast.makeText(VerificationCode.this, "Verification error: " +
                            Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    /**
     * Displays a professional custom alert dialog informing the user that verification was successful.
     * When the user clicks the "OK" button, they are redirected to the Login activity.
     */
    private void showCustomVerificationDialog() {
        /// Inflate the custom dialog layout.
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.custom_alert_verification, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(VerificationCode.this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        final AlertDialog alertDialog = builder.create();

        /// Optionally, you can set or modify the title or message here if needed.
        dialogView.findViewById(R.id.tvAlertTitle);
        dialogView.findViewById(R.id.tvAlertMessage);

        Button btnOk = dialogView.findViewById(R.id.btnAlertOk);
        btnOk.setOnClickListener(v -> {
            alertDialog.dismiss();
            /// Redirect to the Login activity when the "OK" button is pressed.
            Intent intent = new Intent(VerificationCode.this, Login.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        alertDialog.show();
    }



    private String getCodeFromEditTexts() {
        return code1.getText().toString().trim() +
                code2.getText().toString().trim() +
                code3.getText().toString().trim() +
                code4.getText().toString().trim() +
                code5.getText().toString().trim() +
                code6.getText().toString().trim();
    }

    private void setupOTPInputs() {
        code1.addTextChangedListener(new OTPTextWatcher(code1, code2));
        code2.addTextChangedListener(new OTPTextWatcher(code2, code3));
        code3.addTextChangedListener(new OTPTextWatcher(code3, code4));
        code4.addTextChangedListener(new OTPTextWatcher(code4, code5));
        code5.addTextChangedListener(new OTPTextWatcher(code5, code6));
        code6.addTextChangedListener(new OTPTextWatcher(code6, null));

        setupDeleteKeyListener(code2, code1);
        setupDeleteKeyListener(code3, code2);
        setupDeleteKeyListener(code4, code3);
        setupDeleteKeyListener(code5, code4);
        setupDeleteKeyListener(code6, code5);
    }

    private static class OTPTextWatcher implements TextWatcher {
        private final EditText nextView;
        public OTPTextWatcher(EditText current, EditText nextView) {
            this.nextView = nextView;
        }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 1 && nextView != null) {
                nextView.requestFocus();
            }
        }
        @Override public void afterTextChanged(Editable s) { }
    }

    private void setupDeleteKeyListener(final EditText currentField, final EditText previousField) {
        currentField.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
                if (currentField.getText().toString().isEmpty() && previousField != null) {
                    previousField.requestFocus();
                    previousField.setText("");
                    return true;
                }
            }
            return false;
        });
    }
}



///No Current User
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.app.AlertDialog;
//import android.app.ProgressDialog;
//import android.content.Intent;
//import android.os.Bundle;
//import android.os.CountDownTimer;
//import android.text.Editable;
//import android.text.TextUtils;
//import android.text.TextWatcher;
//import android.view.KeyEvent;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.widget.EditText;
//import android.widget.TextView;
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.google.firebase.FirebaseException;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.auth.PhoneAuthCredential;
//import com.google.firebase.auth.PhoneAuthOptions;
//import com.google.firebase.auth.PhoneAuthProvider;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Objects;
//import java.util.concurrent.TimeUnit;
//
//public class VerificationCode extends AppCompatActivity {
//
//    private TextView timerTextView, resend, numberView;
//    private EditText code1, code2, code3, code4, code5, code6;
//    private ProgressDialog progressDialog;
//    private String verificationId;
//    private PhoneAuthProvider.ForceResendingToken resendToken;
//    private FirebaseAuth mAuth;
//    private CountDownTimer countDownTimer;
//    private static final long COUNTDOWN_TIME = 120000;
//    private boolean isTimerRunning = false;
//
//    @SuppressLint("SetTextI18n")
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_verification_code);
//
//        mAuth = FirebaseAuth.getInstance();
//        resend = findViewById(R.id.resend);
//        timerTextView = findViewById(R.id.time);
//        numberView = findViewById(R.id.number);
//        code1 = findViewById(R.id.code1);
//        code2 = findViewById(R.id.code2);
//        code3 = findViewById(R.id.code3);
//        code4 = findViewById(R.id.code4);
//        code5 = findViewById(R.id.code5);
//        code6 = findViewById(R.id.code6);
//        Button submit = findViewById(R.id.button);
//        Button backButton = findViewById(R.id.back);
//
//        setupOTPInputs();
//        progressDialog = new ProgressDialog(this);
//        progressDialog.setMessage("Verifying...");
//        progressDialog.setCancelable(false);
//
//        resend.setEnabled(false);
//        resend.setText("");
//
//        String phoneNumber = getIntent().getStringExtra("phoneNumber");
//        if (!TextUtils.isEmpty(phoneNumber)) {
//            numberView.setText(phoneNumber);
//            startPhoneNumberVerification(phoneNumber);
//        } else {
//            Toast.makeText(this, "Phone number not available.", Toast.LENGTH_SHORT).show();
//        }
//
//        resend.setOnClickListener(v -> {
//            if (isTimerRunning) {
//                Toast.makeText(VerificationCode.this, "Please wait for cooldown", Toast.LENGTH_SHORT).show();
//            } else if (!TextUtils.isEmpty(phoneNumber) && resendToken != null) {
//                resend.setEnabled(false);
//                resend.setText("Resend");
//                resendVerificationCode(phoneNumber, resendToken);
//            } else {
//                Toast.makeText(VerificationCode.this, "Unable to resend code.", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        submit.setOnClickListener(v -> {
//            String code = getCodeFromEditTexts();
//            if (verificationId == null) {
//                Toast.makeText(VerificationCode.this, "OTP code error", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            if (!TextUtils.isEmpty(code) && code.length() == 6) {
//                progressDialog.show();
//                verifyPhoneNumberWithCode(verificationId, code);
//            } else {
//                Toast.makeText(VerificationCode.this, "Please enter the 6-digit code.", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        backButton.setOnClickListener(v -> finish());
//    }
//
//    private void startCountDownTimer() {
//        if (countDownTimer != null) countDownTimer.cancel();
//        isTimerRunning = true;
//        countDownTimer = new CountDownTimer(COUNTDOWN_TIME, 1000) {
//            @SuppressLint("SetTextI18n")
//            @Override
//            public void onTick(long millisUntilFinished) {
//                long secondsLeft = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished);
//                timerTextView.setText(secondsLeft + "s");
//            }
//            @SuppressLint("SetTextI18n")
//            @Override
//            public void onFinish() {
//                timerTextView.setText("0s");
//                isTimerRunning = false;
//                resend.setEnabled(true);
//                resend.setText("Resend");
//            }
//        }.start();
//    }
//
//    private void startPhoneNumberVerification(String phoneNumber) {
//        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
//                .setPhoneNumber(phoneNumber)
//                .setTimeout(60L, TimeUnit.SECONDS)
//                .setActivity(this)
//                .setCallbacks(mCallbacks)
//                .build();
//        PhoneAuthProvider.verifyPhoneNumber(options);
//    }
//
//    private final PhoneAuthProvider.OnVerificationStateChangedCallbacks mCallbacks =
//            new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
//                @Override
//                public void onVerificationCompleted(PhoneAuthCredential credential) {
//                    // Empty - no auto-processing
//                }
//
//                @Override
//                public void onVerificationFailed(FirebaseException e) {
//                    progressDialog.dismiss();
//                    Toast.makeText(VerificationCode.this, "Verification failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
//                }
//
//                @Override
//                public void onCodeSent(@NonNull String verifyId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
//                    verificationId = verifyId;
//                    resendToken = token;
//                    Toast.makeText(VerificationCode.this, "Verification code sent.", Toast.LENGTH_SHORT).show();
//                    startCountDownTimer();
//                    resend.setEnabled(false);
//                    resend.setText("Wait");
//                }
//            };
//
//    private void resendVerificationCode(String phoneNumber, PhoneAuthProvider.ForceResendingToken token) {
//        PhoneAuthOptions options = PhoneAuthOptions.newBuilder(mAuth)
//                .setPhoneNumber(phoneNumber)
//                .setTimeout(60L, TimeUnit.SECONDS)
//                .setActivity(this)
//                .setCallbacks(mCallbacks)
//                .setForceResendingToken(token)
//                .build();
//        PhoneAuthProvider.verifyPhoneNumber(options);
//        Toast.makeText(this, "Resending code...", Toast.LENGTH_SHORT).show();
//    }
//
//
//    private void verifyPhoneNumberWithCode(String verificationId, String code) {
//        progressDialog.show();
//
//        PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
//        FirebaseUser currentUser = mAuth.getCurrentUser();
//
//        if (currentUser != null) {
//            DatabaseReference userRef = FirebaseDatabase.getInstance()
//                    .getReference("users")
//                    .child(currentUser.getUid());
//            userRef.get().addOnCompleteListener(task -> {
//                if (task.isSuccessful()) {
//                    DataSnapshot snapshot = task.getResult();
//                    Boolean isVerified = snapshot.child("phoneVerified").getValue(Boolean.class);
//                    if (isVerified != null && isVerified) {
//                        progressDialog.dismiss();
//                        // If already verified then show our custom alert dialog.
//                        showCustomVerificationDialog();
//                    } else {
//                        mAuth.signInWithCredential(credential).addOnCompleteListener(credentialTask -> {
//                            progressDialog.dismiss();
//                            if (credentialTask.isSuccessful()) {
//                                Map<String, Object> updates = new HashMap<>();
//                                updates.put("phoneVerified", true);
//                                String phone = getIntent().getStringExtra("phoneNumber");
//                                if (!TextUtils.isEmpty(phone)) {
//                                    updates.put("phoneNumber", phone);
//                                }
//                                userRef.updateChildren(updates).addOnCompleteListener(updateTask -> {
//                                    if (updateTask.isSuccessful()) {
//                                        /// Show custom alert dialog upon successful update.
//                                        showCustomVerificationDialog();
//                                    } else {
//                                        Toast.makeText(VerificationCode.this, "Database update failed: " +
//                                                Objects.requireNonNull(updateTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
//                                    }
//                                });
//                            } else {
//                                Toast.makeText(VerificationCode.this, "Verification error: " +
//                                        Objects.requireNonNull(credentialTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }
//                } else {
//                    progressDialog.dismiss();
//                    Toast.makeText(VerificationCode.this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
//                }
//            });
//        } else {
//            mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
//                progressDialog.dismiss();
//                if (task.isSuccessful()) {
//                    FirebaseUser firebaseUser = mAuth.getCurrentUser();
//                    if (firebaseUser != null) {
//                        DatabaseReference userRef = FirebaseDatabase.getInstance()
//                                .getReference("users")
//                                .child(firebaseUser.getUid());
//                        Map<String, Object> updates = new HashMap<>();
//                        updates.put("phoneVerified", true);
//                        String phone = getIntent().getStringExtra("phoneNumber");
//                        if (!TextUtils.isEmpty(phone)) {
//                            updates.put("phoneNumber", phone);
//                        }
//                        userRef.updateChildren(updates).addOnCompleteListener(updateTask -> {
//                            if (updateTask.isSuccessful()) {
//                                // Clear back stack
//                                Intent intent = new Intent(VerificationCode.this, BottomNavigation.class);
//                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                                startActivity(intent);
//                                overridePendingTransition(0, 0);
//                                finish();
//                            } else {
//                                Toast.makeText(VerificationCode.this, "Database update failed: " +
//                                        Objects.requireNonNull(updateTask.getException()).getMessage(), Toast.LENGTH_SHORT).show();
//                            }
//                        });
//                    }
//                } else {
//                    Toast.makeText(VerificationCode.this, "Verification error: " +
//                            Objects.requireNonNull(task.getException()).getMessage(), Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//    }
//
//
//    /**
//     * Displays a professional custom alert dialog informing the user that verification was successful.
//     * When the user clicks the "OK" button, they are redirected to the Login activity.
//     */
//    private void showCustomVerificationDialog() {
//        /// Inflate the custom dialog layout.
//        LayoutInflater inflater = getLayoutInflater();
//        View dialogView = inflater.inflate(R.layout.custom_alert_verification, null);
//
//        AlertDialog.Builder builder = new AlertDialog.Builder(VerificationCode.this);
//        builder.setView(dialogView);
//        builder.setCancelable(false);
//
//        final AlertDialog alertDialog = builder.create();
//
//        /// Optionally, you can set or modify the title or message here if needed.
//        dialogView.findViewById(R.id.tvAlertTitle);
//        dialogView.findViewById(R.id.tvAlertMessage);
//
//        Button btnOk = dialogView.findViewById(R.id.btnAlertOk);
//        btnOk.setOnClickListener(v -> {
//            alertDialog.dismiss();
//            /// Redirect to the Login activity when the "OK" button is pressed.
//            Intent intent = new Intent(VerificationCode.this, Login.class);
//            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//            startActivity(intent);
//            overridePendingTransition(0, 0);
//            finish();
//        });
//
//        alertDialog.show();
//    }
//
//
//
//    private String getCodeFromEditTexts() {
//        return code1.getText().toString().trim() +
//                code2.getText().toString().trim() +
//                code3.getText().toString().trim() +
//                code4.getText().toString().trim() +
//                code5.getText().toString().trim() +
//                code6.getText().toString().trim();
//    }
//
//    private void setupOTPInputs() {
//        code1.addTextChangedListener(new OTPTextWatcher(code1, code2));
//        code2.addTextChangedListener(new OTPTextWatcher(code2, code3));
//        code3.addTextChangedListener(new OTPTextWatcher(code3, code4));
//        code4.addTextChangedListener(new OTPTextWatcher(code4, code5));
//        code5.addTextChangedListener(new OTPTextWatcher(code5, code6));
//        code6.addTextChangedListener(new OTPTextWatcher(code6, null));
//
//        setupDeleteKeyListener(code2, code1);
//        setupDeleteKeyListener(code3, code2);
//        setupDeleteKeyListener(code4, code3);
//        setupDeleteKeyListener(code5, code4);
//        setupDeleteKeyListener(code6, code5);
//    }
//
//    private static class OTPTextWatcher implements TextWatcher {
//        private final EditText nextView;
//        public OTPTextWatcher(EditText current, EditText nextView) {
//            this.nextView = nextView;
//        }
//        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
//        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
//            if (s.length() == 1 && nextView != null) {
//                nextView.requestFocus();
//            }
//        }
//        @Override public void afterTextChanged(Editable s) { }
//    }
//
//    private void setupDeleteKeyListener(final EditText currentField, final EditText previousField) {
//        currentField.setOnKeyListener((v, keyCode, event) -> {
//            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL) {
//                if (currentField.getText().toString().isEmpty() && previousField != null) {
//                    previousField.requestFocus();
//                    previousField.setText("");
//                    return true;
//                }
//            }
//            return false;
//        });
//    }
//}
