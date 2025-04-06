package com.example.resort.home.data;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;

import androidx.appcompat.app.AppCompatActivity;

import com.example.resort.R;
import com.github.chrisbanes.photoview.PhotoView;

public class ImageZoomActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_zoom);

        PhotoView photoView = findViewById(R.id.photo_view);

        // Get the Base64 string from the intent
        String base64Image = getIntent().getStringExtra("BASE64_IMAGE");
        if (base64Image != null && base64Image.contains(",")) {
            String encodedImage = base64Image.split(",")[1];
            byte[] decodedString = Base64.decode(encodedImage, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
            photoView.setImageBitmap(bitmap);
        } else {
            // Handle error or set a default image
            photoView.setImageResource(R.drawable.ic_no_image);
        }
    }
}
