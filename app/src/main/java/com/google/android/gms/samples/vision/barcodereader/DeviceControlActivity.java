/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.samples.vision.barcodereader;


import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.RequiresApi;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.samples.vision.barcodereader.BluetoothLeService;
import com.google.android.gms.samples.vision.barcodereader.DeviceScanActivity;
import com.google.android.gms.samples.vision.barcodereader.R;

import java.util.ArrayList;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends ListActivity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public static final String EXTRA_DATA                                                       = "EXTRA_DATA";
    public static final String EXTRA_DEVICE                                                     = "EXTRA_DEVICE";

    private ListView mResultList;
    private ListViewAdapter mResultAdapter;

    public ArrayList<BluetoothLeService> leServicesList = new ArrayList<>();
    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private boolean mOpen = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    public StartActivity mStartActivity;

    public ImageView result_img;
    private MediaPlayer mp;
    private MediaPlayer mp1;

    private int num = 0;

    private class ListViewAdapter extends BaseAdapter {
        private ArrayList<BluetoothLeService> mListService;
        protected ArrayList<BluetoothGatt> mGattList;
        protected ArrayList<BluetoothGattCallback> mGattCallbackList;
        private ArrayList<BluetoothDevice> mDevices;
        private LayoutInflater mInflator;
        private BluetoothDevice currentDevice;

        public ListViewAdapter(ArrayList<BluetoothGatt> gattList) {
            super();
            mListService = new ArrayList<BluetoothLeService>();
            mDevices = new ArrayList<BluetoothDevice>();
            mGattList = gattList;
            mGattCallbackList = mBluetoothLeService.mGattCallbackList;
            mInflator = DeviceControlActivity.this.getLayoutInflater();
        }

        public void addLeService(BluetoothLeService LeService) {
            if(!mListService.contains(LeService)) {
                mListService.add(LeService);
            }
        }

        public void addDevice(BluetoothDevice device) {
            if(!mDevices.contains(device)) {
                mDevices.add(device);
            }
            dataChange();
        }

        public BluetoothLeService getService(int position) {
            return mListService.get(position);
        }

        public void clear() {
            mListService.clear();
        }

        @Override
        public int getCount() {
            return mDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }


        public void dataChange() {
            mResultAdapter.notifyDataSetChanged();
        }

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @SuppressLint("ResourceType")
        @Override
        public View getView(final int i, View view, ViewGroup viewGroup) {
            Log.e(TAG, "result view start // " + mGattList.get(i).getDevice().getAddress());
            final DeviceControlActivity.ViewHolder viewHolder;
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.result_layout, null);
                viewHolder = new DeviceControlActivity.ViewHolder();
                viewHolder.mDetectionTextView = (TextView) view.findViewById(R.id.result_title);
                viewHolder.mDetectionSwitch = (Switch) view.findViewById(R.id.switch_detect);
                viewHolder.mResultToolbar = (Toolbar) view.findViewById(R.id.pme_detect_toolbar);
                // PME result
                result_img = (ImageView) view.findViewById(R.id.result_img);
                view.setTag(viewHolder);
            } else {
                viewHolder = (DeviceControlActivity.ViewHolder) view.getTag();
            }

//            final BluetoothLeService service = mListService.get(i);
            currentDevice = mGattList.get(i).getDevice();
            final BluetoothGatt currentGatt = mGattList.get(i);
            final String deviceName = currentDevice.getName();
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.mDetectionTextView.setText(deviceName + " " + currentDevice.getAddress());
            else
                viewHolder.mDetectionTextView.setText(R.string.unknown_device);
//            viewHolder.deviceAddress.setText(device.getAddress());


            if (viewHolder.mResultToolbar != null) {
//            viewHolder.mResultToolbar.setTitle("result_title");

                viewHolder.mDetectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                        mBluetoothLeService.setCharacteristicNotification(mGattList.get(i), mBluetoothLeService.mClassificationCharacteristic, isChecked);
                        Log.d(TAG, mGattList.get(i).getDevice().getAddress() + "// clicked // " + i);
                        /*
                        if (viewHolder.mDetectionSwitch.get) {
                            Log.d("PME FRAGEMENT: ", "switch on for " + currentDevice.getAddress());
                            mBluetoothLeService.setCharacteristicNotification(mBluetoothLeService.mClassificationCharacteristic, true);
                            viewHolder.mDetectionSwitch.setChecked(isChecked);
                        } else {
                            Log.d("PME FRAGEMENT: ", "switch off for " + mDeviceAddress);
                            mBluetoothLeService.setCharacteristicNotification(mBluetoothLeService.mClassificationCharacteristic, false);
                            viewHolder.mDetectionSwitch.setChecked(!isChecked);
                        }
                        */
                    }
                });
            }
            else {
                Log.d(TAG, "button is null");
            }

            return view;
        }
    }

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            Log.e(TAG, "onServiceConnected ///////////////////////////////");
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
            mBluetoothLeService.mLeServiceList.add(mBluetoothLeService);
            mResultAdapter = new ListViewAdapter(mBluetoothLeService.mGattList);
            setListAdapter(mResultAdapter);
            for (int i = 0; i < mBluetoothLeService.mDevices.size(); i++) {
                Log.d(TAG, "BLE added " + mBluetoothLeService.mDevices.get(i).getAddress());
                Log.d("LeService // ", mBluetoothLeService.mGattList.get(i).getDevice().getAddress());
                mResultAdapter.addDevice(mBluetoothLeService.mDevices.get(i));
            }
            mConnected = true;
            connectionResult();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            final String mDeviceAddress = intent.getStringExtra(BluetoothLeService.EXTRA_DEVICE);
            Log.d("ACTION : ",  mDeviceAddress + "//////" + action);
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
            } else if (BluetoothLeService.TEMPERATURE_NOTIFICATION.equals(action)) {
                final String temperature = intent.getExtras().getString(EXTRA_DATA);
                TempChangeEvent(temperature);
            } else if (BluetoothLeService.HUMIDITY_NOTIFICATION.equals(action)) {
                final String humidity = intent.getExtras().getString(EXTRA_DATA);
                HumidChangeEvent(humidity);
            } else if (BluetoothLeService.CLASSIFICATION_NOTIFICATION.equals(action)) {
                final String motion0 = intent.getExtras().getString(BluetoothLeService.EXTRA_DATA_CLASSIFICATION_0);
                final String motion1 = intent.getExtras().getString(BluetoothLeService.EXTRA_DATA_CLASSIFICATION_1);
                final String motion2 = intent.getExtras().getString(BluetoothLeService.EXTRA_DATA_CLASSIFICATION_2);
                final String motion3 = intent.getExtras().getString(BluetoothLeService.EXTRA_DATA_CLASSIFICATION_3);
                MotionChangeEvent(motion0, motion1, motion2, motion3);
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
            }
        }
    };

    // If a given GATT characteristic is selected, check for supported features.  This sample
    // demonstrates 'Read' and 'Notify' features.  See
    // http://d.android.com/reference/android/bluetooth/BluetoothGatt.html for the complete
    // list of supported characteristic features.
    private final ExpandableListView.OnChildClickListener servicesListClickListner =
            new ExpandableListView.OnChildClickListener() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    if (mGattCharacteristics != null) {
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            // If there is an active notification on a characteristic, clear
                            // it first so it doesn't update the data field on the user interface.
                            if (mNotifyCharacteristic != null) {
//                                mBluetoothLeService.setCharacteristicNotification(
//                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                            mNotifyCharacteristic = characteristic;
                            Log.d(TAG, characteristic + "clicked");
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.result_layout);

        final Intent intent = getIntent();
        mDeviceName = new String(intent.getStringExtra(EXTRAS_DEVICE_NAME));
        mDeviceAddress = new String(intent.getStringExtra(EXTRAS_DEVICE_ADDRESS));
        mp = MediaPlayer.create(DeviceControlActivity.this, R.raw.daehanminkook);
        mp1 = MediaPlayer.create(DeviceControlActivity.this, R.raw.clap);

        /*
        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListner);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        addButton = (Button) findViewById(R.id.add);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(DeviceControlActivity.this, MainActivity.class));
//                mStartActivity = new StartActivity();
//                mStartActivity.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);
            }
        });

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        */

        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mGattUpdateReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mGattUpdateReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        Log.d("TAG", "register_receiver_done");
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }


    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            case R.id.menu_add:
                mStartActivity = new StartActivity();
                mStartActivity.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, 0);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothLeService.TEMPERATURE_NOTIFICATION);
        intentFilter.addAction(BluetoothLeService.HUMIDITY_NOTIFICATION);
        intentFilter.addAction(BluetoothLeService.CLASSIFICATION_NOTIFICATION);
        intentFilter.addAction(BluetoothLeService.MOTION_NOTIFICATION);
        return intentFilter;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    public void changeCharacterValue(BluetoothGattCharacteristic characteristic) {
        Log.d("Character : ",  "changed");
        characteristic.setValue(1, android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8, 0);
        Log.v("Character Value", String.valueOf(characteristic.getValue()));
    }

    public void TempChangeEvent(String temperature) {
        Log.d("TMP received",  mDeviceAddress + "//////" + temperature);
    }

    public void HumidChangeEvent(String humidity) {
        Log.d("HUM received",  mDeviceAddress + "//////" + humidity);
    }

    public void MotionChangeEvent(String status1, String status2, String status3, String status4) {
        Log.d("MOT received",  mDeviceAddress + "//////" + status1 + status2 + status3 + status4);

        if(status3.equals("6") && !mp.isPlaying()) {
            num++;
            result_img.setImageResource(R.drawable.daehanminkuk);
            if(mp.isPlaying())
                num = 0;
            if(num > 3  && !mp.isPlaying()) {
                num = 0;
                mp.start();
            }
        } else if(status3.equals("3") && !mp1.isPlaying()) {
            mp1.start();
        }
    }

    class StartActivity extends AsyncTask {

        @Override
        protected Object doInBackground(Object[] objects) {
            startActivity(new Intent(DeviceControlActivity.this, DeviceScanActivity.class));
            return null;
        }
    }

    private class ViewHolder {
        public Toolbar mResultToolbar;
        public Switch mDetectionSwitch;
        public TextView mDetectionTextView;
    }

    public void connectionResult() {
        if(mConnected)
            Toast.makeText(getApplicationContext(), "Connect Success", Toast.LENGTH_LONG).show();
        else
            Toast.makeText(getApplicationContext(), "Connect Fail", Toast.LENGTH_LONG).show();
    }
}
