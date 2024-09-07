package eu.tutorials.sos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import com.google.firebase.messaging.FirebaseMessaging;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;

import eu.tutorials.sos.databinding.ActivityMainBinding;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private OkHttpClient httpClient;
    TextView navUserName;
    TextView navUserPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        httpClient = new OkHttpClient();
        fetchFcmToken();
        // Start the LocationService immediately when the app is opened
        startLocationService();

        checkLocationPermission();
        setupToolbarAndNavigation();
        binding.appBarMain.fab.setOnClickListener(view -> sendSos());
        handleWidgetIntent(getIntent());

        // Fetch user data from Firestore and update the navigation header
        updateNavigationHeader();
    }

    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        Log.w("MainActivity", "Fetching FCM token failed", task.getException());
                        return;
                    }

                    // Get the FCM token
                    String token = task.getResult();
                    Log.i("MainActivity", "FCM Token: " + token);

                    // Optionally, send this token to your server or store it in Firestore
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        updateFcmTokenInFirestore(user.getUid(), token);
                    }
                });
    }

    private void updateFcmTokenInFirestore(String uid, String token) {
        firestore.collection("users").document(uid)
                .update("fcmToken", token)
                .addOnSuccessListener(aVoid -> Log.i("MainActivity", "FCM token updated in Firestore"))
                .addOnFailureListener(e -> Log.e("MainActivity", "Failed to update FCM token in Firestore", e));
    }

    private void startLocationService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Send the location immediately upon opening the MainActivity
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                double latitude = location.getLatitude();
                                double longitude = location.getLongitude();
                                Log.i("MainActivity", "Location obtained: Lat=" + latitude + " Long=" + longitude);

                                // Update Firestore with the current location and timestamp
                                updateFirestoreLocation(user.getUid(), latitude, longitude, new Date());
                            } else {
                                Log.e("MainActivity", "User is not authenticated");
                                Toast.makeText(this, "User is not authenticated. Please log in.", Toast.LENGTH_LONG).show();
                            }
                        }
                    })
                    .addOnFailureListener(e -> Log.e("MainActivity", "Error getting location", e));

            // Start the LocationService
            Intent intent = new Intent(this, LocationService.class);
            startService(intent);
        } else {
            Toast.makeText(this, "Location permission is required to start location service.", Toast.LENGTH_LONG).show();
        }
    }

    private void updateNavigationHeader() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            // Get the navigation view and header view
            NavigationView navigationView = binding.navView;
            View headerView = navigationView.getHeaderView(0);
            navUserName = headerView.findViewById(R.id.textView);
            navUserPhone = headerView.findViewById(R.id.textView2);

            // Fetch user data from Firestore
            firestore.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String name = documentSnapshot.getString("name");
                            String phone = documentSnapshot.getString("phone");

                            // Update the TextViews with fetched data
                            navUserName.setText(name != null ? name : "User Name");
                            navUserPhone.setText(phone != null ? phone : "Phone Number");
                        } else {
                            Toast.makeText(MainActivity.this, "User data not found in Firestore.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Failed to fetch user data", e);
                        Toast.makeText(MainActivity.this, "Failed to fetch user data. Please try again.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e("MainActivity", "User is not authenticated");
            Toast.makeText(MainActivity.this, "User is not authenticated. Please log in.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupToolbarAndNavigation() {
        setSupportActionBar(binding.appBarMain.toolbar);

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_settings) {
            logout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration)
                || super.onSupportNavigateUp();
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied. Some features may not work.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void sendSos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission is required to send SOS.", Toast.LENGTH_LONG).show();
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(this, location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Log.i("MainActivity", "Location obtained: Lat=" + latitude + " Long=" + longitude);

                            // Fetch user data from Firestore
                            firestore.collection("users").document(user.getUid()).get()
                                    .addOnSuccessListener(documentSnapshot -> {
                                        if (documentSnapshot.exists()) {
                                            String name = documentSnapshot.getString("name");
                                            String phone = documentSnapshot.getString("phone");
                                            sendToServer(name != null ? name : "Anonymous", phone != null ? phone : "XXXXXXXXXX", latitude, longitude, new Date());
                                        } else {
                                            Toast.makeText(MainActivity.this, "User data not found in Firestore.", Toast.LENGTH_SHORT).show();
                                        }
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e("MainActivity", "Failed to fetch user data", e);
                                        Toast.makeText(MainActivity.this, "Failed to fetch user data. Please try again.", Toast.LENGTH_SHORT).show();
                                    });
                        } else {
                            Toast.makeText(this, "Unable to obtain location. Try again.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Error getting location", e);
                        Toast.makeText(this, "Error getting location. Try again.", Toast.LENGTH_LONG).show();
                    });
        } else {
            Log.e("MainActivity", "User is not authenticated");
            Toast.makeText(this, "User is not authenticated. Please log in.", Toast.LENGTH_LONG).show();
        }
    }

    private void sendToServer(String name, String phone, double latitude, double longitude, Date timestamp) {
        String url = "https://your-server-url/endpoint";
        RequestBody formBody = new FormBody.Builder()
                .add("name", name)
                .add("phone", phone)
                .add("latitude", String.valueOf(latitude))
                .add("longitude", String.valueOf(longitude))
                .add("timestamp", timestamp.toString())
                .build();

        Request request = new Request.Builder()
                .url(url)
                .post(formBody)
                .build();

        httpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("MainActivity", "Error sending SOS request", e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send SOS. Try again.", Toast.LENGTH_LONG).show());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    Log.i("MainActivity", "SOS request sent successfully");
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "SOS sent successfully!", Toast.LENGTH_LONG).show());
                } else {
                    Log.e("MainActivity", "Failed to send SOS request. Server responded with code: " + response.code());
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send SOS. Try again.", Toast.LENGTH_LONG).show());
                }
            }
        });
    }

    private void updateFirestoreLocation(String uid, double latitude, double longitude, Date timestamp) {
        firestore.collection("users").document(uid)
                .update("latitude", latitude, "longitude", longitude, "timestamp", timestamp)
                .addOnSuccessListener(aVoid -> Log.i("MainActivity", "Location updated in Firestore"))
                .addOnFailureListener(e -> Log.e("MainActivity", "Failed to update location in Firestore", e));
    }

    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent = new Intent(MainActivity.this, Login.class);
        startActivity(intent);
        finish();
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("fromWidget", false)) {
            sendSos();
        }
    }
}
