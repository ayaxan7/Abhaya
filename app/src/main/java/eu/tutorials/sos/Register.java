package eu.tutorials.sos;

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
            String name1, email1, password1, phone1;
            name1 = String.valueOf(name.getText()).trim();
            email1 = String.valueOf(email.getText()).trim();
            password1 = String.valueOf(password.getText()).trim();
            phone1 = String.valueOf(phone.getText()).trim();

            if (name1.isEmpty() || email1.isEmpty() || password1.isEmpty() || phone1.isEmpty()) {
                Toast.makeText(Register.this, "Enter all fields", Toast.LENGTH_SHORT).show();
                bar.setVisibility(View.GONE);
                return;
            }

            if (password1.length() < 6) {
                Toast.makeText(Register.this, "Password should be at least 6 characters", Toast.LENGTH_SHORT).show();
                bar.setVisibility(View.GONE);
                return;
            }

            mAuth.createUserWithEmailAndPassword(email1, password1)
                    .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            bar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                // Registration successful
                                FirebaseUser user = mAuth.getCurrentUser();
                                if (user != null) {
                                    // Save user data locally
                                    SharedPreferences sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                                    SharedPreferences.Editor editor = sharedPreferences.edit();
                                    editor.putString("name", name1);
                                    editor.putString("phone", phone1);
                                    editor.apply();

                                    Toast.makeText(Register.this, "Registration Successful.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(Register.this, MainActivity.class);
                                    startActivity(intent);
                                    finish();
                                }
                            } else {
                                Toast.makeText(Register.this, "Registration failed.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        });
    }
}
