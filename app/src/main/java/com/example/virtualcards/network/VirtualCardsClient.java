package com.example.virtualcards.network;

import androidx.annotation.NonNull;

import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.TableModel;
import com.example.virtualcards.model.interfaces.ModelInterface;
import com.example.virtualcards.model.interfaces.ModelSubscriber;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageTransmitter;

import java.nio.ByteBuffer;
import java.util.UUID;

public class VirtualCardsClient implements MessageReceiver, ModelInterface {

    private byte id;

    private MessageTransmitter transmitter;
    private TableModel model;

    public VirtualCardsClient(@NonNull MessageTransmitter transmitter, byte playerId){
        this.transmitter = transmitter;
        this.model = TableModel.getModel();
        id = playerId;
    }

    @Override
    public void receive(ByteBuffer receivedBytes) {
        NetworkData.Payload payload = NetworkData.deserialize(receivedBytes);
        assert !receivedBytes.hasRemaining() : "received message buffer had remaining although every info that should have been send was read";

        switch(payload.operation){
            case GET:
                model.reserveObject(payload.player, model.getObject(payload.getObject(0)));
            case MOVE:
                model.moveObject(model.getObject(payload.getObject(0)), payload.x, payload.y);
                break;
            case HIT:
                model.hitObject(model.getObject(payload.getObject(0)));
                break;
            case DROP:
                model.dropObject(model.getObject(payload.getObject(0)), payload.x, payload.y);
                break;
            case EXTRACT:
                break;
            case SHUFFLE:
        }
    }

    @Override
    public void subscribeView(ModelSubscriber view) {
        model.subscribeView(view);
    }

    @Override
    public GameObject getObject(float x, float y) {
        GameObject object = model.getObject(x,y);
        if(object != null){
            transmitter.send(NetworkData.serialize(NetworkData.Operation.GET, object.id));
        }
        return null;
    }

    @Override
    public GameObject getObject(UUID id) {
        return model.getObject(id);
    }

    @Override
    public void reserveObject(byte player, GameObject gameObject) {

    }

    @Override
    public void moveObject(GameObject object, float x, float y) {

    }

    @Override
    public void dropObject(GameObject object, float x, float y) {

    }

    @Override
    public void hitObject(GameObject object) {

    }

    @Override
    public GameObject extractObject(GameObject object) {
        return null;
    }
}
