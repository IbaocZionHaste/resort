package com.example.resort.accommodation.data;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper1 {

    private final DatabaseReference databaseReference;

    public DatabaseHelper1() {
        // Initialize Firebase Database reference
        databaseReference = FirebaseDatabase.getInstance().getReference("products");
    }

    // Firebase callback interface
    public interface FirebaseCallback {
        void onCallback(List<Accommodation> products);
        void onError(String errorMessage);
    }

    // Fetch products with category filtering
    public void fetchProducts(String category, final FirebaseCallback callback) {
        Query query = databaseReference; // Default reference to products node

        // Apply category filter if specified
        if (category != null && !category.isEmpty()) {
            query = databaseReference.orderByChild("category").equalTo(category);
        }

        // Add a value event listener to fetch the data
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Accommodation> products = new ArrayList<>();
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    Accommodation product = productSnapshot.getValue(Accommodation.class);
                    if (product != null) {
                        products.add(product);
                    }
                }
                callback.onCallback(products); // Pass the filtered list to the callback
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Database Error: " + error.getMessage());
            }
        });
    }

    // Search products by name (case-insensitive) within a specific category
    public void searchProductsByName(String searchTerm, String category, final FirebaseCallback callback) {
        // Retrieve all products (we'll filter client-side)
        Query query = databaseReference;
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Accommodation> products = new ArrayList<>();
                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
                    Accommodation product = productSnapshot.getValue(Accommodation.class);
                    if (product != null && product.getName() != null && product.getCategory() != null) {
                        // Filter by name (case-insensitive) and category match
                        if (product.getName().toLowerCase().contains(searchTerm.toLowerCase())
                                && product.getCategory().equalsIgnoreCase(category)) {
                            products.add(product);
                        }
                    }
                }
                callback.onCallback(products); // Return the filtered list based on the search term and category
            }

            @Override
            public void onCancelled(DatabaseError error) {
                callback.onError("Database Error: " + error.getMessage());
            }
        });
    }
}


///Fix Current
//package com.example.resort.accommodation.data;
//
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.Query;
//import com.google.firebase.database.ValueEventListener;
//import java.util.ArrayList;
//import java.util.List;
//
//public class DatabaseHelper1 {
//
//    private final DatabaseReference databaseReference;
//
//    public DatabaseHelper1() {
//        // Initialize Firebase Database reference
//        databaseReference = FirebaseDatabase.getInstance().getReference("products");
//    }
//
//    // Firebase callback interface
//    public interface FirebaseCallback {
//        void onCallback(List<Accommodation> products);
//        void onError(String errorMessage);
//    }
//
//    // Fetch products with category filtering
//    public void fetchProducts(String category, final FirebaseCallback callback) {
//        Query query = databaseReference; // Default reference to products node
//
//        // Apply category filter if specified
//        if (category != null && !category.isEmpty()) {
//            query = databaseReference.orderByChild("category").equalTo(category);
//        }
//
//        // Add a value event listener to fetch the data
//        query.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                List<Accommodation> products = new ArrayList<>();
//                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
//                    Accommodation product = productSnapshot.getValue(Accommodation.class);
//                    if (product != null) {
//                        products.add(product);
//                    }
//                }
//                callback.onCallback(products); // Pass the filtered list to the callback
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                callback.onError("Database Error: " + error.getMessage());
//            }
//        });
//    }
//
//    // Search products by name (case-insensitive) within a specific category
//    public void searchProductsByName(String searchTerm, String category, final FirebaseCallback callback) {
//        // Retrieve all products (we'll filter client-side)
//        Query query = databaseReference;
//        query.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                List<Accommodation> products = new ArrayList<>();
//                for (DataSnapshot productSnapshot : snapshot.getChildren()) {
//                    Accommodation product = productSnapshot.getValue(Accommodation.class);
//                    if (product != null && product.getName() != null && product.getCategory() != null) {
//                        // Filter by name (case-insensitive) and category match
//                        if (product.getName().toLowerCase().contains(searchTerm.toLowerCase())
//                                && product.getCategory().equalsIgnoreCase(category)) {
//                            products.add(product);
//                        }
//                    }
//                }
//                callback.onCallback(products); // Return the filtered list based on the search term and category
//            }
//
//            @Override
//            public void onCancelled(DatabaseError error) {
//                callback.onError("Database Error: " + error.getMessage());
//            }
//        });
//    }
//}
//
