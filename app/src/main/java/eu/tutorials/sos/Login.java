package eu.tutorials.sos;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class Login extends AppCompatActivity {
    TextInputEditText email, password;
    Button btn_login;
    FirebaseAuth mAuth;
    FirebaseFirestore db;
    ProgressBar bar;
    TextView register;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToMainActivity();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        email = findViewById(R.id.email);
        password = findViewById(R.id.password);
        btn_login = findViewById(R.id.btn_login);
        bar = findViewById(R.id.bar);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        register = findViewById(R.id.register);

        register.setOnClickListener(v -> {
            Intent intent = new Intent(Login.this, Register.class);
            startActivity(intent);
        });

        btn_login.setOnClickListener(v -> {
            String email1, password1;
            email1 = String.valueOf(email.getText()).trim();
            password1 = String.valueOf(password.getText()).trim();

            if (email1.isEmpty() || password1.isEmpty()) {
                Toast.makeText(Login.this, "Enter all fields", Toast.LENGTH_SHORT).show();
                bar.setVisibility(View.GONE);
                return;
            }

            bar.setVisibility(View.VISIBLE);
            mAuth.signInWithEmailAndPassword(email1, password1)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if (task.isSuccessful()) {
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    Log.d("Firestore", "User ID: " + user.getUid());
                                    fetchUserData(user.getUid());
                                }
                            } else {
                                Toast.makeText(Login.this, "Authentication failed.",
                                        Toast.LENGTH_SHORT).show();
                                bar.setVisibility(View.GONE);
                            }
                        }
                    });
        });
    }
    private void fetchUserData(String uid) {
        db.collection("users").document(uid).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()) {
                            DocumentSnapshot document = task.getResult();
                            if (document.exists()) {
                                // Fetching data
                                String name = document.getString("name");
                                String phone = document.getString("phone");

                                // Logging to check if the data is correctly fetched
                                Log.d("Firestore", "Name: " + name + ", Phone: " + phone);

                                if (name == null || phone == null) {
                                    Toast.makeText(Login.this, "User data is incomplete.", Toast.LENGTH_SHORT).show();
                                } else {

                                    navigateToMainActivity();
                                }
                            } else {
                                // No document exists for this UID
                                Toast.makeText(Login.this, "No user data found.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            // Failed to fetch document
                            Toast.makeText(Login.this, "Failed to fetch user data.", Toast.LENGTH_SHORT).show();
                            Log.e("Firestore", "Error fetching user data: ", task.getException());
                        }
                        bar.setVisibility(View.GONE);
                    }
                });
    }





    private void navigateToMainActivity() {
        Intent intent = new Intent(Login.this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
