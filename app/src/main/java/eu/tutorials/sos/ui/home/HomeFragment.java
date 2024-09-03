package eu.tutorials.sos.ui.home;
import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
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
        sosButton.setOnClickListener(v -> sendSos());

        return root;
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

                            // Send the data to the deployed server
                            sendToServer(latitude, longitude, timestamp);
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

    private void sendToServer(double latitude, double longitude, Date timestamp) {
        new Thread(() -> {
            try {
                URL url = new URL("https://sih-backend-8bsr.onrender.com/api/data"); // Replace with your deployed server's URL
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);
                // Create JSON object with the required format
                JSONObject json = new JSONObject();
                json.put("longitude", longitude);
                json.put("latitude", latitude);
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
