package eu.tutorials.sos;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Spinner;  // Import Spinner
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class Register extends AppCompatActivity {
    EditText name, email, password, phone, gender;
    Button btn_register;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ProgressBar bar;
    TextView login;
    String gender1;
    @Override
    public void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Intent intent = new Intent(Register.this, MainActivity.class);
            Log.d("Registration Successful", "User already logged in");
            startActivity(intent);
            finish();
            return;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        name = findViewById(R.id.name);
        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        phone = findViewById(R.id.phone);
        btn_register = findViewById(R.id.btn_register);
        bar = findViewById(R.id.bar);
        gender1="";
        login = findViewById(R.id.login);
        RadioButton radioMale = findViewById(R.id.radioMale);
        RadioButton radioFemale = findViewById(R.id.radioFemale);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        login.setOnClickListener(v -> {
            Intent intent = new Intent(Register.this, Login.class);
            startActivity(intent);
            finish();
        });
        btn_register.setOnClickListener(v -> {
            bar.setVisibility(View.VISIBLE);
            String name1 = name.getText().toString().trim();
            String email1 = email.getText().toString().trim();
            String password1 = password.getText().toString().trim();
            String phone1 = phone.getText().toString().trim();
            if (!radioMale.isChecked() && !radioFemale.isChecked()) {
                Toast.makeText(Register.this, "Please select your gender", Toast.LENGTH_SHORT).show();
                bar.setVisibility(View.GONE);
                btn_register.setVisibility(View.VISIBLE);
                return;
            }
            if (radioMale.isChecked()) {
                gender1 = "Male";
            } else if (radioFemale.isChecked()) {
                gender1 = "Female";
            }

            // Validate other fields
            if (name1.isEmpty() || email1.isEmpty() || password1.isEmpty() || phone1.isEmpty() || gender1.isEmpty()) {
                Toast.makeText(Register.this, "Enter all fields", Toast.LENGTH_SHORT).show();
                bar.setVisibility(View.GONE);
                btn_register.setVisibility(View.VISIBLE);
                return;
            }

            if (password1.length() < 6) {
                Toast.makeText(Register.this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
                bar.setVisibility(View.GONE);
                btn_register.setVisibility(View.VISIBLE);
                return;
            }


            mAuth.createUserWithEmailAndPassword(email1, password1)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String userId = user.getUid();
                                Map<String, Object> userData = new HashMap<>();
                                userData.put("name", name1);
                                userData.put("phone", phone1);
                                userData.put("email", email1);
                                userData.put("gender", gender1);
                                db.collection("users").document(userId)
                                        .set(userData)
                                        .addOnSuccessListener(aVoid -> {
                                            Log.d("Firestore", "User data successfully written!");
                                            Toast.makeText(Register.this, "Registration successful!", Toast.LENGTH_SHORT).show();
                                            SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                            SharedPreferences.Editor editor = sharedPreferences.edit();
                                            editor.putString("name", name1);
                                            editor.putString("phone", phone1);
                                            editor.apply();
                                            Intent intent = new Intent(Register.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w("Firestore", "Error writing user data", e);
                                            Toast.makeText(Register.this, "Failed to save user data. Please try again.", Toast.LENGTH_SHORT).show();
                                            bar.setVisibility(View.GONE);
                                            btn_register.setVisibility(View.VISIBLE);
                                        });
                            }
                        } else {
                            Toast.makeText(Register.this, "Authentication failed. " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            bar.setVisibility(View.GONE);
                            btn_register.setVisibility(View.VISIBLE);
                        }
                    });
        });

    }
}

