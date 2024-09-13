package eu.tutorials.sos;
import android.Manifest;

import android.content.Intent;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.messaging.FirebaseMessaging;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
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
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import eu.tutorials.sos.databinding.ActivityMainBinding;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_CONTACTS_PERMISSION = 1;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 2;
    private static final int PICK_CONTACT_REQUEST = 2;
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
                R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow)
                .setOpenableLayout(binding.drawerLayout)
                .build();
        NavigationUI.setupActionBarWithNavController(this, navController, mAppBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            if (destination.getId() == R.id.nav_home) {
                binding.appBarMain.fab.setOnClickListener(view -> sendSos());
            } else if (destination.getId() == R.id.nav_slideshow) {
                binding.appBarMain.fab.setOnClickListener(view -> {
                    checkContactsPermission();
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
    private void checkContactsPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CONTACTS}, REQUEST_CONTACTS_PERMISSION);
        } else {
            openContactsPicker();
        }
    }
    private void openContactsPicker() {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, PICK_CONTACT_REQUEST);
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

                        RequestBody formBody = new FormBody.Builder()
                                .add("name", name)
                                .add("phone", phone)
                                .add("latitude", String.valueOf(latitude))
                                .add("longitude", String.valueOf(longitude))
                                .add("timestamp", new Date().toString())
                                .build();
                        Request request = new Request.Builder()
                                .url("https://sih-backend-8bsr.onrender.com/api/data")
                                .post(formBody)
                                .build();

                        httpClient.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send SOS. Try again.", Toast.LENGTH_LONG).show());
                            }

                            @Override
                            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                                if (response.isSuccessful()) {
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "SOS sent successfully!", Toast.LENGTH_LONG).show());
                                } else {
                                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Failed to send SOS. Try again.", Toast.LENGTH_LONG).show());
                                }
                            }
                        });
                    })
                    .addOnFailureListener(e -> Toast.makeText(this, "User data not found.", Toast.LENGTH_SHORT).show());
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
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK) {
            Uri contactUri = data.getData();
            if (contactUri != null) {
                retrieveContactInfo(contactUri);
            }
        }
    }

    private void retrieveContactInfo(Uri contactUri) {
        String[] projection = {ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.HAS_PHONE_NUMBER};

        Cursor cursor = getContentResolver().query(contactUri, projection, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String contactId = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID));
            String contactName = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME));
            int hasPhoneNumber = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER));
            if (hasPhoneNumber > 0) {
                Cursor phoneCursor = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER},
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null);
                if (phoneCursor != null && phoneCursor.moveToFirst()) {
                    String contactPhone = phoneCursor.getString(phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    contactPhone = contactPhone.replaceAll("\\s+", "");
                    if(contactPhone.startsWith("+91")){
                        contactPhone=contactPhone.substring(3);
                    }
                    Log.d("ContactPhoneAfter", "Modified phone number: " + contactPhone);
                    phoneCursor.close();
                    FirebaseFirestore db = FirebaseFirestore.getInstance();
                    FirebaseAuth mAuth = FirebaseAuth.getInstance();
                    FirebaseUser currentUser = mAuth.getCurrentUser();
                    Map<String, Object> friend = new HashMap<>();
                    friend.put("name", contactName);
                    friend.put("Phone", contactPhone);
                    friend.put("UID",currentUser.getUid().toString());
                    String finalContactPhone = contactPhone;
                    db.collection("friends")
                            .add(friend)
                            .addOnSuccessListener(documentReference -> {
                                Toast.makeText(MainActivity.this, "Contact added successfully!", Toast.LENGTH_SHORT).show();
                                db.collection("users")
                                        .whereEqualTo("phone", finalContactPhone)
                                        .get()
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                if (!task.getResult().isEmpty()) {
                                                    // Phone number found in 'users' collection
                                                    Log.d("Firestore", "Phone number found in users collection after upload.");
                                                    Toast.makeText(MainActivity.this, "Phone number found in users collection.", Toast.LENGTH_SHORT).show();
                                                    DocumentSnapshot userDoc = task.getResult().getDocuments().get(0);
                                                    String fcmToken = userDoc.getString("fcmToken");
                                                    if (fcmToken != null) {
                                                        DocumentReference friendDocRef = db.collection("friends").document(documentReference.getId());
                                                        friendDocRef.update("fcmToken", fcmToken)
                                                                .addOnSuccessListener(aVoid -> {
                                                                    Log.d("Firestore", "FCMTokens successfully updated in friends document.");

                                                                })
                                                                .addOnFailureListener(e -> {
                                                                    Log.w("Firestore", "Error updating FCMTokens", e);

                                                                });
                                                    }
                                                } else {
                                                    Log.d("Firestore", "Phone number not found in users collection.");

                                                }
                                            } else {
                                                Log.w("Firestore", "Error querying users collection", task.getException());
                                                Toast.makeText(MainActivity.this, "Error querying users collection.", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(MainActivity.this, "Error adding contact: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }
            }
            cursor.close();
        }
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
        } if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i("MainActivity", "Notification permission granted");
            } else {
                Toast.makeText(this, "Notification permission denied.", Toast.LENGTH_LONG).show();
            }
        }
        if (requestCode == REQUEST_CONTACTS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openContactsPicker();
            } else {
                Toast.makeText(this, "Permission denied to read contacts", Toast.LENGTH_SHORT).show();
            }
        }
    }
}