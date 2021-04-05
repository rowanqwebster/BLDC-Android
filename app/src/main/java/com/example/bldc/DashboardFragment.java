package com.example.bldc;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.Random;

public class DashboardFragment extends Fragment {

    private final Boolean debug = true;
    private final String TAG = "DashboardFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 0;
    private static final int REQUEST_ENABLE_BT = 1;

    // Layout Views
    private ProgressBar mPowerProgress;
    private TextView mPowerIndicator;
    private ProgressBar mVoltageProgress;
    private TextView mVoltageIndicator;
    private ProgressBar mCurrentProgress;
    private TextView mCurrentIndicator;
    private ProgressBar mSpeedProgress;
    private TextView mSpeedIndicator;
    private ProgressBar mTempProgress;
    private TextView mTempIndicator;
    private ProgressBar mCapacityProgress;
    private TextView mCapacityIndicator;

    private Handler UIHandler;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (mBluetoothAdapter == null)
        {
            FragmentActivity activity = getActivity();
            Toast.makeText(activity, "Bluetooth not available on this device", Toast.LENGTH_LONG).show();
            if (activity != null) {
                activity.finish();
            }
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        if (!mBluetoothAdapter.isEnabled())
        {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        else if (mMonitorService == null){
            setupBT();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        UIHandler.removeCallbacksAndMessages(null);
        if (mMonitorService != null) {
            getActivity().unbindService(myConnection);
            getActivity().stopService(new Intent(getActivity(), MonitorService.class));
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mMonitorService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mMonitorService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mMonitorService.stop();
            }
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.bluetooth_chat, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.insecure_connect_scan) {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getActivity(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
        }
        else if (item.getItemId() == R.id.action_settings)
        {
            Intent settingsIntent = new Intent(getActivity(), SettingsActivity.class);
            startActivity(settingsIntent);
            return true;
        }
        else if (item.getItemId() == R.id.action_disconnect)
        {
            mMonitorService.stop();
            return true;
        }
        return false;
    }

    @Override
    public void onPrepareOptionsMenu(@NonNull Menu menu) {
        super.onPrepareOptionsMenu(menu);
        for (int i = 1; i<menu.size(); i++)
        {
            menu.getItem(i).setEnabled(connected || debug);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        View powerProgress = view.findViewById(R.id.power_progress);
        View voltageProgress = view.findViewById(R.id.voltage_progress);
        View currentProgress = view.findViewById(R.id.current_progress);
        View tempProgress = view.findViewById(R.id.temp_progress);
        View capacityProgress = view.findViewById(R.id.capacity_progress);

        ((TextView)powerProgress.findViewById(R.id.progress_label)).setText(getString(R.string.power_label));
        mPowerProgress = powerProgress.findViewById(R.id.progress);
        mPowerIndicator = powerProgress.findViewById(R.id.progress_indicator);

        ((TextView)voltageProgress.findViewById(R.id.progress_label)).setText(getString(R.string.voltage_label));
        mVoltageProgress = voltageProgress.findViewById(R.id.progress);
        mVoltageIndicator = voltageProgress.findViewById(R.id.progress_indicator);

        ((TextView)currentProgress.findViewById(R.id.progress_label)).setText(getString(R.string.current_label));
        mCurrentProgress = currentProgress.findViewById(R.id.progress);
        mCurrentIndicator = currentProgress.findViewById(R.id.progress_indicator);

        ((TextView)tempProgress.findViewById(R.id.progress_label)).setText(getString(R.string.temp_label));
        mTempProgress = tempProgress.findViewById(R.id.progress);
        mTempIndicator = tempProgress.findViewById(R.id.progress_indicator);

        ((TextView)capacityProgress.findViewById(R.id.progress_label)).setText(getString(R.string.capacity_label));
        mCapacityProgress = capacityProgress.findViewById(R.id.progress);
        mCapacityIndicator = capacityProgress.findViewById(R.id.progress_indicator);

        mSpeedProgress = view.findViewById(R.id.speed_progress);
        mSpeedIndicator = view.findViewById(R.id.speed_indicator);

        Random r = new Random();

        DBHelper dbHelper = new DBHelper(getActivity());
        UIHandler = new Handler(Looper.getMainLooper());
        UIHandler.post(new Runnable() {
            @Override
            public void run() {
                if (connected || debug) {
                    double speed = dbHelper.getInfo(Constants.SPEED);
                    mSpeedProgress.setProgress((int)speed);
                    mSpeedIndicator.setText(getString(R.string.speed_indicator, (int) speed));
                    double power = dbHelper.getInfo(Constants.POWER);
                    mPowerProgress.setProgress((int) power);
                    mPowerIndicator.setText(getString(R.string.power_indicator, power));
                    double current = dbHelper.getInfo(Constants.CURRENT);
                    mCurrentProgress.setProgress((int) current);
                    mCurrentIndicator.setText(getString(R.string.current_indicator, current));
                    double voltage = dbHelper.getInfo(Constants.BATTERY_VOLT);
                    mVoltageProgress.setProgress((int) voltage);
                    mVoltageIndicator.setText(getString(R.string.voltage_indicator, voltage));
                    double temp = dbHelper.getInfo(Constants.CONTROL_TEMP);
                    mTempProgress.setProgress((int) temp);
                    mTempIndicator.setText(getString(R.string.temp_indicator, temp));
                    double capacity = dbHelper.getInfo(Constants.BATTERY_REM);
                    mCapacityProgress.setProgress((int) capacity);
                    mCapacityIndicator.setText(getString(R.string.capacity_indicator, capacity));
                }

                UIHandler.postDelayed(this,100);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupBT();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getActivity(), R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
        }
    }

    /**
     * Set up the UI and background operations for chat.
     */
    private void setupBT() {
        Log.d(TAG, "setupChat()");

        getActivity().startService(new Intent(getActivity(), MonitorService.class));
        getActivity().bindService(new Intent(getActivity(), MonitorService.class), myConnection, Context.BIND_AUTO_CREATE);

    }

    private MonitorService mMonitorService;
    private final ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            mMonitorService = ((MonitorService.LocalBinder) binder).getService();
            mMonitorService.setHandler(mHandler);
            mMonitorService.stop();
            Log.d(TAG,"Connected to monitor service");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG,"Monitor service disconnected");
        }
    };

    /**
     * Updates the status on the action bar.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        if (activity instanceof AppCompatActivity)
        {
            AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
            final ActionBar actionBar = appCompatActivity.getSupportActionBar();
            if (null == actionBar) {
                return;
            }
            actionBar.setSubtitle(resId);
        }
    }

    /**
     * Updates the status on the action bar.
     *
     * @param subTitle status
     */
    private void setStatus(CharSequence subTitle) {
        FragmentActivity activity = getActivity();
        if (null == activity) {
            return;
        }
        if (activity instanceof AppCompatActivity)
        {
            AppCompatActivity appCompatActivity = (AppCompatActivity) activity;
            final ActionBar actionBar = appCompatActivity.getSupportActionBar();
            if (null == actionBar) {
                return;
            }
            actionBar.setSubtitle(subTitle);
        }
    }

    private boolean connected;
    
    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    connected = false;
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            connected = true;
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_NONE:
                            resetStats();
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != activity) {
                        Toast.makeText(activity, "Connected to " + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != activity) {
                        Toast.makeText(activity, msg.getData().getString(Constants.TOAST), Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link DeviceListActivity#EXTRA_DEVICE_ADDRESS} extra.
     */
    private void connectDevice(Intent data) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mMonitorService.connect(device);
    }

    private void resetStats()
    {
        mSpeedIndicator.setText("--");
        mCapacityIndicator.setText("--");
        mTempIndicator.setText("--");
        mPowerIndicator.setText("--");
        mVoltageIndicator.setText("--");
        mCurrentIndicator.setText("--");
    }

}