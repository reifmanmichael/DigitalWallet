package com.example.digitalwallet;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.digitalwallet.Model.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFullName, etDisplayName, etEmail, etPhone, etAddress, etPassword;
    private Button btnRegister, btnTypePersonal, btnTypeBusiness;
    private TextView checkLength, checkCase, checkNumSym;

    private String selectedAccountType = "Personal";
    private boolean isPasswordValid = false;

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Handle Notches & Full Screen
        getWindow().getDecorView().setOnApplyWindowInsetsListener((view, insets) -> {
            view.setPadding(0, insets.getInsets(WindowInsets.Type.systemBars()).top, 0, 0);
            return insets;
        });

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("Users");

        initViews();
        setupPasswordWatcher();
        setupTypeButtons();

        btnRegister.setOnClickListener(v -> attemptRegister());
    }

    private void initViews() {
        etFullName = findViewById(R.id.etFullName);
        etDisplayName = findViewById(R.id.etDisplayName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        etAddress = findViewById(R.id.etAddress);
        etPassword = findViewById(R.id.etPassword);

        btnRegister = findViewById(R.id.btnRegister);
        btnTypePersonal = findViewById(R.id.btnTypePersonal);
        btnTypeBusiness = findViewById(R.id.btnTypeBusiness);

        checkLength = findViewById(R.id.checkLength);
        checkCase = findViewById(R.id.checkCase);
        checkNumSym = findViewById(R.id.checkNumSym);
    }

    private void setupTypeButtons() {
        View.OnClickListener listener = v -> {
            boolean isPersonal = v.getId() == R.id.btnTypePersonal;
            selectedAccountType = isPersonal ? "Personal" : "Business";

            // Toggle visual state (Custom visual logic)
            btnTypePersonal.setBackgroundResource(isPersonal ? R.drawable.bg_rounded_button : R.drawable.bg_rounded_input);
            btnTypePersonal.setTextColor(isPersonal ? Color.WHITE : Color.BLACK);

            btnTypeBusiness.setBackgroundResource(!isPersonal ? R.drawable.bg_rounded_button : R.drawable.bg_rounded_input);
            btnTypeBusiness.setTextColor(!isPersonal ? Color.WHITE : Color.BLACK);
        };

        btnTypePersonal.setOnClickListener(listener);
        btnTypeBusiness.setOnClickListener(listener);
    }

    private void setupPasswordWatcher() {
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String input = s.toString();

                boolean hasLength = input.length() >= 8 && input.length() <= 20;
                boolean hasCase = !input.equals(input.toLowerCase()) && !input.equals(input.toUpperCase());
                boolean hasNumSym = Pattern.compile("[0-9]").matcher(input).find() || Pattern.compile("[^a-zA-Z0-9]").matcher(input).find();

                updateCheckColor(checkLength, hasLength);
                updateCheckColor(checkCase, hasCase);
                updateCheckColor(checkNumSym, hasNumSym);

                isPasswordValid = hasLength && hasCase && hasNumSym;
            }
        });
    }

    private void updateCheckColor(TextView view, boolean isValid) {
        int color = isValid ? ContextCompat.getColor(this, R.color.success_green) : Color.GRAY;
        view.setTextColor(color);
    }

    private void attemptRegister() {
        if (!isPasswordValid) {
            etPassword.setError("Password criteria not met");
            return;
        }

        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        if (email.isEmpty()) return;

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    saveUserToDatabase(authResult.getUser().getUid());
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(RegisterActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveUserToDatabase(String uid) {
        User user = new User(
                uid,
                etFullName.getText().toString(),
                etDisplayName.getText().toString(),
                etEmail.getText().toString(),
                etPhone.getText().toString(),
                etAddress.getText().toString(),
                selectedAccountType
        );

        mDatabase.child(uid).setValue(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(RegisterActivity.this, MainActivity.class));
                finishAffinity();
            }
        });
    }
}