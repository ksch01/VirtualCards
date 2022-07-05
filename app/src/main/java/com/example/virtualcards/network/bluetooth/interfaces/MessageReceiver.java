package com.example.virtualcards.network.bluetooth.interfaces;

import java.nio.ByteBuffer;

public interface MessageReceiver {
    void receive(ByteBuffer receivedBytes);
}
