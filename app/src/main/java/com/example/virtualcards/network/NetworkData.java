package com.example.virtualcards.network;

import android.util.Log;

import com.example.virtualcards.model.Card;
import com.example.virtualcards.model.CardStack;
import com.example.virtualcards.model.GameObject;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class NetworkData {

    public enum Operation {
        RESERVE((byte)0, PayloadType.GAME_OBJECT_AND_PLAYER, false),
        MOVE((byte)1, PayloadType.GAME_OBJECT_AND_COORDINATES, true),
        DROP((byte)2, PayloadType.GAME_OBJECT_AND_COORDINATES, false),
        HIT((byte)3, PayloadType.GAME_OBJECT, false),
        STACK((byte)4, PayloadType.GAME_OBJECTS, false),
        EXTRACT((byte)5, PayloadType.GAME_OBJECT_AND_PLAYER, false),
        SYNC((byte)6, PayloadType.GAME_OBJECTS_FULL, false);

        Operation(byte id, PayloadType payload, boolean optional){
            this.id = id;
            this.payload = payload;
            this.isDispensable = optional;
        }
        private static Operation getOperation(byte id){
            switch (id){
                case 0: return RESERVE;
                case 1: return MOVE;
                case 2: return DROP;
                case 3: return HIT;
                case 4: return STACK;
                case 5: return EXTRACT;
                case 6: return SYNC;
            }
            throw new IllegalArgumentException("There is no operation with id: "+id);
        }
        private final byte id;
        public final boolean isDispensable;
        protected final PayloadType payload;
    }

    //TODO migrate to payload
    public enum PayloadType {
        GAME_OBJECT,
        GAME_OBJECT_AND_COORDINATES,
        GAME_OBJECT_AND_PLAYER,
        GAME_OBJECTS,
        GAME_OBJECTS_FULL
    }

    static Payload deserialize(ByteBuffer buffer){
        Operation operation = Operation.getOperation(buffer.get());
        switch(operation.payload){
            case GAME_OBJECT: return getPayloadObject(operation, buffer);
            case GAME_OBJECTS: return getPayloadObjects(operation, buffer);
            case GAME_OBJECT_AND_COORDINATES: return getPayloadObjectCoords(operation, buffer);
            case GAME_OBJECT_AND_PLAYER: return getPayloadObjectPlayer(operation, buffer);
            case GAME_OBJECTS_FULL: return getPayloadGameObjectsFull(operation, buffer);
        }
        throw new IllegalArgumentException("Payload could not be deserialized from data");
    }

    private static Payload getPayloadObject(Operation operation, ByteBuffer buffer){
        return new Payload(operation, getUUID(buffer));
    }

    private static Payload getPayloadObjectCoords(Operation operation, ByteBuffer buffer){
        return new Payload(operation, getUUID(buffer), buffer.getFloat(), buffer.getFloat());
    }

    private static Payload getPayloadObjects(Operation operation, ByteBuffer buffer){
        ArrayList<UUID> gameObjects = new ArrayList<>();
        int position = buffer.position();
        int messageSize = buffer.getInt();
        while(buffer.position() - position < messageSize){
            gameObjects.add(getUUID(buffer));
        }
        return new Payload(operation, gameObjects.toArray());
    }

    private static Payload getPayloadObjectPlayer(Operation operation, ByteBuffer buffer){
        return new Payload(operation, getUUID(buffer), buffer.get());
    }

    private static Payload getPayloadGameObjectsFull(Operation operation, ByteBuffer buffer){
        int position = buffer.position();
        int messageSize = buffer.getInt();

        List<Object> objects = new ArrayList<>();

        while(buffer.position() - position < messageSize){
            int objectSize = buffer.getInt();
            UUID id = getUUID(buffer);
            float x = buffer.getFloat();
            float y = buffer.getFloat();

            Object gameObject = null;

            switch(buffer.get()){
                case Card.CARD_IDENTIFIER:
                    gameObject = getCard(buffer, id, x, y);
                    break;
                case CardStack.CARD_STACK_IDENTIFIER:
                    gameObject = getCardStack(buffer, objectSize, id ,x ,y);
            }

            Log.e("DESERIALIZE", "---> deserialized object with id " + ((GameObject)gameObject).id);
            objects.add(gameObject);
        }

        return new Payload(operation, objects);
    }

    private static UUID getUUID(ByteBuffer buffer){
        return new UUID(buffer.getLong(), buffer.getLong());
    }

    private static Card getCard(ByteBuffer buffer, UUID id, float x, float y){
        return new Card(
                id,
                x,
                y,
                Card.Suit.get(buffer.get()),
                Card.Value.get(buffer.get()),
                buffer.get() == 1);
    }

    private static CardStack getCardStack(ByteBuffer buffer, int byteSize, UUID id, float x, float y){
        List<Card> cards = new ArrayList<>();
        int position = buffer.position();
        byteSize -= GameObject.BYTE_SIZE + 1;

        Log.e("DESERIALIZE", "Card Stack id was " + id);


        while(buffer.position() - position < byteSize){

            UUID cardId = getUUID(buffer);
            cards.add(getCard(buffer, cardId, x, y));
        }

        return new CardStack(
                id,
                x,
                y,
                cards
        );
    }

    static byte[] serialize(Operation operation, UUID gameObjectId){
        assert operation.payload == PayloadType.GAME_OBJECT: "Get data for operation \""+operation.name()+"\" called for wrong parameters.";

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

    static byte[] serialize(Operation operation, UUID gameObjectId, byte playerId){
        assert operation.payload == PayloadType.GAME_OBJECT_AND_PLAYER : "Get data for operation \""+operation.name()+"\" called for wrong parameters.";

        byte[] data = new byte[18];
        ByteBuffer buffer = ByteBuffer.wrap(data);

        buffer.put(operation.id);
        buffer.putLong(gameObjectId.getMostSignificantBits());
        buffer.putLong(gameObjectId.getLeastSignificantBits());
        buffer.put(playerId);

        return data;
    }

    static byte[] serialize(Operation operation, UUID... gameObjects){
        assert operation.payload == PayloadType.GAME_OBJECTS : "Get data for operation \""+operation.name()+"\" called for wrong parameters.";

        int serializedSize = 16 * gameObjects.length + 4;
        byte[] data = new byte[serializedSize + 1];
        ByteBuffer buffer = ByteBuffer.wrap(data);

        buffer.put(operation.id);
        buffer.putInt(serializedSize);
        for(UUID uuid : gameObjects){
            buffer.putLong(uuid.getMostSignificantBits());
            buffer.putLong(uuid.getLeastSignificantBits());
        }

        return data;
    }

    static byte[] serialize(Operation operation, List<GameObject> gameObjects){
        assert operation.payload == PayloadType.GAME_OBJECTS_FULL : "Get data for operation \""+operation.name()+"\" called for wrong parameters.";

        Log.e("SERIALIZE", "Serialize object with id " + gameObjects.get(0).id + ", serialized " + gameObjects.size() + " object(s).");
        byte[] gameObjectsBytes = GameObject.getMultipleBytes(gameObjects);
        byte[] data = new byte[gameObjectsBytes.length + 1];
        ByteBuffer buffer = ByteBuffer.wrap(data);

        buffer.put(operation.id);
        buffer.put(gameObjectsBytes);

        return data;
    }
}
