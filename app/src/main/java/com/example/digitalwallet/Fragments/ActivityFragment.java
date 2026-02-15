package com.example.digitalwallet.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.digitalwallet.Model.Pocket;
import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ActivityFragment extends Fragment {

    private LineChart lineChart;
    private TextView tvAiInsights, rateUsd, rateEur, rateGbp;
    private ProgressBar pbAiLoading;
    private MaterialCardView cardAiProposal;
    private TextView tvProposalTitle, tvProposalDesc;
    private Button btnAcceptProposal, btnDenyProposal;
    private MaterialButtonToggleGroup toggleGroup;
    
    private List<Transaction> allTransactions = new ArrayList<>();
    private User currentUser;
    private String timeframe = "week";
    private OkHttpClient client = new OkHttpClient();

    // AI Proposal State
    private String pendingActionType = null; // "SAVINGS_TRANSFER", "LOCK_INSURANCE", "BANK_WITHDRAW"
    private double pendingAmount = 0;
    private String pendingPocketName = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        lineChart = view.findViewById(R.id.lineChart);
        tvAiInsights = view.findViewById(R.id.tvAiInsights);
        rateUsd = view.findViewById(R.id.rateUsd);
        rateEur = view.findViewById(R.id.rateEur);
        rateGbp = view.findViewById(R.id.rateGbp);
        toggleGroup = view.findViewById(R.id.toggleGroup);
        pbAiLoading = view.findViewById(R.id.pbAiLoading);

        // AI Proposal UI
        cardAiProposal = view.findViewById(R.id.cardAiProposal);
        tvProposalTitle = view.findViewById(R.id.tvProposalTitle);
        tvProposalDesc = view.findViewById(R.id.tvProposalDesc);
        btnAcceptProposal = view.findViewById(R.id.btnAcceptProposal);
        btnDenyProposal = view.findViewById(R.id.btnDenyProposal);

        setupChart();
        loadUserData();
        fetchExchangeRates();

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnDay) timeframe = "day";
                else if (checkedId == R.id.btnWeek) timeframe = "week";
                else if (checkedId == R.id.btnYear) timeframe = "year";
                updateChart();
            }
        });

        btnAcceptProposal.setOnClickListener(v -> executeAiProposal());
        btnDenyProposal.setOnClickListener(v -> cardAiProposal.setVisibility(View.GONE));

        return view;
    }

    private void setupChart() {
        lineChart.getDescription().setEnabled(false);
        lineChart.setDrawGridBackground(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setDragEnabled(true);
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(Color.GRAY);

        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#F2F2F7"));
        lineChart.getAxisLeft().setTextColor(Color.GRAY);
        lineChart.getAxisLeft().setAxisMinimum(0f);
    }

    private void loadUserData() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                currentUser = snapshot.getValue(User.class);
                
                // Load transactions after getting user
                loadTransactions(snapshot.child("transactions"));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTransactions(DataSnapshot txSnapshot) {
        allTransactions.clear();
        for (DataSnapshot ds : txSnapshot.getChildren()) {
            Transaction tx = ds.getValue(Transaction.class);
            if (tx != null && "completed".equals(tx.status)) {
                allTransactions.add(tx);
            }
        }
        updateChart();
        runDeepAiAnalysis();
    }

    private void runDeepAiAnalysis() {
        if (allTransactions.isEmpty() || currentUser == null) return;

        pbAiLoading.setVisibility(View.VISIBLE);

        // Analysis variables
        double totalSent = 0;
        double totalReceived = 0;
        long recentLimit = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L); // Last 7 days
        double recentSent = 0;

        for (Transaction tx : allTransactions) {
            if ("sent".equals(tx.type)) {
                totalSent += tx.amount;
                if (tx.timestamp > recentLimit) recentSent += tx.amount;
            } else {
                totalReceived += tx.amount;
            }
        }

        double netFlow = totalReceived - totalSent;
        double balance = currentUser.balance;

        // Complex Decision Tree (The "Brain")
        if (netFlow > 1000 && balance > 2000) {
            // Case 1: High Surplus
            suggestHighSurplusAction(balance);
        } else if (recentSent > (totalReceived / 4) && balance < 500) {
            // Case 2: Negative Trend / Low Balance Warning
            suggestDefensiveAction(balance);
        } else if (balance > 10000) {
            // Case 3: Idle Wealth
            suggestWealthManagement(balance);
        } else {
            tvAiInsights.setText("Your finances are looking stable. I'm monitoring your patterns for optimization opportunities.");
            cardAiProposal.setVisibility(View.GONE);
        }

        pbAiLoading.setVisibility(View.GONE);
    }

    private void suggestHighSurplusAction(double balance) {
        double amountToSave = Math.floor(balance * 0.6);
        tvAiInsights.setText("Surplus Detected: You have ₪" + String.format("%.0f", balance) + " sitting idle. I recommend moving ₪" + amountToSave + " to a new Savings Pocket to earn interest.");
        
        pendingActionType = "SAVINGS_TRANSFER";
        pendingAmount = amountToSave;
        pendingPocketName = "Smart Savings";
        
        showProposal("Optimize Your Wealth", 
            "Create 'Smart Savings' and transfer ₪" + amountToSave + " immediately?");
    }

    private void suggestDefensiveAction(double balance) {
        tvAiInsights.setText("Alert: Your spending is outpacing your income this week. You need a safety net.");
        
        pendingActionType = "LOCK_INSURANCE";
        pendingAmount = balance > 100 ? balance * 0.5 : balance;
        pendingPocketName = "Emergency Shield";

        showProposal("Emergency Shield", 
            "Create a locked Emergency Pocket with ₪" + String.format("%.0f", pendingAmount) + " to prevent overspending?");
    }

    private void suggestWealthManagement(double balance) {
        double toBank = 5000;
        tvAiInsights.setText("Strategic Insight: You're holding a large balance. It's safer to keep some in your primary bank account.");

        pendingActionType = "BANK_WITHDRAW";
        pendingAmount = toBank;

        showProposal("Secure Funds", 
            "Withdraw ₪5,000 to your linked bank account for long-term security?");
    }

    private void showProposal(String title, String desc) {
        tvProposalTitle.setText(title);
        tvProposalDesc.setText(desc);
        cardAiProposal.setVisibility(View.VISIBLE);
    }

    private void executeAiProposal() {
        if (currentUser == null || pendingActionType == null) return;
        
        String uid = currentUser.uid;
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        DatabaseReference userRef = rootRef.child("Users").child(uid);
        
        Map<String, Object> updates = new HashMap<>();

        if ("SAVINGS_TRANSFER".equals(pendingActionType) || "LOCK_INSURANCE".equals(pendingActionType)) {
            // Create Pocket + Transfer Funds
            String pocketId = UUID.randomUUID().toString();
            Pocket p = new Pocket(pocketId, pendingPocketName, pendingAmount, "Savings", "LOCK_INSURANCE".equals(pendingActionType));
            p.iconName = "ic_pocket_default";
            
            updates.put("pockets/" + pocketId, p);
            updates.put("balance", currentUser.balance - pendingAmount);
            
            // Log as transaction
            String txId = UUID.randomUUID().toString();
            Transaction tx = new Transaction(txId, "sent", "completed", pendingAmount, System.currentTimeMillis(), 
                "AI_SYSTEM", "AI Optimization", "#007AFF", uid);
            updates.put("transactions/" + txId, tx);

        } else if ("BANK_WITHDRAW".equals(pendingActionType)) {
            // Withdraw to Bank
            updates.put("balance", currentUser.balance - pendingAmount);
            updates.put("bankBalance", currentUser.bankBalance + pendingAmount);
            
            String txId = UUID.randomUUID().toString();
            Transaction tx = new Transaction(txId, "sent", "completed", pendingAmount, System.currentTimeMillis(), 
                "BANK", "Bank Transfer", "#000000", uid);
            updates.put("transactions/" + txId, tx);
        }

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "AI optimization executed successfully!", Toast.LENGTH_LONG).show();
                cardAiProposal.setVisibility(View.GONE);
                tvAiInsights.setText("Action complete. Your financial structure has been optimized based on the plan.");
            }
        });
    }

    private void updateChart() {
        if (!isAdded()) return;

        TreeMap<Long, Double> dataPoints = new TreeMap<>();
        long startTime = getStartTime();
        
        prePopulateTimeline(dataPoints, startTime);

        for (Transaction tx : allTransactions) {
            if (tx.timestamp >= startTime) {
                long key = getNormalizedTime(tx.timestamp);
                double amount = "sent".equals(tx.type) ? -tx.amount : tx.amount;
                
                if (dataPoints.containsKey(key)) {
                    dataPoints.put(key, dataPoints.get(key) + amount);
                }
            }
        }

        List<Entry> entries = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        int i = 0;
        
        for (Map.Entry<Long, Double> entry : dataPoints.entrySet()) {
            entries.add(new Entry(i, entry.getValue().floatValue()));
            labels.add(formatLabel(entry.getKey()));
            i++;
        }

        if (entries.isEmpty()) {
            lineChart.clear();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Financial Flow");
        dataSet.setColor(Color.parseColor("#007AFF"));
        dataSet.setLineWidth(2.5f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#007AFF"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawFilled(false); 
        dataSet.setDrawValues(false);

        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < labels.size()) return labels.get(index);
                return "";
            }
        });

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.animateX(800);
        lineChart.invalidate();
    }

    private void prePopulateTimeline(TreeMap<Long, Double> dataPoints, long startTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(startTime);
        long now = System.currentTimeMillis();

        while (cal.getTimeInMillis() <= now) {
            dataPoints.put(getNormalizedTime(cal.getTimeInMillis()), 0.0);
            if ("day".equals(timeframe)) cal.add(Calendar.HOUR_OF_DAY, 1);
            else if ("week".equals(timeframe)) cal.add(Calendar.DAY_OF_YEAR, 1);
            else cal.add(Calendar.MONTH, 1);
        }
    }

    private long getStartTime() {
        Calendar cal = Calendar.getInstance();
        if ("day".equals(timeframe)) {
            cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0);
        } else if ("week".equals(timeframe)) {
            cal.add(Calendar.DAY_OF_YEAR, -6); cal.set(Calendar.HOUR_OF_DAY, 0);
        } else {
            cal.add(Calendar.MONTH, -11); cal.set(Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTimeInMillis();
    }

    private long getNormalizedTime(long ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts);
        cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0);
        if (!"day".equals(timeframe)) { cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0); }
        if ("year".equals(timeframe)) { cal.set(Calendar.DAY_OF_MONTH, 1); }
        return cal.getTimeInMillis();
    }

    private String formatLabel(long ts) {
        if ("day".equals(timeframe)) return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts));
        if ("week".equals(timeframe)) return new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date(ts));
        return new SimpleDateFormat("MMM", Locale.getDefault()).format(new Date(ts));
    }

    private void fetchExchangeRates() {
        Request request = new Request.Builder().url("https://open.er-api.com/v6/latest/ILS").build();
        client.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(@NonNull Call call, @NonNull IOException e) {}
            @Override public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONObject rates = json.getJSONObject("rates");
                        final String usdIls = String.format("%.2f", 1 / rates.getDouble("USD"));
                        final String eurIls = String.format("%.2f", 1 / rates.getDouble("EUR"));
                        final String gbpIls = String.format("%.2f", 1 / rates.getDouble("GBP"));
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                rateUsd.setText("₪ " + usdIls); rateEur.setText("₪ " + eurIls); rateGbp.setText("₪ " + gbpIls);
                            });
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }
}
