package com.example.resort;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.resort.aboutus.data.Question;
import com.example.resort.aboutus.data.Staff;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;


public class AboutUs extends AppCompatActivity {

    /// Staff RecyclerView variables
    private RecyclerView recyclerViewStaff;
    private DatabaseReference staffDatabaseReference;
    private FirebaseRecyclerAdapter<Staff, StaffViewHolder> staffAdapter;

    /// Question RecyclerView variables
    private RecyclerView recyclerViewQuestion;
    private DatabaseReference questionDatabaseReference;
    private FirebaseRecyclerAdapter<Question, QuestionViewHolder> questionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_about_us);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Back Button Setup
        Button back = findViewById(R.id.back2);
        back.setOnClickListener(v -> onBackPressed());

        // === Set up Staff RecyclerView ===
        recyclerViewStaff = findViewById(R.id.recyclerViewStaff);
        recyclerViewStaff.setLayoutManager(new LinearLayoutManager(this));
        staffDatabaseReference = FirebaseDatabase.getInstance().getReference().child("staff");
        /// Optional: Order by timestamp if desired
        Query staffQuery = staffDatabaseReference.orderByChild("timestamp");

        FirebaseRecyclerOptions<Staff> staffOptions = new FirebaseRecyclerOptions.Builder<Staff>()
                .setQuery(staffQuery, Staff.class)
                .build();

        staffAdapter = new FirebaseRecyclerAdapter<Staff, StaffViewHolder>(staffOptions) {
            @NonNull
            @Override
            public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_staff, parent, false);
                return new StaffViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull StaffViewHolder holder, int position, @NonNull Staff model) {
                /// Bind the staff data (without click functionality)
                holder.staffNameTextView.setText(model.getStaffName());
                holder.positionTextView.setText((position + 1) + ". " + model.getPosition() + ":");
            }
        };
        recyclerViewStaff.setAdapter(staffAdapter);

        /// === Set up Question RecyclerView ===
        recyclerViewQuestion = findViewById(R.id.recyclerViewQuestion);
        recyclerViewQuestion.setLayoutManager(new LinearLayoutManager(this));
        questionDatabaseReference = FirebaseDatabase.getInstance().getReference().child("question");
        /// Optional: Order questions by timestamp if desired
        Query questionQuery = questionDatabaseReference.orderByChild("timestamp");

        FirebaseRecyclerOptions<Question> questionOptions = new FirebaseRecyclerOptions.Builder<Question>()
                .setQuery(questionQuery, Question.class)
                .build();

        questionAdapter = new FirebaseRecyclerAdapter<Question, QuestionViewHolder>(questionOptions) {
            @NonNull
            @Override
            public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_question, parent, false);
                return new QuestionViewHolder(view);
            }

            @Override
            protected void onBindViewHolder(@NonNull QuestionViewHolder holder, int position, @NonNull Question model) {
                /// Bind the question data (without click functionality)
                holder.questionTitleTextView.setText((position + 1) + ". " + model.getTitle());
                holder.questionAnswerTextView.setText("Answer: " + model.getAnswer());
            }
        };
        recyclerViewQuestion.setAdapter(questionAdapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (staffAdapter != null) {
            staffAdapter.startListening();
        }
        if (questionAdapter != null) {
            questionAdapter.startListening();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (staffAdapter != null) {
            staffAdapter.stopListening();
        }
        if (questionAdapter != null) {
            questionAdapter.stopListening();
        }
    }

    // ViewHolder for Staff items
    public static class StaffViewHolder extends RecyclerView.ViewHolder {
        TextView staffNameTextView;
        TextView positionTextView;

        public StaffViewHolder(@NonNull View itemView) {
            super(itemView);
            staffNameTextView = itemView.findViewById(R.id.staffNameTextView);
            positionTextView = itemView.findViewById(R.id.positionTextView);
        }
    }

    // ViewHolder for Question items
    public static class QuestionViewHolder extends RecyclerView.ViewHolder {
        TextView questionTitleTextView;
        TextView questionAnswerTextView;

        public QuestionViewHolder(@NonNull View itemView) {
            super(itemView);
            questionTitleTextView = itemView.findViewById(R.id.questionTitleTextView);
            questionAnswerTextView = itemView.findViewById(R.id.questionAnswerTextView);
        }
    }
}

///No Current
//package com.example.resort;
//
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//
//import androidx.activity.EdgeToEdge;
//import androidx.annotation.NonNull;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import androidx.recyclerview.widget.LinearLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.resort.aboutus.data.Question;
//import com.example.resort.aboutus.data.Staff;
//import com.firebase.ui.database.FirebaseRecyclerAdapter;
//import com.firebase.ui.database.FirebaseRecyclerOptions;
//import com.google.firebase.database.DatabaseReference;
//import com.google.firebase.database.FirebaseDatabase;
//import com.google.firebase.database.Query;
//
//
//public class AboutUs extends AppCompatActivity {
//
//    /// Staff RecyclerView variables
//    private RecyclerView recyclerViewStaff;
//    private DatabaseReference staffDatabaseReference;
//    private FirebaseRecyclerAdapter<Staff, StaffViewHolder> staffAdapter;
//
//    /// Question RecyclerView variables
//    private RecyclerView recyclerViewQuestion;
//    private DatabaseReference questionDatabaseReference;
//    private FirebaseRecyclerAdapter<Question, QuestionViewHolder> questionAdapter;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_about_us);
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//
//        // Back Button Setup
//        Button back = findViewById(R.id.back2);
//        back.setOnClickListener(v -> onBackPressed());
//
//        // === Set up Staff RecyclerView ===
//        recyclerViewStaff = findViewById(R.id.recyclerViewStaff);
//        recyclerViewStaff.setLayoutManager(new LinearLayoutManager(this));
//        staffDatabaseReference = FirebaseDatabase.getInstance().getReference().child("staff");
//        /// Optional: Order by timestamp if desired
//        Query staffQuery = staffDatabaseReference.orderByChild("timestamp");
//
//        FirebaseRecyclerOptions<Staff> staffOptions = new FirebaseRecyclerOptions.Builder<Staff>()
//                .setQuery(staffQuery, Staff.class)
//                .build();
//
//        staffAdapter = new FirebaseRecyclerAdapter<Staff, StaffViewHolder>(staffOptions) {
//            @NonNull
//            @Override
//            public StaffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//                View view = LayoutInflater.from(parent.getContext())
//                        .inflate(R.layout.item_staff, parent, false);
//                return new StaffViewHolder(view);
//            }
//
//            @Override
//            protected void onBindViewHolder(@NonNull StaffViewHolder holder, int position, @NonNull Staff model) {
//                /// Bind the staff data (without click functionality)
//                holder.staffNameTextView.setText(model.getStaffName());
//                holder.positionTextView.setText((position + 1) + ". " + model.getPosition() + ":");
//            }
//        };
//        recyclerViewStaff.setAdapter(staffAdapter);
//
//        /// === Set up Question RecyclerView ===
//        recyclerViewQuestion = findViewById(R.id.recyclerViewQuestion);
//        recyclerViewQuestion.setLayoutManager(new LinearLayoutManager(this));
//        questionDatabaseReference = FirebaseDatabase.getInstance().getReference().child("question");
//        /// Optional: Order questions by timestamp if desired
//        Query questionQuery = questionDatabaseReference.orderByChild("timestamp");
//
//        FirebaseRecyclerOptions<Question> questionOptions = new FirebaseRecyclerOptions.Builder<Question>()
//                .setQuery(questionQuery, Question.class)
//                .build();
//
//        questionAdapter = new FirebaseRecyclerAdapter<Question, QuestionViewHolder>(questionOptions) {
//            @NonNull
//            @Override
//            public QuestionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//                View view = LayoutInflater.from(parent.getContext())
//                        .inflate(R.layout.item_question, parent, false);
//                return new QuestionViewHolder(view);
//            }
//
//            @Override
//            protected void onBindViewHolder(@NonNull QuestionViewHolder holder, int position, @NonNull Question model) {
//                /// Bind the question data (without click functionality)
//                holder.questionTitleTextView.setText((position + 1) + ". " + model.getTitle());
//                holder.questionAnswerTextView.setText("Answer: " + model.getAnswer());
//            }
//        };
//        recyclerViewQuestion.setAdapter(questionAdapter);
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();
//        if (staffAdapter != null) {
//            staffAdapter.startListening();
//        }
//        if (questionAdapter != null) {
//            questionAdapter.startListening();
//        }
//    }
//
//    @Override
//    protected void onStop() {
//        super.onStop();
//        if (staffAdapter != null) {
//            staffAdapter.stopListening();
//        }
//        if (questionAdapter != null) {
//            questionAdapter.stopListening();
//        }
//    }
//
//    // ViewHolder for Staff items
//    public static class StaffViewHolder extends RecyclerView.ViewHolder {
//        TextView staffNameTextView;
//        TextView positionTextView;
//
//        public StaffViewHolder(@NonNull View itemView) {
//            super(itemView);
//            staffNameTextView = itemView.findViewById(R.id.staffNameTextView);
//            positionTextView = itemView.findViewById(R.id.positionTextView);
//        }
//    }
//
//    // ViewHolder for Question items
//    public static class QuestionViewHolder extends RecyclerView.ViewHolder {
//        TextView questionTitleTextView;
//        TextView questionAnswerTextView;
//
//        public QuestionViewHolder(@NonNull View itemView) {
//            super(itemView);
//            questionTitleTextView = itemView.findViewById(R.id.questionTitleTextView);
//            questionAnswerTextView = itemView.findViewById(R.id.questionAnswerTextView);
//        }
//    }
//}
