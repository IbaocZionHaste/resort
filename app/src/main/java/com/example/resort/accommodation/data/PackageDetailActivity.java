package com.example.resort.accommodation.data;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Base64;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.resort.R;
import com.example.resort.addcart.data.CartItem;
import com.example.resort.addcart.data.CartManager;
import com.example.resort.review.data.Review;
import com.example.resort.review.data.ReviewAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.ByteArrayOutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PackageDetailActivity extends AppCompatActivity {

    // Field for available date (if provided from extras)
    private String availableDate;

    // Swipe images list: holds either Integer (resource ID) or Bitmap (from album)
    private List<Object> swipeImages;
    private int currentImageIndex = 0;
    private ImageView ivImageSwipe; // This is your main image view (ivCottageImage)

    // Dot indicators container (ensure your XML has a LinearLayout with id "llDots")
    private LinearLayout llDots;
    private void updateAverageRating(List<Review> reviews) {
        float total = 0;
        for (Review review : reviews) {
            total += review.getRate();
        }
        float average = reviews.size() > 0 ? total / reviews.size() : 0.0f;
        TextView tvRating = findViewById(R.id.rating);
        String newRating = String.format("%.1f", average);

        // Optional: animate rating update
        tvRating.setAlpha(0f);
        tvRating.setText(newRating);
        tvRating.animate().alpha(1f).setDuration(500).start();
    }



    // ----- Helper Methods for Dot Indicators -----
    private void setupDots() {
        llDots.removeAllViews();
        for (int i = 0; i < swipeImages.size(); i++) {
            TextView dot = new TextView(this);
            dot.setText("●"); // Unicode bullet
            dot.setTextSize(15);
            dot.setPadding(8, 0, 8, 0);
            dot.setTextColor(getResources().getColor(R.color.grey)); // Inactive color
            llDots.addView(dot);
        }
        updateDots();
    }

    private void updateDots() {
        for (int i = 0; i < llDots.getChildCount(); i++) {
            TextView dot = (TextView) llDots.getChildAt(i);
            if (i == currentImageIndex) {
                dot.setTextColor(getResources().getColor(R.color.light_blue)); // Active dot
            } else {
                dot.setTextColor(getResources().getColor(R.color.grey));
            }
        }
    }


    // ----- Display the current image from swipeImages -----
    private void displayCurrentImage() {
        Object item = swipeImages.get(currentImageIndex);
        if (item instanceof Integer) {
            ivImageSwipe.setImageResource((Integer) item);
        } else if (item instanceof Bitmap) {
            ivImageSwipe.setImageBitmap((Bitmap) item);
        } else if (item instanceof String) {
            // Use Glide to load the image URL (Firebase Storage URL)
            Glide.with(this)
                    .load((String) item)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(ivImageSwipe);
        }
    }

    // ----- Swipe Handlers -----
    private void onSwipeLeft() {
        if (currentImageIndex < swipeImages.size() - 1) {
            currentImageIndex++;
            displayCurrentImage();
            updateDots();
        }
    }

    private void onSwipeRight() {
        if (currentImageIndex > 0) {
            currentImageIndex--;
            displayCurrentImage();
            updateDots();
        }
    }


    // ----- Asynchronous Album Data Fetching -----
    // Fetches album data from Firebase. If an album's productName matches the cottage name,
    // then its photo1, photo2, and photo3 are assumed to be Firebase Storage URLs and replace the default images.
    private void fetchAlbumData(final String cottageName) {
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums");
        albumRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                boolean found = false;
                for (DataSnapshot albumSnapshot : dataSnapshot.getChildren()) {
                    String productName = albumSnapshot.child("productName").getValue(String.class);
                    if (productName != null && productName.equals(cottageName)) {
                        found = true;
                        String photo1Str = albumSnapshot.child("photo1").getValue(String.class);
                        String photo2Str = albumSnapshot.child("photo2").getValue(String.class);
                        String photo3Str = albumSnapshot.child("photo3").getValue(String.class);
                        // Remove prefix if present (e.g., "data:image/png;base64,") if any, though not needed for URLs.
                        if (photo1Str != null && photo1Str.contains(",")) {
                            photo1Str = photo1Str.substring(photo1Str.indexOf(",") + 1);
                        }
                        if (photo2Str != null && photo2Str.contains(",")) {
                            photo2Str = photo2Str.substring(photo2Str.indexOf(",") + 1);
                        }
                        if (photo3Str != null && photo3Str.contains(",")) {
                            photo3Str = photo3Str.substring(photo3Str.indexOf(",") + 1);
                        }
                        // Instead of decoding Base64, we assume these are now Firebase Storage URLs.
                        swipeImages.clear();
                        swipeImages.add(photo1Str);
                        swipeImages.add(photo2Str);
                        swipeImages.add(photo3Str);
                        currentImageIndex = 0;
                        displayCurrentImage();
                        updateDots();
                        break;
                    }
                }
                // If no matching album is found, defaults remain.
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                // Handle potential errors here
            }
        });
    }


    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_package_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Extract extras from the intent.
        Intent intent = getIntent();
        String productId = intent.getStringExtra("productId");
        String name = intent.getStringExtra("accommodationName");
        String description = intent.getStringExtra("accommodationDesc");
        String capacityCottage = intent.getStringExtra("capacityCottage");
        String food1 = intent.getStringExtra("food1");
        String food2 = intent.getStringExtra("food2");
        String food3 = intent.getStringExtra("food3");
        String food4 = intent.getStringExtra("food4");
        String food5 = intent.getStringExtra("food5");
        String food6 = intent.getStringExtra("food6");
        String status = intent.getStringExtra("accommodationStat");
        String beverage = intent.getStringExtra("accommodationBeverage");
        String cottage = intent.getStringExtra("accommodationCottage");
        String price = intent.getStringExtra("accommodationPrice");
        //String imageUrl = intent.getStringExtra("accommodationImage");
        // Get the available date extra (if provided)
        availableDate = intent.getStringExtra("accommodationAvailableDate");

        // Find views in the layout.
        TextView tvName = findViewById(R.id.tvPackageName);
        TextView tvDescription = findViewById(R.id.tvPackageDescription);
        TextView tvCapacity = findViewById(R.id.tvPackageCapacity);
        TextView tvPackageItems = findViewById(R.id.tvPackageItems);
        TextView tvPackageItems2 = findViewById(R.id.tvPackageItems2);
        TextView tvPackageItems3 = findViewById(R.id.tvPackageItems3);
        TextView tvPackageItems4 = findViewById(R.id.tvPackageItems4);
        TextView tvPackageItems5 = findViewById(R.id.tvPackageItems5);
        TextView tvPackageItems6 = findViewById(R.id.tvPackageItems6);
        TextView tvCottage = findViewById(R.id.tvPackageCottage);
        TextView tvBeverage = findViewById(R.id.tvPackageBeverage);
        TextView tvPrice = findViewById(R.id.tvPackagePrice);
        TextView tvStatus = findViewById(R.id.tvPackageStatus);
        // NEW: TextView to display the available date.
        TextView tvAvailableDate = findViewById(R.id.tvAvailableDate);
        ivImageSwipe= findViewById(R.id.ivPackageImage);
        ImageView btnBack = findViewById(R.id.btn);
        Button btnAddToCart = findViewById(R.id.btn_add_to_cart);

        // Dot indicators container (ensure you have this in your XML)
        llDots = findViewById(R.id.llDots);

        // Set default swipe images (using your existing drawables)
        swipeImages = new ArrayList<>();
        swipeImages.add(R.drawable.ic_no_image);
        swipeImages.add(R.drawable.ic_no_image);
        swipeImages.add(R.drawable.ic_no_image);
        currentImageIndex = 0;
        displayCurrentImage();
        setupDots();

        // Setup swipe gesture detection
        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
            @Override
            public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
                float diffX = e2.getX() - e1.getX();
                float diffY = e2.getY() - e1.getY();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffX > 0) {
                            onSwipeRight();
                        } else {
                            onSwipeLeft();
                        }
                        return true;
                    }
                }
                return false;
            }
        });
        ivImageSwipe.setClickable(true);
        ivImageSwipe.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                return gestureDetector.onTouchEvent(event);
            }
        });



        btnBack.setOnClickListener(v -> onBackPressed());

        // Use the item name passed via the Intent as the favorite key.
        final String favoriteKey = getIntent().getStringExtra("accommodationName");

        // Find the heart icon view.
        ImageView heartIcon = findViewById(R.id.heart);
        final boolean[] isFavorite = {false}; // This holds the mutable favorite state for this item

        // Retrieve the current Firebase user.
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            // Reference the current user's favorites using the item name as key.
            DatabaseReference userFavoritesRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("favorites");

            // Check if this specific item (by name) is already in favorites.
            userFavoritesRef.child(favoriteKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        isFavorite[0] = true;
                        // Set heart icon to red if it is already marked as favorite.
                        heartIcon.setColorFilter(getResources().getColor(R.color.red));
                    } else {
                        isFavorite[0] = false;
                        heartIcon.clearColorFilter();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle errors if needed.
                }
            });

            // Toggle the favorite state when the heart icon is clicked.
            heartIcon.setOnClickListener(v -> {
                if (isFavorite[0]) {
                    // Remove this item from favorites.
                    userFavoritesRef.child(favoriteKey).removeValue();
                    heartIcon.clearColorFilter();  // Revert to the default icon color.
                    isFavorite[0] = false;
                    Toast.makeText(PackageDetailActivity.this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
                } else {
                    // Add this item to favorites. Only this item's name is stored.
                    userFavoritesRef.child(favoriteKey).setValue(true);
                    heartIcon.setColorFilter(getResources().getColor(R.color.red));
                    isFavorite[0] = true;
                    Toast.makeText(PackageDetailActivity.this, "Added to Favorites", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Update status UI based on the passed "status" string.
        if ("Available".equalsIgnoreCase(status)) {
            tvStatus.setText("Available");
            tvStatus.setTextColor(getResources().getColor(R.color.green));
            btnAddToCart.setEnabled(true);
            btnAddToCart.setText("Book Now");
        } else if ("Unavailable".equalsIgnoreCase(status)) {
            tvStatus.setText("Sold Out");
            tvStatus.setTextColor(getResources().getColor(R.color.red));
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Sold Out");
        }

        if ("Available".equalsIgnoreCase(status)) {
            tvStatus.setText("Available");
            tvStatus.setTextColor(getResources().getColor(R.color.green));
            btnAddToCart.setEnabled(true);
            btnAddToCart.setText("Book Now");

            // Hide availableDate in UI.
            tvAvailableDate.setText("");
            tvAvailableDate.setVisibility(View.GONE);

            // Update status in Firebase without removing availableDate.
            if (productId != null && !productId.trim().isEmpty()) {
                DatabaseReference productRef = FirebaseDatabase.getInstance()
                        .getReference("products").child("Package").child(productId);
                productRef.child("status").setValue("Available");
            }

        } else if ("Unavailable".equalsIgnoreCase(status)) {
            tvStatus.setText("Sold Out");
            tvStatus.setTextColor(getResources().getColor(R.color.red));
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Sold Out");

            // Process if availableDate exists.
            if (availableDate != null && !availableDate.trim().isEmpty()) {
                String extractedDate = availableDate;
                if (extractedDate.startsWith("Date:")) {
                    extractedDate = extractedDate.substring(5).trim();
                }
                int parenIndex = extractedDate.indexOf("(");
                if (parenIndex != -1) {
                    extractedDate = extractedDate.substring(0, parenIndex).trim();
                }
                final SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd yyyy", Locale.getDefault());
                try {
                    Date parsedDate = sdf.parse(extractedDate);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(parsedDate);
                    // Add one day.
                    cal.add(Calendar.DATE, 1);
                    final String adjustedDate = sdf.format(cal.getTime());
                    final Date availableDateObj = sdf.parse(adjustedDate);

                    // Set up a Handler to check every second asynchronously.
                    final Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            Date currentDateObj = new Date();
                            // Check if current date is after or equal to the adjusted available date.
                            if (currentDateObj.after(availableDateObj) || sdf.format(currentDateObj).equals(adjustedDate)) {
                                // Update Firebase status to Available and remove availableDate.
                                if (productId != null && !productId.trim().isEmpty()) {
                                    DatabaseReference productRef = FirebaseDatabase.getInstance()
                                            .getReference("products")
                                            .child("Package")
                                            .child(productId);
                                    productRef.child("status").setValue("Available");
                                    // Remove the availableDate field.
                                    productRef.child("availableDate").removeValue();
                                }
                                // Hide availableDate in UI.
                                tvAvailableDate.setText("");
                                tvAvailableDate.setVisibility(View.GONE);
                            } else {
                                // Display adjusted date in UI while not expired.
                                tvAvailableDate.setText("Available Date: " + adjustedDate);
                                tvAvailableDate.setVisibility(View.VISIBLE);
                                handler.postDelayed(this, 1000);
                            }
                        }
                    };
                    handler.post(runnable);

                } catch (ParseException e) {
                    e.printStackTrace();
                    tvAvailableDate.setText("");
                    tvAvailableDate.setVisibility(View.GONE);
                }
            } else {
                tvAvailableDate.setVisibility(View.GONE);
            }
        }

        // Set price text
        if (price != null && !price.trim().isEmpty()) {
            tvPrice.setText("Price: ₱" + price);
        } else {
            tvPrice.setText("Price: ₱0.00");
        }

        // Set remaining texts.
        tvName.setText(name);
        tvDescription.setText(description);
        tvCapacity.setText("Capacity: " + capacityCottage);
        tvPackageItems.setText("Food 1: " + food1);
        tvPackageItems2.setText("Food 2: " + food2);
        tvPackageItems3.setText("Food 3: " + food3);
        tvPackageItems4.setText("Food 4: " + food4);
        tvPackageItems5.setText("Food 5: " + food5);
        tvPackageItems6.setText("Food 6: " + food6);
        tvCottage.setText("Cottage: " + cottage);
        tvBeverage.setText("Beverage: " + beverage);



        // --- NEW CODE: Initialize RecyclerView for Reviews ---
        RecyclerView recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        List<Review> reviewList = new ArrayList<>();
        ReviewAdapter reviewAdapter = new ReviewAdapter(reviewList);
        recyclerViewReviews.setAdapter(reviewAdapter);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

//        // Fetch reviews for the current package (match by itemName).
//        final String packageName = tvName.getText().toString();
//        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
//        usersRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                List<Review> reviews = new ArrayList<>();
//                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
//                    DataSnapshot ratingsSnapshot = userSnapshot.child("MyRating");
//                    if (ratingsSnapshot.exists()) {
//                        for (DataSnapshot ratingSnapshot : ratingsSnapshot.getChildren()) {
//                            String itemName = ratingSnapshot.child("itemName").getValue(String.class);
//                            if (packageName.equals(itemName)) {
//                                String comment = ratingSnapshot.child("comment").getValue(String.class);
//                                String date = ratingSnapshot.child("date").getValue(String.class);
//                                Integer rate = ratingSnapshot.child("rate").getValue(Integer.class);
//                                String user = ratingSnapshot.child("user").getValue(String.class);
//                                String category = ratingSnapshot.child("category").getValue(String.class);
//
//                                Review review = new Review(user, rate, comment, date, category, itemName);
//                                reviews.add(review);
//                            }
//                        }
//                    }
//                }
//                reviewAdapter.updateReviews(reviews);
//                updateAverageRating(reviews);
//            }

        final String cottageName = tvName.getText().toString().trim();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Review> reviews = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot ratingsSnapshot = userSnapshot.child("MyRating");
                    if (ratingsSnapshot.exists()) {
                        for (DataSnapshot ratingSnapshot : ratingsSnapshot.getChildren()) {
                            String itemNameField = ratingSnapshot.child("itemName")
                                    .getValue(String.class);
                            if (itemNameField != null) {
                                // split the stored string into individual names
                                String[] storedNames = itemNameField.split("\\s*,\\s*");
                                // check if our cottageName is one of them
                                boolean matches = false;
                                for (String n : storedNames) {
                                    if (cottageName.equalsIgnoreCase(n.trim())) {
                                        matches = true;
                                        break;
                                    }
                                }

                                if (matches) {
                                    String comment  = ratingSnapshot.child("comment")
                                            .getValue(String.class);
                                    String date     = ratingSnapshot.child("date")
                                            .getValue(String.class);
                                    Integer rate    = ratingSnapshot.child("rate")
                                            .getValue(Integer.class);
                                    String user     = ratingSnapshot.child("user")
                                            .getValue(String.class);
                                    String category = ratingSnapshot.child("category")
                                            .getValue(String.class);

                                    Review review = new Review(
                                            user, rate, comment, date, category, cottageName
                                    );
                                    reviews.add(review);
                                }
                            }
                        }
                    }
                }
                reviewAdapter.updateReviews(reviews);
                updateAverageRating(reviews);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(PackageDetailActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
        // --- END NEW CODE ---

        // Add to Cart functionality.
        btnAddToCart.setOnClickListener(v -> {
            // Retrieve the current Firebase user and its UID.
            FirebaseUser currentUserForCart = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUserForCart == null) {
                Toast.makeText(PackageDetailActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            String uid = currentUserForCart.getUid();

            // Check if an item with the same name already exists in the cart.
            boolean productExists = false;
            for (CartItem cartItem : CartManager.getInstance(this, uid).getCartItems()) {
                if (cartItem.getName().equals(name)) {
                    productExists = true;
                    break;
                }
            }
            if (productExists) {
                Toast.makeText(PackageDetailActivity.this, "Sorry, only 1 item of this product can be booked.", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check product availability.
            if (!"Available".equalsIgnoreCase(status)) {
                String msg = "This item is sold out";
                if (availableDate != null && !availableDate.trim().isEmpty()) {
                    msg += " until " + availableDate;
                }
                Toast.makeText(PackageDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                return;
            }

            double itemPrice = 0.0;
            try {
                itemPrice = Double.parseDouble(price.trim());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            /// --- Revised: Always use the first image (photo1) for the cart ---
            /// Instead of converting a Bitmap to Base64, we check if the first image is a URL.
            String photoForCart = "";
            Object firstImage = swipeImages.get(0);
            if (firstImage instanceof String) {
                photoForCart = (String) firstImage;
            } else if (firstImage instanceof Bitmap) {
                // Optionally, if still a Bitmap, you might want to save it locally or use a fallback.
                photoForCart = "";
            } else if (firstImage instanceof Integer) {
                // If it's a resource, you can choose to leave it as an empty string or a default URL.
                photoForCart = "";
            }
            // --- End revised section ---

            // Create the CartItem with category "Package" and add it to the cart.
            CartItem item = new CartItem(name, itemPrice, "Package",photoForCart);
            CartManager.getInstance(this, uid).addItem(item);
            Toast.makeText(PackageDetailActivity.this, "Added to Cart Successfully", Toast.LENGTH_SHORT).show();
        });


        // ----- Fetch Album Data Asynchronously -----
        fetchAlbumData(tvName.getText().toString());
    }
}

///Fix Current
//package com.example.resort.accommodation.data;
//
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.graphics.drawable.BitmapDrawable;
//import android.os.Bundle;
//import android.os.Handler;
//import android.util.Base64;
//import android.view.GestureDetector;
//import android.view.MotionEvent;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ImageView;
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
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//import com.bumptech.glide.load.engine.DiskCacheStrategy;
//import com.example.resort.R;
//import com.example.resort.addcart.data.CartItem;
//import com.example.resort.addcart.data.CartManager;
//import com.example.resort.review.data.Review;
//import com.example.resort.review.data.ReviewAdapter;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.io.ByteArrayOutputStream;
//import java.text.ParseException;
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.List;
//import java.util.Locale;
//
//public class PackageDetailActivity extends AppCompatActivity {
//
//    // Field for available date (if provided from extras)
//    private String availableDate;
//
//    // Swipe images list: holds either Integer (resource ID) or Bitmap (from album)
//    private List<Object> swipeImages;
//    private int currentImageIndex = 0;
//    private ImageView ivImageSwipe; // This is your main image view (ivCottageImage)
//
//    // Dot indicators container (ensure your XML has a LinearLayout with id "llDots")
//    private LinearLayout llDots;
//    private void updateAverageRating(List<Review> reviews) {
//        float total = 0;
//        for (Review review : reviews) {
//            total += review.getRate();
//        }
//        float average = reviews.size() > 0 ? total / reviews.size() : 0.0f;
//        TextView tvRating = findViewById(R.id.rating);
//        String newRating = String.format("%.1f", average);
//
//        // Optional: animate rating update
//        tvRating.setAlpha(0f);
//        tvRating.setText(newRating);
//        tvRating.animate().alpha(1f).setDuration(500).start();
//    }
//
//
//
//    // ----- Helper Methods for Dot Indicators -----
//    private void setupDots() {
//        llDots.removeAllViews();
//        for (int i = 0; i < swipeImages.size(); i++) {
//            TextView dot = new TextView(this);
//            dot.setText("●"); // Unicode bullet
//            dot.setTextSize(15);
//            dot.setPadding(8, 0, 8, 0);
//            dot.setTextColor(getResources().getColor(R.color.grey)); // Inactive color
//            llDots.addView(dot);
//        }
//        updateDots();
//    }
//
//    private void updateDots() {
//        for (int i = 0; i < llDots.getChildCount(); i++) {
//            TextView dot = (TextView) llDots.getChildAt(i);
//            if (i == currentImageIndex) {
//                dot.setTextColor(getResources().getColor(R.color.light_blue)); // Active dot
//            } else {
//                dot.setTextColor(getResources().getColor(R.color.grey));
//            }
//        }
//    }
//
//
//    // ----- Display the current image from swipeImages -----
//    private void displayCurrentImage() {
//        Object item = swipeImages.get(currentImageIndex);
//        if (item instanceof Integer) {
//            ivImageSwipe.setImageResource((Integer) item);
//        } else if (item instanceof Bitmap) {
//            ivImageSwipe.setImageBitmap((Bitmap) item);
//        } else if (item instanceof String) {
//            // Use Glide to load the image URL (Firebase Storage URL)
//            Glide.with(this)
//                    .load((String) item)
//                    .diskCacheStrategy(DiskCacheStrategy.ALL)
//                    .into(ivImageSwipe);
//        }
//    }
//
//    // ----- Swipe Handlers -----
//    private void onSwipeLeft() {
//        if (currentImageIndex < swipeImages.size() - 1) {
//            currentImageIndex++;
//            displayCurrentImage();
//            updateDots();
//        }
//    }
//
//    private void onSwipeRight() {
//        if (currentImageIndex > 0) {
//            currentImageIndex--;
//            displayCurrentImage();
//            updateDots();
//        }
//    }
//
//
//    // ----- Asynchronous Album Data Fetching -----
//    // Fetches album data from Firebase. If an album's productName matches the cottage name,
//    // then its photo1, photo2, and photo3 are assumed to be Firebase Storage URLs and replace the default images.
//    private void fetchAlbumData(final String cottageName) {
//        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums");
//        albumRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                boolean found = false;
//                for (DataSnapshot albumSnapshot : dataSnapshot.getChildren()) {
//                    String productName = albumSnapshot.child("productName").getValue(String.class);
//                    if (productName != null && productName.equals(cottageName)) {
//                        found = true;
//                        String photo1Str = albumSnapshot.child("photo1").getValue(String.class);
//                        String photo2Str = albumSnapshot.child("photo2").getValue(String.class);
//                        String photo3Str = albumSnapshot.child("photo3").getValue(String.class);
//                        // Remove prefix if present (e.g., "data:image/png;base64,") if any, though not needed for URLs.
//                        if (photo1Str != null && photo1Str.contains(",")) {
//                            photo1Str = photo1Str.substring(photo1Str.indexOf(",") + 1);
//                        }
//                        if (photo2Str != null && photo2Str.contains(",")) {
//                            photo2Str = photo2Str.substring(photo2Str.indexOf(",") + 1);
//                        }
//                        if (photo3Str != null && photo3Str.contains(",")) {
//                            photo3Str = photo3Str.substring(photo3Str.indexOf(",") + 1);
//                        }
//                        // Instead of decoding Base64, we assume these are now Firebase Storage URLs.
//                        swipeImages.clear();
//                        swipeImages.add(photo1Str);
//                        swipeImages.add(photo2Str);
//                        swipeImages.add(photo3Str);
//                        currentImageIndex = 0;
//                        displayCurrentImage();
//                        updateDots();
//                        break;
//                    }
//                }
//                // If no matching album is found, defaults remain.
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                // Handle potential errors here
//            }
//        });
//    }
//
//
//    @SuppressLint("SetTextI18n")
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_package_detail);
//
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Extract extras from the intent.
//        Intent intent = getIntent();
//        String productId = intent.getStringExtra("productId");
//        String name = intent.getStringExtra("accommodationName");
//        String description = intent.getStringExtra("accommodationDesc");
//        String capacityCottage = intent.getStringExtra("capacityCottage");
//        String food1 = intent.getStringExtra("food1");
//        String food2 = intent.getStringExtra("food2");
//        String food3 = intent.getStringExtra("food3");
//        String food4 = intent.getStringExtra("food4");
//        String food5 = intent.getStringExtra("food5");
//        String food6 = intent.getStringExtra("food6");
//        String status = intent.getStringExtra("accommodationStat");
//        String beverage = intent.getStringExtra("accommodationBeverage");
//        String cottage = intent.getStringExtra("accommodationCottage");
//        String price = intent.getStringExtra("accommodationPrice");
//        //String imageUrl = intent.getStringExtra("accommodationImage");
//        // Get the available date extra (if provided)
//        availableDate = intent.getStringExtra("accommodationAvailableDate");
//
//        // Find views in the layout.
//        TextView tvName = findViewById(R.id.tvPackageName);
//        TextView tvDescription = findViewById(R.id.tvPackageDescription);
//        TextView tvCapacity = findViewById(R.id.tvPackageCapacity);
//        TextView tvPackageItems = findViewById(R.id.tvPackageItems);
//        TextView tvPackageItems2 = findViewById(R.id.tvPackageItems2);
//        TextView tvPackageItems3 = findViewById(R.id.tvPackageItems3);
//        TextView tvPackageItems4 = findViewById(R.id.tvPackageItems4);
//        TextView tvPackageItems5 = findViewById(R.id.tvPackageItems5);
//        TextView tvPackageItems6 = findViewById(R.id.tvPackageItems6);
//        TextView tvCottage = findViewById(R.id.tvPackageCottage);
//        TextView tvBeverage = findViewById(R.id.tvPackageBeverage);
//        TextView tvPrice = findViewById(R.id.tvPackagePrice);
//        TextView tvStatus = findViewById(R.id.tvPackageStatus);
//        // NEW: TextView to display the available date.
//        TextView tvAvailableDate = findViewById(R.id.tvAvailableDate);
//        ivImageSwipe= findViewById(R.id.ivPackageImage);
//        ImageView btnBack = findViewById(R.id.btn);
//        Button btnAddToCart = findViewById(R.id.btn_add_to_cart);
//
//        // Dot indicators container (ensure you have this in your XML)
//        llDots = findViewById(R.id.llDots);
//
//        // Set default swipe images (using your existing drawables)
//        swipeImages = new ArrayList<>();
//        swipeImages.add(R.drawable.ic_no_image);
//        swipeImages.add(R.drawable.ic_no_image);
//        swipeImages.add(R.drawable.ic_no_image);
//        currentImageIndex = 0;
//        displayCurrentImage();
//        setupDots();
//
//        // Setup swipe gesture detection
//        final GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
//            private static final int SWIPE_THRESHOLD = 100;
//            private static final int SWIPE_VELOCITY_THRESHOLD = 100;
//            @Override
//            public boolean onFling(MotionEvent e1, @NonNull MotionEvent e2, float velocityX, float velocityY) {
//                float diffX = e2.getX() - e1.getX();
//                float diffY = e2.getY() - e1.getY();
//                if (Math.abs(diffX) > Math.abs(diffY)) {
//                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
//                        if (diffX > 0) {
//                            onSwipeRight();
//                        } else {
//                            onSwipeLeft();
//                        }
//                        return true;
//                    }
//                }
//                return false;
//            }
//        });
//        ivImageSwipe.setClickable(true);
//        ivImageSwipe.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                return gestureDetector.onTouchEvent(event);
//            }
//        });
//
//
//
//        btnBack.setOnClickListener(v -> onBackPressed());
//
//        // Use the item name passed via the Intent as the favorite key.
//        final String favoriteKey = getIntent().getStringExtra("accommodationName");
//
//        // Find the heart icon view.
//        ImageView heartIcon = findViewById(R.id.heart);
//        final boolean[] isFavorite = {false}; // This holds the mutable favorite state for this item
//
//        // Retrieve the current Firebase user.
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            // Reference the current user's favorites using the item name as key.
//            DatabaseReference userFavoritesRef = FirebaseDatabase.getInstance()
//                    .getReference("users")
//                    .child(currentUser.getUid())
//                    .child("favorites");
//
//            // Check if this specific item (by name) is already in favorites.
//            userFavoritesRef.child(favoriteKey).addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot snapshot) {
//                    if (snapshot.exists()) {
//                        isFavorite[0] = true;
//                        // Set heart icon to red if it is already marked as favorite.
//                        heartIcon.setColorFilter(getResources().getColor(R.color.red));
//                    } else {
//                        isFavorite[0] = false;
//                        heartIcon.clearColorFilter();
//                    }
//                }
//                @Override
//                public void onCancelled(@NonNull DatabaseError error) {
//                    // Handle errors if needed.
//                }
//            });
//
//            // Toggle the favorite state when the heart icon is clicked.
//            heartIcon.setOnClickListener(v -> {
//                if (isFavorite[0]) {
//                    // Remove this item from favorites.
//                    userFavoritesRef.child(favoriteKey).removeValue();
//                    heartIcon.clearColorFilter();  // Revert to the default icon color.
//                    isFavorite[0] = false;
//                    Toast.makeText(PackageDetailActivity.this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
//                } else {
//                    // Add this item to favorites. Only this item's name is stored.
//                    userFavoritesRef.child(favoriteKey).setValue(true);
//                    heartIcon.setColorFilter(getResources().getColor(R.color.red));
//                    isFavorite[0] = true;
//                    Toast.makeText(PackageDetailActivity.this, "Added to Favorites", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//
//        // Update status UI based on the passed "status" string.
//        if ("Available".equalsIgnoreCase(status)) {
//            tvStatus.setText("Available");
//            tvStatus.setTextColor(getResources().getColor(R.color.green));
//            btnAddToCart.setEnabled(true);
//            btnAddToCart.setText("Book Now");
//        } else if ("Unavailable".equalsIgnoreCase(status)) {
//            tvStatus.setText("Sold Out");
//            tvStatus.setTextColor(getResources().getColor(R.color.red));
//            btnAddToCart.setEnabled(false);
//            btnAddToCart.setText("Sold Out");
//        }
//
//        if ("Available".equalsIgnoreCase(status)) {
//            tvStatus.setText("Available");
//            tvStatus.setTextColor(getResources().getColor(R.color.green));
//            btnAddToCart.setEnabled(true);
//            btnAddToCart.setText("Book Now");
//
//            // Hide availableDate in UI.
//            tvAvailableDate.setText("");
//            tvAvailableDate.setVisibility(View.GONE);
//
//            // Update status in Firebase without removing availableDate.
//            if (productId != null && !productId.trim().isEmpty()) {
//                DatabaseReference productRef = FirebaseDatabase.getInstance()
//                        .getReference("products").child("Package").child(productId);
//                productRef.child("status").setValue("Available");
//            }
//
//        } else if ("Unavailable".equalsIgnoreCase(status)) {
//            tvStatus.setText("Sold Out");
//            tvStatus.setTextColor(getResources().getColor(R.color.red));
//            btnAddToCart.setEnabled(false);
//            btnAddToCart.setText("Sold Out");
//
//            // Process if availableDate exists.
//            if (availableDate != null && !availableDate.trim().isEmpty()) {
//                String extractedDate = availableDate;
//                if (extractedDate.startsWith("Date:")) {
//                    extractedDate = extractedDate.substring(5).trim();
//                }
//                int parenIndex = extractedDate.indexOf("(");
//                if (parenIndex != -1) {
//                    extractedDate = extractedDate.substring(0, parenIndex).trim();
//                }
//                final SimpleDateFormat sdf = new SimpleDateFormat("MMMM dd yyyy", Locale.getDefault());
//                try {
//                    Date parsedDate = sdf.parse(extractedDate);
//                    Calendar cal = Calendar.getInstance();
//                    cal.setTime(parsedDate);
//                    // Add one day.
//                    cal.add(Calendar.DATE, 1);
//                    final String adjustedDate = sdf.format(cal.getTime());
//                    final Date availableDateObj = sdf.parse(adjustedDate);
//
//                    // Set up a Handler to check every second asynchronously.
//                    final Handler handler = new Handler();
//                    Runnable runnable = new Runnable() {
//                        @Override
//                        public void run() {
//                            Date currentDateObj = new Date();
//                            // Check if current date is after or equal to the adjusted available date.
//                            if (currentDateObj.after(availableDateObj) || sdf.format(currentDateObj).equals(adjustedDate)) {
//                                // Update Firebase status to Available and remove availableDate.
//                                if (productId != null && !productId.trim().isEmpty()) {
//                                    DatabaseReference productRef = FirebaseDatabase.getInstance()
//                                            .getReference("products")
//                                            .child("Package")
//                                            .child(productId);
//                                    productRef.child("status").setValue("Available");
//                                    // Remove the availableDate field.
//                                    productRef.child("availableDate").removeValue();
//                                }
//                                // Hide availableDate in UI.
//                                tvAvailableDate.setText("");
//                                tvAvailableDate.setVisibility(View.GONE);
//                            } else {
//                                // Display adjusted date in UI while not expired.
//                                tvAvailableDate.setText("Available Date: " + adjustedDate);
//                                tvAvailableDate.setVisibility(View.VISIBLE);
//                                handler.postDelayed(this, 1000);
//                            }
//                        }
//                    };
//                    handler.post(runnable);
//
//                } catch (ParseException e) {
//                    e.printStackTrace();
//                    tvAvailableDate.setText("");
//                    tvAvailableDate.setVisibility(View.GONE);
//                }
//            } else {
//                tvAvailableDate.setVisibility(View.GONE);
//            }
//        }
//
//        // Set price text
//        if (price != null && !price.trim().isEmpty()) {
//            tvPrice.setText("Price: ₱" + price);
//        } else {
//            tvPrice.setText("Price: ₱0.00");
//        }
//
//        // Set remaining texts.
//        tvName.setText(name);
//        tvDescription.setText(description);
//        tvCapacity.setText("Capacity: " + capacityCottage);
//        tvPackageItems.setText("Food 1: " + food1);
//        tvPackageItems2.setText("Food 2: " + food2);
//        tvPackageItems3.setText("Food 3: " + food3);
//        tvPackageItems4.setText("Food 4: " + food4);
//        tvPackageItems5.setText("Food 5: " + food5);
//        tvPackageItems6.setText("Food 6: " + food6);
//        tvCottage.setText("Cottage: " + cottage);
//        tvBeverage.setText("Beverage: " + beverage);
//
//
//
//        // --- NEW CODE: Initialize RecyclerView for Reviews ---
//        RecyclerView recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
//        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
//        List<Review> reviewList = new ArrayList<>();
//        ReviewAdapter reviewAdapter = new ReviewAdapter(reviewList);
//        recyclerViewReviews.setAdapter(reviewAdapter);
//        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
//
//        // Fetch reviews for the current package (match by itemName).
//        final String packageName = tvName.getText().toString();
//        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
//        usersRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
//                List<Review> reviews = new ArrayList<>();
//                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
//                    DataSnapshot ratingsSnapshot = userSnapshot.child("MyRating");
//                    if (ratingsSnapshot.exists()) {
//                        for (DataSnapshot ratingSnapshot : ratingsSnapshot.getChildren()) {
//                            String itemName = ratingSnapshot.child("itemName").getValue(String.class);
//                            if (packageName.equals(itemName)) {
//                                String comment = ratingSnapshot.child("comment").getValue(String.class);
//                                String date = ratingSnapshot.child("date").getValue(String.class);
//                                Integer rate = ratingSnapshot.child("rate").getValue(Integer.class);
//                                String user = ratingSnapshot.child("user").getValue(String.class);
//                                String category = ratingSnapshot.child("category").getValue(String.class);
//
//                                Review review = new Review(user, rate, comment, date, category, itemName);
//                                reviews.add(review);
//                            }
//                        }
//                    }
//                }
//                reviewAdapter.updateReviews(reviews);
//                updateAverageRating(reviews);
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(PackageDetailActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
//            }
//        });
//        // --- END NEW CODE ---
//
//        // Add to Cart functionality.
//        btnAddToCart.setOnClickListener(v -> {
//            // Retrieve the current Firebase user and its UID.
//            FirebaseUser currentUserForCart = FirebaseAuth.getInstance().getCurrentUser();
//            if (currentUserForCart == null) {
//                Toast.makeText(PackageDetailActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            String uid = currentUserForCart.getUid();
//
//            // Check if an item with the same name already exists in the cart.
//            boolean productExists = false;
//            for (CartItem cartItem : CartManager.getInstance(this, uid).getCartItems()) {
//                if (cartItem.getName().equals(name)) {
//                    productExists = true;
//                    break;
//                }
//            }
//            if (productExists) {
//                Toast.makeText(PackageDetailActivity.this, "Sorry, only 1 item of this product can be booked.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Check product availability.
//            if (!"Available".equalsIgnoreCase(status)) {
//                String msg = "This item is sold out";
//                if (availableDate != null && !availableDate.trim().isEmpty()) {
//                    msg += " until " + availableDate;
//                }
//                Toast.makeText(PackageDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            double itemPrice = 0.0;
//            try {
//                itemPrice = Double.parseDouble(price.trim());
//            } catch (NumberFormatException e) {
//                e.printStackTrace();
//            }
//
//            /// --- Revised: Always use the first image (photo1) for the cart ---
//            /// Instead of converting a Bitmap to Base64, we check if the first image is a URL.
//            String photoForCart = "";
//            Object firstImage = swipeImages.get(0);
//            if (firstImage instanceof String) {
//                photoForCart = (String) firstImage;
//            } else if (firstImage instanceof Bitmap) {
//                // Optionally, if still a Bitmap, you might want to save it locally or use a fallback.
//                photoForCart = "";
//            } else if (firstImage instanceof Integer) {
//                // If it's a resource, you can choose to leave it as an empty string or a default URL.
//                photoForCart = "";
//            }
//            // --- End revised section ---
//
//            // Create the CartItem with category "Package" and add it to the cart.
//            CartItem item = new CartItem(name, itemPrice, "Package",photoForCart);
//            CartManager.getInstance(this, uid).addItem(item);
//            Toast.makeText(PackageDetailActivity.this, "Added to Cart Successfully", Toast.LENGTH_SHORT).show();
//        });
//
//
//        // ----- Fetch Album Data Asynchronously -----
//        fetchAlbumData(tvName.getText().toString());
//    }
//}
