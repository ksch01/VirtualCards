package com.example.virtualcards.network;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

public class NetworkData {

    public enum Operation {
        GET((byte)0, PayloadType.GAME_OBJECT),
        MOVE((byte)1, PayloadType.GAME_OBJECT_AND_COORDINATES),
        DROP((byte)2, PayloadType.GAME_OBJECT_AND_COORDINATES),
        HIT((byte)3, PayloadType.GAME_OBJECT),
        SHUFFLE((byte)4, PayloadType.GAME_OBJECTS),
        EXTRACT((byte)5, PayloadType.GAME_OBJECTS);

        Operation(byte id, PayloadType payload){
            this.id = id;
            this.payload = payload;
        }
        private static Operation getOperation(byte id){
            switch (id){
                case 0: return GET;
                case 1: return MOVE;
                case 2: return DROP;
                case 3: return HIT;
                case 4: return SHUFFLE;
                case 5: return EXTRACT;
            }
            throw new IllegalArgumentException("There is no operation with id: "+id);
        }
        private final byte id;
        protected final PayloadType payload;
    }
    private enum PayloadType {
        GAME_OBJECT,
        GAME_OBJECT_AND_COORDINATES,
        GAME_OBJECTS
    }
    public static class Payload{
        public final Operation operation;
        public final float x, y;
        private final UUID[] gameObjects;

        private Payload(Operation operation, UUID[] gameObjects, float x, float y){
            this.operation = operation;
            this.x = x;
            this.y = y;
            this.gameObjects = gameObjects;
        }

        public UUID getObject(int index){
            return gameObjects[index];
        }
    }

    static Payload deserialize(byte[] data){
        ByteBuffer buffer = ByteBuffer.wrap(data);
        Operation operation = Operation.getOperation(buffer.get());
        switch(operation.payload){
            case GAME_OBJECT: return getPayloadObject(operation, buffer);
            case GAME_OBJECTS: return getPayloadObjects(operation, buffer);
            case GAME_OBJECT_AND_COORDINATES: return getPayloadObjectCoords(operation, buffer);
        }
        throw new IllegalArgumentException("Payload could not be deserialized from data");
    }

    private static Payload getPayloadObject(Operation operation, ByteBuffer buffer){
        return new Payload(operation, new UUID[]{getUUID(buffer)}, Float.MIN_VALUE, Float.MIN_VALUE);
    }

    private static Payload getPayloadObjectCoords(Operation operation, ByteBuffer buffer){
        return new Payload(operation, new UUID[]{getUUID(buffer)}, buffer.getFloat(), buffer.getFloat());
    }

    private static Payload getPayloadObjects(Operation operation, ByteBuffer buffer){
        ArrayList<UUID> gameObjects = new ArrayList<>();
        while(buffer.hasRemaining()){
            gameObjects.add(getUUID(buffer));
        }
        return new Payload(operation, gameObjects.toArray(new UUID[0]), Float.MIN_VALUE, Float.MIN_VALUE);
    }

    private static UUID getUUID(ByteBuffer buffer){
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    static byte[] serialize(Operation operation, UUID gameObjectId){
        assert operation.payload == PayloadType.GAME_OBJECT : "Get data for operation \""+operation.name()+"\" called for wrong parameters.";

        byte[] data = new byte[17];
        ByteBuffer buffer = ByteBuffer.wrap(data);

        buffer.put(operation.id);
        buffer.putLong(gameObjectId.getMostSignificantBits());
        buffer.putLong(gameObjectId.getLeastSignificantBits());

        return data;
    }

    static byte[] serialize(Operation operation, UUID gameObjectId, float x, float y){
        assert operation.payload == PayloadType.GAME_OBJECT_AND_COORDINATES : "Get data for operation \""+operation.name()+"\" called for wrong parameters.";

        byte[] data = new byte[25];
        ByteBuffer buffer = ByteBuffer.wrap(data);

        buffer.put(operation.id);
        buffer.putLong(gameObjectId.getMostSignificantBits());
        buffer.putLong(gameObjectId.getLeastSignificantBits());
        buffer.putFloat(x);
        buffer.putFloat(y);

        return data;
    }

    static byte[] serialize(Operation operation, UUID... gameObjects){
        assert operation.payload == PayloadType.GAME_OBJECTS : "Get data for operation \""+operation.name()+"\" called for wrong parameters.";

        byte[] data = new byte[16 * gameObjects.length + 1];
        ByteBuffer buffer = ByteBuffer.wrap(data);

        buffer.put(operation.id);
        for(UUID uuid : gameObjects){
            buffer.putLong(uuid.getMostSignificantBits());
            buffer.putLong(uuid.getLeastSignificantBits());
        }

        return data;
    }
}
