package com.example.digitalwallet;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.example.digitalwallet.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    private ImageView imgCheckLength, imgCheckCase, imgCheckSymbol;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Force Light Mode for Registration
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        imgCheckLength = findViewById(R.id.imgCheckLength);
        imgCheckCase = findViewById(R.id.imgCheckCase);
        imgCheckSymbol = findViewById(R.id.imgCheckNumSym);

        setupPasswordWatcher();

        btnRegister.setOnClickListener(v -> registerUser());

        if (tvLogin != null) {
            tvLogin.setOnClickListener(v -> {
                startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
                finish();
            });
        }
    }

    private void setupPasswordWatcher() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pass = s.toString();
                boolean hasLength = pass.length() >= 8 && pass.length() <= 20;
                updateCheckColor(imgCheckLength, hasLength);
                boolean hasUpper = pass.matches(".*[A-Z].*");
                boolean hasLower = pass.matches(".*[a-z].*");
                updateCheckColor(imgCheckCase, hasUpper && hasLower);
                boolean hasNum = pass.matches(".*[0-9].*");
                boolean hasSymbol = pass.matches(".*[!@#$%^&*()_+=|<>?{}\\[\\]~-].*");
                updateCheckColor(imgCheckSymbol, hasNum || hasSymbol);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void updateCheckColor(ImageView view, boolean isValid) {
        if (view == null) return;
        if (isValid) {
            view.setColorFilter(Color.parseColor("#34C759"));
        } else {
            view.setColorFilter(Color.parseColor("#FF3B30"));
        }
    }

    private void registerUser() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(name)) { etName.setError("Required"); return; }
        if (TextUtils.isEmpty(email)) { etEmail.setError("Required"); return; }
        if (TextUtils.isEmpty(phone)) { etPhone.setError("Required"); return; }
        if (TextUtils.isEmpty(password)) { etPassword.setError("Required"); return; }

        if (password.length() < 8) {
            etPassword.setError("Password too short");
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String uid = user.getUid();

                        String[] colors = {"#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#007AFF", "#AF52DE", "#FF2D55"};
                        String randomColor = colors[new Random().nextInt(colors.length)];

                        // Generate random bank balance between 1M and 10B
                        long min = 1_000_000L;
                        long max = 10_000_000_000L;
                        double randomBankBalance = min + (new Random().nextDouble() * (max - min));

                        User newUser = new User(uid, name, email, phone, 0.0, randomBankBalance, randomColor);

                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(uid)
                                .setValue(newUser)
                                .addOnCompleteListener(dbTask -> {
                                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                                    if (dbTask.isSuccessful()) {
                                        // Set default light theme for new user
                                        SharedPreferences prefs = getSharedPreferences("theme_prefs_" + uid, MODE_PRIVATE);
                                        prefs.edit().putBoolean("dark_mode", false).apply();
                                        
                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        String error = dbTask.getException() != null ? dbTask.getException().getMessage() : "Database error";
                                        Toast.makeText(RegisterActivity.this, "Save Failed: " + error, Toast.LENGTH_LONG).show();
                                    }
                                });
                    } else {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        
                        String errorMessage = "Registration Failed";
                        Exception exception = task.getException();
                        
                        if (exception instanceof FirebaseAuthUserCollisionException) {
                            errorMessage = "This email is already registered.";
                        } else if (exception instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMessage = "Invalid email format.";
                        } else if (exception instanceof FirebaseAuthWeakPasswordException) {
                            errorMessage = "Password is too weak.";
                        } else if (exception != null) {
                            errorMessage = exception.getMessage();
                        }
                        
                        Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
    }
}