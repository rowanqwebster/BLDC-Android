package com.example.bldc.ui.dashboard;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import com.example.bldc.R;
import com.example.bldc.ui.home.HomeFragment;

import java.io.IOException;
import java.util.UUID;

public class DashboardFragment extends Fragment {

    private DashboardViewModel dashboardViewModel;
    private String address;

    Button btn1, btn2, btn3, btn4, btn5, btnDisconnect, btn6;
    TextView lumn;

    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        dashboardViewModel =
                new ViewModelProvider(this).get(DashboardViewModel.class);
        View root = inflater.inflate(R.layout.fragment_dashboard, container, false);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        address = getArguments().getString(HomeFragment.EXTRA_ADDRESS);

        btn1 = getView().findViewById(R.id.button2);
        btn2 = getView().findViewById(R.id.button3);
        btn3 = getView().findViewById(R.id.button5);
        btn4 = getView().findViewById(R.id.button6);
        btn5 = getView().findViewById(R.id.button7);
        btn6 = getView().findViewById(R.id.button8);
        btnDisconnect = getView().findViewById(R.id.button4);
        lumn = getView().findViewById(R.id.textView2);

        new ConnectBT().execute();

        btn1.setOnClickListener(v -> sendSignal(13));

        btn2.setOnClickListener(v -> sendSignal(22));

        btn3.setOnClickListener(v -> sendSignal(46));

        btn4.setOnClickListener(v -> sendSignal(96));

        btn5.setOnClickListener(v -> sendSignal(189));

        btnDisconnect.setOnClickListener(v -> Disconnect());

        btn6.setOnClickListener(v -> receiveSignal());
    }

    private void sendSignal(int number)
    {
        msg("Sending: " + number);
        if ( btSocket != null )
        {
            try
            {
                btSocket.getOutputStream().write(number);
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    private void receiveSignal()
    {
        int rec = 0;
        if ( btSocket != null )
        {
            try
            {
                rec = btSocket.getInputStream().read();
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }

        lumn.setText(String.valueOf(rec));
    }

    private void Disconnect () {
        if ( btSocket!=null ) {
            try {
                btSocket.close();
            } catch(IOException e) {
                msg("Error");
            }
        }

        //finish();
    }

    private void msg (String s) {
        Toast.makeText(getActivity(), s, Toast.LENGTH_SHORT).show();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected void onPreExecute () {
            progress = ProgressDialog.show(getActivity(), "Connecting...", "Please Wait!!!");
        }

        @Override
        protected Void doInBackground (Void... devices) {
            try {
                if ( btSocket==null || !isBtConnected ) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice device = myBluetooth.getRemoteDevice(address);
                    btSocket = device.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute (Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                //finish();
            } else {
                msg("Connected");
                isBtConnected = true;
            }

            progress.dismiss();
        }
    }


}