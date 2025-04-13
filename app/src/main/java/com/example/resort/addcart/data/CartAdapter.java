package com.example.resort.addcart.data;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.MultiTransformation;
import com.bumptech.glide.load.resource.bitmap.CenterCrop;
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
        if (("Cottage".equals(item.getCategory()) || "Boat".equals(item.getCategory()))
                && item.getCapacity() != null) {
            holder.capacity.setText("Capacity: " + item.getCapacity());
            holder.capacity.setVisibility(View.VISIBLE);
        } else {
            holder.capacity.setVisibility(View.GONE);
        }

        /// Check category: hide plus/minus and quantity for Boat, Cottage, and Package items
        if ("Boat".equals(item.getCategory()) || "Cottage".equals(item.getCategory()) || "Package".equals(item.getCategory())) {
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
        }
    }
}




///No Get Current User
//package com.example.resort.addcart.data;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
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
//import com.example.resort.R;
//import java.util.List;
//
//public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
//
//    private List<CartItem> cartItems;
//    private final Context context;
//    private final CartUpdateListener updateListener;
//
//    public CartAdapter(Context context, List<CartItem> cartItems, CartUpdateListener updateListener) {
//        this.context = context;
//        this.cartItems = cartItems;
//        this.updateListener = updateListener;
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
//        // Set item name and category
//        holder.nameText.setText(item.getName());
//        holder.category.setText(item.getCategory());
//        // Set price (unit price * quantity)
//        holder.priceText.setText("₱" + String.format("%.2f", (item.getPrice() * item.getQuantity())));
//        holder.quantityText.setText(String.valueOf(item.getQuantity()));
//
//        // Display capacity if the item is a Cottage or Boat and capacity is not null
//        if (("Cottage".equals(item.getCategory()) || "Boat".equals(item.getCategory()))
//                && item.getCapacity() != null) {
//            holder.capacity.setText("Capacity: " + item.getCapacity());
//            holder.capacity.setVisibility(View.VISIBLE);
//        } else {
//            holder.capacity.setVisibility(View.GONE);
//        }
//
//        // Check category: hide plus/minus and quantity for Boat and Cottage items only
//        if ("Boat".equals(item.getCategory()) || "Cottage".equals(item.getCategory())  || "Package".equals(item.getCategory())) {
//            holder.plusButton.setVisibility(View.GONE);
//            holder.minusButton.setVisibility(View.GONE);
//            holder.quantityText.setVisibility(View.GONE);
//        } else {
//            // For other items (including Cottage Boat Package), show the buttons and quantity
//            holder.plusButton.setVisibility(View.VISIBLE);
//            holder.minusButton.setVisibility(View.VISIBLE);
//            holder.quantityText.setVisibility(View.VISIBLE);
//
//            // Plus button: increase quantity if below 10
//            holder.plusButton.setOnClickListener(v -> {
//                if (item.getQuantity() < 10) {
//                    item.setQuantity(item.getQuantity() + 1);
//                    CartManager.getInstance(context, null).persistCart();  // Persist change
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
//                    CartManager.getInstance(context, null).persistCart();
//                    notifyItemChanged(position);
//                    updateListener.onCartUpdated();
//                }
//            });
//        }
//
//        // Load image using Glide from Base64 string using a data URI
//        String base64Image = item.getImageUrl();
//        if (base64Image != null && !base64Image.trim().isEmpty()) {
//            String imageData = "data:image/png;base64," + base64Image;
//            Glide.with(context)
//                    .load(imageData)
//                    .placeholder(R.drawable.ic_profile_about)
//                    .into(holder.itemImage);
//        } else {
//            Glide.with(context)
//                    .load(R.drawable.ic_profile_about)
//                    .into(holder.itemImage);
//        }
//
//        // Delete button: remove the item from the cart
//        holder.deleteButton.setOnClickListener(v -> {
//            CartManager.getInstance(context, null).removeItem(item);
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
//    // For resetting cart items after a successful booking
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



//package com.example.resort.addcart.data;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//import com.example.resort.R;
//import java.util.List;
//
//public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
//
//    private List<CartItem> cartItems;
//    private final Context context;
//    private final CartUpdateListener updateListener;
//
//    public CartAdapter(Context context, List<CartItem> cartItems, CartUpdateListener updateListener) {
//        this.context = context;
//        this.cartItems = cartItems;
//        this.updateListener = updateListener;
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
//        // Set item name and category (ensure your CartItem has a getCategory() method)
//        holder.nameText.setText(item.getName());
//        holder.category.setText(item.getCategory());
//        // Set price (unit price * quantity)
//        holder.priceText.setText("₱" + String.format("%.2f", (item.getPrice() * item.getQuantity())));
//        holder.quantityText.setText(String.valueOf(item.getQuantity()));
//
//        // Plus button: increase quantity if below 10
//        holder.plusButton.setOnClickListener(v -> {
//            if (item.getQuantity() < 10) {
//                item.setQuantity(item.getQuantity() + 1);
//                notifyItemChanged(position);
//                updateListener.onCartUpdated();
//            } else {
//                Toast.makeText(context, "Maximum limit reached", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // Minus button: decrease quantity if above 1
//        holder.minusButton.setOnClickListener(v -> {
//            if (item.getQuantity() > 1) {
//                item.setQuantity(item.getQuantity() - 1);
//                notifyItemChanged(position);
//                updateListener.onCartUpdated();
//            }
//        });
//
//        // Delete button: remove the item from the cart
//        holder.deleteButton.setOnClickListener(v -> {
//            cartItems.remove(position);
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
//    // For resetting cart items after a successful booking
//    public void updateCartItems(List<CartItem> newItems) {
//        this.cartItems = newItems;
//    }
//
//    static class CartViewHolder extends RecyclerView.ViewHolder {
//        TextView nameText, priceText, quantityText, category, capacity;
//        ImageView plusButton, minusButton, deleteButton;
//
//        public CartViewHolder(@NonNull View itemView) {
//            super(itemView);
//            category = itemView.findViewById(R.id.itemCategory);
//            nameText = itemView.findViewById(R.id.itemName);
//            priceText = itemView.findViewById(R.id.itemPrice);
//            quantityText = itemView.findViewById(R.id.itemQuantity);
//            capacity = itemView.findViewById(R.id.itemCapacity);
//            plusButton = itemView.findViewById(R.id.plusButton);
//            minusButton = itemView.findViewById(R.id.minusButton);
//            deleteButton = itemView.findViewById(R.id.deleteButton);
//        }
//    }
//}

/// no store data cart if exit
//package com.example.resort.addcart.data;
//
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageButton;
//import android.widget.ImageView;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//import com.example.resort.R;
//import java.util.List;
//
//public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
//
//    private List<CartItem> cartItems;
//    private final Context context;
//    private final CartUpdateListener updateListener;
//
//    public CartAdapter(Context context, List<CartItem> cartItems, CartUpdateListener updateListener) {
//        this.context = context;
//        this.cartItems = cartItems;
//        this.updateListener = updateListener;
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
//        // Ipakita ang pangalan at total price (unit price * quantity)
//        holder.nameText.setText(item.getName());
//        holder.priceText.setText("₱" + String.format("%.2f", (item.getPrice() * item.getQuantity())));
//        holder.quantityText.setText(String.valueOf(item.getQuantity()));
//
//        // Plus button: dagdagan ang quantity kung wala pa sa 10
//        holder.plusButton.setOnClickListener(v -> {
//            if (item.getQuantity() < 10) {
//                item.setQuantity(item.getQuantity() + 1);
//                notifyItemChanged(position);
//                updateListener.onCartUpdated(); // Notify for overall cart update
//            } else {
//                Toast.makeText(context, "Maximum limit reached", Toast.LENGTH_SHORT).show();
//            }
//        });
//
//        // Minus button: bawasan ang quantity kung higit pa sa 1
//        holder.minusButton.setOnClickListener(v -> {
//            if (item.getQuantity() > 1) {
//                item.setQuantity(item.getQuantity() - 1);
//                notifyItemChanged(position);
//                updateListener.onCartUpdated();
//            }
//        });
//
//        // Delete button: tanggalin ang item sa cart
//        holder.deleteButton.setOnClickListener(v -> {
//            cartItems.remove(position);
//            notifyItemRemoved(position);
//            notifyItemRangeChanged(position, cartItems.size());
//            updateListener.onCartUpdated();
//        });
//    }
//
//
//    @Override
//    public int getItemCount() {
//        return cartItems.size();
//    }
//
//    // this for reset after book Success
//    public void updateCartItems(List<CartItem> newItems) {
//        this.cartItems = newItems;
//    }
//    static class CartViewHolder extends RecyclerView.ViewHolder {
//        TextView nameText, priceText, quantityText, category;
//        ImageView plusButton, minusButton, deleteButton;
//
//        public CartViewHolder(@NonNull View itemView) {
//            super(itemView);
//            category = itemView.findViewById(R.id.itemCategory);
//            nameText = itemView.findViewById(R.id.itemName);
//            priceText = itemView.findViewById(R.id.itemPrice);
//            quantityText = itemView.findViewById(R.id.itemQuantity);
//            plusButton = itemView.findViewById(R.id.plusButton);
//            minusButton = itemView.findViewById(R.id.minusButton);
//            deleteButton = itemView.findViewById(R.id.deleteButton);
//        }
//    }
//}
//
//

/// this no limit in the cart increase
//    @Override
//    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
//        CartItem item = cartItems.get(position);
//        // Ipakita ang total price: unit price * quantity
//        holder.nameText.setText(item.getName());
//        holder.priceText.setText("₱" + String.format("%.2f", (item.getPrice() * item.getQuantity())));
//        holder.quantityText.setText(String.valueOf(item.getQuantity()));
//
//        // Plus button: dagdagan ang quantity at update total price ng item
//        holder.plusButton.setOnClickListener(v -> {
//            item.setQuantity(item.getQuantity() + 1);
//            notifyItemChanged(position);
//            updateListener.onCartUpdated(); // notify the activity
//        });
//
//        // Minus button: bawasan ang quantity kung higit pa sa 1
//        holder.minusButton.setOnClickListener(v -> {
//            if (item.getQuantity() > 1) {
//                item.setQuantity(item.getQuantity() - 1);
//                notifyItemChanged(position);
//                updateListener.onCartUpdated(); // notify the activity
//            }
//        });
//
//        // Delete button: tanggalin ang item sa cart
//        holder.deleteButton.setOnClickListener(v -> {
//            cartItems.remove(position);
//            notifyItemRemoved(position);
//            notifyItemRangeChanged(position, cartItems.size());
//            updateListener.onCartUpdated(); // notify the activity
//        });
//    }

/// success but no increase and decrease price also total
//import android.content.Context;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ImageButton;
//import android.widget.TextView;
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//import com.example.resort.R;
//import java.util.List;
//
//public class CartAdapter extends RecyclerView.Adapter<CartAdapter.CartViewHolder> {
//
//    private List<CartItem> cartItems;
//    private Context context;
//
//    public CartAdapter(Context context, List<CartItem> cartItems) {
//        this.context = context;
//        this.cartItems = cartItems;
//    }
//
//    @NonNull
//    @Override
//    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
//        return new CartViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
//        CartItem item = cartItems.get(position);
//        holder.nameText.setText(item.getName());
//        holder.priceText.setText("₱" + String.format("%.2f", item.getPrice()));
//        holder.quantityText.setText(String.valueOf(item.getQuantity()));
//
//        // Increase quantity
//        holder.plusButton.setOnClickListener(v -> {
//            item.setQuantity(item.getQuantity() + 1);
//            notifyItemChanged(position);
//        });
//
//        // Decrease quantity
//        holder.minusButton.setOnClickListener(v -> {
//            if (item.getQuantity() > 1) {
//                item.setQuantity(item.getQuantity() - 1);
//                notifyItemChanged(position);
//            }
//        });
//
//        // Delete item
//        holder.deleteButton.setOnClickListener(v -> {
//            cartItems.remove(position);
//            notifyItemRemoved(position);
//            notifyItemRangeChanged(position, cartItems.size());
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return cartItems.size();
//    }
//
//    static class CartViewHolder extends RecyclerView.ViewHolder {
//        TextView nameText, priceText, quantityText;
//        ImageButton plusButton, minusButton, deleteButton;
//
//        public CartViewHolder(@NonNull View itemView) {
//            super(itemView);
//            nameText = itemView.findViewById(R.id.itemName);
//            priceText = itemView.findViewById(R.id.itemPrice);
//            quantityText = itemView.findViewById(R.id.itemQuantity);
//            plusButton = itemView.findViewById(R.id.plusButton);
//            minusButton = itemView.findViewById(R.id.minusButton);
//            deleteButton = itemView.findViewById(R.id.deleteButton);
//        }
//    }
//}

