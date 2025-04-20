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
    /// Declare the badge TextView as a member variable for notification receive
    private TextView badgeCount;


    //Shortcut Button
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        LinearLayout accommodationShortcut = view.findViewById(R.id.accommodation);
        LinearLayout exploreShortcut = view.findViewById(R.id.location);

        accommodationShortcut.setOnClickListener(v -> {
            if (getActivity() instanceof BottomNavigation) {
                BottomNavigation bottomNav = (BottomNavigation) getActivity();

                /// Change ang fragment
                bottomNav.loadFragment(new AccommodationFragment());

                /// I-update ang Bottom Navigation para same pick
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
        profileImage = view.findViewById(R.id.imageView2); /// Make sure this ID exists in your XML
        usernameTextView = view.findViewById(R.id.textView15); /// Make sure this ID exists in your XML
        loadProfileData(); /// Load profile data (username and profile image)

        recyclerView = view.findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        /// Optimize RecyclerView for better performance
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        ///noinspection deprecation
        recyclerView.setDrawingCacheEnabled(true);
        ///noinspection deprecation
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        /// Enable smooth scrolling
        recyclerView.setNestedScrollingEnabled(false);

        /// Initialize ProgressBar
        progressBar = view.findViewById(R.id.progressBar);

        /// Fetch promotions from the database
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
                /// Get current query (if empty, adapter should reload the full list)
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

        /// Add a touch listener on the root view to detect clicks outside the search bar
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN && searchBar.isFocused()) {
                /// Get searchBar's screen coordinates
                int[] location = new int[2];
                searchBar.getLocationOnScreen(location);
                int left = location[0];
                int top = location[1];
                int right = left + searchBar.getWidth();
                int bottom = top + searchBar.getHeight();
                float x = event.getRawX();
                float y = event.getRawY();

                /// If the touch event is outside the searchBar, clear focus and reset search
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


        ///This not use the color
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

    /// Load profile data including username , number and profile image from Firebase Storage
    private String cachedImageUrl = null;
    private String cachedUsername = null;
    private ValueEventListener profileListener;

    private void loadProfileData() {
        // Attach persistent listener only once
        if (profileListener == null) {
            profileListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        // Retrieve new data from Firebase
                        String newImageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
                        String newUsername = dataSnapshot.child("username").getValue(String.class);

                        // Update profile image if the URL has changed
                        if (!Objects.equals(newImageUrl, cachedImageUrl)) {
                            cachedImageUrl = newImageUrl;
                            if (newImageUrl != null && !newImageUrl.equals("default") && !newImageUrl.isEmpty()) {
                                Picasso.get()
                                        .load(newImageUrl)
                                        .placeholder(R.drawable.profile)
                                        .into(profileImage);
                            } else {
                                profileImage.setImageResource(R.drawable.profile);
                            }
                        }

                        // Update username if it has changed
                        if (!Objects.equals(newUsername, cachedUsername)) {
                            cachedUsername = newUsername;
                            if (newUsername != null) {
                                usernameTextView.setText(newUsername);
                            }
                        }

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Use getActivity() if in a Fragment; otherwise use 'this' or the Activity context.
                    Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
                }
            };

            // Attach the listener persistently so it monitors for changes.
            userRef.addValueEventListener(profileListener);
        }
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

///No Current User
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Rect;
//import android.graphics.drawable.Drawable;
//import android.os.Bundle;
//import android.os.Handler;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.util.DisplayMetrics;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.ViewTreeObserver;
//import android.view.inputmethod.InputMethodManager;
//import android.widget.EditText;
//import android.widget.ImageView;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.home.data.DatabaseHelper;
//import com.example.resort.home.data.Promotion;
//import com.example.resort.home.data.PromotionsAdapter;
//import com.google.android.material.bottomnavigation.BottomNavigationView;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//import com.google.firebase.storage.FirebaseStorage;
//import com.google.firebase.storage.StorageReference;
//import com.squareup.picasso.Picasso;
//
//import java.util.List;
//import java.util.Objects;
//
//public class HomeFragment extends Fragment {
//
//    private RecyclerView recyclerView;
//    private PromotionsAdapter adapter;
//    private LinearLayout selectedLayout;
//    private ProgressBar progressBar;
//    private ImageView profileImage;
//    private TextView usernameTextView;
//    private DatabaseReference userRef;
//    /// Declare the badge TextView as a member variable for notification receive
//    private TextView badgeCount;
//
//
//    //Shortcut Button
//    @Override
//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        LinearLayout accommodationShortcut = view.findViewById(R.id.accommodation);
//        LinearLayout exploreShortcut = view.findViewById(R.id.location);
//
//        accommodationShortcut.setOnClickListener(v -> {
//            if (getActivity() instanceof BottomNavigation) {
//                BottomNavigation bottomNav = (BottomNavigation) getActivity();
//
//                /// Change ang fragment
//                bottomNav.loadFragment(new AccommodationFragment());
//
//                /// I-update ang Bottom Navigation para same pick
//                BottomNavigationView bottomNavigationView = bottomNav.findViewById(R.id.bottomNavigation);
//                bottomNavigationView.setSelectedItemId(R.id.nav_accommodation);
//            }
//        });
//
//        exploreShortcut.setOnClickListener(v -> {
//            if (getActivity() instanceof BottomNavigation) {
//                BottomNavigation bottomNav = (BottomNavigation) getActivity();
//
//                // Change ang fragment
//                bottomNav.loadFragment(new ExploreFragment());
//
//                // I-update ang Bottom Navigation para same pick
//                BottomNavigationView bottomNavigationView = bottomNav.findViewById(R.id.bottomNavigation);
//                bottomNavigationView.setSelectedItemId(R.id.nav_explore);
//            }
//        });
//
//        // Booking Status Shortcut
//        LinearLayout progress = view.findViewById(R.id.progress);
//        progress.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), BookingStatus.class);
//            startActivity(intent);
//        });
//        LinearLayout review = view.findViewById(R.id.review);
//        review.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Feedback.class);
//            startActivity(intent);
//        });
//        LinearLayout about = view.findViewById(R.id.about);
//        about.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), AboutUs.class);
//            startActivity(intent);
//        });
//
//        LinearLayout history = view.findViewById(R.id.history);
//        history.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), BookingHistory.class);
//            startActivity(intent);
//        });
//    }
//    //Shortcut Button End
//
//
//    @SuppressLint("ClickableViewAccessibility")
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.home_fragment, container, false);
//
//
//        // Initialize your badge TextView and start listening for notifications from Firebase.
//        badgeCount = view.findViewById(R.id.badge_count);
//        listenForFirebaseNotifications();
//
//        // Initialize Firebase and check for logged-in user
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            // No user is logged in, redirect to login screen
//            Intent intent = new Intent(getActivity(), Login.class);
//            startActivity(intent);
//            requireActivity().finish();
//            return view;
//        }
//        userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
//
//        // Notification button click listener
//        ImageView notificationBtn = view.findViewById(R.id.notification);
//        notificationBtn.setOnClickListener(v -> {
//            Intent intent = new Intent(getActivity(), Notification.class);
//            startActivity(intent);
//            // Mark all notifications as read.
//            markAllNotificationsAsRead();
//        });
//
//        // Initialize views
//        profileImage = view.findViewById(R.id.imageView2); /// Make sure this ID exists in your XML
//        usernameTextView = view.findViewById(R.id.textView15); /// Make sure this ID exists in your XML
//        loadProfileData(); /// Load profile data (username and profile image)
//
//        recyclerView = view.findViewById(R.id.recycleView);
//        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
//
//        /// Optimize RecyclerView for better performance
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setItemViewCacheSize(20);
//        ///noinspection deprecation
//        recyclerView.setDrawingCacheEnabled(true);
//        ///noinspection deprecation
//        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
//        /// Enable smooth scrolling
//        recyclerView.setNestedScrollingEnabled(false);
//
//        /// Initialize ProgressBar
//        progressBar = view.findViewById(R.id.progressBar);
//
//        // Fetch promotions from the database
//        DatabaseHelper databaseHelper = new DatabaseHelper();
//        databaseHelper.fetchPromotions(new DatabaseHelper.DataStatus() {
//            @Override
//            public void DataLoaded(List<Promotion> promotions) {
//                progressBar.setVisibility(View.GONE);
//                adapter = new PromotionsAdapter(getContext(), promotions);
//                recyclerView.setAdapter(adapter);
//            }
//
//            @Override
//            public void Error(String message) {
//                progressBar.setVisibility(View.GONE);
//                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_SHORT).show();
//            }
//        });
//        progressBar.setVisibility(View.VISIBLE);
//
//
//        ///Search Bar
//        final View rootView = view.findViewById(android.R.id.content);
//        final EditText searchBar = view.findViewById(R.id.editTextText3);
//        final Handler searchHandler = new Handler();
//        final Runnable filterRunnable = new Runnable() {
//            @Override
//            public void run() {
//                /// Get current query (if empty, adapter should reload the full list)
//                String query = searchBar.getText().toString();
//                if (adapter != null) {
//                    adapter.getFilter().filter(query);
//                }
//            }
//        };
//
//        searchBar.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                // No action needed here.
//            }
//
//            @Override
//            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                // Remove any pending filter action to debounce the input
//                searchHandler.removeCallbacks(filterRunnable);
//            }
//
//            @Override
//            public void afterTextChanged(Editable s) {
//                // Post a delayed runnable (e.g., 300ms) to perform the search
//                searchHandler.postDelayed(filterRunnable, 300);
//            }
//        });
//
//        /// Add a touch listener on the root view to detect clicks outside the search bar
//        view.setOnTouchListener((v, event) -> {
//            if (event.getAction() == MotionEvent.ACTION_DOWN && searchBar.isFocused()) {
//                /// Get searchBar's screen coordinates
//                int[] location = new int[2];
//                searchBar.getLocationOnScreen(location);
//                int left = location[0];
//                int top = location[1];
//                int right = left + searchBar.getWidth();
//                int bottom = top + searchBar.getHeight();
//                float x = event.getRawX();
//                float y = event.getRawY();
//
//                /// If the touch event is outside the searchBar, clear focus and reset search
//                if (x < left || x > right || y < top || y > bottom) {
//                    searchBar.clearFocus();
//                    searchBar.setText("");
//                    if (adapter != null) {
//                        adapter.getFilter().filter(""); // Reset filter to show full list
//                    }
//                    // Optionally hide the keyboard:
//                    InputMethodManager imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//                    if (imm != null) {
//                        imm.hideSoftInputFromWindow(searchBar.getWindowToken(), 0);
//                    }
//                }
//            }
//            return false;
//        });
//
//        searchBar.setOnFocusChangeListener(new View.OnFocusChangeListener() {
//            @Override
//            public void onFocusChange(View v, boolean hasFocus) {
//                if (hasFocus) {
//                    searchBar.setText("");
//                }
//            }
//        });
//        ///Search Bar
//
//
//
//        LinearLayout allColorLayout = view.findViewById(R.id.accommodation);
//        LinearLayout cottageLayout = view.findViewById(R.id.progress);
//        LinearLayout boatLayout = view.findViewById(R.id.location);
//        LinearLayout reviewLayout = view.findViewById(R.id.review);
//        LinearLayout aboutLayout = view.findViewById(R.id.about);
//
//        /// Set default selection and listeners
//        selectedLayout = allColorLayout;
//        allColorLayout.setOnClickListener(v -> setSelectedLayout(allColorLayout));
//        cottageLayout.setOnClickListener(v -> setSelectedLayout(cottageLayout));
//        boatLayout.setOnClickListener(v -> setSelectedLayout(boatLayout));
//        reviewLayout.setOnClickListener(v -> setSelectedLayout(reviewLayout));
//        aboutLayout.setOnClickListener(v -> setSelectedLayout(aboutLayout));
//
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
//
//    /// Load profile data including username , number and profile image from Firebase Storage
//    private String cachedImageUrl = null;
//    private String cachedUsername = null;
//    private ValueEventListener profileListener;
//
//    private void loadProfileData() {
//        // Attach persistent listener only once
//        if (profileListener == null) {
//            profileListener = new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                    if (dataSnapshot.exists()) {
//                        // Retrieve new data from Firebase
//                        String newImageUrl = dataSnapshot.child("imageUrl").getValue(String.class);
//                        String newUsername = dataSnapshot.child("username").getValue(String.class);
//
//                        // Update profile image if the URL has changed
//                        if (!Objects.equals(newImageUrl, cachedImageUrl)) {
//                            cachedImageUrl = newImageUrl;
//                            if (newImageUrl != null && !newImageUrl.equals("default") && !newImageUrl.isEmpty()) {
//                                Picasso.get()
//                                        .load(newImageUrl)
//                                        .placeholder(R.drawable.profile)
//                                        .into(profileImage);
//                            } else {
//                                profileImage.setImageResource(R.drawable.profile);
//                            }
//                        }
//
//                        // Update username if it has changed
//                        if (!Objects.equals(newUsername, cachedUsername)) {
//                            cachedUsername = newUsername;
//                            if (newUsername != null) {
//                                usernameTextView.setText(newUsername);
//                            }
//                        }
//
//                    }
//                }
//
//                @Override
//                public void onCancelled(@NonNull DatabaseError databaseError) {
//                    // Use getActivity() if in a Fragment; otherwise use 'this' or the Activity context.
//                    Toast.makeText(getActivity(), "Failed to load profile data", Toast.LENGTH_SHORT).show();
//                }
//            };
//
//            // Attach the listener persistently so it monitors for changes.
//            userRef.addValueEventListener(profileListener);
//        }
//    }
//
//
//    /**
//     * Listen for new messages from booking status in Firebase.
//     * Assumes each booking snapshot may have a "newMessage" field.
//     */
//    private void listenForFirebaseNotifications() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("notifications");
//
//        notificationRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                int notificationCount = 0;
//                for (DataSnapshot child : snapshot.getChildren()) {
//                    Boolean read = child.child("read").getValue(Boolean.class);
//                    if (read == null || !read) {
//                        notificationCount++;
//                    }
//                }
//                updateBadgeCount(notificationCount);
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) { }
//        });
//    }
//
//    private void updateBadgeCount(int count) {
//        if (count > 0) {
//            badgeCount.setText(String.valueOf(count));
//            badgeCount.setVisibility(View.VISIBLE);
//        } else {
//            badgeCount.setVisibility(View.GONE);
//        }
//    }
//
//    private void markAllNotificationsAsRead() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) return;
//        String userId = currentUser.getUid();
//        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("notifications");
//        notificationRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                for (DataSnapshot child : snapshot.getChildren()) {
//                    child.getRef().child("read").setValue(true);
//                }
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) { }
//        });
//    }
//
//}
