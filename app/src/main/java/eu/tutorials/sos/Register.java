package eu.tutorials.sos;
import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class Register extends AppCompatActivity {
    TextInputEditText name, email, password, phone;
    Button btn_register;
    FirebaseAuth mAuth;
    ProgressBar bar;
    TextView login;

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
        mAuth = FirebaseAuth.getInstance();
        bar = findViewById(R.id.bar);
        login = findViewById(R.id.login);

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

            if (name1.isEmpty() || email1.isEmpty() || password1.isEmpty() || phone1.isEmpty()) {
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
                        bar.setVisibility(View.GONE);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                // Save user data locally
                                SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                SharedPreferences.Editor editor = sharedPreferences.edit();
                                editor.putString("name", name1);
                                editor.putString("phone", phone1);
                                editor.apply();
                                Toast.makeText(Register.this, "Registration Successful.", Toast.LENGTH_SHORT).show();

                                // Sign in and get the token
                                mAuth.signInWithEmailAndPassword(email1, password1)
                                        .addOnCompleteListener(signInTask -> {
                                            if (signInTask.isSuccessful()) {
                                                FirebaseUser signedInUser = FirebaseAuth.getInstance().getCurrentUser();
                                                if (signedInUser != null) {
                                                    signedInUser.getIdToken(false).addOnCompleteListener(tokenTask -> {
                                                        if (tokenTask.isSuccessful()) {
                                                            String idToken = tokenTask.getResult().getToken();
                                                            Log.d("Auth Token", "Token: " + idToken);
                                                            // Use this token in your API request
                                                        }
                                                    });
                                                }
                                            }
                                        });

                                Intent intent = new Intent(Register.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Toast.makeText(Register.this, "Registration failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });
    }
}
