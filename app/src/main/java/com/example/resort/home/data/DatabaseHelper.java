package com.example.resort.home.data;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper {
    private DatabaseReference promotionsRef;

    public interface DataStatus {
        void DataLoaded(List<Promotion> promotions);
        void Error(String message);
    }

    public DatabaseHelper() {
        promotionsRef = FirebaseDatabase.getInstance().getReference("promotions");
    }

    public void fetchPromotions(final DataStatus dataStatus) {
        promotionsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Promotion> promotionsList = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Promotion promo = dataSnapshot.getValue(Promotion.class);
                    promotionsList.add(promo);
                }
                dataStatus.DataLoaded(promotionsList);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                dataStatus.Error(error.getMessage());
            }
        });
    }
}

