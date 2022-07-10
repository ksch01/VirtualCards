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

public class VirtualCardsServer implements MessageReceiver, ModelInterface {

    private final MessageTransmitter transmitter;
    private final ModelInterface model;

    public VirtualCardsServer(@NonNull MessageTransmitter transmitter){
        this.transmitter = transmitter;
        this.model = TableModel.getModel();
    }

    @Override
    public void receive(ByteBuffer receivedBytes) {
        byte[] bytes = receivedBytes.array();
        byte operator = receivedBytes.get();
        NetworkData.Payload remoteOperation = NetworkData.deserialize(receivedBytes);
        switch(remoteOperation.operation){
            case MOVE:
                moveObject(model.getObject(remoteOperation.getObject(0)), remoteOperation.x, remoteOperation.y);
            case GET:

        }
    }

    @Override
    public void subscribeView(ModelSubscriber view) {
        model.subscribeView(view);
    }

    private void getObject(byte operator, UUID objectId){

    }

    @Override
    public GameObject getObject(float x, float y) {
        return model.getObject(x, y);
    }

    @Override
    public GameObject getObject(UUID id) {
        return model.getObject(id);
    }

    @Override
    public void reserveObject(byte player, GameObject object) {
        if(object != null)return;
        transmitter.send(NetworkData.serialize(NetworkData.Operation.GET, object.id, player));
        model.reserveObject(player, object);
    }

    @Override
    public void moveObject(GameObject object, float x, float y) {
        if(object == null)return;
        transmitter.send(NetworkData.serialize(NetworkData.Operation.MOVE, object.id, x, y));
        model.moveObject(object, x, y);
    }

    @Override
    public void dropObject(GameObject object, float x, float y) {
        if(object == null)return;
        transmitter.send(NetworkData.serialize(NetworkData.Operation.DROP, object.id, x, y));
        model.dropObject(object, x, y);
    }

    @Override
    public void hitObject(GameObject object) {
        if(object == null)return;
        transmitter.send(NetworkData.serialize(NetworkData.Operation.HIT, object.id));
        model.hitObject(object);
    }

    @Override
    public GameObject extractObject(GameObject object) {
        if(object == null)return null;
        GameObject extracted = model.extractObject(object);
        if(extracted != null){
            transmitter.send(NetworkData.serialize(NetworkData.Operation.EXTRACT, object.id, extracted.id));
        }
        return extracted;
    }
}
