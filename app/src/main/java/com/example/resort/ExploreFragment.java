package com.example.resort;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
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
    private final LatLng destinationLatLng = new LatLng(10.1341723, 124.3829805);
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
    /// Average walking speed (~83 m/min)
    private final double AVERAGE_WALKING_SPEED = 83.0;
    // Toggle for following user's location.
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


        /// Toggle follow mode: the user can enable/disable auto-updating of the camera.
        btnFollow.setOnClickListener(v -> {
            isFollowing = !v.isSelected();
            v.setSelected(isFollowing);
            int color = ContextCompat.getColor(requireContext(), isFollowing ? R.color.light_blue : R.color.white);
            btnFollow.setBackgroundTintList(ColorStateList.valueOf(color));

            // Change the map type when follow mode is toggled.
            if (userMarker != null) {
                googleMap.setMapType(isFollowing ? GoogleMap.MAP_TYPE_HYBRID : GoogleMap.MAP_TYPE_NORMAL);
                if (isFollowing) {
                    updateCameraFixedZoom();
                }
            }
        });

        // Refresh button to update distance and ETA manually.
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
        // Initially center the camera on the destination with a default zoom level.
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(destinationLatLng, 12f));
        // Add the resort marker.
        googleMap.addMarker(new MarkerOptions().position(destinationLatLng).title("Island View Beachfront Resort"));

        // Enable the built-in MyLocation layer if permission is granted.
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            googleMap.setMyLocationEnabled(true);
            startLocationUpdates();

            // Automatically enable follow mode without modifying the follow button's appearance.
            isFollowing = true;
            updateCameraFixedZoom();
        }

        // Listener to disable follow mode on manual camera gesture.
        googleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int reason) {
                // Disable follow mode if the user moves the camera manually.
                if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE && isFollowing) {
                    isFollowing = false;
                    // Optionally, update the follow button appearance since user interacted.
                    btnFollow.setSelected(false);
                    btnFollow.setBackgroundTintList(ColorStateList.valueOf(
                            ContextCompat.getColor(requireContext(), R.color.white)
                    ));
                }
            }
        });
    }


    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create();
        // Set the update interval to 1 second.
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
        // Add or update the user marker.
        if (userMarker == null) {
            userMarker = googleMap.addMarker(new MarkerOptions().position(userLatLng).title("You"));
            startBlinking();
        } else {
            userMarker.setPosition(userLatLng);
        }
        // Draw the route from the current location to the destination.
        fetchRoute(userLatLng, destinationLatLng);

        // Update camera only if follow mode is active.
        if (isFollowing) {
            updateCameraFixedZoom();
        }
    }

    // This method dynamically adjusts the camera zoom level based on the distance between the user and destination,
    // sets a 3D tilt (45°), and rotates the camera to face the destination.
    private void updateCameraFixedZoom() {
        if (userMarker == null) return;

        // Calculate the midpoint between the user's location and the destination.
        double midLat = (userMarker.getPosition().latitude + destinationLatLng.latitude) / 2;
        double midLng = (userMarker.getPosition().longitude + destinationLatLng.longitude) / 2;
        LatLng midPoint = new LatLng(midLat, midLng);

        // Determine distance in meters between the user and the destination.
        float[] results = new float[1];
        Location.distanceBetween(userMarker.getPosition().latitude, userMarker.getPosition().longitude,
                destinationLatLng.latitude, destinationLatLng.longitude, results);
        float distanceMeters = results[0];

        // Dynamically adjust zoom level based on distance.
        float dynamicZoom;
        if (distanceMeters < 1000) {
            dynamicZoom = 17f;
        } else if (distanceMeters < 3000) {
            dynamicZoom = 15f;
        } else if (distanceMeters < 5000) {
            dynamicZoom = 13f;
        } else {
            dynamicZoom = 12f;
        }

        // Compute bearing from the user's location to the destination.
        float bearing = getBearing(userMarker.getPosition(), destinationLatLng);

        // Build the camera position with 3D tilt and the calculated dynamic zoom and bearing.
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(midPoint)
                .zoom(dynamicZoom)
                .tilt(45)       // 3D effect tilt angle.
                .bearing(bearing)   // Rotate so the destination is in view.
                .build();

        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    // Helper method to calculate bearing between two LatLng points.
    private float getBearing(LatLng from, LatLng to) {
        double fromLat = Math.toRadians(from.latitude);
        double fromLng = Math.toRadians(from.longitude);
        double toLat = Math.toRadians(to.latitude);
        double toLng = Math.toRadians(to.longitude);
        double deltaLng = toLng - fromLng;
        double y = Math.sin(deltaLng) * Math.cos(toLat);
        double x = Math.cos(fromLat) * Math.sin(toLat) -
                Math.sin(fromLat) * Math.cos(toLat) * Math.cos(deltaLng);
        double bearing = Math.atan2(y, x);
        bearing = Math.toDegrees(bearing);
        return (float)((bearing + 360) % 360);
    }

    /// Fetch the route from Google Directions API and draw a polyline on the map.
    private void fetchRoute(LatLng origin, LatLng destination) {
        // Replace YOUR_API_KEY_HERE with your actual API key.
        String apiKey = "AIzaSyBmR1p6AFBkS3GcuL9nabMTc39bztQnwgU";
        // Using walking mode and forcing the region to PH (Philippines).
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
                    // Remove the old polyline if it exists.
                    if (routePolyline != null) {
                        routePolyline.remove();
                    }
                    PolylineOptions polylineOptions = new PolylineOptions()
                            .addAll(points)
                            .width(15f)
                            .color(Color.BLUE)
                            .geodesic(true);
                    routePolyline = googleMap.addPolyline(polylineOptions);

                    // Update camera if follow mode is active.
                    if (isFollowing) {
                        updateCameraFixedZoom();
                    }
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
        stopBlinking();
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


    /// Declare a Handler and flag for blinking.
    private final Handler blinkHandler = new Handler();
    private boolean isMarkerVisible = true;
    private boolean isBlinking = false;

    /// For destination (resort) marker blinking.
    private final Handler destBlinkHandler = new Handler();
    private boolean isDestMarkerVisible = true;
    private boolean isDestBlinking = false;

    // Runnable that toggles marker visibility.
    private final Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            if (userMarker != null) {
                isMarkerVisible = !isMarkerVisible;
                userMarker.setVisible(isMarkerVisible);
            }
            // Repeat the runnable after 500ms (adjust the delay as needed).
            blinkHandler.postDelayed(this, 500);
        }
    };

    private void startBlinking() {
        if (!isBlinking) {
            isBlinking = true;
            blinkHandler.post(blinkRunnable);
        }
    }

    // Stop blinking effect.
    private void stopBlinking() {
        isBlinking = false;
        blinkHandler.removeCallbacks(blinkRunnable);
        // Ensure marker is visible when blinking stops.
        if (userMarker != null) {
            userMarker.setVisible(true);
        }
    }


}


///The map so active i cant scroll
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
//    /// Destination: Island View Beachfront Resort in Bohol, Philippines.
//    private final LatLng destinationLatLng = new LatLng( 10.1341723, 124.3829805);
//    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1000;
//    /// Average walking speed (~83 m/min)
//    private final double AVERAGE_WALKING_SPEED = 83.0;
//    // Toggle for following user's location. In our update, follow mode now only toggles the button color,
//    // and the camera always adjusts to show both markers (fixed zoom).
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
//        /// Set up the location callback to update location, route, and distance/ETA
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
//        /// Toggle follow mode: now, regardless of toggle, we always update the camera to show both markers.
//        btnFollow.setOnClickListener(v -> {
//            isFollowing = !v.isSelected();
//            v.setSelected(isFollowing);
//            int color = ContextCompat.getColor(requireContext(), isFollowing ? R.color.light_blue : R.color.white);
//            btnFollow.setBackgroundTintList(ColorStateList.valueOf(color));
//
//            if (userMarker != null) {
//
//                googleMap.setMapType(isFollowing ? GoogleMap.MAP_TYPE_HYBRID : GoogleMap.MAP_TYPE_NORMAL);
//
//                /// Adjust camera
//                updateCameraFixedZoom();
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
//        // Always update camera view to show both markers if needed.
//        updateCameraFixedZoom();
//    }
//
//    // Calculate the midpoint between the user's location and the destination
//    // and update the camera with a fixed zoom level (11.8f)
//    private void updateCameraFixedZoom() {
//        if (userMarker == null) return;
//        double midLat = (userMarker.getPosition().latitude + destinationLatLng.latitude) / 2;
//        double midLng = (userMarker.getPosition().longitude + destinationLatLng.longitude) / 2;
//        LatLng midPoint = new LatLng(midLat, midLng);
//        CameraPosition cameraPosition = new CameraPosition.Builder()
//                .target(midPoint)
//                .zoom(12.3f)  /// Fixed zoom level; adjust as needed
//                .tilt(0)
//                .build();
//        googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//    }
//
//    /// Fetch the route from Google Directions API and draw a polyline on the map.
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
//                            .width(15f)
//                            .color(Color.BLUE)
//                            .geodesic(true);
//                    routePolyline = googleMap.addPolyline(polylineOptions);
//
//                    // Update camera if needed (this update now always shows both markers)
//                    updateCameraFixedZoom();
//                } else {
//                    Log.e(TAG, "No routes found in the response.");
//                }
//            } catch (JSONException e) {
//                Log.e(TAG, "JSON parsing error: " + e.toString());
//            }
//        }
//    }
//
//
//    /// Decode an encoded polyline string into a list of LatLng points.
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


