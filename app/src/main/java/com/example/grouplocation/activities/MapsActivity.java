package com.example.grouplocation.activities;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;

import com.example.grouplocation.LocationService;
import com.example.grouplocation.MainActivity;
import com.example.grouplocation.R;
import com.example.grouplocation.utilities.Constants;
import com.example.grouplocation.utilities.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.grouplocation.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private FusedLocationProviderClient fusedLocationClient;
    private GoogleMap mMap;

    private List<Polyline> polylines = new ArrayList<>();

    List<Pair<Marker, Bitmap>> markersWithDrawables = new ArrayList<>();
    private LocationCallback locationCallback;

    private Button button;
    private ActivityMapsBinding binding;

    private static final int REQUEST_CODE_NOTIFICATION_PERMISSION = 1001;

    private static final int PERMISSION_REQUEST_CODE = 100;

    private Boolean First = true;

    private PreferenceManager preferenceManager;
    double latitude;
    Bitmap bitmap;
    private DocumentReference docRef;
    double longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                // Request the POST_NOTIFICATIONS permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_CODE_NOTIFICATION_PERMISSION);
            }
        }
        preferenceManager = new PreferenceManager(this);
        FirebaseFirestore dataBase = FirebaseFirestore.getInstance();
        docRef = dataBase.collection(Constants.KEY_COLLECTION_GROUPS).document(preferenceManager.getString(Constants.KEY_GROUP_NAME));
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE), Base64.DEFAULT);
        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        bitmap = getCircularBitmap(bitmap);
        button = findViewById(R.id.leaveBtn);
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get the current array (assuming it is under the field "locations")
                HashMap<String, Object> documentData = (HashMap<String, Object>) documentSnapshot.getData();

                if (documentData != null) {
                    // Iterate over all fields in the document
                    HashMap<String,Object> user = (HashMap<String, Object>) documentData.get(preferenceManager.getString(Constants.KEY_NAME));
                    String bol = (String) user.get(Constants.KEY_NAME_HOST);
                    if (bol.equals("true")) {
                        button.setText("End");
                    }
                }
            }
        });
        setListeners();
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        checkAndRequestPermissions();

    }

    public void setListeners() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ((button.getText().toString()).equals("End")) {
                    docRef.delete();
                } else {
                    docRef.update(preferenceManager.getString(Constants.KEY_NAME), FieldValue.delete());
                }
                Intent serviceIntent = new Intent(getApplicationContext(), LocationService.class);
                stopService(serviceIntent);
                preferenceManager.putBoolean(Constants.KEY_HAS_GROUP, false);
                preferenceManager.putString(Constants.KEY_GROUP_NAME, "");
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        updateUsersLocation();
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                changeZoom();
            }
        });
        // Add a marker in Sydney and move the camera
    }

    protected Bitmap getCircularBitmap(Bitmap srcBitmap) {
        // Calculate the circular bitmap width with border
        int squareBitmapWidth = Math.min(srcBitmap.getWidth(), srcBitmap.getHeight());
        // Initialize a new instance of Bitmap
        Bitmap dstBitmap = Bitmap.createBitmap (
                squareBitmapWidth, // Width
                squareBitmapWidth, // Height
                Bitmap.Config.ARGB_8888 // Config
        );
        Canvas canvas = new Canvas(dstBitmap);
        // Initialize a new Paint instance
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        Rect rect = new Rect(0, 0, squareBitmapWidth, squareBitmapWidth);
        RectF rectF = new RectF(rect);
        canvas.drawOval(rectF, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        // Calculate the left and top of copied bitmap
        float left = (squareBitmapWidth-srcBitmap.getWidth())/2;
        float top = (squareBitmapWidth-srcBitmap.getHeight())/2;
        canvas.drawBitmap(srcBitmap, left, top, paint);
        // Free the native object associated with this bitmap.
        srcBitmap.recycle();
        // Return the circular bitmap
        return dstBitmap;
    }

    private void checkAndRequestPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.FOREGROUND_SERVICE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                            Manifest.permission.POST_NOTIFICATIONS
                    },
                    PERMISSION_REQUEST_CODE);
        } else {
            startLocationService();
        }
    }

    private void startLocationService() {
        Intent serviceIntent = new Intent(this, LocationService.class);
        ContextCompat.startForegroundService(this, serviceIntent);
    }
    private void updateUsersLocation() {
        markersWithDrawables = new ArrayList<>();
        removeAllMarkers();
        removeAllPolylines();
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                // Get the current array (assuming it is under the field "locations")
                HashMap<String, Object> documentData = (HashMap<String, Object>) documentSnapshot.getData();

                if (documentData != null) {
                    // Iterate over all fields in the document
                    for (HashMap.Entry<String, Object> entry : documentData.entrySet()) {
                        String fieldName = entry.getKey();
                        Object fieldValue = entry.getValue();

                        // If the field value is a Map, you can process it further (nested map)
                        if (fieldValue instanceof HashMap) {
                            HashMap<String, Object> nestedMap = (HashMap<String, Object>) fieldValue;
                            byte[] decodedString = Base64.decode(nestedMap.get(Constants.KEY_IMAGE).toString(), Base64.DEFAULT);

                            // Convert the byte array to a Bitmap
                            Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                            BitmapDescriptor customIcon = BitmapDescriptorFactory.fromBitmap(decodedBitmap);
                            LatLng userLocation = new LatLng(Double.parseDouble(nestedMap.get(Constants.KEY_LATITUDE).toString()), Double.parseDouble( nestedMap.get(Constants.KEY_LONGITUDE).toString()));
                            Marker marker = mMap.addMarker(new MarkerOptions().position(userLocation).title(fieldName).icon(customIcon));
                            markersWithDrawables.add(new Pair<>(marker, decodedBitmap));
                            if (fieldName.equals(preferenceManager.getString(Constants.KEY_NAME)) && First && !nestedMap.get(Constants.KEY_LATITUDE).toString().equals("0")) {
                                First = false;
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 17));
                            }
                            if (nestedMap.containsKey("points")) {
                                int color = Integer.parseInt(nestedMap.get(Constants.KEY_COLOR).toString());
                                List<LatLng> points = new ArrayList<>();
                                HashMap<String, Object> pointsFirebase = (HashMap<String, Object>) nestedMap.get("points");
                                for (int i = 0; i < pointsFirebase.size(); i++) {
                                    HashMap<String, Object> twoPoints = (HashMap<String, Object>) pointsFirebase.get(String.valueOf(i));
                                    Double latitude = Double.parseDouble(twoPoints.get(Constants.KEY_LATITUDE).toString());
                                    Double longitude = Double.parseDouble(twoPoints.get(Constants.KEY_LONGITUDE).toString());
                                    points.add(new LatLng(latitude, longitude));
                                }
                                addPoly(points, color);
                            }
                        }
                    }
                    changeZoom();
                }
            }
        });
        int delay;
        if (First) {
            delay = 1000;
        } else {
            delay = 10000;
        }
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // Your code here
                updateUsersLocation();
            }
        }, delay);
    }

    public void addPoly(List<LatLng> points, int color) {
        Polyline polyline = mMap.addPolyline(new PolylineOptions()
                .addAll(points) // Add all points from the list
                .width(50)      // Line width in pixels
                .color(color));

        polylines.add(polyline);
    }

    private void removeAllPolylines() {
        for (Polyline polyline : polylines) {
            polyline.remove(); // Remove the polyline from the map
        }
        polylines.clear(); // Clear the list
    }

    public void changeZoom() {
        float zoomLevel = mMap.getCameraPosition().zoom;

        // Calculate the scale based on zoom level
        float scale = 1 + (zoomLevel - 17) * 0.1f;  // Adjust this scale factor

        // Iterate over all markers and their associated drawables in the list
        for (Pair<Marker, Bitmap> entry : markersWithDrawables) {
            Marker marker = entry.first;
            Bitmap originalBitmap = entry.second;

            // Resize the bitmap based on the zoom level
            Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap,
                    (int) (originalBitmap.getWidth() * scale),
                    (int) (originalBitmap.getHeight() * scale), false);

            // Update the marker's icon with the resized bitmap
            marker.setIcon(BitmapDescriptorFactory.fromBitmap(resizedBitmap));
        }
    }

    public void removeAllMarkers() {
        if (mMap != null) {
            mMap.clear(); // Clears all markers and overlays
        }
    }
}