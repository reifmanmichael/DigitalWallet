package com.example.digitalwallet;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.digitalwallet.Model.Pocket;
import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.Utils.CustomPopup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class PocketTransferActivity extends AppCompatActivity {

    private String pocketId, pocketName, mode;
    private DatabaseReference mUserRef;
    private double currentBalance = 0;
    private double pocketAmount = 0;

    private EditText etAmount;
    private TextView tvType, tvName, btnConfirm;
    private View cardError;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pocket_transfer);

        pocketId = getIntent().getStringExtra("pocket_id");
        pocketName = getIntent().getStringExtra("pocket_name");
        mode = getIntent().getStringExtra("mode"); // "deposit" or "withdraw"

        String uid = FirebaseAuth.getInstance().getUid();
        mUserRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        tvType = findViewById(R.id.tvTransferType);
        tvName = findViewById(R.id.tvPocketName);
        etAmount = findViewById(R.id.etAmount);
        btnConfirm = findViewById(R.id.btnConfirm);
        cardError = findViewById(R.id.cardErrorPopup);

        tvType.setText(mode.substring(0, 1).toUpperCase() + mode.substring(1));
        tvName.setText(pocketName);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        loadData();

        etAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                validateAmount(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnConfirm.setOnClickListener(v -> performTransfer());
    }

    private void loadData() {
        mUserRef.child("balance").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) currentBalance = Double.parseDouble(snapshot.getValue().toString());
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        mUserRef.child("pockets").child(pocketId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Pocket p = snapshot.getValue(Pocket.class);
                if (p != null) pocketAmount = p.amount;
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void validateAmount(String s) {
        if (s.isEmpty()) {
            btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_inactive));
            btnConfirm.setEnabled(false);
            cardError.setVisibility(View.GONE);
            return;
        }

        double val = Double.parseDouble(s);
        boolean isError = false;

        if (mode.equals("deposit")) {
            if (val > currentBalance) isError = true;
        } else {
            if (val > pocketAmount) isError = true;
        }

        if (isError) {
            cardError.setVisibility(View.VISIBLE);
            btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_inactive));
            btnConfirm.setEnabled(false);
        } else {
            cardError.setVisibility(View.GONE);
            btnConfirm.setBackgroundTintList(ContextCompat.getColorStateList(this, R.color.btn_black_active));
            btnConfirm.setEnabled(true);
        }
    }

    private void performTransfer() {
        String amountStr = etAmount.getText().toString();
        double amount = Double.parseDouble(amountStr);
        Map<String, Object> updates = new HashMap<>();

        if (mode.equals("deposit")) {
            updates.put("balance", currentBalance - amount);
            updates.put("pockets/" + pocketId + "/amount", pocketAmount + amount);
        } else {
            updates.put("balance", currentBalance + amount);
            updates.put("pockets/" + pocketId + "/amount", pocketAmount - amount);
        }

        // --- NEW: Add this to the general transactions list as well ---
        String txId = mUserRef.child("transactions").push().getKey();
        long timestamp = System.currentTimeMillis();
        
        // Pocket transactions are technically "internal", so we'll use a special format
        Transaction tx = new Transaction(txId, 
                mode.equals("deposit") ? "sent" : "received", 
                "completed", 
                amount, 
                timestamp, 
                pocketId, 
                "Pocket: " + pocketName, 
                "#8E8E93", // Neutral gray for pocket transactions
                FirebaseAuth.getInstance().getUid());

        updates.put("transactions/" + txId, tx);
        
        // Also save to a pocket-specific history if needed in future
        updates.put("pockets/" + pocketId + "/activity/" + txId, tx);

        mUserRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String action = mode.equals("deposit") ? "Deposited" : "Withdrew";
                String target = mode.equals("deposit") ? "into your pocket." : "from your pocket.";
                MainActivity.pendingSuccessMessage = action + " ₪" + amountStr + " " + target;
                finish(); 
            }
        });
    }
}