package com.example.digitalwallet.Fragments;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.digitalwallet.ContactDetailsActivity;
import com.example.digitalwallet.HistoryActivity;
import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.R;
import com.example.digitalwallet.RequestActivity;
import com.example.digitalwallet.SendActivity;
import com.example.digitalwallet.Utils.ProfileUtils;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class HomeFragment extends Fragment {

    private TextView tvMainBalance, tvStickyBalance, tvCurrencySymbol;
    private View stickyHeader, balanceContainer;
    private LinearLayout recentContainer, mostActiveContainer;
    private DatabaseReference mUserRef;
    private DatabaseReference mRootRef;
    private String myUid;
    private double myCurrentBalance = 0;
    
    private final List<Transaction> recentTransactions = new ArrayList<>();
    private final Set<String> displayedMostActiveUids = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        mRootRef = FirebaseDatabase.getInstance().getReference();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mUserRef = mRootRef.child("Users").child(myUid);
        }

        tvMainBalance = view.findViewById(R.id.tvMainBalance);
        tvStickyBalance = view.findViewById(R.id.tvStickyBalance);
        tvCurrencySymbol = view.findViewById(R.id.tvCurrencySymbol);
        stickyHeader = view.findViewById(R.id.stickyHeader);
        balanceContainer = view.findViewById(R.id.balanceContainer);
        ImageButton btnDeposit = view.findViewById(R.id.btnDeposit);
        recentContainer = view.findViewById(R.id.recentActivityContainer);
        mostActiveContainer = view.findViewById(R.id.mostActiveContainer);

        tvCurrencySymbol.setVisibility(View.GONE);
        tvMainBalance.setText("Loading...");

        NestedScrollView scrollView = view.findViewById(R.id.homeScrollView);
        scrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
            int threshold = balanceContainer.getHeight();
            if (scrollY > threshold) {
                stickyHeader.animate().alpha(1f).setDuration(200).start();
            } else {
                stickyHeader.animate().alpha(0f).setDuration(200).start();
            }
        });

        btnDeposit.setOnClickListener(v -> depositDummyMoney());
        view.findViewById(R.id.btnSend).setOnClickListener(v -> startActivity(new Intent(getActivity(), SendActivity.class)));
        view.findViewById(R.id.btnRequest).setOnClickListener(v -> startActivity(new Intent(getActivity(), RequestActivity.class)));
        view.findViewById(R.id.btnSeeAllHistory).setOnClickListener(v -> startActivity(new Intent(getActivity(), HistoryActivity.class)));

        loadUserData();
        loadRecentActivity();
        loadMostActive();

        return view;
    }

    private void loadUserData() {
        if (mUserRef == null) return;
        mUserRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                if (user != null) {
                    myCurrentBalance = user.balance;
                    tvCurrencySymbol.setVisibility(View.VISIBLE);
                    tvMainBalance.setTextSize(56);
                    tvMainBalance.setText(String.format("%.2f", user.balance));
                    tvStickyBalance.setText(String.format("₪ %.2f", user.balance));
                    
                    renderTransactions();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void renderTransactions() {
        if (!isAdded() || getContext() == null) return;
        
        recentContainer.removeAllViews();
        if (recentTransactions.isEmpty()) {
            showEmptyHistory();
        } else {
            for (Transaction tx : recentTransactions) {
                addTransactionView(tx);
            }
        }
    }

    private void addTransactionView(Transaction tx) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_recent_activity, recentContainer, false);

        TextView name = view.findViewById(R.id.tvTxName);
        TextView details = view.findViewById(R.id.tvTxDate);
        TextView amount = view.findViewById(R.id.tvTxAmount);
        View profileContainer = view.findViewById(R.id.layoutProfileContainer);
        ImageView icon = view.findViewById(R.id.imgTxIcon);
        LinearLayout layoutActions = view.findViewById(R.id.layoutRequestActions);
        TextView btnAccept = view.findViewById(R.id.btnAccept);
        TextView btnDecline = view.findViewById(R.id.btnDecline);

        name.setText(tx.relatedUserName != null ? tx.relatedUserName : "Unknown");

        SimpleDateFormat sdf = new SimpleDateFormat("d.M.yy", Locale.getDefault());
        String dateStr = sdf.format(new Date(tx.timestamp));
        
        String statusText = tx.status != null ? tx.status : "completed";
        statusText = statusText.substring(0, 1).toUpperCase() + statusText.substring(1);
        details.setText(dateStr + " • " + statusText);

        amount.setText("₪" + String.format("%.2f", tx.amount));
        amount.setTextColor(ContextCompat.getColor(getContext(), R.color.amount_text));

        ProfileUtils.setProfileInitial(profileContainer, tx.relatedUserName, tx.relatedUserColor);

        icon.setColorFilter(ContextCompat.getColor(getContext(), R.color.tx_arrow_color));
        if ("sent".equals(tx.type)) {
            icon.setImageResource(R.drawable.ic_arrow_send);
            icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.bg_tx_sent)));
        } else {
            icon.setImageResource(R.drawable.ic_arrow_request);
            icon.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.bg_tx_received)));
        }

        // Action Logic for Pending Transactions
        if ("pending".equals(tx.status)) {
            if (!myUid.equals(tx.initiatorUid)) {
                // I am the receiver of a Send OR the target of a Request
                layoutActions.setVisibility(View.VISIBLE);
                
                if ("sent".equals(tx.type)) {
                    // This means I was REQUESTED money from. I must pay.
                    boolean canAccept = myCurrentBalance >= tx.amount;
                    if (canAccept) {
                        btnAccept.setAlpha(1.0f);
                        btnAccept.setOnClickListener(v -> handleRequest(tx, true));
                    } else {
                        btnAccept.setAlpha(0.3f);
                        btnAccept.setOnClickListener(v -> Toast.makeText(getContext(), "Insufficient funds to pay", Toast.LENGTH_SHORT).show());
                    }
                    btnAccept.setText("Pay");
                } else {
                    // This means I RECEIVED money (it's in limbo). I must accept.
                    btnAccept.setAlpha(1.0f);
                    btnAccept.setOnClickListener(v -> handleRequest(tx, true));
                    btnAccept.setText("Accept");
                }

                btnDecline.setOnClickListener(v -> handleRequest(tx, false));
            } else {
                // I am the initiator. I am waiting for the other person.
                details.setText(dateStr + " • Waiting...");
            }
        }

        recentContainer.addView(view);
        View line = new View(getContext());
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        line.setBackgroundColor(ContextCompat.getColor(getContext(), R.color.separator));
        recentContainer.addView(line);
    }

    private void handleRequest(Transaction tx, boolean accept) {
        Map<String, Object> updates = new HashMap<>();
        String status = accept ? "completed" : "declined";

        mRootRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot root) {
                DataSnapshot myUserSnap = root.child("Users").child(myUid);
                DataSnapshot otherUserSnap = root.child("Users").child(tx.relatedUserUid);
                
                if (!myUserSnap.exists() || !otherUserSnap.exists()) return;

                double myBal = Double.parseDouble(myUserSnap.child("balance").getValue().toString());
                double otherBal = Double.parseDouble(otherUserSnap.child("balance").getValue().toString());

                if (accept) {
                    if ("sent".equals(tx.type)) {
                        // I was requested money. I must pay now.
                        if (myBal < tx.amount) {
                            Toast.makeText(getContext(), "Insufficient funds", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        updates.put("Users/" + myUid + "/balance", myBal - tx.amount);
                        updates.put("Users/" + tx.relatedUserUid + "/balance", otherBal + tx.amount);
                    } else {
                        // I received money from limbo. Sender already paid.
                        updates.put("Users/" + myUid + "/balance", myBal + tx.amount);
                    }
                } else {
                    // Declined
                    if ("received".equals(tx.type)) {
                        // I declined money sent to me. Return it to the sender.
                        updates.put("Users/" + tx.relatedUserUid + "/balance", otherBal + tx.amount);
                    }
                    // If it was a request and I decline, no balance changes needed.
                }
                finalizeRequestUpdate(tx, status, updates);
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void finalizeRequestUpdate(Transaction tx, String status, Map<String, Object> updates) {
        updates.put("Users/" + myUid + "/transactions/" + tx.id + "/status", status);
        updates.put("Users/" + tx.relatedUserUid + "/transactions/" + tx.id + "/status", status);
        
        mRootRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String msg = status.equals("completed") ? "Transaction completed!" : "Transaction declined.";
                Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecentActivity() {
        if (mUserRef == null) return;
        Query query = mUserRef.child("transactions").orderByChild("timestamp").limitToLast(3);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                recentTransactions.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    recentTransactions.add(data.getValue(Transaction.class));
                }
                Collections.reverse(recentTransactions);
                renderTransactions();
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showEmptyHistory() {
        TextView empty = new TextView(getContext());
        empty.setText("No recent transactions");
        empty.setTextColor(ContextCompat.getColor(getContext(), R.color.text_gray));
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, 80, 0, 80);
        recentContainer.addView(empty);
    }

    private void loadMostActive() {
        if (mUserRef == null) return;
        mUserRef.child("frequencies").orderByValue().limitToLast(4)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || getContext() == null) return;
                        
                        mostActiveContainer.removeAllViews();
                        displayedMostActiveUids.clear();
                        
                        List<String> userIds = new ArrayList<>();
                        for (DataSnapshot data : snapshot.getChildren()) userIds.add(data.getKey());
                        Collections.reverse(userIds);
                        
                        if (userIds.isEmpty()) {
                            showEmptyMostActive();
                        } else {
                            for (String id : userIds) fetchAndAddActiveContact(id);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void showEmptyMostActive() {
        TextView empty = new TextView(getContext());
        empty.setText("No contacts yet");
        empty.setTextColor(ContextCompat.getColor(getContext(), R.color.text_gray));
        empty.setGravity(Gravity.CENTER);
        empty.setPadding(0, 80, 0, 80);
        mostActiveContainer.addView(empty);
    }

    private void fetchAndAddActiveContact(String userId) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded() || getContext() == null) return;
                        
                        if (displayedMostActiveUids.contains(userId)) return;
                        displayedMostActiveUids.add(userId);

                        User u = snapshot.getValue(User.class);
                        if (u != null) {
                            if (u.uid == null) u.uid = userId;
                            
                            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_most_active, mostActiveContainer, false);
                            View profileContainer = view.findViewById(R.id.layoutProfileContainer);
                            
                            ProfileUtils.setProfileInitial(profileContainer, u.displayName, u.profileColor);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
                            view.setLayoutParams(params);
                            ((TextView) view.findViewById(R.id.tvActiveName)).setText(u.displayName.split(" ")[0]);
                            view.setOnClickListener(v -> {
                                Intent intent = new Intent(getActivity(), ContactDetailsActivity.class);
                                intent.putExtra("contact_uid", u.uid);
                                intent.putExtra("contact_name", u.displayName);
                                intent.putExtra("contact_color", u.profileColor);
                                startActivity(intent);
                            });
                            mostActiveContainer.addView(view);
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void depositDummyMoney() {
        if (mUserRef == null) return;
        mUserRef.child("balance").get().addOnSuccessListener(snapshot -> {
            double current = 0;
            if (snapshot.exists()) current = Double.parseDouble(Objects.requireNonNull(snapshot.getValue()).toString());
            mUserRef.child("balance").setValue(current + 1000.00);
        });
    }
}
