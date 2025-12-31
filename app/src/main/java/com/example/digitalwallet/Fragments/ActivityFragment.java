package com.example.digitalwallet.Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.digitalwallet.R;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ActivityFragment extends Fragment {

    private LineChart lineChart;
    private TextView rateUsd, rateEur, rateGbp;
    private final Handler handler = new Handler();
    private final Random random = new Random();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_activity, container, false);

        lineChart = view.findViewById(R.id.lineChart);
        rateUsd = view.findViewById(R.id.rateUsd);
        rateEur = view.findViewById(R.id.rateEur);
        rateGbp = view.findViewById(R.id.rateGbp);

        setupChart();
        startCurrencySimulation();

        return view;
    }

    private void setupChart() {
        List<Entry> entries = new ArrayList<>();
        // Create dummy "Flow" data
        for (int i = 0; i < 7; i++) {
            entries.add(new Entry(i, random.nextInt(500) + 100));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Spending (Last 7 Days)");
        dataSet.setColor(Color.parseColor("#2E86DE"));
        dataSet.setCircleColor(Color.parseColor("#FFD32A"));
        dataSet.setLineWidth(3f);
        dataSet.setCircleRadius(5f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Curved lines
        dataSet.setDrawValues(false);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        lineChart.getDescription().setEnabled(false);
        lineChart.getAxisRight().setEnabled(false);
        lineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        lineChart.animateY(1500); // Epic animation
        lineChart.invalidate();
    }

    private void startCurrencySimulation() {
        // Simulate live market changes every 3 seconds
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                updateRate(rateUsd, 3.60, 3.70);
                updateRate(rateEur, 3.90, 4.05);
                updateRate(rateGbp, 4.55, 4.70);
                handler.postDelayed(this, 3000);
            }
        };
        handler.post(runnable);
    }

    private void updateRate(TextView view, double min, double max) {
        double val = min + (max - min) * random.nextDouble();
        view.setText(String.format("₪ %.2f", val));

        // Flash animation
        view.animate().alpha(0.5f).setDuration(200).withEndAction(() ->
                view.animate().alpha(1f).setDuration(200).start()
        ).start();
    }
}