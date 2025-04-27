package com.example.resort;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.hbb20.CountryCodePicker;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * SignUpNext Activity now forwards to OTP verification.
 * Data is not saved until OTP is confirmed.
 *
 * After submission, all user inputs including formatted phone number
 * are passed to VerificationOtp.
 */
public class SignUpNext extends AppCompatActivity {
    private static final String LOCATIONS_URL =
            "https://raw.githubusercontent.com/xemasiv/psgc2/master/tree.json";
    private static final String PREFS_NAME = "locations_cache";
    private static final String KEY_LOCATIONS_JSON = "locations_json";

    private EditText etLastName;
    private EditText etFirstName;
    private EditText etMI;
    private Spinner streetSpinner;
    private EditText etAge;
    private EditText etPhone;
    private Spinner genderSpinner;
    private Spinner provinceSpinner;
    private Spinner municipalitySpinner;
    private Spinner barangaySpinner;
    private CountryCodePicker ccp;
    private RequestQueue requestQueue;
    private JSONObject locationsJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up_next);

        /// Initialize views
        etLastName = findViewById(R.id.editTextText6);
        etFirstName = findViewById(R.id.editTextText15);
        etMI = findViewById(R.id.editTextText11);
        streetSpinner = findViewById(R.id.editTextText32);
        etAge = findViewById(R.id.editTextTextPassword);
        etPhone = findViewById(R.id.editTextTextPassword2);
        genderSpinner = findViewById(R.id.genderSpinner);
        provinceSpinner = findViewById(R.id.editTextText33);
        municipalitySpinner = findViewById(R.id.editTextText30);
        barangaySpinner = findViewById(R.id.editTextText31);
        ccp = findViewById(R.id.ccp);
        Button btnNext = findViewById(R.id.button);

        /// Initialize network queue
        requestQueue = Volley.newRequestQueue(this);

        /// Setup spinners and load data
        setupGenderSpinner();
        setupPurokSpinner();
        fetchLocationsJson();


        /// Listener for province spinner
        provinceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (view instanceof TextView) ((TextView) view).setTextColor(Color.BLACK);

                if (pos > 0) {
                    populateMunicipalities(provinceSpinner.getSelectedItem().toString());
                } else {
                    resetSpinner(municipalitySpinner, "Select Municipality...");
                    resetSpinner(barangaySpinner, "Select Barangay...");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        /// Listener for municipality spinner
        municipalitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                if (view instanceof TextView) ((TextView) view).setTextColor(Color.BLACK);
                if (pos > 0) {
                    populateBarangays(
                            provinceSpinner.getSelectedItem().toString(),
                            municipalitySpinner.getSelectedItem().toString()

                    );
                } else {
                    resetSpinner(barangaySpinner, "Select Barangay...");
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });


        /// Next button click: validate and forward to OTP
        btnNext.setOnClickListener(v -> {
            if (isInputValid()) {
                Intent intent = new Intent(SignUpNext.this, VerificationOtp.class);
                intent.putExtra("lastName", etLastName.getText().toString().trim());
                intent.putExtra("firstName", etFirstName.getText().toString().trim());
                intent.putExtra("middleInitial", etMI.getText().toString().trim());
                intent.putExtra("province", provinceSpinner.getSelectedItem().toString());
                intent.putExtra("municipality", municipalitySpinner.getSelectedItem().toString());
                intent.putExtra("barangay", barangaySpinner.getSelectedItem().toString());
                intent.putExtra("street", streetSpinner.getSelectedItem().toString());
                intent.putExtra("age", etAge.getText().toString().trim());
                String rawPhone = etPhone.getText().toString().trim();
                String formattedPhone = formatPhoneNumber(rawPhone);
                intent.putExtra("phoneNumber", ccp.getSelectedCountryCodeWithPlus() + formattedPhone);
                intent.putExtra("gender", genderSpinner.getSelectedItem().toString());
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(SignUpNext.this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchLocationsJson() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String cached = prefs.getString(KEY_LOCATIONS_JSON, null);
        if (cached != null) {
            try {
                locationsJson = new JSONObject(cached);
                populateProvinces();
                return;
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        JsonObjectRequest req = new JsonObjectRequest(
                Request.Method.GET,
                LOCATIONS_URL,
                null,
                response -> {
                    locationsJson = response;
                    populateProvinces();
                    prefs.edit()
                            .putString(KEY_LOCATIONS_JSON, response.toString())
                            .apply();
                },
                error -> Toast.makeText(this, "Failed to load locations.", Toast.LENGTH_SHORT).show()
        );
        req.setShouldCache(true);
        requestQueue.add(req);
    }


    private void populateProvinces() {
        List<String> list = new ArrayList<>();
        list.add("Select Province...");
        Iterator<String> regions = locationsJson.keys();
        while (regions.hasNext()) {
            String region = regions.next();
            JSONObject regObj = locationsJson.optJSONObject(region);
            if (regObj == null) continue;
            Iterator<String> provs = regObj.keys();
            while (provs.hasNext()) {
                String prov = provs.next();
                if (!"population".equals(prov)) list.add(prov);
            }
        }
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        provinceSpinner.setAdapter(ad);
    }

    private void populateMunicipalities(String province) {
        try {
            JSONObject provObj = null;
            Iterator<String> regions = locationsJson.keys();
            while (regions.hasNext()) {
                JSONObject reg = locationsJson.optJSONObject(regions.next());
                if (reg != null && reg.has(province)) {
                    provObj = reg.optJSONObject(province);
                    break;
                }
            }
            if (provObj == null) {
                resetSpinner(municipalitySpinner, "Select Municipality...");
                return;
            }
            List<String> list = new ArrayList<>();
            list.add("Select Municipality...");
            Iterator<String> munis = provObj.keys();
            while (munis.hasNext()) {
                String m = munis.next();
                if (!"population".equals(m)) list.add(m);
            }
            ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            municipalitySpinner.setAdapter(ad);
        } catch (Exception e) {
            e.printStackTrace();
        }
        resetSpinner(barangaySpinner, "Select Barangay...");
    }

    private void populateBarangays(String province, String municipality) {
        try {
            JSONObject provObj = null;
            Iterator<String> regions = locationsJson.keys();
            while (regions.hasNext()) {
                JSONObject reg = locationsJson.optJSONObject(regions.next());
                if (reg != null && reg.has(province)) provObj = reg.optJSONObject(province);
            }
            if (provObj == null) return;
            JSONObject munObj = provObj.optJSONObject(municipality);
            if (munObj == null) return;
            List<String> list = new ArrayList<>();
            list.add("Select Barangay...");
            Iterator<String> bars = munObj.keys();
            while (bars.hasNext()) {
                String b = bars.next();
                if (!"population".equals(b) && !"class".equals(b)) list.add(b);
            }

            ArrayAdapter<String> ad = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    list
            ) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    TextView tv = (TextView) super.getView(position, convertView, parent);
                    tv.setTextColor(Color.BLACK);
                    return tv;
                }
            };

            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            barangaySpinner.setAdapter(ad);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupPurokSpinner() {
        List<String> list = new ArrayList<>();
        list.add("Select Purok...");
        for (int i = 1; i <= 20; i++) list.add("Purok " + i);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        streetSpinner.setAdapter(ad);
        streetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                if (v instanceof TextView) ((TextView) v).setTextColor(Color.BLACK);
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        });
    }

    private void setupGenderSpinner() {
        List<String> list = new ArrayList<>();
        list.add("Select Gender");
        list.add("Male");
        list.add("Female");
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list) {
            @Override public View getView(int pos, View cv, ViewGroup parent) {
                TextView tv = (TextView) super.getView(pos, cv, parent);
                tv.setTextColor(Color.BLACK);
                return tv;
            }
            @Override public View getDropDownView(int pos, View cv, ViewGroup parent) {
                TextView tv = (TextView) super.getDropDownView(pos, cv, parent);
                tv.setTextColor(Color.BLACK);
                tv.setBackgroundColor(Color.WHITE);
                return tv;
            }
        };
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        genderSpinner.setAdapter(ad);
    }

    private void resetSpinner(Spinner spinner, String prompt) {
        List<String> list = new ArrayList<>();
        list.add(prompt);
        ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
        ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(ad);
    }

    private boolean isInputValid() {
        return !TextUtils.isEmpty(etLastName.getText().toString().trim()) &&
                !TextUtils.isEmpty(etFirstName.getText().toString().trim()) &&
                provinceSpinner.getSelectedItemPosition() > 0 &&
                municipalitySpinner.getSelectedItemPosition() > 0 &&
                barangaySpinner.getSelectedItemPosition() > 0 &&
                streetSpinner.getSelectedItemPosition() > 0 &&
                !TextUtils.isEmpty(etAge.getText().toString().trim()) &&
                !TextUtils.isEmpty(etPhone.getText().toString().trim()) &&
                genderSpinner.getSelectedItemPosition() > 0;
    }


    private String formatPhoneNumber(String phone) {
        phone = phone.replaceAll("[^\\d+]", "");
        if (phone.startsWith("+63")) return phone.substring(3);
        if (phone.startsWith("63")) return phone.substring(2);
        if (phone.startsWith("0")) return phone.substring(1);
        return phone;
    }
}

///This barangay the data original
//            ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
//            ad.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            barangaySpinner.setAdapter(ad);

///This the original
//package com.example.resort;
//
//import android.content.Intent;
//import android.graphics.Color;
//import android.os.Bundle;
//import android.text.TextUtils;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.AdapterView;
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
//import com.android.volley.Request;
//import com.android.volley.RequestQueue;
//import com.android.volley.toolbox.JsonObjectRequest;
//import com.android.volley.toolbox.Volley;
//import com.hbb20.CountryCodePicker;
//
//import org.json.JSONObject;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//
///**
// * SignUpNext Activity now supports all Philippine provinces.
// * Ensure AndroidManifest.xml contains:
// * <uses-permission android:name="android.permission.INTERNET" />
// */
//public class SignUpNext extends AppCompatActivity {
//
//    private static final String LOCATIONS_URL =
//            "https://raw.githubusercontent.com/xemasiv/psgc2/master/tree.json";
//
//    private EditText etLastName, etFirstName, etMI, etStreet, etAge, etPhone;
//    private Spinner genderSpinner, provinceSpinner, municipalitySpinner, barangaySpinner, streetSpinner ;
//    private CountryCodePicker ccp;
//    private RequestQueue requestQueue;
//    private JSONObject locationsJson;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        EdgeToEdge.enable(this);
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_sign_up_next);
//
//        // Initialize views
//        etLastName         = findViewById(R.id.editTextText6);
//        etFirstName        = findViewById(R.id.editTextText15);
//        etMI               = findViewById(R.id.editTextText11);
//        streetSpinner           = findViewById(R.id.editTextText32);
//        etAge              = findViewById(R.id.editTextTextPassword);
//        etPhone            = findViewById(R.id.editTextTextPassword2);
//
//        genderSpinner      = findViewById(R.id.genderSpinner);
//        provinceSpinner    = findViewById(R.id.editTextText33);
//        municipalitySpinner = findViewById(R.id.editTextText30);
//        barangaySpinner    = findViewById(R.id.editTextText31);
//
//        ccp                = findViewById(R.id.ccp);
//        Button btnNext     = findViewById(R.id.button);
//
//        /// Network queue
//        requestQueue = Volley.newRequestQueue(this);
//
//        /// Setup gender and start loading locations
//        setupGenderSpinner();
//        fetchLocationsJson();
//        setupPurokSpinner();
//
//        provinceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
//                if (view instanceof TextView) {
//                    ((TextView) view).setTextColor(Color.BLACK); // Set to black
//                }
//
//                if (pos > 0) {
//                    String provName = provinceSpinner.getSelectedItem().toString();
//                    populateMunicipalities(provName);
//                } else {
//                    resetSpinner(municipalitySpinner, "Select Municipality...");
//                    resetSpinner(barangaySpinner, "Select Barangay...");
//                }
//            }
//
//            @Override public void onNothingSelected(AdapterView<?> parent) {}
//        });
//
//        municipalitySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
//                if (view instanceof TextView) {
//                    ((TextView) view).setTextColor(Color.BLACK); // Set to black
//                }
//
//                if (pos > 0) {
//                    String provName = provinceSpinner.getSelectedItem().toString();
//                    String muniName = municipalitySpinner.getSelectedItem().toString();
//                    populateBarangays(provName, muniName);
//                } else {
//                    resetSpinner(barangaySpinner, "Select Barangay...");
//                }
//            }
//
//            @Override public void onNothingSelected(AdapterView<?> parent) {}
//        });
//
//        barangaySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
//                if (view instanceof TextView) {
//                    ((TextView) view).setTextColor(Color.BLACK); // Set to black
//                }
//
//                /// Add any additional logic here if needed
//            }
//
//            @Override public void onNothingSelected(AdapterView<?> parent) {}
//        });
//
//
//        btnNext.setOnClickListener(v -> {
//            if (isInputValid()) {
//                navigateToSummary();
//            } else {
//                Toast.makeText(SignUpNext.this, "Please fill in all required fields.", Toast.LENGTH_SHORT).show();
//            }
//        });
//    }
//
//    private void fetchLocationsJson() {
//        JsonObjectRequest req = new JsonObjectRequest(
//                Request.Method.GET,
//                LOCATIONS_URL,
//                null,
//                response -> {
//                    locationsJson = response;
//                    populateProvinces();
//                },
//                error -> Toast.makeText(this, "Failed to load location data.", Toast.LENGTH_SHORT).show()
//        );
//        requestQueue.add(req);
//    }
//
//    private void populateProvinces() {
//        List<String> provinces = new ArrayList<>();
//        provinces.add("Select Province...");
//        // Flatten provinces under each region
//        Iterator<String> regionKeys = locationsJson.keys();
//        while (regionKeys.hasNext()) {
//            String regionName = regionKeys.next();
//            JSONObject regionObj = locationsJson.optJSONObject(regionName);
//            if (regionObj == null) continue;
//            Iterator<String> provKeys = regionObj.keys();
//            while (provKeys.hasNext()) {
//                String prov = provKeys.next();
//                if ("population".equals(prov)) continue;
//                provinces.add(prov);
//            }
//        }
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, provinces);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        provinceSpinner.setAdapter(adapter);
//    }
//
//
//
//    private void populateMunicipalities(String provName) {
//        try {
//            JSONObject provObj = null;
//            Iterator<String> regionKeys = locationsJson.keys();
//            while (regionKeys.hasNext()) {
//                JSONObject region = locationsJson.optJSONObject(regionKeys.next());
//                if (region != null && region.has(provName)) {
//                    provObj = region.optJSONObject(provName);
//                    break;
//                }
//            }
//            if (provObj == null) {
//                resetSpinner(municipalitySpinner, "Select Municipality...");
//                return;
//            }
//            List<String> munis = new ArrayList<>();
//            munis.add("Select Municipality...");
//            Iterator<String> muniKeys = provObj.keys();
//            while (muniKeys.hasNext()) {
//                String mun = muniKeys.next();
//                if ("population".equals(mun)) continue;
//                munis.add(mun);
//            }
//            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, munis);
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            municipalitySpinner.setAdapter(adapter);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        resetSpinner(barangaySpinner, "Select Barangay...");
//    }
//
//    private void populateBarangays(String provName, String muniName) {
//        try {
//            JSONObject provObj = null;
//            Iterator<String> regionKeys = locationsJson.keys();
//            while (regionKeys.hasNext()) {
//                JSONObject region = locationsJson.optJSONObject(regionKeys.next());
//                if (region != null && region.has(provName)) {
//                    provObj = region.optJSONObject(provName);
//                    break;
//                }
//            }
//            if (provObj == null) return;
//            JSONObject muniObj = provObj.optJSONObject(muniName);
//            if (muniObj == null) return;
//            List<String> barangays = new ArrayList<>();
//            barangays.add("Select Barangay...");
//            Iterator<String> keys = muniObj.keys();
//            while (keys.hasNext()) {
//                String key = keys.next();
//                if ("class".equals(key) || "population".equals(key)) continue;
//                barangays.add(key);
//            }
//            ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, barangays);
//            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//            barangaySpinner.setAdapter(adapter);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    /** Populate Purok spinner with 1–100 **/
//    private void setupPurokSpinner() {
//        List<String> puroks = new ArrayList<>();
//        puroks.add("Select Purok...");
//        for (int i = 1; i <= 100; i++) {
//            puroks.add("Purok " + i);
//        }
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
//                android.R.layout.simple_spinner_item, puroks);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        streetSpinner.setAdapter(adapter);
//
//        streetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
//                if (view instanceof TextView) ((TextView) view).setTextColor(Color.BLACK);
//            }
//            @Override public void onNothingSelected(AdapterView<?> parent) {}
//        });
//    }
//
//    private void resetSpinner(Spinner spinner, String prompt) {
//        List<String> list = new ArrayList<>();
//        list.add(prompt);
//        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, list);
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        spinner.setAdapter(adapter);
//    }
//
//    private boolean isInputValid() {
//        return !TextUtils.isEmpty(etLastName.getText().toString().trim()) &&
//                !TextUtils.isEmpty(etFirstName.getText().toString().trim()) &&
//                provinceSpinner.getSelectedItemPosition() > 0 &&
//                municipalitySpinner.getSelectedItemPosition() > 0 &&
//                barangaySpinner.getSelectedItemPosition() > 0 &&
//                !TextUtils.isEmpty(etStreet.getText().toString().trim()) &&
//                !TextUtils.isEmpty(etAge.getText().toString().trim()) &&
//                !TextUtils.isEmpty(etPhone.getText().toString().trim()) &&
//                genderSpinner.getSelectedItemPosition() > 0;
//    }
//
//    private void navigateToSummary() {
//        Intent intent = new Intent(SignUpNext.this, SignUp.class);
//        intent.putExtra("lastName", etLastName.getText().toString().trim());
//        intent.putExtra("firstName", etFirstName.getText().toString().trim());
//        intent.putExtra("middleInitial", etMI.getText().toString().trim());
//        intent.putExtra("province", provinceSpinner.getSelectedItem().toString());
//        intent.putExtra("municipality", municipalitySpinner.getSelectedItem().toString());
//        intent.putExtra("barangay", barangaySpinner.getSelectedItem().toString());
//        intent.putExtra("street", etStreet.getText().toString().trim());
//        intent.putExtra("age", etAge.getText().toString().trim());
//        String rawPhone = etPhone.getText().toString().trim();
//        String formattedPhone = formatPhoneNumber(rawPhone);
//        intent.putExtra("phoneNumber", ccp.getSelectedCountryCodeWithPlus() + formattedPhone);
//        intent.putExtra("gender", genderSpinner.getSelectedItem().toString());
//        startActivity(intent);
//        overridePendingTransition(0, 0);
//        resetFields();
//    }
//
//    private String formatPhoneNumber(String phone) {
//        phone = phone.replaceAll("[^\\d+]", "");
//        if (phone.startsWith("+63")) return phone.substring(3);
//        if (phone.startsWith("63")) return phone.substring(2);
//        if (phone.startsWith("0")) return phone.substring(1);
//        return phone;
//    }
//
//    private void setupGenderSpinner() {
//        List<String> genders = new ArrayList<>();
//        genders.add("Select Gender");
//        genders.add("Male");
//        genders.add("Female");
//        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, genders) {
//            @Override public View getView(int pos, View cv, ViewGroup parent) {
//                TextView tv = (TextView) super.getView(pos, cv, parent);
//                tv.setTextColor(getResources().getColor(android.R.color.black));
//                return tv;
//            }
//            @Override public View getDropDownView(int pos, View cv, ViewGroup parent) {
//                TextView tv = (TextView) super.getDropDownView(pos, cv, parent);
//                tv.setTextColor(getResources().getColor(android.R.color.darker_gray));
//                return tv;
//            }
//        };
//        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
//        genderSpinner.setAdapter(adapter);
//    }
//
//    private void resetFields() {
//        etLastName.setText("");
//        etFirstName.setText("");
//        etMI.setText("");
//        etStreet.setText("");
//        etAge.setText("");
//        etPhone.setText("");
//        genderSpinner.setSelection(0);
//        provinceSpinner.setSelection(0);
//        municipalitySpinner.setSelection(0);
//        barangaySpinner.setSelection(0);
//        ccp.resetToDefaultCountry();
//    }
//}











//package com.example.resort;
//
//import android.content.Intent;
//import android.graphics.Color;
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
//                ///String phoneCombined = ccp.getSelectedCountryCodeWithPlus() + etPhone.getText().toString().trim();
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

