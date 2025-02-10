package com.example.ecg3;

import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;


import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.io.InputStream;
import java.util.UUID;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private LineChart lineChart;
    private LineData lineData;
    private LineDataSet lineDataSet;
    private ArrayList<Entry> dataValues;
    private Handler handler;

    private static final String DEVICE_ADDRESS = "XX:XX:XX:XX:XX:XX"; // Replace with your Bluetooth device address
    private static final UUID DEVICE_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Standard UUID for serial devices

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnConnect = findViewById(R.id.btnConnect);
        lineChart = findViewById(R.id.lineChart);

        // Initialize the chart
        dataValues = new ArrayList<Entry>();
        lineDataSet = new LineDataSet(dataValues, "Live Data");
        lineData = new LineData(lineDataSet);
        lineChart.setData(lineData);

        // Customize chart appearance
        Description desc = new Description();
        desc.setText("Real-Time Data");
        lineChart.setDescription(desc);

        handler = new Handler();

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        btnConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectToBluetooth();
            }
        });
    }

    private void connectToBluetooth() {
        try {
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            bluetoothSocket = device.createRfcommSocketToServiceRecord(DEVICE_UUID);
            bluetoothSocket.connect();

            Toast.makeText(this, "Connected to Bluetooth", Toast.LENGTH_SHORT).show();
            readDataFromBluetooth();
        } catch (Exception e) {
            Log.e("Bluetooth", "Connection failed", e);
            Toast.makeText(this, "Bluetooth Connection Failed", Toast.LENGTH_SHORT).show();
        }
    }

    private void readDataFromBluetooth() {
        new Thread(() -> {
            try {
                InputStream inputStream = bluetoothSocket.getInputStream();
                byte[] buffer = new byte[1024];
                int bytes;

                while ((bytes = inputStream.read(buffer)) != -1) {
                    String data = new String(buffer, 0, bytes);
                    float value = Float.parseFloat(data.trim());
                    updateGraph(value);
                }
            } catch (Exception e) {
                Log.e("Bluetooth", "Data read failed", e);
            }
        }).start();
    }

    private void updateGraph(float value) {
        handler.post(() -> {
            dataValues.add(new Entry(dataValues.size(), value));
            lineDataSet.notifyDataSetChanged();
            lineData.notifyDataChanged();
            lineChart.notifyDataSetChanged();
            lineChart.invalidate();
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (Exception e) {
            Log.e("Bluetooth", "Error closing socket", e);
        }
    }
}