package com.example.resort.accommodation.data;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
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
import java.util.ArrayList;
import java.util.List;

public class DessertDetailActivity extends AppCompatActivity {

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
        setContentView(R.layout.activity_dessert_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = getIntent();
        String name = intent.getStringExtra("accommodationName");
        String description = intent.getStringExtra("accommodationDesc");
        String flavorToppings = intent.getStringExtra("flavorToppings");
        String pieceNameDessert = intent.getStringExtra("pieceNameDessert");
        String perfectFor = intent.getStringExtra("perfectFor");
        String price = intent.getStringExtra("accommodationPrice");
        String status = intent.getStringExtra("accommodationStat");
        String imageUrl = intent.getStringExtra("accommodationImage");

        TextView tvName = findViewById(R.id.tvDessertName);
        TextView tvDescription = findViewById(R.id.tvDessertDescription);
        TextView tvFlavor = findViewById(R.id.tvDessertFlavor);
        TextView tvPiece = findViewById(R.id.tvDessertSize);
        TextView tvPerfectFor = findViewById(R.id.tvDessertPerfectFor);
        TextView tvPrice = findViewById(R.id.tvDessertPrice);
        TextView tvStatus = findViewById(R.id.tvDessertStatus);
        ivImageSwipe= findViewById(R.id.ivDessertImage);
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
                    Toast.makeText(DessertDetailActivity.this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
                } else {
                    // Add this item to favorites. Only this item's name is stored.
                    userFavoritesRef.child(favoriteKey).setValue(true);
                    heartIcon.setColorFilter(getResources().getColor(R.color.red));
                    isFavorite[0] = true;
                    Toast.makeText(DessertDetailActivity.this, "Added to Favorites", Toast.LENGTH_SHORT).show();
                }
            });
        }


        if ("Available".equalsIgnoreCase(status)) {
            tvStatus.setText("Available");
            tvStatus.setTextColor(getResources().getColor(R.color.green));
        } else if ("Unavailable".equalsIgnoreCase(status)) {
            tvStatus.setText("Unavailable");
            tvStatus.setTextColor(getResources().getColor(R.color.red));
        }

        // Set the price text with the Philippine peso sign and per piece format.
        if (price != null && !price.trim().isEmpty()) {
            tvPrice.setText("₱" + price + " / Per Piece");
        } else {
            tvPrice.setText("₱0.00 / Per Piece");
        }

        tvName.setText(name);
        tvPiece.setText("Size: " + pieceNameDessert);
        tvDescription.setText(description);
        tvFlavor.setText("Flavor: " + flavorToppings);
        tvPerfectFor.setText("Perfect For: " + perfectFor);



        // --- NEW CODE: Initialize RecyclerView for Reviews ---
        RecyclerView recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        List<Review> reviewList = new ArrayList<>();
        ReviewAdapter reviewAdapter = new ReviewAdapter(reviewList);
        recyclerViewReviews.setAdapter(reviewAdapter);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        // Fetch reviews for the current dessert (match by itemName)
        final String dessertName = tvName.getText().toString();
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Review> reviews = new ArrayList<>();
                for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                    DataSnapshot ratingsSnapshot = userSnapshot.child("MyRating");
                    if (ratingsSnapshot.exists()) {
                        for (DataSnapshot ratingSnapshot : ratingsSnapshot.getChildren()) {
                            String itemName = ratingSnapshot.child("itemName").getValue(String.class);
                            if (dessertName.equals(itemName)) {
                                String comment = ratingSnapshot.child("comment").getValue(String.class);
                                String date = ratingSnapshot.child("date").getValue(String.class);
                                Integer rate = ratingSnapshot.child("rate").getValue(Integer.class);
                                String user = ratingSnapshot.child("user").getValue(String.class);
                                String category = ratingSnapshot.child("category").getValue(String.class);

                                // Create a Review object.
                                Review review = new Review(user, rate, comment, date, category, itemName);
                                reviews.add(review);
                            }
                        }
                    }
                }
                reviewAdapter.updateReviews(reviews);
                updateAverageRating(reviews); // Update the average rating view here.
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(DessertDetailActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });
        // --- END NEW CODE ---

        // Add to Cart functionality.
        btnAddToCart.setOnClickListener(v -> {
            // Retrieve current Firebase user and get its UID.
            FirebaseUser currentUserForCart = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUserForCart == null) {
                Toast.makeText(DessertDetailActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            String uid = currentUserForCart.getUid();

            // Retrieve the product name from the TextView.
            String itemName = tvName.getText().toString();

            // Get current cart items using user-specific CartManager.
            List<CartItem> cartItems = CartManager.getInstance(this, uid).getCartItems();
            CartItem existingItem = null;
            for (CartItem ci : cartItems) {
                if (ci.getName().equals(itemName)) {
                    existingItem = ci;
                    break;
                }
            }

            if (existingItem != null) {
                // If the product exists, check its quantity.
                if (existingItem.getQuantity() >= 10) {
                    Toast.makeText(DessertDetailActivity.this, "Product limit reached (Max 10 items)", Toast.LENGTH_SHORT).show();
                } else {
                    // Increase the product's quantity by one.
                    existingItem.setQuantity(existingItem.getQuantity() + 1);
                    CartManager.getInstance(this, uid).persistCart();
                    Toast.makeText(DessertDetailActivity.this, "Added to Cart Successfully", Toast.LENGTH_SHORT).show();
                }
            } else {
                // If the product doesn't exist in the cart, add it with a quantity of 1.
                double itemPrice = 0.0;
                try {
                    itemPrice = Double.parseDouble(price);
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

                // Create a CartItem; ensure your CartItem class supports (name, price, category, image).
                CartItem item = new CartItem(itemName, itemPrice, "Dessert", photoForCart);
                CartManager.getInstance(this, uid).addItem(item);
                Toast.makeText(DessertDetailActivity.this, "Added to Cart Successfully", Toast.LENGTH_SHORT).show();
            }
        });

        // ----- Fetch Album Data Asynchronously -----
        fetchAlbumData(tvName.getText().toString());
    }
}



//package com.example.resort.accommodation.data;
//
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.ImageView;
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
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class DessertDetailActivity extends AppCompatActivity {
//
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
//    @SuppressLint("SetTextI18n")
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_dessert_detail);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        Intent intent = getIntent();
//        String name = intent.getStringExtra("accommodationName");
//        String description = intent.getStringExtra("accommodationDesc");
//        String flavorToppings = intent.getStringExtra("flavorToppings");
//        String pieceNameDessert = intent.getStringExtra("pieceNameDessert");
//        String perfectFor = intent.getStringExtra("perfectFor");
//        String price = intent.getStringExtra("accommodationPrice");
//        String status = intent.getStringExtra("accommodationStat");
//        String imageUrl = intent.getStringExtra("accommodationImage");
//
//        TextView tvName = findViewById(R.id.tvDessertName);
//        TextView tvDescription = findViewById(R.id.tvDessertDescription);
//        TextView tvFlavor = findViewById(R.id.tvDessertFlavor);
//        TextView tvPiece = findViewById(R.id.tvDessertSize);
//        TextView tvPerfectFor = findViewById(R.id.tvDessertPerfectFor);
//        TextView tvPrice = findViewById(R.id.tvDessertPrice);
//        TextView tvStatus = findViewById(R.id.tvDessertStatus);
//        ImageView ivImage = findViewById(R.id.ivDessertImage);
//        ImageView btnBack = findViewById(R.id.btn);
//        Button btnAddToCart = findViewById(R.id.btn_add_to_cart);
//
//        btnBack.setOnClickListener(v -> onBackPressed());
//
//        if ("Available".equalsIgnoreCase(status)) {
//            tvStatus.setText("Available");
//            tvStatus.setTextColor(getResources().getColor(R.color.green));
//        } else if ("Unavailable".equalsIgnoreCase(status)) {
//            tvStatus.setText("Unavailable");
//            tvStatus.setTextColor(getResources().getColor(R.color.red));
//        }
//
//
//        // Set the price text with the Philippine peso sign and per piece format
//        if (price != null && !price.trim().isEmpty()) {
//            tvPrice.setText("₱" + price + " / Per Piece");
//        } else {
//            tvPrice.setText("₱0.00 / Per Piece");
//        }
//
//
//        tvName.setText(name);
//        tvPiece.setText("Size: " + pieceNameDessert);
//        tvDescription.setText(description);
//        tvFlavor.setText("Flavor: " + flavorToppings);
//        tvPerfectFor.setText("Perfect For: " + perfectFor);
//
//        Glide.with(this)
//                .load(imageUrl)
//                .skipMemoryCache(true)
//                .diskCacheStrategy(DiskCacheStrategy.NONE)
//                .centerInside()
//                //.placeholder(R.drawable.loading_screen)
//                .into(ivImage);
//
//        // --- NEW CODE: Initialize RecyclerView for Reviews ---
//        RecyclerView recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
//        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
//        List<Review> reviewList = new ArrayList<>();
//        ReviewAdapter reviewAdapter = new ReviewAdapter(reviewList);
//        recyclerViewReviews.setAdapter(reviewAdapter);
//        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
//
//
//        // Fetch reviews for the current cottage (match by itemName)
//        final String dessertName = tvName.getText().toString();
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
//                            if (dessertName.equals(itemName)) {
//                                String comment = ratingSnapshot.child("comment").getValue(String.class);
//                                String date = ratingSnapshot.child("date").getValue(String.class);
//                                Integer rate = ratingSnapshot.child("rate").getValue(Integer.class);
//                                String user = ratingSnapshot.child("user").getValue(String.class);
//                                String category = ratingSnapshot.child("category").getValue(String.class);
//
//                                // Create a Review object (ensure your Review model is updated accordingly)
//                                //noinspection DataFlowIssue
//                                Review review = new Review(user, rate, comment, date, category, itemName);
//                                reviews.add(review);
//                            }
//                        }
//                    }
//                }
//                reviewAdapter.updateReviews(reviews);
//                updateAverageRating(reviews); // Update the average rating view here
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError databaseError) {
//                Toast.makeText(DessertDetailActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
//            }
//        });
//        // --- END NEW CODE ---
//
//
//        // Add to Cart functionality with default category "Cottage" and modified name.
//        btnAddToCart.setOnClickListener(v -> {
//            // Retrieve the product name from the TextView.
//            String itemName = tvName.getText().toString();
//
//            // Get current cart items.
//            String userId = "";
//            List<CartItem> cartItems = CartManager.getInstance(this, userId).getCartItems();
//            CartItem existingItem = null;
//            for (CartItem ci : cartItems) {
//                if (ci.getName().equals(itemName)) {
//                    existingItem = ci;
//                    break;
//                }
//            }
//
//            if (existingItem != null) {
//                // If the product exists, check its quantity.
//                if (existingItem.getQuantity() >= 10) {
//                    Toast.makeText(DessertDetailActivity.this, "Product limit reached (Max 10 items)", Toast.LENGTH_SHORT).show();
//                } else {
//                    // Increase the product's quantity by one.
//                    existingItem.setQuantity(existingItem.getQuantity() + 1);
//                    CartManager.getInstance(this, userId).persistCart();
//                    Toast.makeText(DessertDetailActivity.this, "Added to Cart Successfully", Toast.LENGTH_SHORT).show();
//                }
//            } else {
//                // If the product doesn't exist in the cart, add it with a quantity of 1.
//                double itemPrice = 0.0;
//                try {
//                    // Parse the price from your original variable.
//                    itemPrice = Double.parseDouble(price);
//                } catch (NumberFormatException e) {
//                    e.printStackTrace();
//                }
//
//                String base64Image = "";
//                if (ivImage.getDrawable() instanceof android.graphics.drawable.BitmapDrawable) {
//                    android.graphics.Bitmap bitmap = ((android.graphics.drawable.BitmapDrawable) ivImage.getDrawable()).getBitmap();
//                    java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
//                    bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, baos);
//                    byte[] imageBytes = baos.toByteArray();
//                    base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
//                }
//
//                // Create a CartItem; ensure your CartItem class has been updated to accept (name, price, category)
//                CartItem item = new CartItem(itemName, itemPrice, "Dessert", base64Image);
//                CartManager.getInstance(this, userId).addItem(item);
//                Toast.makeText(DessertDetailActivity.this, "Added to Cart Successfully", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//}
//
