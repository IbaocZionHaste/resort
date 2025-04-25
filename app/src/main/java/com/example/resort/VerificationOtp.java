
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.FirebaseException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

public class VerificationOtp extends AppCompatActivity {

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
        setContentView(R.layout.activity_verification_otp);

        // Ensure phoneNumber extra exists, otherwise return to SignUpNext
        Intent incoming = getIntent();
        Bundle extras = incoming.getExtras();
        if (extras == null || !extras.containsKey("phoneNumber")) {
            Toast.makeText(this, "Phone number missing. Please enter again.", Toast.LENGTH_LONG).show();
            Intent back = new Intent(this, SignUpNext.class);
            if (extras != null) back.putExtras(extras);
            startActivity(back);
            finish();
            return;
        }

        String phoneNumber = incoming.getStringExtra("phoneNumber");

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

        // Display phone and automatically start verification
        numberView.setText(phoneNumber);

        setupOTPInputs();
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Verifying...");
        progressDialog.setCancelable(false);

        // Initiate phone auth
        startPhoneNumberVerification(phoneNumber);

        resend.setEnabled(false);
        resend.setText("");

        resend.setOnClickListener(v -> {
            if (isTimerRunning) {
                Toast.makeText(this, "Please wait for cooldown", Toast.LENGTH_SHORT).show();
            } else if (!TextUtils.isEmpty(phoneNumber) && resendToken != null) {
                resend.setEnabled(false);
                resend.setText("Resend");
                resendVerificationCode(phoneNumber, resendToken);
            } else {
                Toast.makeText(this, "Unable to resend code.", Toast.LENGTH_SHORT).show();
            }
        });

        submit.setOnClickListener(v -> {
            String code = getCodeFromEditTexts();
            if (verificationId == null || code.length() != 6) {
                Toast.makeText(this, "Please enter the 6-digit code.", Toast.LENGTH_SHORT).show();
                return;
            }
            progressDialog.show();
            PhoneAuthCredential credential = PhoneAuthProvider.getCredential(verificationId, code);
            mAuth.signInWithCredential(credential).addOnCompleteListener(task -> {
                progressDialog.dismiss();
                if (task.isSuccessful()) {
                    showCustomVerificationDialog(extras);
                } else {
                    Toast.makeText(this, "Verification error: " + task.getException().getMessage(),
                            Toast.LENGTH_LONG).show();
                }
            });
        });

        backButton.setOnClickListener(v -> {
            Intent back = new Intent(this, SignUpNext.class);
            back.putExtras(extras);
            startActivity(back);
            finish();
        });
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
                @Override public void onVerificationCompleted(PhoneAuthCredential credential) {}
                @Override public void onVerificationFailed(FirebaseException e) {
                    progressDialog.dismiss();
                    Toast.makeText(VerificationOtp.this, "Verification failed: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                }
                @Override public void onCodeSent(@NonNull String verifyId, @NonNull PhoneAuthProvider.ForceResendingToken token) {
                    verificationId = verifyId;
                    resendToken = token;
                    Toast.makeText(VerificationOtp.this, "Code sent.", Toast.LENGTH_SHORT).show();
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

    private void startCountDownTimer() {
        if (countDownTimer != null) countDownTimer.cancel();
        isTimerRunning = true;
        countDownTimer = new CountDownTimer(COUNTDOWN_TIME, 1000) {
            @SuppressLint("SetTextI18n")
            @Override public void onTick(long millisUntilFinished) {
                timerTextView.setText(TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) + "s");
            }
            @SuppressLint("SetTextI18n")
            @Override public void onFinish() {
                timerTextView.setText("0s");
                isTimerRunning = false;
                resend.setEnabled(true);
                resend.setText("Resend");
            }
        }.start();
    }

    private void showCustomVerificationDialog(Bundle extras) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View dialogView = inflater.inflate(R.layout.custom_alert_verification, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        AlertDialog alertDialog = builder.create();

        Button btnOk = dialogView.findViewById(R.id.btnAlertOk);
        btnOk.setOnClickListener(v -> {
            alertDialog.dismiss();
            Intent intent = new Intent(VerificationOtp.this, SignUp.class);
            intent.putExtras(extras);
            intent.putExtra("phoneVerified", true);
            startActivity(intent);
            finish();
        });

        alertDialog.show();
    }

    private String getCodeFromEditTexts() {
        return code1.getText().toString().trim()
                + code2.getText().toString().trim()
                + code3.getText().toString().trim()
                + code4.getText().toString().trim()
                + code5.getText().toString().trim()
                + code6.getText().toString().trim();
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
        public OTPTextWatcher(EditText current, EditText nextView) { this.nextView = nextView; }
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 1 && nextView != null) nextView.requestFocus();
        }
        @Override public void afterTextChanged(Editable s) {}
    }

    private void setupDeleteKeyListener(final EditText currentField, final EditText previousField) {
        currentField.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_DEL
                    && currentField.getText().toString().isEmpty() && previousField != null) {
                previousField.requestFocus(); previousField.setText(""); return true;
            }
            return false;
        });
    }
}
