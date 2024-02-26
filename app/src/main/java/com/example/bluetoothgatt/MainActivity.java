package com.example.bluetoothgatt;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothLeScanner mBluetoothLeScanner;

    private BluetoothGattCharacteristic bluetoothGattCharacteristic;

    private Handler mHandler;

    private BluetoothGatt bluetoothGatt;
    //    private String deviceAddress = "F4:6B:EB:7E:53:26";
    private String uartServiceUUID = "6E400001-B5A3-F393-E0A9-E50E24DCCA9E";
    private String txCharacteristicUUID = "6E400003-B5A3-F393-E0A9-E50E24DCCA9E";
    private String rxCharacteristicUUID = "6E400002-B5A3-F393-E0A9-E50E24DCCA9E";
    private Button btScan, btSendData;
    private boolean mScanning;
    private static final long SCAN_PERIOD = 10000;

    private static final int REQUEST_ENABLE_BT = 1;

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btScan = findViewById(R.id.btScan);
        btSendData = findViewById(R.id.btSendData);

        mHandler = new Handler();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermission();
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        mBluetoothLeScanner = bluetoothAdapter.getBluetoothLeScanner();

        btScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });

        btSendData.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataToDevice();
            }
        });
    }

    private void sendDataToDevice() {
        if (bluetoothGatt == null) {
            Toast.makeText(this, "Device not connect", Toast.LENGTH_SHORT).show();
        } else {
//            BluetoothGattCharacteristic characteristic = bluetoothGatt.getService(UUID.fromString(uartServiceUUID))
//                    .getCharacteristic(UUID.fromString(rxCharacteristicUUID));
            // Gửi dữ liệu đi
            byte[] startPackage = new byte[]{ 'R', 'G', '*', 'S', (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF };
            bluetoothGattCharacteristic.setValue(startPackage);

            // Gửi dữ liệu bằng cách gọi writeCharacteristic
            @SuppressLint("MissingPermission") boolean success = bluetoothGatt.writeCharacteristic(bluetoothGattCharacteristic);
            if (!success) {
                // Xử lý lỗi nếu không gửi được dữ liệu
                Log.d("ERROR", "CAN't SEND DATA");
            } else {
                // Gửi thành công
                // Xử lý lỗi nếu không gửi được dữ liệu
                Log.d("Success", "CAN't SEND DATA");
            }
        }
    }


    @SuppressLint("MissingPermission")
    private void connectToDevice(String deviceAddress) {
        BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(deviceAddress);
        bluetoothGatt = bluetoothDevice.connectGatt(this, false, gattCallback);
    }

    @SuppressLint("MissingPermission")
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @SuppressLint("MissingPermission")
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothLeScanner.startScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    private ScanCallback mLeScanCallback =
            new ScanCallback() {
                @SuppressLint("MissingPermission")
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    Log.d("Device Scan", result.getDevice().getAddress());

                    if (result.getDevice().getAddress().equals("F4:6B:EB:7E:53:26")) {
                        connectToDevice(result.getDevice().getAddress());
                    }
                }
            };

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @SuppressLint("MissingPermission")
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("BluetoothGattCallback", "STATE_CONNECTED");
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                Log.d("BluetoothGattCallback", "STATE_DISCONNECTED");
            }
        }

        @SuppressLint("MissingPermission")
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                bluetoothGattCharacteristic = bluetoothGatt.getService(UUID.fromString(uartServiceUUID))
                        .getCharacteristic(UUID.fromString(rxCharacteristicUUID));
                bluetoothGatt.setCharacteristicNotification(bluetoothGattCharacteristic, true);
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                // Characteristic write successful
                Log.d("GATT_SUCCESS", "Characteristic write successful");
            } else {
                // Characteristic write failed
                Log.d("GATT_SUCCESS", "Characteristic write failed");
            }
        }

        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            Log.d("onCharacteristicChanged", Arrays.toString(value));
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @RequiresApi(api = Build.VERSION_CODES.S)
    private void requestPermission() {
        // Connected to the device, now discover services
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            String[] PERMISSIONS = {
                    android.Manifest.permission.BLUETOOTH_CONNECT,
            };
            requestPermissions(PERMISSIONS, 101);
        }
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onDestroy() {
        super.onDestroy();
        bluetoothGatt.close();
    }
}