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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import java.util.StringTokenizer;

public class DashboardFragment extends Fragment {

    private final String TAG = "DashboardFragment";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    // Layout Views
    private ListView mConversationView;
    private EditText mOutEditText;
    private Button mSendButton;
    private ProgressBar mPowerProgress;
    private TextView mPowerIndicator;
    private ProgressBar mVoltageProgress;
    private TextView mVoltageIndicator;
    private ProgressBar mCurrentProgress;
    private TextView mCurrentIndicator;

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    /**
     * Array adapter for the conversation thread
     */
    private ArrayAdapter<String> mConversationArrayAdapter;

    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    //private BluetoothService mBTService = null;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mConversationView = view.findViewById(R.id.in);
        mOutEditText = view.findViewById(R.id.edit_text_out);
        mSendButton = view.findViewById(R.id.button_send);

        mPowerProgress = view.findViewById(R.id.powerProgress);
        mPowerIndicator = view.findViewById(R.id.powerInd);
        mVoltageProgress = view.findViewById(R.id.voltageProgress);
        mVoltageIndicator = view.findViewById(R.id.voltageInd);
        mCurrentProgress = view.findViewById(R.id.currentProgress);
        mCurrentIndicator = view.findViewById(R.id.currentInd);
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

        // Initialize the array adapter for the conversation thread
        mConversationArrayAdapter = new ArrayAdapter<>(getActivity(), R.layout.message);

        mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        mSendButton.setOnClickListener(v -> {
            // Send a message using content of the edit text widget
            View view = getView();
            if (null != view) {
                TextView textView = view.findViewById(R.id.edit_text_out);
                String message = textView.getText().toString();
                sendMessage(message);
            }
        });

        getActivity().startService(new Intent(getActivity(), MonitorService.class));
        getActivity().bindService(new Intent(getActivity(), MonitorService.class), myConnection, Context.BIND_AUTO_CREATE);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer();
    }

    private MonitorService mMonitorService;
    public ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            mMonitorService = ((MonitorService.LocalBinder) binder).getService();
            mMonitorService.setHandler(mHandler);
            Log.d(TAG,"Connected to monitor service");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG,"Monitor service disconnected");
        }
    };

    /**
     * Sends a message.
     *
     * @param message A string of text to send.
     */
    private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mMonitorService.getState() != MonitorService.STATE_CONNECTED)
        {
            Toast.makeText(getActivity(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            message = "pwm freq=" + message + "&";
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mMonitorService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
            mOutEditText.setText(mOutStringBuffer);
        }
    }

    /**
     * The action listener for the EditText widget, to listen for the return key
     */
    private final TextView.OnEditorActionListener mWriteListener = (view, actionId, event) -> {
        // If the action is a key-up event on the return key, send the message
        if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
            String message = view.getText().toString();
            sendMessage(message);
        }
        return true;
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



    /**
     * The Handler that gets information back from the BluetoothChatService
     */
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            FragmentActivity activity = getActivity();
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, mConnectedDeviceName));
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case Constants.MESSAGE_READ:
                    String readMessage = (String) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    parseData(readMessage);
                    mConversationArrayAdapter.add(mConnectedDeviceName + ":  " + readMessage);
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

    private void parseData(String input)
    {
        StringTokenizer token = new StringTokenizer(input, "=");
        if (token.countTokens() == 2)
        {
            String key = token.nextToken();
            String val = token.nextToken();
            switch (key) {
                case Constants.POWER:
                    mPowerIndicator.setText(getString(R.string.power_indicator, val));
                    mPowerProgress.setProgress((int)Float.parseFloat(val));
                    break;
                case Constants.CURRENT:
                    mCurrentIndicator.setText(getString(R.string.current_indicator, val));
                    mCurrentProgress.setProgress((int)Float.parseFloat(val));
                    break;
                case Constants.VOLTAGE:
                    mVoltageIndicator.setText(getString(R.string.voltage_indicator, val));
                    mVoltageProgress.setProgress((int)Float.parseFloat(val));
                    break;
                default:
                    Log.d(TAG,"Unexpected key value: " + key);
            }
        }
        else
        {
            Log.d(TAG, "Received non-parsable information from MCU");
        }
    }


}