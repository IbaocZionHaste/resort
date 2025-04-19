package com.example.resort;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.hbb20.CountryCodePicker;

import java.util.Arrays;
import java.util.List;

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

        /// —— NEW: Setup spinner with “Select gender”, “Male”, “Female” in BLACK text
        setupGenderSpinner();

        /// Button click listener
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
                        TextUtils.isEmpty(etPhone.getText().toString().trim()) ||
                        genderSpinner.getSelectedItemPosition() == 0) {  // ensure gender picked

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

                /// Format phone number properly for PH numbers
                String rawPhone = etPhone.getText().toString().trim();
                String formattedPhone = formatPhoneNumber(rawPhone);
                String phoneCombined = ccp.getSelectedCountryCodeWithPlus() + formattedPhone;
                intent.putExtra("phoneNumber", phoneCombined);
                intent.putExtra("gender", genderSpinner.getSelectedItem().toString());
                overridePendingTransition(0, 0);
                startActivity(intent);
                resetFields();
            }
        });
    }


    /// Function to clean PH phone number input
    private String formatPhoneNumber(String phone) {
        /// Remove all spaces, dashes, parentheses just in case
        phone = phone.replaceAll("[^\\d+]", "");

        /// If starts with +63, strip it
        if (phone.startsWith("+63")) {
            return phone.substring(3);
        }

        /// If starts with 63, strip it
        if (phone.startsWith("63")) {
            return phone.substring(2);
        }

        /// If starts with 0 (like 09xxxxxxxxx), remove the 0
        if (phone.startsWith("0")) {
            return phone.substring(1);
        }

        // Else return as-is
        return phone;
    }


    /// Set up spinner items and force black text
    private void setupGenderSpinner() {
        List<String> genders = Arrays.asList("Select Gender", "Male", "Female");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                genders
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(Color.BLACK);
                tv.setTextSize(17);
                tv.setHintTextColor(Color.GRAY);
                return tv;
            }

            @Override
            public View getDropDownView(int position, View convertView, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
                tv.setTextColor(Color.BLACK);
                tv.setBackgroundColor(Color.WHITE);
                return tv;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);
        genderSpinner.setPrompt("Select Gender");
    }

    /// Method to reset all input fields after processing
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



///Fix Current
//package com.example.resort;
//
//import android.content.Intent;
//import android.graphics.Color;
//import android.graphics.Typeface;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Spinner;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.hbb20.CountryCodePicker;
//
//import java.util.Arrays;
//import java.util.List;
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
//        // Initialize views
//        etLastName     = findViewById(R.id.editTextText6);
//        etFirstName    = findViewById(R.id.editTextText15);
//        etMI           = findViewById(R.id.editTextText11);
//        etBarangay     = findViewById(R.id.editTextText31);
//        etMunicipality = findViewById(R.id.editTextText30);
//        etProvince     = findViewById(R.id.editTextText33);
//        etStreet       = findViewById(R.id.editTextText32);
//        etAge          = findViewById(R.id.editTextTextPassword);
//        etPhone        = findViewById(R.id.editTextTextPassword2);
//        genderSpinner  = findViewById(R.id.genderSpinner);
//        ccp            = findViewById(R.id.ccp);
//        Button btnNextPersonal = findViewById(R.id.button);
//
//        /// —— NEW: Setup spinner with “Select gender”, “Male”, “Female” in BLACK text
//        setupGenderSpinner();
//
//        /// Button click listener
//        btnNextPersonal.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                // Validate required fields
//                if (TextUtils.isEmpty(etLastName.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etFirstName.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etBarangay.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etMunicipality.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etProvince.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etStreet.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etAge.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etPhone.getText().toString().trim()) ||
//                        genderSpinner.getSelectedItemPosition() == 0) {  // ensure gender picked
//
//                    Toast.makeText(SignUpNext.this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                // Create an Intent to go to the next activity
//                Intent intent = new Intent(SignUpNext.this, SignUp.class);
//                intent.putExtra("lastName", etLastName.getText().toString().trim());
//                intent.putExtra("firstName", etFirstName.getText().toString().trim());
//                intent.putExtra("middleInitial", etMI.getText().toString().trim());
//                intent.putExtra("barangay", etBarangay.getText().toString().trim());
//                intent.putExtra("municipality", etMunicipality.getText().toString().trim());
//                intent.putExtra("province", etProvince.getText().toString().trim());
//                intent.putExtra("street", etStreet.getText().toString().trim());
//                intent.putExtra("age", etAge.getText().toString().trim());
//
//                /// Format phone number properly for PH numbers
//                String rawPhone = etPhone.getText().toString().trim();
//                String formattedPhone = formatPhoneNumber(rawPhone);
//                String phoneCombined = ccp.getSelectedCountryCodeWithPlus() + formattedPhone;
//                intent.putExtra("phoneNumber", phoneCombined);
//                intent.putExtra("gender", genderSpinner.getSelectedItem().toString());
//                overridePendingTransition(0, 0);
//                startActivity(intent);
//                resetFields();
//            }
//        });
//    }
//
//
//    /// Function to clean PH phone number input
//    private String formatPhoneNumber(String phone) {
//        /// Remove all spaces, dashes, parentheses just in case
//        phone = phone.replaceAll("[^\\d+]", "");
//
//        /// If starts with +63, strip it
//        if (phone.startsWith("+63")) {
//            return phone.substring(3);
//        }
//
//        /// If starts with 63, strip it
//        if (phone.startsWith("63")) {
//            return phone.substring(2);
//        }
//
//        /// If starts with 0 (like 09xxxxxxxxx), remove the 0
//        if (phone.startsWith("0")) {
//            return phone.substring(1);
//        }
//
//        // Else return as-is
//        return phone;
//    }
//
//
//    /// Set up spinner items and force black text
//    private void setupGenderSpinner() {
//        List<String> genders = Arrays.asList("Select Gender", "Male", "Female");
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
//                this,
//                android.R.layout.simple_spinner_item,
//                genders
//        ) {
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                TextView tv = (TextView) super.getView(position, convertView, parent);
//                tv.setTextColor(Color.BLACK);
//                tv.setTextSize(17);
//                tv.setHintTextColor(Color.GRAY);
//                return tv;
//            }
//
//            @Override
//            public View getDropDownView(int position, View convertView, ViewGroup parent) {
//                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
//                tv.setTextColor(Color.BLACK);
//                tv.setBackgroundColor(Color.WHITE);
//                return tv;
//            }
//        };
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        genderSpinner.setAdapter(adapter);
//        genderSpinner.setPrompt("Select Gender");
//    }
//
//    /// Method to reset all input fields after processing
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
//        genderSpinner.setSelection(0);
//        ccp.resetToDefaultCountry();
//    }
//}
//


///Get Current fix
//package com.example.resort;
//
//import android.content.Intent;
//import android.graphics.Color;
//import android.graphics.Typeface;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Spinner;
//import android.widget.TextView;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
//
//import com.hbb20.CountryCodePicker;
//
//import java.util.Arrays;
//import java.util.List;
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
//        // Initialize views
//        etLastName     = findViewById(R.id.editTextText6);
//        etFirstName    = findViewById(R.id.editTextText15);
//        etMI           = findViewById(R.id.editTextText11);
//        etBarangay     = findViewById(R.id.editTextText31);
//        etMunicipality = findViewById(R.id.editTextText30);
//        etProvince     = findViewById(R.id.editTextText33);
//        etStreet       = findViewById(R.id.editTextText32);
//        etAge          = findViewById(R.id.editTextTextPassword);
//        etPhone        = findViewById(R.id.editTextTextPassword2);
//        genderSpinner  = findViewById(R.id.genderSpinner);
//        ccp            = findViewById(R.id.ccp);
//        Button btnNextPersonal = findViewById(R.id.button);
//
//        // —— NEW: Setup spinner with “Select gender”, “Male”, “Female” in BLACK text
//        setupGenderSpinner();
//
//        // Button click listener
//        btnNextPersonal.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//
//                // Validate required fields
//                if (TextUtils.isEmpty(etLastName.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etFirstName.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etBarangay.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etMunicipality.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etProvince.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etStreet.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etAge.getText().toString().trim()) ||
//                        TextUtils.isEmpty(etPhone.getText().toString().trim()) ||
//                        genderSpinner.getSelectedItemPosition() == 0) {  // ensure gender picked
//
//                    Toast.makeText(SignUpNext.this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
//                    return;
//                }
//
//                // Create an Intent to go to the next activity
//                Intent intent = new Intent(SignUpNext.this, SignUp.class);
//                intent.putExtra("lastName", etLastName.getText().toString().trim());
//                intent.putExtra("firstName", etFirstName.getText().toString().trim());
//                intent.putExtra("middleInitial", etMI.getText().toString().trim());
//                intent.putExtra("barangay", etBarangay.getText().toString().trim());
//                intent.putExtra("municipality", etMunicipality.getText().toString().trim());
//                intent.putExtra("province", etProvince.getText().toString().trim());
//                intent.putExtra("street", etStreet.getText().toString().trim());
//                intent.putExtra("age", etAge.getText().toString().trim());
//
//                // Combine country code and phone number
//                String phoneCombined = ccp.getSelectedCountryCodeWithPlus() + etPhone.getText().toString().trim();
//                intent.putExtra("phoneNumber", phoneCombined);
//                intent.putExtra("gender", genderSpinner.getSelectedItem().toString());
//                overridePendingTransition(0, 0);
//                startActivity(intent);
//                resetFields();
//            }
//        });
//    }
//
//    /// Set up spinner items and force black text
//    private void setupGenderSpinner() {
//        List<String> genders = Arrays.asList("Select gender", "Male", "Female");
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
//                this,
//                android.R.layout.simple_spinner_item,
//                genders
//        ) {
//            @Override
//            public View getView(int position, View convertView, ViewGroup parent) {
//                TextView tv = (TextView) super.getView(position, convertView, parent);
//                tv.setTextColor(Color.BLACK);
//                tv.setTextSize(18);
//                tv.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
//                return tv;
//            }
//
//            @Override
//            public View getDropDownView(int position, View convertView, ViewGroup parent) {
//                TextView tv = (TextView) super.getDropDownView(position, convertView, parent);
//                tv.setTextColor(Color.BLACK);
//                tv.setBackgroundColor(Color.WHITE);
//                return tv;
//            }
//        };
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        genderSpinner.setAdapter(adapter);
//        genderSpinner.setPrompt("Select gender");
//    }
//
//    /// Method to reset all input fields after processing
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
//        genderSpinner.setSelection(0);
//        ccp.resetToDefaultCountry();
//    }
//}

