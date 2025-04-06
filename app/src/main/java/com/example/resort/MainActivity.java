//package com.example.resort;
//
//import android.app.Dialog;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.os.Bundle;
//import android.os.Handler;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.viewpager.widget.ViewPager;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//public class MainActivity extends AppCompatActivity {
//
//    private ViewPager viewPager;
//    private Handler handler;
//    private Runnable autoSwipeRunnable;
//    private Dialog loadingDialog;
//    private Dialog noInternetDialog;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        // Fullscreen
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//
//        // Check if there is an Internet connection
//        if (!isNetworkAvailable()) {
//            showNoInternetDialog();
//            return;
//        }
//
//        // Show the custom loading screen (only the progress spinner and message, tinted light blue)
//        showLoading();
//
//        // Check user status BEFORE inflating landing page
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
//            mDatabase.child("users").child(currentUser.getUid()).child("isOnline")
//                    .addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if (loadingDialog != null && loadingDialog.isShowing()) {
//                                loadingDialog.dismiss();
//                            }
//                            Boolean isOnline = snapshot.getValue(Boolean.class);
//                            if (isOnline != null && isOnline) {
//                                // Quick auto-login: redirect immediately without transition animation
//                                Intent intent = new Intent(MainActivity.this, BottomNavigation.class);
//                                startActivity(intent);
//                                overridePendingTransition(0, 0); // blink effect
//                                finish();
//                            } else {
//                                // If not online, initialize the landing page
//                                initLanding();
//                            }
//                        }
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            if (loadingDialog != null && loadingDialog.isShowing()) {
//                                loadingDialog.dismiss();
//                            }
//                            // On error, fall back to the landing page
//                            initLanding();
//                        }
//                    });
//        } else {
//            // No user logged in? Show the landing page.
//            if (loadingDialog != null && loadingDialog.isShowing()) {
//                loadingDialog.dismiss();
//            }
//            initLanding();
//        }
//    }
//
//    // Initialize landing page with ViewPager, dots indicator, auto-swipe, etc.
//    private void initLanding() {
//        setContentView(R.layout.activity_main);
//        EdgeToEdge.enable(this);
//
//        // Apply window insets for an edge-to-edge experience
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Set up ViewPager for landing pages
//        viewPager = findViewById(R.id.viewPager);
//        ViewPagerLanding adapter = new ViewPagerLanding(getSupportFragmentManager());
//        viewPager.setAdapter(adapter);
//
//        // Set up dots indicator for the pages
//        createDotsIndicator(0);
//        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
//            @Override
//            public void onPageSelected(int position) {
//                createDotsIndicator(position);
//                // Stop auto-swipe when we reach the last page (position 3)
//                if (position == 3) {
//                    stopAutoSwipe();
//                }
//            }
//            @Override
//            public void onPageScrollStateChanged(int state) {}
//        });
//
//        // Setup auto-swipe handler and runnable
//        handler = new Handler();
//        autoSwipeRunnable = new Runnable() {
//            @Override
//            public void run() {
//                int currentItem = viewPager.getCurrentItem();
//                if (currentItem < 3) {
//                    viewPager.setCurrentItem(currentItem + 1, true);
//                    handler.postDelayed(this, 2000); // 2-second delay
//                } else {
//                    stopAutoSwipe();
//                }
//            }
//        };
//
//        // Start auto-swipe after 2 seconds
//        handler.postDelayed(autoSwipeRunnable, 2000);
//    }
//
//    // Create a dots indicator for the ViewPager
//    private void createDotsIndicator(int position) {
//        LinearLayout dotsLayout = findViewById(R.id.dotsLayout);
//        dotsLayout.removeAllViews();
//        for (int i = 0; i < 4; i++) {
//            View dot = new View(this);
//            dot.setBackgroundResource(i == position ? R.drawable.active_dot : R.drawable.inactive_dot);
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
//            params.setMargins(4, 0, 4, 0);
//            dotsLayout.addView(dot, params);
//        }
//    }
//
//    // Stop auto-swipe to prevent memory leaks
//    private void stopAutoSwipe() {
//        if (handler != null && autoSwipeRunnable != null) {
//            handler.removeCallbacks(autoSwipeRunnable);
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        stopAutoSwipe();
//    }
//
//    // Check for network connectivity
//    private boolean isNetworkAvailable() {
//        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (connectivityManager != null) {
//            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
//            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
//        }
//        return false;
//    }
//
//    // Show a custom "No Internet" dialog with a Retry button
//    private void showNoInternetDialog() {
//        noInternetDialog = new Dialog(this);
//        noInternetDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
//        noInternetDialog.setContentView(R.layout.custom_no_internet);
//        noInternetDialog.setCancelable(false);
//        if (noInternetDialog.getWindow() != null) {
//            noInternetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        }
//        // Set up the Retry button from the custom layout
//        Button retryButton = noInternetDialog.findViewById(R.id.btnRetry);
//        retryButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                noInternetDialog.dismiss();
//                recreate();
//            }
//        });
//        noInternetDialog.show();
//    }
//
//    // Show a custom loading dialog with only the progress spinner and message (light blue tint)
//    private void showLoading() {
//        loadingDialog = new Dialog(this);
//        loadingDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
//        loadingDialog.setContentView(R.layout.custom_progress_dialog);
//        loadingDialog.setCancelable(false);
//        if (loadingDialog.getWindow() != null) {
//            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        }
//        loadingDialog.show();
//    }
//}
//

/// No swipe automatic
package com.example.resort;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.viewpager.widget.ViewPager;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class MainActivity extends AppCompatActivity {

    private ViewPager viewPager;
    private Dialog loadingDialog;
    private Dialog noInternetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fullscreen
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Check if there is an Internet connection
        if (!isNetworkAvailable()) {
            showNoInternetDialog();
            return;
        }

        // Show the custom loading screen (only the progress spinner and message, tinted light blue)
        showLoading();

        /// Check user status BEFORE inflating landing page
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
            mDatabase.child("users").child(currentUser.getUid())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (loadingDialog != null && loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            // Retrieve both the online status and phone verification status from your database.
                            Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);
                            Boolean phoneVerified = snapshot.child("phoneVerified").getValue(Boolean.class);

                            // If the phone is not verified, block auto-login regardless of online status.
                            if (phoneVerified == null || !phoneVerified) {
                                // User is not verified – no shortcut login.
                                initLanding();
                            } else {
                                // User is verified, now check if they're marked as online.
                                if (isOnline != null && isOnline) {
                                    // Quick auto-login: redirect immediately without transition animation.
                                    Intent intent = new Intent(MainActivity.this, BottomNavigation.class);
                                    startActivity(intent);
                                    overridePendingTransition(0, 0); // blink effect
                                    finish();
                                } else {
                                    // Not online yet; initialize the landing page.
                                    initLanding();
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            if (loadingDialog != null && loadingDialog.isShowing()) {
                                loadingDialog.dismiss();
                            }
                            // On error, fall back to the landing page.
                            initLanding();
                        }
                    });
        } else {
            // No user logged in? Show the landing page.
            if (loadingDialog != null && loadingDialog.isShowing()) {
                loadingDialog.dismiss();
            }
            initLanding();
        }
    }

    // Initialize landing page with ViewPager and dots indicator (auto-swipe removed)
    private void initLanding() {
        setContentView(R.layout.activity_main);
        EdgeToEdge.enable(this);

        // Apply window insets for an edge-to-edge experience
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Set up ViewPager for landing pages
        viewPager = findViewById(R.id.viewPager);
        ViewPagerLanding adapter = new ViewPagerLanding(getSupportFragmentManager());
        viewPager.setAdapter(adapter);

        // Set up dots indicator for the pages
        createDotsIndicator(0);
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) { }
            @Override
            public void onPageSelected(int position) {
                createDotsIndicator(position);
                // Auto-swipe functionality removed – no further actions here.
            }
            @Override
            public void onPageScrollStateChanged(int state) { }
        });
    }

    // Create a dots indicator for the ViewPager
    private void createDotsIndicator(int position) {
        LinearLayout dotsLayout = findViewById(R.id.dotsLayout);
        dotsLayout.removeAllViews();
        for (int i = 0; i < 4; i++) {
            View dot = new View(this);
            dot.setBackgroundResource(i == position ? R.drawable.active_dot : R.drawable.inactive_dot);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
            params.setMargins(4, 0, 4, 0);
            dotsLayout.addView(dot, params);
        }
    }


    // Check for network connectivity
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    // Show a custom "No Internet" dialog with a Retry button
    private void showNoInternetDialog() {
        noInternetDialog = new Dialog(this);
        noInternetDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        noInternetDialog.setContentView(R.layout.custom_no_internet);
        noInternetDialog.setCancelable(false);
        if (noInternetDialog.getWindow() != null) {
            noInternetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        // Set up the Retry button from the custom layout
        Button retryButton = noInternetDialog.findViewById(R.id.btnRetry);
        retryButton.setOnClickListener(v -> {
            noInternetDialog.dismiss();
            recreate();
        });
        noInternetDialog.show();
    }

    // Show a custom loading dialog with only the progress spinner and message (light blue tint)
    private void showLoading() {
        loadingDialog = new Dialog(this);
        loadingDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        loadingDialog.setContentView(R.layout.custom_progress_dialog);
        loadingDialog.setCancelable(false);
        if (loadingDialog.getWindow() != null) {
            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        loadingDialog.show();
    }
}

///Auto swipe this code
//Faster load if the internet strong
//package com.example.resort;
//
//import android.app.Dialog;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.net.ConnectivityManager;
//import android.net.NetworkInfo;
//import android.os.Bundle;
//import android.os.Handler;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.viewpager.widget.ViewPager;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//public class MainActivity extends AppCompatActivity {
//
//    private ViewPager viewPager;
//    private Handler handler;
//    private Runnable autoSwipeRunnable;
//    private Dialog loadingDialog;
//    private Dialog noInternetDialog;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//        // Fullscreen
//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);
//
//        // Check if there is an Internet connection
//        if (!isNetworkAvailable()) {
//            showNoInternetDialog();
//            return;
//        }
//
//        // Show the custom loading screen (only the progress spinner and message, tinted light blue)
//        showLoading();
//
//
//        /// Check user status BEFORE inflating landing page
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
//            mDatabase.child("users").child(currentUser.getUid())
//                    .addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if (loadingDialog != null && loadingDialog.isShowing()) {
//                                loadingDialog.dismiss();
//                            }
//                            // Retrieve both the online status and phone verification status from your database.
//                            Boolean isOnline = snapshot.child("isOnline").getValue(Boolean.class);
//                            Boolean phoneVerified = snapshot.child("phoneVerified").getValue(Boolean.class);
//
//                            // If the phone is not verified, block auto-login regardless of online status.
//                            if (phoneVerified == null || !phoneVerified) {
//                                // User is not verified – no shortcut login.
//                                initLanding();
//                            } else {
//                                // User is verified, now check if they're marked as online.
//                                if (isOnline != null && isOnline) {
//                                    // Quick auto-login: redirect immediately without transition animation.
//                                    Intent intent = new Intent(MainActivity.this, BottomNavigation.class);
//                                    startActivity(intent);
//                                    overridePendingTransition(0, 0); // blink effect
//                                    finish();
//                                } else {
//                                    // Not online yet; initialize the landing page.
//                                    initLanding();
//                                }
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            if (loadingDialog != null && loadingDialog.isShowing()) {
//                                loadingDialog.dismiss();
//                            }
//                            // On error, fall back to the landing page.
//                            initLanding();
//                        }
//                    });
//        } else {
//            // No user logged in? Show the landing page.
//            if (loadingDialog != null && loadingDialog.isShowing()) {
//                loadingDialog.dismiss();
//            }
//            initLanding();
//        }
//    }
//
//
//
//    // Initialize landing page with ViewPager, dots indicator, auto-swipe, etc.
//    private void initLanding() {
//        setContentView(R.layout.activity_main);
//        EdgeToEdge.enable(this);
//
//        // Apply window insets for an edge-to-edge experience
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Set up ViewPager for landing pages
//        viewPager = findViewById(R.id.viewPager);
//        ViewPagerLanding adapter = new ViewPagerLanding(getSupportFragmentManager());
//        viewPager.setAdapter(adapter);
//
//        // Set up dots indicator for the pages
//        createDotsIndicator(0);
//        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
//            @Override
//            public void onPageSelected(int position) {
//                createDotsIndicator(position);
//                // Stop auto-swipe when we reach the last page (position 3)
//                if (position == 3) {
//                    stopAutoSwipe();
//                }
//            }
//            @Override
//            public void onPageScrollStateChanged(int state) {}
//        });
//
//        // Setup auto-swipe handler and runnable
//        handler = new Handler();
//        autoSwipeRunnable = new Runnable() {
//            @Override
//            public void run() {
//                int currentItem = viewPager.getCurrentItem();
//                if (currentItem < 3) {
//                    viewPager.setCurrentItem(currentItem + 1, true);
//                    handler.postDelayed(this, getAutoSwipeDelay());
//                } else {
//                    stopAutoSwipe();
//                }
//            }
//        };
//
//        // Start auto-swipe after delay (faster if on WiFi)
//        handler.postDelayed(autoSwipeRunnable, getAutoSwipeDelay());
//    }
//
//    // Create a dots indicator for the ViewPager
//    private void createDotsIndicator(int position) {
//        LinearLayout dotsLayout = findViewById(R.id.dotsLayout);
//        dotsLayout.removeAllViews();
//        for (int i = 0; i < 4; i++) {
//            View dot = new View(this);
//            dot.setBackgroundResource(i == position ? R.drawable.active_dot : R.drawable.inactive_dot);
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
//            params.setMargins(4, 0, 4, 0);
//            dotsLayout.addView(dot, params);
//        }
//    }
//
//    // Stop auto-swipe to prevent memory leaks
//    private void stopAutoSwipe() {
//        if (handler != null && autoSwipeRunnable != null) {
//            handler.removeCallbacks(autoSwipeRunnable);
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        stopAutoSwipe();
//    }
//
//    // Check for network connectivity
//    private boolean isNetworkAvailable() {
//        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (connectivityManager != null) {
//            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
//            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
//        }
//        return false;
//    }
//
//    // New helper method: Check if the connection is via WiFi (considered "strong")
//    private boolean isWifiConnected() {
//        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
//        if (connectivityManager != null) {
//            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
//            return activeNetworkInfo != null
//                    && activeNetworkInfo.isConnected()
//                    && activeNetworkInfo.getType() == ConnectivityManager.TYPE_WIFI;
//        }
//        return false;
//    }
//
//    // New helper method: Return a shorter delay if WiFi is connected
//    private int getAutoSwipeDelay() {
//        return isWifiConnected() ? 1000 : 2000; // 1 sec delay if WiFi, else 2 sec delay
//    }
//
//    // Show a custom "No Internet" dialog with a Retry button
//    private void showNoInternetDialog() {
//        noInternetDialog = new Dialog(this);
//        noInternetDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
//        noInternetDialog.setContentView(R.layout.custom_no_internet);
//        noInternetDialog.setCancelable(false);
//        if (noInternetDialog.getWindow() != null) {
//            noInternetDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        }
//        // Set up the Retry button from the custom layout
//        Button retryButton = noInternetDialog.findViewById(R.id.btnRetry);
//        retryButton.setOnClickListener(v -> {
//            noInternetDialog.dismiss();
//            recreate();
//        });
//        noInternetDialog.show();
//    }
//
//    // Show a custom loading dialog with only the progress spinner and message (light blue tint)
//    private void showLoading() {
//        loadingDialog = new Dialog(this);
//        loadingDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
//        loadingDialog.setContentView(R.layout.custom_progress_dialog);
//        loadingDialog.setCancelable(false);
//        if (loadingDialog.getWindow() != null) {
//            loadingDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//        }
//        loadingDialog.show();
//    }
//}


///This no function automatic if the user is true no need to login but false need login
//package com.example.resort;
//
//import android.os.Bundle;
//import android.os.Handler;
//import android.os.Message;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.LinearLayout;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.viewpager.widget.ViewPager;
//
//public class MainActivity extends AppCompatActivity {
//
//    private ViewPager viewPager;
//    private ViewPagerLanding adapter;
//
//    private Handler handler;
//    private Runnable autoSwipeRunnable;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_main);
//
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Set up ViewPager
//        viewPager = findViewById(R.id.viewPager);
//        adapter = new ViewPagerLanding(getSupportFragmentManager());
//        viewPager.setAdapter(adapter);
//
//        // Create dots indicator
//        createDotsIndicator(0);
//        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
//            @Override
//            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
//
//            @Override
//            public void onPageSelected(int position) {
//                createDotsIndicator(position);
//                // Stop auto swipe when we reach the 4th page (position 3)
//                if (position == 3) {
//                    stopAutoSwipe();
//                }
//            }
//
//            @Override
//            public void onPageScrollStateChanged(int state) {}
//        });
//
//        // Auto-swipe setup
//        handler = new Handler();
//        autoSwipeRunnable = new Runnable() {
//            @Override
//            public void run() {
//                // Get current position
//                int currentItem = viewPager.getCurrentItem();
//
//                // If we're not on the last page (position 3), move to the next page
//                if (currentItem < 3) {
//                    viewPager.setCurrentItem(currentItem + 1, true);  // Go to next page
//                } else {
//                    // Stop auto swipe after the 3rd page
//                    stopAutoSwipe();
//                    return;  // Do nothing further, auto swipe stops here
//                }
//
//                // Post this runnable to run again after 2 seconds
//                handler.postDelayed(this, 2000);  // 2000 ms = 2 seconds
//            }
//        };
//
//        // Start auto-swiping
//        handler.postDelayed(autoSwipeRunnable, 2000);  // Start after 2 seconds
//    }
//
//    private void createDotsIndicator(int position) {
//        // Clear previous dots
//        LinearLayout dotsLayout = findViewById(R.id.dotsLayout);
//        dotsLayout.removeAllViews();
//
//        // Create dots
//        for (int i = 0; i < 4; i++) {
//            View dot = new View(this);
//            dot.setBackgroundResource(i == position ? R.drawable.active_dot : R.drawable.inactive_dot);
//            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(16, 16);
//            params.setMargins(4, 0, 4, 0);
//            dotsLayout.addView(dot, params);
//        }
//    }
//
//    // Method to stop the auto swipe
//    private void stopAutoSwipe() {
//        if (handler != null && autoSwipeRunnable != null) {
//            handler.removeCallbacks(autoSwipeRunnable);  // Stop the auto swipe
//        }
//    }
//
//    @Override
//    protected void onDestroy() {
//        super.onDestroy();
//        // Stop the auto-swipe runnable when the activity is destroyed to prevent memory leaks
//        stopAutoSwipe();
//    }
//}
