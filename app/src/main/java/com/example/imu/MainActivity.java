package com.example.imu;
import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private AutoCompleteTextView autoCompleteTextViewUniqueId;
    private List<String> fileNames = new ArrayList<>();
    private ArrayAdapter<String> adapterUniqueId;
    private Button saveButton;
    private String selectedUniqueId;

    private Button assessment;
    public static final int YOUR_REQUEST_CODE = 0;
    private static final int PERMISSIONS_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Request manage all files access permission if not granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }

        if (!hasPermissions()) {
            requestPermissions();
        }

        FileHandling filehandler = new FileHandling();
        filehandler.createCSVFile();

        autoCompleteTextViewUniqueId = findViewById(R.id.myEditText);
        saveButton = findViewById(R.id.Create_id);
        assessment = findViewById(R.id.start_assessment);



        // Read uniqueIds from idfile.csv and populate suggestions
        readUniqueIdFromFile();

        adapterUniqueId = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, fileNames);
        autoCompleteTextViewUniqueId.setAdapter(adapterUniqueId);

        autoCompleteTextViewUniqueId.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                // Handle item selection (if needed)
                selectedUniqueId = adapterView.getItemAtPosition(position).toString();
                // Uncomment the next line if you want to read data when an item is selected
                // readDataFromJson(selectedUniqueId);
                //sendDataToActivity(selectedUniqueId);
            }
        });


        // Add a TextWatcher to enable/disable the button based on text length
        autoCompleteTextViewUniqueId.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                saveButton.setEnabled(editable.length() == 0);
            }
        });

        adapterUniqueId.notifyDataSetChanged();

        // Set OnClickListener for saveButton
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Add code to start MainActivity2
                Intent intent = new Intent(MainActivity.this, MainActivity2.class);

                // Pass a unique identifier or any necessary data to identify MainActivity
                intent.putExtra("MainActivityIdentifier", "uniqueIdentifier");

                startActivityForResult(intent, YOUR_REQUEST_CODE);
            }
        });
        assessment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Check if selectedUniqueId is not null
                    if (selectedUniqueId != null) {
                        // Send data to MainActivity3
                        sendDataToActivity(selectedUniqueId);
                        autoCompleteTextViewUniqueId.getText().clear();
                    } else {
                        // Handle case where selectedUniqueId is null
                        Toast.makeText(MainActivity.this, "Please select a unique ID", Toast.LENGTH_SHORT).show();
                    }
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }
            }

        });
    }

    private boolean hasPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.BLUETOOTH_CONNECT,
                        Manifest.permission.BLUETOOTH_SCAN
                },
                PERMISSIONS_REQUEST_CODE
        );
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            boolean allPermissionsGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }
            if (allPermissionsGranted) {
                // Permissions granted, proceed with your functionality
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                // Permissions not granted, handle the case
                Toast.makeText(this, "App requires permission please allow", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == YOUR_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                String newUniqueId = data.getStringExtra("NewUniqueId");
                if (newUniqueId != null) {
                    autoCompleteTextViewUniqueId.setText(newUniqueId);
                    updateAutoCompleteSuggestions(); // Reload array adapter
                }
            }
        }
    }

    private void readUniqueIdFromFile() {
        File mainFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "imupatientdata");
        File secondaryFolder = new File(mainFolder, "json file");
        File uniqueIdFile = new File(secondaryFolder, "idfile.csv");

        try {
            BufferedReader bufferedReader = new BufferedReader(new FileReader(uniqueIdFile));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                // Add each uniqueId to the suggestions list
                fileNames.add(line);
            }

            bufferedReader.close();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error reading CSV file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // After adding a new unique ID, call this method to update the AutoCompleteTextView
    public void updateAutoCompleteSuggestions() {
        setupAutoCompleteTextView();
    }

    private void setupAutoCompleteTextView() {
        autoCompleteTextViewUniqueId.setAdapter(null); // Clear the existing adapter
        fileNames.clear(); // Clear the existing list

        // Read uniqueIds from idfile.csv and populate suggestions
        readUniqueIdFromFile();

        adapterUniqueId = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, fileNames);
        autoCompleteTextViewUniqueId.setAdapter(adapterUniqueId);

        adapterUniqueId.notifyDataSetChanged();
    }
    public void sendDataToActivity(String data) {
        Intent intent = new Intent(this, MainActivity3.class);
        intent.putExtra("selectedData", data);
        startActivity(intent);
    }
    public void onmyiconClick(View view){
        Intent intent=new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    public void onmyicon1Click(View view){
        Intent intent=new Intent(this, MainActivity5.class);
        startActivity(intent);
    }
}