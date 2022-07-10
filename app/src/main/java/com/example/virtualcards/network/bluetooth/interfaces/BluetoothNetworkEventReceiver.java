package com.example.virtualcards.network.bluetooth.interfaces;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

public interface BluetoothNetworkEventReceiver {
    void receive(int eventCode, UUID recieverId, BluetoothDevice device);
}
