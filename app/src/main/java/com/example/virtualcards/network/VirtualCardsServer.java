package com.example.virtualcards.network;

import androidx.annotation.NonNull;

import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.interfaces.ModelInterface;
import com.example.virtualcards.model.interfaces.ModelSubscriber;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageTransmitter;

import java.nio.ByteBuffer;

public class VirtualCardsServer implements MessageReceiver, ModelInterface {

    public static final int PAYLOAD_SIZE_VARIABLE = -1;
    private final MessageTransmitter transmitter;

    public VirtualCardsServer(@NonNull MessageTransmitter transmitter){
        this.transmitter = transmitter;
    }

    public enum Operation{
        MOVE_OBJECT((byte)1, PayloadType.OBJECT_AND_COORDINATES),
        DROP_OBJECT((byte)2, PayloadType.OBJECT_AND_COORDINATES),
        HIT_OBJECT((byte)3, PayloadType.SINGLE_OBJECT),
        EXTRACT_OBJECT((byte)4, PayloadType.SINGLE_OBJECT);

        Operation(byte id, PayloadType payloadType){
            this.id = id;
            this.type = payloadType;
        }
        public final byte id;
        protected final PayloadType type;

        protected void transmit(MessageTransmitter transmitter, Object[] data){
            byte[] packedData = PayloadType.pack(type, data);
            packedData[0] = id;
            transmitter.send(packedData);
        }
    }

    protected enum PayloadType{

        SINGLE_OBJECT(16),
        MULTIPLE_OBJECTS(PAYLOAD_SIZE_VARIABLE),
        OBJECT_AND_COORDINATES(24);

        PayloadType(int size){
            this.size = size;
        }
        final int size;

        protected static byte[] pack(@NonNull PayloadType payloadType,@NonNull Object[] data){
            byte[] packed;
            ByteBuffer buffer = null;
            if(payloadType.size != PAYLOAD_SIZE_VARIABLE){
                packed = new byte[payloadType.size+1];
                buffer = ByteBuffer.wrap(packed);
                buffer.position(1);
            }

            switch (payloadType){
                case SINGLE_OBJECT:
                    Serialization.putUUID(buffer, ((GameObject)data[0]).id);
                    break;

                case OBJECT_AND_COORDINATES:
                    Serialization.putUUID(buffer, ((GameObject)data[0]).id);
                    buffer.putFloat((float)data[1]).putFloat((float)data[2]);
                    break;

                default:
                    throw new UnsupportedOperationException("Payload type unknown: \"" + payloadType.name() +"\".");
            }
            return buffer.array();
        }
    }

    @Override
    public void received(byte[] receivedBytes) {

    }

    @Override
    public void subscribeView(ModelSubscriber view) {
        throw new UnsupportedOperationException("Can not subscribe to network.");
    }

    @Override
    public GameObject getObject(float x, float y) {
        throw new UnsupportedOperationException("GetObject should be called on the local Model.");
    }

    @Override
    public void moveObject(GameObject object, float x, float y) {
        Operation.MOVE_OBJECT.transmit(transmitter, new Object[]{object, x, y});
    }

    @Override
    public void dropObject(GameObject object, float x, float y) {
        Operation.DROP_OBJECT.transmit(transmitter, new Object[]{object, x, y});
    }

    @Override
    public void hitObject(GameObject object) {
        Operation.HIT_OBJECT.transmit(transmitter, new Object[]{object});
    }

    @Override
    public GameObject extractObject(GameObject object) {
        //TODO will not be working: extracted object has to be asynchronously returned from server.
        Operation.EXTRACT_OBJECT.transmit(transmitter, new Object[]{object});
        return null;
    }
}
