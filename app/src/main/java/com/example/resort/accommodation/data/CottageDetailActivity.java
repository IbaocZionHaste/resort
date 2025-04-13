package com.example.resort.accommodation.data;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout; // For dot indicators
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

@SuppressLint("SetTextI18n")
public class CottageDetailActivity extends AppCompatActivity {

    // Data from the Intent
    private String rawPrice;
    private String availableDate;

    // Swipe images list: holds either Integer (resource ID), Bitmap (from album), or String (Firebase Storage URL)
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

        // Optional: animate rating update.
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

    // ----- onCreate() -----
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_cottage_detail);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Retrieve intent extras
        Intent intent = getIntent();
        String productId = intent.getStringExtra("productId");
        String name = intent.getStringExtra("accommodationName");
        String description = intent.getStringExtra("accommodationDesc");
        String capacity = intent.getStringExtra("accommodationCapacity");
        String status = intent.getStringExtra("accommodationStat");
        String design = intent.getStringExtra("accommodationDesign");
        String location = intent.getStringExtra("accommodationLocation");
        String amenities = intent.getStringExtra("accommodationAmenities");
        rawPrice = intent.getStringExtra("accommodationPrice");
        availableDate = intent.getStringExtra("accommodationAvailableDate");

        // Find views in the layout
        TextView tvName = findViewById(R.id.tvCottageName);
        TextView tvDescription = findViewById(R.id.tvCottageDescription);
        TextView tvCapacity = findViewById(R.id.tvCottageCapacity);
        TextView tvDesign = findViewById(R.id.tvCottageDesign);
        TextView tvLocation = findViewById(R.id.tvCottageLocation);
        TextView tvStatus = findViewById(R.id.tvCottageStatus);
        TextView tvAmenities = findViewById(R.id.tvCottageAmenities);
        TextView tvPrice = findViewById(R.id.tvCottagePrice);
        TextView tvAvailableDate = findViewById(R.id.tvAvailableDate);
        ivImageSwipe = findViewById(R.id.ivCottageImage);
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

        // Favorite handling using accommodationName as key
        final String favoriteKey = name;
        ImageView heartIcon = findViewById(R.id.heart);
        final boolean[] isFavorite = {false};

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            DatabaseReference userFavoritesRef = FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(currentUser.getUid())
                    .child("favorites");

            userFavoritesRef.child(favoriteKey).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        isFavorite[0] = true;
                        heartIcon.setColorFilter(getResources().getColor(R.color.red));
                    } else {
                        isFavorite[0] = false;
                        heartIcon.clearColorFilter();
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError error) { }
            });

            heartIcon.setOnClickListener(v -> {
                if (isFavorite[0]) {
                    userFavoritesRef.child(favoriteKey).removeValue();
                    heartIcon.clearColorFilter();
                    isFavorite[0] = false;
                    Toast.makeText(CottageDetailActivity.this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
                } else {
                    userFavoritesRef.child(favoriteKey).setValue(true);
                    heartIcon.setColorFilter(getResources().getColor(R.color.red));
                    isFavorite[0] = true;
                    Toast.makeText(CottageDetailActivity.this, "Added to Favorites", Toast.LENGTH_SHORT).show();
                }
            });
        }

        // Update status and available date UI
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

            tvAvailableDate.setText("");
            tvAvailableDate.setVisibility(View.GONE);

            if (productId != null && !productId.trim().isEmpty()) {
                DatabaseReference productRef = FirebaseDatabase.getInstance()
                        .getReference("products").child("Cottage").child(productId);
                productRef.child("status").setValue("Available");
            }
        } else if ("Unavailable".equalsIgnoreCase(status)) {
            tvStatus.setText("Sold Out");
            tvStatus.setTextColor(getResources().getColor(R.color.red));
            btnAddToCart.setEnabled(false);
            btnAddToCart.setText("Sold Out");

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
                    cal.add(Calendar.DATE, 1);
                    final String adjustedDate = sdf.format(cal.getTime());
                    final Date availableDateObj = sdf.parse(adjustedDate);

                    final Handler handler = new Handler();
                    Runnable runnable = new Runnable() {
                        @Override
                        public void run() {
                            Date currentDateObj = new Date();
                            if (currentDateObj.after(availableDateObj) || sdf.format(currentDateObj).equals(adjustedDate)) {
                                if (productId != null && !productId.trim().isEmpty()) {
                                    DatabaseReference productRef = FirebaseDatabase.getInstance()
                                            .getReference("products")
                                            .child("Cottage")
                                            .child(productId);
                                    productRef.child("status").setValue("Available");
                                    productRef.child("availableDate").removeValue();
                                }
                                tvAvailableDate.setText("");
                                tvAvailableDate.setVisibility(View.GONE);
                            } else {
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

        if (rawPrice != null && !rawPrice.trim().isEmpty() && capacity != null && !capacity.trim().isEmpty()) {
            tvPrice.setText("Price: ₱" + rawPrice + " / " + capacity + "Pax");
        } else if (rawPrice != null && !rawPrice.trim().isEmpty()) {
            tvPrice.setText("Price: ₱" + rawPrice);
        } else {
            tvPrice.setText("Price: ₱0.00");
        }

        tvName.setText(name);
        tvDescription.setText(description);
        tvAmenities.setText("Amenities: " + amenities);
        tvCapacity.setText("Capacity: " + capacity);
        tvDesign.setText("Design: " + design);
        tvLocation.setText("Location: " + location);

        RecyclerView recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
        List<Review> reviewList = new ArrayList<>();
        ReviewAdapter reviewAdapter = new ReviewAdapter(reviewList);
        recyclerViewReviews.setAdapter(reviewAdapter);
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        final String cottageName = tvName.getText().toString();
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
                            if (cottageName.equals(itemName)) {
                                String comment = ratingSnapshot.child("comment").getValue(String.class);
                                String date = ratingSnapshot.child("date").getValue(String.class);
                                Integer rate = ratingSnapshot.child("rate").getValue(Integer.class);
                                String user = ratingSnapshot.child("user").getValue(String.class);
                                String category = ratingSnapshot.child("category").getValue(String.class);
                                Review review = new Review(user, rate, comment, date, category, itemName);
                                reviews.add(review);
                            }
                        }
                    }
                }
                reviewAdapter.updateReviews(reviews);
                updateAverageRating(reviews);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(CottageDetailActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
            }
        });

        btnAddToCart.setOnClickListener(v -> {
            FirebaseUser currentUserForCart = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUserForCart == null) {
                Toast.makeText(CottageDetailActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
                return;
            }
            String uid = currentUserForCart.getUid();

            boolean productExists = false;
            for (CartItem cartItem : CartManager.getInstance(CottageDetailActivity.this, uid).getCartItems()) {
                if (cartItem.getName().equals(name)) {
                    productExists = true;
                    break;
                }
            }
            if (productExists) {
                Toast.makeText(CottageDetailActivity.this, "Sorry, only 1 item of this product can be booked.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!"Available".equalsIgnoreCase(status)) {
                String msg = "This item is sold out";
                if (availableDate != null && !availableDate.trim().isEmpty()) {
                    msg += " until " + availableDate;
                }
                Toast.makeText(CottageDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
                return;
            }

            double itemPrice = 0.0;
            try {
                itemPrice = Double.parseDouble(rawPrice.trim());
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

            int itemCapacity = 0;
            try {
                itemCapacity = Integer.parseInt(tvCapacity.getText().toString().replace("Capacity: ", "").trim());
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

            CartItem item = new CartItem(name, itemPrice, "Cottage", itemCapacity, photoForCart);
            CartManager.getInstance(CottageDetailActivity.this, uid).addItem(item);
            Toast.makeText(CottageDetailActivity.this, "Added to Cart Successfully", Toast.LENGTH_SHORT).show();
        });

        // ----- Fetch Album Data Asynchronously -----
        fetchAlbumData(tvName.getText().toString());
    }
}






///This new data after the gallery is done Base 64
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
//import android.widget.LinearLayout; // For dot indicators
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
//@SuppressLint("SetTextI18n")
//public class CottageDetailActivity extends AppCompatActivity {
//
//    // Data from the Intent
//    private String rawPrice;
//    private String availableDate;
//
//    // Swipe images list: holds either Integer (resource ID) or Bitmap (from album)
//    private List<Object> swipeImages;
//    private int currentImageIndex = 0;
//    private ImageView ivImageSwipe; // This is your main image view (ivCottageImage)
//
//    // Dot indicators container (ensure your XML has a LinearLayout with id "llDots")
//    private LinearLayout llDots;
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
//        // Optional: animate rating update.
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
//    // ----- Display the current image from swipeImages -----
//    private void displayCurrentImage() {
//        Object item = swipeImages.get(currentImageIndex);
//        if (item instanceof Integer) {
//            ivImageSwipe.setImageResource((Integer) item);
//        } else if (item instanceof Bitmap) {
//            ivImageSwipe.setImageBitmap((Bitmap) item);
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
//    // ----- Asynchronous Album Data Fetching -----
//    // Fetches album data from Firebase. If an album's productName matches the cottage name,
//    // then its photo1, photo2, and photo3 (Base64 strings) are decoded to Bitmaps and replace the default images.
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
//                        // Remove prefix if present (e.g., "data:image/png;base64,")
//                        if (photo1Str != null && photo1Str.contains(",")) {
//                            photo1Str = photo1Str.substring(photo1Str.indexOf(",") + 1);
//                        }
//                        if (photo2Str != null && photo2Str.contains(",")) {
//                            photo2Str = photo2Str.substring(photo2Str.indexOf(",") + 1);
//                        }
//                        if (photo3Str != null && photo3Str.contains(",")) {
//                            photo3Str = photo3Str.substring(photo3Str.indexOf(",") + 1);
//                        }
//                        Bitmap bitmap1 = decodeBase64(photo1Str);
//                        Bitmap bitmap2 = decodeBase64(photo2Str);
//                        Bitmap bitmap3 = decodeBase64(photo3Str);
//                        // Replace default images with album Bitmaps
//                        swipeImages.clear();
//                        swipeImages.add(bitmap1);
//                        swipeImages.add(bitmap2);
//                        swipeImages.add(bitmap3);
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
//    // Decode a Base64 string to a Bitmap.
//    private Bitmap decodeBase64(String input) {
//        try {
//            byte[] decodedBytes = Base64.decode(input, Base64.DEFAULT);
//            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
//        } catch (Exception e) {
//            e.printStackTrace();
//            return null;
//        }
//    }
//
//
//    // ----- onCreate() -----
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_cottage_detail);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Retrieve intent extras
//        Intent intent = getIntent();
//        String productId = intent.getStringExtra("productId");
//        String name = intent.getStringExtra("accommodationName");
//        String description = intent.getStringExtra("accommodationDesc");
//        String capacity = intent.getStringExtra("accommodationCapacity");
//        String status = intent.getStringExtra("accommodationStat");
//        String design = intent.getStringExtra("accommodationDesign");
//        String location = intent.getStringExtra("accommodationLocation");
//        String amenities = intent.getStringExtra("accommodationAmenities");
//        rawPrice = intent.getStringExtra("accommodationPrice");
////        String imageUrl = intent.getStringExtra("accommodationImage");
//        availableDate = intent.getStringExtra("accommodationAvailableDate");
//
//
//        // Find views in the layout
//        TextView tvName = findViewById(R.id.tvCottageName);
//        TextView tvDescription = findViewById(R.id.tvCottageDescription);
//        TextView tvCapacity = findViewById(R.id.tvCottageCapacity);
//        TextView tvDesign = findViewById(R.id.tvCottageDesign);
//        TextView tvLocation = findViewById(R.id.tvCottageLocation);
//        TextView tvStatus = findViewById(R.id.tvCottageStatus);
//        TextView tvAmenities = findViewById(R.id.tvCottageAmenities);
//        TextView tvPrice = findViewById(R.id.tvCottagePrice);
//        TextView tvAvailableDate = findViewById(R.id.tvAvailableDate);
//        ivImageSwipe = findViewById(R.id.ivCottageImage);
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
//        btnBack.setOnClickListener(v -> onBackPressed());
//
//        // Favorite handling using accommodationName as key
//        final String favoriteKey = name;
//        ImageView heartIcon = findViewById(R.id.heart);
//        final boolean[] isFavorite = {false};
//
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            DatabaseReference userFavoritesRef = FirebaseDatabase.getInstance()
//                    .getReference("users")
//                    .child(currentUser.getUid())
//                    .child("favorites");
//
//            userFavoritesRef.child(favoriteKey).addListenerForSingleValueEvent(new ValueEventListener() {
//                @Override
//                public void onDataChange(@NonNull DataSnapshot snapshot) {
//                    if (snapshot.exists()) {
//                        isFavorite[0] = true;
//                        heartIcon.setColorFilter(getResources().getColor(R.color.red));
//                    } else {
//                        isFavorite[0] = false;
//                        heartIcon.clearColorFilter();
//                    }
//                }
//                @Override
//                public void onCancelled(@NonNull DatabaseError error) { }
//            });
//
//            heartIcon.setOnClickListener(v -> {
//                if (isFavorite[0]) {
//                    userFavoritesRef.child(favoriteKey).removeValue();
//                    heartIcon.clearColorFilter();
//                    isFavorite[0] = false;
//                    Toast.makeText(CottageDetailActivity.this, "Removed from Favorites", Toast.LENGTH_SHORT).show();
//                } else {
//                    userFavoritesRef.child(favoriteKey).setValue(true);
//                    heartIcon.setColorFilter(getResources().getColor(R.color.red));
//                    isFavorite[0] = true;
//                    Toast.makeText(CottageDetailActivity.this, "Added to Favorites", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }
//
//        // Update status and available date UI
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
//            tvAvailableDate.setText("");
//            tvAvailableDate.setVisibility(View.GONE);
//
//            if (productId != null && !productId.trim().isEmpty()) {
//                DatabaseReference productRef = FirebaseDatabase.getInstance()
//                        .getReference("products").child("Cottage").child(productId);
//                productRef.child("status").setValue("Available");
//            }
//        } else if ("Unavailable".equalsIgnoreCase(status)) {
//            tvStatus.setText("Sold Out");
//            tvStatus.setTextColor(getResources().getColor(R.color.red));
//            btnAddToCart.setEnabled(false);
//            btnAddToCart.setText("Sold Out");
//
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
//                    cal.add(Calendar.DATE, 1);
//                    final String adjustedDate = sdf.format(cal.getTime());
//                    final Date availableDateObj = sdf.parse(adjustedDate);
//
//                    final Handler handler = new Handler();
//                    Runnable runnable = new Runnable() {
//                        @Override
//                        public void run() {
//                            Date currentDateObj = new Date();
//                            if (currentDateObj.after(availableDateObj) || sdf.format(currentDateObj).equals(adjustedDate)) {
//                                if (productId != null && !productId.trim().isEmpty()) {
//                                    DatabaseReference productRef = FirebaseDatabase.getInstance()
//                                            .getReference("products")
//                                            .child("Cottage")
//                                            .child(productId);
//                                    productRef.child("status").setValue("Available");
//                                    productRef.child("availableDate").removeValue();
//                                }
//                                tvAvailableDate.setText("");
//                                tvAvailableDate.setVisibility(View.GONE);
//                            } else {
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
//        if (rawPrice != null && !rawPrice.trim().isEmpty() && capacity != null && !capacity.trim().isEmpty()) {
//            tvPrice.setText("Price: ₱" + rawPrice + " / " + capacity + "Pax");
//        } else if (rawPrice != null && !rawPrice.trim().isEmpty()) {
//            tvPrice.setText("Price: ₱" + rawPrice);
//        } else {
//            tvPrice.setText("Price: ₱0.00");
//        }
//
//        tvName.setText(name);
//        tvDescription.setText(description);
//        tvAmenities.setText("Amenities: " + amenities);
//        tvCapacity.setText("Capacity: " + capacity);
//        tvDesign.setText("Design: " + design);
//        tvLocation.setText("Location: " + location);
//
//
//        RecyclerView recyclerViewReviews = findViewById(R.id.recyclerViewReviews);
//        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this));
//        List<Review> reviewList = new ArrayList<>();
//        ReviewAdapter reviewAdapter = new ReviewAdapter(reviewList);
//        recyclerViewReviews.setAdapter(reviewAdapter);
//        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
//
//        final String cottageName = tvName.getText().toString();
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
//                            if (cottageName.equals(itemName)) {
//                                String comment = ratingSnapshot.child("comment").getValue(String.class);
//                                String date = ratingSnapshot.child("date").getValue(String.class);
//                                Integer rate = ratingSnapshot.child("rate").getValue(Integer.class);
//                                String user = ratingSnapshot.child("user").getValue(String.class);
//                                String category = ratingSnapshot.child("category").getValue(String.class);
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
//                Toast.makeText(CottageDetailActivity.this, "Failed to load reviews", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        btnAddToCart.setOnClickListener(v -> {
//            FirebaseUser currentUserForCart = FirebaseAuth.getInstance().getCurrentUser();
//            if (currentUserForCart == null) {
//                Toast.makeText(CottageDetailActivity.this, "User not logged in", Toast.LENGTH_SHORT).show();
//                return;
//            }
//            String uid = currentUserForCart.getUid();
//
//            boolean productExists = false;
//            for (CartItem cartItem : CartManager.getInstance(CottageDetailActivity.this, uid).getCartItems()) {
//                if (cartItem.getName().equals(name)) {
//                    productExists = true;
//                    break;
//                }
//            }
//            if (productExists) {
//                Toast.makeText(CottageDetailActivity.this, "Sorry, only 1 item of this product can be booked.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            if (!"Available".equalsIgnoreCase(status)) {
//                String msg = "This item is sold out";
//                if (availableDate != null && !availableDate.trim().isEmpty()) {
//                    msg += " until " + availableDate;
//                }
//                Toast.makeText(CottageDetailActivity.this, msg, Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            double itemPrice = 0.0;
//            try {
//                itemPrice = Double.parseDouble(rawPrice.trim());
//            } catch (NumberFormatException e) {
//                e.printStackTrace();
//            }
//
//            int itemCapacity = 0;
//            try {
//                itemCapacity = Integer.parseInt(tvCapacity.getText().toString().replace("Capacity: ", "").trim());
//            } catch (NumberFormatException e) {
//                e.printStackTrace();
//            }
//
//            // --- Revised: Always use the first image (photo1) for the cart ---
//            Bitmap cartBitmap = null;
//            Object firstImage = swipeImages.get(0);
//            if (firstImage instanceof Bitmap) {
//                cartBitmap = (Bitmap) firstImage;
//            } else if (firstImage instanceof Integer) {
//                cartBitmap = BitmapFactory.decodeResource(getResources(), (Integer) firstImage);
//            }
//
//            String base64Image = "";
//            if (cartBitmap != null) {
//                ByteArrayOutputStream baos = new ByteArrayOutputStream();
//                cartBitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
//                byte[] imageBytes = baos.toByteArray();
//                base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);
//            }
//            // --- End revised section ---
//
//            CartItem item = new CartItem(name, itemPrice, "Cottage", itemCapacity, base64Image);
//            CartManager.getInstance(CottageDetailActivity.this, uid).addItem(item);
//            Toast.makeText(CottageDetailActivity.this, "Added to Cart Successfully", Toast.LENGTH_SHORT).show();
//        });
//
//
//        // ----- Fetch Album Data Asynchronously -----
//        fetchAlbumData(tvName.getText().toString());
//    }
//}

