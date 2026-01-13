package com.example.digitalwallet.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.digitalwallet.Model.Transaction;
import com.example.digitalwallet.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.json.JSONObject;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ActivityFragment extends Fragment {

    private LineChart lineChart;
    private TextView tvAiInsights, rateUsd, rateEur, rateGbp;
    private MaterialButtonToggleGroup toggleGroup;
    private List<Transaction> allTransactions = new ArrayList<>();
    private String timeframe = "week"; 

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

        setupChart();
        loadTransactions();
        fetchExchangeRates();

        toggleGroup.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btnDay) timeframe = "day";
                else if (checkedId == R.id.btnWeek) timeframe = "week";
                else if (checkedId == R.id.btnYear) timeframe = "year";
                updateChart();
            }
        });

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
        lineChart.getAxisLeft().setAxisMinimum(0f); // Always show from 0
    }

    private void loadTransactions() {
        String uid = FirebaseAuth.getInstance().getUid();
        if (uid == null) return;

        FirebaseDatabase.getInstance().getReference("Users").child(uid).child("transactions")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        allTransactions.clear();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            Transaction tx = ds.getValue(Transaction.class);
                            if (tx != null && "completed".equals(tx.status)) {
                                allTransactions.add(tx);
                            }
                        }
                        updateChart();
                        generateAiInsights();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void updateChart() {
        if (!isAdded()) return;

        TreeMap<Long, Double> dataPoints = new TreeMap<>();
        long startTime = getStartTime();
        
        // --- FIX: Pre-populate the timeline with zeros to ensure a continuous line ---
        prePopulateTimeline(dataPoints, startTime);

        for (Transaction tx : allTransactions) {
            if (tx.timestamp >= startTime) {
                long key = getNormalizedTime(tx.timestamp);
                // We show absolute spending/receiving volume or balance flow? 
                // Let's show positive flow for received, negative for sent.
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
        
        // --- FIX: Remove the full fill to keep it as a clean line ---
        dataSet.setDrawFilled(false); 
        
        dataSet.setDrawValues(false);
        dataSet.setHighLightColor(Color.parseColor("#007AFF"));

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
        lineChart.animateX(800); // Horizontal animate for flow
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
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        } else if ("week".equals(timeframe)) {
            cal.add(Calendar.DAY_OF_YEAR, -6);
            cal.set(Calendar.HOUR_OF_DAY, 0);
        } else {
            cal.add(Calendar.MONTH, -11);
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTimeInMillis();
    }

    private long getNormalizedTime(long ts) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(ts);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (!"day".equals(timeframe)) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
        }
        if ("year".equals(timeframe)) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
        }
        return cal.getTimeInMillis();
    }

    private String formatLabel(long ts) {
        if ("day".equals(timeframe)) return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date(ts));
        if ("week".equals(timeframe)) return new SimpleDateFormat("EEE", Locale.getDefault()).format(new Date(ts));
        return new SimpleDateFormat("MMM", Locale.getDefault()).format(new Date(ts));
    }

    private void generateAiInsights() {
        if (!isAdded()) return;
        if (allTransactions.isEmpty()) {
            tvAiInsights.setText("Not enough data to provide insights yet. Start using your wallet to see AI analysis!");
            return;
        }

        double totalSent = 0, totalReceived = 0;
        for (Transaction tx : allTransactions) {
            if ("sent".equals(tx.type)) totalSent += tx.amount;
            else if ("received".equals(tx.type)) totalReceived += tx.amount;
        }

        String insight;
        if (totalSent > totalReceived) {
            insight = "Your spending is outpacing your income. Focus on essential transfers and consider building a small buffer in your main balance.";
        } else if (totalReceived > totalSent * 1.5) {
            insight = "Excellent management! You have a strong surplus. This is a perfect time to allocate some funds into your Pockets for future goals.";
        } else {
            insight = "Balanced activity. Your incoming and outgoing funds are well-aligned. Keep this consistency to maintain financial stability.";
        }
        
        tvAiInsights.setText(insight);
    }

    private void fetchExchangeRates() {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url("https://open.er-api.com/v6/latest/ILS")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {}

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        JSONObject json = new JSONObject(response.body().string());
                        JSONObject rates = json.getJSONObject("rates");
                        
                        double ilsToUsd = rates.getDouble("USD");
                        double ilsToEur = rates.getDouble("EUR");
                        double ilsToGbp = rates.getDouble("GBP");

                        final String usdIls = String.format("%.2f", 1 / ilsToUsd);
                        final String eurIls = String.format("%.2f", 1 / ilsToEur);
                        final String gbpIls = String.format("%.2f", 1 / ilsToGbp);

                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                rateUsd.setText("₪ " + usdIls);
                                rateEur.setText("₪ " + eurIls);
                                rateGbp.setText("₪ " + gbpIls);
                            });
                        }
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
        });
    }
}