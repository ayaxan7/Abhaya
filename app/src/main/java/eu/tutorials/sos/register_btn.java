package eu.tutorials.sos;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class register_btn extends AppCompatActivity {
    private AppCompatButton loginEmailButton;
    private AppCompatButton loginGoogleButton;

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

        // Initialize buttons
        loginEmailButton = findViewById(R.id.loginEmailButton);
        loginGoogleButton = findViewById(R.id.loginGoogleButton);

        // Set onClick listeners
        loginEmailButton.setOnClickListener(v -> {
            Intent intent = new Intent(register_btn.this, Login.class);
            startActivity(intent);
        });

        loginGoogleButton.setOnClickListener(v -> {
            // Handle Google login button click
            // For example, start a Google login activity
        });
    }
}
