package com.example.digitalwallet.Transfers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.digitalwallet.MainActivity;
import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.R;
import com.example.digitalwallet.Utils.CustomPopup;
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

        myUid = FirebaseAuth.getInstance().getUid();
        mDb = FirebaseDatabase.getInstance().getReference();

        recipientUid = getIntent().getStringExtra("recipient_uid");
        recipientName = getIntent().getStringExtra("recipient_name");
        amountStr = getIntent().getStringExtra("amount");
        mode = getIntent().getStringExtra("mode"); 

        TextView headerInfo = findViewById(R.id.tvHeaderInfo);
        headerInfo.setText(recipientName + " • ₪" + amountStr);

        btnTransfer = findViewById(R.id.btnTransferFinal);
        btnTransfer.setText(("request".equals(mode) ? "Request from " : "Transfer to ") + recipientName);

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
            CustomPopup.show(this, "Error", "Invalid amount");
            resetButton();
            return;
        }

        mDb.child("Users").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot usersSnap) {
                DataSnapshot mySnap = usersSnap.child(myUid);
                DataSnapshot recSnap = usersSnap.child(recipientUid);

                if (!mySnap.exists() || !recSnap.exists()) {
                    CustomPopup.show(TransferReasonActivity.this, "Error", "User details not found");
                    resetButton();
                    return;
                }

                User me = mySnap.getValue(User.class);
                User recipient = recSnap.getValue(User.class);

                if (me == null || recipient == null) {
                    CustomPopup.show(TransferReasonActivity.this, "Error", "Error parsing user data");
                    resetButton();
                    return;
                }

                // Check balance if sending
                if (!"request".equals(mode) && me.balance < amount) {
                    CustomPopup.show(TransferReasonActivity.this, "Insufficient Funds", "You don't have enough balance to complete this transfer.");
                    resetButton();
                    return;
                }

                performPendingTransaction(me, recipient, amount, mySnap, recSnap);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { resetButton(); }
        });
    }

    private void performPendingTransaction(User me, User recipient, double amount, DataSnapshot mySnap, DataSnapshot recSnap) {
        long timestamp = System.currentTimeMillis();
        String txId = mDb.push().getKey();
        if (txId == null) txId = String.valueOf(timestamp);

        String reason = etReason.getText().toString().trim();

        Map<String, Object> updates = new HashMap<>();
        String myColor = me.profileColor != null ? me.profileColor : "#E5E5EA";
        String recColor = recipient.profileColor != null ? recipient.profileColor : "#E5E5EA";

        Transaction myTx, recTx;
        if ("request".equals(mode)) {
            // REQUEST: Initiator (me) is receiver, Target (recipient) is sender. No balance change yet.
            myTx = new Transaction(txId, "received", "pending", amount, timestamp, recipientUid, recipient.displayName, recColor, myUid, reason);
            recTx = new Transaction(txId, "sent", "pending", amount, timestamp, myUid, me.displayName, myColor, myUid, reason);
        } else {
            // SEND: Initiator (me) is sender, Target (recipient) is receiver. 
            // REDUCE balance from initiator immediately (limbo).
            myTx = new Transaction(txId, "sent", "pending", amount, timestamp, recipientUid, recipient.displayName, recColor, myUid, reason);
            recTx = new Transaction(txId, "received", "pending", amount, timestamp, myUid, me.displayName, myColor, myUid, reason);
            
            updates.put("Users/" + myUid + "/balance", me.balance - amount);
        }

        updates.put("Users/" + myUid + "/transactions/" + txId, myTx);
        updates.put("Users/" + recipientUid + "/transactions/" + txId, recTx);
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
                MainActivity.pendingSuccessMessage = ("request".equals(mode) ? "Requested ₪" : "Sent ₪") + amountStr;
                navigateHome();
            } else {
                CustomPopup.show(TransferReasonActivity.this, "Error", "Failed to initiate transaction.");
                resetButton();
            }
        });
    }

    private void resetButton() {
        btnTransfer.setEnabled(true);
        btnTransfer.setAlpha(1f);
        btnTransfer.setText(("request".equals(mode) ? "Request from " : "Transfer to ") + recipientName);
    }

    private void navigateHome() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}