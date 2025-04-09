//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.graphics.drawable.Drawable;
//import android.os.Bundle;
//import android.util.DisplayMetrics;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.home.data.DatabaseHelper;
//import com.example.resort.home.data.Promotion;
//import com.example.resort.home.data.PromotionsAdapter;
//
//import java.util.List;
//
//public class HomeFragment extends Fragment {
//
//    private RecyclerView recyclerView;
//    private PromotionsAdapter adapter;
//    private DatabaseHelper databaseHelper;
//    private LinearLayout selectedLayout;
//    private ProgressBar progressBar;  // Declare the ProgressBar
//
//    @SuppressLint("ClickableViewAccessibility")
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.home_fragment, container, false);
//
//        // ---------------------------
//        // Adapt screen size dynamically
//        // ---------------------------
//        DisplayMetrics displayMetrics = new DisplayMetrics();
//        getActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        int screenWidth = displayMetrics.widthPixels;
//        int screenHeight = displayMetrics.heightPixels;
//        // Example: Adjust RecyclerView height to half of the screen height
//        recyclerView = view.findViewById(R.id.recycleView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//        ViewGroup.LayoutParams recyclerParams = recyclerView.getLayoutParams();
//        recyclerParams.height = screenHeight / 2; // Adapt height based on screen size
//        recyclerView.setLayoutParams(recyclerParams);
//        // ---------------------------
//
//        // Initialize ProgressBar
//        progressBar = view.findViewById(R.id.progressBar);
//
//        // Fetch promotions from the database
//        databaseHelper = new DatabaseHelper();
//        databaseHelper.fetchPromotions(new DatabaseHelper.DataStatus() {
//            @Override
//            public void DataLoaded(List<Promotion> promotions) {
//                // Hide the ProgressBar once data is loaded
//                progressBar.setVisibility(View.GONE);
//                // Set the adapter for the RecyclerView
//                adapter = new PromotionsAdapter(getContext(), promotions);
//                recyclerView.setAdapter(adapter);
//            }
//
//            @Override
//            public void Error(String message) {
//                // Hide the ProgressBar in case of error
//                progressBar.setVisibility(View.GONE);
//                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // Show ProgressBar while data is loading
//        progressBar.setVisibility(View.VISIBLE);
//
//        // Notification button click listener
//        ImageView next = view.findViewById(R.id.notification);
//        next.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Notification.class);
//            startActivity(intent);
//        });
//
//        // Initialize search EditText and detect drawable clicks (search icon)
//        EditText searchBar = view.findViewById(R.id.editTextText3);
//        searchBar.setOnTouchListener((v, event) -> {
//            if (event.getAction() == MotionEvent.ACTION_UP) {
//                Drawable drawableRight = searchBar.getCompoundDrawables()[2]; // Right drawable
//                if (drawableRight != null) {
//                    int drawableWidth = drawableRight.getBounds().width();
//                    int touchAreaStart = searchBar.getRight() - drawableWidth - searchBar.getPaddingRight();
//                    if (event.getRawX() >= touchAreaStart) {
//                        Toast.makeText(getContext(), "Search icon clicked!", Toast.LENGTH_SHORT).show();
//                        return true; // Consume the event
//                    }
//                }
//            }
//            return false;
//        });
//
//        // Initialize LinearLayouts for each item
//        LinearLayout allColorLayout = view.findViewById(R.id.accommodation);
//        LinearLayout cottageLayout = view.findViewById(R.id.progress);
//        LinearLayout boatLayout = view.findViewById(R.id.location);
//        LinearLayout foodLayout = view.findViewById(R.id.food1);
//        LinearLayout beverageLayout = view.findViewById(R.id.beverage1);
//
//        // Set default selection
//        selectedLayout = allColorLayout;
//
//        // Set click listeners to change the selection
//        allColorLayout.setOnClickListener(v -> setSelectedLayout(allColorLayout));
//        cottageLayout.setOnClickListener(v -> setSelectedLayout(cottageLayout));
//        boatLayout.setOnClickListener(v -> setSelectedLayout(boatLayout));
//        foodLayout.setOnClickListener(v -> setSelectedLayout(foodLayout));
//        beverageLayout.setOnClickListener(v -> setSelectedLayout(beverageLayout));
//
//        return view;
//    }
//
//    private void setSelectedLayout(LinearLayout layout) {
//        if (selectedLayout != null) {
//            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
//        }
//        layout.setBackgroundResource(R.drawable.selected_background);
//        selectedLayout = layout;
//    }
//}

package com.example.resort;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.home.data.DatabaseHelper;
import com.example.resort.home.data.Promotion;
import com.example.resort.home.data.PromotionsAdapter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;

import java.util.List;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private PromotionsAdapter adapter;
    private LinearLayout selectedLayout;
    private ProgressBar progressBar;
    private ImageView profileImage;
    private TextView usernameTextView;
    private DatabaseReference userRef;
    // Declare the badge TextView as a member variable for notification receive
    private TextView badgeCount;


    //Shortcut Button
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout accommodationShortcut = view.findViewById(R.id.accommodation); // ID ng LinearLayout
        LinearLayout exploreShortcut = view.findViewById(R.id.location);

        accommodationShortcut.setOnClickListener(v -> {
            if (getActivity() instanceof BottomNavigation) {
                BottomNavigation bottomNav = (BottomNavigation) getActivity();

                // Change ang fragment
                bottomNav.loadFragment(new AccommodationFragment());

                // I-update ang Bottom Navigation para same pick
                BottomNavigationView bottomNavigationView = bottomNav.findViewById(R.id.bottomNavigation);
                bottomNavigationView.setSelectedItemId(R.id.nav_accommodation);
            }
        });

        exploreShortcut.setOnClickListener(v -> {
            if (getActivity() instanceof BottomNavigation) {
                BottomNavigation bottomNav = (BottomNavigation) getActivity();

                // Change ang fragment
                bottomNav.loadFragment(new ExploreFragment());

                // I-update ang Bottom Navigation para same pick
                BottomNavigationView bottomNavigationView = bottomNav.findViewById(R.id.bottomNavigation);
                bottomNavigationView.setSelectedItemId(R.id.nav_explore);
            }
        });
//
//        LinearLayout foodShortcut = view.findViewById(R.id.food1);
//        foodShortcut.setOnClickListener(v -> {
//            if (getActivity() instanceof BottomNavigation) {
//                BottomNavigation bottomNav = (BottomNavigation) getActivity();
//
//                // Change ang fragment
//                bottomNav.loadFragment(new AccommodationFragment());
//
//                // I-update ang Bottom Navigation para same pick
//                BottomNavigationView bottomNavigationView = bottomNav.findViewById(R.id.bottomNavigation);
//                bottomNavigationView.setSelectedItemId(R.id.nav_accommodation);
//            }
//        });
//
//        LinearLayout beverageShortcut = view.findViewById(R.id.beverage1);
//        beverageShortcut.setOnClickListener(v -> {
//            if (getActivity() instanceof BottomNavigation) {
//                BottomNavigation bottomNav = (BottomNavigation) getActivity();
//
//                // Change ang fragment
//                bottomNav.loadFragment(new AccommodationFragment());
//
//                // I-update ang Bottom Navigation para same pick
//                BottomNavigationView bottomNavigationView = bottomNav.findViewById(R.id.bottomNavigation);
//                bottomNavigationView.setSelectedItemId(R.id.nav_accommodation);
//            }
//        });

        // Booking Status Shortcut
        LinearLayout progress = view.findViewById(R.id.progress);
        progress.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BookingStatus.class);
            startActivity(intent);
        });
        LinearLayout review = view.findViewById(R.id.review);
        review.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Feedback.class);
            startActivity(intent);
        });
        LinearLayout about = view.findViewById(R.id.about);
        about.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AboutUs.class);
            startActivity(intent);
        });

        LinearLayout history = view.findViewById(R.id.history);
        history.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), BookingHistory.class);
            startActivity(intent);
        });
    }
    //Shortcut Button End


    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.home_fragment, container, false);


        // Initialize your badge TextView and start listening for notifications from Firebase.
        badgeCount = view.findViewById(R.id.badge_count);
        listenForFirebaseNotifications();

        // Initialize Firebase and check for logged-in user
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            // No user is logged in, redirect to login screen
            Intent intent = new Intent(getActivity(), Login.class);
            startActivity(intent);
            requireActivity().finish();
            return view;
        }
        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());

        // Notification button click listener
        ImageView notificationBtn = view.findViewById(R.id.notification);
        notificationBtn.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), Notification.class);
            startActivity(intent);
            // Mark all notifications as read.
            markAllNotificationsAsRead();
        });

        // Initialize views
        profileImage = view.findViewById(R.id.imageView2); // Make sure this ID exists in your XML
        usernameTextView = view.findViewById(R.id.textView15); // Make sure this ID exists in your XML
        loadProfileData(); // Load profile data (username and profile image)

        // Adapt screen size dynamically for RecyclerView
        //DisplayMetrics displayMetrics = new DisplayMetrics();
        //requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //int screenHeight = displayMetrics.heightPixels;
        recyclerView = view.findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        //ViewGroup.LayoutParams params = recyclerView.getLayoutParams();
        // params.height = screenHeight / 2;
        //recyclerView.setLayoutParams(params);

        // Optimize RecyclerView for better performance
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        //noinspection deprecation
        recyclerView.setDrawingCacheEnabled(true);
        //noinspection deprecation
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        // Enable smooth scrolling
        recyclerView.setNestedScrollingEnabled(false);

        // Initialize ProgressBar
        progressBar = view.findViewById(R.id.progressBar);

        // Fetch promotions from the database
        DatabaseHelper databaseHelper = new DatabaseHelper();
        databaseHelper.fetchPromotions(new DatabaseHelper.DataStatus() {
            @Override
            public void DataLoaded(List<Promotion> promotions) {
                progressBar.setVisibility(View.GONE);
                adapter = new PromotionsAdapter(getContext(), promotions);
                recyclerView.setAdapter(adapter);
            }

            @Override
            public void Error(String message) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
            }
        });
        progressBar.setVisibility(View.VISIBLE);


        ///Search Bar
        final View rootView = view.findViewById(android.R.id.content);
        final EditText searchBar = view.findViewById(R.id.editTextText3);
        final Handler searchHandler = new Handler();
        final Runnable filterRunnable = new Runnable() {
            @Override
            public void run() {
                // Get current query (if empty, adapter should reload the full list)
                String query = searchBar.getText().toString();
                if (adapter != null) {
                    adapter.getFilter().filter(query);
                }
            }
        };

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed here.
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Remove any pending filter action to debounce the input
                searchHandler.removeCallbacks(filterRunnable);
            }

            @Override
            public void afterTextChanged(Editable s) {
                // Post a delayed runnable (e.g., 300ms) to perform the search
                searchHandler.postDelayed(filterRunnable, 300);
            }
        });

        // Add a touch listener on the root view to detect clicks outside the search bar
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && searchBar.isFocused()) {
                // Get searchBar's screen coordinates
                int[] location = new int[2];
                searchBar.getLocationOnScreen(location);
                int left = location[0];
                int top = location[1];
                int right = left + searchBar.getWidth();
                int bottom = top + searchBar.getHeight();
                float x = event.getRawX();
                float y = event.getRawY();

                // If the touch event is outside the searchBar, clear focus and reset search
                if (x < left || x > right || y < top || y > bottom) {
                    searchBar.clearFocus();
                    searchBar.setText("");
                    if (adapter != null) {
                        adapter.getFilter().filter(""); // Reset filter to show full list
                    }
                    // Optionally hide the keyboard:
                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
                    }
                }
            }
            return false;
        });

        searchBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    searchBar.setText("");
                }
            }
        });

        ///Search Bar



        LinearLayout allColorLayout = view.findViewById(R.id.accommodation);
        LinearLayout cottageLayout = view.findViewById(R.id.progress);
        LinearLayout boatLayout = view.findViewById(R.id.location);
        LinearLayout reviewLayout = view.findViewById(R.id.review);
        LinearLayout aboutLayout = view.findViewById(R.id.about);

        /// Set default selection and listeners
        selectedLayout = allColorLayout;
        allColorLayout.setOnClickListener(v -> setSelectedLayout(allColorLayout));
        cottageLayout.setOnClickListener(v -> setSelectedLayout(cottageLayout));
        boatLayout.setOnClickListener(v -> setSelectedLayout(boatLayout));
        reviewLayout.setOnClickListener(v -> setSelectedLayout(reviewLayout));
        aboutLayout.setOnClickListener(v -> setSelectedLayout(aboutLayout));


        return view;
    }

    private void setSelectedLayout(LinearLayout layout) {
        if (selectedLayout != null) {
            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
        }
        layout.setBackgroundResource(R.drawable.selected_background);
        selectedLayout = layout;
    }


    // Load profile data including username and profile image from Firebase Storage
    private void loadProfileData() {
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    // Retrieve and display profile image using Picasso
                    String imageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                    if (imageUrl != null && !imageUrl.equals("default")) {
                        Picasso.get()
                                .load(imageUrl)
                                .error(R.drawable.profile)
                                .into(profileImage);
                    } else {
                        profileImage.setImageResource(R.drawable.profile);
                    }

                    // Optional: Retrieve and display username if available
                    String username = dataSnapshot.child("username").getValue(String.class);
                    if (username != null) {
                        usernameTextView.setText(username);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
            }
        });


    }

    /**
     * Listen for new messages from booking status in Firebase.
     * Assumes each booking snapshot may have a "newMessage" field.
     */
    private void listenForFirebaseNotifications() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("notifications");

        notificationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int notificationCount = 0;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Boolean read = child.child("read").getValue(Boolean.class);
                    if (read == null || !read) {
                        notificationCount++;
                    }
                }
                updateBadgeCount(notificationCount);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void updateBadgeCount(int count) {
        if (count > 0) {
            badgeCount.setText(String.valueOf(count));
            badgeCount.setVisibility(View.VISIBLE);
        } else {
            badgeCount.setVisibility(View.GONE);
        }
    }

    private void markAllNotificationsAsRead() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) return;
        String userId = currentUser.getUid();
        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("notifications");
        notificationRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    child.getRef().child("read").setValue(true);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

}




//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.graphics.drawable.Drawable;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.home.data.DatabaseHelper;
//import com.example.resort.home.data.Promotion;
//import com.example.resort.home.data.PromotionsAdapter;
//
//import java.util.List;
//
//public class HomeFragment extends Fragment {
//
//    private RecyclerView recyclerView;
//    private PromotionsAdapter adapter;
//    private DatabaseHelper databaseHelper;
//    private LinearLayout selectedLayout;
//    private ProgressBar progressBar;  // Declare the ProgressBar
//
//    @SuppressLint("ClickableViewAccessibility")
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.home_fragment, container, false);
//
//        // Initialize ProgressBar
//        progressBar = view.findViewById(R.id.progressBar);
//
//        // Initialize RecyclerView
//        recyclerView = view.findViewById(R.id.recycleView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//
//        // Fetch promotions
//        databaseHelper = new DatabaseHelper();
//        databaseHelper.fetchPromotions(new DatabaseHelper.DataStatus() {
//            @Override
//            public void DataLoaded(List<Promotion> promotions) {
//                // Hide the ProgressBar once data is loaded
//                progressBar.setVisibility(View.GONE);
//
//                // Set the adapter for the RecyclerView
//                adapter = new PromotionsAdapter(getContext(), promotions);
//                recyclerView.setAdapter(adapter);
//            }
//
//            @Override
//            public void Error(String message) {
//                // Hide the ProgressBar in case of error as well
//                progressBar.setVisibility(View.GONE);
//                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // Show ProgressBar while data is loading
//        progressBar.setVisibility(View.VISIBLE);
//
//        // Notification button
//        ImageView next = view.findViewById(R.id.notification);
//        next.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Notification.class);
//            startActivity(intent);
//        });
//
//        // Initialize search EditText
//        EditText searchBar = view.findViewById(R.id.editTextText3);
//
//        // Detect clicks on the right drawable (search icon)
//        searchBar.setOnTouchListener((v, event) -> {
//            if (event.getAction() == MotionEvent.ACTION_UP) {
//                Drawable drawableRight = searchBar.getCompoundDrawables()[2]; // Get right drawable
//                if (drawableRight != null) {
//                    int drawableWidth = drawableRight.getBounds().width();
//                    int touchAreaStart = searchBar.getRight() - drawableWidth - searchBar.getPaddingRight();
//
//                    if (event.getRawX() >= touchAreaStart) {
//                        Toast.makeText(getContext(), "Search icon clicked!", Toast.LENGTH_SHORT).show();
//                        return true; // Consume the event
//                    }
//                }
//            }
//            return false; // Let the event continue
//        });
//
//        // Initialize LinearLayouts for each item
//        LinearLayout allColorLayout = view.findViewById(R.id.accommodation);
//        LinearLayout cottageLayout = view.findViewById(R.id.progress);
//        LinearLayout boatLayout = view.findViewById(R.id.location);
//        LinearLayout foodLayout = view.findViewById(R.id.food1);
//        LinearLayout beverageLayout = view.findViewById(R.id.beverage1);
//
//        // Set default selection
//        selectedLayout = allColorLayout;
//
//        // Set click listeners
//        allColorLayout.setOnClickListener(v -> setSelectedLayout(allColorLayout));
//        cottageLayout.setOnClickListener(v -> setSelectedLayout(cottageLayout));
//        boatLayout.setOnClickListener(v -> setSelectedLayout(boatLayout));
//        foodLayout.setOnClickListener(v -> setSelectedLayout(foodLayout));
//        beverageLayout.setOnClickListener(v -> setSelectedLayout(beverageLayout));
//
//        return view;
//    }
//
//    private void setSelectedLayout(LinearLayout layout) {
//        if (selectedLayout != null) {
//            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
//        }
//        layout.setBackgroundResource(R.drawable.selected_background);
//        selectedLayout = layout;
//    }
//}

//NO PROGRESS BAR
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.graphics.drawable.Drawable;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.home.data.DatabaseHelper;
//import com.example.resort.home.data.Promotion;
//import com.example.resort.home.data.PromotionsAdapter;
//
//import java.util.List;
//
//public class HomeFragment extends Fragment {
//
//    private RecyclerView recyclerView;
//    private PromotionsAdapter adapter;
//    private DatabaseHelper databaseHelper;
//    private LinearLayout selectedLayout;
//
//    @SuppressLint("ClickableViewAccessibility")
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.home_fragment, container, false);
//
//        // Initialize RecyclerView
//        recyclerView = view.findViewById(R.id.recycleView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//
//        // Fetch promotions
//        databaseHelper = new DatabaseHelper();
//        databaseHelper.fetchPromotions(new DatabaseHelper.DataStatus() {
//            @Override
//            public void DataLoaded(List<Promotion> promotions) {
//                adapter = new PromotionsAdapter(getContext(), promotions);
//                recyclerView.setAdapter(adapter);
//            }
//
//            @Override
//            public void Error(String message) {
//                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // Notification button
//        ImageView next = view.findViewById(R.id.notification);
//        next.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Notification.class);
//            startActivity(intent);
//        });
//
//        // Initialize search EditText
//        EditText searchBar = view.findViewById(R.id.editTextText3);
//
//        // Detect clicks on the right drawable (search icon)
//        searchBar.setOnTouchListener((v, event) -> {
//            if (event.getAction() == MotionEvent.ACTION_UP) {
//                Drawable drawableRight = searchBar.getCompoundDrawables()[2]; // Get right drawable
//                if (drawableRight != null) {
//                    int drawableWidth = drawableRight.getBounds().width();
//                    int touchAreaStart = searchBar.getRight() - drawableWidth - searchBar.getPaddingRight();
//
//                    if (event.getRawX() >= touchAreaStart) {
//                        Toast.makeText(getContext(), "Search icon clicked!", Toast.LENGTH_SHORT).show();
//                        return true; // Consume the event
//                    }
//                }
//            }
//            return false; // Let the event continue
//        });
//
//        // Initialize LinearLayouts for each item
//        LinearLayout allColorLayout = view.findViewById(R.id.accommodation);
//        LinearLayout cottageLayout = view.findViewById(R.id.progress);
//        LinearLayout boatLayout = view.findViewById(R.id.location);
//        LinearLayout foodLayout = view.findViewById(R.id.food1);
//        LinearLayout beverageLayout = view.findViewById(R.id.beverage1);
//
//        // Set default selection
//        selectedLayout = allColorLayout;
//
//        // Set click listeners
//        allColorLayout.setOnClickListener(v -> setSelectedLayout(allColorLayout));
//        cottageLayout.setOnClickListener(v -> setSelectedLayout(cottageLayout));
//        boatLayout.setOnClickListener(v -> setSelectedLayout(boatLayout));
//        foodLayout.setOnClickListener(v -> setSelectedLayout(foodLayout));
//        beverageLayout.setOnClickListener(v -> setSelectedLayout(beverageLayout));
//
//        return view;
//    }
//
//    private void setSelectedLayout(LinearLayout layout) {
//        if (selectedLayout != null) {
//            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
//        }
//        layout.setBackgroundResource(R.drawable.selected_background);
//        selectedLayout = layout;
//    }
//}


//NO COTTAGE FILTER
//package com.example.resort;
//
//// HomeFragment.java
//import android.content.Intent;
//import android.media.Image;
//import android.os.Bundle;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//
//public class HomeFragment extends Fragment {
//
//    private LinearLayout selectedLayout;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.home_fragment, container, false);
//
//        // Correctly get the ImageView from the inflated view
//        ImageView next = view.findViewById(R.id.notification); // Ensure this ID matches your XML
//
//        next.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Start Booking2 activity using the fragment's activity context
//                Intent intent = new Intent(getActivity(), Notification.class);
//                startActivity(intent);
//
//            }
//        });
//
//
//        // Initialize LinearLayouts for each item
//        LinearLayout allColorLayout = view.findViewById(R.id.accommodation);
//        LinearLayout cottageLayout = view.findViewById(R.id.progress);
//        LinearLayout boatLayout = view.findViewById(R.id.location);
//        LinearLayout foodLayout = view.findViewById(R.id.food1);
//        LinearLayout beverageLayout = view.findViewById(R.id.beverage1);
//
//        // Set default selection if needed
//        selectedLayout = allColorLayout;
//
//        // Set click listeners
//        allColorLayout.setOnClickListener(v -> setSelectedLayout(allColorLayout));
//        cottageLayout.setOnClickListener(v -> setSelectedLayout(cottageLayout));
//        boatLayout.setOnClickListener(v -> setSelectedLayout(boatLayout));
//        foodLayout.setOnClickListener(v -> setSelectedLayout(foodLayout));
//        beverageLayout.setOnClickListener(v -> setSelectedLayout(beverageLayout));
//
//        return view;
//    }
//
//    private void setSelectedLayout(LinearLayout layout) {
//        // Reset the background of the previously selected layout
//        if (selectedLayout != null) {
//            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
//        }
//
//        // Set the new selected layout background color
//        layout.setBackgroundResource(R.drawable.selected_background);
//        selectedLayout = layout; // Update selected layout
//    }
//
//
//}
