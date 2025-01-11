package com.example.grouplocation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.grouplocation.utilities.Constants;
import com.example.grouplocation.utilities.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LocationService extends Service {
    private static final String TAG = "LocationService";
    private static final String CHANNEL_ID = "LocationServiceChannel";
    private FusedLocationProviderClient fusedLocationProviderClient;
    private LocationCallback locationCallback;

    private DocumentReference docRef;

    private String userName;

    private PreferenceManager preferenceManager;

    @Override
    public void onCreate() {
        super.onCreate();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        FirebaseFirestore dataBase = FirebaseFirestore.getInstance();
        preferenceManager = new PreferenceManager(this);
        userName = preferenceManager.getString(Constants.KEY_NAME);
        docRef = dataBase.collection(Constants.KEY_COLLECTION_GROUPS).document(preferenceManager.getString(Constants.KEY_GROUP_NAME));
        createNotificationChannel();
        startForeground(1, buildNotification("Initializing location service..."));
        startLocationUpdates();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Restart if the service is killed
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (fusedLocationProviderClient != null && locationCallback != null) {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Not a bound service
    }

    private void startLocationUpdates() {
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(10000)
                .setFastestInterval(10000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        HashMap<String, Object> list = new HashMap<>();
                        list.put(Constants.KEY_LATITUDE, location.getLatitude());
                        list.put(Constants.KEY_LONGITUDE, location.getLongitude());
                        docRef.get().addOnSuccessListener(documentSnapshot -> {
                            if (documentSnapshot.exists()) {
                                // Get the current array (assuming it is under the field "locations")
                                HashMap<String, Object> user = (HashMap<String, Object>) documentSnapshot.get(userName);

                                if (user != null) {
                                    // Iterate through the array to find and update the map in the array
                                    user.put(Constants.KEY_LATITUDE, location.getLatitude());
                                    user.put(Constants.KEY_LONGITUDE, location.getLongitude());
                                    HashMap<String, Object> pointsList = new HashMap<>();
                                    if (user.containsKey("points")) {
                                        pointsList = (HashMap<String, Object>) user.get("points");
                                    }
                                    int size = pointsList.size();
                                    HashMap<String, Object> twoPoints = new HashMap<>();
                                    twoPoints.put(Constants.KEY_LATITUDE, location.getLatitude());
                                    twoPoints.put(Constants.KEY_LONGITUDE, location.getLongitude());
                                    pointsList.put(String.valueOf(size), twoPoints);
                                    user.put("points", pointsList);
                                    // Update the document with the modified array
                                    docRef.update(userName, user)
                                            .addOnSuccessListener(aVoid -> {
                                                updateNotification("Location: " + location.getLatitude() + ", " + location.getLongitude());
                                            });
                                }
                            }
                        });
                    }
                }
            }
        };

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
            }
        } else {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification buildNotification(String contentText) {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText(contentText)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String contentText) {
        Notification notification = buildNotification(contentText);
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(1, notification);
        }
    }
}


