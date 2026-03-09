package com.example.digitalwallet.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

import com.example.digitalwallet.Utils.GeminiManager;
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
    
    // Conclusive Proposal Tracking
    private String lastAnalyzedUserTxId = "NONE"; 

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
        String latestUserTxId = "";
        
        for (DataSnapshot ds : txSnapshot.getChildren()) {
            Transaction tx = ds.getValue(Transaction.class);
            if (tx != null && "completed".equals(tx.status)) {
                allTransactions.add(tx);
                // We track the latest transaction that was NOT generated by the AI system
                if (!"AI_SYSTEM".equals(tx.relatedUserUid)) {
                    latestUserTxId = tx.id;
                }
            }
        }
        updateChart();
        
        // Conclusive Logic: Only run AI analysis if a new user-initiated transaction has occurred.
        // This prevents the AI from proposing something new immediately after an AI action is taken.
        if (!latestUserTxId.isEmpty() && !latestUserTxId.equals(lastAnalyzedUserTxId)) {
            lastAnalyzedUserTxId = latestUserTxId;
            runRealGeminiAnalysis();
        }
    }

    private void runRealGeminiAnalysis() {
        if (allTransactions.isEmpty() || currentUser == null) return;

        pbAiLoading.setVisibility(View.VISIBLE);
        tvAiInsights.setText("AI Financial Analyst is analyzing your patterns...");

        // Prepare data for Gemini
        StringBuilder historyBuilder = new StringBuilder();
        historyBuilder.append("User Balance: ₪").append(currentUser.balance).append("\n");
        historyBuilder.append("Recent Transactions:\n");
        
        // Take last 15 transactions for context
        int count = 0;
        for (int i = allTransactions.size() - 1; i >= 0 && count < 15; i--) {
            Transaction tx = allTransactions.get(i);
            historyBuilder.append("- ")
                    .append(tx.type).append(": ₪")
                    .append(tx.amount).append(" (")
                    .append(tx.relatedUserName).append("), Description: ")
                    .append(tx.description != null ? tx.description : "None").append("\n");
            count++;
        }

        String prompt = "You are a professional Financial Analyst for a Digital Wallet app. " +
                "Analyze the following user financial data and provide a concise (2-3 sentences) insight " +
                "about their spending habits or financial health. " +
                "THEN, if you see an opportunity to improve their finances, suggest ONE CONCLUSIVE action " +
                "from this list: SAVINGS_TRANSFER (if they have idle money), LOCK_INSURANCE (if they are overspending), " +
                "or BANK_WITHDRAW (if balance is very high). " +
                "If you have already suggested an action for this specific state, or if nothing is urgently needed, return NONE." +
                "\n\nFORMAT YOUR RESPONSE EXACTLY AS A JSON OBJECT:\n" +
                "{\n" +
                "  \"insight\": \"Your text insight here...\",\n" +
                "  \"proposal_type\": \"SAVINGS_TRANSFER\" | \"LOCK_INSURANCE\" | \"BANK_WITHDRAW\" | \"NONE\",\n" +
                "  \"proposal_title\": \"Brief title\",\n" +
                "  \"proposal_desc\": \"Brief description\",\n" +
                "  \"amount\": 123.45\n" +
                "}\n\n" +
                "USER DATA:\n" + historyBuilder.toString();

        GeminiManager.getInstance().sendText(prompt, getContext(), new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    pbAiLoading.setVisibility(View.GONE);
                    try {
                        // Clean markdown if AI included it
                        String cleanedResult = result.replace("```json", "").replace("```", "").trim();
                        JSONObject json = new JSONObject(cleanedResult);
                        
                        String insight = json.getString("insight");
                        tvAiInsights.setText(insight);

                        String type = json.getString("proposal_type");
                        if (!"NONE".equals(type)) {
                            pendingActionType = type;
                            pendingAmount = json.getDouble("amount");
                            pendingPocketName = type.equals("SAVINGS_TRANSFER") ? "Smart Savings" : "Emergency Shield";
                            
                            showProposal(json.getString("proposal_title"), json.getString("proposal_desc"));
                        } else {
                            cardAiProposal.setVisibility(View.GONE);
                        }
                    } catch (Exception e) {
                        Log.e("Gemini", "Error parsing AI response: " + result, e);
                        tvAiInsights.setText("Analysis complete. Keep up the good work!");
                    }
                });
            }

            @Override
            public void onError(Throwable error) {
                if (!isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    pbAiLoading.setVisibility(View.GONE);
                    tvAiInsights.setText("AI Analyst is currently unavailable. Check back later.");
                    Log.e("Gemini", "AI Error", error);
                });
            }
        });
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
                "AI_SYSTEM", "AI Optimization", "#007AFF", uid, "AI Optimization: " + pendingPocketName);
            updates.put("transactions/" + txId, tx);

        } else if ("BANK_WITHDRAW".equals(pendingActionType)) {
            // Withdraw to Bank
            updates.put("balance", currentUser.balance - pendingAmount);
            updates.put("bankBalance", currentUser.bankBalance + pendingAmount);
            
            String txId = UUID.randomUUID().toString();
            Transaction tx = new Transaction(txId, "sent", "completed", pendingAmount, System.currentTimeMillis(), 
                "BANK", "Bank Transfer", "#000000", uid, "Bank Transfer for Security");
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
