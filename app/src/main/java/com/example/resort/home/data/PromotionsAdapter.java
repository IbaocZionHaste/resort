package com.example.resort.home.data;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.example.resort.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PromotionsAdapter extends RecyclerView.Adapter<PromotionsAdapter.ViewHolder> implements Filterable {

    private Context context;
    private List<Promotion> promotionList;
    private List<Promotion> promotionListFull; /// Full list for filtering

    /// Cache for album images keyed by the promotion's title
    private static final Map<String, List<Bitmap>> albumCache = new HashMap<>();

    public PromotionsAdapter(Context context, List<Promotion> promotionList) {
        this.context = context;
        this.promotionList = promotionList;
        this.promotionListFull = new ArrayList<>(promotionList);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate your layout. It must include a ViewPager2 with id promoViewPager.
        View view = LayoutInflater.from(context).inflate(R.layout.item_promotion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
        final Promotion promo = promotionList.get(position);

        /// Set the textual data.
        holder.title.setText(promo.getTitle());
        holder.description.setText(promo.getDescription());
        holder.discount.setText(promo.getDiscount() + "% OFF");
        holder.duration.setText("Valid for: " + promo.getDuration() + " days");
        holder.startDate.setText("Starts on: " + promo.getStartDate());

        /// Check if we have cached images for this promotion.
        if (albumCache.containsKey(promo.getTitle())) {
            // Use cached images.
            List<Bitmap> images = albumCache.get(promo.getTitle());
            PromotionImagesAdapter imagesAdapter = new PromotionImagesAdapter(images);
            holder.promoViewPager.setAdapter(imagesAdapter);
        } else {
            /// Set a temporary placeholder if desired. Alternatively, you can wait until data comes in.
            List<Bitmap> placeholder = new ArrayList<>();
            Bitmap defaultBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_home_about);
            placeholder.add(defaultBitmap);
            holder.promoViewPager.setAdapter(new PromotionImagesAdapter(placeholder));

            // Query Firebase for album data matching this promotion's title using a continuous listener
            Query query = FirebaseDatabase.getInstance()
                    .getReference("promoalbum")
                    .orderByChild("productName")
                    .equalTo(promo.getTitle());

            query.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<Bitmap> images = new ArrayList<>();
                    if (snapshot.exists()) {
                        /// Assume one matching album; iterate over its photos (photo1, photo2, photo3)
                        for (DataSnapshot albumSnapshot : snapshot.getChildren()) {
                            for (int i = 1; i <= 3; i++) {
                                String photoKey = "photo" + i;
                                String base64Image = albumSnapshot.child(photoKey).getValue(String.class);
                                if (base64Image != null && base64Image.contains(",")) {
                                    /// Extract the Base64 part after the comma.
                                    String encodedImage = base64Image.split(",")[1];
                                    byte[] decodedBytes = Base64.decode(encodedImage, Base64.DEFAULT);
                                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                    images.add(decodedBitmap);
                                }
                            }
                            // Stop after the first album found.
                            break;
                        }
                    }
                    if (images.isEmpty()) {
                        /// No matching album or images found, so fall back to the default placeholder image.
                        Bitmap defaultBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_home_about);
                        images.add(defaultBitmap);
                    }
                    /// Update the cache for this promotion. Subsequent binds use these images.
                    albumCache.put(promo.getTitle(), images);

                    /// Update the ViewPager adapter with the fetched images.
                    PromotionImagesAdapter imagesAdapter = new PromotionImagesAdapter(images);
                    holder.promoViewPager.setAdapter(imagesAdapter);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // On error, show the default placeholder image.
                    List<Bitmap> images = new ArrayList<>();
                    Bitmap defaultBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_home_about);
                    images.add(defaultBitmap);
                    PromotionImagesAdapter imagesAdapter = new PromotionImagesAdapter(images);
                    holder.promoViewPager.setAdapter(imagesAdapter);
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return promotionList.size();
    }

    /// ViewHolder with ViewPager2 for the swipe gallery.
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, discount, duration, startDate;
        ViewPager2 promoViewPager; /// This replaces or complements your ImageView.

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.promoTitle);
            description = itemView.findViewById(R.id.promoDescription);
            discount = itemView.findViewById(R.id.promoDiscount);
            duration = itemView.findViewById(R.id.promoDuration);
            startDate = itemView.findViewById(R.id.promoStartDate);
            promoViewPager = itemView.findViewById(R.id.promoViewPager);
        }
    }

    /// Nested adapter for the swipeable images in the ViewPager2.
    public class PromotionImagesAdapter extends RecyclerView.Adapter<PromotionImagesAdapter.ImageViewHolder> {

        private List<Bitmap> images;

        public PromotionImagesAdapter(List<Bitmap> images) {
            this.images = images;
        }

        @NonNull
        @Override
        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            /// Inflate your item layout for each image (e.g., item_promo_image.xml)
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promo_image, parent, false);
            return new ImageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
            Bitmap image = images.get(position);
            Glide.with(holder.imageView.getContext())
                    .load(image)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .override(800, 800)
                    .centerInside()
                    .transition(DrawableTransitionOptions.withCrossFade(500))
                    .into(holder.imageView);
        }

        @Override
        public int getItemCount() {
            return images.size();
        }

        class ImageViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            public ImageViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }

    // Filter implementation remains the same.
    @Override
    public Filter getFilter() {
        return promotionFilter;
    }

    private Filter promotionFilter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Promotion> filteredList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0) {
                filteredList.addAll(promotionListFull);
            } else {
                String filterPattern = constraint.toString().toLowerCase().trim();
                for (Promotion item : promotionListFull) {
                    if (item.getTitle().toLowerCase().contains(filterPattern)) {
                        filteredList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = filteredList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            promotionList.clear();
            promotionList.addAll((List) results.values);
            notifyDataSetChanged();
        }
    };
}

///No Stored
//package com.example.resort.home.data;
//
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.BitmapFactory;
//import android.net.Uri;
//import android.util.Base64;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Filter;
//import android.widget.Filterable;
//import android.widget.ImageView;
//import android.widget.TextView;
//
//import androidx.annotation.NonNull;
//import androidx.core.content.FileProvider;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.viewpager2.widget.ViewPager2;
//
//import com.bumptech.glide.Glide;
//import com.bumptech.glide.load.engine.DiskCacheStrategy;
//import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
//import com.example.resort.R;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//
//
//public class PromotionsAdapter extends RecyclerView.Adapter<PromotionsAdapter.ViewHolder> implements Filterable {
//
//    private Context context;
//    private List<Promotion> promotionList;
//    private List<Promotion> promotionListFull; // Full list for filtering
//
//    public PromotionsAdapter(Context context, List<Promotion> promotionList) {
//        this.context = context;
//        this.promotionList = promotionList;
//        this.promotionListFull = new ArrayList<>(promotionList);
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        // Make sure your item layout includes a ViewPager2 with id promoViewPager
//        View view = LayoutInflater.from(context).inflate(R.layout.item_promotion, parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
//        final Promotion promo = promotionList.get(position);
//
//        // Set the textual data
//        holder.title.setText(promo.getTitle());
//        holder.description.setText(promo.getDescription());
//        holder.discount.setText(promo.getDiscount() + "% OFF");
//        holder.duration.setText("Valid for: " + promo.getDuration() + " days");
//        holder.startDate.setText("Starts on: " + promo.getStartDate());
//
//        // Query Firebase for album data matching this promotion's title (productName)
//        FirebaseDatabase.getInstance()
//                .getReference("promoalbum")
//                .orderByChild("productName")
//                .equalTo(promo.getTitle())
//                .addListenerForSingleValueEvent(new ValueEventListener() {
//                    @Override
//                    public void onDataChange(@NonNull DataSnapshot snapshot) {
//                        List<Bitmap> images = new ArrayList<>();
//                        if (snapshot.exists()) {
//                            /// Assume one matching album; iterate over its photos (photo1, photo2, photo3)
//                            for (DataSnapshot albumSnapshot : snapshot.getChildren()) {
//                                for (int i = 1; i <= 3; i++) {
//                                    String photoKey = "photo" + i;
//                                    String base64Image = albumSnapshot.child(photoKey).getValue(String.class);
//                                    if (base64Image != null && base64Image.contains(",")) {
//                                        /// Extract the Base64 part after the comma
//                                        String encodedImage = base64Image.split(",")[1];
//                                        byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
//                                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
//                                        images.add(decodedByte);
//                                    }
//                                }
//                                // Stop after the first album found
//                                break;
//                            }
//                        }
//                        if (images.isEmpty()) {
//                            // No matching album or images, so add the default placeholder image
//                            Bitmap defaultBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_home_about);
//                            images.add(defaultBitmap);
//                        }
//                        // Set up the ViewPager adapter with the fetched images
//                        PromotionImagesAdapter imagesAdapter = new PromotionImagesAdapter(images);
//                        holder.promoViewPager.setAdapter(imagesAdapter);
//                    }
//
//                    @Override
//                    public void onCancelled(@NonNull DatabaseError error) {
//                        // On error, show the default placeholder image
//                        List<Bitmap> images = new ArrayList<>();
//                        Bitmap defaultBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.ic_home_about);
//                        images.add(defaultBitmap);
//                        PromotionImagesAdapter imagesAdapter = new PromotionImagesAdapter(images);
//                        holder.promoViewPager.setAdapter(imagesAdapter);
//                    }
//                });
//    }
//
//    @Override
//    public int getItemCount() {
//        return promotionList.size();
//    }
//
//    // ViewHolder with ViewPager2 for the swipe gallery
//    public static class ViewHolder extends RecyclerView.ViewHolder {
//        TextView title, description, discount, duration, startDate;
//        ViewPager2 promoViewPager; // This replaces or complements your ImageView
//
//        public ViewHolder(@NonNull View itemView) {
//            super(itemView);
//            title = itemView.findViewById(R.id.promoTitle);
//            description = itemView.findViewById(R.id.promoDescription);
//            discount = itemView.findViewById(R.id.promoDiscount);
//            duration = itemView.findViewById(R.id.promoDuration);
//            startDate = itemView.findViewById(R.id.promoStartDate);
//            promoViewPager = itemView.findViewById(R.id.promoViewPager);
//        }
//    }
//
//    /// Nested adapter for the swipeable images in the ViewPager2
//    public class PromotionImagesAdapter extends RecyclerView.Adapter<PromotionImagesAdapter.ImageViewHolder> {
//
//        private List<Bitmap> images;
//
//        public PromotionImagesAdapter(List<Bitmap> images) {
//            this.images = images;
//        }
//
//        @NonNull
//        @Override
//        public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//            /// Create an item layout for each image (item_promo_image.xml)
//            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_promo_image, parent, false);
//            return new ImageViewHolder(view);
//        }
//
//        /** @noinspection ClassEscapesDefinedScope*/
//        @Override
//        public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
//            Bitmap image = images.get(position);
//            Glide.with(holder.imageView.getContext())
//                    .load(image)
//                    .skipMemoryCache(true)
//                    .diskCacheStrategy(DiskCacheStrategy.NONE)
//                    .override(800, 800)
//                    .centerInside()
//                    ///.error(R.drawable.ic_home_about)
//                    .transition(DrawableTransitionOptions.withCrossFade(500))
//                    .into(holder.imageView);
//        }
//
//
//        @Override
//        public int getItemCount() {
//            return images.size();
//        }
//
//        class ImageViewHolder extends RecyclerView.ViewHolder {
//            ImageView imageView;
//
//            public ImageViewHolder(@NonNull View itemView) {
//                super(itemView);
//                imageView = itemView.findViewById(R.id.imageView);
//            }
//        }
//    }
//
//    // Filter implementation remains the same...
//    @Override
//    public Filter getFilter() {
//        return promotionFilter;
//    }
//
//    private Filter promotionFilter = new Filter() {
//        @Override
//        protected FilterResults performFiltering(CharSequence constraint) {
//            List<Promotion> filteredList = new ArrayList<>();
//            if (constraint == null || constraint.length() == 0) {
//                filteredList.addAll(promotionListFull);
//            } else {
//                String filterPattern = constraint.toString().toLowerCase().trim();
//                for (Promotion item : promotionListFull) {
//                    if (item.getTitle().toLowerCase().contains(filterPattern)) {
//                        filteredList.add(item);
//                    }
//                }
//            }
//            FilterResults results = new FilterResults();
//            results.values = filteredList;
//            return results;
//        }
//
//        @Override
//        protected void publishResults(CharSequence constraint, FilterResults results) {
//            promotionList.clear();
//            promotionList.addAll((List) results.values);
//            notifyDataSetChanged();
//        }
//    };
//}
//
