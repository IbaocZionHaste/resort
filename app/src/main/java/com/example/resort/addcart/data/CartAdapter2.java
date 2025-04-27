//package com.example.resort.addcart.data;
//
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.Intent;
//import android.view.LayoutInflater;
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
//import com.example.resort.AccommodationAddons;
//import com.example.resort.R;
//import com.example.resort.accommodation.data.RoomDetailActivity;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//
//import java.util.List;
//
//public class CartAdapter2 extends RecyclerView.Adapter<CartAdapter2.CartViewHolder> {
//
//    private List<CartItem> cartItems;
//    private final Context context;
//    private final CartUpdateListener updateListener;
//    private final String userId;
//
//    public CartAdapter2(Context context, List<CartItem> cartItems, CartUpdateListener updateListener) {
//        this.context = context;
//        this.cartItems = cartItems;
//        this.updateListener = updateListener;
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser != null) {
//            this.userId = currentUser.getUid();
//        } else {
//            this.userId = "";
//            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
//        }
//    }
//
//    @NonNull
//    @Override
//    public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        View view = LayoutInflater.from(context).inflate(R.layout.item_cart2, parent, false);
//        return new CartViewHolder(view);
//    }
//
//    @SuppressLint({"DefaultLocale", "SetTextI18n"})
//    @Override
//    public void onBindViewHolder(@NonNull CartViewHolder holder, int position) {
//        CartItem item = cartItems.get(position);
//        holder.nameText.setText(item.getName());
//        holder.category.setText(item.getCategory());
//        holder.priceText.setText("₱" + String.format("%.0f", (item.getPrice() * item.getQuantity())));
//
//        // Show capacity for Cottage, Boat, Room
//        if (("Cottage".equals(item.getCategory()) || "Boat".equals(item.getCategory()) || "Room".equals(item.getCategory()))
//                && item.getCapacity() != null) {
//            holder.capacity.setText("Capacity: " + item.getCapacity());
//            holder.capacity.setVisibility(View.VISIBLE);
//        } else {
//            holder.capacity.setVisibility(View.GONE);
//        }
//
//
//        // Delete button
//        holder.deleteButton.setOnClickListener(v -> {
//            CartManager.getInstance(context, userId).removeItem(item);
//            notifyItemRemoved(position);
//            notifyItemRangeChanged(position, cartItems.size());
//            updateListener.onCartUpdated();
//        });
//
//        // Add Ons button: launch RoomDetailActivity
//        holder.buttonAddOns.setOnClickListener(v -> {
//            Intent intent = new Intent(context, AccommodationAddons.class);
//            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            context.startActivity(intent);
//        });
//
//        // Load image
//        String imageUrl = item.getImageUrl();
//        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
//            Glide.with(context)
//                    .load(imageUrl)
//                    .placeholder(R.drawable.ic_no_image)
//                    .into(holder.itemImage);
//        } else {
//            Glide.with(context)
//                    .load(R.drawable.ic_no_image)
//                    .into(holder.itemImage);
//        }
//    }
//
//    @Override
//    public int getItemCount() {
//        return cartItems.size();
//    }
//
//    public void updateCartItems(List<CartItem> newItems) {
//        this.cartItems = newItems;
//    }
//
//    static class CartViewHolder extends RecyclerView.ViewHolder {
//        TextView nameText, priceText, category, capacity;
//        ImageView deleteButton, itemImage;
//        Button buttonAddOns;
//
//        public CartViewHolder(@NonNull View itemView) {
//            super(itemView);
//            category = itemView.findViewById(R.id.itemCategory);
//            nameText = itemView.findViewById(R.id.itemName);
//            itemImage = itemView.findViewById(R.id.itemImage);
//            priceText = itemView.findViewById(R.id.itemPrice);
//            capacity = itemView.findViewById(R.id.itemCapacity);
//            deleteButton = itemView.findViewById(R.id.deleteButton);
//            buttonAddOns = itemView.findViewById(R.id.buttonAddOns);
//        }
//    }
//}
