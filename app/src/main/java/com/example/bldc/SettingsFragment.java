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
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public class SettingsFragment extends PreferenceFragmentCompat {

    private final String TAG = "SettingsFragment";

    private EditTextPreference powerLimitPreference;
    private EditTextPreference voltageLimitPreference;
    private BluetoothService BTService;
    private DBHelper dbHelper;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.root_preferences, rootKey);
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
    public void onDestroy() {
        super.onDestroy();
        getActivity().unbindService(myConnection);
        Log.d(TAG,"Unbind");
    }

    private MonitorService mMonitorService;
    public ServiceConnection myConnection = new ServiceConnection() {

        public void onServiceConnected(ComponentName className, IBinder binder) {
            mMonitorService = ((MonitorService.LocalBinder) binder).getService();
            Object obj = mMonitorService.getHandler();
            Log.d(TAG,"Connected to monitor service, handler="+obj);
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(TAG,"Monitor service disconnected");
        }
    };

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        powerLimitPreference = findPreference("power_limit");
        powerLimitPreference.setText(String.valueOf(dbHelper.getInfo(Constants.MAX_POWER_DRAW)));
        powerLimitPreference.setOnPreferenceChangeListener(new customPreferenceListener(Constants.MAX_POWER_DRAW));
    }

    private class customPreferenceListener implements Preference.OnPreferenceChangeListener
    {
        String preferenceConst;

        public customPreferenceListener(String prefConst)
        {
            preferenceConst = prefConst;
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String command = preferenceConst + "=" + (String) newValue + "&";
            byte[] bytes = command.getBytes();
            mMonitorService.write(bytes);
            return true;
        }
    }

}