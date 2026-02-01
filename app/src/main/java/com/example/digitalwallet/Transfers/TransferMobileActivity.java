package com.example.digitalwallet.Transfers;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
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
import com.example.digitalwallet.Utils.ProfileUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
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

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        });

        btnContinue.setOnClickListener(v -> {
            if (foundUser != null) {
                Intent intent = new Intent(this, TransferAmountActivity.class);
                intent.putExtra("recipient_uid", foundUser.uid);
                intent.putExtra("recipient_name", foundUser.displayName);
                intent.putExtra("mode", mode);
                startActivity(intent);
            }
        });

        etPhone.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                foundUser = null;
                setContinueActive(false);
                layoutUserResult.setVisibility(View.GONE);
                
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);
            }

            @Override public void afterTextChanged(Editable s) {
                final String phone = s.toString().trim();
                if (phone.length() >= 9) {
                    progressBar.setVisibility(View.VISIBLE);
                    searchRunnable = () -> performSearch(phone);
                    searchHandler.postDelayed(searchRunnable, 500);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }

    private void performSearch(final String phone) {
        final String cleanPhone = phone.replaceAll("[^0-9]", "");
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("Users");
        
        usersRef.orderByChild("phone").equalTo(cleanPhone).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    handleSearchResults(snapshot);
                } else {
                    String noZero = cleanPhone.startsWith("0") ? cleanPhone.substring(1) : cleanPhone;
                    try {
                        long phoneNum = Long.parseLong(noZero);
                        usersRef.orderByChild("phone").equalTo(phoneNum).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot2) {
                                if (snapshot2.exists()) {
                                    handleSearchResults(snapshot2);
                                } else {
                                    // Try string without zero
                                    usersRef.orderByChild("phone").equalTo(noZero).addListenerForSingleValueEvent(new SearchResultListener());
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) { progressBar.setVisibility(View.GONE); }
                        });
                    } catch (Exception e) {
                        progressBar.setVisibility(View.GONE);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                if (error.getMessage().contains("Permission denied")) {
                    Toast.makeText(TransferMobileActivity.this, "Security Rules Blocked Search.", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private class SearchResultListener implements ValueEventListener {
        @Override
        public void onDataChange(@NonNull DataSnapshot snapshot) {
            handleSearchResults(snapshot);
        }
        @Override
        public void onCancelled(@NonNull DatabaseError error) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private void handleSearchResults(DataSnapshot snapshot) {
        progressBar.setVisibility(View.GONE);
        for (DataSnapshot child : snapshot.getChildren()) {
            User user = child.getValue(User.class);
            if (user != null) {
                if (user.uid == null) user.uid = child.getKey();
                if (user.uid != null && user.uid.equals(myUid)) {
                    Toast.makeText(this, "Cannot transfer to yourself", Toast.LENGTH_SHORT).show();
                    return;
                }
                foundUser = user;
                showFoundUser(foundUser);
                return;
            }
        }
    }

    private void showFoundUser(User user) {
        layoutUserResult.setVisibility(View.VISIBLE);
        tvResultName.setText(user.displayName);

        View profileContainer = findViewById(R.id.layoutProfileContainer);
        if (profileContainer != null) {
            ProfileUtils.setProfileInitial(profileContainer, user.displayName, user.profileColor);
        }

        setContinueActive(true);
    }

    private void setContinueActive(boolean active) {
        if (active) {
            btnContinue.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.btn_black_active)));
            btnContinue.setEnabled(true);
            btnContinue.setAlpha(1.0f);
        } else {
            btnContinue.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.btn_black_inactive)));
            btnContinue.setEnabled(false);
            btnContinue.setAlpha(0.5f);
        }
    }
}