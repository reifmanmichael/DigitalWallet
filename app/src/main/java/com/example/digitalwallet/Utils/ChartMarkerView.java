package com.example.digitalwallet.Utils;

import android.content.Context;
import android.widget.TextView;

import com.example.digitalwallet.R;
import com.github.mikephil.charting.components.MarkerView;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.utils.MPPointF;

import java.util.List;

public class ChartMarkerView extends MarkerView {

    private final TextView tvDate, tvValue;
    private final List<String> labels;

    public ChartMarkerView(Context context, int layoutResource, List<String> labels) {
        super(context, layoutResource);
        this.labels = labels;
        tvDate = findViewById(R.id.tvMarkerDate);
        tvValue = findViewById(R.id.tvMarkerValue);
    }

    @Override
    public void refreshContent(Entry e, Highlight highlight) {
        int index = (int) e.getX();
        if (index >= 0 && index < labels.size()) {
            tvDate.setText(labels.get(index));
        }
        tvValue.setText(String.format("₪%.2f", e.getY()));
        super.refreshContent(e, highlight);
    }

    @Override
    public MPPointF getOffset() {
        return new MPPointF(-(getWidth() / 2f), -getHeight());
    }
}