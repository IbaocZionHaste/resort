package com.example.resort.accommodation.data;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.resort.R;
import com.example.resort.addcart.data.CartItem;
import com.example.resort.addcart.data.CartManager;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ProductViewHolder> {

    private final Context context;
    private final List<Accommodation> productsList;

    /// Cache holding album photo URL for each product (keyed by product name).
    private static final Map<String, String> albumPhotoCache = new HashMap<>();

    public ProductsAdapter(Context context, List<Accommodation> productsList) {
        this.context = context;
        this.productsList = productsList;
        attachAlbumListeners();
    }

    /**
     * Attach one continuous Firebase listener per eligible product.
     * When the data changes on Firebase, the listener updates the cache and triggers a UI update.
     */
    private void attachAlbumListeners() {
        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums");

        /// Loop through all products
        for (final Accommodation product : productsList) {
            if (isEligibleForAlbum(product.getCategory())) {
                final String productKey = product.getName();
                /// Attach listener if not already attached; we use the cache's presence as a flag.
                if (!albumPhotoCache.containsKey(productKey)) {
                    Query query = albumRef.orderByChild("productName").equalTo(productKey);
                    query.addValueEventListener(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String albumPhoto = "";
                            if (snapshot.exists()) {
                                /// Process the first matching album for this product
                                for (DataSnapshot albumSnapshot : snapshot.getChildren()) {
                                    String albumCategory = albumSnapshot.child("category").getValue(String.class);
                                    if (albumCategory != null && albumCategory.equalsIgnoreCase(product.getCategory())) {
                                        /// Assume "photo1" is our preferred image.
                                        albumPhoto = albumSnapshot.child("photo1").getValue(String.class);
                                        break;
                                    }
                                }
                            }
                            /// Store the album photo URL into our cache.
                            albumPhotoCache.put(productKey, albumPhoto);
                            /// Trigger a refresh so that any visible item updates automatically.
                            notifyDataSetChanged();
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            albumPhotoCache.put(productKey, "");
                            notifyDataSetChanged();
                        }
                    });
                }
            }
        }
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        /// Inflate your product item layout.
        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ProductViewHolder holder, int position) {
        Accommodation product = productsList.get(position);
        holder.nameTextView.setText(product.getName());
        String imageUrl = product.getImageUrl();

        if (isEligibleForAlbum(product.getCategory())) {
            /// Try to get the cached album photo URL.
            String albumPhoto = albumPhotoCache.get(product.getName());
            holder.albumPhoto1 = albumPhoto;
            if (albumPhoto != null && !albumPhoto.isEmpty()) {
                Glide.with(holder.productImageView.getContext())
                        .load(albumPhoto)
                        .skipMemoryCache(true)
                        .thumbnail(0.1f)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        ///.transition(DrawableTransitionOptions.withCrossFade(100))
                        .into(holder.productImageView);
            } else {
                /// No album photo available; show default placeholder.
                holder.productImageView.setImageResource(R.drawable.ic_no_image);
            }
        } else {
            /// For non-eligible categories, simply load the image from URL.
            Glide.with(holder.productImageView.getContext())
                    .load(imageUrl)
                    .skipMemoryCache(true)
                    .thumbnail(0.1f)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    ///.transition(DrawableTransitionOptions.withCrossFade(100))
                    .into(holder.productImageView);
        }

        /// PLUS ICON: Always visible.
        holder.plusIcon.setVisibility(View.VISIBLE);
        holder.plusIcon.setOnClickListener(v -> {
            /// Preliminary checks
            if ("Boat".equalsIgnoreCase(product.getCategory()) ||
                    "Cottage".equalsIgnoreCase(product.getCategory()) ||
                    "Room".equalsIgnoreCase(product.getCategory()) ||
                    "Package".equalsIgnoreCase(product.getCategory())) {
                if ("Unavailable".equalsIgnoreCase(product.getStatus())) {
                    Toast.makeText(context, "Sorry, the item is not available now. Click the item to view the available date.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            String userId = (currentUser != null) ? currentUser.getUid() : "";
            CartManager cartManager = CartManager.getInstance(context, userId);
            CartItem existingItem = cartManager.getCartItem(product.getName());

            if ("Boat".equalsIgnoreCase(product.getCategory()) ||
                    "Cottage".equalsIgnoreCase(product.getCategory()) ||
                    "Room".equalsIgnoreCase(product.getCategory()) ||
                    "Package".equalsIgnoreCase(product.getCategory())) {
                if (existingItem != null) {
                    ///Toast.makeText(context, "Sorry, only 1 item allowed for " + product.getCategory(), Toast.LENGTH_SHORT).show();
                    Toast.makeText(context, "You’ve already booked this. One per booking!", Toast.LENGTH_SHORT).show();

                    return;
                }
            } else {
                if (existingItem != null && existingItem.getQuantity() >= 10) {
                    ///Toast.makeText(context, "Sorry, only 10 items allowed for " + product.getName(), Toast.LENGTH_SHORT).show();
                    Toast.makeText(context, "Booking limit reached. Max 10 per booking.", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            /// Inflate the custom dialog layout
            View dialogView = LayoutInflater.from(context).inflate(R.layout.custom_cart, null);

            /// Build the AlertDialog
            AlertDialog.Builder builder = new AlertDialog.Builder((Activity) context);
            builder.setView(dialogView);
            builder.setCancelable(true); // Allows dismissal via back button
            AlertDialog alertDialog = builder.create();
            alertDialog.setCanceledOnTouchOutside(true); // Allows dismissal by touching outside
            alertDialog.show();

            /// Handle the OK button click
            Button btnOk = dialogView.findViewById(R.id.btnOk);
            btnOk.setOnClickListener(view -> {
                double priceValue = Double.parseDouble(product.getPrice());
                Integer capacityValue = null;
                if ("Boat".equalsIgnoreCase(product.getCategory()) || "Cottage".equalsIgnoreCase(product.getCategory()) || "Room".equalsIgnoreCase(product.getCategory())) {
                    capacityValue = (product.getCapacity() != null) ? Integer.valueOf(product.getCapacity()) : null;
                }

                /// Use album photo if available; otherwise fall back to the main image URL.
                String photoForCart = (holder.albumPhoto1 != null && !holder.albumPhoto1.isEmpty())
                        ? holder.albumPhoto1
                        : extractToImage(product.getImageUrl());

                CartItem newCartItem = new CartItem(
                        product.getName(),
                        priceValue,
                        product.getCategory(),
                        capacityValue,
                        photoForCart
                );

                if (existingItem != null) {
                    existingItem.setQuantity(existingItem.getQuantity() + 1);
                    cartManager.updateItem(existingItem);
                } else {
                    cartManager.addItem(newCartItem);
                }

                Toast.makeText(context, "Added to booking successfully", Toast.LENGTH_SHORT).show();
                alertDialog.dismiss();
            });

            /// Handle the Cancel button click
            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
            btnCancel.setOnClickListener(view -> alertDialog.dismiss());

            /// Optional: Handle dialog cancellation (e.g., back button or outside touch)
            alertDialog.setOnCancelListener(dialog -> {
                /// No action needed; dialog is simply dismissed
            });
        });

        /// Open detail activity on item click.
        holder.itemView.setOnClickListener(v -> openDetailActivity(product, imageUrl));
    }

    private boolean isEligibleForAlbum(String category) {
        if (category == null) return false;
        return category.equalsIgnoreCase("Cottage") ||
                category.equalsIgnoreCase("Boat") ||
                category.equalsIgnoreCase("Room") ||
                category.equalsIgnoreCase("Food") ||
                category.equalsIgnoreCase("Dessert") ||
                category.equalsIgnoreCase("Alcohol") ||
                category.equalsIgnoreCase("Beverage") ||
                category.equalsIgnoreCase("Package");
    }

    private void openDetailActivity(Accommodation product, String imageUrl) {
        Intent intent;
        // Determine the appropriate activity based on category.
        switch (product.getCategory()) {
            case "Boat":
                intent = new Intent(context, BoatDetailActivity.class);
                break;
            case "Room":
                intent = new Intent(context, RoomDetailActivity.class);
                break;
            case "Food":
                intent = new Intent(context, FoodDetailActivity.class);
                break;
            case "Dessert":
                intent = new Intent(context, DessertDetailActivity.class);
                break;
            case "Beverage":
                intent = new Intent(context, BeverageDetailActivity.class);
                break;
            case "Alcohol":
                intent = new Intent(context, AlcoholDetailActivity.class);
                break;
            case "Package":
                intent = new Intent(context, PackageDetailActivity.class);
                break;
            default:
                intent = new Intent(context, CottageDetailActivity.class);
                break;
        }

        // Pass along extra data.
        intent.putExtra("productId", product.getId());
        intent.putExtra("accommodationName", product.getName());
        intent.putExtra("accommodationDesc", product.getDescription());
        intent.putExtra("accommodationSpec", product.getSpecification());
        intent.putExtra("accommodationStat", product.getStatus());
        intent.putExtra("accommodationPrice", product.getPrice());
        intent.putExtra("accommodationCategory", product.getCategory());
        intent.putExtra("accommodationAmenities", product.getAmenities());
        intent.putExtra("accommodationAvailableDate", product.getAvailableDate());

        intent.putExtra("accommodationReminder", product.getReminder());
        intent.putExtra("accommodationEntrance", product.getEntrance());



        switch (product.getCategory()) {
            case "Boat":
            case "Room":
                intent.putExtra("accommodationCapacity", product.getCapacity());
                intent.putExtra("accommodationDesign", product.getDesign());
                intent.putExtra("accommodationDestination", product.getDestination());
                intent.putExtra("accommodationBoatName", product.getBoatName());
                intent.putExtra("accommodationAddOns", product.getAddOns());
                break;

            case "Food":
                intent.putExtra("food1", product.getFood1());
                intent.putExtra("pieceNameFood", product.getPieceNameFood());
                break;
            case "Beverage":
                intent.putExtra("beverageSize", product.getBeverageSize());
                break;
            case "Dessert":
                intent.putExtra("flavorToppings", product.getFlavorToppings());
                intent.putExtra("perfectFor", product.getPerfectFor());
                intent.putExtra("pieceNameDessert", product.getPieceNameDessert());
                break;
            case "Alcohol":
                intent.putExtra("alcoholSize", product.getAlcoholSize());
                break;
            case "Package":
                intent.putExtra("capacityCottage", product.getCapacityCottage());
                intent.putExtra("food1", product.getFood1());
                intent.putExtra("food2", product.getFood2());
                intent.putExtra("food3", product.getFood3());
                intent.putExtra("food4", product.getFood4());
                intent.putExtra("food5", product.getFood5());
                intent.putExtra("food6", product.getFood6());
                intent.putExtra("accommodationBeverage", product.getBeverage());
                intent.putExtra("accommodationCottage", product.getCottage());
                break;
            case "Cottage":
                intent.putExtra("accommodationCapacity", product.getCapacity());
                intent.putExtra("accommodationDesign", product.getDesign());
                intent.putExtra("accommodationLocation", product.getLocation());
                break;
        }
        intent.putExtra("accommodationImage", imageUrl != null ? imageUrl : "");
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return productsList.size();
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView productImageView;
        TextView nameTextView;
        ImageView plusIcon;
        /// Holds the album photo URL (if fetched).
        public String albumPhoto1 = null;

        ProductViewHolder(View itemView) {
            super(itemView);
            productImageView = itemView.findViewById(R.id.productImage);
            nameTextView = itemView.findViewById(R.id.productName);
            plusIcon = itemView.findViewById(R.id.plus);
            setupPlusIconAnimation();
        }

        /// Set up an animation for the plus icon.
        @SuppressLint("ClickableViewAccessibility")
        private void setupPlusIconAnimation() {
            plusIcon.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            v.setBackgroundColor(Color.parseColor("#ADD8E6"));
                            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
                            break;
                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_CANCEL:
                            v.setBackgroundColor(Color.TRANSPARENT);
                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                            break;
                    }
                    return false;
                }
            });
        }
    }

    /**
     * Helper method to extract an image.
     * Now it just returns the original URL assuming it’s a Firebase Storage URL.
     */
    private String extractToImage(String imageUrl) {
        return imageUrl;
    }
}



///Fix Firebase Storage
//package com.example.resort.accommodation.data;
//
//import android.annotation.SuppressLint;
//import android.app.Activity;
//import android.app.AlertDialog;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.BitmapFactory;
//import android.graphics.Color;
//import android.util.Base64;
//import android.view.LayoutInflater;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//import com.bumptech.glide.load.engine.DiskCacheStrategy;
//import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
//import com.example.resort.R;
//import com.example.resort.addcart.data.CartItem;
//import com.example.resort.addcart.data.CartManager;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.Query;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.List;
//import java.util.HashMap;
//import java.util.Map;
//
//public class ProductsAdapter extends RecyclerView.Adapter<ProductsAdapter.ProductViewHolder> {
//
//    private final Context context;
//    private final List<Accommodation> productsList;
//
//    /// Cache holding album photo URL for each product (keyed by product name).
//    private static final Map<String, String> albumPhotoCache = new HashMap<>();
//
//    public ProductsAdapter(Context context, List<Accommodation> productsList) {
//        this.context = context;
//        this.productsList = productsList;
//        attachAlbumListeners();
//    }
//
//    /**
//     * Attach one continuous Firebase listener per eligible product.
//     * When the data changes on Firebase, the listener updates the cache and triggers a UI update.
//     */
//    private void attachAlbumListeners() {
//        DatabaseReference albumRef = FirebaseDatabase.getInstance().getReference("albums");
//
//        /// Loop through all products
//        for (final Accommodation product : productsList) {
//            if (isEligibleForAlbum(product.getCategory())) {
//                final String productKey = product.getName();
//                /// Attach listener if not already attached; we use the cache's presence as a flag.
//                if (!albumPhotoCache.containsKey(productKey)) {
//                    Query query = albumRef.orderByChild("productName").equalTo(productKey);
//                    query.addValueEventListener(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            String albumPhoto = "";
//                            if (snapshot.exists()) {
//                                /// Process the first matching album for this product
//                                for (DataSnapshot albumSnapshot : snapshot.getChildren()) {
//                                    String albumCategory = albumSnapshot.child("category").getValue(String.class);
//                                    if (albumCategory != null && albumCategory.equalsIgnoreCase(product.getCategory())) {
//                                        /// Assume "photo1" is our preferred image.
//                                        albumPhoto = albumSnapshot.child("photo1").getValue(String.class);
//                                        break;
//                                    }
//                                }
//                            }
//                            /// Store the album photo URL into our cache.
//                            albumPhotoCache.put(productKey, albumPhoto);
//                            /// Trigger a refresh so that any visible item updates automatically.
//                            notifyDataSetChanged();
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//                            albumPhotoCache.put(productKey, "");
//                            notifyDataSetChanged();
//                        }
//                    });
//                }
//            }
//        }
//    }
//
//    @NonNull
//    @Override
//    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        /// Inflate your product item layout.
//        View view = LayoutInflater.from(context).inflate(R.layout.item_product, parent, false);
//        return new ProductViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull final ProductViewHolder holder, int position) {
//        Accommodation product = productsList.get(position);
//        holder.nameTextView.setText(product.getName());
//        String imageUrl = product.getImageUrl();
//
//        if (isEligibleForAlbum(product.getCategory())) {
//            /// Try to get the cached album photo URL.
//            String albumPhoto = albumPhotoCache.get(product.getName());
//            holder.albumPhoto1 = albumPhoto;
//            if (albumPhoto != null && !albumPhoto.isEmpty()) {
//                Glide.with(holder.productImageView.getContext())
//                        .load(albumPhoto)
//                        .skipMemoryCache(true)
//                        .thumbnail(0.1f)
//                        .diskCacheStrategy(DiskCacheStrategy.NONE)
//                        ///.transition(DrawableTransitionOptions.withCrossFade(100))
//                        .into(holder.productImageView);
//            } else {
//                /// No album photo available; show default placeholder.
//                holder.productImageView.setImageResource(R.drawable.ic_no_image);
//            }
//        } else {
//            /// For non-eligible categories, simply load the image from URL.
//            Glide.with(holder.productImageView.getContext())
//                    .load(imageUrl)
//                    .skipMemoryCache(true)
//                    .thumbnail(0.1f)
//                    .diskCacheStrategy(DiskCacheStrategy.NONE)
//                    ///.transition(DrawableTransitionOptions.withCrossFade(100))
//                    .into(holder.productImageView);
//        }
//
//        /// PLUS ICON: Always visible.
//        holder.plusIcon.setVisibility(View.VISIBLE);
//        holder.plusIcon.setOnClickListener(v -> {
//            /// Preliminary checks
//            if ("Boat".equalsIgnoreCase(product.getCategory()) ||
//                    "Cottage".equalsIgnoreCase(product.getCategory()) ||
//                    "Package".equalsIgnoreCase(product.getCategory())) {
//                if ("Unavailable".equalsIgnoreCase(product.getStatus())) {
//                    Toast.makeText(context, "Sorry, the item is not available now. Click the item to view the available date.", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//            }
//
//            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//            String userId = (currentUser != null) ? currentUser.getUid() : "";
//            CartManager cartManager = CartManager.getInstance(context, userId);
//            CartItem existingItem = cartManager.getCartItem(product.getName());
//
//            if ("Boat".equalsIgnoreCase(product.getCategory()) ||
//                    "Cottage".equalsIgnoreCase(product.getCategory()) ||
//                    "Package".equalsIgnoreCase(product.getCategory())) {
//                if (existingItem != null) {
//                    Toast.makeText(context, "Sorry, only 1 item allowed for " + product.getCategory(), Toast.LENGTH_SHORT).show();
//                    return;
//                }
//            } else {
//                if (existingItem != null && existingItem.getQuantity() >= 10) {
//                    Toast.makeText(context, "Sorry, only 10 items allowed for " + product.getName(), Toast.LENGTH_SHORT).show();
//                    return;
//                }
//            }
//
//            /// Inflate the custom dialog layout
//            View dialogView = LayoutInflater.from(context).inflate(R.layout.custom_cart, null);
//
//            /// Build the AlertDialog
//            AlertDialog.Builder builder = new AlertDialog.Builder((Activity) context);
//            builder.setView(dialogView);
//            builder.setCancelable(true); // Allows dismissal via back button
//            AlertDialog alertDialog = builder.create();
//            alertDialog.setCanceledOnTouchOutside(true); // Allows dismissal by touching outside
//            alertDialog.show();
//
//            /// Handle the OK button click
//            Button btnOk = dialogView.findViewById(R.id.btnOk);
//            btnOk.setOnClickListener(view -> {
//                double priceValue = Double.parseDouble(product.getPrice());
//                Integer capacityValue = null;
//                if ("Boat".equalsIgnoreCase(product.getCategory()) || "Cottage".equalsIgnoreCase(product.getCategory())) {
//                    capacityValue = (product.getCapacity() != null) ? Integer.valueOf(product.getCapacity()) : null;
//                }
//
//                /// Use album photo if available; otherwise fall back to the main image URL.
//                String photoForCart = (holder.albumPhoto1 != null && !holder.albumPhoto1.isEmpty())
//                        ? holder.albumPhoto1
//                        : extractToImage(product.getImageUrl());
//
//                CartItem newCartItem = new CartItem(
//                        product.getName(),
//                        priceValue,
//                        product.getCategory(),
//                        capacityValue,
//                        photoForCart
//                );
//
//                if (existingItem != null) {
//                    existingItem.setQuantity(existingItem.getQuantity() + 1);
//                    cartManager.updateItem(existingItem);
//                } else {
//                    cartManager.addItem(newCartItem);
//                }
//
//                Toast.makeText(context, "Add to cart success", Toast.LENGTH_SHORT).show();
//                alertDialog.dismiss();
//            });
//
//            /// Handle the Cancel button click
//            Button btnCancel = dialogView.findViewById(R.id.btnCancel);
//            btnCancel.setOnClickListener(view -> alertDialog.dismiss());
//
//            /// Optional: Handle dialog cancellation (e.g., back button or outside touch)
//            alertDialog.setOnCancelListener(dialog -> {
//                /// No action needed; dialog is simply dismissed
//            });
//        });
//
//        /// Open detail activity on item click.
//        holder.itemView.setOnClickListener(v -> openDetailActivity(product, imageUrl));
//    }
//
//    private boolean isEligibleForAlbum(String category) {
//        if (category == null) return false;
//        return category.equalsIgnoreCase("Cottage") ||
//                category.equalsIgnoreCase("Boat") ||
//                category.equalsIgnoreCase("Food") ||
//                category.equalsIgnoreCase("Dessert") ||
//                category.equalsIgnoreCase("Alcohol") ||
//                category.equalsIgnoreCase("Beverage") ||
//                category.equalsIgnoreCase("Package");
//    }
//
//    private void openDetailActivity(Accommodation product, String imageUrl) {
//        Intent intent;
//        // Determine the appropriate activity based on category.
//        switch (product.getCategory()) {
//            case "Boat":
//                intent = new Intent(context, BoatDetailActivity.class);
//                break;
//            case "Food":
//                intent = new Intent(context, FoodDetailActivity.class);
//                break;
//            case "Dessert":
//                intent = new Intent(context, DessertDetailActivity.class);
//                break;
//            case "Beverage":
//                intent = new Intent(context, BeverageDetailActivity.class);
//                break;
//            case "Alcohol":
//                intent = new Intent(context, AlcoholDetailActivity.class);
//                break;
//            case "Package":
//                intent = new Intent(context, PackageDetailActivity.class);
//                break;
//            default:
//                intent = new Intent(context, CottageDetailActivity.class);
//                break;
//        }
//
//        // Pass along extra data.
//        intent.putExtra("productId", product.getId());
//        intent.putExtra("accommodationName", product.getName());
//        intent.putExtra("accommodationDesc", product.getDescription());
//        intent.putExtra("accommodationSpec", product.getSpecification());
//        intent.putExtra("accommodationStat", product.getStatus());
//        intent.putExtra("accommodationPrice", product.getPrice());
//        intent.putExtra("accommodationCategory", product.getCategory());
//        intent.putExtra("accommodationAmenities", product.getAmenities());
//        intent.putExtra("accommodationAvailableDate", product.getAvailableDate());
//        switch (product.getCategory()) {
//            case "Boat":
//                intent.putExtra("accommodationCapacity", product.getCapacity());
//                intent.putExtra("accommodationDesign", product.getDesign());
//                break;
//            case "Food":
//                intent.putExtra("food1", product.getFood1());
//                intent.putExtra("pieceNameFood", product.getPieceNameFood());
//                break;
//            case "Beverage":
//                intent.putExtra("beverageSize", product.getBeverageSize());
//                break;
//            case "Dessert":
//                intent.putExtra("flavorToppings", product.getFlavorToppings());
//                intent.putExtra("perfectFor", product.getPerfectFor());
//                intent.putExtra("pieceNameDessert", product.getPieceNameDessert());
//                break;
//            case "Alcohol":
//                intent.putExtra("alcoholSize", product.getAlcoholSize());
//                break;
//            case "Package":
//                intent.putExtra("capacityCottage", product.getCapacityCottage());
//                intent.putExtra("food1", product.getFood1());
//                intent.putExtra("food2", product.getFood2());
//                intent.putExtra("food3", product.getFood3());
//                intent.putExtra("food4", product.getFood4());
//                intent.putExtra("food5", product.getFood5());
//                intent.putExtra("food6", product.getFood6());
//                intent.putExtra("accommodationBeverage", product.getBeverage());
//                intent.putExtra("accommodationCottage", product.getCottage());
//                break;
//            case "Cottage":
//                intent.putExtra("accommodationCapacity", product.getCapacity());
//                intent.putExtra("accommodationDesign", product.getDesign());
//                intent.putExtra("accommodationLocation", product.getLocation());
//                break;
//        }
//        intent.putExtra("accommodationImage", imageUrl != null ? imageUrl : "");
//        context.startActivity(intent);
//    }
//
//    @Override
//    public int getItemCount() {
//        return productsList.size();
//    }
//
//    static class ProductViewHolder extends RecyclerView.ViewHolder {
//        ImageView productImageView;
//        TextView nameTextView;
//        ImageView plusIcon;
//
//        /// Holds the album photo URL (if fetched).
//        public String albumPhoto1 = null;
//
//        ProductViewHolder(View itemView) {
//            super(itemView);
//            productImageView = itemView.findViewById(R.id.productImage);
//            nameTextView = itemView.findViewById(R.id.productName);
//            plusIcon = itemView.findViewById(R.id.plus);
//            setupPlusIconAnimation();
//        }
//
//        /// Set up an animation for the plus icon.
//        @SuppressLint("ClickableViewAccessibility")
//        private void setupPlusIconAnimation() {
//            plusIcon.setOnTouchListener(new View.OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    switch (event.getAction()) {
//                        case MotionEvent.ACTION_DOWN:
//                            v.setBackgroundColor(Color.parseColor("#ADD8E6"));
//                            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100).start();
//                            break;
//                        case MotionEvent.ACTION_UP:
//                        case MotionEvent.ACTION_CANCEL:
//                            v.setBackgroundColor(Color.TRANSPARENT);
//                            v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
//                            break;
//                    }
//                    return false;
//                }
//            });
//        }
//    }
//
//    /**
//     * Helper method to extract an image.
//     * Now it just returns the original URL assuming it’s a Firebase Storage URL.
//     */
//    private String extractToImage(String imageUrl) {
//        return imageUrl;
//    }
//}
//
