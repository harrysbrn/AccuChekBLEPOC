package com.senseonics.accuchekbleapp.accuchekble.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.senseonics.accuchekbleapp.accuchekble.R;
import com.senseonics.accuchekbleapp.accuchekble.Service.BluetoothLeService;
import com.senseonics.accuchekbleapp.accuchekble.other.CustomArrayAdapter;

import java.util.ArrayList;
import java.util.List;

public class BLEConnectActivity extends AppCompatActivity {
    private final static String TAG = BLEConnectActivity.class.getSimpleName();

    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;

    private boolean mScanning;

    private static final int RQS_ENABLE_BLUETOOTH = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    Button btnScan;
    ListView listViewLE;

    ArrayList<BluetoothDevice> listBluetoothDevice;
    ListAdapter adapterLeScanResult;

    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bleconnect);

        // Check if BLE is supported on the device.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this,
                    "BLUETOOTH_LE not supported in this device!",
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        getBluetoothAdapterAndLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "bluetoothManager.getAdapter()==null",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Make sure we have access coarse location enabled, if not, prompt the user to enable it
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
        btnScan = (Button) findViewById(R.id.scan);
        btnScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanLeDevice(true);
            }
        });
        listViewLE = (ListView) findViewById(R.id.lelist);

        listBluetoothDevice = new ArrayList<>();
        adapterLeScanResult = new CustomArrayAdapter(this, android.R.layout.simple_list_item_2, listBluetoothDevice);
        listViewLE.setAdapter(adapterLeScanResult);
        listViewLE.setOnItemClickListener(scanResultOnItemClickListener);
        listViewLE.setOnItemLongClickListener(scanResultOnItemLongClickListener);

        mHandler = new Handler();
    }
    AdapterView.OnItemLongClickListener scanResultOnItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            final BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);

            String msg = device.getAddress() + "\n"
                    + device.getBluetoothClass().toString() + "\n"
                    + getBTDeviceType(device);
            new AlertDialog.Builder(BLEConnectActivity.this)
                    .setTitle(device.getName())
                    .setMessage(msg)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    })/*.setNeutralButton("CONNECT", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Intent intent = new Intent(BLEConnectActivity.this,
                            ControlActivity.class);
                    intent.putExtra(ControlActivity.EXTRAS_DEVICE_NAME,
                            device.getName());
                    intent.putExtra(ControlActivity.EXTRAS_DEVICE_ADDRESS,
                            device.getAddress());

                    if (mScanning) {
                        mBluetoothLeScanner.stopScan(scanCallback);
                        mScanning = false;
                        btnScan.setEnabled(true);
                    }
                    startActivity(intent);
                }
            })*/
                    .show();

            return true;
        }
    };
    AdapterView.OnItemClickListener scanResultOnItemClickListener =
            new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    final BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(position);
                    if (device != null) {
                        Log.d(TAG,"##############Device not null");
                        int bondState = device.getBondState();
                        Log.d(TAG,"##############bondState"+bondState);
                        if(bondState == BluetoothDevice.BOND_NONE){
                            Log.d(TAG,"##############bondState-BOND_NONE");
                            boolean bondResult = device.createBond();
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    int newBondState = device.getBondState();
                                    Log.d(TAG,"##############newBondState"+newBondState);
                                    if (newBondState == BluetoothDevice.BOND_BONDED) {
                                        Log.d(TAG,"##############newBondState-BOND_BONDED");
                                        final Intent intent = new Intent(BLEConnectActivity.this,
                                                ControlActivity.class);
                                        intent.putExtra(ControlActivity.EXTRAS_DEVICE_NAME,
                                                device.getName());
                                        intent.putExtra(ControlActivity.EXTRAS_DEVICE_ADDRESS,
                                                device.getAddress());

                                        startActivity(intent);
                                    }else if(newBondState == BluetoothDevice.BOND_BONDING){
                                        Log.d(TAG,"##############newBondState-BOND_BONDING");
                                        waitForDeviceToGetBonded(device);
                                    }
                                }
                            }, 3000);
                        }else if(bondState == BluetoothDevice.BOND_BONDED){
                            Log.d(TAG,"##############bondState-BOND_BONDED");
                            final Intent intent = new Intent(BLEConnectActivity.this,
                                    ControlActivity.class);
                            intent.putExtra(ControlActivity.EXTRAS_DEVICE_NAME,
                                    device.getName());
                            intent.putExtra(ControlActivity.EXTRAS_DEVICE_ADDRESS,
                                    device.getAddress());

                            startActivity(intent);
                        }
                    }
                    if (mScanning) {
                        mBluetoothLeScanner.stopScan(scanCallback);
                        mScanning = false;
                        btnScan.setEnabled(true);
                    }
                }
            };

    private void waitForDeviceToGetBonded(final BluetoothDevice device){
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                int newBondState = device.getBondState();
                Log.d(TAG,"##############waitForDeviceToGetBonded-newBondState"+newBondState);
                if (newBondState == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG,"##############waitForDeviceToGetBonded-newBondState-BOND_BONDED");
                    final Intent intent = new Intent(BLEConnectActivity.this,
                            ControlActivity.class);
                    intent.putExtra(ControlActivity.EXTRAS_DEVICE_NAME,
                            device.getName());
                    intent.putExtra(ControlActivity.EXTRAS_DEVICE_ADDRESS,
                            device.getAddress());

                    startActivity(intent);
                }else if(newBondState == BluetoothDevice.BOND_BONDING){
                    Log.d(TAG,"##############waitForDeviceToGetBonded-newBondState-BOND_BONDING");
                    waitForDeviceToGetBonded(device);
                }
            }
        }, 3000);
    }

    private String getBTDeviceType(BluetoothDevice d) {
        String type = "";

        switch (d.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                type = "DEVICE_TYPE_CLASSIC";
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                type = "DEVICE_TYPE_DUAL";
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                type = "DEVICE_TYPE_LE";
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                type = "DEVICE_TYPE_UNKNOWN";
                break;
            default:
                type = "unknown...";
        }

        return type;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, RQS_ENABLE_BLUETOOTH);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == RQS_ENABLE_BLUETOOTH && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }

        getBluetoothAdapterAndLeScanner();

        // Checks if Bluetooth is supported on the device.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this,
                    "bluetoothManager.getAdapter()==null",
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void getBluetoothAdapterAndLeScanner() {
        // Get BluetoothAdapter and BluetoothLeScanner.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mScanning = false;
    }

    /*
    to call startScan (ScanCallback callback),
    Requires BLUETOOTH_ADMIN permission.
    Must hold ACCESS_COARSE_LOCATION or ACCESS_FINE_LOCATION permission to get results.
     */
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            listBluetoothDevice.clear();
            listViewLE.invalidateViews();

            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(scanCallback);
                    listViewLE.invalidateViews();

                    Toast.makeText(BLEConnectActivity.this,
                            "Scan timeout",
                            Toast.LENGTH_LONG).show();

                    mScanning = false;
                    btnScan.setEnabled(true);
                }
            }, SCAN_PERIOD);

            //mBluetoothLeScanner.startScan(scanCallback);

            //scan specified devices only with ScanFilter
            ScanFilter scanFilter =
                    new ScanFilter.Builder()
                            .setServiceUuid(BluetoothLeService.ParcelUuid_MTR01556566_GLUCOSEService)
                            .build();
            List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();
            scanFilters.add(scanFilter);

            ScanSettings scanSettings =
                    new ScanSettings.Builder().build();

            mBluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback);
            mScanning = true;
            btnScan.setEnabled(false);
        } else {
            mBluetoothLeScanner.stopScan(scanCallback);
            mScanning = false;
            btnScan.setEnabled(true);
        }
    }

    private ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            addBluetoothDevice(result.getDevice());
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            for (ScanResult result : results) {
                addBluetoothDevice(result.getDevice());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Toast.makeText(BLEConnectActivity.this,
                    "onScanFailed: " + String.valueOf(errorCode),
                    Toast.LENGTH_LONG).show();
        }

        private void addBluetoothDevice(BluetoothDevice device) {
            if (!listBluetoothDevice.contains(device)) {
                listBluetoothDevice.add(device);
                listViewLE.invalidateViews();
            }
        }
    };

}
