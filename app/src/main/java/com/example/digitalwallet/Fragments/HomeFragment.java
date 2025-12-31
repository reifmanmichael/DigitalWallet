package com.example.digitalwallet.Fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.digitalwallet.HistoryActivity;
import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.R;
import com.example.digitalwallet.SendActivity;
import com.example.digitalwallet.Transfers.TransferAmountActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private TextView tvMainBalance, tvStickyBalance, tvCurrencySymbol;
    private View stickyHeader, balanceContainer;
    private LinearLayout recentContainer, mostActiveContainer;
    private DatabaseReference mDatabase;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mDatabase = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        }

        // Views
        tvMainBalance = view.findViewById(R.id.tvMainBalance);
        tvStickyBalance = view.findViewById(R.id.tvStickyBalance);
        tvCurrencySymbol = view.findViewById(R.id.tvCurrencySymbol);
        stickyHeader = view.findViewById(R.id.stickyHeader);
        balanceContainer = view.findViewById(R.id.balanceContainer);
        ImageButton btnDeposit = view.findViewById(R.id.btnDeposit);
        recentContainer = view.findViewById(R.id.recentActivityContainer);
        mostActiveContainer = view.findViewById(R.id.mostActiveContainer);

        // Initial State
        tvCurrencySymbol.setVisibility(View.GONE);
        tvMainBalance.setText("Loading...");
        tvMainBalance.setTextSize(32);

        // Sticky Header Logic
        NestedScrollView scrollView = view.findViewById(R.id.homeScrollView);
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            int threshold = balanceContainer.getHeight();
            if (scrollY > threshold) {
                stickyHeader.animate().alpha(1f).setDuration(200).start();
            } else {
                stickyHeader.animate().alpha(0f).setDuration(200).start();
            }
        });

        // Buttons
        btnDeposit.setOnClickListener(v -> depositDummyMoney());
        view.findViewById(R.id.btnSend).setOnClickListener(v -> startActivity(new Intent(getActivity(), SendActivity.class)));

        // "See All" History
        view.findViewById(R.id.btnSeeAllHistory).setOnClickListener(v -> startActivity(new Intent(getActivity(), HistoryActivity.class)));

        // Load Data
        loadUserData();
        loadRecentActivity();
        loadMostActive();

        return view;
    }

    private void loadUserData() {
        if (mDatabase == null) return;
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    tvCurrencySymbol.setVisibility(View.VISIBLE);
                    tvMainBalance.setTextSize(56);
                    tvMainBalance.setText(String.format("%.2f", user.balance));
                    tvStickyBalance.setText(String.format("₪ %.2f", user.balance));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void addTransactionView(Transaction tx) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_recent_activity, recentContainer, false);

        TextView name = view.findViewById(R.id.tvTxName);
        TextView date = view.findViewById(R.id.tvTxDate);
        TextView amount = view.findViewById(R.id.tvTxAmount);
        ImageView icon = view.findViewById(R.id.imgTxIcon);

        name.setText(tx.relatedUserName != null ? tx.relatedUserName : "Unknown");

        // Date Format
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
        date.setText(sdf.format(new Date(tx.timestamp)));

        if ("sent".equals(tx.type)) {
            amount.setText("- ₪" + String.format("%.2f", tx.amount));
            amount.setTextColor(Color.BLACK);
            icon.setImageResource(R.drawable.ic_arrow_send);
            icon.setColorFilter(Color.BLACK);
            icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F2F2F7")));
        } else {
            amount.setText("+ ₪" + String.format("%.2f", tx.amount));
            amount.setTextColor(Color.parseColor("#34C759")); // Green
            icon.setImageResource(R.drawable.ic_arrow_request); // Or a down arrow
            icon.setColorFilter(Color.parseColor("#34C759"));
            icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0F8E5")));
        }

        recentContainer.addView(view);
        // Add separator
        View line = new View(getContext());
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        line.setBackgroundColor(Color.parseColor("#F2F2F7"));
        recentContainer.addView(line);
    }

    private void loadRecentActivity() {
        if (mDatabase == null) return;

        Query query = mDatabase.child("transactions").orderByChild("timestamp").limitToLast(5);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // SAFETY CHECK: Ensure Fragment is still alive and attached
                if (!isAdded() || getContext() == null) return;

                recentContainer.removeAllViews();
                List<Transaction> list = new ArrayList<>();

                for (DataSnapshot data : snapshot.getChildren()) {
                    list.add(data.getValue(Transaction.class));
                }
                Collections.reverse(list);

                if (list.isEmpty()) {
                    TextView empty = new TextView(getContext());
                    empty.setText("No recent transactions");
                    empty.setTextColor(Color.GRAY);
                    empty.setTextSize(14);
                    empty.setGravity(Gravity.CENTER);
                    empty.setPadding(0, 80, 0, 80);

                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT);
                    empty.setLayoutParams(params);

                    recentContainer.addView(empty);
                } else {
                    for (Transaction tx : list) {
                        addTransactionView(tx);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadMostActive() {
        if (mDatabase == null) return;

        mDatabase.child("frequencies").orderByValue().limitToLast(4)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        // SAFETY CHECK: Ensure Fragment is still alive
                        if (!isAdded() || getContext() == null) return;

                        mostActiveContainer.removeAllViews();
                        List<String> userIds = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            userIds.add(data.getKey());
                        }
                        Collections.reverse(userIds);

                        if (userIds.isEmpty()) {
                            TextView empty = new TextView(getContext());
                            empty.setText("No contacts yet");
                            empty.setTextColor(Color.GRAY);
                            empty.setTextSize(14);
                            empty.setGravity(Gravity.CENTER);
                            empty.setPadding(0, 80, 0, 80);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.WRAP_CONTENT);
                            empty.setLayoutParams(params);

                            mostActiveContainer.addView(empty);
                        } else {
                            for (String id : userIds) {
                                fetchAndAddActiveContact(id);
                            }
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void fetchAndAddActiveContact(String userId) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || getContext() == null) return;

                        User u = snapshot.getValue(User.class);
                        if (u != null) {
                            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_most_active, mostActiveContainer, false);
                            ImageView img = view.findViewById(R.id.imgActiveProfile);

                            String color = u.profileColor != null ? u.profileColor : "#E5E5EA";
                            img.setColorFilter(Color.parseColor(color));

                            // --- FIX: Distribute evenly (Weight = 1) ---
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    0,
                                    ViewGroup.LayoutParams.WRAP_CONTENT,
                                    1.0f
                            );
                            view.setLayoutParams(params);
                            // -------------------------------------------

                            TextView name = view.findViewById(R.id.tvActiveName);
                            name.setText(u.displayName.split(" ")[0]);

                            view.setOnClickListener(v -> {
                                Intent intent = new Intent(getActivity(), TransferAmountActivity.class);
                                intent.putExtra("recipient_uid", u.uid);
                                intent.putExtra("recipient_name", u.displayName);
                                startActivity(intent);
                            });
                            mostActiveContainer.addView(view);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void depositDummyMoney() {
        if (mDatabase == null) return;
        mDatabase.child("balance").get().addOnSuccessListener(snapshot -> {
            double current = 0;
            if (snapshot.exists()) current = Double.parseDouble(Objects.requireNonNull(snapshot.getValue()).toString());
            mDatabase.child("balance").setValue(current + 1000.00);
        });
    }
}