package com.example.imu;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONObject;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import android.os.Environment;
import java.io.File;
import java.io.FileWriter;


public class MainActivity4 extends AppCompatActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_PERMISSIONS = 2;
    private static final String TAG = "MainActivity4";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice selectedDevice;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private int connected = 0;

    private ArrayList<BluetoothDevice> deviceList = new ArrayList<>();
    private ArrayAdapter<String> bluetoothArrayAdapter;
    private boolean isReceiverRegistered = false;
    private boolean isReceiving = false;
    private AlertDialog alertDialog;

    private LineChart lineChart;
    private LineDataSet lineDataSet1;

    private ArrayList<Entry> chartEntries1;
    private GyroProcessor gyroProcessor;
    private String filename;
    private String Movementdir;
    private String insidedir;
    private String dirname;
    private static final int CHART_UPDATE_INTERVAL = 40; // 40 milliseconds
    private static final int CHART_UPDATE_BUFFER_SIZE = 100; // 50 data points
    private List<Entry> chartDataBuffer = new ArrayList<>();
    private Handler chartUpdateHandler;
    private Runnable chartUpdateRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main4);
        gyroProcessor = new GyroProcessor();

        Intent intent = getIntent();
        chartUpdateHandler = new Handler(Looper.getMainLooper());
        chartUpdateRunnable = this::updateChart;
        chartUpdateHandler.postDelayed(chartUpdateRunnable, CHART_UPDATE_INTERVAL);


        // Check if the intent has the extra data
        if (intent != null && intent.hasExtra("selectedData")) {
            // Extract the data
            filename = intent.getStringExtra("selectedData");
            Log.d(TAG, "Selected data: " + filename);
        }
        dirname = filename.substring(0, 7); // Extract the first 7 characters

// Print to verify
        Log.d("MainActivity3", "First 7 characters: " + dirname);
        int selectedOption = getIntent().getIntExtra("selectedOption", 1);
        if(selectedOption<=6){
            insidedir="neck";
        }
        else{
            insidedir="shoulder";
        }
        Log.d(TAG,"selectedoption "+insidedir);
        if(selectedOption==1){
            Movementdir="Flexion";
        } else if (selectedOption==2) {
            Movementdir = "Extension";
        } else if (selectedOption==3) {
            Movementdir="Right Lateral Flexion";
        } else if (selectedOption==4) {
            Movementdir="Left Lateral Flexion";
        } else if (selectedOption==5) {
            Movementdir="Right Rotation";
        } else if (selectedOption==6) {
            Movementdir="Left Rotation";
        } else if (selectedOption==7) {
            Movementdir="Abduction Adduction";
        } else if (selectedOption==8) {
            Movementdir="Flexion Extension";
        } else if (selectedOption==9) {
            Movementdir="External Rotation";
        } else if (selectedOption==10) {
            Movementdir="Internal Rotation";
        }
        Log.d(TAG,"Movementdir "+Movementdir);
        int imageResource;
        switch (selectedOption) {
            case 1:
                imageResource = R.drawable.img1;
                break;
            case 2:
                imageResource = R.drawable.img2;
                break;
            case 3:
                imageResource = R.drawable.img3;
                break;
            case 4:
                imageResource = R.drawable.img4;
                break;
            case 5:
                imageResource = R.drawable.img5;
                break;
            case 6:
                imageResource = R.drawable.img6;
                break;
            case 7:
                imageResource = R.drawable.img6;
                break;
            case 8:
                imageResource = R.drawable.img6;
                break;
            case 9:
                imageResource = R.drawable.img6;
                break;
            case 10:
                imageResource = R.drawable.img6;
                break;

            default:
                imageResource = R.drawable.img1;
        }

        ImageView imageView = findViewById(R.id.pic);
        imageView.setImageResource(imageResource);


        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION,
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_ADMIN
                        }, REQUEST_PERMISSIONS);
            } else {
                checkLocationServicesAndStartDiscovery();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                        }, REQUEST_PERMISSIONS);
            } else {
                checkLocationServicesAndStartDiscovery();
            }
        }

        Button assessment = findViewById(R.id.but);
        Button connect=findViewById(R.id.button);
        Button calibration=findViewById(R.id.but2);
        connect.setOnClickListener(v -> checkLocationServicesAndStartDiscovery());
        assessment.setOnClickListener(v -> receiveData());
        calibration.setOnClickListener(v->startCalibration());
        bluetoothArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        // Initialize the chart
        lineChart = findViewById(R.id.lineChart);

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
    }

    private void checkLocationServicesAndStartDiscovery() {
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager != null && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            new AlertDialog.Builder(this)
                    .setTitle("Enable Location")
                    .setMessage("Location services are required for Bluetooth scanning. Please enable location services.")
                    .setPositiveButton("Settings", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        } else {
            startBluetoothDiscovery();
        }
    }

    private void startBluetoothDiscovery() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            return;
        }
        bluetoothAdapter.startDiscovery();
        IntentFilter intentFilter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, intentFilter);
        isReceiverRegistered = true;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Scanning for Bluetooth devices...");
        builder.setAdapter(bluetoothArrayAdapter, (dialog, which) -> {
            selectedDevice = deviceList.get(which);
            if (selectedDevice != null) {
                connectToDevice(selectedDevice);
            } else {
                Toast.makeText(MainActivity4.this, "Device not found", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            bluetoothAdapter.cancelDiscovery();
            deviceList.clear();
            bluetoothArrayAdapter.clear();
            bluetoothArrayAdapter.notifyDataSetChanged();
        });
        alertDialog = builder.create();
        alertDialog.show();
    }

    private void connectToDevice(BluetoothDevice device) {
        final BluetoothSocket[] socket = {null}; // Declare socket as final array

        new Thread(() -> {
            try {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    showToast("Bluetooth permissions not granted");
                    Log.e(TAG, "Bluetooth permissions not granted");
                    return;
                }
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.R) {
                    socket[0] = device.createRfcommSocketToServiceRecord(MY_UUID);
                } else {
                    Method method = device.getClass().getMethod("createRfcommSocket", int.class);
                    socket[0] = (BluetoothSocket) method.invoke(device, 1);
                }
                socket[0].connect();
                if (socket[0].isConnected()) {
                    Log.i(TAG, "Connected successfully to " + device.getName());
                    runOnUiThread(() -> {
                        showToast("Connected successfully to " + device.getName());
                        if (alertDialog != null && alertDialog.isShowing()) {
                            alertDialog.dismiss();
                        }
                    });
                    connected = 1;
                    bluetoothSocket = socket[0];
                    inputStream = socket[0].getInputStream();
                    outputStream = socket[0].getOutputStream();
                    isReceiving = true;

                } else {
                    showToast("Bluetooth disconnected");
                    isReceiving = false;
                }
            } catch (Exception e) {
                Log.e(TAG, "Unable to connect", e);
                runOnUiThread(() -> showToast("Connection failed"));
                connected = 0;
                try {
                    if (socket[0] != null) {
                        socket[0].close();
                    }
                } catch (IOException ex) {
                    Log.e(TAG, "Error closing socket", ex);
                }
            }
        }).start();
    }
    float mxvalue=0;

    private void receiveData() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            List<Byte> byteList = new ArrayList<>();
            double[] offset = loadCalibrationData();

            while (isReceiving) {
                try {
                    int numBytes = inputStream.read(buffer);
                    if (numBytes == -1) {
                        handleDisconnection();
                        break;
                    }

                    for (int i = 0; i < numBytes; i++) {
                        byteList.add(buffer[i]);
                    }

                    while (byteList.size() >= 12) {
                        if (byteList.get(0) == (byte) 255 && byteList.get(1) == (byte) 255) {
                            int payloadSize = byteList.get(2) & 0xFF;
                            if (byteList.size() >= 12) {
                                int checksum = 255 + 255 + payloadSize;
                                byte[] payload = new byte[8];
                                for (int i = 0; i < 8; i++) {
                                    payload[i] = byteList.get(i + 3);
                                    checksum += payload[i] & 0xFF;
                                }
                                checksum = checksum % 256;
                                if (checksum == (byteList.get(11) & 0xFF)) {
                                    short[] data = new short[4];
                                    ByteBuffer.wrap(payload).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);

                                    float gx = data[0];
                                    float gy = data[1];
                                    float gz = data[2];
                                    float gyroAng = gyroProcessor.rom(data, offset);
                                    Log.i(TAG, "Processed Gyro Angle: " + gyroAng);
                                    chartDataBuffer.add(new Entry(mxvalue, gyroAng));
                                    writeCSVData(gx,gy,gz,gyroAng);
                                    mxvalue++;


                                    for (int i = 0; i < 12; i++) {
                                        byteList.remove(0);
                                    }
                                }
                            }
                        } else {
                            byteList.remove(0);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading data", e);
                    handleDisconnection();
                    break;
                }
            }
        }).start();
    }

    @SuppressLint("DefaultLocale")
    private void writeCSVData(float gx, float gy, float gz, float gyroAng) {
        new Thread(() -> {
            File mainFolder = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "imupatientdata");
            File dirnameFolder = new File(mainFolder, dirname);
            File insideDirFolder = new File(dirnameFolder, insidedir);
            File movementFolder= new File(insideDirFolder, Movementdir);
            File csvFile=new File(movementFolder,"data.csv");
            if (!csvFile.exists()) {
                try {
                    csvFile.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "Error creating CSV file", e);
                    return;
                }
            }

            try (FileWriter writer = new FileWriter(csvFile, true)) {
                writer.append(String.format("%f,%f,%f,%f%n", gx, gy, gz, gyroAng));
                writer.flush();
            } catch (IOException e) {
                Log.e(TAG, "Error writing to CSV file", e);
            }
        }).start();
    }

    private void startCalibration() {
        new Thread(() -> {
            byte[] buffer = new byte[1024];
            List<Byte> byteList = new ArrayList<>();
            List<short[]> calibrationData = new ArrayList<>();

            int count = 0;
            while (count < 5000) {
                try {
                    if (inputStream == null) {
                        runOnUiThread(() -> showToast("Input stream is null, cannot read data"));
                        return;
                    }

                    int numBytes = inputStream.read(buffer);
                    if (numBytes == -1) {
                        handleDisconnection();
                        break;
                    }

                    for (int i = 0; i < numBytes; i++) {
                        byteList.add(buffer[i]);
                    }

                    while (byteList.size() >= 12) {
                        if (byteList.get(0) == (byte) 255 && byteList.get(1) == (byte) 255) {
                            int payloadSize = byteList.get(2) & 0xFF;
                            if (byteList.size() >= 12) {
                                int checksum = 255 + 255 + payloadSize;
                                byte[] payload = new byte[8];
                                for (int i = 0; i < 8; i++) {
                                    payload[i] = byteList.get(i + 3);
                                    checksum += payload[i] & 0xFF;
                                }
                                checksum = checksum % 256;
                                if (checksum == (byteList.get(11) & 0xFF)) {
                                    short[] data = new short[4];
                                    ByteBuffer.wrap(payload).order(java.nio.ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(data);
                                    calibrationData.add(data);
                                    count++;
                                }
                                for (int i = 0; i < 12; i++) {
                                    byteList.remove(0);
                                }
                            }
                        } else {
                            byteList.remove(0);
                        }
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading data", e);
                    handleDisconnection();
                    break;
                }
            }

            if (calibrationData.size() >= 5000) {  // Use >= to handle edge cases
                calculateAndStoreCalibrationData(calibrationData);
            } else {
                runOnUiThread(() -> showToast("Calibration failed, not enough data points: " + calibrationData.size()));
            }
        }).start();
    }

    private void calculateAndStoreCalibrationData(List<short[]> calibrationData) {
        long sumGx = 0, sumGy = 0, sumGz = 0;

        for (short[] data : calibrationData) {
            sumGx += data[0];
            sumGy += data[1];
            sumGz += data[2];
        }

        float meanGx = sumGx / 5000.0f;
        float meanGy = sumGy / 5000.0f;
        float meanGz = sumGz / 5000.0f;

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("meanGx", meanGx);
            jsonObject.put("meanGy", meanGy);
            jsonObject.put("meanGz", meanGz);

            String jsonString = jsonObject.toString();
            FileOutputStream fos = openFileOutput("calibration.json", MODE_PRIVATE);
            fos.write(jsonString.getBytes());
            fos.close();

            runOnUiThread(() -> showToast("Calibration completed successfully"));
        } catch (Exception e) {
            Log.e(TAG, "Error storing calibration data", e);
            runOnUiThread(() -> showToast("Error storing calibration data"));
        }
    }

    private double[] loadCalibrationData() {
        double[] offset = {0, 0, 0};
        try {
            FileInputStream fis = openFileInput("calibration.json");
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            fis.close();

            String jsonString = new String(buffer, "UTF-8");
            JSONObject jsonObject = new JSONObject(jsonString);

            offset[0] = jsonObject.getDouble("meanGx");
            offset[1] = jsonObject.getDouble("meanGy");
            offset[2] = jsonObject.getDouble("meanGz");
        } catch (Exception e) {
            Log.e(TAG, "Error loading calibration data", e);
        }
        return offset;
    }

    private void handleDisconnection() {
        isReceiving = false;
        connected = 0;
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing streams", e);
        }
        runOnUiThread(() -> showToast("Connection lost or disconnected"));
    }


    private void updateChart() {
        if (chartDataBuffer.size() >= CHART_UPDATE_BUFFER_SIZE) {
            runOnUiThread(() -> {
                List<Entry> newEntries = new ArrayList<>(chartDataBuffer);
                chartDataBuffer.clear();

                chartEntries1.addAll(newEntries);
                lineDataSet1 = new LineDataSet(chartEntries1, "IMU Data");
                LineData lineData = new LineData(lineDataSet1);
                lineDataSet1.setAxisDependency(YAxis.AxisDependency.LEFT);
                lineDataSet1.setLineWidth(2);
                lineDataSet1.setDrawCircles(false);
                lineChart.setData(lineData);
                lineChart.invalidate();
            });
        }
        chartUpdateHandler.postDelayed(chartUpdateRunnable, CHART_UPDATE_INTERVAL);
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkLocationServicesAndStartDiscovery();
            } else {
                Toast.makeText(this, "Permissions denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Bluetooth enabling cancelled", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (isReceiverRegistered) {
            unregisterReceiver(myReceiver);
        }
        try {
            if (inputStream != null) {
                inputStream.close();
            }
            if (bluetoothSocket != null) {
                bluetoothSocket.close();
            }
        } catch (IOException e) {
            Log.e(TAG, "Error closing streams", e);
        }
    }

    private final BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (ActivityCompat.checkSelfPermission(MainActivity4.this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(MainActivity4.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(context, "Permissions not granted", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (device != null && device.getName() != null) {
                    deviceList.add(device);
                    bluetoothArrayAdapter.add(device.getName());
                    bluetoothArrayAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    private void showToast(String message) {
        runOnUiThread(() -> Toast.makeText(MainActivity4.this, message, Toast.LENGTH_SHORT).show());
    }
}
