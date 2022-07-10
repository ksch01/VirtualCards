package com.example.virtualcards.network.bluetooth.interfaces;

import java.util.Set;
import java.util.UUID;

public interface MessageTransmitter {
    /**
     * Sends the specified data to every entity associated as a receiver by this transmitter.
     * @param data
     */
    void send(byte[] data);

    /**
     * Sends the specified data to the entity associated as a receiver by this transmitter which also is identified by the specified UUID.
     * If there happens to be no such entity, the message is discarded.
     * @param target UUID associated with the desired target
     * @param data
     */
    void send(UUID target, byte[] data);

    /**
     * Returns the UUID which identifies this transmitter's receiver counterpart if existing.
     * If there is no receiver counterpart this method should return null.
     * @return unique receiver id
     */
    UUID getId();
}
