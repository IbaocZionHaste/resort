package com.example.resort.addcart.data;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CartManager {
    /// Map to store CartManager instances for each user.
    private static final Map<String, CartManager> instances = new HashMap<>();
    private final List<CartItem> cartItems;
    private final SharedPreferences sharedPreferences;
    private final Gson gson;
    private static final String CART_KEY = "CART_DATA";

    /// Private constructor that uses a user-specific SharedPreferences file.
    private CartManager(Context context, String userId) {
        sharedPreferences = context.getSharedPreferences("CartPrefs_" + userId, Context.MODE_PRIVATE);
        gson = new Gson();
        cartItems = loadCart();
    }

    /// Get an instance of CartManager for a given user.
    public static synchronized CartManager getInstance(Context context, String userId) {
        if (!instances.containsKey(userId)) {
            instances.put(userId, new CartManager(context.getApplicationContext(), userId));
        }
        return instances.get(userId);
    }

    public void addItem(CartItem item) {
        for (CartItem ci : cartItems) {
            if (ci.getName().equals(item.getName())) {
                ci.setQuantity(ci.getQuantity() + 1);
                saveCart();
                return;
            }
        }
        cartItems.add(item);
        saveCart();
    }


    public boolean hasRoomItems() {
        for (CartItem item : cartItems) {
            if ("Room".equalsIgnoreCase(item.getCategory())) {
                return true;
            }
        }
        return false;
    }


    /// Returns the CartItem that matches the given product name, or null if not found.
    public CartItem getCartItem(String productName) {
        for (CartItem item : cartItems) {
            if (item.getName().equals(productName)) {
                return item;
            }
        }
        return null;
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public void removeItem(CartItem item) {
        cartItems.remove(item);
        saveCart();
    }

    public int getItemCount() {
        int totalItemCount = 0;
        for (CartItem item : cartItems) {
            totalItemCount += item.getQuantity();
        }
        return totalItemCount;
    }

    /// Save cart data persistently in user-specific SharedPreferences.
    private void saveCart() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        String json = gson.toJson(cartItems);
        editor.putString(CART_KEY, json);
        editor.apply();
    }

    /// Load cart data from storage.
    private List<CartItem> loadCart() {
        String json = sharedPreferences.getString(CART_KEY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<CartItem>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void persistCart() {
        saveCart();
    }

    public void clearCartItems() {
        cartItems.clear();
        saveCart();
    }


    /// Updates an existing item (simply persists the changes).
    public void updateItem(CartItem item) {
        // As item is a reference, we simply save the cart.
        saveCart();
    }

}



///Fix Current
//package com.example.resort.addcart.data;
//
//import android.content.Context;
//import android.content.SharedPreferences;
//import com.google.gson.Gson;
//import com.google.gson.reflect.TypeToken;
//import java.lang.reflect.Type;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class CartManager {
//    /// Map to store CartManager instances for each user.
//    private static final Map<String, CartManager> instances = new HashMap<>();
//    private final List<CartItem> cartItems;
//    private final SharedPreferences sharedPreferences;
//    private final Gson gson;
//    private static final String CART_KEY = "CART_DATA";
//
//    /// Private constructor that uses a user-specific SharedPreferences file.
//    private CartManager(Context context, String userId) {
//        sharedPreferences = context.getSharedPreferences("CartPrefs_" + userId, Context.MODE_PRIVATE);
//        gson = new Gson();
//        cartItems = loadCart();
//    }
//
//    /// Get an instance of CartManager for a given user.
//    public static synchronized CartManager getInstance(Context context, String userId) {
//        if (!instances.containsKey(userId)) {
//            instances.put(userId, new CartManager(context.getApplicationContext(), userId));
//        }
//        return instances.get(userId);
//    }
//
//    public void addItem(CartItem item) {
//        for (CartItem ci : cartItems) {
//            if (ci.getName().equals(item.getName())) {
//                ci.setQuantity(ci.getQuantity() + 1);
//                saveCart();
//                return;
//            }
//        }
//        cartItems.add(item);
//        saveCart();
//    }
//
//
//    /// Returns the CartItem that matches the given product name, or null if not found.
//    public CartItem getCartItem(String productName) {
//        for (CartItem item : cartItems) {
//            if (item.getName().equals(productName)) {
//                return item;
//            }
//        }
//        return null;
//    }
//
//    public List<CartItem> getCartItems() {
//        return cartItems;
//    }
//
//    public void removeItem(CartItem item) {
//        cartItems.remove(item);
//        saveCart();
//    }
//
//    public int getItemCount() {
//        int totalItemCount = 0;
//        for (CartItem item : cartItems) {
//            totalItemCount += item.getQuantity();
//        }
//        return totalItemCount;
//    }
//
//    /// Save cart data persistently in user-specific SharedPreferences.
//    private void saveCart() {
//        SharedPreferences.Editor editor = sharedPreferences.edit();
//        String json = gson.toJson(cartItems);
//        editor.putString(CART_KEY, json);
//        editor.apply();
//    }
//
//    /// Load cart data from storage.
//    private List<CartItem> loadCart() {
//        String json = sharedPreferences.getString(CART_KEY, null);
//        if (json == null) return new ArrayList<>();
//        Type type = new TypeToken<List<CartItem>>() {}.getType();
//        return gson.fromJson(json, type);
//    }
//
//    public void persistCart() {
//        saveCart();
//    }
//
//    public void clearCartItems() {
//        cartItems.clear();
//        saveCart();
//    }
//
//
//    /// Updates an existing item (simply persists the changes).
//    public void updateItem(CartItem item) {
//        // As item is a reference, we simply save the cart.
//        saveCart();
//    }
//
//}
//
