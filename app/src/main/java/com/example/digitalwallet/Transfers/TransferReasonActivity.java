package com.example.digitalwallet.Transfers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.digitalwallet.MainActivity;
import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TransferReasonActivity extends AppCompatActivity {

    private String recipientUid, recipientName, amountStr;
    private EditText etReason;
    private TextView btnTransfer; // Global ref to disable it
    private DatabaseReference mDb;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_reason);

        // Security check
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish(); return;
        }

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDb = FirebaseDatabase.getInstance().getReference();

        recipientUid = getIntent().getStringExtra("recipient_uid");
        recipientName = getIntent().getStringExtra("recipient_name");
        amountStr = getIntent().getStringExtra("amount");

        // UI Setup
        TextView headerInfo = findViewById(R.id.tvHeaderInfo);
        headerInfo.setText(recipientName + " • ₪" + amountStr);

        btnTransfer = findViewById(R.id.btnTransferFinal);
        btnTransfer.setText("Transfer to " + recipientName);

        etReason = findViewById(R.id.etReason);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> navigateHome());

        btnTransfer.setOnClickListener(v -> executeTransaction());
    }

    private void executeTransaction() {
        // 1. DISABLE BUTTON IMMEDIATELY to prevent double clicks
        btnTransfer.setEnabled(false);
        btnTransfer.setAlpha(0.5f);
        btnTransfer.setText("Processing...");

        double amount = Double.parseDouble(amountStr);

        // Check Balance
        mDb.child("Users").child(myUid).child("balance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double myBalance = 0;
                if (snapshot.exists() && snapshot.getValue() != null) {
                    myBalance = Double.parseDouble(snapshot.getValue().toString());
                }

                if (myBalance >= amount) {
                    performTransfer(myBalance, amount);
                } else {
                    Toast.makeText(TransferReasonActivity.this, "Insufficient Funds", Toast.LENGTH_SHORT).show();
                    // Re-enable if failed
                    btnTransfer.setEnabled(true);
                    btnTransfer.setAlpha(1f);
                    btnTransfer.setText("Transfer to " + recipientName);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {
                btnTransfer.setEnabled(true);
            }
        });
    }

    private void performTransfer(double currentBalance, double amount) {
        long timestamp = System.currentTimeMillis();
        String txId = mDb.push().getKey(); // Unique ID for this transaction

        // --- STEP 1: DEDUCT FROM ME ---
        mDb.child("Users").child(myUid).child("balance").setValue(currentBalance - amount);

        // --- STEP 2: ADD TO RECIPIENT ---
        mDb.child("Users").child(recipientUid).child("balance").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                double recBalance = 0;
                if (snapshot.exists() && snapshot.getValue() != null) {
                    recBalance = Double.parseDouble(snapshot.getValue().toString());
                }
                // Write new balance
                mDb.child("Users").child(recipientUid).child("balance").setValue(recBalance + amount);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // --- STEP 3: PREPARE HISTORY DATA ---

        // Get Recipient Color (for My History)
        mDb.child("Users").child(recipientUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot recSnap) {
                User recipient = recSnap.getValue(User.class);
                String recColor = (recipient != null && recipient.profileColor != null) ? recipient.profileColor : "#E5E5EA";

                // Save MY History (Sent)
                Transaction myTx = new Transaction(txId, "sent", amount, timestamp, recipientUid, recipientName, recColor);
                mDb.child("Users").child(myUid).child("transactions").child(txId).setValue(myTx);

                // Get My Data (for Recipient History)
                mDb.child("Users").child(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot mySnap) {
                        User me = mySnap.getValue(User.class);
                        String myColor = (me != null && me.profileColor != null) ? me.profileColor : "#E5E5EA";
                        String myName = (me != null) ? me.displayName : "Unknown";

                        // Save THEIR History (Received)
                        Transaction theirTx = new Transaction(txId, "received", amount, timestamp, myUid, myName, myColor);
                        mDb.child("Users").child(recipientUid).child("transactions").child(txId).setValue(theirTx);

                        // --- STEP 4: CONTACTS & FREQUENCY ---

                        // Save Recipient to My Contacts
                        mDb.child("Users").child(myUid).child("saved_contacts").child(recipientUid).setValue(true);

                        // Save ME to Recipient's Contacts (So they see me too!)
                        mDb.child("Users").child(recipientUid).child("saved_contacts").child(myUid).setValue(true);

                        // Update Frequency (For My "Most Active")
                        DatabaseReference freqRef = mDb.child("Users").child(myUid).child("frequencies").child(recipientUid);
                        freqRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot freqSnap) {
                                long count = 0;
                                if (freqSnap.exists() && freqSnap.getValue() != null) {
                                    count = (long) freqSnap.getValue();
                                }
                                freqRef.setValue(count + 1);

                                // --- FINAL STEP: NAVIGATE ---
                                Toast.makeText(TransferReasonActivity.this, "Transfer Successful!", Toast.LENGTH_LONG).show();
                                navigateHome();
                            }
                            @Override public void onCancelled(@NonNull DatabaseError error) {
                                navigateHome(); // Navigate even if stats fail
                            }
                        });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) { navigateHome(); }
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { navigateHome(); }
        });
    }

    private void navigateHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}