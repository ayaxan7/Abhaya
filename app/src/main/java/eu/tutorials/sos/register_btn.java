package eu.tutorials.sos;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

public class register_btn extends AppCompatActivity {
    AppCompatButton loginEmailButton;
    AppCompatButton loginDigiLockerButton;
    FirebaseAuth mauth;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register_btn);

        // Set up window insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        mauth=FirebaseAuth.getInstance();
        if(mauth.getCurrentUser()!=null){
            Intent intent = new Intent(register_btn.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        // Initialize buttons
        loginEmailButton = findViewById(R.id.loginEmailButton);
        loginDigiLockerButton = findViewById(R.id.loginGoogleButton);
        // Set onClick listeners
        loginEmailButton.setOnClickListener(v -> {
            Intent intent = new Intent(register_btn.this, Login.class);
            startActivity(intent);
        });
        loginDigiLockerButton.setOnClickListener(v -> {
            Snackbar.make(findViewById(R.id.main), "This feature is coming soon...", Snackbar.LENGTH_SHORT)
                    .setAction("Action", null).show();
        });
    }
}
