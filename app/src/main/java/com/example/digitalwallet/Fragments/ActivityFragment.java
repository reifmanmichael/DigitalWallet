package com.example.digitalwallet.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.digitalwallet.Utils.ChartMarkerView;
import com.example.digitalwallet.Utils.GeminiManager;
import com.example.digitalwallet.Model.Pocket;
import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.Model.User;
import com.example.digitalwallet.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
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
    
    private final List<Transaction> allTransactions = new ArrayList<>();
    private User currentUser;
    private String timeframe = "week";
    private final OkHttpClient client = new OkHttpClient();
    private final List<String> currentChartLabels = new ArrayList<>();

    // AI Proposal State
    private String pendingActionType = null;
    private double pendingAmount = 0;
    private String pendingPocketName = null;
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
        lineChart.setExtraOffsets(10, 10, 10, 20);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.text_gray));
        xAxis.setTextSize(11f);
        xAxis.setYOffset(15f);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#E5E5EA"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(ContextCompat.getColor(getContext(), R.color.text_gray));
        leftAxis.setTextSize(11f);
        leftAxis.setXOffset(10f);
        leftAxis.setSpaceTop(20f);
        
        lineChart.getLegend().setEnabled(false);
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
                if (!"AI_SYSTEM".equals(tx.relatedUserUid)) {
                    latestUserTxId = tx.id;
                }
            }
        }
        updateChart();
        
        if (!latestUserTxId.isEmpty() && !latestUserTxId.equals(lastAnalyzedUserTxId)) {
            lastAnalyzedUserTxId = latestUserTxId;
            runRealGeminiAnalysis();
        }
    }

    private void runRealGeminiAnalysis() {
        if (allTransactions.isEmpty() || currentUser == null) return;

        pbAiLoading.setVisibility(View.VISIBLE);
        tvAiInsights.setText("AI Financial Analyst is analyzing your patterns...");

        StringBuilder historyBuilder = new StringBuilder();
        historyBuilder.append("User Balance: ₪").append(currentUser.balance).append("\n");
        historyBuilder.append("Recent Transactions:\n");
        
        int count = 0;
        for (int i = allTransactions.size() - 1; i >= 0 && count < 15; i--) {
            Transaction tx = allTransactions.get(i);
            historyBuilder.append("- ").append(tx.type).append(": ₪").append(tx.amount).append(" (").append(tx.relatedUserName).append(")\n");
            count++;
        }

        String prompt = "Analyze user financial data. Return a JSON object with: insight, proposal_type, proposal_title, proposal_desc, amount. DATA:\n" + historyBuilder.toString();

        GeminiManager.getInstance().sendText(prompt, getContext(), new GeminiManager.GeminiCallback() {
            @Override
            public void onSuccess(String result) {
                if (!isAdded()) return;
                getActivity().runOnUiThread(() -> {
                    pbAiLoading.setVisibility(View.GONE);
                    try {
                        String cleanedResult = result.replace("```json", "").replace("```", "").trim();
                        JSONObject json = new JSONObject(cleanedResult);
                        tvAiInsights.setText(json.getString("insight"));
                        String type = json.getString("proposal_type");
                        if (!"NONE".equals(type)) {
                            pendingActionType = type;
                            pendingAmount = json.getDouble("amount");
                            pendingPocketName = type.equals("SAVINGS_TRANSFER") ? "Smart Savings" : "Emergency Shield";
                            showProposal(json.getString("proposal_title"), json.getString("proposal_desc"));
                        } else cardAiProposal.setVisibility(View.GONE);
                    } catch (Exception e) {
                        tvAiInsights.setText("Analysis complete. Looking good!");
                    }
                });
            }

            @Override
            public void onError() {

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
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
        Map<String, Object> updates = new HashMap<>();

        if ("SAVINGS_TRANSFER".equals(pendingActionType) || "LOCK_INSURANCE".equals(pendingActionType)) {
            String pocketId = UUID.randomUUID().toString();
            Pocket p = new Pocket(pocketId, pendingPocketName, pendingAmount, "Savings", "LOCK_INSURANCE".equals(pendingActionType));
            updates.put("pockets/" + pocketId, p);
            updates.put("balance", currentUser.balance - pendingAmount);
        }

        userRef.updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                cardAiProposal.setVisibility(View.GONE);
                tvAiInsights.setText("Action complete. Finances optimized.");
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
        currentChartLabels.clear();
        int i = 0;
        
        for (Map.Entry<Long, Double> entry : dataPoints.entrySet()) {
            entries.add(new Entry(i, entry.getValue().floatValue()));
            currentChartLabels.add(formatLabel(entry.getKey()));
            i++;
        }

        if (entries.isEmpty()) {
            lineChart.clear();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Net Flow");
        
        // --- PREMIUM STYLING ---
        int primaryColor = ContextCompat.getColor(getContext(), R.color.primary_blue);
        dataSet.setColor(primaryColor);
        dataSet.setLineWidth(3f);
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(primaryColor);
        dataSet.setCircleRadius(5f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleHoleRadius(2.5f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);
        
        // Smooth gradient fill
        dataSet.setDrawFilled(true);
        int[] gradientColors = { Color.argb(100, 0, 122, 255), Color.argb(0, 0, 122, 255) };
        dataSet.setFillDrawable(new LinearGradientDrawable(gradientColors));

        // Highlighting
        dataSet.setHighlightEnabled(true);
        dataSet.setDrawHighlightIndicators(true);
        dataSet.setHighLightColor(primaryColor);
        dataSet.setHighlightLineWidth(1f);

        // Marker view for interaction
        ChartMarkerView marker = new ChartMarkerView(getContext(), R.layout.layout_chart_marker, currentChartLabels);
        lineChart.setMarker(marker);

        lineChart.getXAxis().setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                int index = (int) value;
                if (index >= 0 && index < currentChartLabels.size()) return currentChartLabels.get(index);
                return "";
            }
        });

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.animateY(1000);
        lineChart.invalidate();
    }

    private static class LinearGradientDrawable extends android.graphics.drawable.GradientDrawable {
        public LinearGradientDrawable(int[] colors) {
            super(Orientation.TOP_BOTTOM, colors);
        }
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
