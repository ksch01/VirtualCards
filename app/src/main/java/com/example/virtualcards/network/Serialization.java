package com.example.virtualcards.network;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.UUID;

public class Serialization {
    public static UUID asUUID(byte[] bytes){
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        long firstLong = bb.getLong();
        long secondLong = bb.getLong();
        return new UUID(firstLong, secondLong);
    }

    public static void putUUID(ByteBuffer buffer, UUID uuid){
        buffer.putLong(uuid.getMostSignificantBits());
        buffer.putLong(uuid.getLeastSignificantBits());
    }
}
