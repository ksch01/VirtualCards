package com.example.virtualcards.network.bluetooth.interfaces;

import android.bluetooth.BluetoothDevice;

public interface DeviceReceiver {
    void received(BluetoothDevice device);
}
