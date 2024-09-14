package eu.tutorials.sos;

import android.Manifest;
import android.content.Intent;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.messaging.FirebaseMessaging;
import android.content.pm.PackageManager;
import android.os.Build;
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
import androidx.core.app.NotificationManagerCompat;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;


import eu.tutorials.sos.databinding.ActivityMainBinding;
import okhttp3.Call;
import okhttp3.Callback;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseFirestore firestore;
    private OkHttpClient httpClient;
    private TextView navUserName;
    private TextView navUserPhone;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initializeFirebaseServices();
        setupToolbarAndNavigation();
        handleWidgetIntent(getIntent());

        checkLocationPermission();
        startLocationService();
        requestNotificationPermission();

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        AppBarConfiguration mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_slideshow)
                .setOpenableLayout(binding.drawerLayout)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_home) {
                binding.appBarMain.fab.setImageResource(R.drawable.incognito_1__1_);
                binding.appBarMain.fab.setOnClickListener(view -> sendSos());
            } else if (destination.getId() == R.id.nav_slideshow) {
                binding.appBarMain.fab.setImageResource(R.drawable.add_friend_svgrepo_com);
                binding.appBarMain.fab.setOnClickListener(view -> {
                    Snackbar.make(findViewById(android.R.id.content), "Coming Soon...", Snackbar.LENGTH_SHORT).show();
                });
                }
            });
        }

    private void initializeFirebaseServices() {
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        httpClient = new OkHttpClient();
        fetchFcmToken();
        updateNavigationHeader();

    }

    private void fetchFcmToken() {
        FirebaseMessaging.getInstance().getToken().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String token = task.getResult();
                Log.i("MainActivity", "FCM Token: " + token);
                updateFcmTokenInFirestore(token);
            } else {
                Log.w("MainActivity", "Fetching FCM token failed", task.getException());
            }
        });
    }

    private void updateFcmTokenInFirestore(String token) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            firestore.collection("users").document(user.getUid())
                    .update("fcmToken", token)
                    .addOnSuccessListener(aVoid -> Log.i("MainActivity", "FCM token updated in Firestore"))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to update FCM token in Firestore", e));
        }
    }

    private void setupToolbarAndNavigation() {
        setSupportActionBar(binding.appBarMain.toolbar);
        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);
    }

    private void updateNavigationHeader() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            NavigationView navigationView = binding.navView;
            View headerView = navigationView.getHeaderView(0);
            navUserName = headerView.findViewById(R.id.textView);
            navUserPhone = headerView.findViewById(R.id.textView2);

            firestore.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            navUserName.setText(documentSnapshot.getString("name"));
                            navUserPhone.setText(documentSnapshot.getString("phone"));
                        } else {
                            Toast.makeText(MainActivity.this, "User data not found.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to fetch user data", e));
        }
    }

    private void checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void startLocationService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    updateFirestoreLocation(location.getLatitude(), location.getLongitude());
                }
            });
            Intent intent = new Intent(this, LocationService.class);
            startService(intent);
        }
    }

    private void updateFirestoreLocation(double latitude, double longitude) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            firestore.collection("users").document(user.getUid())
                    .update("location", new GeoPoint(latitude, longitude))
                    .addOnSuccessListener(aVoid -> Log.i("MainActivity", "Location updated in Firestore"))
                    .addOnFailureListener(e -> Log.e("MainActivity", "Failed to update location", e));
        }
    }

    private void sendSos() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission is required to send SOS.", Toast.LENGTH_LONG).show();
                return;
            }

            fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
                if (location != null) {
                    sendSosRequest(location.getLatitude(), location.getLongitude());
                } else {
                    Toast.makeText(this, "Unable to obtain location.", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void sendSosRequest(double latitude, double longitude) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            firestore.collection("users").document(user.getUid()).get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String name = documentSnapshot.getString("name");
                        String phone = documentSnapshot.getString("phone");
                        user.getIdToken(true).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                String idToken = task.getResult().getToken();

                                JSONObject json = new JSONObject();
                                try {
                                    json.put("longitude", longitude);
                                    json.put("latitude", latitude);
                                    json.put("name", "Anonymous");
                                    json.put("phoneNo", "XXXXXXXXXX");
                                    json.put("time", System.currentTimeMillis());
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                RequestBody requestBody = RequestBody.create(
                                        json.toString(),
                                        MediaType.parse("application/json; charset=utf-8")
                                );
                                Log.d("SOS Request", "Sending SOS with fields: " +
                                        "longitude=" + longitude + ", " +
                                        "latitude=" + latitude + ", " +
                                        "name=" + "Anonymous" + ", " +
                                        "phoneNo=" + "XXXXXXXXXX" + ", " +
                                        "time=" +  System.currentTimeMillis());

                                Request request = new Request.Builder()
                                        .url("https://sih-backend-8bsr.onrender.com/api/data")
                                        .addHeader("Authorization", "Bearer " + idToken)
                                        .post(requestBody)
                                        .build();

                                httpClient.newCall(request).enqueue(new Callback() {
                                    @Override
                                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                        Log.e("SOS Request", "Failed to send SOS request", e);
                                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send SOS. Try again.", Toast.LENGTH_LONG).show());
                                    }

                                    @Override
                                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                        if (response.isSuccessful()) {
                                            Log.d("SOS Request", "SOS sent successfully. Response code: " + response.code());
                                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "SOS sent successfully!", Toast.LENGTH_LONG).show());
                                        } else {
                                            Log.e("SOS Request", "Failed to send SOS. Response code: " + response.code() + ". Response body: " + response.body().string());
                                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send SOS. Try again.", Toast.LENGTH_LONG).show());
                                        }
                                    }
                                });
                            } else {
                                Log.e("SOS Request", "Failed to get ID token.");
                                Toast.makeText(MainActivity.this, "Failed to get ID token.", Toast.LENGTH_SHORT).show();
                            }
                        }).addOnFailureListener(e -> {
                            Log.e("SOS Request", "Failed to get ID token.", e);
                            Toast.makeText(MainActivity.this, "Failed to get ID token.", Toast.LENGTH_SHORT).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e("SOS Request", "User data not found.", e);
                        Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Log.e("SOS Request", "No authenticated user found.");
        }
    }


    private void requestNotificationPermission() {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, NOTIFICATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("fromWidget", false)) {
            sendSos();
        }
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

    private void logout() {
        mAuth.signOut();
        startActivity(new Intent(this, Login.class));
        finish();
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, mAppBarConfiguration) || super.onSupportNavigateUp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Location permission denied. Some features may not work.", Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("MainActivity", "Notification permission granted");
            } else {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_LONG).show();
            }
        }
    }
}