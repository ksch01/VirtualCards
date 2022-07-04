package com.example.virtualcards.network;

import android.bluetooth.BluetoothDevice;

public interface DeviceReceiver {
    void received(BluetoothDevice device);
}
