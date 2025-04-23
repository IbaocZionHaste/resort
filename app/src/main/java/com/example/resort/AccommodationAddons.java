package com.example.resort;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.accommodation.data.Accommodation;
import com.example.resort.accommodation.data.DatabaseHelper1;
import com.example.resort.accommodation.data.ProductsAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Objects;

public class AccommodationAddons extends AppCompatActivity {

    private LinearLayout selectedLayout;
    private RecyclerView recyclerView;
    private ProductsAdapter productsAdapter;
    private ProgressBar progressBar;
    private EditText searchEditText;

    /// Track the currently selected category
    private String currentCategory = "Food";

    private DatabaseReference userRef;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accommodation_addons);

        progressBar       = findViewById(R.id.progressBar);
        searchEditText    = findViewById(R.id.searchEditText);
        recyclerView      = findViewById(R.id.recycleView);
        ImageView backBtn = findViewById(R.id.btn);
        ConstraintLayout mainLayout = findViewById(R.id.mainLayout);

        progressBar.setVisibility(View.GONE);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setNestedScrollingEnabled(false);

        // Fetch default "Food" products
        fetchProductsByCategory("Food");

        // Hide keyboard when tapping outside
        mainLayout.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                hideKeyboard();
                searchEditText.clearFocus();
            }
            return false;
        });

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) hideKeyboard();
        });

        // Search filter
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) { }
            @Override public void afterTextChanged(Editable e) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String term = s.toString().trim();
                if (!term.isEmpty()) searchProducts(term);
                else             fetchProductsByCategory(currentCategory);
            }
        });


        backBtn.setOnClickListener(v -> onBackPressed());

        // Firebase user check
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, Login.class));
            finish();
            return;
        }
        userRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(currentUser.getUid());

        // Banned-user dialog
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                String status = snap.child("status").getValue(String.class);
                if ("banned".equalsIgnoreCase(status)) {
                    new AlertDialog.Builder(AccommodationAddons.this)
                            .setTitle("Account Suspended")
                            .setMessage("You are banned because of suspicious activity.")
                            .setPositiveButton("OK", (dlg, which) -> {
                                FirebaseAuth.getInstance().signOut();
                                Intent i = new Intent(AccommodationAddons.this, Login.class);
                                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                startActivity(i);
                                finish();
                            })
                            .setCancelable(false)
                            .show();
                }
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {
                Log.e("AccommodationActivity", "Firebase error: " + error.getMessage());
            }
        });

        // Clear on click
        searchEditText.setOnClickListener(v -> searchEditText.setText(""));

        // Category buttons
        LinearLayout foodLayout    = findViewById(R.id.food);
        LinearLayout dessertLayout = findViewById(R.id.dessert);
        LinearLayout bevLayout     = findViewById(R.id.beverage);
        LinearLayout alcLayout     = findViewById(R.id.alcohol);

        // Default selected
        selectedLayout = foodLayout;
        setSelectedLayout(selectedLayout);

        foodLayout.setOnClickListener(v -> {
            setSelectedLayout(foodLayout);
            fetchProductsByCategory("Food");
        });
        dessertLayout.setOnClickListener(v -> {
            setSelectedLayout(dessertLayout);
            fetchProductsByCategory("Dessert");
        });
        bevLayout.setOnClickListener(v -> {
            setSelectedLayout(bevLayout);
            fetchProductsByCategory("Beverage");
        });
        alcLayout.setOnClickListener(v -> {
            setSelectedLayout(alcLayout);
            fetchProductsByCategory("Alcohol");
        });

        // fetch again to ensure view is populated
        fetchProductsByCategory("Food");
    }

    private void setSelectedLayout(LinearLayout layout) {
        if (selectedLayout != null) {
            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
        }
        layout.setBackgroundResource(R.drawable.selected_background);
        selectedLayout = layout;
    }

    private void fetchProductsByCategory(String category) {
        currentCategory = category;
        progressBar.setVisibility(View.VISIBLE);
        new DatabaseHelper1().fetchProducts(category, new DatabaseHelper1.FirebaseCallback() {
            @Override public void onCallback(List<Accommodation> products) {
                progressBar.setVisibility(View.GONE);
                productsAdapter = new ProductsAdapter(AccommodationAddons.this, products);
                recyclerView.setAdapter(productsAdapter);
            }
            @Override public void onError(String msg) {
                progressBar.setVisibility(View.GONE);
                Log.e("AccommodationActivity", "Error: " + msg);
                Toast.makeText(AccommodationAddons.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void searchProducts(String term) {
        progressBar.setVisibility(View.VISIBLE);
        new DatabaseHelper1().searchProductsByName(term, currentCategory, new DatabaseHelper1.FirebaseCallback() {
            @Override public void onCallback(List<Accommodation> products) {
                progressBar.setVisibility(View.GONE);
                productsAdapter = new ProductsAdapter(AccommodationAddons.this, products);
                recyclerView.setAdapter(productsAdapter);
            }
            @Override public void onError(String msg) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(AccommodationAddons.this, "Error: " + msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        View focused = getCurrentFocus();
        if (focused != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(focused.getWindowToken(), 0);
        }
    }
}
