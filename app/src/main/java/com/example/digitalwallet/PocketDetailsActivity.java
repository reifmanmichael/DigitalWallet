package com.example.digitalwallet;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.digitalwallet.Model.Pocket;
import com.example.digitalwallet.Model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PocketDetailsActivity extends AppCompatActivity {

    private String pocketId;
    private DatabaseReference mUserRef;
    private Pocket currentPocket;
    private double mainBalance = 0;

    private TextView tvName, tvBalance, tvNoActivity;
    private ImageView imgIcon;
    private View btnWithdraw, btnDeposit, btnMore;
    private RecyclerView recyclerActivity;
    private ActivityAdapter activityAdapter;
    private final List<Transaction> activityList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pocket_details);

        pocketId = getIntent().getStringExtra("pocket_id");
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null || pocketId == null) {
            finish();
            return;
        }

        mUserRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);

        tvName = findViewById(R.id.tvPocketDetailName);
        tvBalance = findViewById(R.id.tvPocketDetailBalance);
        imgIcon = findViewById(R.id.imgPocketDetailIcon);
        tvNoActivity = findViewById(R.id.tvNoActivity);
        
        btnWithdraw = findViewById(R.id.btnWithdraw);
        btnDeposit = findViewById(R.id.btnDeposit);
        btnMore = findViewById(R.id.btnMore);

        recyclerActivity = findViewById(R.id.recyclerPocketActivity);
        recyclerActivity.setLayoutManager(new LinearLayoutManager(this));
        activityAdapter = new ActivityAdapter(activityList);
        recyclerActivity.setAdapter(activityAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadPocketData();
        loadMainBalance();

        btnDeposit.setOnClickListener(v -> {
            Intent intent = new Intent(this, PocketTransferActivity.class);
            intent.putExtra("pocket_id", pocketId);
            intent.putExtra("pocket_name", currentPocket != null ? currentPocket.name : "");
            intent.putExtra("mode", "deposit");
            startActivity(intent);
        });

        btnWithdraw.setOnClickListener(v -> {
            if (currentPocket != null && currentPocket.isLocked) {
                Toast.makeText(this, "This pocket is locked!", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(this, PocketTransferActivity.class);
            intent.putExtra("pocket_id", pocketId);
            intent.putExtra("pocket_name", currentPocket != null ? currentPocket.name : "");
            intent.putExtra("mode", "withdraw");
            startActivity(intent);
        });

        btnMore.setOnClickListener(this::showMoreMenu);
    }

    private void loadPocketData() {
        mUserRef.child("pockets").child(pocketId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                currentPocket = snapshot.getValue(Pocket.class);
                if (currentPocket != null) {
                    tvName.setText(currentPocket.name);
                    tvBalance.setText(String.format("₪%.2f", currentPocket.amount));
                    
                    if (currentPocket.iconName != null) {
                        int resId = getResources().getIdentifier(currentPocket.iconName, "drawable", getPackageName());
                        if (resId != 0) imgIcon.setImageResource(resId);
                    }

                    // --- HANDLE CLOSED STATE ---
                    updateUIForClosedState(currentPocket.isClosed);

                    // --- LOAD ACTIVITY ---
                    loadActivity(snapshot.child("activity"));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadActivity(DataSnapshot activitySnapshot) {
        activityList.clear();
        for (DataSnapshot ds : activitySnapshot.getChildren()) {
            Transaction tx = ds.getValue(Transaction.class);
            if (tx != null) activityList.add(tx);
        }
        Collections.sort(activityList, (a, b) -> Long.compare(b.timestamp, a.timestamp));
        
        if (activityList.isEmpty()) {
            tvNoActivity.setVisibility(View.VISIBLE);
            recyclerActivity.setVisibility(View.GONE);
        } else {
            tvNoActivity.setVisibility(View.GONE);
            recyclerActivity.setVisibility(View.VISIBLE);
            activityAdapter.notifyDataSetChanged();
        }
    }

    private void updateUIForClosedState(boolean isClosed) {
        if (isClosed) {
            btnWithdraw.setEnabled(false);
            btnDeposit.setEnabled(false);
            btnMore.setEnabled(false);
            
            btnWithdraw.setAlpha(0.3f);
            btnDeposit.setAlpha(0.3f);
            btnMore.setAlpha(0.3f);
        } else {
            btnWithdraw.setEnabled(true);
            btnDeposit.setEnabled(true);
            btnMore.setEnabled(true);
            
            btnWithdraw.setAlpha(1.0f);
            btnDeposit.setAlpha(1.0f);
            btnMore.setAlpha(1.0f);
        }
    }

    private void loadMainBalance() {
        mUserRef.child("balance").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    mainBalance = Double.parseDouble(snapshot.getValue().toString());
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showMoreMenu(View v) {
        if (currentPocket == null || currentPocket.isClosed) return;
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add(currentPocket.isLocked ? "Unlock" : "Lock");
        popup.getMenu().add("Edit");
        popup.getMenu().add("Delete");

        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.equals("Lock") || title.equals("Unlock")) {
                mUserRef.child("pockets").child(pocketId).child("isLocked").setValue(!currentPocket.isLocked);
            } else if (title.equals("Edit")) {
                // Future rename logic
            } else if (title.equals("Delete")) {
                confirmDelete();
            }
            return true;
        });
        popup.show();
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Pocket")
                .setMessage("Are you sure? All funds will be returned to your main balance.")
                .setPositiveButton("Delete", (d, w) -> {
                    mUserRef.child("balance").setValue(mainBalance + currentPocket.amount);
                    mUserRef.child("pockets").child(pocketId).child("isClosed").setValue(true);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private class ActivityAdapter extends RecyclerView.Adapter<ActivityAdapter.ViewHolder> {
        List<Transaction> list;
        ActivityAdapter(List<Transaction> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transaction tx = list.get(position);
            
            holder.name.setText("deposit".equals(tx.type) ? "Deposit to pocket" : "Withdraw from pocket");
            holder.date.setText(new SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(new Date(tx.timestamp)));
            holder.amount.setText(String.format("₪%.2f", tx.amount));
            
            // Amount color: Always black/white (amount_text)
            holder.amount.setTextColor(ContextCompat.getColor(PocketDetailsActivity.this, R.color.amount_text));
            
            // Icon Logic: Consistent with HomeFragment (received vs sent)
            holder.icon.setColorFilter(ContextCompat.getColor(PocketDetailsActivity.this, R.color.tx_arrow_color));
            if ("withdraw".equals(tx.type)) {
                // Withdrawal from pocket = Money OUT (Sent)
                holder.icon.setImageResource(R.drawable.ic_arrow_send);
                holder.icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(PocketDetailsActivity.this, R.color.bg_tx_sent)));
                holder.icon.setRotation(0); // Ensure no rotation if ic_arrow_send is already correct
            } else {
                // Deposit to pocket = Money IN (Received)
                holder.icon.setImageResource(R.drawable.ic_arrow_request);
                holder.icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(PocketDetailsActivity.this, R.color.bg_tx_received)));
                holder.icon.setRotation(0);
            }

            holder.profileContainer.setVisibility(View.GONE);
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, date, amount;
            ImageView icon;
            View profileContainer;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvTxName);
                date = v.findViewById(R.id.tvTxDate);
                amount = v.findViewById(R.id.tvTxAmount);
                icon = v.findViewById(R.id.imgTxIcon);
                profileContainer = v.findViewById(R.id.layoutProfileContainer);
            }
        }
    }
}