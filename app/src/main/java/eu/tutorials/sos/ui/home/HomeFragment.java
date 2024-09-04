package eu.tutorials.sos.ui.home;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONObject;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import eu.tutorials.sos.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private HandlerThread handlerThread;
    private Handler backgroundHandler;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // Initialize the HandlerThread for background operations
        handlerThread = new HandlerThread("SOSBackgroundThread");
        handlerThread.start();
        backgroundHandler = new Handler(handlerThread.getLooper());

        Button sosButton = binding.sosButton;

        // Add touch listener for the shrinking and bouncing effect
        sosButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // Shrink the button when pressed
                        animateShrink(v);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // Bounce back to original size when released
                        animateBounceBack(v);
                        sendSos();
                        break;
                }
                return true;
            }
        });

        return root;
    }

    private void animateShrink(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.8f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.8f);
        scaleX.setDuration(150);
        scaleY.setDuration(150);

        AnimatorSet shrinkSet = new AnimatorSet();
        shrinkSet.playTogether(scaleX, scaleY);
        shrinkSet.start();
    }

    private void animateBounceBack(View view) {
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.2f, 1.0f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.2f, 1.0f);
        scaleX.setDuration(300);
        scaleY.setDuration(300);

        AnimatorSet bounceBackSet = new AnimatorSet();
        bounceBackSet.playTogether(scaleX, scaleY);
        bounceBackSet.start();
    }

    private void sendSos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Date timestamp = new Date();

            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Log.i("SOS", "Location obtained: Lat=" + latitude + " Long=" + longitude);

                            // Retrieve name and phone from SharedPreferences
                            SharedPreferences sharedPreferences = requireActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                            String name = sharedPreferences.getString("name", "");
                            String phone = sharedPreferences.getString("phone", "");

                            // Send the data to the deployed server
                            backgroundHandler.post(() -> sendToServer(name, phone, latitude, longitude, timestamp));
                        } else {
                            showToast("Unable to obtain location. Try again.");
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("SOS", "Error getting location", e);
                        showToast("Error getting location. Try again.");
                    });
        } else {
            Log.e("SOS", "User is not authenticated");
            showToast("User is not authenticated. Please log in.");
        }
    }

    private void sendToServer(String name, String phone, double latitude, double longitude, Date timestamp) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            user.getIdToken(true).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    String idToken = task.getResult().getToken();
                    try {
                        HttpURLConnection conn = setupHttpURLConnection(idToken);

                        // Create JSON object with the required format
                        JSONObject json = new JSONObject();
                        json.put("latitude", latitude);
                        json.put("longitude", longitude);
                        json.put("name", name);
                        json.put("phoneNo", phone);
                        json.put("time", timestamp.toString());

                        // Write JSON data to output stream
                        try (OutputStream os = conn.getOutputStream()) {
                            os.write(json.toString().getBytes("UTF-8"));
                            os.flush();
                        }

                        int responseCode = conn.getResponseCode();
                        if (responseCode == 200) {
                            Log.i("SOS", "SOS sent successfully");
                        } else {
                            Log.e("SOS", "Failed to send SOS. Response Code: " + responseCode);
                        }

                        conn.disconnect();
                    } catch (Exception e) {
                        Log.e("SOS", "Error sending SOS", e);
                    }
                } else {
                    Log.e("SOS", "Error getting authentication token", task.getException());
                    showToast("Error getting authentication token. Try again.");
                }
            });
        } else {
            Log.e("SOS", "User is not authenticated");
            showToast("User is not authenticated. Please log in.");
        }
    }

    private HttpURLConnection setupHttpURLConnection(String idToken) throws Exception {
        URL url = new URL("https://sih-backend-8bsr.onrender.com/api/data");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setRequestProperty("Authorization", "Bearer " + idToken);
        conn.setDoOutput(true);
        return conn;
    }

    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handlerThread.quitSafely();
    }
}
