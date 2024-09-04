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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import eu.tutorials.sos.databinding.ActivityMainBinding;
public class MainActivity extends AppCompatActivity {
    private AppBarConfiguration mAppBarConfiguration;
    private ActivityMainBinding binding;
    private FirebaseAuth mAuth;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize Firebase Auth and FusedLocationProviderClient
        mAuth = FirebaseAuth.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Request location permission on startup
        checkLocationPermission();

        setSupportActionBar(binding.appBarMain.toolbar);
        binding.appBarMain.fab.setOnClickListener(view -> sendSos());

        DrawerLayout drawer = binding.drawerLayout;
        NavigationView navigationView = binding.navView;
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(drawer)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(navigationView, navController);

        // Handle intent from the SOS widget
        handleWidgetIntent(getIntent());
        // Access the NavigationView


        // Inflate the header view
        View headerView = navigationView.getHeaderView(0);

        // Find the TextViews in the header
        TextView navUserName = headerView.findViewById(R.id.textView);
        TextView navUserPhone = headerView.findViewById(R.id.textView2);

        // Access SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userName = sharedPreferences.getString("name", "User Name");
        String userPhone = sharedPreferences.getString("phone", "Phone Number");

        // Set the TextViews with user data
        navUserName.setText(userName);
        navUserPhone.setText(userPhone);
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
            Date timestamp = new Date();

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
                            // Retrieve name and phone from SharedPreferences
                            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                            String name = sharedPreferences.getString("name", "Unknown");
                            String phone = sharedPreferences.getString("phone", "9999999999");

                            // Send the data to the deployed server
                            sendToServer("Anonyomous", "XXXXXXXXXX", latitude, longitude, timestamp);
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
            try {
                URL url = new URL("https://sih-backend-8bsr.onrender.com/api/data");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                JSONObject json = new JSONObject();
                json.put("latitude", latitude);
                json.put("longitude", longitude);
                json.put("name", name); // Use the retrieved name
                json.put("phoneNo", phone); // Use the retrieved phone
                json.put("time", timestamp.toString());

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes("UTF-8"));
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                Log.i("MainActivity", "Server Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    Log.i("MainActivity", "SOS sent successfully");
                } else {
                    Log.e("MainActivity", "Failed to send SOS. Response Code: " + responseCode);
                }

                Log.i("MainActivity", "Data sent: " + json.toString());
                conn.disconnect();

            } catch (Exception e) {
                Log.e("MainActivity", "Error sending SOS", e);
            }
        }).start();
    }

    private void handleWidgetIntent(Intent intent) {
        if (intent != null && "SEND_SOS".equals(intent.getAction())) {
            sendSos(); // Trigger the SOS action when the intent is received
        }
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(MainActivity.this, Login.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
