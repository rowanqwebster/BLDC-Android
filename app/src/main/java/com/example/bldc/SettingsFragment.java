package com.example.bldc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

public class SettingsFragment extends PreferenceFragmentCompat {

    private final String TAG = "SettingsFragment";

    private SwitchPreference battFlagPreference;
    private SeekBarPreference powerLimitPreference;
    private SeekBarPreference currentLimitPreference;
    private SeekBarPreference pwmFreqPreference;
    private SeekBarPreference batteryNumPreference;
    private SeekBarPreference speedLimitPreference;
    private ListPreference driveModePreference;
    private ListPreference battChemPreference;
    private BluetoothService BTService;
    private DBHelper dbHelper;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
        Log.i(TAG, "ocp");
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().bindService(new Intent(getActivity(), MonitorService.class), myConnection, Context.BIND_AUTO_CREATE);
        Log.d(TAG, "Bind");

        dbHelper = new DBHelper(getActivity());
        Log.i(TAG, String.valueOf(dbHelper.getInfo(Constants.BATTERY_VOLT)));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        battFlagPreference = findPreference("battery_connected");
        if (battFlagPreference != null){
            battFlagPreference.setChecked((int)dbHelper.getInfo(Constants.BATTERY_FLAG)==1);
            battFlagPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String command = Constants.BATTERY_FLAG + "=" + ((boolean) newValue ? 1 : 0) + "&";
                mMonitorService.write(command.getBytes());
                Log.d(TAG, command);
                return true;
            });
        }
        batteryNumPreference = findPreference("batt_num");
        if (batteryNumPreference != null) {
            batteryNumPreference.setMin(6);
            batteryNumPreference.setValue((int) dbHelper.getInfo(Constants.BATTERY_CELLS));
            batteryNumPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.BATTERY_CELLS));
        }
        pwmFreqPreference = findPreference("pwm_freq");
        if (pwmFreqPreference !=null)
        {
            pwmFreqPreference.setMin(2);
            pwmFreqPreference.setValue((int) dbHelper.getInfo(Constants.PWM_FREQ));
            pwmFreqPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.PWM_FREQ));
        }
        powerLimitPreference = findPreference("power_limit");
        if (powerLimitPreference != null)
        {
            powerLimitPreference.setValue((int) dbHelper.getInfo(Constants.MAX_POWER_DRAW));
            powerLimitPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.MAX_POWER_DRAW));
        }
        currentLimitPreference = findPreference("current_limit");
        if (currentLimitPreference != null)
        {
            currentLimitPreference.setValue((int) dbHelper.getInfo(Constants.MAX_CURRENT_DRAW));
            currentLimitPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.MAX_CURRENT_DRAW));
        }
        speedLimitPreference = findPreference("speed_limit");
        if (speedLimitPreference != null)
        {
            speedLimitPreference.setValue((int) dbHelper.getInfo(Constants.MAX_SPEED));
            speedLimitPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.MAX_SPEED));
        }
        driveModePreference = findPreference("driving_mode");
        if (driveModePreference != null)
        {
            driveModePreference.setValue(String.valueOf((int)dbHelper.getInfo(Constants.DRIVING_MODE)));
            driveModePreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.DRIVING_MODE));
        }
        battChemPreference = findPreference("battery_chemistry");
        if (battChemPreference != null)
        {
            battChemPreference.setValue(String.valueOf((int)dbHelper.getInfo(Constants.BATTERY_CHEMISTRY)));
            battChemPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.BATTERY_CHEMISTRY));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(myConnection);
        Log.d(TAG,"Unbind");
    }

    private MonitorService mMonitorService;
    public ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            mMonitorService = ((MonitorService.LocalBinder) binder).getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG,"Monitor service disconnected");
        }
    };

    private class customPreferenceListener implements Preference.OnPreferenceChangeListener
    {
        String preferenceConst;

        public customPreferenceListener(String prefConst)
        {
            preferenceConst = prefConst;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String command = preferenceConst + "=" + newValue + "&";
            mMonitorService.write(command.getBytes());
            Log.d(TAG, command);
            return true;
        }
    }
}