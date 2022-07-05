package com.example.virtualcards.network.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

class BluetoothClientThread extends Thread {

    private final String TAG = "BluetoothClientThread";

    private final BluetoothSocket socket;
    private final BluetoothDevice device;

    private final BluetoothNetwork network;
    private final Handler handler;

    public BluetoothClientThread(@NonNull BluetoothNetwork network, @NonNull BluetoothDevice device) {
        super();

        this.network = network;
        handler = network.getHandler();

        this.device = device;
        BluetoothSocket socket = null;

        try {
            if (ActivityCompat.checkSelfPermission(network.getActivity(), Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                network.getActivity().requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BluetoothNetwork.REQUEST_BLUETOOTH_CONNECT);
            }
            socket = device.createRfcommSocketToServiceRecord(BluetoothNetwork.BLUETOOTH_ID);
        } catch (IOException e) {
            Log.e(TAG, "Socket could not be created.", e);
        }
        this.socket = socket;
    }

    @Override
    public void run() {
        if (ActivityCompat.checkSelfPermission(network.getActivity(), Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            network.getActivity().requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, BluetoothNetwork.REQUEST_BLUETOOTH_SCAN);
        }
        network.stopDiscoverDevices();

        try{
            socket.connect();

            network.connected(socket);
            handler.obtainMessage(BluetoothNetwork.HANDLER_TYPE_CONNECTED, socket.getRemoteDevice()).sendToTarget();

            Log.i(TAG, "Successfully connected to target device.");
        }catch (IOException connectException){
            Log.d(TAG, "Could not connect to socket. Closing socket ...");

            cancel();
            handler.obtainMessage(BluetoothNetwork.HANDLER_TYPE_CONNECTION_FAILED, socket.getRemoteDevice()).sendToTarget();

            return;
        }
    }

    public void cancel(){
        try{
            socket.close();
        }catch (IOException e){
            Log.e(TAG, "Could not close the client socket.", e);
        }
    }
}
