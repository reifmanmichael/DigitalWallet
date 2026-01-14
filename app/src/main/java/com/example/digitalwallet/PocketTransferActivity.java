package com.example.digitalwallet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.digitalwallet.Model.Pocket;
import com.example.digitalwallet.Model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class PocketTransferActivity extends AppCompatActivity {

    private String pocketId, pocketName, mode; // mode: "deposit" or "withdraw"
    private double currentPocketAmount, mainBalance;
    private EditText etAmount;
    private TextView btnConfirm, tvType, tvPocketName;
    private View cardErrorPopup;
    private TextView tvErrorDetail;
    private DatabaseReference mUserRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pocket_transfer);

        pocketId = getIntent().getStringExtra("pocket_id");
        pocketName = getIntent().getStringExtra("pocket_name");
        mode = getIntent().getStringExtra("mode");

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || pocketId == null) { finish(); return; }
        mUserRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        etAmount = findViewById(R.id.etAmount);
        btnConfirm = findViewById(R.id.btnConfirm);
        tvType = findViewById(R.id.tvTransferType);
        tvPocketName = findViewById(R.id.tvPocketName);
        cardErrorPopup = findViewById(R.id.cardErrorPopup);
        tvErrorDetail = findViewById(R.id.tvErrorDetail);

        tvPocketName.setText(pocketName);
        tvType.setText(mode.substring(0, 1).toUpperCase() + mode.substring(1));
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        loadData();

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAmount(s.toString().trim());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnConfirm.setOnClickListener(v -> executeTransfer());
    }

    private void loadData() {
        mUserRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Object balObj = snapshot.child("balance").getValue();
                    mainBalance = balObj != null ? Double.parseDouble(balObj.toString()) : 0.0;
                    
                    DataSnapshot pSnap = snapshot.child("pockets").child(pocketId);
                    if (pSnap.exists()) {
                        Object amtObj = pSnap.child("amount").getValue();
                        currentPocketAmount = amtObj != null ? Double.parseDouble(amtObj.toString()) : 0.0;
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void validateAmount(String input) {
        if (input.isEmpty()) {
            btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_inactive));
            btnConfirm.setEnabled(false);
            hideError();
            return;
        }

        try {
            double amount = Double.parseDouble(input);
            boolean isExceeded = false;

            if ("deposit".equals(mode)) {
                isExceeded = amount > mainBalance;
                tvErrorDetail.setText("You cannot type more than what you have in your current balance.");
            } else {
                isExceeded = amount > currentPocketAmount;
                tvErrorDetail.setText("Cannot withdraw more than what is currently in the pocket balance.");
            }

            if (isExceeded) {
                showError();
                btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_inactive));
                btnConfirm.setEnabled(false);
            } else if (amount > 0) {
                hideError();
                btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_active));
                btnConfirm.setEnabled(true);
            } else {
                hideError();
                btnConfirm.setEnabled(false);
            }
        } catch (NumberFormatException e) {
            hideError();
            btnConfirm.setEnabled(false);
        }
    }

    private void showError() {
        if (cardErrorPopup.getVisibility() == View.VISIBLE) return;
        
        cardErrorPopup.setVisibility(View.VISIBLE);
        cardErrorPopup.setAlpha(0f);
        cardErrorPopup.setTranslationY(100f);
        cardErrorPopup.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(300)
                .start();
    }

    private void hideError() {
        if (cardErrorPopup.getVisibility() == View.GONE) return;
        
        cardErrorPopup.animate()
                .alpha(0f)
                .translationY(100f)
                .setDuration(200)
                .withEndAction(() -> cardErrorPopup.setVisibility(View.GONE))
                .start();
    }

    private void executeTransfer() {
        double amount = Double.parseDouble(etAmount.getText().toString());
        Map<String, Object> updates = new HashMap<>();

        String txId = mUserRef.child("pockets").child(pocketId).child("activity").push().getKey();
        Transaction tx = new Transaction();
        tx.id = txId;
        tx.amount = amount;
        tx.timestamp = System.currentTimeMillis();
        tx.status = "completed";
        tx.type = mode; // "deposit" or "withdraw"

        if ("deposit".equals(mode)) {
            updates.put("balance", mainBalance - amount);
            updates.put("pockets/" + pocketId + "/amount", currentPocketAmount + amount);
        } else {
            updates.put("balance", mainBalance + amount);
            updates.put("pockets/" + pocketId + "/amount", currentPocketAmount - amount);
        }
        
        updates.put("pockets/" + pocketId + "/activity/" + txId, tx);

        mUserRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Transfer successful", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}