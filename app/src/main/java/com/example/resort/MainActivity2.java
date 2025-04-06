package com.example.resort;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.simform.custombottomnavigation.SSCustomBottomNavigation;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class MainActivity2 extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main2);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        SSCustomBottomNavigation bottomNavigation = findViewById(R.id.bottomNavigation);
        // Adding items to the bottom navigation
        bottomNavigation.add(new SSCustomBottomNavigation.Model(1, R.drawable.ic_home_home, "Home"));
        bottomNavigation.add(new SSCustomBottomNavigation.Model(2, R.drawable.ic_accommodation, "Accommodation"));
        //bottomNavigation.add(new SSCustomBottomNavigation.Model(3, R.drawable.ic_plus, "Booking"));
        bottomNavigation.add(new SSCustomBottomNavigation.Model(4, R.drawable.ic_explore, "Explore"));
        bottomNavigation.add(new SSCustomBottomNavigation.Model(5, R.drawable.ic_profile, "Profile"));

        bottomNavigation.show(1, true);
        // Load the default fragment (Home) when the activity starts
        loadFragment(new HomeFragment());

        // Setting up the menu click listener with fragment replacement
        bottomNavigation.setOnClickMenuListener(model -> {
            Fragment selectedFragment = null;
            switch (model.getId()) {
                case 1:
                    selectedFragment = new HomeFragment();
                    break;
                case 2:
                    selectedFragment = new AccommodationFragment();
                    break;
//                    case 3:
//                        selectedFragment = new BookingFragment();
//                        break;
                case 4:
                    selectedFragment = new ExploreFragment();
                    break;
                case 5:
                    selectedFragment = new ProfileFragment();
                    break;
            }
            loadFragment(selectedFragment);
            return null;
        });
    }

    private void loadFragment(Fragment fragment) {
        // Replace the current fragment with the selected one
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.frame, fragment) // Make sure you have a FrameLayout with this ID in your activity_main2 layout
                    .commit();
        }
    }

}
