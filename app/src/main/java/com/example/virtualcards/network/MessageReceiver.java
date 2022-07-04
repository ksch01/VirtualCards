package com.example.virtualcards.network;

public interface MessageReceiver {
    void received(byte[] receivedBytes);
}
