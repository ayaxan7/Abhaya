package eu.tutorials.sos.ui.home;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

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
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            Log.i("SOS", "Location obtained: Lat=" + latitude + " Long=" + longitude);

                            // Retrieve name and phone from SharedPreferences
                            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                            String name = sharedPreferences.getString("name", "");
                            String phone = sharedPreferences.getString("phone", "");

                            // Send the data to the deployed server
                            sendToServer(name, phone, latitude, longitude, timestamp);
                        } else {
                            Toast.makeText(getContext(), "Unable to obtain location. Try again.", Toast.LENGTH_LONG).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e("SOS", "Error getting location", e);
                        Toast.makeText(getContext(), "Error getting location. Try again.", Toast.LENGTH_LONG).show();
                    });
        } else {
            Log.e("SOS", "User is not authenticated");
            Toast.makeText(getContext(), "User is not authenticated. Please log in.", Toast.LENGTH_LONG).show();
        }
    }

    private void sendToServer(String name, String phone, double latitude, double longitude, Date timestamp) {
        new Thread(() -> {
            try {
                URL url = new URL("https://sih-backend-8bsr.onrender.com/api/data"); // Replace with your deployed server's URL
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                // Create JSON object with the required format
                JSONObject json = new JSONObject();
                json.put("latitude", latitude);
                json.put("longitude", longitude);
                json.put("name", name);
                json.put("phoneNo", phone);
                json.put("time", timestamp.toString());
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(json.toString().getBytes("UTF-8"));
                    os.flush();
                }
                int responseCode = conn.getResponseCode();
                Log.i("SOS", "Server Response Code: " + responseCode);
                if (responseCode == 200) {
                    Log.i("SOS", "SOS sent successfully");
                } else {
                    Log.e("SOS", "Failed to send SOS. Response Code: " + responseCode);
                }

                // Log the JSON data that was sent
                Log.i("SOS", "Data sent: " + json.toString());

                conn.disconnect();

            } catch (Exception e) {
                Log.e("SOS", "Error sending SOS", e);
            }
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                sendSos();
            } else {
                Toast.makeText(getContext(), "Location permission denied. Cannot send SOS.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
