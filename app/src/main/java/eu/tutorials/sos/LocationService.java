    package eu.tutorials.sos;

    import android.app.Notification;
    import android.app.NotificationChannel;
    import android.app.NotificationManager;
    import android.app.Service;
    import android.content.Context;
    import android.content.Intent;
    import android.content.pm.PackageManager;
    import android.location.Location;
    import android.os.Build;
    import android.os.IBinder;
    import android.util.Log;
    import androidx.annotation.Nullable;
    import androidx.core.app.ActivityCompat;
    import androidx.core.app.NotificationCompat;
    import com.google.android.gms.location.FusedLocationProviderClient;
    import com.google.android.gms.location.LocationCallback;
    import com.google.android.gms.location.LocationRequest;
    import com.google.android.gms.location.LocationResult;
    import com.google.android.gms.location.LocationServices;
    import com.google.firebase.auth.FirebaseAuth;
    import com.google.firebase.firestore.FirebaseFirestore;

    public class LocationService extends Service {

        private FusedLocationProviderClient fusedLocationClient;
        private FirebaseFirestore firestore;
        private FirebaseAuth mAuth;
        private static final String CHANNEL_ID = "LocationServiceChannel";
        private static final int NOTIFICATION_ID = 1;
        private LocationCallback locationCallback;

        @Override
        public void onCreate() {
            super.onCreate();
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
            firestore = FirebaseFirestore.getInstance();
            mAuth = FirebaseAuth.getInstance();

            createNotificationChannel();
            startForegroundService();
            startLocationUpdates();
            requestSingleLocationUpdate();
        }

        private void createNotificationChannel() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "Location Service Channel",
                        NotificationManager.IMPORTANCE_LOW // Use IMPORTANCE_LOW or higher based on your needs
                );
                channel.setDescription("Channel for location service notifications");
                NotificationManager manager = getSystemService(NotificationManager.class);
                if (manager != null) {
                    manager.createNotificationChannel(channel);
                }
            }
        }



        private void startForegroundService() {
            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Location Tracking")
                    .setContentText("This app is accessing your location.")
                    .setSmallIcon(R.drawable.app_icon) // Replace with your icon
                    .setOngoing(true) // Makes the notification non-dismissible
                    .build();

            startForeground(NOTIFICATION_ID, notification);
        }



        private void startLocationUpdates() {
            // Check for permissions before starting location updates
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("LocationService", "Location permission not granted");
                return;
            }

            // Fetch and send the initial location immediately
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            updateLocationInFirestore(location); // Send the initial location
                        } else {
                            Log.e("LocationService", "Initial location is null");
                        }
                    })
                    .addOnFailureListener(e -> Log.e("LocationService", "Failed to fetch initial location", e));

            // Start the periodic location updates
            LocationRequest locationRequest = LocationRequest.create();
            locationRequest.setInterval(150000); // 5 minutes
            locationRequest.setFastestInterval(150000); // 5 minutes
            locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            locationCallback = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult != null) {
                        Location location = locationResult.getLastLocation();
                        if (location != null) {
                            updateLocationInFirestore(location);
                        }
                    }
                }
            };

            try {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, getMainLooper());
            } catch (SecurityException e) {
                Log.e("LocationService", "Location permission not granted", e);
            }
        }
        public void requestSingleLocationUpdate() {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("LocationService", "Location permission not granted");
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            updateLocationInFirestore(location); // Send the location immediately
                        } else {
                            Log.e("LocationService", "Location is null");
                        }
                    })
                    .addOnFailureListener(e -> Log.e("LocationService", "Failed to fetch location", e));
        }

        private void updateLocationInFirestore(Location location) {
            String uid = mAuth.getCurrentUser().getUid();
            if (uid != null) {
                long timestamp = System.currentTimeMillis();

                firestore.collection("users").document(uid)
                        .update("latitude", location.getLatitude(),
                                "longitude", location.getLongitude(),
                                "timestamp", timestamp)
                        .addOnSuccessListener(aVoid -> Log.d("LocationService", "Location updated"))
                        .addOnFailureListener(e -> Log.e("LocationService", "Failed to update location", e));
            }
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return START_STICKY;
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (locationCallback != null) {
                fusedLocationClient.removeLocationUpdates(locationCallback);
            }
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
