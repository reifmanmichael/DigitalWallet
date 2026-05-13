package com.example.digitalwallet.Fragments;

import android.content.Intent;
import android.os.Bundle;
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
import com.example.digitalwallet.DepositActivity;
import com.example.digitalwallet.HistoryActivity;
import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.R;
import com.example.digitalwallet.RequestActivity;
import com.example.digitalwallet.SendActivity;
import com.example.digitalwallet.Utils.ProfileUtils;
import com.example.digitalwallet.WithdrawActivity;
import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment {

    private TextView tvMainBalance, tvStickyBalance, tvCurrencySymbol;
    private View stickyHeader, balanceContainer;
    private LinearLayout recentContainer, mostActiveContainer;
    private DatabaseReference mUserRef;
    private DatabaseReference mUsersRef;
    private String myUid;
    private double myCurrentBalance = 0;
    
    private final List<Transaction> recentTransactions = new ArrayList<>();
    private final Set<String> displayedMostActiveUids = new HashSet<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        mUsersRef = rootRef.child("Users");
        
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            mUserRef = mUsersRef.child(myUid);
        }

        tvMainBalance = view.findViewById(R.id.tvMainBalance);
        tvStickyBalance = view.findViewById(R.id.tvStickyBalance);
        tvCurrencySymbol = view.findViewById(R.id.tvCurrencySymbol);
        stickyHeader = view.findViewById(R.id.stickyHeader);
        balanceContainer = view.findViewById(R.id.balanceContainer);
        ImageButton btnDeposit = view.findViewById(R.id.btnDeposit);
        ImageButton btnMore = view.findViewById(R.id.btnMore);
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

        btnDeposit.setOnClickListener(v -> startActivity(new Intent(getActivity(), DepositActivity.class)));
        btnMore.setOnClickListener(v -> showMoreMenu());
        
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
                    tvMainBalance.setText(String.format(Locale.getDefault(), "%.2f", user.balance));
                    tvStickyBalance.setText(String.format(Locale.getDefault(), "₪ %.2f", user.balance));
                    
                    renderTransactions();
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void showMoreMenu() {
        if (getContext() == null) return;
        
        BottomSheetDialog dialog = new BottomSheetDialog(getContext());
        View view = getLayoutInflater().inflate(R.layout.dialog_home_more, null);
        dialog.setContentView(view);

        View bottomSheet = (View) view.getParent();
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
        }

        view.findViewById(R.id.btnMenuWithdraw).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(getActivity(), WithdrawActivity.class));
        });

        dialog.show();
    }

    private void loadRecentActivity() {
        if (mUserRef == null) return;

        mUserRef.child("transactions").orderByChild("timestamp").limitToLast(30)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        recentTransactions.clear();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Transaction tx = data.getValue(Transaction.class);
                            if (tx != null) {

                                if ("AI".equalsIgnoreCase(tx.relatedUserName) || "Bank".equalsIgnoreCase(tx.relatedUserName)) {
                                    continue;
                                }
                                tx.id = data.getKey();
                                recentTransactions.add(tx);
                            }
                        }
                        Collections.reverse(recentTransactions);

                        if (recentTransactions.size() > 10) {
                            List<Transaction> limited = new ArrayList<>(recentTransactions.subList(0, 10));
                            recentTransactions.clear();
                            recentTransactions.addAll(limited);
                        }
                        
                        renderTransactions();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void loadMostActive() {
        if (mUserRef == null) return;
        mUserRef.child("transactions").orderByChild("timestamp").limitToLast(100)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Map<String, Integer> counts = new HashMap<>();
                        Map<String, Transaction> lastTx = new HashMap<>();
                        for (DataSnapshot data : snapshot.getChildren()) {
                            Transaction tx = data.getValue(Transaction.class);
                            if (tx != null && tx.relatedUserUid != null) {

                                if ("AI".equalsIgnoreCase(tx.relatedUserName) || "Bank".equalsIgnoreCase(tx.relatedUserName)) {
                                    continue;
                                }
                                counts.put(tx.relatedUserUid, counts.getOrDefault(tx.relatedUserUid, 0) + 1);
                                lastTx.put(tx.relatedUserUid, tx);
                            }
                        }
                        
                        List<String> uids = new ArrayList<>(counts.keySet());
                        Collections.sort(uids, (a, b) -> counts.get(b).compareTo(counts.get(a)));
                        
                        updateMostActiveUI(uids, lastTx);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateMostActiveUI(List<String> uids, Map<String, Transaction> lastTx) {
        if (!isAdded() || getContext() == null) return;
        mostActiveContainer.removeAllViews();
        displayedMostActiveUids.clear();

        int count = 0;
        for (String uid : uids) {
            if (count >= 5) break;
            Transaction tx = lastTx.get(uid);
            if (tx != null) {
                addMostActiveView(tx);
                displayedMostActiveUids.add(uid);
                count++;
            }
        }
        
        if (count == 0) {
            showEmptyContacts();
        }
    }

    private void showEmptyContacts() {
        TextView empty = new TextView(getContext());
        empty.setText("No contacts yet");
        empty.setPadding(20, 0, 20, 0);
        empty.setTextColor(ContextCompat.getColor(getContext(), R.color.text_gray));
        mostActiveContainer.addView(empty);
    }

    private void addMostActiveView(Transaction tx) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_most_active, mostActiveContainer, false);
        View profileContainer = view.findViewById(R.id.layoutProfileContainer);
        TextView name = view.findViewById(R.id.tvActiveName);

        name.setText(tx.relatedUserName != null ? tx.relatedUserName.split(" ")[0] : "User");
        ProfileUtils.setProfileInitial(profileContainer, tx.relatedUserName, tx.relatedUserColor);

        view.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), ContactDetailsActivity.class);
            intent.putExtra("uid", tx.relatedUserUid);
            intent.putExtra("name", tx.relatedUserName);
            intent.putExtra("color", tx.relatedUserColor);
            startActivity(intent);
        });

        mostActiveContainer.addView(view);
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

    private void showEmptyHistory() {
        TextView empty = new TextView(getContext());
        empty.setText("No recent transactions");
        empty.setPadding(40, 40, 40, 40);
        empty.setTextColor(ContextCompat.getColor(getContext(), R.color.text_gray));
        recentContainer.addView(empty);
    }

    private void addTransactionView(Transaction tx) {
        View view = LayoutInflater.from(getContext()).inflate(R.layout.item_recent_activity, recentContainer, false);

        TextView name = view.findViewById(R.id.tvTxName);
        TextView details = view.findViewById(R.id.tvTxDate);
        TextView amount = view.findViewById(R.id.tvTxAmount);
        TextView description = view.findViewById(R.id.tvTxDescription);
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

        amount.setText("₪" + String.format(Locale.getDefault(), "%.2f", tx.amount));
        amount.setTextColor(ContextCompat.getColor(getContext(), R.color.amount_text));

        if (tx.description != null && !tx.description.isEmpty()) {
            description.setVisibility(View.VISIBLE);
            description.setText(tx.description);
        } else {
            description.setVisibility(View.GONE);
        }

        ProfileUtils.setProfileInitial(profileContainer, tx.relatedUserName, tx.relatedUserColor);

        icon.setColorFilter(ContextCompat.getColor(getContext(), R.color.tx_arrow_color));
        if ("sent".equals(tx.type)) {
            icon.setImageResource(R.drawable.ic_arrow_send);
            icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.bg_tx_sent)));
        } else {
            icon.setImageResource(R.drawable.ic_arrow_request);
            icon.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(getContext(), R.color.bg_tx_received)));
        }

        if ("pending".equals(tx.status)) {
            if (!myUid.equals(tx.initiatorUid)) {
                layoutActions.setVisibility(View.VISIBLE);
                
                if ("sent".equals(tx.type)) {
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
                    btnAccept.setAlpha(1.0f);
                    btnAccept.setOnClickListener(v -> handleRequest(tx, true));
                    btnAccept.setText("Accept");
                }

                btnDecline.setOnClickListener(v -> handleRequest(tx, false));
            } else {
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
        final Map<String, Object> updates = new HashMap<>();
        final String status = accept ? "completed" : "declined";

        mUsersRef.child(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot mySnap) {
                mUsersRef.child(tx.relatedUserUid).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot otherSnap) {
                        if (!mySnap.exists() || !otherSnap.exists()) return;

                        Object myBalObj = mySnap.child("balance").getValue();
                        Object otherBalObj = otherSnap.child("balance").getValue();
                        
                        if (myBalObj == null || otherBalObj == null) return;

                        double myBal = Double.parseDouble(myBalObj.toString());
                        double otherBal = Double.parseDouble(otherBalObj.toString());

                        if (accept) {
                            if ("sent".equals(tx.type)) {
                                if (myBal < tx.amount) {
                                    Toast.makeText(getContext(), "Insufficient funds", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                updates.put("Users/" + myUid + "/balance", myBal - tx.amount);
                                updates.put("Users/" + tx.relatedUserUid + "/balance", otherBal + tx.amount);
                            } else {
                                if (otherBal < tx.amount) {
                                    Toast.makeText(getContext(), "Requester has insufficient funds", Toast.LENGTH_SHORT).show();
                                    return;
                                }
                                updates.put("Users/" + myUid + "/balance", myBal + tx.amount);
                                updates.put("Users/" + tx.relatedUserUid + "/balance", otherBal - tx.amount);
                            }
                        }

                        updates.put("Users/" + myUid + "/transactions/" + tx.id + "/status", status);
                        updates.put("Users/" + tx.relatedUserUid + "/transactions/" + tx.id + "/status", status);

                        FirebaseDatabase.getInstance().getReference().updateChildren(updates)
                                .addOnSuccessListener(aVoid -> {
                                    String msg = accept ? "Transaction completed" : "Transaction declined";
                                    Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                                });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
