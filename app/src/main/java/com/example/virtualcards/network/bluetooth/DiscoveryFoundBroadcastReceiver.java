package com.example.virtualcards.network.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

class DiscoveryFoundBroadcastReceiver extends BroadcastReceiver {

    private BluetoothNetwork network;

    public DiscoveryFoundBroadcastReceiver(@NonNull BluetoothNetwork network){
        this.network = network;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(BluetoothDevice.ACTION_FOUND.equals(action)){
            network.discovered(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
        }
    }
}
