package com.example.digitalwallet;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.digitalwallet.Model.Transaction;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private List<Transaction> fullList = new ArrayList<>();
    private List<Transaction> filteredList = new ArrayList<>();

    private EditText etSearch;
    private TextView btnAll, btnSent, btnReceived;

    // State
    private String currentFilterType = "all"; // all, sent, received
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        // UI Refs
        etSearch = findViewById(R.id.etSearch);
        btnAll = findViewById(R.id.btnFilterAll);
        btnSent = findViewById(R.id.btnFilterSent);
        btnReceived = findViewById(R.id.btnFilterReceived);

        // Setup Recycler
        recyclerView = findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        // Actions
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        // Search Logic
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Filter Buttons
        btnAll.setOnClickListener(v -> updateFilterType("all"));
        btnSent.setOnClickListener(v -> updateFilterType("sent"));
        btnReceived.setOnClickListener(v -> updateFilterType("received"));

        loadHistory();
    }

    private void updateFilterType(String type) {
        currentFilterType = type;

        // Update Visuals (Black for active, Gray for inactive)
        updateBtnVisual(btnAll, type.equals("all"));
        updateBtnVisual(btnSent, type.equals("sent"));
        updateBtnVisual(btnReceived, type.equals("received"));

        applyFilters();
    }

    private void updateBtnVisual(TextView btn, boolean isActive) {
        if (isActive) {
            btn.setBackgroundResource(R.drawable.bg_filter_chip_active);
            btn.setTextColor(Color.WHITE);
        } else {
            btn.setBackgroundResource(R.drawable.bg_filter_chip_inactive);
            btn.setTextColor(Color.BLACK);
        }
    }

    private void applyFilters() {
        filteredList.clear();

        for (Transaction tx : fullList) {
            // 1. Check Type
            boolean typeMatch = currentFilterType.equals("all") || tx.type.equals(currentFilterType);

            // 2. Check Search (Name or whatever data we have)
            // Note: Since Transaction object currently only stores "relatedUserName",
            // searching email/phone would require updating the Transaction model to store those too.
            // For now, we search the Name.
            boolean searchMatch = currentSearchQuery.isEmpty();
            if (!currentSearchQuery.isEmpty() && tx.relatedUserName != null) {
                if (tx.relatedUserName.toLowerCase().contains(currentSearchQuery)) {
                    searchMatch = true;
                }
            }

            if (typeMatch && searchMatch) {
                filteredList.add(tx);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadHistory() {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("transactions")
                .orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        fullList.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            fullList.add(data.getValue(Transaction.class));
                        }
                        Collections.reverse(fullList); // Newest first
                        applyFilters(); // Apply current filters to new data
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    // --- ADAPTER ---
    class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
        List<Transaction> list;
        HistoryAdapter(List<Transaction> list) { this.list = list; }

        @NonNull @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_recent_activity, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Transaction tx = list.get(position);
            String color = tx.relatedUserColor != null ? tx.relatedUserColor : "#E5E5EA";
            holder.name.setText(tx.relatedUserName != null ? tx.relatedUserName : "Unknown");

            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
            holder.date.setText(sdf.format(new Date(tx.timestamp)));

            holder.icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(color)));
            holder.icon.setColorFilter(Color.WHITE);

            if ("sent".equals(tx.type)) {
                holder.amount.setText("- ₪" + String.format("%.2f", tx.amount));
                holder.amount.setTextColor(Color.BLACK);
                holder.icon.setImageResource(R.drawable.ic_arrow_send);
                holder.icon.setColorFilter(Color.BLACK);
                holder.icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F2F2F7")));
            } else {
                holder.amount.setText("+ ₪" + String.format("%.2f", tx.amount));
                holder.amount.setTextColor(Color.parseColor("#34C759"));
                holder.icon.setImageResource(R.drawable.ic_arrow_request);
                holder.icon.setColorFilter(Color.parseColor("#34C759"));
                holder.icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0F8E5")));
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, date, amount;
            ImageView icon;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvTxName);
                date = v.findViewById(R.id.tvTxDate);
                amount = v.findViewById(R.id.tvTxAmount);
                icon = v.findViewById(R.id.imgTxIcon);
            }
        }
    }
}