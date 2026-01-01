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
import com.example.digitalwallet.Utils.ProfileUtils;
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

    private String currentFilterType = "all";
    private String currentSearchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        etSearch = findViewById(R.id.etSearch);
        btnAll = findViewById(R.id.btnFilterAll);
        btnSent = findViewById(R.id.btnFilterSent);
        btnReceived = findViewById(R.id.btnFilterReceived);

        recyclerView = findViewById(R.id.recyclerHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(filteredList);
        recyclerView.setAdapter(adapter);

        findViewById(R.id.btnClose).setOnClickListener(v -> finish());

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnAll.setOnClickListener(v -> updateFilterType("all"));
        btnSent.setOnClickListener(v -> updateFilterType("sent"));
        btnReceived.setOnClickListener(v -> updateFilterType("received"));

        loadHistory();
    }

    private void updateFilterType(String type) {
        currentFilterType = type;
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
            boolean typeMatch = currentFilterType.equals("all") || tx.type.equals(currentFilterType);
            boolean searchMatch = currentSearchQuery.isEmpty() || (tx.relatedUserName != null && tx.relatedUserName.toLowerCase().contains(currentSearchQuery));

            if (typeMatch && searchMatch) {
                filteredList.add(tx);
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void loadHistory() {
        if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
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
                        Collections.reverse(fullList);
                        applyFilters();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

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
            holder.name.setText(tx.relatedUserName != null ? tx.relatedUserName : "Unknown");

            SimpleDateFormat sdf = new SimpleDateFormat("d.M.yy", Locale.getDefault());
            String dateStr = sdf.format(new Date(tx.timestamp));
            String status = tx.status != null ? tx.status : "completed";
            status = status.substring(0, 1).toUpperCase() + status.substring(1);
            holder.details.setText(dateStr + " • " + status);

            holder.amount.setText("₪" + String.format("%.2f", tx.amount));
            holder.amount.setTextColor(Color.BLACK);

            ProfileUtils.setProfileInitial(holder.profileContainer, tx.relatedUserName, tx.relatedUserColor);

            // COLOR CODED ICON
            holder.icon.setColorFilter(Color.BLACK);
            if ("sent".equals(tx.type)) {
                holder.icon.setImageResource(R.drawable.ic_arrow_send);
                holder.icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFE5E5"))); // Light Red
            } else {
                holder.icon.setImageResource(R.drawable.ic_arrow_request);
                holder.icon.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E5F9E5"))); // Light Green
            }
        }

        @Override public int getItemCount() { return list.size(); }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView name, details, amount;
            View profileContainer;
            ImageView icon;
            ViewHolder(View v) {
                super(v);
                name = v.findViewById(R.id.tvTxName);
                details = v.findViewById(R.id.tvTxDate);
                amount = v.findViewById(R.id.tvTxAmount);
                profileContainer = v.findViewById(R.id.layoutProfileContainer);
                icon = v.findViewById(R.id.imgTxIcon);
            }
        }
    }
}