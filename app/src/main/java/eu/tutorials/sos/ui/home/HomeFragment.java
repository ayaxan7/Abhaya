package eu.tutorials.sos.ui.home;
import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
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
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.util.Date;

import eu.tutorials.sos.databinding.FragmentHomeBinding;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private FusedLocationProviderClient fusedLocationClient;
    private final OkHttpClient client = new OkHttpClient();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(getActivity());

        Button sosButton = binding.sosButton;

        sosButton.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    animateShrink(v);
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    animateBounceBack(v);
                    sendSos();
                    break;
            }
            return true;
        });

        return root;
    }

    private void animateShrink(View view) {
        view.animate().scaleX(0.8f).scaleY(0.8f).setDuration(150).start();
    }

    private void animateBounceBack(View view) {
        view.animate().scaleX(1.2f).scaleY(1.2f).setDuration(300)
                .withEndAction(() -> view.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()).start();
    }
    private void sendSos() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
                return;
            }

            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(getActivity(), location -> {
                        if (location != null) {
                            double latitude = location.getLatitude();
                            double longitude = location.getLongitude();
                            long timestamp = System.currentTimeMillis(); // Use current time in millis

                            Log.i("SOS", "Location obtained: Lat=" + latitude + " Long=" + longitude);

                            SharedPreferences sharedPreferences = getActivity().getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
                            String name = sharedPreferences.getString("name", "");
                            String phone = sharedPreferences.getString("phone", "");

                            user.getIdToken(true)
                                    .addOnCompleteListener(task -> {
                                        if (task.isSuccessful()) {
                                            String idToken = task.getResult().getToken();
                                            new SendSosTask(idToken, name, phone, latitude, longitude, timestamp).execute();

                                            // Update Firestore with the latest location and timestamp
                                            FirebaseFirestore firestore = FirebaseFirestore.getInstance();
                                            String uid = user.getUid();

                                            firestore.collection("users").document(uid)
                                                    .update("latitude", latitude,
                                                            "longitude", longitude,
                                                            "timestamp", timestamp)
                                                    .addOnSuccessListener(aVoid -> Log.d("SOS", "Location updated in Firestore"))
                                                    .addOnFailureListener(e -> Log.e("SOS", "Failed to update location in Firestore", e));

                                        } else {
                                            Log.e("SOS", "Failed to get ID token", task.getException());
                                            Toast.makeText(getContext(), "Failed to authenticate. Please try again.", Toast.LENGTH_LONG).show();
                                        }
                                    });
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



    private class SendSosTask extends AsyncTask<Void, Void, Boolean> {
        private final String idToken;
        private final String name;
        private final String phone;
        private final double latitude;
        private final double longitude;
        private final long timestamp; // Use long for timestamp

        public SendSosTask(String idToken, String name, String phone, double latitude, double longitude, long timestamp) {
            this.idToken = idToken;
            Log.d("SOS", "ID Token: " + idToken);
            this.name = name;
            this.phone = phone;
            this.latitude = latitude;
            this.longitude = longitude;
            this.timestamp = timestamp; // Use long
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                JSONObject json = new JSONObject();
                json.put("latitude", latitude);
                json.put("longitude", longitude);
                json.put("name", name);
                json.put("phoneNo", phone);
                json.put("time", timestamp); // Use long timestamp

                MediaType JSON = MediaType.get("application/json; charset=utf-8");
                RequestBody body = RequestBody.create(json.toString(), JSON);

                Request request = new Request.Builder()
                        .url("https://sih-backend-8bsr.onrender.com/api/data")
                        .addHeader("Authorization", "Bearer " + idToken)
                        .post(body)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) {
                        Log.i("SOS", "SOS sent successfully");
                        return true;
                    } else {
                        Log.e("SOS", "Failed to send SOS. Response Code: " + response.code());
                        return false;
                    }
                }
            } catch (Exception e) {
                Log.e("SOS", "Error sending SOS", e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Toast.makeText(getContext(), "SOS sent successfully", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getContext(), "Failed to send SOS", Toast.LENGTH_LONG).show();
            }
        }
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
