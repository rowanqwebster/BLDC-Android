package com.example.bldc;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
//import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i(TAG, "Starting application.");

        //if (savedInstanceState == null)
        //{
        //    FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        //    DashboardFragment fragment = new DashboardFragment();
        //    transaction.replace(R.id.sample_content_fragment, fragment);
        //    transaction.commit();
        //}
    }
}