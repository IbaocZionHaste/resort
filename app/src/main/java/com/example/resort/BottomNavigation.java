package com.example.resort;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class BottomNavigation extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bottom_navigation);

        /// Access the BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);

        /// Disable a specific menu item (e.g., index 2)
        bottomNavigationView.getMenu().getItem(2).setEnabled(false);

        /// Load the default fragment only if savedInstanceState is null
        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_accommodation) {
                selectedFragment = new AccommodationFragment();
            } else if (itemId == R.id.nav_explore) {
                selectedFragment = new ExploreFragment();
            } else if (itemId == R.id.nav_profile) {
                selectedFragment = new ProfileFragment();
            }
            return loadFragment(selectedFragment);
        });


        /// FloatingActionButton to open a new page
        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(v -> {
            // Start a new activity (replace NewPageActivity with your actual activity)
            Intent intent = new Intent(BottomNavigation.this, Booking.class);
            overridePendingTransition(0, 0); // Instant transition (blink effect)
            startActivity(intent);
        });
    }

    boolean loadFragment(Fragment fragment) {
        if (!isNetworkAvailable()) {
            showNoInternetDialog();
            return false;
        }
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frame_layout, fragment)
                .commit();
        return true;
    }


//    boolean loadFragment(Fragment fragment) {
//        if (fragment != null) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.frame_layout, fragment) // Updated to frame_layout
//                    .commit();
//            return true;
//        }
//        return false;
//    }

    ///exit the app no need to logout
    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        /// Inflate the custom layout
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.exit_dialog, null);

        /// Create the AlertDialog using the custom view.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();

        /// Set up button listeners.
        Button btnYes = dialogView.findViewById(R.id.btnYes);
        Button btnNo = dialogView.findViewById(R.id.btnNo);

        btnYes.setOnClickListener(v -> {
            dialog.dismiss();
            finish(); /// Exits the activity.
        });

        btnNo.setOnClickListener(v -> dialog.dismiss());

        /// Show the custom alert dialog.
        dialog.show();
    }

    private void showNoInternetDialog() {
        /// 1) Inflate the XML layout you provided
        View dialogView = getLayoutInflater().inflate(
                R.layout.custom_no_internet,
                null
        );

        /// 2) Build a non-cancelable AlertDialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)  // user MUST tap Retry
                .create();

        /// 3) Wire up the Retry button
        Button btnRetry = dialogView.findViewById(R.id.btnRetry);
        btnRetry.setOnClickListener(v -> {
            dialog.dismiss();
            if (!isNetworkAvailable()) {
                showNoInternetDialog();
            }
        });

        /// 4) Finally, show it
        dialog.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

}


///No Current
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.app.AlertDialog;
//import android.content.Intent;
//import android.graphics.Color;
//import android.graphics.drawable.GradientDrawable;
//import android.os.Bundle;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.content.ContextCompat;
//import androidx.fragment.app.Fragment;
//import com.google.android.material.bottomnavigation.BottomNavigationView;
//import com.google.android.material.floatingactionbutton.FloatingActionButton;
//
//import android.view.Gravity;
//import android.view.LayoutInflater;
//import android.view.MenuItem;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.TextView;
//
//public class BottomNavigation extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        EdgeToEdge.enable(this);
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_bottom_navigation);
//
//        /// Access the BottomNavigationView
//        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigation);
//
//        /// Disable a specific menu item (e.g., index 2)
//        bottomNavigationView.getMenu().getItem(2).setEnabled(false);
//
//        /// Load the default fragment only if savedInstanceState is null
//        if (savedInstanceState == null) {
//            loadFragment(new HomeFragment());
//        }
//
//
//        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
//            Fragment selectedFragment = null;
//            int itemId = item.getItemId();
//            if (itemId == R.id.nav_home) {
//                selectedFragment = new HomeFragment();
//            } else if (itemId == R.id.nav_accommodation) {
//                selectedFragment = new AccommodationFragment();
//            } else if (itemId == R.id.nav_explore) {
//                selectedFragment = new ExploreFragment();
//            } else if (itemId == R.id.nav_profile) {
//                selectedFragment = new ProfileFragment();
//            }
//            return loadFragment(selectedFragment);
//        });
//
//
//        /// FloatingActionButton to open a new page
//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(v -> {
//            // Start a new activity (replace NewPageActivity with your actual activity)
//            Intent intent = new Intent(BottomNavigation.this, Booking.class);
//            overridePendingTransition(0, 0); // Instant transition (blink effect)
//            startActivity(intent);
//        });
//    }
//
//
//    boolean loadFragment(Fragment fragment) {
//        if (fragment != null) {
//            getSupportFragmentManager().beginTransaction()
//                    .replace(R.id.frame_layout, fragment) // Updated to frame_layout
//                    .commit();
//            return true;
//        }
//        return false;
//    }
//
//   ///exit the app no need to logout
//    @SuppressLint("MissingSuperCall")
//    @Override
//    public void onBackPressed() {
//        /// Inflate the custom layout
//        LayoutInflater inflater = getLayoutInflater();
//        View dialogView = inflater.inflate(R.layout.exit_dialog, null);
//
//        /// Create the AlertDialog using the custom view.
//        AlertDialog.Builder builder = new AlertDialog.Builder(this);
//        builder.setView(dialogView);
//        AlertDialog dialog = builder.create();
//
//        /// Set up button listeners.
//        Button btnYes = dialogView.findViewById(R.id.btnYes);
//        Button btnNo = dialogView.findViewById(R.id.btnNo);
//
//        btnYes.setOnClickListener(v -> {
//            dialog.dismiss();
//            finish(); /// Exits the activity.
//        });
//
//        btnNo.setOnClickListener(v -> dialog.dismiss());
//
//        /// Show the custom alert dialog.
//        dialog.show();
//    }
//
//}
