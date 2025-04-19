package com.example.resort;

import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class FragmentTwo extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        /// Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_two, container, false);

        /// Find the button by its ID
        Button myButton = view.findViewById(R.id.button);
        TextView login = view.findViewById(R.id.loginview);

        /// Set a click listener on the button
        myButton.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), SignUpNext.class);
            startActivity(intent);
        });

        /// Set a click listener on the button
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), Login.class);
                startActivity(intent);

            }
        });

        return view;
    }

}
