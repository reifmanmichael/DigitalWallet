package com.example.digitalwallet;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.digitalwallet.Model.Pocket;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CreateSavingsPlanActivity extends AppCompatActivity {

    private EditText etInitialAmount;
    private TextView btnContinue;
    private String pocketName, pocketType;
    private double currentMainBalance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_savings_plan);

        pocketName = getIntent().getStringExtra("pocket_name");
        pocketType = getIntent().getStringExtra("pocket_type");

        etInitialAmount = findViewById(R.id.etInitialAmount);
        btnContinue = findViewById(R.id.btnContinue);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        loadMainBalance();

        etInitialAmount.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double val = Double.parseDouble(s.toString().trim());
                    if (val > 0) {
                        btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(CreateSavingsPlanActivity.this, R.color.btn_black_active));
                        btnContinue.setEnabled(true);
                    } else {
                        throw new NumberFormatException();
                    }
                } catch (NumberFormatException e) {
                    btnContinue.setBackgroundTintList(ContextCompat.getColorStateList(CreateSavingsPlanActivity.this, R.color.btn_black_inactive));
                    btnContinue.setEnabled(false);
                }
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnContinue.setOnClickListener(v -> finalizeSavingsPocket());
    }

    private void finalizeSavingsPocket() {
        double amount = Double.parseDouble(etInitialAmount.getText().toString());
        if (amount > currentMainBalance) {
            Toast.makeText(this, "Insufficient funds", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference mUserRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        String key = mUserRef.child("pockets").push().getKey();
        if (key == null) return;

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, 4);

        Pocket pocket = new Pocket(key, pocketName, amount, pocketType, true);
        pocket.iconName = "ic_savings_illustration"; // Pre-determined icon
        pocket.interestRate = 0.04;
        pocket.lockEndDate = cal.getTimeInMillis();
        pocket.initialDeposit = amount;

        Map<String, Object> updates = new HashMap<>();
        updates.put("balance", currentMainBalance - amount);
        updates.put("pockets/" + key, pocket);

        mUserRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Intent intent = new Intent(this, MainActivity.class);
                intent.putExtra("show_pocket_success", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
            }
        });
    }

    private void loadMainBalance() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("balance")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) currentMainBalance = Double.parseDouble(snapshot.getValue().toString());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }
}