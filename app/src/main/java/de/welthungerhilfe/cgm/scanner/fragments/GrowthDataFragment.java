/*
 * Child Growth Monitor - quick and accurate data on malnutrition
 * Copyright (c) 2018 Markus Matiaschek <mmatiaschek@gmail.com> for Welthungerhilfe
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.welthungerhilfe.cgm.scanner.fragments;

import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;

import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.ScatterChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.ScatterData;
import com.github.mikephil.charting.data.ScatterDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.github.mikephil.charting.interfaces.datasets.IScatterDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.jaredrummler.materialspinner.MaterialSpinner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import de.welthungerhilfe.cgm.scanner.R;
import de.welthungerhilfe.cgm.scanner.activities.CreateDataActivity;
import de.welthungerhilfe.cgm.scanner.models.Measure;
import de.welthungerhilfe.cgm.scanner.views.VerticalTextView;

public class GrowthDataFragment extends Fragment {
    private Context context;

    private final String[] boys = {"wfa_boys_p_exp.txt", "lhfa_boys_p_exp.txt", "wfh_boys_p_exp.txt", "hcfa_boys_p_exp.txt", "acfa_boys_p_exp.txt"};
    private final String[] girls = {"wfa_girls_p_exp.txt", "lhfa_girls_p_exp.txt", "wfh_girls_p_exp.txt", "hcfa_girls_p_exp.txt", "acfa_girls_p_exp.txt"};

    private LineChart mChart;
    private MaterialSpinner dropChart;

    private VerticalTextView txtYAxis;
    private TextView txtXAxis;

    private TextView txtLabel;

    private int chartType = 0;

    public static GrowthDataFragment newInstance(Context context) {
        GrowthDataFragment fragment = new GrowthDataFragment();
        fragment.context = context;

        return fragment;
    }

    public void onResume() {
        super.onResume();
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(getView().getWindowToken(), 0);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_growth, container, false);

        txtLabel = view.findViewById(R.id.txtLabel);
        if (((CreateDataActivity)context).person != null) {
            txtLabel.setText(((CreateDataActivity)context).person.getSex());
        }

        txtYAxis = view.findViewById(R.id.txtYAxis);
        txtXAxis = view.findViewById(R.id.txtXAxis);

        //chartGrowth = view.findViewById(R.id.chartGrowth);
        mChart = view.findViewById(R.id.chart1);
        dropChart = view.findViewById(R.id.dropChart);

        @SuppressLint("ResourceType") String[] filters = getResources().getStringArray(R.array.filters);
        dropChart.setItems(filters);
        dropChart.setOnItemSelectedListener(new MaterialSpinner.OnItemSelectedListener<String>() {
            @Override public void onItemSelected(MaterialSpinner view, int position, long id, String item) {
                chartType = position;
                setData();
            }
        });

        initChart();
        //setChartData();

        return view;
    }

    private void initChart() {
        // no description text
        mChart.getDescription().setEnabled(false);

        // enable touch gestures
        mChart.setTouchEnabled(true);

        mChart.setDragDecelerationFrictionCoef(0.9f);

        // enable scaling and dragging
        mChart.setDragEnabled(true);
        mChart.setScaleEnabled(true);
        mChart.setDrawGridBackground(false);
        mChart.setHighlightPerDragEnabled(true);

        // set an alternative background color
        mChart.setBackgroundColor(Color.WHITE);

        // get the legend (only possible after setting data)
        mChart.getLegend().setEnabled(true);

        mChart.getAxisLeft().setEnabled(true);
        mChart.getAxisLeft().setDrawGridLines(true);

        mChart.getXAxis().setDrawAxisLine(true);
        mChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        mChart.getXAxis().setDrawGridLines(true);
        /*
        mChart.getXAxis().setGranularity(1.0f);
        mChart.getXAxis().setGranularityEnabled(true);
        */

        setData();
    }

    public void setData() {
        if (context == null || mChart == null) {
            return;
        }
        if (((CreateDataActivity)context).measures == null || ((CreateDataActivity)context).measures.size() == 0) {
            return;
        }

        switch (chartType) {
            case 0:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_weight);
                break;
            case 1:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_height);
                break;
            case 2:
                txtXAxis.setText(R.string.axis_height);
                txtYAxis.setText(R.string.axis_weight);
                break;
            case 3:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_head);
                break;
            case 4:
                txtXAxis.setText(R.string.axis_age);
                txtYAxis.setText(R.string.axis_muac);
                break;
        }

        long birthday = ((CreateDataActivity)context).person.getBirthday();

        ArrayList<ILineDataSet> dataSets = new ArrayList<ILineDataSet>();

        // ----------------------- Line for manual measures -------------------------- //
        ArrayList<Entry> measures = new ArrayList<Entry>();

        double maxHeight = 0;

        for (Measure measure : ((CreateDataActivity)context).measures) {
            if (!measure.getType().equals("manual"))
                continue;

            if (measure.getHeight() > maxHeight)
                maxHeight = measure.getHeight();

            long day = (measure.getDate() - birthday) / 1000 / 60 / 60 / 24;

            if (chartType == 0)
                measures.add(new Entry(day, (float) measure.getWeight()));
            else if (chartType == 1)
                measures.add(new Entry(day, (float) measure.getHeight()));
            else if (chartType == 2) {
                measures.add(new Entry((float) measure.getHeight(), (float) measure.getWeight()));
                Collections.sort(measures, new Comparator<Entry>() {
                    @Override
                    public int compare(Entry o1, Entry o2) {
                        return Float.compare(o1.getX(), o2.getX());
                    }
                });
            } else if (chartType == 3) {
                measures.add(new Entry(day, (float) measure.getHeadCircumference()));
            } else if (chartType == 4) {
                measures.add(new Entry(day, (float) measure.getMuac()));
            }
        }

        if (measures.size() > 1)
            dataSets.add(createDataSet(measures, "measure", Color.rgb(0, 0, 0), 3f, false));
        else {
            dataSets.add(createDataSet(measures, "measure", Color.rgb(0, 0, 0), 3f, true));
        }

        // ------------------------- Line for ruler values ---------------------------------- //
        long days = (System.currentTimeMillis() - birthday) / 1000 / 60 / 60 / 24 + 100;

        ArrayList<Entry> p3 = new ArrayList<Entry>();
        ArrayList<Entry> p15 = new ArrayList<Entry>();
        ArrayList<Entry> p50 = new ArrayList<Entry>();
        ArrayList<Entry> p85 = new ArrayList<Entry>();
        ArrayList<Entry> p97 = new ArrayList<Entry>();

        try {
            BufferedReader reader = null;

            if (((CreateDataActivity)context).person.getSex().equals("female"))
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(girls[chartType]), "UTF-8"));
            else
                reader = new BufferedReader(new InputStreamReader(context.getAssets().open(boys[chartType]), "UTF-8"));

            String mLine;
            while ((mLine = reader.readLine()) != null) {
                String[] arr = mLine.split("\t");
                float rule = 0;
                try {
                    rule = Float.parseFloat(arr[0]);
                } catch (Exception e) {
                    continue;
                }

                p3.add(new Entry(rule, Float.parseFloat(arr[6])));
                p15.add(new Entry(rule, Float.parseFloat(arr[9])));
                p50.add(new Entry(rule, Float.parseFloat(arr[11])));
                p85.add(new Entry(rule, Float.parseFloat(arr[13])));
                p97.add(new Entry(rule, Float.parseFloat(arr[16])));

                if ((chartType == 0 || chartType == 1 || chartType == 3 || chartType == 4) && rule > days)
                    break;
                /*
                if (chartType == 1 && rule > maxHeight)
                    break;
                */
                if (chartType == 2 && rule > maxHeight)
                    break;
            }
            reader.close();

            dataSets.add(createDataSet(p3, "3rd", Color.rgb(212, 53, 62), 1.5f, false));
            dataSets.add(createDataSet(p15, "15th", Color.rgb(230, 122, 58), 1.5f, false));
            dataSets.add(createDataSet(p50, "50th", Color.rgb(55, 129, 69), 1.5f, false));
            dataSets.add(createDataSet(p85, "85th", Color.rgb(230, 122, 58), 1.5f, false));
            dataSets.add(createDataSet(p97, "97th", Color.rgb(212, 53, 62), 1.5f, false));
        } catch (IOException e) {
            e.printStackTrace();
        }

        mChart.setData(new LineData(dataSets));
        //mChart.invalidate();
        mChart.animateX(3000);
    }

    protected LineDataSet createDataSet(ArrayList<Entry> values, String label, int color, float width, boolean circle) {
        LineDataSet dataSet = new LineDataSet(values, label);
        dataSet.setAxisDependency(YAxis.AxisDependency.LEFT);
        dataSet.setColor(color);
        dataSet.setValueTextColor(ColorTemplate.getHoloBlue());
        dataSet.setLineWidth(width);
        dataSet.setDrawCircles(circle);
        dataSet.setCircleColor(color);
        dataSet.setCircleRadius(3f);
        dataSet.setDrawValues(false);
        dataSet.setFillAlpha(65);
        dataSet.setFillColor(ColorTemplate.getHoloBlue());
        dataSet.setHighLightColor(Color.rgb(244, 117, 117));
        dataSet.setDrawCircleHole(false);

        return dataSet;
    }
}
