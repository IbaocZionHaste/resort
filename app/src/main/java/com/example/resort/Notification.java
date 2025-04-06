package com.example.resort;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.notification.data.NotificationAdapter;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Notification extends AppCompatActivity {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Map<String, Object>> notificationList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_notification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back button functionality.
        Button back = findViewById(R.id.back2);
        back.setOnClickListener(v -> onBackPressed());

        // Setup RecyclerView with a reversed LinearLayoutManager to show new notifications at the top.
        rvNotifications = findViewById(R.id.rvNotifications);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setReverseLayout(true);    // Reverse the layout order
        layoutManager.setStackFromEnd(true);       // Start stacking from the end
        rvNotifications.setLayoutManager(layoutManager);

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        // Attach swipe-to-delete functionality.
        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // Not supporting move action.
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();

                // Inflate the custom alert layout.
                AlertDialog.Builder builder = new AlertDialog.Builder(Notification.this);
                View customView = getLayoutInflater().inflate(R.layout.custom_notification_dialog, null);
                builder.setView(customView);

                // Create the dialog and make it non-cancelable.
                AlertDialog dialog = builder.create();
                dialog.setCancelable(false);

                // Get references to the buttons in the custom layout.
                Button btnCancel = customView.findViewById(R.id.btnCancel);
                Button btnDelete = customView.findViewById(R.id.btnDelete);

                // Set up the "Delete" button action.
                btnDelete.setOnClickListener(v -> {
                    // Get the unique key from the notification map.
                    Map<String, Object> notify = notificationList.get(position);
                    String notificationId = (String) notify.get("notificationId");
                    if (notificationId != null) {
                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (currentUser != null) {
                            String userId = currentUser.getUid();
                            DatabaseReference ref = FirebaseDatabase.getInstance()
                                    .getReference("users")
                                    .child(userId)
                                    .child("notifications")
                                    .child(notificationId);
                            ref.removeValue();
                        }
                    }
                    // Remove item from local list and notify adapter.
                    notificationList.remove(position);
                    adapter.notifyItemRemoved(position);
                    dialog.dismiss();
                });

                // Set up the "Cancel" button action.
                btnCancel.setOnClickListener(v -> {
                    // Cancel deletion: notify adapter to rebind the item.
                    adapter.notifyItemChanged(position);
                    dialog.dismiss();
                });

                // Show the custom alert dialog.
                dialog.show();
            }
        };

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
        itemTouchHelper.attachToRecyclerView(rvNotifications);

        // Load notifications from Firebase.
        loadNotificationsFromFirebase();
    }

    /**
     * Loads notifications from Firebase under the current user's "notifications" node.
     * Also, adds the unique key (notificationId) from the snapshot to each notification map.
     */
    private void loadNotificationsFromFirebase() {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }
        String userId = currentUser.getUid();
        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(userId)
                .child("notifications");

        notificationRef.addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();
                for (DataSnapshot notifySnapshot : snapshot.getChildren()) {
                    // Each notification is stored as a Map.
                    // Add the unique key to the map.
                    //noinspection unchecked
                    Map<String, Object> notification = (Map<String, Object>) notifySnapshot.getValue();
                    if (notification != null) {
                        notification.put("notificationId", notifySnapshot.getKey());
                        notificationList.add(notification);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(Notification.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
            }
        });
    }
}

///This code the message the new is down and the old is top
//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.recyclerview.widget.ItemTouchHelper;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.notification.data.NotificationAdapter;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.DataSnapshot;
//import com.google.firebase.database.DatabaseError;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.ValueEventListener;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//public class Notification extends AppCompatActivity {
//
//    private RecyclerView rvNotifications;
//    private NotificationAdapter adapter;
//    private List<Map<String, Object>> notificationList;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_notification);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Back button functionality.
//        Button back = findViewById(R.id.back2);
//        back.setOnClickListener(v -> onBackPressed());
//
//        // Setup RecyclerView.
//        rvNotifications = findViewById(R.id.rvNotifications);
//        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
//        notificationList = new ArrayList<>();
//        adapter = new NotificationAdapter(notificationList);
//        rvNotifications.setAdapter(adapter);
//
//        // Attach swipe-to-delete functionality.
//        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
//            @Override
//            public boolean onMove(@NonNull RecyclerView recyclerView,
//                                  @NonNull RecyclerView.ViewHolder viewHolder,
//                                  @NonNull RecyclerView.ViewHolder target) {
//                // Not supporting move action.
//                return false;
//            }
//
//            @Override
//            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
//                int position = viewHolder.getAdapterPosition();
//
//                // Inflate the custom alert layout.
//                AlertDialog.Builder builder = new AlertDialog.Builder(Notification.this);
//                View customView = getLayoutInflater().inflate(R.layout.custom_notification_dialog, null);
//                builder.setView(customView);
//
//                // Create the dialog and make it non-cancelable.
//                AlertDialog dialog = builder.create();
//                dialog.setCancelable(false);
//
//                // Get references to the buttons in the custom layout.
//                Button btnCancel = customView.findViewById(R.id.btnCancel);
//                Button btnDelete = customView.findViewById(R.id.btnDelete);
//
//                // Set up the "Delete" button action.
//                btnDelete.setOnClickListener(v -> {
//                    // Get the unique key from the notification map.
//                    Map<String, Object> notify = notificationList.get(position);
//                    String notificationId = (String) notify.get("notificationId");
//                    if (notificationId != null) {
//                        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//                        if (currentUser != null) {
//                            String userId = currentUser.getUid();
//                            DatabaseReference ref = FirebaseDatabase.getInstance()
//                                    .getReference("users")
//                                    .child(userId)
//                                    .child("notifications")
//                                    .child(notificationId);
//                            ref.removeValue();
//                        }
//                    }
//                    // Remove item from local list and notify adapter.
//                    notificationList.remove(position);
//                    adapter.notifyItemRemoved(position);
//                    dialog.dismiss();
//                });
//
//                // Set up the "Cancel" button action.
//                btnCancel.setOnClickListener(v -> {
//                    // Cancel deletion: notify adapter to rebind the item.
//                    adapter.notifyItemChanged(position);
//                    dialog.dismiss();
//                });
//
//                // Show the custom alert dialog.
//                dialog.show();
//            }
//        };
//
//        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleCallback);
//        itemTouchHelper.attachToRecyclerView(rvNotifications);
//
//        // Load notifications from Firebase.
//        loadNotificationsFromFirebase();
//    }
//
//    /**
//     * Loads notifications from Firebase under the current user's "notifications" node.
//     * Also, adds the unique key (notificationId) from the snapshot to each notification map.
//     */
//    private void loadNotificationsFromFirebase() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        String userId = currentUser.getUid();
//        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("notifications");
//
//        notificationRef.addValueEventListener(new ValueEventListener() {
//            @SuppressLint("NotifyDataSetChanged")
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                notificationList.clear();
//                for (DataSnapshot notifySnapshot : snapshot.getChildren()) {
//                    // Each notification is stored as a Map.
//                    // Add the unique key to the map.
//                    //noinspection unchecked
//                    Map<String, Object> notification = (Map<String, Object>) notifySnapshot.getValue();
//                    if (notification != null) {
//                        notification.put("notificationId", notifySnapshot.getKey());
//                        notificationList.add(notification);
//                    }
//                }
//                adapter.notifyDataSetChanged();
//            }
//
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(Notification.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//}



//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AlertDialog;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.ItemTouchHelper;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import com.example.resort.R;
//import com.example.resort.notification.data.NotificationAdapter;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.*;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//public class Notification extends AppCompatActivity {
//
//    private RecyclerView rvNotifications;
//    private NotificationAdapter adapter;
//    private List<Map<String, Object>> notificationList;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_notification);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Back button functionality.
//        Button back = findViewById(R.id.back2);
//        back.setOnClickListener(v -> onBackPressed());
//
//        // Setup RecyclerView.
//        rvNotifications = findViewById(R.id.rvNotifications);
//        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
//        notificationList = new ArrayList<>();
//        adapter = new NotificationAdapter(notificationList);
//        rvNotifications.setAdapter(adapter);
//
//        // Attach swipe-to-delete functionality.
//        ItemTouchHelper.SimpleCallback simpleCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
//            @Override
//            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
//                // Not supporting move action.
//                return false;
//            }
//            @Override
//            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
//                int position = viewHolder.getAdapterPosition();
//                // Show confirmation dialog.
//                new AlertDialog.Builder(Notification.this)
//                        .setTitle("Delete Notification")
//                        .setMessage("Are you sure you want to delete this notification?")
//                        .setPositiveButton("Delete", (dialog, which) -> {
//                            // Get the unique key from the notification map.
//                            Map<String, Object> notify = notificationList.get(position);
//                            String notificationId = (String) notify.get("notificationId");
//                            if (notificationId != null) {
//                                FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//                                if (currentUser != null) {
//                                    String userId = currentUser.getUid();
//                                    DatabaseReference ref = FirebaseDatabase.getInstance()
//                                            .getReference("users")
//                                            .child(userId)
//                                            .child("notifications")
//                                            .child(notificationId);
//                                    ref.removeValue();
//                                }
//                            }
//                            // Remove item from local list and notify adapter.
//                            notificationList.remove(position);
//                            adapter.notifyItemRemoved(position);
//                        })
//                        .setNegativeButton("Cancel", (dialog, which) -> {
//                            // Cancel deletion: notify adapter to rebind the item.
//                            adapter.notifyItemChanged(position);
//                            dialog.dismiss();
//                        })
//                        .setCancelable(false)
//                        .show();
//            }
//        };
//        new ItemTouchHelper(simpleCallback).attachToRecyclerView(rvNotifications);
//
//        // Load notifications from Firebase.
//        loadNotificationsFromFirebase();
//    }
//
//    /**
//     * Loads notifications from Firebase under the current user's "notifications" node.
//     * Also, adds the unique key (notificationId) from the snapshot to each notification map.
//     */
//    private void loadNotificationsFromFirebase() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        String userId = currentUser.getUid();
//        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("notifications");
//
//        notificationRef.addValueEventListener(new ValueEventListener() {
//            @SuppressLint("NotifyDataSetChanged")
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                notificationList.clear();
//                for (DataSnapshot notifySnapshot : snapshot.getChildren()) {
//                    // Each notification is stored as a Map.
//                    // Add the unique key to the map.
//                    //noinspection unchecked
//                    Map<String, Object> notification = (Map<String, Object>) notifySnapshot.getValue();
//                    if (notification != null) {
//                        notification.put("notificationId", notifySnapshot.getKey());
//                        notificationList.add(notification);
//                    }
//                }
//                adapter.notifyDataSetChanged();
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(Notification.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//}
//



//package com.example.resort;
//
//import android.annotation.SuppressLint;
//import android.os.Bundle;
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//import com.example.resort.notification.data.NotificationAdapter;
//import com.google.firebase.auth.FirebaseAuth;
//import com.google.firebase.auth.FirebaseUser;
//import com.google.firebase.database.*;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//
//public class Notification extends AppCompatActivity {
//
//    private RecyclerView rvNotifications;
//    private NotificationAdapter adapter;
//    private List<Map<String, Object>> notificationList;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_notification);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Back button functionality.
//        Button back = findViewById(R.id.back2);
//        back.setOnClickListener(v -> onBackPressed());
//
//        // Setup RecyclerView.
//        rvNotifications = findViewById(R.id.rvNotifications);
//        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
//        notificationList = new ArrayList<>();
//        adapter = new NotificationAdapter(notificationList);
//        rvNotifications.setAdapter(adapter);
//
//        // Load notifications from Firebase.
//        loadNotificationsFromFirebase();
//    }
//
//    /**
//     * Loads notifications from Firebase under the current user's "notifications" node.
//     */
//    private void loadNotificationsFromFirebase() {
//        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
//        if (currentUser == null) {
//            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        String userId = currentUser.getUid();
//        DatabaseReference notificationRef = FirebaseDatabase.getInstance()
//                .getReference("users")
//                .child(userId)
//                .child("notifications");
//
//        notificationRef.addValueEventListener(new ValueEventListener() {
//            @SuppressLint("NotifyDataSetChanged")
//            @Override
//            public void onDataChange(@NonNull DataSnapshot snapshot) {
//                notificationList.clear();
//                for (DataSnapshot notifySnapshot : snapshot.getChildren()) {
//                    // Each notification is expected to be stored as a Map with keys "message" and "timestamp".
//                    //noinspection unchecked
//                    Map<String, Object> notification = (Map<String, Object>) notifySnapshot.getValue();
//                    if (notification != null) {
//                        notificationList.add(notification );
//                    }
//                }
//                adapter.notifyDataSetChanged();
//            }
//            @Override
//            public void onCancelled(@NonNull DatabaseError error) {
//                Toast.makeText(Notification.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//}
