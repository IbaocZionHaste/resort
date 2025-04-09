package com.example.resort;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.feedback.data.MyRatingAdapter;
import com.example.resort.feedback.data.RatingItem;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class Feedback extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MyRatingAdapter adapter;
    private List<RatingItem> ratingItemList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_feedback);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Next layout function via fab2.
        FloatingActionButton nextFab = findViewById(R.id.fab2);
        nextFab.setOnClickListener(v -> {
            // Start the next activity.
            Intent intent = new Intent(Feedback.this, Comment.class);
            startActivity(intent);
        });

        // Back button functionality.
        Button back = findViewById(R.id.back2);
        back.setOnClickListener(v -> onBackPressed());

        // Setup RecyclerView with reversed layout to show newest items on top.
        recyclerView = findViewById(R.id.recyclerViewFeedback);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);    /// Reverse the order.
        layoutManager.setStackFromEnd(true);     /// Stack items from the end.
        recyclerView.setLayoutManager(layoutManager);

        adapter = new MyRatingAdapter(ratingItemList);
        recyclerView.setAdapter(adapter);

        // Fetch MyRating data from Firebase.
        fetchMyRatings();
    }

    private void fetchMyRatings() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference ratingRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("MyRating");

        ratingRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                ratingItemList.clear();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    RatingItem item = ds.getValue(RatingItem.class);
                    if (item != null) {
                        ratingItemList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(Feedback.this, "Error fetching ratings", Toast.LENGTH_SHORT).show();
            }
        });
    }
}




///This code the message the new is down and the old is top
//package com.example.resort;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.feedback.data.MyRatingAdapter;
//import com.example.resort.feedback.data.RatingItem;
//import com.google.android.material.floatingactionbutton.FloatingActionButton;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.ArrayList;
//import java.util.List;
//
//public class Feedback extends AppCompatActivity {
//
//    private RecyclerView recyclerView;
//    private MyRatingAdapter adapter;
//    private List<RatingItem> ratingItemList = new ArrayList<>();
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_feedback);
//
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Next layout function via fab2
//        FloatingActionButton nextFab = findViewById(R.id.fab2);
//        nextFab.setOnClickListener(v -> {
//            // Start the next activity
//            Intent intent = new Intent(Feedback.this, Comment.class);
//            startActivity(intent);
//        });
//
//        // Back button functionality.
//        Button back = findViewById(R.id.back2);
//        back.setOnClickListener(v -> onBackPressed());
//
//        // Setup RecyclerView.
//        recyclerView = findViewById(R.id.recyclerViewFeedback);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//        adapter = new MyRatingAdapter(ratingItemList);
//        recyclerView.setAdapter(adapter);
//
//        // Fetch MyRating data from Firebase.
//        fetchMyRatings();
//    }
//
//    private void fetchMyRatings() {
//        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
//        DatabaseReference ratingRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("MyRating");
//
//        ratingRef.addValueEventListener(new ValueEventListener() {
//            @Override
//            public void onDataChange(DataSnapshot snapshot) {
//                ratingItemList.clear();
//                for (DataSnapshot ds : snapshot.getChildren()) {
//                    RatingItem item = ds.getValue(RatingItem.class);
//                    if (item != null) {
//                        ratingItemList.add(item);
//                    }
//                }
//                adapter.notifyDataSetChanged();
//            }
//            @Override
//            public void onCancelled(DatabaseError error) {
//                Toast.makeText(Feedback.this, "Error fetching ratings", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//}
//

///Default
//package com.example.resort;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.google.android.material.floatingactionbutton.FloatingActionButton;
//
//public class Feedback extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_feedback);
//
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Back button functionality
//        Button back = findViewById(R.id.back2); // Make sure this ID matches your XML
//        back.setOnClickListener(v -> {
//            // Go back to the previous activity
//            onBackPressed();
//        });
//
//        // Next layout function via fab2
//        FloatingActionButton nextFab = findViewById(R.id.fab2);
//        nextFab.setOnClickListener(v -> {
//            // Start the next activity
//            Intent intent = new Intent(Feedback.this, Comment.class);
//            startActivity(intent);
//        });
//    }
//}
