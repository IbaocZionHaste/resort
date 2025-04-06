///FIX THE SEARCH BAR
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.util.DisplayMetrics;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.inputmethod.InputMethodManager;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.Toast;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import com.example.resort.accommodation.data.Accommodation;
//import com.example.resort.accommodation.data.DatabaseHelper1;
//import com.example.resort.accommodation.data.ProductsAdapter;
//
//import java.util.List;
//import java.util.Objects;
//
//public class AccommodationFragment extends Fragment {
//
//    private LinearLayout selectedLayout;
//    private RecyclerView recyclerView;
//    private ProductsAdapter productsAdapter;
//    private ProgressBar progressBar;
//    private EditText searchEditText;
//
//
//    @SuppressLint("ClickableViewAccessibility")
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.accommodation_fragment, container, false);
//
//        progressBar = view.findViewById(R.id.progressBar);
//        progressBar.setVisibility(View.GONE);
//        searchEditText = view.findViewById(R.id.searchEditText);
//        recyclerView = view.findViewById(R.id.recycleView);
//        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
//
//        // ---------------------------
//        // Adapt screen size dynamically using DisplayMetrics
//        // ---------------------------
//        //DisplayMetrics displayMetrics = new DisplayMetrics();
//        //noinspection deprecation
//        //requireActivity().getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
//        //int screenWidth = displayMetrics.widthPixels;
//        //int screenHeight = displayMetrics.heightPixels;
//        // Example: Adjust RecyclerView height to half the screen height
//        //ViewGroup.LayoutParams recyclerParams = recyclerView.getLayoutParams();
//        //recyclerParams.height = screenHeight / 2; // adapt height based on screen size
//        // recyclerView.setLayoutParams(recyclerParams);
//        // ---------------------------
//        //float dpWidth = displayMetrics.widthPixels / displayMetrics.density;
//        //int spanCount = Math.max(3, (int)(dpWidth / 180)); // 180dp per column as a baseline
//        //recyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));
//
//        // Optimize RecyclerView for better performance
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setItemViewCacheSize(20);
//        //noinspection deprecation
//        recyclerView.setDrawingCacheEnabled(true);
//        //noinspection deprecation
//        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
//        // Enable smooth scrolling
//        recyclerView.setNestedScrollingEnabled(false);
//
//        // Load products initially
//        fetchProductsByCategory("Cottage");
//
//        // Dismiss keyboard when tapping outside EditText
//        ConstraintLayout mainLayout = view.findViewById(R.id.mainLayout);
//        mainLayout.setOnTouchListener((v, event) -> {
//            hideKeyboard();
//            searchEditText.clearFocus();
//            return false;
//        });
//
//        // Hide keyboard when focus is lost
//        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
//            if (!hasFocus) hideKeyboard();
//        });
//
//        // Search listener
//        searchEditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
//                String searchTerm = charSequence.toString().trim();
//                if (!searchTerm.isEmpty()) {
//                    searchProducts(searchTerm);
//                } else {
//                    fetchProductsByCategory("Cottage");
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {}
//        });
//
//        // Initialize category buttons
//        LinearLayout cottageLayout = view.findViewById(R.id.cottage);
//        LinearLayout boatLayout = view.findViewById(R.id.boat);
//        LinearLayout foodLayout = view.findViewById(R.id.food);
//        LinearLayout dessertLayout = view.findViewById(R.id.dessert);
//        LinearLayout beverageLayout = view.findViewById(R.id.beverage);
//        LinearLayout alcoholLayout = view.findViewById(R.id.alcohol);
//        LinearLayout packageLayout = view.findViewById(R.id.pack);
//
//        // Set default selection to "Cottage"
//        selectedLayout = cottageLayout;
//        setSelectedLayout(selectedLayout);
//
//        // Set click listeners for category selection
//        cottageLayout.setOnClickListener(v -> {
//            setSelectedLayout(cottageLayout);
//            fetchProductsByCategory("Cottage");
//        });
//        boatLayout.setOnClickListener(v -> {
//            setSelectedLayout(boatLayout);
//            fetchProductsByCategory("Boat");
//        });
//        foodLayout.setOnClickListener(v -> {
//            setSelectedLayout(foodLayout);
//            fetchProductsByCategory("Food");
//        });
//        dessertLayout.setOnClickListener(v -> {
//            setSelectedLayout(dessertLayout);
//            fetchProductsByCategory("Dessert");
//        });
//        beverageLayout.setOnClickListener(v -> {
//            setSelectedLayout(beverageLayout);
//            fetchProductsByCategory("Beverage");
//        });
//        alcoholLayout.setOnClickListener(v -> {
//            setSelectedLayout(alcoholLayout);
//            fetchProductsByCategory("Alcohol");
//        });
//        packageLayout.setOnClickListener(v -> {
//            setSelectedLayout(packageLayout);
//            fetchProductsByCategory("Package");
//        });
//
//        // Fetch default category products
//        fetchProductsByCategory("Cottage");
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
//    private void fetchProductsByCategory(String category) {
//        progressBar.setVisibility(View.VISIBLE);
//        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
//        databaseHelper.fetchProducts(category, new DatabaseHelper1.FirebaseCallback() {
//            @Override
//            public void onCallback(List<Accommodation> products) {
//                progressBar.setVisibility(View.GONE);
//                productsAdapter = new ProductsAdapter(getContext(), products);
//                recyclerView.setAdapter(productsAdapter);
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                progressBar.setVisibility(View.GONE);
//                Log.e("AccommodationFragment", "Error fetching data: " + errorMessage);
//            }
//        });
//    }
//
//    private void searchProducts(String searchTerm) {
//        progressBar.setVisibility(View.VISIBLE);
//        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
//        databaseHelper.searchProductsByName(searchTerm, new DatabaseHelper1.FirebaseCallback() {
//            @Override
//            public void onCallback(List<Accommodation> products) {
//                progressBar.setVisibility(View.GONE);
//                productsAdapter = new ProductsAdapter(getContext(), products);
//                recyclerView.setAdapter(productsAdapter);
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                progressBar.setVisibility(View.GONE);
//                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void hideKeyboard() {
//        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (imm != null && requireActivity().getCurrentFocus() != null) {
//            imm.hideSoftInputFromWindow(Objects.requireNonNull(requireActivity().getCurrentFocus()).getWindowToken(), 0);
//        }
//    }
//}


package com.example.resort;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.resort.accommodation.data.Accommodation;
import com.example.resort.accommodation.data.DatabaseHelper1;
import com.example.resort.accommodation.data.ProductsAdapter;
import java.util.List;
import java.util.Objects;

public class AccommodationFragment extends Fragment {

    private LinearLayout selectedLayout;
    private RecyclerView recyclerView;
    private ProductsAdapter productsAdapter;
    private ProgressBar progressBar;
    private EditText searchEditText;

    // Track the currently selected category
    private String currentCategory = "Cottage";

    @SuppressLint("ClickableViewAccessibility")
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.accommodation_fragment, container, false);

        progressBar = view.findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);
        searchEditText = view.findViewById(R.id.searchEditText);
        recyclerView = view.findViewById(R.id.recycleView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));

        recyclerView.setHasFixedSize(true);
        recyclerView.setItemViewCacheSize(20);
        recyclerView.setDrawingCacheEnabled(true);
        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        recyclerView.setNestedScrollingEnabled(false);

        // Load default products for "Cottage"
        fetchProductsByCategory("Cottage");

        ConstraintLayout mainLayout = view.findViewById(R.id.mainLayout);
        mainLayout.setOnTouchListener((v, event) -> {
            hideKeyboard();
            searchEditText.clearFocus();
            return false;
        });

        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) hideKeyboard();
        });

        // Search listener - filter within the current category
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                String searchTerm = charSequence.toString().trim();
                if (!searchTerm.isEmpty()) {
                    searchProducts(searchTerm);
                } else {
                    fetchProductsByCategory(currentCategory);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) { }
        });

        // Clear search text when search bar is clicked
        searchEditText.setOnClickListener(v -> searchEditText.setText(""));

        // Category button initializations
        LinearLayout cottageLayout = view.findViewById(R.id.cottage);
        LinearLayout boatLayout = view.findViewById(R.id.boat);
        LinearLayout foodLayout = view.findViewById(R.id.food);
        LinearLayout dessertLayout = view.findViewById(R.id.dessert);
        LinearLayout beverageLayout = view.findViewById(R.id.beverage);
        LinearLayout alcoholLayout = view.findViewById(R.id.alcohol);
        LinearLayout packageLayout = view.findViewById(R.id.pack);

        // Set default selection to "Cottage"
        selectedLayout = cottageLayout;
        setSelectedLayout(selectedLayout);

        cottageLayout.setOnClickListener(v -> {
            setSelectedLayout(cottageLayout);
            fetchProductsByCategory("Cottage");
        });
        boatLayout.setOnClickListener(v -> {
            setSelectedLayout(boatLayout);
            fetchProductsByCategory("Boat");
        });
        foodLayout.setOnClickListener(v -> {
            setSelectedLayout(foodLayout);
            fetchProductsByCategory("Food");
        });
        dessertLayout.setOnClickListener(v -> {
            setSelectedLayout(dessertLayout);
            fetchProductsByCategory("Dessert");
        });
        beverageLayout.setOnClickListener(v -> {
            setSelectedLayout(beverageLayout);
            fetchProductsByCategory("Beverage");
        });
        alcoholLayout.setOnClickListener(v -> {
            setSelectedLayout(alcoholLayout);
            fetchProductsByCategory("Alcohol");
        });
        packageLayout.setOnClickListener(v -> {
            setSelectedLayout(packageLayout);
            fetchProductsByCategory("Package");
        });

        // Fetch default products again for "Cottage"
        fetchProductsByCategory("Cottage");
        return view;
    }

    private void setSelectedLayout(LinearLayout layout) {
        if (selectedLayout != null) {
            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
        }
        layout.setBackgroundResource(R.drawable.selected_background);
        selectedLayout = layout;
    }

    private void fetchProductsByCategory(String category) {
        // Update current category for search filtering
        currentCategory = category;
        progressBar.setVisibility(View.VISIBLE);
        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
        databaseHelper.fetchProducts(category, new DatabaseHelper1.FirebaseCallback() {
            @Override
            public void onCallback(List<Accommodation> products) {
                progressBar.setVisibility(View.GONE);
                productsAdapter = new ProductsAdapter(getContext(), products);
                recyclerView.setAdapter(productsAdapter);
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Log.e("AccommodationFragment", "Error fetching data: " + errorMessage);
            }
        });
    }

    private void searchProducts(String searchTerm) {
        progressBar.setVisibility(View.VISIBLE);
        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
        // Search within the currently selected category
        databaseHelper.searchProductsByName(searchTerm, currentCategory, new DatabaseHelper1.FirebaseCallback() {
            @Override
            public void onCallback(List<Accommodation> products) {
                progressBar.setVisibility(View.GONE);
                productsAdapter = new ProductsAdapter(getContext(), products);
                recyclerView.setAdapter(productsAdapter);
            }

            @Override
            public void onError(String errorMessage) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && requireActivity().getCurrentFocus() != null) {
            imm.hideSoftInputFromWindow(Objects.requireNonNull(requireActivity().getCurrentFocus()).getWindowToken(), 0);
        }
    }
}


//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.view.inputmethod.InputMethodManager;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.constraintlayout.widget.ConstraintLayout;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.accommodation.data.Accommodation;
//import com.example.resort.accommodation.data.DatabaseHelper1;
//import com.example.resort.accommodation.data.ProductsAdapter;
//
//import java.util.List;
//
//public class AccommodationFragment extends Fragment {
//
//    private LinearLayout selectedLayout;
//    private RecyclerView recyclerView;
//    private ProductsAdapter productsAdapter;
//    private ProgressBar progressBar;
//    private EditText searchEditText;
//
//    @SuppressLint("ClickableViewAccessibility")
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_accommodation, container, false);
//
//        progressBar = view.findViewById(R.id.progressBar);
//        progressBar.setVisibility(View.GONE);
//        searchEditText = view.findViewById(R.id.searchEditText);
//        recyclerView = view.findViewById(R.id.recycleView);
//        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
//
//        // Optimize RecyclerView for better performance
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setItemViewCacheSize(20);
//        recyclerView.setDrawingCacheEnabled(true);
//        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
//
//        // Enable smooth scrolling
//        recyclerView.setNestedScrollingEnabled(false);
//
//        // Load products initially
//        fetchProductsByCategory("Cottage");
//
//
//        // Dismiss keyboard when tapping outside EditText
//        ConstraintLayout mainLayout = view.findViewById(R.id.mainLayout);
//        mainLayout.setOnTouchListener((v, event) -> {
//            hideKeyboard();
//            searchEditText.clearFocus();
//            return false;
//        });
//
//        // Hide keyboard when focus is lost
//        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
//            if (!hasFocus) hideKeyboard();
//        });
//
//        // Search listener
//        searchEditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
//                String searchTerm = charSequence.toString().trim();
//                if (!searchTerm.isEmpty()) {
//                    searchProducts(searchTerm);
//                } else {
//                    fetchProductsByCategory("Cottage");
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {}
//        });
//
//        // Initialize category buttons
//        LinearLayout cottageLayout = view.findViewById(R.id.cottage);
//        LinearLayout boatLayout = view.findViewById(R.id.boat);
//        LinearLayout foodLayout = view.findViewById(R.id.food);
//        LinearLayout dessertLayout = view.findViewById(R.id.dessert);
//        LinearLayout beverageLayout = view.findViewById(R.id.beverage);
//        LinearLayout alcoholLayout = view.findViewById(R.id.alcohol);
//        LinearLayout packageLayout = view.findViewById(R.id.pack);
//
//        // Set default selection to "Cottage"
//        selectedLayout = cottageLayout;
//        setSelectedLayout(selectedLayout);
//
//        // Set click listeners for category selection
//        cottageLayout.setOnClickListener(v -> {
//            setSelectedLayout(cottageLayout);
//            fetchProductsByCategory("Cottage");
//        });
//        boatLayout.setOnClickListener(v -> {
//            setSelectedLayout(boatLayout);
//            fetchProductsByCategory("Boat");
//        });
//        foodLayout.setOnClickListener(v -> {
//            setSelectedLayout(foodLayout);
//            fetchProductsByCategory("Food");
//        });
//        dessertLayout.setOnClickListener(v -> {
//            setSelectedLayout(dessertLayout);
//            fetchProductsByCategory("Dessert");
//        });
//        beverageLayout.setOnClickListener(v -> {
//            setSelectedLayout(beverageLayout);
//            fetchProductsByCategory("Beverage");
//        });
//        alcoholLayout.setOnClickListener(v -> {
//            setSelectedLayout(alcoholLayout);
//            fetchProductsByCategory("Alcohol");
//        });
//        packageLayout.setOnClickListener(v -> {
//            setSelectedLayout(packageLayout);
//            fetchProductsByCategory("Package");
//        });
//
//        // Fetch default category products
//        fetchProductsByCategory("Cottage");
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
//    private void fetchProductsByCategory(String category) {
//        progressBar.setVisibility(View.VISIBLE);
//        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
//        databaseHelper.fetchProducts(category, new DatabaseHelper1.FirebaseCallback() {
//            @Override
//            public void onCallback(List<Accommodation> products) {
//                progressBar.setVisibility(View.GONE);
//                productsAdapter = new ProductsAdapter(getContext(), products);
//                recyclerView.setAdapter(productsAdapter);
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                progressBar.setVisibility(View.GONE);
//                Log.e("AccommodationFragment", "Error fetching data: " + errorMessage);
//            }
//        });
//    }
//
//    private void searchProducts(String searchTerm) {
//        progressBar.setVisibility(View.VISIBLE);
//        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
//        databaseHelper.searchProductsByName(searchTerm, new DatabaseHelper1.FirebaseCallback() {
//            @Override
//            public void onCallback(List<Accommodation> products) {
//                progressBar.setVisibility(View.GONE);
//                productsAdapter = new ProductsAdapter(getContext(), products);
//                recyclerView.setAdapter(productsAdapter);
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                progressBar.setVisibility(View.GONE);
//                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void hideKeyboard() {
//        InputMethodManager imm = (InputMethodManager) requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
//        if (imm != null && requireActivity().getCurrentFocus() != null) {
//            imm.hideSoftInputFromWindow(requireActivity().getCurrentFocus().getWindowToken(), 0);
//        }
//    }
//}


//IMPROVE THE SEARCH View Active field to hide
//package com.example.resort;
//
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.EditText;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.accommodation.data.Accommodation;
//import com.example.resort.accommodation.data.DatabaseHelper1;
//import com.example.resort.accommodation.data.ProductsAdapter;
//
//import java.util.List;
//
//public class AccommodationFragment extends Fragment {
//
//    private LinearLayout selectedLayout;
//    private RecyclerView recyclerView;
//    private ProductsAdapter productsAdapter;
//    private ProgressBar progressBar;
//    private EditText searchEditText;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_accommodation, container, false);
//
//        progressBar = view.findViewById(R.id.progressBar);
//        progressBar.setVisibility(View.GONE);
//
//        searchEditText = view.findViewById(R.id.searchEditText);  // Assuming you have an EditText with this ID
//
//
//        // Search listener
//        searchEditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
//                String searchTerm = charSequence.toString().trim();
//                if (!searchTerm.isEmpty()) {
//                    searchProducts(searchTerm);
//                } else {
//                    fetchProductsByCategory("Cottage");  // Default category if search is empty
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) {}
//        });
//
//        // Initialize category LinearLayouts
//        LinearLayout cottageLayout = view.findViewById(R.id.cottage);
//        LinearLayout boatLayout = view.findViewById(R.id.boat);
//        LinearLayout foodLayout = view.findViewById(R.id.food);
//        LinearLayout beverageLayout = view.findViewById(R.id.beverage);
//        LinearLayout packageLayout = view.findViewById(R.id.pack);
//
//        // Set default selection to "cottage"
//        selectedLayout = cottageLayout;
//        setSelectedLayout(selectedLayout);
//
//        // Set click listeners for category selection
//        cottageLayout.setOnClickListener(v -> {
//            setSelectedLayout(cottageLayout);
//            fetchProductsByCategory("Cottage");
//        });
//        boatLayout.setOnClickListener(v -> {
//            setSelectedLayout(boatLayout);
//            fetchProductsByCategory("Boat");
//        });
//        foodLayout.setOnClickListener(v -> {
//            setSelectedLayout(foodLayout);
//            fetchProductsByCategory("Food");
//        });
//        beverageLayout.setOnClickListener(v -> {
//            setSelectedLayout(beverageLayout);
//            fetchProductsByCategory("Beverage");
//        });
//        packageLayout.setOnClickListener(v -> {
//            setSelectedLayout(packageLayout);
//            fetchProductsByCategory("Package");
//        });
//
//        recyclerView = view.findViewById(R.id.recycleView);
//        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
//
//        // Initially fetch products (for the default category or empty search)
//        fetchProductsByCategory("Cottage");
//
//        return view;
//    }
//
//    private void setSelectedLayout(LinearLayout layout) {
//        // Reset the background of the previously selected layout
//        if (selectedLayout != null) {
//            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
//        }
//        // Set the new selected layout background color
//        layout.setBackgroundResource(R.drawable.selected_background);
//        selectedLayout = layout;
//    }
//
//    private void fetchProductsByCategory(String category) {
//        // Show ProgressBar while loading data
//        progressBar.setVisibility(View.VISIBLE);
//
//        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
//        databaseHelper.fetchProducts(category, new DatabaseHelper1.FirebaseCallback() {
//            @Override
//            public void onCallback(List<Accommodation> products) {
//                // Hide ProgressBar once data is fetched
//                progressBar.setVisibility(View.GONE);
//
//                // Update the adapter with the fetched products
//                productsAdapter = new ProductsAdapter(getContext(), products);
//                recyclerView.setAdapter(productsAdapter); // Refresh the RecyclerView
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                // Hide ProgressBar in case of error
//                progressBar.setVisibility(View.GONE);
//                Log.e("AccommodationFragment", "Error fetching data: " + errorMessage); // Handle error
//            }
//        });
//    }
//
//    // Method to search products by name
//    private void searchProducts(String searchTerm) {
//        progressBar.setVisibility(View.VISIBLE);
//
//        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
//        databaseHelper.searchProductsByName(searchTerm, new DatabaseHelper1.FirebaseCallback() {
//            @Override
//            public void onCallback(List<Accommodation> products) {
//                progressBar.setVisibility(View.GONE);
//                productsAdapter = new ProductsAdapter(getContext(), products);
//                recyclerView.setAdapter(productsAdapter);
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                progressBar.setVisibility(View.GONE);
//                // Improved error message display
//                String userFriendlyMessage = "Error: " + errorMessage;
//                Toast.makeText(getContext(), userFriendlyMessage, Toast.LENGTH_SHORT).show();
//            }
//        });
//
//    }
//
//}
// NO SEARCH
//package com.example.resort;
//
//import android.os.Bundle;
//import android.util.Log;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.LinearLayout;
//import android.widget.ProgressBar;
//
//import com.example.resort.accommodation.data.Accommodation;
//import com.example.resort.accommodation.data.DatabaseHelper1;
//import com.example.resort.accommodation.data.ProductsAdapter;
//
//import java.util.List;
//
//public class AccommodationFragment extends Fragment {
//
//    private LinearLayout selectedLayout;
//    private RecyclerView recyclerView;
//    private ProductsAdapter productsAdapter;
//    private ProgressBar progressBar;  // Declare ProgressBar
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_accommodation, container, false);
//
//        // Initialize ProgressBar
//        progressBar = view.findViewById(R.id.progressBar); // Make sure this exists in your XML layout file
//        progressBar.setVisibility(View.GONE); // Initially hidden
//
//        // Initialize category LinearLayouts
//        LinearLayout cottageLayout = view.findViewById(R.id.cottage);
//        LinearLayout boatLayout = view.findViewById(R.id.boat);
//        LinearLayout foodLayout = view.findViewById(R.id.food);
//        LinearLayout beverageLayout = view.findViewById(R.id.beverage);
//        LinearLayout packageLayout = view.findViewById(R.id.pack);
//
//        // Set default selection to "cottage"
//        selectedLayout = cottageLayout;
//        setSelectedLayout(selectedLayout);
//
//        // Set click listeners for category selection
//        cottageLayout.setOnClickListener(v -> {
//            setSelectedLayout(cottageLayout);
//            fetchProductsByCategory("Cottage");
//        });
//        boatLayout.setOnClickListener(v -> {
//            setSelectedLayout(boatLayout);
//            fetchProductsByCategory("Boat");
//        });
//        foodLayout.setOnClickListener(v -> {
//            setSelectedLayout(foodLayout);
//            fetchProductsByCategory("Food");
//        });
//        beverageLayout.setOnClickListener(v -> {
//            setSelectedLayout(beverageLayout);
//            fetchProductsByCategory("Beverage");
//        });
//        packageLayout.setOnClickListener(v -> {
//            setSelectedLayout(packageLayout);
//            fetchProductsByCategory("Package");
//        });
//
//        // Setup RecyclerView with a grid layout (2 columns per row)
//        recyclerView = view.findViewById(R.id.recycleView);
//        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
//
//        // Initially fetch products (for the default "Cottage" category)
//        fetchProductsByCategory("Cottage");
//
//        return view;
//    }
//
//    private void setSelectedLayout(LinearLayout layout) {
//        // Reset the background of the previously selected layout
//        if (selectedLayout != null) {
//            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
//        }
//        // Set the new selected layout background color
//        layout.setBackgroundResource(R.drawable.selected_background);
//        selectedLayout = layout;
//    }
//
//    private void fetchProductsByCategory(String category) {
//        // Show ProgressBar while loading data
//        progressBar.setVisibility(View.VISIBLE);
//
//        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
//        databaseHelper.fetchProducts(category, new DatabaseHelper1.FirebaseCallback() {
//            @Override
//            public void onCallback(List<Accommodation> products) {
//                // Hide ProgressBar once data is fetched
//                progressBar.setVisibility(View.GONE);
//
//                // Update the adapter with the fetched products
//                productsAdapter = new ProductsAdapter(getContext(), products);
//                recyclerView.setAdapter(productsAdapter); // Refresh the RecyclerView
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                // Hide ProgressBar in case of error
//                progressBar.setVisibility(View.GONE);
//                Log.e("AccommodationFragment", "Error fetching data: " + errorMessage); // Handle error
//            }
//        });
//    }
//}



//NO PROGRESS BAR
//package com.example.resort;
//
//import android.util.Log;
//import android.os.Bundle;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.LinearLayout;
//
//import com.example.resort.accommodation.data.Accommodation;
//import com.example.resort.accommodation.data.DatabaseHelper1;
//import com.example.resort.accommodation.data.ProductsAdapter;
//
//import java.util.List;
//
//public class AccommodationFragment extends Fragment {
//
//    private LinearLayout selectedLayout;
//    private RecyclerView recyclerView;
//    private ProductsAdapter productsAdapter;
//
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_accommodation, container, false);
//
//        // Initialize category LinearLayouts
//        LinearLayout cottageLayout = view.findViewById(R.id.cottage);
//        LinearLayout boatLayout = view.findViewById(R.id.boat);
//        LinearLayout foodLayout = view.findViewById(R.id.food);
//        LinearLayout beverageLayout = view.findViewById(R.id.beverage);
//        LinearLayout packageLayout = view.findViewById(R.id.pack);
//
//        // Set default selection to "cottage"
//        selectedLayout = cottageLayout;
//        setSelectedLayout(selectedLayout);
//
//        // Set click listeners for category selection
//        cottageLayout.setOnClickListener(v -> {
//            setSelectedLayout(cottageLayout);
//            fetchProductsByCategory("Cottage");
//        });
//        boatLayout.setOnClickListener(v -> {
//            setSelectedLayout(boatLayout);
//            fetchProductsByCategory("Boat");
//        });
//        foodLayout.setOnClickListener(v -> {
//            setSelectedLayout(foodLayout);
//            fetchProductsByCategory("Food");
//        });
//        beverageLayout.setOnClickListener(v -> {
//            setSelectedLayout(beverageLayout);
//            fetchProductsByCategory("Beverage");
//        });
//        packageLayout.setOnClickListener(v -> {
//            setSelectedLayout(packageLayout);
//            fetchProductsByCategory("Package");
//        });
//
//        // Setup RecyclerView with a grid layout (2 columns per row)
//        recyclerView = view.findViewById(R.id.recycleView);
//        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
//
//        // Initially fetch products (for the default "Cottage" category)
//        fetchProductsByCategory("Cottage");
//
//        return view;
//    }
//
//    private void setSelectedLayout(LinearLayout layout) {
//        // Reset the background of the previously selected layout
//        if (selectedLayout != null) {
//            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
//        }
//        // Set the new selected layout background color
//        layout.setBackgroundResource(R.drawable.selected_background);
//        selectedLayout = layout;
//    }
//
//    private void fetchProductsByCategory(String category) {
//        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
//        databaseHelper.fetchProducts(category, new DatabaseHelper1.FirebaseCallback() {
//            @Override
//            public void onCallback(List<Accommodation> products) {
//                // Update the adapter with the fetched products
//                productsAdapter = new ProductsAdapter(getContext(), products);
//                recyclerView.setAdapter(productsAdapter); // Refresh the RecyclerView
//            }
//
//            @Override
//            public void onError(String errorMessage) {
//                Log.e("AccommodationFragment", "Error fetching data: " + errorMessage); // Handle error
//            }
//        });
//    }
//
//
//}



//package com.example.resort;
//
//import android.os.Bundle;
//import androidx.annotation.NonNull;
//import androidx.annotation.Nullable;
//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import android.util.Log;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.LinearLayout;
//import com.example.resort.accommodation.data.Accommodation;
//import com.example.resort.accommodation.data.DatabaseHelper1;
//import com.example.resort.accommodation.data.ProductsAdapter;
//
//import java.util.List;
//
//public class AccommodationFragment extends Fragment {
//
//    private LinearLayout selectedLayout;
//    private RecyclerView recyclerView;
//    private ProductsAdapter productsAdapter;
//
//    @Nullable
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
//                             @Nullable Bundle savedInstanceState) {
//        View view = inflater.inflate(R.layout.fragment_accommodation, container, false);
//
//        // Initialize category LinearLayouts
//        LinearLayout cottageLayout = view.findViewById(R.id.cottage);
//        LinearLayout boatLayout = view.findViewById(R.id.boat);
//        LinearLayout foodLayout = view.findViewById(R.id.food);
//        LinearLayout beverageLayout = view.findViewById(R.id.beverage);
//        LinearLayout packageLayout = view.findViewById(R.id.pack);
//
//        // Set default selection to "cottage"
//        selectedLayout = cottageLayout;
//        setSelectedLayout(selectedLayout);
//
//        // Set click listeners for category selection
//        cottageLayout.setOnClickListener(v -> {
//            setSelectedLayout(cottageLayout);
//            // Optionally, add filtering logic by category here.
//        });
//        boatLayout.setOnClickListener(v -> setSelectedLayout(boatLayout));
//        foodLayout.setOnClickListener(v -> setSelectedLayout(foodLayout));
//        beverageLayout.setOnClickListener(v -> setSelectedLayout(beverageLayout));
//        packageLayout.setOnClickListener(v -> setSelectedLayout(packageLayout));
//
//        // Setup RecyclerView with a grid layout (2 columns per row)
//        recyclerView = view.findViewById(R.id.recycleView);
//        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2
//        ));
//
//        // Fetch data from Firebase using DatabaseHelper1
//        DatabaseHelper1 databaseHelper = new DatabaseHelper1();
//        databaseHelper.fetchProducts(new DatabaseHelper1.FirebaseCallback() {
//            @Override
//            public void onCallback(List<Accommodation> products) {
//                productsAdapter = new ProductsAdapter(getContext(), products);
//                recyclerView.setAdapter(productsAdapter);
//            }
//
//            @Override
//            public void onError(Exception e) {
//                Log.e("AccommodationFragment", "Error fetching data", e);
//            }
//        });
//
//        return view;
//    }
//
//    private void setSelectedLayout(LinearLayout layout) {
//        // Reset the background of the previously selected layout
//        if (selectedLayout != null) {
//            selectedLayout.setBackgroundResource(R.drawable.border_rounded);
//        }
//        // Set the new selected layout background color
//        layout.setBackgroundResource(R.drawable.selected_background);
//        selectedLayout = layout;
//    }
//}
