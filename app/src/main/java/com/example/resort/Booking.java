package com.example.resort;

import android.annotation.SuppressLint;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.R;
import com.example.resort.addcart.data.CartAdapter;
import com.example.resort.addcart.data.CartItem;
import com.example.resort.addcart.data.CartManager;
import com.example.resort.addcart.data.CartUpdateListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class Booking extends AppCompatActivity implements CartUpdateListener {

    private TextView totalPriceText;
    private CartAdapter adapter;
    private EditText editTextDate, editTextTimeIn, editTextTimeOut;
    private TextView messageTextView;

    /// User-specific CartManager instance.
    private CartManager cartManager;

    private List<CartItem> cartItems;

    /// current cart items

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_booking);

        /// Retrieve the current Firebase user.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            /// If user is not logged in, exit this activity.
            finish();
            return;
        }
        String userId = currentUser.getUid();

        /// Initialize the CartManager using the current user's UID.
        cartManager = CartManager.getInstance(this, userId);
        cartItems = cartManager.getCartItems();


        /// Checkout button and back arrow.
        Button btnCheckout = findViewById(R.id.next);
        ImageView backArrow = findViewById(R.id.backArrow);
        backArrow.setOnClickListener(v -> onBackPressed());

        /// Adjust layout for system insets (edge-to-edge).
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        /// Fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        /// Setup RecyclerView for Cart Items.
        RecyclerView cartRecyclerView = findViewById(R.id.recyclerView);
        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        totalPriceText = findViewById(R.id.totalPriceText);
        adapter = new CartAdapter(this, cartManager.getCartItems(), this);
        cartRecyclerView.setAdapter(adapter);
        updateTotalPrice();

        /// Set button click to proceed to checkout.
        btnCheckout.setOnClickListener(v -> goToCheckout());

        /// Initialize EditText fields and message TextView for date and time inputs.
        editTextDate = findViewById(R.id.editTextDate);
        editTextTimeIn = findViewById(R.id.editTextTimeIn);
        editTextTimeOut = findViewById(R.id.editTextTimeOut);
        messageTextView = findViewById(R.id.message);

        /// If a Room-category item was removed elsewhere, reset fields
        CartItem roomItem = null;
        for (CartItem ci : cartManager.getCartItems()) {
            if ("Room".equalsIgnoreCase(ci.getCategory())) {
                roomItem = ci;
                break;
            }
        }
        if (roomItem == null && !cartManager.hasRoomItems()) {
            /// No room items: ensure date/time are cleared and editable
            setupDateAndTimePickers();
        }

        /// Set up the date and time pickers.
        setupDateAndTimePickers();
        setupPickers();

    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        adapter.updateCartItems(cartManager.getCartItems());
        adapter.notifyDataSetChanged();
        updateTotalPrice();
        setupDateAndTimePickers();
    }

    @Override
    public void onCartUpdated() {
        updateTotalPrice();
        setupDateAndTimePickers();
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    private void updateTotalPrice() {
        double total = 0.0;
        List<CartItem> items = cartManager.getCartItems();
        for (CartItem item : items) {
            total += item.getPrice() * item.getQuantity();
        }
        totalPriceText.setText("Total: ₱" + String.format("%.2f", total));
    }

    @SuppressLint("SetTextI18n")
    private void goToCheckout() {
        /// Ensure the date and time fields are filled in.
        if (editTextDate.getText().toString().trim().isEmpty() ||
                editTextTimeIn.getText().toString().trim().isEmpty() ||
                editTextTimeOut.getText().toString().trim().isEmpty()) {
            messageTextView.setText("Please fill in the Date, Time-In, and Time-Out fields.");
            messageTextView.setTextColor(Color.RED);
            return;
        }

        /// Process cart items.
        double total = 0.0;
        List<CartItem> items = cartManager.getCartItems();
        ArrayList<String> itemDetails = new ArrayList<>();

        for (CartItem item : items) {
            double itemTotal = item.getPrice() * item.getQuantity();
            total += itemTotal;
            @SuppressLint("DefaultLocale")
            String detail = String.format("%-40s||%10.2f",
                    item.getName() + " (" + item.getCategory() + ") x" + item.getQuantity() + " -",
                    itemTotal);
            itemDetails.add(detail);
        }

        if (total == 0) {
            totalPriceText.setText("Cart is empty!");
            totalPriceText.setTextColor(Color.RED);
            return;
        }

        /// Generate formatted booking info: "Date: March 9 2025 (8am - 4pm)".
        String bookingInfo = getFormattedBookingInfo(editTextDate.getText().toString(),
                editTextTimeIn.getText().toString(), editTextTimeOut.getText().toString());

        /// Pass cart details and booking info to the BookingReceipt activity.
        Intent intent = new Intent(this, BookingReceipt.class);
        intent.putStringArrayListExtra("CART_ITEMS", itemDetails);
        intent.putExtra("TOTAL_PRICE", total);
        intent.putExtra("BOOKING_DATE", bookingInfo);
        startActivity(intent);
        overridePendingTransition(0, 0);
        finish();
    }


    /// Room Only Automatic Set Date and Time
    @SuppressLint("DefaultLocale")
    private void setupDateAndTimePickers() {
        cartItems = cartManager.getCartItems();

        if (cartManager.hasRoomItems()) {
            /// Auto-fill tomorrow's date
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            String formattedDate = String.format("%02d/%02d/%04d",
                    calendar.get(Calendar.MONTH) + 1,
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.YEAR));
            editTextDate.setText(formattedDate);

            /// Fixed time-in and time-out for Room
            editTextTimeIn.setText("03:00 PM");
            editTextTimeOut.setText("11:00 AM");

            /// Make them semi-transparent
            editTextTimeIn.setAlpha(0.5f);
            editTextTimeOut.setAlpha(0.5f);

            /// Disable editing
            editTextTimeIn.setEnabled(false);
            editTextTimeOut.setEnabled(false);

            /// Calculate duration: 3:00 PM to 11:00 AM next day = 20 hours
            int inHour = 15;  /// 3 PM
            int inMin = 0;
            int outHour = 11; /// 11 AM
            int outMin = 0;

            int diffMins = ((24 - inHour + outHour) * 60 + (outMin - inMin));
            long diffHrs = diffMins / 60;

            messageTextView.setTextColor(Color.BLACK);
            messageTextView.setText(String.format(
                    Locale.getDefault(),
                    "Please be informed. Check‑out time is %d hour%s after Check‑in.",
                    diffHrs,
                    diffHrs > 1 ? "s" : ""
            ));
        } else {
            /// Reset everything and allow manual input
            editTextDate.setText("");
            editTextTimeIn.setText("");
            editTextTimeOut.setText("");

            /// Re-enable fields
            editTextDate.setEnabled(true);
            editTextTimeIn.setEnabled(true);
            editTextTimeOut.setEnabled(true);

            /// Reset opacity (make fully visible again)
            editTextTimeIn.setAlpha(1.0f);
            editTextTimeOut.setAlpha(1.0f);

            /// Optional: Reset text color if you changed it earlier
            editTextTimeIn.setTextColor(Color.BLACK);
            editTextTimeOut.setTextColor(Color.BLACK);

            messageTextView.setText("Please be informed. bookings must be made at least 1 day in advance. Check-out time is hours after the Check-in time.");  /// Clear the message
        }
    }


    ///This check in time picker
    @SuppressLint("DefaultLocale")
    private void setupPickers() {

        /// Prevent keyboard & ensure pickers only
        editTextDate.setFocusable(false);
        editTextTimeIn.setFocusable(false);
        editTextTimeOut.setFocusable(false);

        /// --- DATE PICKER (prevent past dates) ---
        editTextDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            DatePickerDialog dlg = new DatePickerDialog(
                    this,
                    (view, year, month, day) ->
                            editTextDate.setText(String.format("%02d/%02d/%04d", month+1, day, year)),
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
            );
            /// Disable today: minimum = tomorrow
            c.add(Calendar.DAY_OF_MONTH, 1);

            /// min = tomorrow :contentReference[oaicite:3]{index=3}
            dlg.getDatePicker().setMinDate(c.getTimeInMillis());
            dlg.show();
        });

        /// --- TIME-IN PICKER (6 AM → 6 PM office hours + custom gate) ---
        editTextTimeIn.setOnClickListener(v -> {
            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
            int initH = now.get(Calendar.HOUR_OF_DAY);
            int initM = now.get(Calendar.MINUTE);

            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
                /// 0) CUSTOM GATE: only 6–9 any minute, PLUS exactly 10:00 AM
                if (hourOfDay < 6 || hourOfDay > 10) {
                    Toast.makeText(this,
                            "Please pick between 6 AM and 10 AM only",
                            Toast.LENGTH_SHORT).show();
                    editTextTimeIn.setText("");
                    editTextTimeOut.setText("");
                    return;
                }

                if (hourOfDay == 10 && minute != 0) {
                    Toast.makeText(this,
                            "10 AM must be exactly 10:00",
                            Toast.LENGTH_SHORT).show();
                    editTextTimeIn.setText("");
                    editTextTimeOut.setText("");
                    return;
                }
                /// 1) Office hours: 6 AM–6 PM > this Not Use because 6 to 10 am only the user pick the time other not allow
                int inMins = hourOfDay * 60 + minute;
                final int OPEN = 6 * 60;   /// 6:00 AM
                final int CLOSE = 18 * 60; /// 6:00 PM
                if (inMins < OPEN || inMins >= CLOSE) {
                    Toast.makeText(this,
                            "Office hours are 6:00 AM–6:00 PM only",
                            Toast.LENGTH_SHORT).show();
                    editTextTimeIn.setText("");
                    editTextTimeOut.setText("");
                    return;
                }

                /// 2) Must book at least 1 hr before closing > this Not Use because 6 to 10 am only the user pick the time other not allow
                if (CLOSE - inMins < 60) {
                    Toast.makeText(this,
                            "You need to book at least 1 full hour before 6 PM",
                            Toast.LENGTH_SHORT).show();
                    editTextTimeIn.setText("");
                    editTextTimeOut.setText("");
                    return;
                }


                /// 3) Compute check-out: up to 8 hrs, capped at 6 PM
                int outMins = Math.min(inMins + 8*60, CLOSE);
                long diffHrs = (outMins - inMins) / 60;
                /// 4) Format & display
                String inStr = formatTime(hourOfDay, minute);
                int outH = outMins / 60;
                int outMin = outMins % 60;
                String outStr = formatTime(outH, outMin);

                editTextTimeIn.setText(inStr);
                editTextTimeOut.setText(outStr);
                messageTextView.setTextColor(Color.BLACK);
                messageTextView.setText(String.format(
                        Locale.getDefault(),
                        "Please be informed. Check-out time is %d hour%s after Check-in.",
                        diffHrs,
                        diffHrs > 1 ? "s" : ""
                ));
            },
                    initH, initM, false)
                    /// show TimePickerDialog :contentReference[oaicite:4]{index=4}
                    .show();
        });
    }


    /// Unchanged formatter
    @SuppressLint("DefaultLocale")
    private String formatTime(int hourOfDay, int minute) {
        int dispH = hourOfDay % 12;
        if (dispH == 0) dispH = 12;
        String amPm = (hourOfDay < 12) ? "AM" : "PM";
        return String.format("%d:%02d %s", dispH, minute, amPm);
    }

    private String getFormattedBookingInfo(String dateStr, String timeInStr, String timeOutStr) {
        try {
            SimpleDateFormat inDate  = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
            Date parsedDate          = inDate.parse(dateStr);
            SimpleDateFormat outDate = new SimpleDateFormat("MMMM d yyyy", Locale.getDefault());
            String formattedDate     = outDate.format(parsedDate);

            return "Date: " + formattedDate
                    + " (" + timeInStr.toLowerCase()
                    + " - " + timeOutStr.toLowerCase() + ")";
        } catch (ParseException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            return "";
        }
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

}


///This code not filter 6am to 10am only allow and the other not allow to pick
//    @SuppressLint("DefaultLocale")
//    private void setupPickers() {
//        editTextDate.setFocusable(false);
//        editTextTimeIn.setFocusable(false);
//        editTextTimeOut.setFocusable(false);
//
//        /// --- DATE PICKER (prevent past dates) ---
//        editTextDate.setOnClickListener(v -> {
//            Calendar c = Calendar.getInstance();
//            DatePickerDialog dlg = new DatePickerDialog(
//                    this,
//                    (view, year, month, day) ->
//                            editTextDate.setText(String.format("%02d/%02d/%04d", month+1, day, year)),
//                    c.get(Calendar.YEAR),
//                    c.get(Calendar.MONTH),
//                    c.get(Calendar.DAY_OF_MONTH)
//            );
//
//            /// Disable today: set minimum date to tomorrow
//            c.add(Calendar.DAY_OF_MONTH, 1);
//            dlg.getDatePicker().setMinDate(c.getTimeInMillis());
//            dlg.show();
//        });

//
//        /// --- TIME-IN PICKER (6 AM → 6 PM office hours) ---
//        editTextTimeIn.setOnClickListener(v -> {
//            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
//            int initH = now.get(Calendar.HOUR_OF_DAY);
//            int initM = now.get(Calendar.MINUTE);
//
//            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
//                int inMins = hourOfDay * 60 + minute;
//                final int OPEN = 6 * 60;   // 6:00 AM
//                final int CLOSE = 18 * 60; // 6:00 PM
//
//                /// 1) Must be within 6 AM–6 PM
//                if (inMins < OPEN || inMins >= CLOSE) {
//                    Toast.makeText(this,
//                            "Office hours are 6:00 AM–6:00 PM only",
//                            Toast.LENGTH_SHORT).show();
//                    editTextTimeIn.setText("");
//                    editTextTimeOut.setText("");
//                    return;
//                }
//
//                /// 2) Need at least 1 hr before closing
//                if (CLOSE - inMins < 60) {
//                    Toast.makeText(this,
//                            "You need to book at least 1 full hour before 6 PM",
//                            Toast.LENGTH_SHORT).show();
//                    editTextTimeIn.setText("");
//                    editTextTimeOut.setText("");
//                    return;
//                }
//
//                /// 3) Calculate check‑out: up to 8 hrs max, capped at 6 PM
//                int outMins = Math.min(inMins + 8*60, CLOSE);
//                long diffHrs = (outMins - inMins) / 60;
//
//                /// 4) Format & display
//                String inStr = formatTime(hourOfDay, minute);
//                int outH = outMins / 60;
//                int outMin = outMins % 60;
//                String outStr = formatTime(outH, outMin);
//
//                editTextTimeIn.setText(inStr);
//                editTextTimeOut.setText(outStr);
//                messageTextView.setTextColor(Color.BLACK);
//                messageTextView.setText(String.format(
//                        Locale.getDefault(),
//                        "Please be informed. Check‑out time is %d hour%s after Check‑in.",
//                        diffHrs,
//                        diffHrs > 1 ? "s" : ""
//                ));
//
//            }, initH, initM, false).show();
//        });
//    }


/// This code 12am 6 am office hour
//    private void setupDateAndTimePickers() {
//        editTextDate.setFocusable(false);
//        editTextTimeIn.setFocusable(false);
//        editTextTimeOut.setFocusable(false);
//
//        /// --- DATE PICKER ---
//        editTextDate.setOnClickListener(v -> {
//            Calendar c = Calendar.getInstance();
//            new DatePickerDialog(this,
//                    (view, year, month, day) ->
//                            editTextDate.setText(String.format("%02d/%02d/%04d", month+1, day, year)),
//                    c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH))
//                    .show();
//        });
//
//        /// --- TIME-IN PICKER w/ office-hours + duration logic ---
//        editTextTimeIn.setOnClickListener(v -> {
//            Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
//            int initH = now.get(Calendar.HOUR_OF_DAY);
//            int initM = now.get(Calendar.MINUTE);
//
//            new TimePickerDialog(this, (view, hourOfDay, minute) -> {
//                int inMins = hourOfDay * 60 + minute;
//
//                /// disallow midnight–5:59 AM
//                if (inMins < 360) {
//                    Toast.makeText(this,
//                            "Office hours are 6:00 AM–12:00 AM only",
//                            Toast.LENGTH_SHORT).show();
//                    editTextTimeIn.setText("");
//                    editTextTimeOut.setText("");
//                    return;
//                }
//
//                /// Remaining minutes until midnight
//                int untilMid = 24*60 - inMins;
//                int outMins;
//                long diffHrs;
//
//                /// ONLY allow exactly 11:00 PM → gives 60 mins to midnight
//                if (inMins == 23*60) {
//                    outMins  = 24*60;
//                    diffHrs  = 1;
//                }
//                else if (inMins > 23*60) {
//                    // anything past 11:00 PM is <60 mins
//                    Toast.makeText(this,
//                            "Minimum booking is 1 full hour before midnight",
//                            Toast.LENGTH_SHORT).show();
//                    editTextTimeIn.setText("");
//                    editTextTimeOut.setText("");
//                    return;
//                }
//                else {
//                    // Must be able to fit 2 h min before midnight
//                    if (untilMid < 2*60) {
//                        Toast.makeText(this,
//                                "Minimum booking is 2 hours before midnight",
//                                Toast.LENGTH_SHORT).show();
//                        editTextTimeIn.setText("");
//                        editTextTimeOut.setText("");
//                        return;
//                    }
//                    // Normal: up to 8 h, cap at midnight
//                    outMins = Math.min(inMins + 8*60, 24*60);
//                    diffHrs = (outMins - inMins) / 60;
//                }
//
//                // Format and set fields
//                String inStr  = formatTime(hourOfDay, minute);
//                int  outH     = outMins / 60;
//                int  outMin   = outMins % 60;
//                String outStr = formatTime(outH, outMin);
//
//                editTextTimeIn .setText(inStr);
//                editTextTimeOut.setText(outStr);
//
//                messageTextView.setTextColor(Color.BLACK);
//                messageTextView.setText(String.format(
//                        Locale.getDefault(),
//                        "Please be informed. Check-out time is %d hour%s after Check-in.",
//                        diffHrs, diffHrs>1?"s":""));
//
//            }, initH, initM, false).show();
//        });
//
//    }
//
//
//    @SuppressLint("DefaultLocale")
//    private String formatTime(int hourOfDay, int minute) {
//        int dispH = hourOfDay % 12;
//        if (dispH == 0) dispH = 12;
//        String amPm = (hourOfDay < 12) ? "AM" : "PM";
//        return String.format("%d:%02d %s", dispH, minute, amPm);
//    }


///No Current User
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.app.DatePickerDialog;
//import android.app.TimePickerDialog;
//import android.content.Intent;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.R;
//import com.example.resort.addcart.data.CartAdapter;
//import com.example.resort.addcart.data.CartItem;
//import com.example.resort.addcart.data.CartManager;
//import com.example.resort.addcart.data.CartUpdateListener;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.time.LocalTime;
//import java.time.format.DateTimeFormatter;
//import java.time.temporal.ChronoUnit;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//import java.util.TimeZone;
//
//public class Booking extends AppCompatActivity implements CartUpdateListener {
//
//    private TextView totalPriceText;
//    private CartAdapter adapter;
//    private EditText editTextDate, editTextTimeIn, editTextTimeOut;
//    private TextView messageTextView;
//    // User-specific CartManager instance.
//    private CartManager cartManager;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_booking);
//
//        // Retrieve the current Firebase user.
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            // If user is not logged in, exit this activity.
//            finish();
//            return;
//        }
//        String userId = currentUser.getUid();
//
//        // Initialize the CartManager using the current user's UID.
//        cartManager = CartManager.getInstance(this, userId);
//
//        // Checkout button and back arrow.
//        Button btnCheckout = findViewById(R.id.next);
//        ImageView backArrow = findViewById(R.id.backArrow);
//        backArrow.setOnClickListener(v -> onBackPressed());
//
//        // Adjust layout for system insets (edge-to-edge).
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Fullscreen
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//        // Setup RecyclerView for Cart Items.
//        RecyclerView cartRecyclerView = findViewById(R.id.recyclerView);
//        cartRecyclerView.setLayoutManager(new LinearLayoutManager(this));
//        totalPriceText = findViewById(R.id.totalPriceText);
//        adapter = new CartAdapter(this, cartManager.getCartItems(), this);
//        cartRecyclerView.setAdapter(adapter);
//        updateTotalPrice();
//
//        // Set button click to proceed to checkout.
//        btnCheckout.setOnClickListener(v -> goToCheckout());
//
//        // Initialize EditText fields and message TextView for date and time inputs.
//        editTextDate = findViewById(R.id.editTextDate);
//        editTextTimeIn = findViewById(R.id.editTextTimeIn);
//        editTextTimeOut = findViewById(R.id.editTextTimeOut);
//        messageTextView = findViewById(R.id.message);
//
//        // Set up the date and time pickers.
//        setupDateAndTimePickers();
//    }
//
//    @SuppressLint("NotifyDataSetChanged")
//    @Override
//    protected void onResume() {
//        super.onResume();
//        adapter.updateCartItems(cartManager.getCartItems());
//        adapter.notifyDataSetChanged();
//        updateTotalPrice();
//    }
//
//    @Override
//    public void onCartUpdated() {
//        updateTotalPrice();
//    }
//
//    @SuppressLint({"DefaultLocale", "SetTextI18n"})
//    private void updateTotalPrice() {
//        double total = 0.0;
//        List<CartItem> items = cartManager.getCartItems();
//        for (CartItem item : items) {
//            total += item.getPrice() * item.getQuantity();
//        }
//        totalPriceText.setText("Total: ₱" + String.format("%.2f", total));
//    }
//
//    @SuppressLint("SetTextI18n")
//    private void goToCheckout() {
//        // Ensure the date and time fields are filled in.
//        if (editTextDate.getText().toString().trim().isEmpty() ||
//                editTextTimeIn.getText().toString().trim().isEmpty() ||
//                editTextTimeOut.getText().toString().trim().isEmpty()) {
//            messageTextView.setText("Please fill in the Date, Time-In, and Time-Out fields.");
//            messageTextView.setTextColor(Color.RED);
//            return;
//        }
//
//        // Process cart items.
//        double total = 0.0;
//        List<CartItem> items = cartManager.getCartItems();
//        ArrayList<String> itemDetails = new ArrayList<>();
//
//        for (CartItem item : items) {
//            double itemTotal = item.getPrice() * item.getQuantity();
//            total += itemTotal;
//            @SuppressLint("DefaultLocale")
//            String detail = String.format("%-40s||%10.2f",
//                    item.getName() + " (" + item.getCategory() + ") x" + item.getQuantity() + " -",
//                    itemTotal);
//            itemDetails.add(detail);
//        }
//
//        if (total == 0) {
//            totalPriceText.setText("Cart is empty!");
//            totalPriceText.setTextColor(Color.RED);
//            return;
//        }
//
//        // Generate formatted booking info: "Date: March 9 2025 (8am - 4pm)".
//        String bookingInfo = getFormattedBookingInfo(editTextDate.getText().toString(),
//                editTextTimeIn.getText().toString(), editTextTimeOut.getText().toString());
//
//        // Pass cart details and booking info to the BookingReceipt activity.
//        Intent intent = new Intent(this, BookingReceipt.class);
//        intent.putStringArrayListExtra("CART_ITEMS", itemDetails);
//        intent.putExtra("TOTAL_PRICE", total);
//        intent.putExtra("BOOKING_DATE", bookingInfo);
//        startActivity(intent);
//        overridePendingTransition(0, 0);
//        finish();
//    }
//
//    private void setupDateAndTimePickers() {
//        // Disable keyboard popup.
//        editTextDate.setFocusable(false);
//        editTextTimeIn.setFocusable(false);
//        editTextTimeOut.setFocusable(false);
//
//        // Date Picker for the Date field.
//        editTextDate.setOnClickListener(v -> {
//            Calendar calendar = Calendar.getInstance();
//            int year = calendar.get(Calendar.YEAR);
//            int month = calendar.get(Calendar.MONTH);
//            int day = calendar.get(Calendar.DAY_OF_MONTH);
//
//            @SuppressLint("DefaultLocale") DatePickerDialog datePickerDialog = new DatePickerDialog(Booking.this,
//                    (view, selectedYear, selectedMonth, selectedDay) -> {
//                        // Format date as MM/dd/yyyy.
//                        editTextDate.setText(String.format("%02d/%02d/%04d", selectedMonth + 1, selectedDay, selectedYear));
//                    }, year, month, day);
//            datePickerDialog.show();
//        });
//
//        // Time Picker for Time-In.
//        editTextTimeIn.setOnClickListener(v -> {
//            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
//            int hour = calendar.get(Calendar.HOUR_OF_DAY);
//            int minute = calendar.get(Calendar.MINUTE);
//
//            TimePickerDialog timePickerDialog = new TimePickerDialog(Booking.this,
//                    (view, hourOfDay, minute1) -> {
//                        // Set Time-In field.
//                        String formattedTimeIn = formatTime(hourOfDay, minute1);
//                        editTextTimeIn.setText(formattedTimeIn);
//
//                        // Automatically set Time-Out to be 8 hours later.
//                        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Manila"));
//                        cal.set(Calendar.HOUR_OF_DAY, hourOfDay);
//                        cal.set(Calendar.MINUTE, minute1);
//                        cal.add(Calendar.HOUR_OF_DAY, 8);
//                        int outHour = cal.get(Calendar.HOUR_OF_DAY);
//                        int outMinute = cal.get(Calendar.MINUTE);
//                        String formattedTimeOut = formatTime(outHour, outMinute);
//                        editTextTimeOut.setText(formattedTimeOut);
//
//                        updateDurationMessage();
//                    }, hour, minute, false);
//            timePickerDialog.show();
//        });
//
//        // Manual Time-Out selection is disabled since it's auto-calculated.
//    }
//
//    @SuppressLint("DefaultLocale")
//    private String formatTime(int hourOfDay, int minute) {
//        int displayHour = hourOfDay % 12;
//        if (displayHour == 0) displayHour = 12;
//        String amPm = (hourOfDay < 12) ? "AM" : "PM";
//        return String.format("%d:%02d %s", displayHour, minute, amPm);
//    }
//
//    @SuppressLint("SetTextI18n")
//    private void updateDurationMessage() {
//        String timeInStr = editTextTimeIn.getText().toString();
//        String timeOutStr = editTextTimeOut.getText().toString();
//
//        if (timeInStr.isEmpty() || timeOutStr.isEmpty()) return;
//
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault());
//        try {
//            LocalTime timeIn = LocalTime.parse(timeInStr, formatter);
//            LocalTime timeOut = LocalTime.parse(timeOutStr, formatter);
//
//            long diffMinutes = ChronoUnit.MINUTES.between(timeIn, timeOut);
//            if (diffMinutes < 0) {
//                diffMinutes += 24 * 60;
//            }
//
//            long diffHours = diffMinutes / 60; // Only calculate hours
//
//            messageTextView.setText(String.format(Locale.getDefault(),
//                    "Please be informed. Check-out time is %d hours after Check-in.",
//                    diffHours));
//        } catch (Exception e) {
//            e.printStackTrace();
//            messageTextView.setText("Invalid time format. Please use 'h:mm AM/PM'.");
//            messageTextView.setTextColor(Color.RED);
//        }
//    }
//
//    private String getFormattedBookingInfo(String dateStr, String timeInStr, String timeOutStr) {
//        try {
//            SimpleDateFormat inputDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.getDefault());
//            Date parsedDate = inputDateFormat.parse(dateStr);
//
//            SimpleDateFormat outputDateFormat = new SimpleDateFormat("MMMM d yyyy", Locale.getDefault());
//            String formattedDate = outputDateFormat.format(parsedDate);
//
//            String formattedTimeIn = formatTimeForReceipt(timeInStr);
//            String formattedTimeOut = formatTimeForReceipt(timeOutStr);
//
//            return "Date: " + formattedDate + " (" + formattedTimeIn + " - " + formattedTimeOut + ")";
//        } catch (ParseException e) {
//            e.printStackTrace();
//            return "";
//        }
//    }
//
//    private String formatTimeForReceipt(String timeStr) {
//        try {
//            SimpleDateFormat inputTimeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
//            Date parsedTime = inputTimeFormat.parse(timeStr);
//            Calendar cal = Calendar.getInstance();
//            cal.setTime(parsedTime);
//            int minutes = cal.get(Calendar.MINUTE);
//            SimpleDateFormat outputTimeFormat;
//            if (minutes == 0) {
//                outputTimeFormat = new SimpleDateFormat("ha", Locale.getDefault());
//            } else {
//                outputTimeFormat = new SimpleDateFormat("h:mma", Locale.getDefault());
//            }
//            return outputTimeFormat.format(parsedTime).toLowerCase();
//        } catch (ParseException e) {
//            e.printStackTrace();
//            return timeStr;
//        }
//    }
//}
//
