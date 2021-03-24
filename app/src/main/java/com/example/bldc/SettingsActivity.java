package com.example.bldc;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class SettingsActivity extends AppCompatActivity {

    private final String TAG = "SettingsActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();

        bindService(new Intent(this, MonitorService.class), myConnection, Context.BIND_AUTO_CREATE);

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
}