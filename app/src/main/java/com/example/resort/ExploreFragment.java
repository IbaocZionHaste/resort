package com.example.resort;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExploreFragment extends Fragment implements OnMapReadyCallback {

    private static final String TAG = "ExploreFragment";
    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Marker userMarker;
    private Polyline routePolyline;
    private TextView tvDistance, tvETA;
    private Button btnFollow, btnRefresh;

    /// Destination: Island View Beachfront Resort in Bohol, Philippines.
    private final LatLng destinationLatLng = new LatLng( 10.1341723, 124.3829805);
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    /// Average walking speed (~83 m/min)
    private final double AVERAGE_WALKING_SPEED = 83.0;
    // Toggle for following user's location. In our update, follow mode now only toggles the button color,
    // and the camera always adjusts to show both markers (fixed zoom).
    private boolean isFollowing = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.explore_fragment, container, false);
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        tvDistance = view.findViewById(R.id.tvDistance);
        tvETA = view.findViewById(R.id.tvETA);
        btnFollow = view.findViewById(R.id.btnFollow);
        btnRefresh = view.findViewById(R.id.btnRefresh);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        SupportMapFragment mapFragment = (SupportMapFragment)
                getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        } else {
            Log.e(TAG, "MapFragment is null! Check your layout file.");
        }

        /// Set up the location callback to update location, route, and distance/ETA
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult == null) {
                    Log.w(TAG, "LocationResult is null");
                    return;
                }
                Location location = locationResult.getLastLocation();
                Log.d(TAG, "Location update: " + location.getLatitude() + ", " + location.getLongitude());
                updateLocationOnMap(location);
                updateDistanceAndETA(location);
            }
        };

        /// Toggle follow mode: now, regardless of toggle, we always update the camera to show both markers.
        btnFollow.setOnClickListener(v -> {
            isFollowing = !v.isSelected();
            v.setSelected(isFollowing);
            int color = ContextCompat.getColor(requireContext(), isFollowing ? R.color.light_blue : R.color.white);
            btnFollow.setBackgroundTintList(ColorStateList.valueOf(color));

            if (userMarker != null) {

                googleMap.setMapType(isFollowing ? GoogleMap.MAP_TYPE_HYBRID : GoogleMap.MAP_TYPE_NORMAL);

                /// Adjust camera
                updateCameraFixedZoom();
            }
        });


        // Refresh button to update distance and ETA manually
        btnRefresh.setOnClickListener(v -> fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                updateDistanceAndETA(location);
            } else {
                Log.w(TAG, "Last location is null");
            }
        }));
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        // Initially center the camera on the destination with a default zoom level
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 12f));
        // Add the resort marker
        googleMap.addMarker(new MarkerOptions().position(destinationLatLng)
                .title("Island View Beachfront Resort"));

        // Enable the built-in MyLocation layer if permission is granted
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            googleMap.setMyLocationEnabled(true);
            startLocationUpdates();
        }
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        // Set the update interval to 1 second
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted!");
            return;
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
    }

    private void updateLocationOnMap(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        // Add or update the user marker
        if (userMarker == null) {
            userMarker = googleMap.addMarker(new MarkerOptions().position(userLatLng).title("You"));
        } else {
            userMarker.setPosition(userLatLng);
        }
        // Draw the route from the current location to the destination
        fetchRoute(userLatLng, destinationLatLng);

        // Always update camera view to show both markers if needed.
        updateCameraFixedZoom();
    }

    // Calculate the midpoint between the user's location and the destination
    // and update the camera with a fixed zoom level (11.8f)
    private void updateCameraFixedZoom() {
        if (userMarker == null) return;
        double midLat = (userMarker.getPosition().latitude + destinationLatLng.latitude) / 2;
        double midLng = (userMarker.getPosition().longitude + destinationLatLng.longitude) / 2;
        LatLng midPoint = new LatLng(midLat, midLng);
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(midPoint)
                .zoom(12.3f)  /// Fixed zoom level; adjust as needed
                .tilt(0)
                .build();
        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    /// Fetch the route from Google Directions API and draw a polyline on the map.
    private void fetchRoute(LatLng origin, LatLng destination) {
        // Replace YOUR_API_KEY_HERE with your actual API key
        String apiKey = "AIzaSyBmR1p6AFBkS3GcuL9nabMTc39bztQnwgU";
        // Using walking mode for the route
        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
                + origin.latitude + "," + origin.longitude
                + "&destination=" + destination.latitude + "," + destination.longitude
                + "&mode=walking&region=PH&key=" + apiKey;
        new FetchRouteTask().execute(url);
    }

    @SuppressLint("StaticFieldLeak")
    private class FetchRouteTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String data = "";
            try {
                URL url = new URL(params[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                InputStream stream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                data = sb.toString();
                reader.close();
                stream.close();
                connection.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error fetching route: " + e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            try {
                JSONObject json = new JSONObject(result);
                JSONArray routes = json.getJSONArray("routes");
                if (routes.length() > 0) {
                    JSONObject route = routes.getJSONObject(0);
                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
                    String encodedPoints = overviewPolyline.getString("points");
                    List<LatLng> points = decodePoly(encodedPoints);
                    // Remove the old polyline if it exists
                    if (routePolyline != null) {
                        routePolyline.remove();
                    }
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(points)
                            .width(15f)
                            .color(Color.BLUE)
                            .geodesic(true);
                    routePolyline = googleMap.addPolyline(polylineOptions);

                    // Update camera if needed (this update now always shows both markers)
                    updateCameraFixedZoom();
                } else {
                    Log.e(TAG, "No routes found in the response.");
                }
            } catch (JSONException e) {
                Log.e(TAG, "JSON parsing error: " + e.toString());
            }
        }
    }


    /// Decode an encoded polyline string into a list of LatLng points.
    private List<LatLng> decodePoly(String encoded) {
        List<LatLng> poly = new ArrayList<>();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;
        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;
            poly.add(new LatLng((lat / 1E5), (lng / 1E5)));
        }
        return poly;
    }

    private void updateDistanceAndETA(Location location) {
        float[] results = new float[1];
        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
                destinationLatLng.latitude, destinationLatLng.longitude, results);
        float distanceMeters = results[0];
        double distanceKm = distanceMeters / 1000.0;
        tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distanceKm));

        int etaMinutes = AVERAGE_WALKING_SPEED > 0 ? (int) Math.round(distanceMeters / AVERAGE_WALKING_SPEED) : 0;
        tvETA.setText("ETA: " + etaMinutes + " min");
    }

    @Override
    public void onResume() {
        super.onResume();
        startLocationUpdates();
    }

    @Override
    public void onPause() {
        super.onPause();
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED) {
                    if (googleMap != null) {
                        googleMap.setMyLocationEnabled(true);
                    }
                    startLocationUpdates();
                }
            } else {
                Log.e(TAG, "Location permission denied");
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}


//package com.example.resort;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.location.Location;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.os.Looper;
//import android.util.Log;
//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;
//import androidx.fragment.app.Fragment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.CameraPosition;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.Polyline;
//import com.google.android.gms.maps.model.PolylineOptions;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Locale;
//
//public class ExploreFragment extends Fragment implements OnMapReadyCallback {
//
//    private static final String TAG = "ExploreFragment";
//    private GoogleMap googleMap;
//    private FusedLocationProviderClient fusedLocationClient;
//    private LocationCallback locationCallback;
//    private Marker userMarker;
//    private Polyline routePolyline;
//    private TextView tvDistance, tvETA;
//    private Button btnFollow, btnRefresh;
//
//    // Destination: Island View Beachfront Resort in Bohol, Philippines.
//    private final LatLng destinationLatLng = new LatLng(10.1415, 124.3669);
//    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
//    // Average walking speed (~83 m/min)
//    private final double AVERAGE_WALKING_SPEED = 83.0;
//    // Toggle for following user's location (if false, the camera uses a fixed zoom with both markers visible)
//    private boolean isFollowing = false;
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.explore_fragment, container, false);
//    }
//
//    @SuppressLint("MissingPermission")
//    @Override
//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        tvDistance = view.findViewById(R.id.tvDistance);
//        tvETA = view.findViewById(R.id.tvETA);
//        btnFollow = view.findViewById(R.id.btnFollow);
//        btnRefresh = view.findViewById(R.id.btnRefresh);
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
//
//        SupportMapFragment mapFragment = (SupportMapFragment)
//                getChildFragmentManager().findFragmentById(R.id.map);
//        if (mapFragment != null) {
//            mapFragment.getMapAsync(this);
//        } else {
//            Log.e(TAG, "MapFragment is null! Check your layout file.");
//        }
//
//        // Set up the location callback to update location, route, and distance/ETA
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(@NonNull LocationResult locationResult) {
//                if (locationResult == null) {
//                    Log.w(TAG, "LocationResult is null");
//                    return;
//                }
//                Location location = locationResult.getLastLocation();
//                Log.d(TAG, "Location update: " + location.getLatitude() + ", " + location.getLongitude());
//                updateLocationOnMap(location);
//                updateDistanceAndETA(location);
//            }
//        };
//
//        // Toggle follow mode: "Following" centers on your location,
//        // "Fixed Zoom" uses a calculated midpoint with a fixed zoom (18f)
//        btnFollow.setOnClickListener(v -> {
//            isFollowing = !isFollowing;
//            btnFollow.setText(isFollowing ? "Following" : "Fixed Zoom");
//            if (userMarker != null) {
//                if (isFollowing) {
//                    // Center on the user's location
//                    CameraPosition cameraPosition = new CameraPosition.Builder()
//                            .target(userMarker.getPosition())
//                            .zoom(18f)
//                            .tilt(0)
//                            .build();
//                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//                } else {
//                    // Adjust camera to show both markers with fixed zoom
//                    updateCameraFixedZoom();
//                }
//            }
//        });
//
//        // Refresh button to update distance and ETA manually
//        btnRefresh.setOnClickListener(v -> fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
//            if (location != null) {
//                updateDistanceAndETA(location);
//            } else {
//                Log.w(TAG, "Last location is null");
//            }
//        }));
//    }
//
//    @Override
//    public void onMapReady(@NonNull GoogleMap map) {
//        googleMap = map;
//        // Initially center the camera on the destination with a default zoom level
//        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 12f));
//        // Add the resort marker
//        googleMap.addMarker(new MarkerOptions().position(destinationLatLng)
//                .title("Island View Beachfront Resort"));
//
//        // Enable the built-in MyLocation layer if permission is granted
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
//        } else {
//            googleMap.setMyLocationEnabled(true);
//            startLocationUpdates();
//        }
//    }
//
//    private void startLocationUpdates() {
//        LocationRequest locationRequest = LocationRequest.create();
//        // Set the update interval to 1 second
//        locationRequest.setInterval(1000);
//        locationRequest.setFastestInterval(1000);
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            Log.e(TAG, "Location permission not granted!");
//            return;
//        }
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//    }
//
//    private void updateLocationOnMap(Location location) {
//        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//        // Add or update the user marker
//        if (userMarker == null) {
//            userMarker = googleMap.addMarker(new MarkerOptions().position(userLatLng).title("You"));
//        } else {
//            userMarker.setPosition(userLatLng);
//        }
//        // Draw the route from the current location to the destination
//        fetchRoute(userLatLng, destinationLatLng);
//
//        // Adjust camera view based on follow mode
//        if (isFollowing) {
//            // Center on the user's location with fixed zoom 18
//            CameraPosition cameraPosition = new CameraPosition.Builder()
//                    .target(userLatLng)
//                    .zoom(18f)
//                    .tilt(0)
//                    .build();
//            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//        } else {
//            // Show both markers using a fixed zoom by calculating the midpoint
//            updateCameraFixedZoom();
//        }
//    }
//
//    // Calculate the midpoint between the user's location and the destination
//    // and update the camera with a fixed zoom level (18f)
//    private void updateCameraFixedZoom() {
//        if (userMarker == null) return;
//        double midLat = (userMarker.getPosition().latitude + destinationLatLng.latitude) / 2;
//        double midLng = (userMarker.getPosition().longitude + destinationLatLng.longitude) / 2;
//        LatLng midPoint = new LatLng(midLat, midLng);
//        CameraPosition cameraPosition = new CameraPosition.Builder()
//                .target(midPoint)
//                .zoom(11.7f)
//                .tilt(0)
//                .build();
//        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//    }
//
//
//
//    // Fetch the route from Google Directions API and draw a polyline on the map.
//    private void fetchRoute(LatLng origin, LatLng destination) {
//        // Replace YOUR_API_KEY_HERE with your actual API key
//        String apiKey = "AIzaSyBmR1p6AFBkS3GcuL9nabMTc39bztQnwgU";
//        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
//                + origin.latitude + "," + origin.longitude
//                + "&destination=" + destination.latitude + "," + destination.longitude
//                + "&mode=driving&region=PH&key=" + apiKey;
//        new FetchRouteTask().execute(url);
//    }
//
//    @SuppressLint("StaticFieldLeak")
//    private class FetchRouteTask extends AsyncTask<String, Void, String> {
//        @Override
//        protected String doInBackground(String... params) {
//            String data = "";
//            try {
//                URL url = new URL(params[0]);
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.connect();
//                InputStream stream = connection.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
//                StringBuilder sb = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                }
//                data = sb.toString();
//                reader.close();
//                stream.close();
//                connection.disconnect();
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching route: " + e.toString());
//            }
//            return data;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            try {
//                JSONObject json = new JSONObject(result);
//                JSONArray routes = json.getJSONArray("routes");
//                if (routes.length() > 0) {
//                    JSONObject route = routes.getJSONObject(0);
//                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
//                    String encodedPoints = overviewPolyline.getString("points");
//                    List<LatLng> points = decodePoly(encodedPoints);
//                    if (routePolyline != null) {
//                        routePolyline.remove();
//                    }
//                    PolylineOptions polylineOptions = new PolylineOptions()
//                            .addAll(points)
//                            .width(20f)
//                            .color(Color.parseColor("#ADD8E6"))
//                            .geodesic(true);
//                    routePolyline = googleMap.addPolyline(polylineOptions);
//                }
//            } catch (JSONException e) {
//                Log.e(TAG, "JSON parsing error: " + e.toString());
//            }
//        }
//    }
//
//    // Decode an encoded polyline string into a list of LatLng points.
//    private List<LatLng> decodePoly(String encoded) {
//        List<LatLng> poly = new ArrayList<>();
//        int index = 0, len = encoded.length();
//        int lat = 0, lng = 0;
//        while (index < len) {
//            int b, shift = 0, result = 0;
//            do {
//                b = encoded.charAt(index++) - 63;
//                result |= (b & 0x1f) << shift;
//                shift += 5;
//            } while (b >= 0x20);
//            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//            lat += dlat;
//            shift = 0;
//            result = 0;
//            do {
//                b = encoded.charAt(index++) - 63;
//                result |= (b & 0x1f) << shift;
//                shift += 5;
//            } while (b >= 0x20);
//            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//            lng += dlng;
//            poly.add(new LatLng((lat / 1E5), (lng / 1E5)));
//        }
//        return poly;
//    }
//
//    private void updateDistanceAndETA(Location location) {
//        float[] results = new float[1];
//        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
//                destinationLatLng.latitude, destinationLatLng.longitude, results);
//        float distanceMeters = results[0];
//        double distanceKm = distanceMeters / 1000.0;
//        tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distanceKm));
//
//        int etaMinutes = AVERAGE_WALKING_SPEED > 0 ? (int) Math.round(distanceMeters / AVERAGE_WALKING_SPEED) : 0;
//        tvETA.setText("ETA: " + etaMinutes + " min");
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        startLocationUpdates();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        fusedLocationClient.removeLocationUpdates(locationCallback);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                        == PackageManager.PERMISSION_GRANTED) {
//                    if (googleMap != null) {
//                        googleMap.setMyLocationEnabled(true);
//                    }
//                    startLocationUpdates();
//                }
//            } else {
//                Log.e(TAG, "Location permission denied");
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }
//}
//
//
//
//


//package com.example.resort;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.content.pm.PackageManager;
//import android.content.res.ColorStateList;
//import android.graphics.Color;
//import android.location.Location;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.os.Looper;
//import android.util.Log;
//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;
//import androidx.core.content.ContextCompat;
//import androidx.fragment.app.Fragment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.CameraPosition;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.LatLngBounds;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.Polyline;
//import com.google.android.gms.maps.model.PolylineOptions;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Locale;
//
//public class ExploreFragment extends Fragment implements OnMapReadyCallback {
//
//    private static final String TAG = "ExploreFragment";
//    private GoogleMap googleMap;
//    private FusedLocationProviderClient fusedLocationClient;
//    private LocationCallback locationCallback;
//    private Marker userMarker;
//    private Polyline routePolyline;
//    private TextView tvDistance, tvETA;
//    private Button btnFollow, btnRefresh;
//
//    // Destination: Island View Beachfront Resort in Bohol, Philippines.
//    private final LatLng destinationLatLng = new LatLng(10.1415, 124.3669);
//    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
//    // Average walking speed (~83 m/min)
//    private final double AVERAGE_WALKING_SPEED = 83.0;
//    // Toggle for following user's location (if false, the camera uses a fixed zoom with both markers visible)
//    private boolean isFollowing = false;
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.explore_fragment, container, false);
//    }
//
//    @SuppressLint("MissingPermission")
//    @Override
//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        tvDistance = view.findViewById(R.id.tvDistance);
//        tvETA = view.findViewById(R.id.tvETA);
//        btnFollow = view.findViewById(R.id.btnFollow);
//        btnRefresh = view.findViewById(R.id.btnRefresh);
//
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
//
//        SupportMapFragment mapFragment = (SupportMapFragment)
//                getChildFragmentManager().findFragmentById(R.id.map);
//        if (mapFragment != null) {
//            mapFragment.getMapAsync(this);
//        } else {
//            Log.e(TAG, "MapFragment is null! Check your layout file.");
//        }
//
//        // Set up the location callback to update location, route, and distance/ETA
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(@NonNull LocationResult locationResult) {
//                if (locationResult == null) {
//                    Log.w(TAG, "LocationResult is null");
//                    return;
//                }
//                Location location = locationResult.getLastLocation();
//                Log.d(TAG, "Location update: " + location.getLatitude() + ", " + location.getLongitude());
//                updateLocationOnMap(location);
//                updateDistanceAndETA(location);
//            }
//        };
//
//        // Toggle follow mode: "Following" centers on your location,
//        // "Fixed Zoom" uses a calculated midpoint with a fixed zoom (11.7f)
//        btnFollow.setOnClickListener(v -> {
//            boolean isSelected = !v.isSelected();
//            v.setSelected(isSelected);
//            // Update your follow mode flag
//            isFollowing = isSelected;
//            // Use requireContext() for getting the color
//            int color = ContextCompat.getColor(requireContext(), isSelected ? R.color.red : R.color.white);
//            btnFollow.setBackgroundTintList(ColorStateList.valueOf(color));
//
//            if (userMarker != null) {
//                if (isFollowing) {
//                    // Center on the user's location
//                    CameraPosition cameraPosition = new CameraPosition.Builder()
//                            .target(userMarker.getPosition())
//                            .zoom(18f)
//                            .tilt(0)
//                            .build();
//                    googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//                } else {
//                    // Adjust camera to show both markers with fixed zoom
//                    updateCameraFixedZoom();
//                }
//            }
//        });
//
//
//        // Refresh button to update distance and ETA manually
//        btnRefresh.setOnClickListener(v -> fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
//            if (location != null) {
//                updateDistanceAndETA(location);
//            } else {
//                Log.w(TAG, "Last location is null");
//            }
//        }));
//    }
//
//    @Override
//    public void onMapReady(@NonNull GoogleMap map) {
//        googleMap = map;
//        // Initially center the camera on the destination with a default zoom level
//        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 12f));
//        // Add the resort marker
//        googleMap.addMarker(new MarkerOptions().position(destinationLatLng)
//                .title("Island View Beachfront Resort"));
//
//        // Enable the built-in MyLocation layer if permission is granted
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
//        } else {
//            googleMap.setMyLocationEnabled(true);
//            startLocationUpdates();
//        }
//    }
//
//    private void startLocationUpdates() {
//        LocationRequest locationRequest = LocationRequest.create();
//        // Set the update interval to 1 second
//        locationRequest.setInterval(1000);
//        locationRequest.setFastestInterval(1000);
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            Log.e(TAG, "Location permission not granted!");
//            return;
//        }
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//    }
//
//    private void updateLocationOnMap(Location location) {
//        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//        // Add or update the user marker
//        if (userMarker == null) {
//            userMarker = googleMap.addMarker(new MarkerOptions().position(userLatLng).title("You"));
//        } else {
//            userMarker.setPosition(userLatLng);
//        }
//        // Draw the route from the current location to the destination
//        fetchRoute(userLatLng, destinationLatLng);
//
//        // Adjust camera view based on follow mode
//        if (isFollowing) {
//            // Center on the user's location with fixed zoom 18
//            CameraPosition cameraPosition = new CameraPosition.Builder()
//                    .target(userLatLng)
//                    .zoom(18f)
//                    .tilt(0)
//                    .build();
//            googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//        } else {
//            // Only update camera if needed, not on every location update.
//            // This can be done just once when the route is drawn or by a manual refresh.
//        }
//    }
//
//
//    // Calculate the midpoint between the user's location and the destination
//    // and update the camera with a fixed zoom level (11.7f)
//    private void updateCameraFixedZoom() {
//        if (userMarker == null) return;
//        double midLat = (userMarker.getPosition().latitude + destinationLatLng.latitude) / 2;
//        double midLng = (userMarker.getPosition().longitude + destinationLatLng.longitude) / 2;
//        LatLng midPoint = new LatLng(midLat, midLng);
//        CameraPosition cameraPosition = new CameraPosition.Builder()
//                .target(midPoint)
//                .zoom(11.7f)  // Fixed zoom level; adjust as needed
//                .tilt(0)
//                .build();
//        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//    }
//
//
//    // Fetch the route from Google Directions API and draw a polyline on the map.
//    private void fetchRoute(LatLng origin, LatLng destination) {
//        // Replace YOUR_API_KEY_HERE with your actual API key
//        String apiKey = "AIzaSyBmR1p6AFBkS3GcuL9nabMTc39bztQnwgU";
//        // Using walking mode for the route
//        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
//                + origin.latitude + "," + origin.longitude
//                + "&destination=" + destination.latitude + "," + destination.longitude
//                + "&mode=walking&region=PH&key=" + apiKey;
//        new FetchRouteTask().execute(url);
//    }
//
//    @SuppressLint("StaticFieldLeak")
//    private class FetchRouteTask extends AsyncTask<String, Void, String> {
//        @Override
//        protected String doInBackground(String... params) {
//            String data = "";
//            try {
//                URL url = new URL(params[0]);
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.connect();
//                InputStream stream = connection.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
//                StringBuilder sb = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                }
//                data = sb.toString();
//                reader.close();
//                stream.close();
//                connection.disconnect();
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching route: " + e.toString());
//            }
//            return data;
//        }
//
//        @Override
//        protected void onPostExecute(String result) {
//            try {
//                JSONObject json = new JSONObject(result);
//                JSONArray routes = json.getJSONArray("routes");
//                if (routes.length() > 0) {
//                    JSONObject route = routes.getJSONObject(0);
//                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
//                    String encodedPoints = overviewPolyline.getString("points");
//                    List<LatLng> points = decodePoly(encodedPoints);
//                    // Remove the old polyline if it exists
//                    if (routePolyline != null) {
//                        routePolyline.remove();
//                    }
//                    PolylineOptions polylineOptions = new PolylineOptions()
//                            .addAll(points)
//                            .width(20f)
//                            .color(Color.BLUE) // set your blue color here
//                            .geodesic(true);
//                    routePolyline = googleMap.addPolyline(polylineOptions);
//
//                    // If not following, update the camera once.
//                    if (!isFollowing) {
//                        updateCameraFixedZoom();
//                    }
//                } else {
//                    Log.e(TAG, "No routes found in the response.");
//                }
//            } catch (JSONException e) {
//                Log.e(TAG, "JSON parsing error: " + e.toString());
//            }
//        }
//
//    }
//
//    // Decode an encoded polyline string into a list of LatLng points.
//    private List<LatLng> decodePoly(String encoded) {
//        List<LatLng> poly = new ArrayList<>();
//        int index = 0, len = encoded.length();
//        int lat = 0, lng = 0;
//        while (index < len) {
//            int b, shift = 0, result = 0;
//            do {
//                b = encoded.charAt(index++) - 63;
//                result |= (b & 0x1f) << shift;
//                shift += 5;
//            } while (b >= 0x20);
//            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//            lat += dlat;
//            shift = 0;
//            result = 0;
//            do {
//                b = encoded.charAt(index++) - 63;
//                result |= (b & 0x1f) << shift;
//                shift += 5;
//            } while (b >= 0x20);
//            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//            lng += dlng;
//            poly.add(new LatLng((lat / 1E5), (lng / 1E5)));
//        }
//        return poly;
//    }
//
//    private void updateDistanceAndETA(Location location) {
//        float[] results = new float[1];
//        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
//                destinationLatLng.latitude, destinationLatLng.longitude, results);
//        float distanceMeters = results[0];
//        double distanceKm = distanceMeters / 1000.0;
//        tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distanceKm));
//
//        int etaMinutes = AVERAGE_WALKING_SPEED > 0 ? (int) Math.round(distanceMeters / AVERAGE_WALKING_SPEED) : 0;
//        tvETA.setText("ETA: " + etaMinutes + " min");
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        startLocationUpdates();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        fusedLocationClient.removeLocationUpdates(locationCallback);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                        == PackageManager.PERMISSION_GRANTED) {
//                    if (googleMap != null) {
//                        googleMap.setMyLocationEnabled(true);
//                    }
//                    startLocationUpdates();
//                }
//            } else {
//                Log.e(TAG, "Location permission denied");
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }
//}


///Working
//package com.example.resort;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//import android.location.Location;
//import android.os.AsyncTask;
//import android.os.Bundle;
//import android.os.Looper;
//import android.util.Log;
//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;
//import androidx.fragment.app.Fragment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.Polyline;
//import com.google.android.gms.maps.model.PolylineOptions;
//import org.json.JSONArray;
//import org.json.JSONException;
//import org.json.JSONObject;
//import java.io.BufferedReader;
//import java.io.InputStream;
//import java.io.InputStreamReader;
//import java.net.HttpURLConnection;
//import java.net.URL;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Locale;
//
//public class ExploreFragment extends Fragment implements OnMapReadyCallback, SensorEventListener {
//
//    private static final String TAG = "ExploreFragment";
//    private GoogleMap googleMap;
//    private FusedLocationProviderClient fusedLocationClient;
//    private LocationCallback locationCallback;
//    private Marker userMarker;
//    private Polyline routePolyline;
//    private TextView tvDistance, tvETA;
//    private Button btnFollow, btnRefresh;
//
//    // Destination: Island View Beachfront Resort in Bohol, Philippines.
//    private final LatLng destinationLatLng = new LatLng(10.1415, 124.3669);
//
//    // Sensor management for compass functionality
//    private SensorManager sensorManager;
//    private float[] accelerometerReading = new float[3];
//    private float[] magnetometerReading = new float[3];
//
//    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
//    // Average walking speed (~83 m/min)
//    private final double AVERAGE_WALKING_SPEED = 83.0;
//    // Start in follow mode so that camera centers on user location
//    private boolean isFollowing = true;
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.explore_fragment, container, false);
//    }
//
//    @SuppressLint("MissingPermission")
//    @Override
//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        tvDistance = view.findViewById(R.id.tvDistance);
//        tvETA = view.findViewById(R.id.tvETA);
//        btnFollow = view.findViewById(R.id.btnFollow);
//        btnRefresh = view.findViewById(R.id.btnRefresh);
//
//        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
//
//        // Obtain the SupportMapFragment and set its async callback
//        SupportMapFragment mapFragment = (SupportMapFragment)
//                getChildFragmentManager().findFragmentById(R.id.map);
//        if (mapFragment != null) {
//            mapFragment.getMapAsync(this);
//        } else {
//            Log.e(TAG, "MapFragment is null! Check your layout file.");
//        }
//
//        // Set up location callback for receiving updates
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(@NonNull LocationResult locationResult) {
//                if (locationResult == null) {
//                    Log.w(TAG, "LocationResult is null");
//                    return;
//                }
//                Location location = locationResult.getLastLocation();
//                Log.d(TAG, "Location update: " + location.getLatitude() + ", " + location.getLongitude());
//                updateLocationOnMap(location);
//                updateDistanceAndETA(location);
//            }
//        };
//
//        // Toggle follow mode
//        btnFollow.setOnClickListener(v -> {
//            isFollowing = !isFollowing;
//            btnFollow.setText(isFollowing ? "Following" : "Follow");
//            if (userMarker != null && isFollowing) {
//                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), 16f));
//            }
//        });
//
//        // Refresh button to update distance and ETA manually
//        btnRefresh.setOnClickListener(v -> fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
//            if (location != null) {
//                updateDistanceAndETA(location);
//            } else {
//                Log.w(TAG, "Last location is null");
//            }
//        }));
//    }
//
//    @Override
//    public void onMapReady(@NonNull GoogleMap map) {
//        googleMap = map;
//        // Center the camera on Bohol with a zoom level that shows the region
//        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 12f));
//        // Add destination marker
//        googleMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Island View Beachfront Resort"));
//
//        // Check for location permission and start location updates
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
//        } else {
//            googleMap.setMyLocationEnabled(true);
//            startLocationUpdates();
//        }
//    }
//
//    private void startLocationUpdates() {
//        LocationRequest locationRequest = LocationRequest.create();
//        locationRequest.setInterval(5000);         // Update every 5 seconds
//        locationRequest.setFastestInterval(2000);    // Fastest update every 2 seconds
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            Log.e(TAG, "Location permission not granted!");
//            return;
//        }
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//    }
//
//    private void updateLocationOnMap(Location location) {
//        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//        if (userMarker == null) {
//            userMarker = googleMap.addMarker(new MarkerOptions().position(userLatLng).title("You"));
//        } else {
//            userMarker.setPosition(userLatLng);
//        }
//        if (isFollowing) {
//            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f));
//        }
//        // Fetch and draw the driving route that follows the road
//        fetchRoute(userLatLng, destinationLatLng);
//    }
//
//    // Fetch route from Google Directions API and draw a thick, light blue polyline following roads.
//    private void fetchRoute(LatLng origin, LatLng destination) {
//        // Replace YOUR_API_KEY_HERE with your actual API key
//        String apiKey = "AIzaSyAOVYRIgupAurZup5y1PRh8Ismb1A3lLao";
//        // Added region=PH parameter to focus on the Philippines.
//        String url = "https://maps.googleapis.com/maps/api/directions/json?origin="
//                + origin.latitude + "," + origin.longitude
//                + "&destination=" + destination.latitude + "," + destination.longitude
//                + "&mode=driving&region=PH&key=" + apiKey;
//        new FetchRouteTask().execute(url);
//    }
//
//
//    /** @noinspection deprecation*/
//    @SuppressLint("StaticFieldLeak")
//    private class FetchRouteTask extends AsyncTask<String, Void, String> {
//        @Override
//        protected String doInBackground(String... params) {
//            String data = "";
//            try {
//                URL url = new URL(params[0]);
//                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
//                connection.connect();
//                InputStream stream = connection.getInputStream();
//                BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
//                StringBuilder sb = new StringBuilder();
//                String line;
//                while ((line = reader.readLine()) != null) {
//                    sb.append(line);
//                }
//                data = sb.toString();
//                reader.close();
//                stream.close();
//                connection.disconnect();
//            } catch (Exception e) {
//                Log.e(TAG, "Error fetching route: " + e.toString());
//            }
//            return data;
//        }
//
//
//        @Override
//        protected void onPostExecute(String result) {
//            try {
//                JSONObject json = new JSONObject(result);
//                JSONArray routes = json.getJSONArray("routes");
//                if (routes.length() > 0) {
//                    JSONObject route = routes.getJSONObject(0);
//                    JSONObject overviewPolyline = route.getJSONObject("overview_polyline");
//                    String encodedPoints = overviewPolyline.getString("points");
//                    List<LatLng> points = decodePoly(encodedPoints);
//                    // Draw the route polyline in light blue with thicker width (15f)
//                    if (routePolyline != null) {
//                        routePolyline.remove();
//                    }
//                    PolylineOptions polylineOptions = new PolylineOptions()
//                            .addAll(points)
//                            .width(20f)
//                            .color(Color.parseColor("#ADD8E6"))
//                            .geodesic(true);
//                    routePolyline = googleMap.addPolyline(polylineOptions);
//                }
//            } catch (JSONException e) {
//                Log.e(TAG, "JSON parsing error: " + e.toString());
//            }
//        }
//    }
//
//    // Decode encoded polyline into list of LatLng
//    private List<LatLng> decodePoly(String encoded) {
//        List<LatLng> poly = new ArrayList<>();
//        int index = 0, len = encoded.length();
//        int lat = 0, lng = 0;
//        while (index < len) {
//            int b, shift = 0, result = 0;
//            do {
//                b = encoded.charAt(index++) - 63;
//                result |= (b & 0x1f) << shift;
//                shift += 5;
//            } while (b >= 0x20);
//            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//            lat += dlat;
//            shift = 0;
//            result = 0;
//            do {
//                b = encoded.charAt(index++) - 63;
//                result |= (b & 0x1f) << shift;
//                shift += 5;
//            } while (b >= 0x20);
//            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
//            lng += dlng;
//            poly.add(new LatLng((lat / 1E5), (lng / 1E5)));
//        }
//        return poly;
//    }
//
//    private void updateDistanceAndETA(Location location) {
//        // Use the straight-line distance to update distance/ETA (or refine using route length if needed)
//        float[] results = new float[1];
//        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
//                destinationLatLng.latitude, destinationLatLng.longitude, results);
//        float distanceMeters = results[0];
//        double distanceKm = distanceMeters / 1000.0;
//        tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distanceKm));
//
//        int etaMinutes = AVERAGE_WALKING_SPEED > 0 ? (int) Math.round(distanceMeters / AVERAGE_WALKING_SPEED) : 0;
//        tvETA.setText("ETA: " + etaMinutes + " min");
//    }
//
//    // SensorEventListener for compass functionality
//    @Override
//    public void onSensorChanged(SensorEvent event) {
//        if (event == null) return;
//        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            System.arraycopy(event.values, 0, accelerometerReading, 0, event.values.length);
//        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//            System.arraycopy(event.values, 0, magnetometerReading, 0, event.values.length);
//        }
//        updateCompass();
//    }
//
//    private void updateCompass() {
//        float[] rotationMatrix = new float[9];
//        boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
//        if (success && userMarker != null) {
//            float[] orientationAngles = new float[3];
//            SensorManager.getOrientation(rotationMatrix, orientationAngles);
//            float azimuthDegrees = (float) Math.toDegrees(orientationAngles[0]);
//            userMarker.setRotation(-azimuthDegrees);
//        }
//    }
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        // Not used
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        if (accelerometer != null) {
//            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
//        }
//        if (magnetometer != null) {
//            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
//        }
//        startLocationUpdates();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        sensorManager.unregisterListener(this);
//        fusedLocationClient.removeLocationUpdates(locationCallback);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                        == PackageManager.PERMISSION_GRANTED) {
//                    if (googleMap != null) {
//                        googleMap.setMyLocationEnabled(true);
//                    }
//                    startLocationUpdates();
//                }
//            } else {
//                Log.e(TAG, "Location permission denied");
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }
//}
//




///this not accurate
//package com.example.resort;
//
//import android.Manifest;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//import android.location.Location;
//import android.os.Bundle;
//import android.os.Looper;
//import android.util.Log;
//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;
//import androidx.fragment.app.Fragment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.Polyline;
//import com.google.android.gms.maps.model.PolylineOptions;
//import java.util.Locale;
//
//public class ExploreFragment extends Fragment implements OnMapReadyCallback, SensorEventListener {
//
//    private static final String TAG = "ExploreFragment";
//    private GoogleMap googleMap;
//    private FusedLocationProviderClient fusedLocationClient;
//    private LocationCallback locationCallback;
//    private Marker userMarker;
//    private Polyline polyline;
//    private TextView tvDistance, tvETA;
//    private Button btnFollow, btnRefresh;
//
//    // Destination: Island View Beachfront Resort
//    private final LatLng destinationLatLng = new LatLng(9.730524256180338, 124.55669380904);
//
//    // Sensor management for compass functionality
//    private SensorManager sensorManager;
//    private float[] accelerometerReading = new float[3];
//    private float[] magnetometerReading = new float[3];
//
//    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
//    // Average walking speed (~83 m/min)
//    private final double AVERAGE_WALKING_SPEED = 83.0;
//    // Set follow mode true by default so that camera centers on your location
//    private boolean isFollowing = true;
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.explore_fragment, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        tvDistance = view.findViewById(R.id.tvDistance);
//        tvETA = view.findViewById(R.id.tvETA);
//        btnFollow = view.findViewById(R.id.btnFollow);
//        btnRefresh = view.findViewById(R.id.btnRefresh);
//
//        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
//
//        // Obtain the SupportMapFragment and set its async callback
//        SupportMapFragment mapFragment = (SupportMapFragment)
//                getChildFragmentManager().findFragmentById(R.id.map);
//        if (mapFragment != null) {
//            mapFragment.getMapAsync(this);
//        } else {
//            Log.e(TAG, "MapFragment is null! Check your layout.");
//        }
//
//        // Set up location callback for receiving location updates
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                if (locationResult == null) {
//                    Log.w(TAG, "LocationResult is null");
//                    return;
//                }
//                Location location = locationResult.getLastLocation();
//                Log.d(TAG, "Location update: " + location.getLatitude() + ", " + location.getLongitude());
//                updateLocationOnMap(location);
//                updateDistanceAndETA(location);
//            }
//        };
//
//        // Toggle follow mode
//        btnFollow.setOnClickListener(v -> {
//            isFollowing = !isFollowing;
//            btnFollow.setText(isFollowing ? "Following" : "Follow");
//            if (userMarker != null && isFollowing) {
//                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), 16f));
//            }
//        });
//
//        // Refresh button to manually update distance and ETA
//        btnRefresh.setOnClickListener(v -> fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
//            if (location != null) {
//                updateDistanceAndETA(location);
//            } else {
//                Log.w(TAG, "Last location is null");
//            }
//        }));
//    }
//
//    @Override
//    public void onMapReady(@NonNull GoogleMap map) {
//        googleMap = map;
//        // Add destination marker
//        googleMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Island View Beachfront Resort"));
//
//        // Check for location permission and start location updates
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    LOCATION_PERMISSION_REQUEST_CODE);
//        } else {
//            googleMap.setMyLocationEnabled(true);
//            startLocationUpdates();
//        }
//    }
//
//    private void startLocationUpdates() {
//        LocationRequest locationRequest = LocationRequest.create();
//        locationRequest.setInterval(5000); // Update every 5 seconds
//        locationRequest.setFastestInterval(2000); // Fastest update every 2 seconds
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            Log.e(TAG, "Location permission not granted!");
//            return;
//        }
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//    }
//
//    private void updateLocationOnMap(Location location) {
//        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//        if (userMarker == null) {
//            userMarker = googleMap.addMarker(new MarkerOptions().position(userLatLng).title("You"));
//        } else {
//            userMarker.setPosition(userLatLng);
//        }
//        if (isFollowing) {
//            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f));
//        }
//        drawPolyline(userLatLng, destinationLatLng);
//    }
//
//    private void drawPolyline(LatLng start, LatLng end) {
//        if (polyline != null) {
//            polyline.remove();
//        }
//        polyline = googleMap.addPolyline(new PolylineOptions()
//                .add(start, end)
//                .color(Color.BLUE)
//                .width(5f));
//    }
//
//    private void updateDistanceAndETA(Location location) {
//        float[] results = new float[1];
//        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
//                destinationLatLng.latitude, destinationLatLng.longitude, results);
//        float distanceMeters = results[0];
//        double distanceKm = distanceMeters / 1000.0;
//        tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distanceKm));
//
//        int etaMinutes = AVERAGE_WALKING_SPEED > 0 ? (int) Math.round(distanceMeters / AVERAGE_WALKING_SPEED) : 0;
//        tvETA.setText("ETA: " + etaMinutes + " min");
//    }
//
//    // SensorEventListener methods for compass functionality
//    @Override
//    public void onSensorChanged(SensorEvent event) {
//        if (event == null) return;
//        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            System.arraycopy(event.values, 0, accelerometerReading, 0, event.values.length);
//        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//            System.arraycopy(event.values, 0, magnetometerReading, 0, event.values.length);
//        }
//        updateCompass();
//    }
//
//    private void updateCompass() {
//        float[] rotationMatrix = new float[9];
//        boolean success = SensorManager.getRotationMatrix(rotationMatrix, null,
//                accelerometerReading, magnetometerReading);
//        if (success && userMarker != null) {
//            float[] orientationAngles = new float[3];
//            SensorManager.getOrientation(rotationMatrix, orientationAngles);
//            float azimuthRadians = orientationAngles[0];
//            float azimuthDegrees = (float) Math.toDegrees(azimuthRadians);
//            userMarker.setRotation(-azimuthDegrees);
//        }
//    }
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        // Not used
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        // Register sensor listeners
//        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        if (accelerometer != null) {
//            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
//        }
//        if (magnetometer != null) {
//            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
//        }
//        startLocationUpdates();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        sensorManager.unregisterListener(this);
//        fusedLocationClient.removeLocationUpdates(locationCallback);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 &&
//                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                        == PackageManager.PERMISSION_GRANTED) {
//                    if (googleMap != null) {
//                        googleMap.setMyLocationEnabled(true);
//                    }
//                    startLocationUpdates();
//                }
//            } else {
//                Log.e(TAG, "Location permission denied");
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }
//}




//package com.example.resort;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.content.Context;
//import android.content.pm.PackageManager;
//import android.graphics.Color;
//import android.hardware.Sensor;
//import android.hardware.SensorEvent;
//import android.hardware.SensorEventListener;
//import android.hardware.SensorManager;
//import android.location.Location;
//import android.os.Bundle;
//import android.os.Looper;
//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;
//import androidx.fragment.app.Fragment;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.Button;
//import android.widget.TextView;
//import com.google.android.gms.location.FusedLocationProviderClient;
//import com.google.android.gms.location.LocationCallback;
//import com.google.android.gms.location.LocationRequest;
//import com.google.android.gms.location.LocationResult;
//import com.google.android.gms.location.LocationServices;
//import com.google.android.gms.maps.CameraUpdateFactory;
//import com.google.android.gms.maps.GoogleMap;
//import com.google.android.gms.maps.OnMapReadyCallback;
//import com.google.android.gms.maps.SupportMapFragment;
//import com.google.android.gms.maps.model.LatLng;
//import com.google.android.gms.maps.model.Marker;
//import com.google.android.gms.maps.model.MarkerOptions;
//import com.google.android.gms.maps.model.Polyline;
//import com.google.android.gms.maps.model.PolylineOptions;
//import java.util.Locale;
//
//public class ExploreFragment extends Fragment implements OnMapReadyCallback, SensorEventListener {
//
//    private GoogleMap googleMap;
//    private FusedLocationProviderClient fusedLocationClient;
//    private LocationCallback locationCallback;
//    private Marker userMarker;
//    private Polyline polyline;
//    private TextView tvDistance, tvETA;
//    private Button btnFollow, btnRefresh;
//
//    // Destination: Island View Beachfront Resort (coordinates from your provided link)
//    private final LatLng destinationLatLng = new LatLng(9.730524256180338, 124.55669380904);
//
//    // Sensor management for compass functionality
//    private SensorManager sensorManager;
//    private float[] accelerometerReading = new float[3];
//    private float[] magnetometerReading = new float[3];
//
//    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
//    // Average walking speed (~83 m/min or about 5 km/h)
//    private final double AVERAGE_WALKING_SPEED = 83.0;
//    private boolean isFollowing = false;
//
//    @Override
//    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout (ensure your layout file contains a SupportMapFragment with id "map"
//        // and the necessary UI elements with IDs tvDistance, tvETA, btnFollow, btnRefresh)
//        return inflater.inflate(R.layout.explore_fragment, container, false);
//    }
//
//    @SuppressLint("MissingPermission")
//    @Override
//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        tvDistance = view.findViewById(R.id.tvDistance);
//        tvETA = view.findViewById(R.id.tvETA);
//        btnFollow = view.findViewById(R.id.btnFollow);
//        btnRefresh = view.findViewById(R.id.btnRefresh);
//
//        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
//        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
//
//        SupportMapFragment mapFragment = (SupportMapFragment)
//                getChildFragmentManager().findFragmentById(R.id.map);
//        if (mapFragment != null) {
//            mapFragment.getMapAsync(this);
//        }
//
//        // Set up location callback to receive location updates
//        locationCallback = new LocationCallback() {
//            @Override
//            public void onLocationResult(LocationResult locationResult) {
//                if (locationResult == null) return;
//                Location location = locationResult.getLastLocation();
//                updateLocationOnMap(location);
//                updateDistanceAndETA(location);
//            }
//        };
//
//        // Toggle follow mode to center map on user location
//        btnFollow.setOnClickListener(v -> {
//            isFollowing = !isFollowing;
//            btnFollow.setText(isFollowing ? "Following" : "Follow");
//            if (userMarker != null && isFollowing) {
//                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), 16f));
//            }
//        });
//
//        // Refresh button to update distance and ETA manually
//        btnRefresh.setOnClickListener(v -> {
//            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
//                if (location != null) {
//                    updateDistanceAndETA(location);
//                }
//            });
//        });
//    }
//
//    @Override
//    public void onMapReady(@NonNull GoogleMap map) {
//        googleMap = map;
//        // Add a marker for the destination with the provided location details
//        googleMap.addMarker(new MarkerOptions()
//                .position(destinationLatLng)
//                .title("Island View Beachfront Resort"));
//
//        // Check for location permission and start location updates if granted
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
//                    LOCATION_PERMISSION_REQUEST_CODE);
//        } else {
//            startLocationUpdates();
//            googleMap.setMyLocationEnabled(true);
//        }
//    }
//
//    private void startLocationUpdates() {
//        LocationRequest locationRequest = LocationRequest.create();
//        locationRequest.setInterval(5000); // 5 seconds
//        locationRequest.setFastestInterval(2000); // 2 seconds
//        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                != PackageManager.PERMISSION_GRANTED) {
//            return;
//        }
//        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
//    }
//
//    private void updateLocationOnMap(Location location) {
//        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
//        if (userMarker == null) {
//            userMarker = googleMap.addMarker(new MarkerOptions().position(userLatLng).title("You"));
//        } else {
//            userMarker.setPosition(userLatLng);
//        }
//        if (isFollowing) {
//            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 16f));
//        }
//        drawPolyline(userLatLng, destinationLatLng);
//    }
//
//    private void drawPolyline(LatLng start, LatLng end) {
//        if (polyline != null) {
//            polyline.remove();
//        }
//        polyline = googleMap.addPolyline(new PolylineOptions()
//                .add(start, end)
//                .color(Color.BLUE)
//                .width(5f));
//    }
//
//    private void updateDistanceAndETA(Location location) {
//        float[] results = new float[1];
//        Location.distanceBetween(location.getLatitude(), location.getLongitude(),
//                destinationLatLng.latitude, destinationLatLng.longitude, results);
//        float distanceMeters = results[0];
//        double distanceKm = distanceMeters / 1000.0;
//        tvDistance.setText(String.format(Locale.getDefault(), "Distance: %.2f km", distanceKm));
//
//        // Calculate ETA in minutes using average walking speed
//        int etaMinutes = AVERAGE_WALKING_SPEED > 0 ? (int) Math.round(distanceMeters / AVERAGE_WALKING_SPEED) : 0;
//        tvETA.setText("ETA: " + etaMinutes + " min");
//    }
//
//    // SensorEventListener methods for compass functionality
//    @Override
//    public void onSensorChanged(SensorEvent event) {
//        if (event == null) return;
//        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
//            System.arraycopy(event.values, 0, accelerometerReading, 0, event.values.length);
//        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
//            System.arraycopy(event.values, 0, magnetometerReading, 0, event.values.length);
//        }
//        updateCompass();
//    }
//
//    private void updateCompass() {
//        float[] rotationMatrix = new float[9];
//        boolean success = SensorManager.getRotationMatrix(rotationMatrix, null,
//                accelerometerReading, magnetometerReading);
//        if (success && userMarker != null) {
//            float[] orientationAngles = new float[3];
//            SensorManager.getOrientation(rotationMatrix, orientationAngles);
//            float azimuthRadians = orientationAngles[0];
//            float azimuthDegrees = (float) Math.toDegrees(azimuthRadians);
//            userMarker.setRotation(-azimuthDegrees);
//        }
//    }
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//        // No action needed
//    }
//
//    @Override
//    public void onResume() {
//        super.onResume();
//        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        Sensor magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
//        if (accelerometer != null) {
//            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
//        }
//        if (magnetometer != null) {
//            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_UI);
//        }
//        startLocationUpdates();
//    }
//
//    @Override
//    public void onPause() {
//        super.onPause();
//        sensorManager.unregisterListener(this);
//        fusedLocationClient.removeLocationUpdates(locationCallback);
//    }
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            if (grantResults.length > 0 &&
//                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
//                        == PackageManager.PERMISSION_GRANTED) {
//                    startLocationUpdates();
//                    if (googleMap != null) {
//                        googleMap.setMyLocationEnabled(true);
//                    }
//                }
//            }
//        } else {
//            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        }
//    }
//}



///this free oly view in the map
//package com.example.resort;
//
//import android.Manifest;
//import android.annotation.SuppressLint;
//import android.content.pm.PackageManager;
//import android.os.Bundle;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.webkit.WebChromeClient;
//import android.webkit.WebSettings;
//import android.webkit.WebView;
//import android.webkit.WebViewClient;
//import androidx.annotation.NonNull;
//import androidx.core.app.ActivityCompat;
//import androidx.fragment.app.Fragment;
//
//public class ExploreFragment extends Fragment {
//
//    private WebView webView;
//    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        return inflater.inflate(R.layout.explore_fragment, container, false);
//    }
//
//    @Override
//    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
//        super.onViewCreated(view, savedInstanceState);
//
//        webView = view.findViewById(R.id.webview);
//        setupWebView();
//
//        // Check if location permissions are granted
//        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
//        } else {
//            loadGoogleMaps();
//        }
//    }
//
//    @SuppressLint("SetJavaScriptEnabled")
//    private void setupWebView() {
//        WebSettings webSettings = webView.getSettings();
//        webSettings.setJavaScriptEnabled(true); // Enable JavaScript
//        webSettings.setDomStorageEnabled(true); // Enable DOM Storage
//        webSettings.setGeolocationEnabled(true); // Enable Geolocation
//
//        // Handle geolocation permission request in WebView
//        webView.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public void onGeolocationPermissionsShowPrompt(String origin, android.webkit.GeolocationPermissions.Callback callback) {
//                // Grant permission for geolocation
//                callback.invoke(origin, true, false); // 'true' to allow location access
//            }
//        });
//
//        // Prevent redirects from opening in external browser
//        webView.setWebViewClient(new WebViewClient());
//    }
//
//    private void loadGoogleMaps() {
//        String html = "<html>" +
//                "<body style=\"margin:0;padding:0;\">" +
//                "<iframe width=\"100%\" height=\"100%\" " +
//                "src=\"https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3932.419065203344!2d124.55669380904!3d9.730524256180338!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x3300a104bf34b06d%3A0x41eb95bb35e8d20c!2sIsland%20View%20Beachfront%20Resort!5e0!3m2!1sfil!2sph!4v1738849880663!5m2!1sfil!2sph\" " +
//                "frame border=\"0\" style=\"border:0;\" allow fullscreen></iframe>" +
//                "</body>" +
//                "</html>";
//        webView.loadData(html, "text/html", "utf-8");
//    }
//
//
//
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
//            //noinspection StatementWithEmptyBody
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                loadGoogleMaps(); // Reload Google Maps if permission is granted
//            } else {
//                // Handle the case where permission is denied
//                // You can show a message or alert dialog here.
//            }
//        }
//    }
//}
