package com.example.virtualcards.network.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;

class BluetoothServerThread extends Thread {

    private final String TAG = "BluetoothServerThread";

    private final BluetoothServerSocket serverSocket;

    private final BluetoothNetwork network;
    private final Handler handler;

    public BluetoothServerThread(@NonNull BluetoothNetwork network) throws IOException {
        super();

        this.network = network;
        handler = network.getHandler();

        AppCompatActivity activity = network.getActivity();
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT}, BluetoothNetwork.REQUEST_BLUETOOTH_CONNECT);
        }
        serverSocket = network.getBluetoothAdapter().listenUsingInsecureRfcommWithServiceRecord(BluetoothNetwork.BLUETOOTH_APP_NAME, BluetoothNetwork.BLUETOOTH_ID);
    }

    @Override
    public void run(){
        while(true){
            BluetoothSocket socket;
            try{
                socket = serverSocket.accept();
            }catch (IOException e){
                Log.d(TAG, "Server socket accept failed.");
                break;
            }

            if(socket != null){
                Log.i(TAG, "Device has successfully connected.");
                network.connected(socket);
                handler.obtainMessage(BluetoothNetwork.HANDLER_TYPE_CONNECTED, socket.getRemoteDevice()).sendToTarget();
            }
        }
        network.serverClosed();
    }

    public void cancel(){
        try{
            serverSocket.close();
        }catch (IOException e){
            Log.e(TAG, "Could not close the connect socket", e);
        }
    }
}
