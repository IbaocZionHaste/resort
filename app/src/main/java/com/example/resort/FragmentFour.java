package com.example.resort;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.Map;

public class FragmentFour extends Fragment {

    private ImageView photoImageView;
    private ProgressBar progressBar;
    /// Simple cache for storing decoded byte arrays keyed by the Base64 data string.
    private static final Map<String, byte[]> base64Cache = new HashMap<>();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_fragment_four, container, false);
        photoImageView = view.findViewById(R.id.photoImageView);
        progressBar = view.findViewById(R.id.progressBar);

        // Show progress bar initially
        progressBar.setVisibility(View.VISIBLE);

        // Reference to the stored image in Firebase Realtime Database
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Pages")
                .child("image")
                .child("photo3");

        // Use a single-value event listener to fetch the image data once
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String imageUrl = snapshot.getValue(String.class);
                if (imageUrl != null && imageUrl.startsWith("data:image/")) {
                    // Extract the Base64 part of the data URL.
                    String base64Data = imageUrl.substring(imageUrl.indexOf(",") + 1);
                    // Check if the decoded bytes are already cached.
                    byte[] imageBytes = base64Cache.get(base64Data);
                    if (imageBytes == null) {
                        imageBytes = Base64.decode(base64Data, Base64.DEFAULT);
                        base64Cache.put(base64Data, imageBytes);
                    }
                    // Decode the byte array into a Bitmap and set it to the ImageView.
                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
                    photoImageView.setImageBitmap(decodedBitmap);
                }
                // Hide the progress bar once data is loaded
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle potential errors and hide progress bar in case of error
                progressBar.setVisibility(View.GONE);
            }
        });

        return view;
    }
}
