package com.example.resort;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import com.example.resort.history.data.BookingAdapter;
import com.example.resort.history.data.BookingData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class BookingHistory extends AppCompatActivity {
    private RecyclerView recyclerView;
    private BookingAdapter adapter;
    private List<BookingData> bookingList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_booking_history);

        recyclerView = findViewById(R.id.recyclerViewFeedback);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);     /// Reverse the order.
        layoutManager.setStackFromEnd(true);      /// Stack items from the end.
        recyclerView.setLayoutManager(layoutManager);
        ///recyclerView.setLayoutManager(new LinearLayoutManager(this));

        bookingList = new ArrayList<>();
        adapter = new BookingAdapter(this, bookingList);
        recyclerView.setAdapter(adapter);

        loadBookingData();

        // Back button functionality.
        Button back = findViewById(R.id.back2);
        back.setOnClickListener(v -> onBackPressed());
    }

    private void loadBookingData() {
        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
                .child(userId)
                .child("MyHistory");

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                bookingList.clear();
                for (DataSnapshot historySnap : snapshot.getChildren()) {
                    BookingData booking = historySnap.getValue(BookingData.class);
                    if (booking != null) {
                        /// Optionally, set the Firebase key as the ID.
                        booking.setId(historySnap.getKey());
                        bookingList.add(booking);
                    }
                }
                adapter.notifyDataSetChanged();
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                /// Handle errors here
            }
        });
    }
}


///No Current User
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.os.Bundle;
//import android.widget.Button;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//
//import com.example.resort.history.data.BookingAdapter;
//import com.example.resort.history.data.BookingData;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Objects;
//
//public class BookingHistory extends AppCompatActivity {
//    private RecyclerView recyclerView;
//    private BookingAdapter adapter;
//    private List<BookingData> bookingList;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        EdgeToEdge.enable(this);
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_booking_history);
//
//        recyclerView = findViewById(R.id.recyclerViewFeedback);
//        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        layoutManager.setReverseLayout(true);     /// Reverse the order.
//        layoutManager.setStackFromEnd(true);      /// Stack items from the end.
//        recyclerView.setLayoutManager(layoutManager);
//        ///recyclerView.setLayoutManager(new LinearLayoutManager(this));
//
//        bookingList = new ArrayList<>();
//        adapter = new BookingAdapter(this, bookingList);
//        recyclerView.setAdapter(adapter);
//
//        loadBookingData();
//
//        // Back button functionality.
//        Button back = findViewById(R.id.back2);
//        back.setOnClickListener(v -> onBackPressed());
//    }
//
//    private void loadBookingData() {
//        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
//        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users")
//                .child(userId)
//                .child("MyHistory");
//
//        ref.addListenerForSingleValueEvent(new ValueEventListener() {
//            @SuppressLint("NotifyDataSetChanged")
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                bookingList.clear();
//                for (DataSnapshot historySnap : snapshot.getChildren()) {
//                    BookingData booking = historySnap.getValue(BookingData.class);
//                    if (booking != null) {
//                        /// Optionally, set the Firebase key as the ID.
//                        booking.setId(historySnap.getKey());
//                        bookingList.add(booking);
//                    }
//                }
//                adapter.notifyDataSetChanged();
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                /// Handle errors here
//            }
//        });
//    }
//}
