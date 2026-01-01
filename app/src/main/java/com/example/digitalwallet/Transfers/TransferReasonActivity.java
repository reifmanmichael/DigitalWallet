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

import java.util.HashMap;
import java.util.Map;

public class TransferReasonActivity extends AppCompatActivity {

    private String recipientUid, recipientName, amountStr, mode;
    private EditText etReason;
    private TextView btnTransfer;
    private DatabaseReference mDb;
    private String myUid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transfer_reason);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            finish(); return;
        }

        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        mDb = FirebaseDatabase.getInstance().getReference();

        recipientUid = getIntent().getStringExtra("recipient_uid");
        recipientName = getIntent().getStringExtra("recipient_name");
        amountStr = getIntent().getStringExtra("amount");
        mode = getIntent().getStringExtra("mode"); 

        // UI Setup
        TextView headerInfo = findViewById(R.id.tvHeaderInfo);
        headerInfo.setText(recipientName + " • ₪" + amountStr);

        btnTransfer = findViewById(R.id.btnTransferFinal);
        if ("request".equals(mode)) {
            btnTransfer.setText("Request from " + recipientName);
        } else {
            btnTransfer.setText("Transfer to " + recipientName);
        }

        etReason = findViewById(R.id.etReason);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnClose).setOnClickListener(v -> navigateHome());

        btnTransfer.setOnClickListener(v -> executeTransaction());
    }

    private void executeTransaction() {
        btnTransfer.setEnabled(false);
        btnTransfer.setAlpha(0.5f);
        btnTransfer.setText("Processing...");

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid amount", Toast.LENGTH_SHORT).show();
            resetButton();
            return;
        }

        mDb.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot rootSnap) {
                DataSnapshot mySnap = rootSnap.child("Users").child(myUid);
                DataSnapshot recSnap = rootSnap.child("Users").child(recipientUid);

                if (!mySnap.exists() || !recSnap.exists()) {
                    Toast.makeText(TransferReasonActivity.this, "User not found", Toast.LENGTH_SHORT).show();
                    resetButton();
                    return;
                }

                User me = mySnap.getValue(User.class);
                User recipient = recSnap.getValue(User.class);

                if (me == null || recipient == null) {
                    Toast.makeText(TransferReasonActivity.this, "Error retrieving user data", Toast.LENGTH_SHORT).show();
                    resetButton();
                    return;
                }

                // According to the user, BOTH Send and Request should initiate requests (pending)
                performPendingTransaction(me, recipient, amount, mySnap, recSnap);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { resetButton(); }
        });
    }

    private void performPendingTransaction(User me, User recipient, double amount, DataSnapshot mySnap, DataSnapshot recSnap) {
        long timestamp = System.currentTimeMillis();
        String txId = mDb.push().getKey();
        if (txId == null) txId = String.valueOf(timestamp);

        Map<String, Object> updates = new HashMap<>();
        
        String myColor = me.profileColor != null ? me.profileColor : "#E5E5EA";
        String recColor = recipient.profileColor != null ? recipient.profileColor : "#E5E5EA";

        Transaction myTx, recTx;

        if ("request".equals(mode)) {
            // I (Initiator) am requesting -> I expect to receive
            myTx = new Transaction(txId, "received", "pending", amount, timestamp, recipientUid, recipient.displayName, recColor, myUid);
            // They (Target) are requested from -> They are expected to send
            recTx = new Transaction(txId, "sent", "pending", amount, timestamp, myUid, me.displayName, myColor, myUid);
        } else {
            // I (Initiator) am sending -> I am the one sending
            myTx = new Transaction(txId, "sent", "pending", amount, timestamp, recipientUid, recipient.displayName, recColor, myUid);
            // They (Target) are receiving -> They are receiving
            recTx = new Transaction(txId, "received", "pending", amount, timestamp, myUid, me.displayName, myColor, myUid);
        }

        updates.put("Users/" + myUid + "/transactions/" + txId, myTx);
        updates.put("Users/" + recipientUid + "/transactions/" + txId, recTx);

        // Update contacts and frequencies
        updates.put("Users/" + myUid + "/saved_contacts/" + recipientUid, true);
        updates.put("Users/" + recipientUid + "/saved_contacts/" + myUid, true);

        long myFreq = 0;
        if (mySnap.child("frequencies").child(recipientUid).exists()) {
            Object val = mySnap.child("frequencies").child(recipientUid).getValue();
            if (val instanceof Long) myFreq = (Long) val;
        }
        long recFreq = 0;
        if (recSnap.child("frequencies").child(myUid).exists()) {
            Object val = recSnap.child("frequencies").child(myUid).getValue();
            if (val instanceof Long) recFreq = (Long) val;
        }
        updates.put("Users/" + myUid + "/frequencies/" + recipientUid, myFreq + 1);
        updates.put("Users/" + recipientUid + "/frequencies/" + myUid, recFreq + 1);

        mDb.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String msg = "request".equals(mode) ? "Request Sent!" : "Transaction Initiated!";
                Toast.makeText(TransferReasonActivity.this, msg, Toast.LENGTH_LONG).show();
                navigateHome();
            } else {
                Toast.makeText(TransferReasonActivity.this, "Failed to initiate transaction", Toast.LENGTH_SHORT).show();
                resetButton();
            }
        });
    }

    private void resetButton() {
        btnTransfer.setEnabled(true);
        btnTransfer.setAlpha(1f);
        if ("request".equals(mode)) {
            btnTransfer.setText("Request from " + recipientName);
        } else {
            btnTransfer.setText("Transfer to " + recipientName);
        }
    }

    private void navigateHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}