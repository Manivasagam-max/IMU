package com.example.imu;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity5 extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteTextViewUniqueId;
    private List<String> fileNames = new ArrayList<>();
    private ArrayAdapter<String> adapterUniqueId;
    private String selectedUniqueId;
    private String selectedOption = ""; // Initialize selectedOption to an empty string
    private String selectedOption1;
    private Button fetch;
    private String dirname;
    private LineChart lineChart;
    private LineDataSet lineDataSet1;
    private ArrayList<Entry> chartEntries1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main5);

        autoCompleteTextViewUniqueId = findViewById(R.id.myEditText);
        fetch=findViewById(R.id.but3);
        Spinner spinner = findViewById(R.id.spinner1);
        Spinner spinner2 = findViewById(R.id.spinner2);

        ArrayList<String> part = new ArrayList<>();
        part.add("select Part");
        part.add("neck");
        part.add("shoulder");
        // Create an ArrayAdapter using the list of items and a default spinner layout
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, part);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedOption = parent.getItemAtPosition(position).toString();
                if (selectedOption.equals("select Part")) {
                    Toast.makeText(MainActivity5.this, "select part", Toast.LENGTH_SHORT).show();
                } else {
                    ArrayList<String> movement = new ArrayList<>();
                    if (selectedOption.equals("neck")) {
                        movement.add("select movement");
                        movement.add("Flexion");
                        movement.add("Extension");
                        movement.add("Left Lateral Flexion");
                        movement.add("Right Lateral Flexion");
                        movement.add("Left Rotation");
                        movement.add("Right Rotation");
                    } else if (selectedOption.equals("shoulder")) {
                        // Add shoulder movements here if needed
                        movement.add("select movement");
                        movement.add("Abduction Adduction");
                        movement.add("Flexion Extension");
                        movement.add("Internal Rotation");
                        movement.add("External Rotation");
                    }
                    ArrayAdapter<String> adapter1 = new ArrayAdapter<>(MainActivity5.this, android.R.layout.simple_spinner_item, movement);
                    adapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinner2.setAdapter(adapter1);

                    spinner2.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                            selectedOption1 = parent.getItemAtPosition(position).toString();
                            if (selectedOption1.equals("select movement")) {
                                Toast.makeText(MainActivity5.this, "select Movement", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> parent) {
                            Toast.makeText(MainActivity5.this, "select part", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                Toast.makeText(MainActivity5.this, "select part", Toast.LENGTH_SHORT).show();
            }
        });
        readUniqueIdFromFile();
        adapterUniqueId = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, fileNames);
        autoCompleteTextViewUniqueId.setAdapter(adapterUniqueId);

        autoCompleteTextViewUniqueId.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                selectedUniqueId = adapterView.getItemAtPosition(position).toString();
                dirname =selectedUniqueId.substring(0, 7);
                Log.d("MainActivity5",dirname);

            }
        });
        lineChart = findViewById(R.id.lineChart1);
        chartEntries1 = new ArrayList<>();
        // Customize the chart
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawAxisLine(false);

        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setDrawGridLines(false);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(360f);

        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
        rightAxis.setDrawGridLines(false);

        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);
        lineChart.invalidate();

        fetch.setOnClickListener(v-> fetchdata());
    }

    private void readUniqueIdFromFile() {
        File mainFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "imupatientdata");
        File secondaryFolder = new File(mainFolder, "json file");
        File uniqueIdFile = new File(secondaryFolder, "idfile.csv");

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(uniqueIdFile));
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                fileNames.add(line);
            }
            bufferedReader.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading CSV file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    private void fetchdata(){
        Log.d("MainActivity5",selectedOption );
        Log.d("MainActivity5",selectedOption1 );
        File mainFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "imupatientdata");
        File dirnameFolder = new File(mainFolder, dirname);
        File insideDirFolder = new File(dirnameFolder, selectedOption);
        File movementFolder= new File(insideDirFolder, selectedOption1);
        File csvFile=new File(movementFolder,"data.csv");
        if (!csvFile.exists()){
            Toast.makeText(this, "No Data found", Toast.LENGTH_SHORT).show();
        } else {
            try {
                BufferedReader bufferedReader = new BufferedReader(new FileReader(csvFile));
                String line;
                int xIndex = 0;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] columns = line.split(",");
                    if (columns.length >= 4) {
                        float value = Float.parseFloat(columns[3]);
                        chartEntries1.add(new Entry(xIndex++, value));
                    }
                }
                bufferedReader.close();

                lineDataSet1 = new LineDataSet(chartEntries1, "Gyro Angle");
                lineDataSet1.setAxisDependency(YAxis.AxisDependency.LEFT);
                LineData lineData = new LineData(lineDataSet1);
                lineDataSet1.setLineWidth(2);
                lineDataSet1.setDrawCircles(false);
                lineChart.setData(lineData);
                lineChart.invalidate();

                Toast.makeText(this, "Data loaded successfully", Toast.LENGTH_SHORT).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error reading CSV file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }

    }

    public void onmyiconClick(View view) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void onmyicon1Click(View view) {
        Intent intent = new Intent(this, MainActivity5.class);
        startActivity(intent);
    }
}
