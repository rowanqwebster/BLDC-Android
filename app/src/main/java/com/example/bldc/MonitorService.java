/*
 * Copyright (C) 2014 The Android Open Source Project
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
 *
 * FILE MODIFIED FROM BluetoothChatService.java TO SUIT THE NEEDS OF THIS PROJECT
 * Converted to service, ConnectedThread functionality changed to read strings, removed listening functionality
 *
 */

package com.example.bldc;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.UUID;

public class MonitorService extends Service {

    private static final String TAG = "MonitorService";
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter mAdapter;
    private Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private int mState;
    private int mNewState;

    // Constants that indicate the current connection state
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device

    private DBHelper dbHelper;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service started");
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        dbHelper = new DBHelper(this);
        dbHelper.resetInfo();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private final IBinder myBinder = new LocalBinder();

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        MonitorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return MonitorService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "OnStartCommand()");
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mState = STATE_NONE;
        mNewState = mState;
        return super.onStartCommand(intent, flags, startId);
    }

    public void setHandler(Handler handler)
    {
        mHandler = handler;
    }

    /**
     * Update UI title according to the current state of the chat connection
     */
    private synchronized void updateUserInterfaceTitle() {
        mState = getState();
        Log.d(TAG, "updateUserInterfaceTitle() " + mNewState + " -> " + mState);
        mNewState = mState;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, mNewState, -1).sendToTarget();
    }

    /**
     * Return the current connection state.
     */
    public synchronized int getState() {
        return mState;
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param device The BluetoothDevice to connect
     */
    public synchronized void connect(BluetoothDevice device) {
        Log.d(TAG, "connect to: " + device);

        // Cancel any thread attempting to make a connection
        if (mState == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     *
     * @param socket The BluetoothSocket on which the connection was made
     * @param device The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket, BluetoothDevice
            device) {
        Log.d(TAG, "connected");

        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // Send the name of the connected device back to the UI Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Update UI title
        updateUserInterfaceTitle();
    }



    @Override
    public void onDestroy() {
        super.onDestroy();
        MonitorService.this.stop();
    }

    /**
     * Write to the ConnectedThread in an unsynchronized manner
     *
     * @param out The bytes to write
     * @see ConnectedThread#write(byte[])
     */
    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (mState != STATE_CONNECTED) {
                Log.d(TAG, "Could not write, not connected");
                return;
            }
            r = mConnectedThread;
        }
        // Perform the write unsynchronized
        r.write(out);
    }

    /**
     * Stop all threads
     */
    public synchronized void stop() {
        Log.d(TAG, "stop");

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
        msg.setData(bundle);        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        MonitorService.this.stop();
    }


    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        mState = STATE_NONE;
        // Update UI title
        updateUserInterfaceTitle();

        // Start the service over to restart listening mode
        MonitorService.this.stop();
    }


    // New Class for Connecting Thread
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createInsecureRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket's create() method failed.", e);
            }
            mmSocket = tmp;
            mState = STATE_CONNECTING;
        }

        @Override
        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            mAdapter.cancelDiscovery();

            try {
                mmSocket.connect();
            }
            catch (IOException e)
            {
                try{
                    mmSocket.close();
                }
                catch (IOException e2)
                {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            synchronized (MonitorService.this)
            {
                mConnectThread = null;
            }

            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    // New Class for Connected Thread
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            mState = STATE_CONNECTED;
        }

        public void run() {
            Log.i(TAG,"Begin mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;
            StringBuilder stringBuilder = new StringBuilder();

            while (mState == STATE_CONNECTED)
            {
                try {
                    bytes = mmInStream.read(buffer);
                    String partialMessage = new String(buffer, 0, bytes);
                    stringBuilder.append(partialMessage);
                    while (stringBuilder.indexOf("&") >= 0) {
                        int index = stringBuilder.indexOf("&");
                        String readMessage = stringBuilder.substring(0, index);
                        mHandler.obtainMessage(Constants.MESSAGE_READ, -1, -1, readMessage).sendToTarget();
                        updateDB(readMessage);
                        //Log.i(TAG, "Received string: " + readMessage);
                        stringBuilder.delete(0, index + 1);
                    }
                }
                catch (IOException e){
                    Log.e(TAG, "disconnected.", e);
                    connectionLost();
                    break;
                }
            }
        }

        private void updateDB(String message){
            StringTokenizer token = new StringTokenizer(message, "=");
            if (token.countTokens() == 2)
            {
                String key = token.nextToken();
                String val = token.nextToken();
                try {
                    float fVal = Float.parseFloat(val);
                    dbHelper.setInfo(key, fVal);
                }
                catch (NumberFormatException e)
                {
                    Log.d(TAG, "Invalid value formatting");
                }
            }
            else
            {
                Log.d(TAG, "Received non-parsable information from MCU");
            }
        }

        //write method
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);//write bytes over BT connection via out stream
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer).sendToTarget();
                Log.i(TAG, "Sent string: " + Arrays.toString(buffer));
            } catch (IOException e){
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
