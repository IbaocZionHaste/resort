//package com.example.resort;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Spinner;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.hbb20.CountryCodePicker;
//
//public class SignUpNext extends AppCompatActivity {
//
//    private EditText etLastName, etFirstName, etMI, etBarangay, etMunicipality, etProvince, etStreet, etAge, etPhone;
//    private Spinner genderSpinner;
//    private CountryCodePicker ccp;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        EdgeToEdge.enable(this);
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_sign_up_next);
//
//        // Initialize views (IDs must match your XML layout)
//        etLastName = findViewById(R.id.editTextText6);
//        etFirstName = findViewById(R.id.editTextText15);
//        etMI = findViewById(R.id.editTextText11);
//        etBarangay = findViewById(R.id.editTextText31);
//        etMunicipality = findViewById(R.id.editTextText30);
//        etProvince = findViewById(R.id.editTextText33);
//        etStreet = findViewById(R.id.editTextText32);
//        etAge = findViewById(R.id.editTextTextPassword);
//        etPhone = findViewById(R.id.editTextTextPassword2);
//        genderSpinner = findViewById(R.id.genderSpinner);
//        ccp = findViewById(R.id.ccp);
//        Button btnNextPersonal = findViewById(R.id.button);
//
//        // Initialize and set up Spinner
//        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
//                this, R.array.gender_options, android.R.layout.simple_spinner_item);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        genderSpinner.setAdapter(adapter);
//
//        btnNextPersonal.setOnClickListener(v -> {
//            // Validate that all required fields are filled
//            if (TextUtils.isEmpty(etLastName.getText().toString().trim()) ||
//                    TextUtils.isEmpty(etFirstName.getText().toString().trim()) ||
//                    TextUtils.isEmpty(etBarangay.getText().toString().trim()) ||
//                    TextUtils.isEmpty(etMunicipality.getText().toString().trim()) ||
//                    TextUtils.isEmpty(etProvince.getText().toString().trim()) ||
//                    TextUtils.isEmpty(etStreet.getText().toString().trim()) ||
//                    TextUtils.isEmpty(etAge.getText().toString().trim()) ||
//                    TextUtils.isEmpty(etPhone.getText().toString().trim())) {
//                Toast.makeText(SignUpNext.this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Create an Intent to go to the account creation activity
//            Intent intent = new Intent(SignUpNext.this, SignUp.class);
//
//            // Pass personal data as extras
//            intent.putExtra("lastName", etLastName.getText().toString().trim());
//            intent.putExtra("firstName", etFirstName.getText().toString().trim());
//            intent.putExtra("middleInitial", etMI.getText().toString().trim());
//            intent.putExtra("barangay", etBarangay.getText().toString().trim());
//            intent.putExtra("municipality", etMunicipality.getText().toString().trim());
//            intent.putExtra("province", etProvince.getText().toString().trim());
//            intent.putExtra("street", etStreet.getText().toString().trim());
//            intent.putExtra("age", etAge.getText().toString().trim());
//
//            // Combine country code with phone number
//            String phoneCombined = ccp.getSelectedCountryCodeWithPlus() + etPhone.getText().toString().trim();
//            intent.putExtra("phoneNumber", phoneCombined);
//            intent.putExtra("gender", genderSpinner.getSelectedItem().toString());
//
//            startActivity(intent);
//            resetFields(); // Reset all fields after passing data
//        });
//    }
//
//    // Method to reset all fields
//    private void resetFields() {
//        etLastName.setText("");
//        etFirstName.setText("");
//        etMI.setText("");
//        etBarangay.setText("");
//        etMunicipality.setText("");
//        etProvince.setText("");
//        etStreet.setText("");
//        etAge.setText("");
//        etPhone.setText("");
//        genderSpinner.setSelection(0); // Reset spinner to the first item
//        ccp.resetToDefaultCountry(); // Reset country code picker to default
//    }
//}
//


//package com.example.resort;
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.AdapterView;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Spinner;
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//import com.hbb20.CountryCodePicker;
//
//public class SignUpNext extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_sign_up_next);
//
//        // Initialize Country Code Picker (CCP)
//        CountryCodePicker ccp = findViewById(R.id.ccp);
//        EditText phoneNumberEditText = findViewById(R.id.editTextTextPassword2);
//
//        // Automatically detect the user's country
//        ccp.setAutoDetectedCountry(true);
//        ccp.setDefaultCountryUsingNameCode("PH"); // Default to Philippines
//
//        // Initialize the Spinner for Gender
//        Spinner genderSpinner = findViewById(R.id.genderSpinner);
//        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
//                this,
//                R.array.gender_options, // String array resource
//                android.R.layout.simple_spinner_item
//        );
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        genderSpinner.setAdapter(adapter);
//        genderSpinner.setSelection(0); // Default to "Select Gender"
//
//        // Handle Gender Selection
//        genderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                // Do something if needed
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                // Do nothing
//            }
//        });
//
//        // Button to proceed
//        Button myButton = findViewById(R.id.button);
//        myButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                String countryCode = ccp.getSelectedCountryCodeWithPlus(); // e.g., +63
//                String phoneNumber = phoneNumberEditText.getText().toString();
//                String fullPhoneNumber = countryCode + phoneNumber; // e.g., +631234567890
//
//                String selectedGender = genderSpinner.getSelectedItem().toString();
//                if (selectedGender.equals("Select Gender")) {
//                    selectedGender = ""; // Prevent passing hint
//                }
//
//                // Pass data to next activity
//                Intent intent = new Intent(SignUpNext.this, SignUp.class);
//                intent.putExtra("PHONE_NUMBER", fullPhoneNumber);
//                intent.putExtra("GENDER", selectedGender);
//                startActivity(intent);
//            }
//        });
//
//        // Handle insets for edge-to-edge layout
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//    }
//}

//package com.example.resort;
//
//import android.content.Intent;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.ArrayAdapter;
//import android.widget.Spinner;
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//import androidx.core.graphics.Insets;
//import androidx.core.view.ViewCompat;
//import androidx.core.view.WindowInsetsCompat;
//
//public class SignUpNext extends AppCompatActivity {
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
//        setContentView(R.layout.activity_sign_up_next);
//
//        // Add the button logic here
//        Button myButton = findViewById(R.id.button);  // Replace with your button's actual ID
//
//        // Set a click listener on the button
//        myButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                // Start another activity when the button is clicked
//                Intent intent = new Intent(SignUpNext.this, SignUp.class);  // Replace SignUpNext with the desired activity
//                startActivity(intent);
//            }
//        });
//
//        // Handle insets for edge-to-edge layout
//        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
//            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
//            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
//            return insets;
//        });
//    }
//}

package com.example.resort;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.hbb20.CountryCodePicker;

public class SignUpNext extends AppCompatActivity {

    private EditText etLastName, etFirstName, etMI, etBarangay, etMunicipality, etProvince, etStreet, etAge, etPhone;
    private Spinner genderSpinner;
    private CountryCodePicker ccp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_next);

        // Initialize views
        etLastName     = findViewById(R.id.editTextText6);
        etFirstName    = findViewById(R.id.editTextText15);
        etMI           = findViewById(R.id.editTextText11);
        etBarangay     = findViewById(R.id.editTextText31);
        etMunicipality = findViewById(R.id.editTextText30);
        etProvince     = findViewById(R.id.editTextText33);
        etStreet       = findViewById(R.id.editTextText32);
        etAge          = findViewById(R.id.editTextTextPassword);
        etPhone        = findViewById(R.id.editTextTextPassword2);
        genderSpinner  = findViewById(R.id.genderSpinner);
        ccp            = findViewById(R.id.ccp);
        Button btnNextPersonal = findViewById(R.id.button);

        // Setup the gender spinner adapter
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        // No manual onFocusChangeListeners are added now.
        // The view will only scroll if the keyboard appears, thanks to adjustResize.

        btnNextPersonal.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Validate required fields
                if (TextUtils.isEmpty(etLastName.getText().toString().trim()) ||
                        TextUtils.isEmpty(etFirstName.getText().toString().trim()) ||
                        TextUtils.isEmpty(etBarangay.getText().toString().trim()) ||
                        TextUtils.isEmpty(etMunicipality.getText().toString().trim()) ||
                        TextUtils.isEmpty(etProvince.getText().toString().trim()) ||
                        TextUtils.isEmpty(etStreet.getText().toString().trim()) ||
                        TextUtils.isEmpty(etAge.getText().toString().trim()) ||
                        TextUtils.isEmpty(etPhone.getText().toString().trim())) {

                    Toast.makeText(SignUpNext.this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Create an Intent to go to the next activity
                Intent intent = new Intent(SignUpNext.this, SignUp.class);
                intent.putExtra("lastName", etLastName.getText().toString().trim());
                intent.putExtra("firstName", etFirstName.getText().toString().trim());
                intent.putExtra("middleInitial", etMI.getText().toString().trim());
                intent.putExtra("barangay", etBarangay.getText().toString().trim());
                intent.putExtra("municipality", etMunicipality.getText().toString().trim());
                intent.putExtra("province", etProvince.getText().toString().trim());
                intent.putExtra("street", etStreet.getText().toString().trim());
                intent.putExtra("age", etAge.getText().toString().trim());

                // Combine country code and phone number
                String phoneCombined = ccp.getSelectedCountryCodeWithPlus() + etPhone.getText().toString().trim();
                intent.putExtra("phoneNumber", phoneCombined);
                intent.putExtra("gender", genderSpinner.getSelectedItem().toString());

                startActivity(intent);
                resetFields();
            }
        });
    }

    // Method to reset all input fields after processing
    private void resetFields() {
        etLastName.setText("");
        etFirstName.setText("");
        etMI.setText("");
        etBarangay.setText("");
        etMunicipality.setText("");
        etProvince.setText("");
        etStreet.setText("");
        etAge.setText("");
        etPhone.setText("");
        genderSpinner.setSelection(0);
        ccp.resetToDefaultCountry();
    }
}

