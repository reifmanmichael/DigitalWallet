package com.example.digitalwallet.Transfers;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.digitalwallet.MainActivity;
import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TransferMobileActivity extends AppCompatActivity {

    private EditText etPhone;
    private TextView btnContinue, tvResultName;
    private LinearLayout layoutUserResult;
    private ProgressBar progressBar;
    private String mode;
    private String myUid;

    private Handler searchHandler = new Handler(Looper.getMainLooper());
    private Runnable searchRunnable;
    private User foundUser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_mobile);

        mode = getIntent().getStringExtra("mode");
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        }

        etPhone = findViewById(R.id.etPhone);
        btnContinue = findViewById(R.id.btnContinue);
        layoutUserResult = findViewById(R.id.layoutUserResult);
        tvResultName = findViewById(R.id.tvResultName);
        progressBar = findViewById(R.id.progressBar);

        // Navigation
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        // Continue Logic
        btnContinue.setOnClickListener(v -> {
            if (foundUser != null) {
                Intent intent = new Intent(this, TransferAmountActivity.class);
                intent.putExtra("recipient_uid", foundUser.uid);
                intent.putExtra("recipient_name", foundUser.displayName);
                intent.putExtra("mode", mode);
                startActivity(intent);
            }
        });

        // Input Listener with Debounce (Wait 500ms after typing stops)
        etPhone.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Reset State on type
                foundUser = null;
                setContinueActive(false);
                layoutUserResult.setVisibility(View.GONE);
                progressBar.setVisibility(View.GONE);

                // Cancel previous search
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }

            @Override public void afterTextChanged(Editable s) {
                String phone = s.toString().trim();
                if (phone.length() >= 9) { // Minimal realistic length
                    progressBar.setVisibility(View.VISIBLE);
                    searchRunnable = () -> searchUser(phone);
                    searchHandler.postDelayed(searchRunnable, 600); // Wait 0.6s
                }
            }
        });
    }

    private void searchUser(String phone) {
        FirebaseDatabase.getInstance().getReference("Users")
                .orderByChild("phone")
                .equalTo(phone)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        progressBar.setVisibility(View.GONE);
                        if (snapshot.exists()) {
                            // User Found
                            for (DataSnapshot child : snapshot.getChildren()) {
                                User user = child.getValue(User.class);
                                if (user != null) {
                                    if (user.uid == null) user.uid = child.getKey();
                                    
                                    // SECURITY: Cannot send/request to yourself
                                    if (user.uid != null && user.uid.equals(myUid)) {
                                        Toast.makeText(TransferMobileActivity.this, "You cannot transfer to yourself", Toast.LENGTH_SHORT).show();
                                        return;
                                    }
                                    
                                    foundUser = user;
                                    showFoundUser(foundUser);
                                    return;
                                }
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {
                        progressBar.setVisibility(View.GONE);
                    }
                });
    }

    private void showFoundUser(User user) {
        layoutUserResult.setVisibility(View.VISIBLE);
        tvResultName.setText(user.displayName);

        // Color Logic
        ImageView bg = findViewById(R.id.imgResultBg);
        String color = user.profileColor != null ? user.profileColor : "#E5E5EA";
        try {
            bg.setColorFilter(android.graphics.Color.parseColor(color));
        } catch (Exception e) {
            bg.setColorFilter(android.graphics.Color.GRAY);
        }

        setContinueActive(true);
    }

    private void setContinueActive(boolean active) {
        if (active) {
            // Use Black for active
            btnContinue.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.btn_black_active)));
        } else {
            // Use Gray for inactive
            btnContinue.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.btn_black_inactive)));
        }
    }
}