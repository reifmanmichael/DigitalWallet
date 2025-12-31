package com.example.digitalwallet;

import android.content.Intent;
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

import com.example.digitalwallet.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Random;

public class RegisterActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone, etPassword;
    private Button btnRegister;
    private TextView tvLogin;
    private ProgressBar progressBar;

    // Validator Icons
    private ImageView imgCheckLength, imgCheckCase, imgCheckSymbol;

    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // Init Views
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etPassword = findViewById(R.id.etPassword);
        btnRegister = findViewById(R.id.btnRegister);
        tvLogin = findViewById(R.id.tvLogin);
        progressBar = findViewById(R.id.progressBar);

        // Init Validator Icons (MAKE SURE THESE IDs MATCH YOUR XML)
        imgCheckLength = findViewById(R.id.imgCheckLength); // The square next to "8-20 Characters"
        imgCheckCase = findViewById(R.id.imgCheckCase);     // The square next to "Upper & Lowercase"
        imgCheckSymbol = findViewById(R.id.imgCheckNumSym); // The square next to "Number or Symbol"

        // *** ADD THE PASSWORD LISTENER HERE ***
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

                // 1. Check Length (8-20)
                boolean hasLength = pass.length() >= 8 && pass.length() <= 20;
                updateCheckColor(imgCheckLength, hasLength);

                // 2. Check Case (Upper & Lower)
                boolean hasUpper = pass.matches(".*[A-Z].*");
                boolean hasLower = pass.matches(".*[a-z].*");
                updateCheckColor(imgCheckCase, hasUpper && hasLower);

                // 3. Check Symbol OR Number
                boolean hasNum = pass.matches(".*[0-9].*");
                // Checks for special characters
                boolean hasSymbol = pass.matches(".*[!@#$%^&*()_+=|<>?{}\\[\\]~-].*");
                updateCheckColor(imgCheckSymbol, hasNum || hasSymbol);
            }

            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void updateCheckColor(ImageView view, boolean isValid) {
        if (view == null) return;
        if (isValid) {
            view.setColorFilter(Color.parseColor("#34C759")); // Green
        } else {
            view.setColorFilter(Color.parseColor("#FF3B30")); // Red
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

        // Optional: Enforce these rules strictly before creating account
        if (password.length() < 8) {
            etPassword.setError("Password too short");
            return;
        }

        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getCurrentUser().getUid();

                        String[] colors = {"#FF3B30", "#FF9500", "#FFCC00", "#34C759", "#007AFF", "#AF52DE", "#FF2D55"};
                        String randomColor = colors[new Random().nextInt(colors.length)];

                        User newUser = new User(uid, name, email, phone, 0.0, randomColor);

                        FirebaseDatabase.getInstance().getReference("Users")
                                .child(uid)
                                .setValue(newUser)
                                .addOnCompleteListener(dbTask -> {
                                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                                    if (dbTask.isSuccessful()) {
                                        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                        startActivity(intent);
                                        finish();
                                    } else {
                                        Toast.makeText(RegisterActivity.this, "Save Failed", Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        if (progressBar != null) progressBar.setVisibility(View.GONE);
                        Toast.makeText(RegisterActivity.this, "Registration Failed", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}