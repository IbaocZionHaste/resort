package com.example.resort.addcart.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
import com.example.resort.AccommodationAddons;
import com.example.resort.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.List;
import java.util.Map;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.request.RequestOptions;


public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {

    private List<CartItem> cartItems;
    private final Context context;
    private final CartUpdateListener updateListener;
    private final String userId;

    public CartAdapter(Context context, List<CartItem> cartItems, CartUpdateListener updateListener) {
        this.context = context;
        this.cartItems = cartItems;
        this.updateListener = updateListener;
        /// Retrieve the current user once and store the userId
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            this.userId = currentUser.getUid();
        } else {
            /// Handle case where user is not logged in as needed
            this.userId = "";
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
        }
    }

    @NonNull
    @Override
    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new CartViewHolder(view);
    }

    @SuppressLint({"DefaultLocale", "SetTextI18n"})
    @Override
    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        /// Set item name and category
        holder.nameText.setText(item.getName());
        holder.category.setText(item.getCategory());
        /// Set price (unit price * quantity)
        holder.priceText.setText("₱" + String.format("%.0f", (item.getPrice() * item.getQuantity())));
        holder.quantityText.setText(String.valueOf(item.getQuantity()));

        /// Display capacity if the item is a Cottage or Boat and capacity is not null
        if (("Cottage".equals(item.getCategory()) || "Boat".equals(item.getCategory()) || "Room".equals(item.getCategory()))
                && item.getCapacity() != null) {
            holder.capacity.setText("Capacity: " + item.getCapacity());
            holder.capacity.setVisibility(View.VISIBLE);
        } else {
            holder.capacity.setVisibility(View.GONE);
        }

        /// Check category: hide plus/minus and quantity for Boat, Cottage, and Package items
        if ("Boat".equals(item.getCategory()) || "Cottage".equals(item.getCategory()) || "Package".equals(item.getCategory()) || "Room".equals(item.getCategory())) {
            holder.plusButton.setVisibility(View.GONE);
            holder.minusButton.setVisibility(View.GONE);
            holder.quantityText.setVisibility(View.GONE);
        } else {
            /// For other items, show the buttons and quantity
            holder.plusButton.setVisibility(View.VISIBLE);
            holder.minusButton.setVisibility(View.VISIBLE);
            holder.quantityText.setVisibility(View.VISIBLE);

            /// Plus button: increase quantity if below 10
            holder.plusButton.setOnClickListener(v -> {
                if (item.getQuantity() < 10) {
                    item.setQuantity(item.getQuantity() + 1);
                    CartManager.getInstance(context, userId).persistCart();  /// Pass the proper userId
                    notifyItemChanged(position);
                    updateListener.onCartUpdated();
                } else {
                    Toast.makeText(context, "Maximum limit reached", Toast.LENGTH_SHORT).show();
                }
            });

            // Minus button: decrease quantity if above 1
            holder.minusButton.setOnClickListener(v -> {
                if (item.getQuantity() > 1) {
                    item.setQuantity(item.getQuantity() - 1);
                    CartManager.getInstance(context, userId).persistCart();
                    notifyItemChanged(position);
                    updateListener.onCartUpdated();
                }
            });
        }


        /// Load image directly from the Firebase Storage URL
        String firebaseImageUrl = item.getImageUrl();
        if (firebaseImageUrl != null && !firebaseImageUrl.trim().isEmpty()) {
            Glide.with(context)
                    .load(firebaseImageUrl)
                    .placeholder(R.drawable.ic_no_image)
                    .into(holder.itemImage);
        } else {
            Glide.with(context)
                    .load(R.drawable.ic_no_image)
                    .into(holder.itemImage);
        }


        // Delete button: remove the item from the cart
        holder.deleteButton.setOnClickListener(v -> {
            CartManager.getInstance(context, userId).removeItem(item);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, cartItems.size());
            updateListener.onCartUpdated();
        });


        String cat = item.getCategory();
        if ("Food".equals(cat) || "Dessert".equals(cat) || "Beverage".equals(cat) || "Alcohol".equals(cat)) {
            holder.button.setVisibility(View.GONE);
        } else {
            holder.button.setVisibility(View.VISIBLE);
            holder.button.setText("Add ons");
            holder.button.setOnClickListener(v -> {
                Intent intent = new Intent(context, AccommodationAddons.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            });
        }


    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    /// For resetting cart items after a successful booking
    public void updateCartItems(List<CartItem> newItems) {
        this.cartItems = newItems;
    }

    static class CartViewHolder extends RecyclerView.ViewHolder {
        TextView nameText, priceText, quantityText, category, capacity;
        ImageView plusButton, minusButton, deleteButton, itemImage;
        Button button;

        public CartViewHolder(@NonNull View itemView) {
            super(itemView);
            category = itemView.findViewById(R.id.itemCategory);
            nameText = itemView.findViewById(R.id.itemName);
            itemImage = itemView.findViewById(R.id.itemImage);
            priceText = itemView.findViewById(R.id.itemPrice);
            quantityText = itemView.findViewById(R.id.itemQuantity);
            capacity = itemView.findViewById(R.id.itemCapacity);
            plusButton = itemView.findViewById(R.id.plusButton);
            minusButton = itemView.findViewById(R.id.minusButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            button = itemView.findViewById(R.id.button);
        }
    }
}



///Fix Current
//package com.example.resort.addcart.data;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.SharedPreferences;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.bumptech.glide.Glide;
//import com.bumptech.glide.load.MultiTransformation;
//import com.bumptech.glide.load.resource.bitmap.CenterCrop;
//import com.example.resort.R;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//
//import java.util.List;
//import java.util.Map;
//import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
//import com.bumptech.glide.request.RequestOptions;
//
//
//public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
//
//    private List<CartItem> cartItems;
//    private final Context context;
//    private final CartUpdateListener updateListener;
//    private final String userId;
//
//    public CartAdapter(Context context, List<CartItem> cartItems, CartUpdateListener updateListener) {
//        this.context = context;
//        this.cartItems = cartItems;
//        this.updateListener = updateListener;
//        /// Retrieve the current user once and store the userId
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            this.userId = currentUser.getUid();
//        } else {
//            /// Handle case where user is not logged in as needed
//            this.userId = "";
//            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    @NonNull
//    @Override
//    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
//        return new CartViewHolder(view);
//    }
//
//    @SuppressLint({"DefaultLocale", "SetTextI18n"})
//    @Override
//    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
//        CartItem item = cartItems.get(position);
//        /// Set item name and category
//        holder.nameText.setText(item.getName());
//        holder.category.setText(item.getCategory());
//        /// Set price (unit price * quantity)
//        holder.priceText.setText("₱" + String.format("%.0f", (item.getPrice() * item.getQuantity())));
//        holder.quantityText.setText(String.valueOf(item.getQuantity()));
//
//        /// Display capacity if the item is a Cottage or Boat and capacity is not null
//        if (("Cottage".equals(item.getCategory()) || "Boat".equals(item.getCategory()))
//                && item.getCapacity() != null) {
//            holder.capacity.setText("Capacity: " + item.getCapacity());
//            holder.capacity.setVisibility(View.VISIBLE);
//        } else {
//            holder.capacity.setVisibility(View.GONE);
//        }
//
//        /// Check category: hide plus/minus and quantity for Boat, Cottage, and Package items
//        if ("Boat".equals(item.getCategory()) || "Cottage".equals(item.getCategory()) || "Package".equals(item.getCategory())) {
//            holder.plusButton.setVisibility(View.GONE);
//            holder.minusButton.setVisibility(View.GONE);
//            holder.quantityText.setVisibility(View.GONE);
//        } else {
//            /// For other items, show the buttons and quantity
//            holder.plusButton.setVisibility(View.VISIBLE);
//            holder.minusButton.setVisibility(View.VISIBLE);
//            holder.quantityText.setVisibility(View.VISIBLE);
//
//            /// Plus button: increase quantity if below 10
//            holder.plusButton.setOnClickListener(v -> {
//                if (item.getQuantity() < 10) {
//                    item.setQuantity(item.getQuantity() + 1);
//                    CartManager.getInstance(context, userId).persistCart();  /// Pass the proper userId
//                    notifyItemChanged(position);
//                    updateListener.onCartUpdated();
//                } else {
//                    Toast.makeText(context, "Maximum limit reached", Toast.LENGTH_SHORT).show();
//                }
//            });
//
//            // Minus button: decrease quantity if above 1
//            holder.minusButton.setOnClickListener(v -> {
//                if (item.getQuantity() > 1) {
//                    item.setQuantity(item.getQuantity() - 1);
//                    CartManager.getInstance(context, userId).persistCart();
//                    notifyItemChanged(position);
//                    updateListener.onCartUpdated();
//                }
//            });
//        }
//
//
//        /// Load image directly from the Firebase Storage URL
//        String firebaseImageUrl = item.getImageUrl();
//        if (firebaseImageUrl != null && !firebaseImageUrl.trim().isEmpty()) {
//            Glide.with(context)
//                    .load(firebaseImageUrl)
//                    .placeholder(R.drawable.ic_no_image)
//                    .into(holder.itemImage);
//        } else {
//            Glide.with(context)
//                    .load(R.drawable.ic_no_image)
//                    .into(holder.itemImage);
//        }
//
//
//        // Delete button: remove the item from the cart
//        holder.deleteButton.setOnClickListener(v -> {
//            CartManager.getInstance(context, userId).removeItem(item);
//            notifyItemRemoved(position);
//            notifyItemRangeChanged(position, cartItems.size());
//            updateListener.onCartUpdated();
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return cartItems.size();
//    }
//
//    /// For resetting cart items after a successful booking
//    public void updateCartItems(List<CartItem> newItems) {
//        this.cartItems = newItems;
//    }
//
//    static class CartViewHolder extends RecyclerView.ViewHolder {
//        TextView nameText, priceText, quantityText, category, capacity;
//        ImageView plusButton, minusButton, deleteButton, itemImage;
//
//        public CartViewHolder(@NonNull View itemView) {
//            super(itemView);
//            category = itemView.findViewById(R.id.itemCategory);
//            nameText = itemView.findViewById(R.id.itemName);
//            itemImage = itemView.findViewById(R.id.itemImage);
//            priceText = itemView.findViewById(R.id.itemPrice);
//            quantityText = itemView.findViewById(R.id.itemQuantity);
//            capacity = itemView.findViewById(R.id.itemCapacity);
//            plusButton = itemView.findViewById(R.id.plusButton);
//            minusButton = itemView.findViewById(R.id.minusButton);
//            deleteButton = itemView.findViewById(R.id.deleteButton);
//        }
//    }
//}
//
//
