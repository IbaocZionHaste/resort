package com.example.resort;

import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
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

        /// Setup the gender spinner adapter
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this, R.array.gender_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(adapter);

        /// No manual onFocusChangeListeners are added now.
        /// The view will only scroll if the keyboard appears, thanks to adjustResize.
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
                overridePendingTransition(0, 0);
                startActivity(intent);
                resetFields();
            }
        });
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




///No Current User
//package com.example.resort;
//
//import android.content.Intent;
//import android.graphics.Rect;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.view.MotionEvent;
//import android.view.View;
//import android.view.inputmethod.InputMethodManager;
//import android.widget.ArrayAdapter;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Spinner;
//import android.widget.Toast;
//
//import androidx.activity.EdgeToEdge;
//import androidx.appcompat.app.AppCompatActivity;
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
//        /// Setup the gender spinner adapter
//        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
//                this, R.array.gender_options, android.R.layout.simple_spinner_item);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        genderSpinner.setAdapter(adapter);
//
//        /// No manual onFocusChangeListeners are added now.
//        /// The view will only scroll if the keyboard appears, thanks to adjustResize.
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
//                        TextUtils.isEmpty(etPhone.getText().toString().trim())) {
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
//
//
