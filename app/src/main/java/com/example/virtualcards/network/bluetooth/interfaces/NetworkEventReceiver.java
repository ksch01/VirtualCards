package com.example.virtualcards.network.bluetooth.interfaces;

import android.bluetooth.BluetoothDevice;

import java.util.UUID;

public interface NetworkEventReceiver {
    /**
     * Network event codes
     */
    int EVENT_CODE_DISCOVERED = 0,
            EVENT_CODE_CONNECTED = 1,
            EVENT_CODE_DISCONNECTED = 2,
            EVENT_CODE_CONNECTION_FAILED = 3;

    void receive(int eventCode, UUID receiverId, BluetoothDevice device);
}
