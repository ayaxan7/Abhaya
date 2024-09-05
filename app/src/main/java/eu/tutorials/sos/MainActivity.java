package eu.tutorials.sos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
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

        checkLocationPermission();
        setupToolbarAndNavigation();
        binding.appBarMain.fab.setOnClickListener(view -> sendSos());
        handleWidgetIntent(getIntent());

        // Fetch user data from Firestore and update the navigation header
        updateNavigationHeader();
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
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Log.e("MainActivity", "User is not authenticated");
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "User is not authenticated.", Toast.LENGTH_LONG).show());
            return;
        }

        // Retrieve ID token for authentication
        user.getIdToken(true).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String idToken = task.getResult().getToken();

                // Create JSON object for the request body
                JSONObject json = new JSONObject();
                try {
                    json.put("latitude", latitude);
                    json.put("longitude", longitude);
                    json.put("name", name);
                    json.put("phoneNo", phone);
                    json.put("time", timestamp.toString());
                } catch (Exception e) {
                    Log.e("MainActivity", "Failed to create JSON object", e);
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to create JSON object.", Toast.LENGTH_LONG).show());
                    return;
                }

                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                RequestBody requestBody = RequestBody.create(json.toString(), JSON);

                Request request = new Request.Builder()
                        .url("https://sih-backend-8bsr.onrender.com/api/data") // Replace with your endpoint URL
                        .addHeader("Authorization", "Bearer " + idToken)
                        .post(requestBody)
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NonNull Call call, @NonNull IOException e) {
                        Log.e("MainActivity", "Error sending SOS", e);
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send SOS. Please try again.", Toast.LENGTH_LONG).show());
                    }

                    @Override
                    public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                        if (response.isSuccessful()) {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "SOS sent successfully!", Toast.LENGTH_LONG).show());
                        } else {
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send SOS. Response: " + response.message(), Toast.LENGTH_LONG).show());
                        }
                    }
                });
            } else {
                Log.e("MainActivity", "Failed to get ID token", task.getException());
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to get ID token. Please log in again.", Toast.LENGTH_LONG).show());
            }
        });
    }


    private void handleWidgetIntent(Intent intent) {
        // Implement logic to handle intents from widgets here if needed
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, Login.class);
        finish();
        startActivity(intent);
        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show();
        // Redirect to login activity or handle logout action
    }
}
