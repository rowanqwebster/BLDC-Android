package com.example.bldc;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SeekBarPreference;
import androidx.preference.SwitchPreference;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat {

    private final String TAG = "SettingsFragment";

    private DBHelper dbHelper;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActivity().bindService(new Intent(getActivity(), MonitorService.class), myConnection, Context.BIND_AUTO_CREATE);
        dbHelper = new DBHelper(getActivity());
    }

    public static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditTextPreference currentLimitPref = findPreference("current_limit_2");
        if (currentLimitPref != null)
        {
            currentLimitPref.setText(String.format(Locale.getDefault(),"%.2f", dbHelper.getInfo(Constants.MAX_CURRENT_DRAW)));
            currentLimitPref.setOnBindEditTextListener(editText -> editText.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL));
            currentLimitPref.setOnPreferenceChangeListener((preference, newValue) -> {
                float clampedValue = clamp(Float.parseFloat(newValue.toString()),0,5);
                boolean valid = Float.parseFloat(newValue.toString()) == clampedValue;
                if (!valid)
                {
                    Toast.makeText(getActivity(),"Invalid value", Toast.LENGTH_SHORT).show();
                }
                else {
                    String command = Constants.MAX_CURRENT_DRAW + "=" + Float.parseFloat(newValue.toString()) + "&";
                    mMonitorService.write(command.getBytes());
                    Log.d(TAG, command);
                }
                return valid;
            });
        }
        SwitchPreference battFlagPreference = findPreference("battery_connected");
        if (battFlagPreference != null){
            battFlagPreference.setChecked((int)dbHelper.getInfo(Constants.BATTERY_FLAG)==1);
            battFlagPreference.setOnPreferenceChangeListener((preference, newValue) -> {
                String command = Constants.BATTERY_FLAG + "=" + ((boolean) newValue ? 1 : 0) + "&";
                mMonitorService.write(command.getBytes());
                Log.d(TAG, command);
                return true;
            });
        }
        SeekBarPreference batteryNumPreference = findPreference("batt_num");
        if (batteryNumPreference != null) {
            batteryNumPreference.setMin(6);
            batteryNumPreference.setValue((int) dbHelper.getInfo(Constants.BATTERY_CELLS));
            batteryNumPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.BATTERY_CELLS));
        }
        SeekBarPreference pwmFreqPreference = findPreference("pwm_freq");
        if (pwmFreqPreference !=null)
        {
            pwmFreqPreference.setMin(2);
            pwmFreqPreference.setValue((int) dbHelper.getInfo(Constants.PWM_FREQ));
            pwmFreqPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.PWM_FREQ));
        }
        SeekBarPreference powerLimitPreference = findPreference("power_limit");
        if (powerLimitPreference != null)
        {
            powerLimitPreference.setValue((int) dbHelper.getInfo(Constants.MAX_POWER_DRAW));
            powerLimitPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.MAX_POWER_DRAW));
        }
        SeekBarPreference currentLimitPreference = findPreference("current_limit");
        if (currentLimitPreference != null)
        {
            currentLimitPreference.setValue((int) dbHelper.getInfo(Constants.MAX_CURRENT_DRAW));
            currentLimitPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.MAX_CURRENT_DRAW));
        }
        SeekBarPreference speedLimitPreference = findPreference("speed_limit");
        if (speedLimitPreference != null)
        {
            speedLimitPreference.setValue((int) dbHelper.getInfo(Constants.MAX_SPEED));
            speedLimitPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.MAX_SPEED));
        }
        ListPreference driveModePreference = findPreference("driving_mode");
        if (driveModePreference != null)
        {
            driveModePreference.setValue(String.valueOf((int)dbHelper.getInfo(Constants.DRIVING_MODE)));
            driveModePreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.DRIVING_MODE));
        }
        ListPreference battChemPreference = findPreference("battery_chemistry");
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