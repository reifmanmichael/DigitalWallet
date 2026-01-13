package com.example.digitalwallet;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class LoginActivity extends AppCompatActivity {

    private EditText etInput, etPassword;
    private TextView tvLogo;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Mode for Login/Register
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            applyUserThemeAndNavigate(mAuth.getCurrentUser());
            return;
        }

        etInput = findViewById(R.id.etLoginInput);
        etPassword = findViewById(R.id.etLoginPass);
        tvLogo = findViewById(R.id.tvLogo);

        // "Breathing" Animation for the Logo
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(tvLogo, View.SCALE_X, 1f, 1.05f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(tvLogo, View.SCALE_Y, 1f, 1.05f);
        scaleX.setDuration(2000);
        scaleY.setDuration(2000);
        scaleX.setRepeatCount(ObjectAnimator.INFINITE);
        scaleY.setRepeatCount(ObjectAnimator.INFINITE);
        scaleX.setRepeatMode(ObjectAnimator.REVERSE);
        scaleY.setRepeatMode(ObjectAnimator.REVERSE);
        scaleX.start();
        scaleY.start();

        findViewById(R.id.btnLogin).setOnClickListener(v -> handleLogin());
        findViewById(R.id.tvGoToRegister).setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class))
        );
    }

    private void handleLogin() {
        String input = etInput.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (input.isEmpty() || pass.isEmpty()) return;

        if (input.contains("@")) {
            loginWithEmail(input, pass);
        } else {
            loginWithPhoneLookup(input, pass);
        }
    }

    private void loginWithPhoneLookup(String phone, String password) {
        FirebaseDatabase.getInstance().getReference("Users")
                .orderByChild("phone")
                .equalTo(phone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot child : snapshot.getChildren()) {
                                String email = child.child("email").getValue(String.class);
                                if (email != null) {
                                    loginWithEmail(email, password);
                                    return;
                                }
                            }
                        }
                        Toast.makeText(LoginActivity.this, "Phone number not found.", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(LoginActivity.this, "Database error.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loginWithEmail(String email, String password) {
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    applyUserThemeAndNavigate(authResult.getUser());
                })
                .addOnFailureListener(e ->
                        Toast.makeText(LoginActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void applyUserThemeAndNavigate(FirebaseUser user) {
        if (user != null) {
            SharedPreferences prefs = getSharedPreferences("theme_prefs_" + user.getUid(), MODE_PRIVATE);
            boolean isDarkMode = prefs.getBoolean("dark_mode", false);
            AppCompatDelegate.setDefaultNightMode(isDarkMode ? 
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        }
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}