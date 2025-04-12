package com.example.resort;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Comment extends AppCompatActivity {

    // List to store review items with extra metadata.
    private final ArrayList<ReviewItem> reviewItemsList = new ArrayList<>();
    private EditText recentEditText;

    // Displays the rating prompt.
    private TextView messageTextView;

    // For user's comment.
    private EditText commentEditText;

    // These variables will hold the selected rating value and review details.
    private int selectedRating = 0;
    private String selectedReviewCategory = "";

    // Tracks which item was selected.
    private int selectedReviewIndex = -1;

    // RatingBar references for resetting later.
    private RatingBar ratingBar5, ratingBar4, ratingBar3, ratingBar2, ratingBar1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_comment);


        Button back = findViewById(R.id.back2);
        back.setOnClickListener(v -> finish());

        // Initialize RatingBars.
        ratingBar5 = findViewById(R.id.ratingBar5);
        ratingBar4 = findViewById(R.id.ratingBar4);
        ratingBar3 = findViewById(R.id.ratingBar3);
        ratingBar2 = findViewById(R.id.ratingBar2);
        ratingBar1 = findViewById(R.id.ratingBar1);

        // Initialize Rate buttons.
        Button btnRate5 = findViewById(R.id.rate5);
        Button btnRate4 = findViewById(R.id.rate4);
        Button btnRate3 = findViewById(R.id.rate3);
        Button btnRate2 = findViewById(R.id.rate2);
        Button btnRate1 = findViewById(R.id.rate1);

        int yellowColor = ContextCompat.getColor(this, R.color.yellow);
        int defaultColor = ContextCompat.getColor(this, R.color.grey);

        // Set up rate button click listeners.
        btnRate5.setOnClickListener(v -> {
            ratingBar5.setRating(5);
            ratingBar4.setRating(0);
            ratingBar3.setRating(0);
            ratingBar2.setRating(0);
            ratingBar1.setRating(0);
            ratingBar5.setProgressTintList(ColorStateList.valueOf(yellowColor));
            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
            selectedRating = 5;
        });

        btnRate4.setOnClickListener(v -> {
            ratingBar4.setRating(4);
            ratingBar5.setRating(0);
            ratingBar3.setRating(0);
            ratingBar2.setRating(0);
            ratingBar1.setRating(0);
            ratingBar4.setProgressTintList(ColorStateList.valueOf(yellowColor));
            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
            selectedRating = 4;
        });

        btnRate3.setOnClickListener(v -> {
            ratingBar3.setRating(3);
            ratingBar5.setRating(0);
            ratingBar4.setRating(0);
            ratingBar2.setRating(0);
            ratingBar1.setRating(0);
            ratingBar3.setProgressTintList(ColorStateList.valueOf(yellowColor));
            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
            selectedRating = 3;
        });

        btnRate2.setOnClickListener(v -> {
            ratingBar2.setRating(2);
            ratingBar5.setRating(0);
            ratingBar4.setRating(0);
            ratingBar3.setRating(0);
            ratingBar1.setRating(0);
            ratingBar2.setProgressTintList(ColorStateList.valueOf(yellowColor));
            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
            selectedRating = 2;
        });

        btnRate1.setOnClickListener(v -> {
            ratingBar1.setRating(1);
            ratingBar5.setRating(0);
            ratingBar4.setRating(0);
            ratingBar3.setRating(0);
            ratingBar2.setRating(0);
            ratingBar1.setProgressTintList(ColorStateList.valueOf(yellowColor));
            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
            selectedRating = 1;
        });

        // Initialize Recent field.
        recentEditText = findViewById(R.id.Recent);
        recentEditText.setText("Recent Booking");
        recentEditText.setFocusable(false);

        // Initialize rating prompt and comment field.
        messageTextView = findViewById(R.id.textView2);
        commentEditText = findViewById(R.id.Comment);

        // Fetch review items from Firebase.
        fetchReviewNames();

        // Submit button setup.
        Button submitButton = findViewById(R.id.Submit);
        submitButton.setOnClickListener(v -> submitRating());
    }

    /**
     * Fetch review items from the current user's "MyReview" node.
     * Processes nodes "accommodations", "foodAndDrinks", and "package".
     * For list nodes, each item’s key is stored; for "package", we assume a single object.
     */


    private void fetchReviewNames() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("MyReviewDone");

        myReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                reviewItemsList.clear();
                // Iterate through each review node.
                for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
                    String reviewKey = reviewSnapshot.getKey();
                    // Process "accommodations".
                    DataSnapshot accommodationsSnapshot = reviewSnapshot.child("accommodations");
                    if (accommodationsSnapshot.exists()) {
                        for (DataSnapshot itemSnapshot : accommodationsSnapshot.getChildren()) {
                            String name = itemSnapshot.child("name").getValue(String.class);
                            String category = itemSnapshot.child("category").getValue(String.class);
                            if (name != null && category != null) {
                                String itemKey = itemSnapshot.getKey();
                                reviewItemsList.add(new ReviewItem(name, category, reviewKey, "accommodations", itemKey));
                            }
                        }
                    }
                    // Process "foodAndDrinks".
                    DataSnapshot foodSnapshot = reviewSnapshot.child("foodAndDrinks");
                    if (foodSnapshot.exists()) {
                        for (DataSnapshot itemSnapshot : foodSnapshot.getChildren()) {
                            String name = itemSnapshot.child("name").getValue(String.class);
                            String category = itemSnapshot.child("category").getValue(String.class);
                            if (name != null && category != null) {
                                String itemKey = itemSnapshot.getKey();
                                reviewItemsList.add(new ReviewItem(name, category, reviewKey, "foodAndDrinks", itemKey));
                            }
                        }
                    }
                    // Process "package" as a single object.
                    DataSnapshot packageSnapshot = reviewSnapshot.child("package");
                    if (packageSnapshot.exists()) {
                        String name = packageSnapshot.child("name").getValue(String.class);
                        String category = packageSnapshot.child("category").getValue(String.class);
                        if (name != null && category != null) {
                            // For package, we use "package" as the node type and key.
                            reviewItemsList.add(new ReviewItem(name, category, reviewKey, "package", "package"));
                        }
                    }
                }
                setupRecentField();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Comment.this, "Error fetching data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Set up the Recent field so that tapping it shows a selection dialog with review items.
     * When an item is selected, update the field text, store its index and category,
     * and update the rating prompt accordingly.
     */
    private void setupRecentField() {
        recentEditText.setOnClickListener(v -> {
            if (!reviewItemsList.isEmpty()) {
                CharSequence[] namesArray = new CharSequence[reviewItemsList.size()];
                for (int i = 0; i < reviewItemsList.size(); i++) {
                    namesArray[i] = reviewItemsList.get(i).name;
                }

                new AlertDialog.Builder(Comment.this)
                        .setTitle("Tap to select review")
                        .setItems(namesArray, (dialog, which) -> {
                            ReviewItem selectedItem = reviewItemsList.get(which);
                            recentEditText.setText(selectedItem.name);
                            selectedReviewCategory = selectedItem.category.toLowerCase();
                            selectedReviewIndex = which;

                            // Update rating prompt based on category.
                            switch (selectedReviewCategory) {
                                case "boat":
                                case "cottage":
                                    messageTextView.setText("Please rate our accommodation");
                                    break;
                                case "food":
                                case "dessert":
                                case "beverage":
                                case "alcohol":
                                    messageTextView.setText("Please rate our Food & Drink");
                                    break;
                                case "package":
                                    messageTextView.setText("Please rate our package");
                                    break;
                                default:
                                    messageTextView.setText("Please rate our service");
                                    break;
                            }
                        })
                        .show();
            } else {
                Toast.makeText(Comment.this, "No review items available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Submits the rating record to the current user's "MyRating" node,
     * removes the rated review item permanently from Firebase, and resets the UI.
     */
    private void submitRating() {
        if (recentEditText.getText().toString().equals("Tap to select review") || selectedReviewCategory.isEmpty()) {
            Toast.makeText(this, "Please select a review item", Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedRating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }

        String commentText = commentEditText.getText().toString().trim();
        /// Add condition to check if the comment field is empty
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Please add a comment", Toast.LENGTH_SHORT).show();
            return;
        }

        /// Get the current user's ID and reference to their data
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);

        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ///Inside the onDataChange() method of userRef.child("username").addListenerForSingleValueEvent(...)
                String username = snapshot.getValue(String.class);
                if (username == null || username.isEmpty()) {
                    username = "Unknown User";
                }

                Map<String, Object> ratingRecord = new HashMap<>();
                ratingRecord.put("user", username);
                ratingRecord.put("rate", selectedRating);
                ratingRecord.put("comment", commentText);
                ratingRecord.put("category", selectedReviewCategory);

                // New field: include the item name.
                if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
                    ratingRecord.put("itemName", reviewItemsList.get(selectedReviewIndex).name);
                }

                // Format date with AM/PM in Philippine time.
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
                sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Manila"));
                String currentDate = sdf.format(new Date());
                ratingRecord.put("date", currentDate);
                // Then push the ratingRecord to Firebase as before.

                DatabaseReference ratingRef = FirebaseDatabase.getInstance()
                        .getReference("users")
                        .child(userId)
                        .child("MyRating");
                ratingRef.push().setValue(ratingRecord, (error, ref) -> {
                    if (error == null) {
                        Toast.makeText(Comment.this, "Rating submitted successfully", Toast.LENGTH_SHORT).show();

                        // Remove the rated review item permanently from Firebase.
                        if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
                            ReviewItem item = reviewItemsList.get(selectedReviewIndex);
                            DatabaseReference itemRef;
                            if (item.nodeType.equals("package")) {
                                itemRef = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(userId)
                                        .child("MyReviewDone")
                                        .child(item.parentReviewKey)
                                        .child("package");
                            } else {
                                itemRef = FirebaseDatabase.getInstance()
                                        .getReference("users")
                                        .child(userId)
                                        .child("MyReviewDone")
                                        .child(item.parentReviewKey)
                                        .child(item.nodeType)
                                        .child(item.itemKey);
                            }
                            itemRef.removeValue();
                        }

                        /// Remove the item from the in-memory list.
                        if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
                            reviewItemsList.remove(selectedReviewIndex);
                        }

                        /// Reset the UI fields.
                        recentEditText.setText("Tap to select review");
                        selectedReviewCategory = "";
                        selectedReviewIndex = -1;
                        selectedRating = 0;
                        commentEditText.setText("");

                        /// Reset all RatingBars to zero.
                        ratingBar5.setRating(0);
                        ratingBar4.setRating(0);
                        ratingBar3.setRating(0);
                        ratingBar2.setRating(0);
                        ratingBar1.setRating(0);
                    } else {
                        Toast.makeText(Comment.this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Comment.this, "Failed to retrieve user info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Inner class to hold review item data along with Firebase metadata.
    private static class ReviewItem {
        String name;
        String category;
        String parentReviewKey; // The key of the review node in MyReview.
        String nodeType; // "accommodations", "foodAndDrinks", or "package"
        String itemKey;  // The key of the item (for list nodes) or "package" for a package.

        ReviewItem(String name, String category, String parentReviewKey, String nodeType, String itemKey) {
            this.name = name;
            this.category = category;
            this.parentReviewKey = parentReviewKey;
            this.nodeType = nodeType;
            this.itemKey = itemKey;
        }
    }
}


///No Current User
//package com.example.resort;
//
//import android.content.res.ColorStateList;
//import android.os.Bundle;
//import android.view.Gravity;
//import android.view.View;
//import android.view.WindowManager;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.RatingBar;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.content.ContextCompat;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.text.SimpleDateFormat;
//import java.util.ArrayList;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Locale;
//import java.util.Map;
//
//public class Comment extends AppCompatActivity {
//
//    // List to store review items with extra metadata.
//    private final ArrayList<ReviewItem> reviewItemsList = new ArrayList<>();
//    private EditText recentEditText;
//
//    // Displays the rating prompt.
//    private TextView messageTextView;
//
//    // For user's comment.
//    private EditText commentEditText;
//
//    // These variables will hold the selected rating value and review details.
//    private int selectedRating = 0;
//    private String selectedReviewCategory = "";
//
//    // Tracks which item was selected.
//    private int selectedReviewIndex = -1;
//
//    // RatingBar references for resetting later.
//    private RatingBar ratingBar5, ratingBar4, ratingBar3, ratingBar2, ratingBar1;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_comment);
//
//
//        Button back = findViewById(R.id.back2);
//        back.setOnClickListener(v -> finish());
//
//        // Initialize RatingBars.
//        ratingBar5 = findViewById(R.id.ratingBar5);
//        ratingBar4 = findViewById(R.id.ratingBar4);
//        ratingBar3 = findViewById(R.id.ratingBar3);
//        ratingBar2 = findViewById(R.id.ratingBar2);
//        ratingBar1 = findViewById(R.id.ratingBar1);
//
//        // Initialize Rate buttons.
//        Button btnRate5 = findViewById(R.id.rate5);
//        Button btnRate4 = findViewById(R.id.rate4);
//        Button btnRate3 = findViewById(R.id.rate3);
//        Button btnRate2 = findViewById(R.id.rate2);
//        Button btnRate1 = findViewById(R.id.rate1);
//
//        int yellowColor = ContextCompat.getColor(this, R.color.yellow);
//        int defaultColor = ContextCompat.getColor(this, R.color.grey);
//
//        // Set up rate button click listeners.
//        btnRate5.setOnClickListener(v -> {
//            ratingBar5.setRating(5);
//            ratingBar4.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 5;
//        });
//
//        btnRate4.setOnClickListener(v -> {
//            ratingBar4.setRating(4);
//            ratingBar5.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 4;
//        });
//
//        btnRate3.setOnClickListener(v -> {
//            ratingBar3.setRating(3);
//            ratingBar5.setRating(0);
//            ratingBar4.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 3;
//        });
//
//        btnRate2.setOnClickListener(v -> {
//            ratingBar2.setRating(2);
//            ratingBar5.setRating(0);
//            ratingBar4.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar1.setRating(0);
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 2;
//        });
//
//        btnRate1.setOnClickListener(v -> {
//            ratingBar1.setRating(1);
//            ratingBar5.setRating(0);
//            ratingBar4.setRating(0);
//            ratingBar3.setRating(0);
//            ratingBar2.setRating(0);
//            ratingBar1.setProgressTintList(ColorStateList.valueOf(yellowColor));
//            ratingBar5.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar4.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar3.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            ratingBar2.setProgressTintList(ColorStateList.valueOf(defaultColor));
//            selectedRating = 1;
//        });
//
//        // Initialize Recent field.
//        recentEditText = findViewById(R.id.Recent);
//        recentEditText.setText("Recent Booking");
//        recentEditText.setFocusable(false);
//
//        // Initialize rating prompt and comment field.
//        messageTextView = findViewById(R.id.textView2);
//        commentEditText = findViewById(R.id.Comment);
//
//        // Fetch review items from Firebase.
//        fetchReviewNames();
//
//        // Submit button setup.
//        Button submitButton = findViewById(R.id.Submit);
//        submitButton.setOnClickListener(v -> submitRating());
//    }
//
//    /**
//     * Fetch review items from the current user's "MyReview" node.
//     * Processes nodes "accommodations", "foodAndDrinks", and "package".
//     * For list nodes, each item’s key is stored; for "package", we assume a single object.
//     */
//
//
//    private void fetchReviewNames() {
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference myReviewRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyReviewDone");
//
//        myReviewRef.addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                reviewItemsList.clear();
//                // Iterate through each review node.
//                for (DataSnapshot reviewSnapshot : snapshot.getChildren()) {
//                    String reviewKey = reviewSnapshot.getKey();
//                    // Process "accommodations".
//                    DataSnapshot accommodationsSnapshot = reviewSnapshot.child("accommodations");
//                    if (accommodationsSnapshot.exists()) {
//                        for (DataSnapshot itemSnapshot : accommodationsSnapshot.getChildren()) {
//                            String name = itemSnapshot.child("name").getValue(String.class);
//                            String category = itemSnapshot.child("category").getValue(String.class);
//                            if (name != null && category != null) {
//                                String itemKey = itemSnapshot.getKey();
//                                reviewItemsList.add(new ReviewItem(name, category, reviewKey, "accommodations", itemKey));
//                            }
//                        }
//                    }
//                    // Process "foodAndDrinks".
//                    DataSnapshot foodSnapshot = reviewSnapshot.child("foodAndDrinks");
//                    if (foodSnapshot.exists()) {
//                        for (DataSnapshot itemSnapshot : foodSnapshot.getChildren()) {
//                            String name = itemSnapshot.child("name").getValue(String.class);
//                            String category = itemSnapshot.child("category").getValue(String.class);
//                            if (name != null && category != null) {
//                                String itemKey = itemSnapshot.getKey();
//                                reviewItemsList.add(new ReviewItem(name, category, reviewKey, "foodAndDrinks", itemKey));
//                            }
//                        }
//                    }
//                    // Process "package" as a single object.
//                    DataSnapshot packageSnapshot = reviewSnapshot.child("package");
//                    if (packageSnapshot.exists()) {
//                        String name = packageSnapshot.child("name").getValue(String.class);
//                        String category = packageSnapshot.child("category").getValue(String.class);
//                        if (name != null && category != null) {
//                            // For package, we use "package" as the node type and key.
//                            reviewItemsList.add(new ReviewItem(name, category, reviewKey, "package", "package"));
//                        }
//                    }
//                }
//                setupRecentField();
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                Toast.makeText(Comment.this, "Error fetching data", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    /**
//     * Set up the Recent field so that tapping it shows a selection dialog with review items.
//     * When an item is selected, update the field text, store its index and category,
//     * and update the rating prompt accordingly.
//     */
//    private void setupRecentField() {
//        recentEditText.setOnClickListener(v -> {
//            if (!reviewItemsList.isEmpty()) {
//                CharSequence[] namesArray = new CharSequence[reviewItemsList.size()];
//                for (int i = 0; i < reviewItemsList.size(); i++) {
//                    namesArray[i] = reviewItemsList.get(i).name;
//                }
//
//                new AlertDialog.Builder(Comment.this)
//                        .setTitle("Tap to select review")
//                        .setItems(namesArray, (dialog, which) -> {
//                            ReviewItem selectedItem = reviewItemsList.get(which);
//                            recentEditText.setText(selectedItem.name);
//                            selectedReviewCategory = selectedItem.category.toLowerCase();
//                            selectedReviewIndex = which;
//
//                            // Update rating prompt based on category.
//                            switch (selectedReviewCategory) {
//                                case "boat":
//                                case "cottage":
//                                    messageTextView.setText("Please rate our accommodation");
//                                    break;
//                                case "food":
//                                case "dessert":
//                                case "beverage":
//                                case "alcohol":
//                                    messageTextView.setText("Please rate our Food & Drink");
//                                    break;
//                                case "package":
//                                    messageTextView.setText("Please rate our package");
//                                    break;
//                                default:
//                                    messageTextView.setText("Please rate our service");
//                                    break;
//                            }
//                        })
//                        .show();
//            } else {
//                Toast.makeText(Comment.this, "No review items available", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    /**
//     * Submits the rating record to the current user's "MyRating" node,
//     * removes the rated review item permanently from Firebase, and resets the UI.
//     */
//    private void submitRating() {
//        if (recentEditText.getText().toString().equals("Tap to select review") || selectedReviewCategory.isEmpty()) {
//            Toast.makeText(this, "Please select a review item", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        if (selectedRating == 0) {
//            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        String commentText = commentEditText.getText().toString().trim();
//        /// Add condition to check if the comment field is empty
//        if (commentText.isEmpty()) {
//            Toast.makeText(this, "Please add a comment", Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        /// Get the current user's ID and reference to their data
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(userId);
//
//        userRef.child("username").addListenerForSingleValueEvent(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                ///Inside the onDataChange() method of userRef.child("username").addListenerForSingleValueEvent(...)
//                String username = snapshot.getValue(String.class);
//                if (username == null || username.isEmpty()) {
//                    username = "Unknown User";
//                }
//
//                Map<String, Object> ratingRecord = new HashMap<>();
//                ratingRecord.put("user", username);
//                ratingRecord.put("rate", selectedRating);
//                ratingRecord.put("comment", commentText);
//                ratingRecord.put("category", selectedReviewCategory);
//
//                // New field: include the item name.
//                if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
//                    ratingRecord.put("itemName", reviewItemsList.get(selectedReviewIndex).name);
//                }
//
//                // Format date with AM/PM in Philippine time.
//                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.getDefault());
//                sdf.setTimeZone(java.util.TimeZone.getTimeZone("Asia/Manila"));
//                String currentDate = sdf.format(new Date());
//                ratingRecord.put("date", currentDate);
//                // Then push the ratingRecord to Firebase as before.
//
//                DatabaseReference ratingRef = FirebaseDatabase.getInstance()
//                        .getReference("users")
//                        .child(userId)
//                        .child("MyRating");
//                ratingRef.push().setValue(ratingRecord, (error, ref) -> {
//                    if (error == null) {
//                        Toast.makeText(Comment.this, "Rating submitted successfully", Toast.LENGTH_SHORT).show();
//
//                        // Remove the rated review item permanently from Firebase.
//                        if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
//                            ReviewItem item = reviewItemsList.get(selectedReviewIndex);
//                            DatabaseReference itemRef;
//                            if (item.nodeType.equals("package")) {
//                                itemRef = FirebaseDatabase.getInstance()
//                                        .getReference("users")
//                                        .child(userId)
//                                        .child("MyReviewDone")
//                                        .child(item.parentReviewKey)
//                                        .child("package");
//                            } else {
//                                itemRef = FirebaseDatabase.getInstance()
//                                        .getReference("users")
//                                        .child(userId)
//                                        .child("MyReviewDone")
//                                        .child(item.parentReviewKey)
//                                        .child(item.nodeType)
//                                        .child(item.itemKey);
//                            }
//                            itemRef.removeValue();
//                        }
//
//                        /// Remove the item from the in-memory list.
//                        if (selectedReviewIndex != -1 && selectedReviewIndex < reviewItemsList.size()) {
//                            reviewItemsList.remove(selectedReviewIndex);
//                        }
//
//                        /// Reset the UI fields.
//                        recentEditText.setText("Tap to select review");
//                        selectedReviewCategory = "";
//                        selectedReviewIndex = -1;
//                        selectedRating = 0;
//                        commentEditText.setText("");
//
//                        /// Reset all RatingBars to zero.
//                        ratingBar5.setRating(0);
//                        ratingBar4.setRating(0);
//                        ratingBar3.setRating(0);
//                        ratingBar2.setRating(0);
//                        ratingBar1.setRating(0);
//                    } else {
//                        Toast.makeText(Comment.this, "Failed to submit rating", Toast.LENGTH_SHORT).show();
//                    }
//                });
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                Toast.makeText(Comment.this, "Failed to retrieve user info", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    //Inner class to hold review item data along with Firebase metadata.
//    private static class ReviewItem {
//        String name;
//        String category;
//        String parentReviewKey; // The key of the review node in MyReview.
//        String nodeType; // "accommodations", "foodAndDrinks", or "package"
//        String itemKey;  // The key of the item (for list nodes) or "package" for a package.
//
//        ReviewItem(String name, String category, String parentReviewKey, String nodeType, String itemKey) {
//            this.name = name;
//            this.category = category;
//            this.parentReviewKey = parentReviewKey;
//            this.nodeType = nodeType;
//            this.itemKey = itemKey;
//        }
//    }
//}
//
