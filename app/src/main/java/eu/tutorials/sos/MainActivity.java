package eu.tutorials.sos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Date;

import eu.tutorials.sos.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private StringRequest PostRequest;
    private RequestQueue GetRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        checkLocationPermission();
        setupToolbarAndNavigation();
        binding.appBarMain.fab.setOnClickListener(view -> sendSos());
        handleWidgetIntent(getIntent());
        setupNavigationHeader();
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

                            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                            String name = sharedPreferences.getString("name", "Anonymous");
                            String phone = sharedPreferences.getString("phone", "XXXXXXXXXX");

                            sendToServer(name, phone, latitude, longitude, new Date());
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
        new Thread(() -> {
            final HttpURLConnection[] connHolder = new HttpURLConnection[1]; // Wrapper to hold the connection

            try {
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
                        Log.i("MainActivity", "ID Token: " + idToken); // Log the ID Token

                        try {
                            // Define the server URL
                            URL url = new URL("https://sih-backend-8bsr.onrender.com/api/data");
                            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                            connHolder[0] = conn; // Update the holder with the connection
                            conn.setRequestMethod("POST");
                            conn.setRequestProperty("Content-Type", "application/json; utf-8");
                            conn.setRequestProperty("Authorization", "Bearer " + idToken);
                            conn.setDoOutput(true);

                            // Create the JSON payload
                            JSONObject json = new JSONObject();
                            json.put("latitude", latitude);
                            json.put("longitude", longitude);
                            json.put("name", name);
                            json.put("phoneNo", phone);
                            json.put("time", timestamp.toString());
                            Log.i("MainActivity", "JSON Payload: " + json.toString()); // Log the JSON payload

                            try (OutputStream os = conn.getOutputStream()) {
                                try {
                                    os.write(json.toString().getBytes("UTF-8"));
                                    os.flush();
                                } catch (UnsupportedEncodingException e) {
                                    Log.e("MainActivity", "Unsupported Encoding: " + e.getMessage(), e);
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Unsupported encoding error. Please try again.", Toast.LENGTH_LONG).show());
                                    return;
                                }
                            }

                            int responseCode = conn.getResponseCode();
                            Log.i("MainActivity", "Server Response Code: " + responseCode);

                            if (responseCode == HttpURLConnection.HTTP_OK) {
                                Log.i("MainActivity", "SOS sent successfully");
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "SOS sent successfully!", Toast.LENGTH_LONG).show());
                            } else {
                                Log.e("MainActivity", "Failed to send SOS. Response Code: " + responseCode);
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send SOS. Please try again.", Toast.LENGTH_LONG).show());
                            }
                        } catch (MalformedURLException e) {
                            Log.e("MainActivity", "Malformed URL: " + e.getMessage(), e);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Invalid URL format. Please try again.", Toast.LENGTH_LONG).show());
                        } catch (ProtocolException e) {
                            Log.e("MainActivity", "Protocol Error: " + e.getMessage(), e);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Protocol error. Please try again.", Toast.LENGTH_LONG).show());
                        } catch (IOException e) {
                            Log.e("MainActivity", "I/O Error: " + e.getMessage(), e);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Network error. Please check your connection and try again.", Toast.LENGTH_LONG).show());
                        } catch (JSONException e) {
                            Log.e("MainActivity", "JSON Error: " + e.getMessage(), e);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Error creating JSON payload. Please try again.", Toast.LENGTH_LONG).show());
                        } catch (Exception e) {
                            Log.e("MainActivity", "Unexpected Error: " + e.getMessage(), e);
                            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Unexpected error occurred. Please try again.", Toast.LENGTH_LONG).show());
                        } finally {
                            if (connHolder[0] != null) {
                                connHolder[0].disconnect();
                            }
                        }
                    } else {
                        Log.e("MainActivity", "Failed to get ID token", task.getException());
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to authenticate. Please log in again.", Toast.LENGTH_LONG).show());
                    }
                });
            } catch (Exception e) {
                Log.e("MainActivity", "Unexpected Error: " + e.getMessage(), e);
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Unexpected error occurred. Please try again.", Toast.LENGTH_LONG).show());
            }
        }).start();
    }







    private void handleWidgetIntent(Intent intent) {
        if (intent != null && "SEND_SOS".equals(intent.getAction())) {
            sendSos();
        }
    }

    private void setupNavigationHeader() {
        NavigationView navigationView = binding.navView;
        View headerView = navigationView.getHeaderView(0);

        TextView navUserName = headerView.findViewById(R.id.textView);
        TextView navUserPhone = headerView.findViewById(R.id.textView2);

        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userName = sharedPreferences.getString("name", "User Name");
        String userPhone = sharedPreferences.getString("phone", "Phone Number");

        navUserName.setText(userName);
        navUserPhone.setText(userPhone);
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(MainActivity.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
