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



///No Current User
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.os.Bundle;
//import android.text.Editable;
//import android.text.TextWatcher;
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
//    // Track the currently selected category
//    private String currentCategory = "Cottage";
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
//        recyclerView.setHasFixedSize(true);
//        recyclerView.setItemViewCacheSize(20);
//        recyclerView.setDrawingCacheEnabled(true);
//        recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
//        recyclerView.setNestedScrollingEnabled(false);
//
//        // Load default products for "Cottage"
//        fetchProductsByCategory("Cottage");
//
//        ConstraintLayout mainLayout = view.findViewById(R.id.mainLayout);
//        mainLayout.setOnTouchListener((v, event) -> {
//            hideKeyboard();
//            searchEditText.clearFocus();
//            return false;
//        });
//
//        searchEditText.setOnFocusChangeListener((v, hasFocus) -> {
//            if (!hasFocus) hideKeyboard();
//        });
//
//        // Search listener - filter within the current category
//        searchEditText.addTextChangedListener(new TextWatcher() {
//            @Override
//            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }
//
//            @Override
//            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
//                String searchTerm = charSequence.toString().trim();
//                if (!searchTerm.isEmpty()) {
//                    searchProducts(searchTerm);
//                } else {
//                    fetchProductsByCategory(currentCategory);
//                }
//            }
//
//            @Override
//            public void afterTextChanged(Editable editable) { }
//        });
//
//        // Clear search text when search bar is clicked
//        searchEditText.setOnClickListener(v -> searchEditText.setText(""));
//
//        // Category button initializations
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
//        // Fetch default products again for "Cottage"
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
//        // Update current category for search filtering
//        currentCategory = category;
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
//        // Search within the currently selected category
//        databaseHelper.searchProductsByName(searchTerm, currentCategory, new DatabaseHelper1.FirebaseCallback() {
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
//
