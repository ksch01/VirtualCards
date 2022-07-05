package com.example.virtualcards.network;

import androidx.annotation.NonNull;

import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.interfaces.ModelInterface;
import com.example.virtualcards.model.interfaces.ModelSubscriber;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageTransmitter;

import java.nio.ByteBuffer;

public class VirtualCardsServer implements MessageReceiver, ModelInterface {

    private final MessageTransmitter transmitter;
    private final ModelInterface model;

    public VirtualCardsServer(@NonNull MessageTransmitter transmitter, @NonNull ModelInterface model){
        this.transmitter = transmitter;
        this.model = model;
    }

    @Override
    public void receive(ByteBuffer receivedBytes) {
        //NetworkData.Payload remoteOperation = NetworkData.deserialize(receivedBytes);
        //switch(remoteOperation.operation){
        //    case GET:
        //
        //}
    }

    @Override
    public void subscribeView(ModelSubscriber view) {
        model.subscribeView(view);
    }

    @Override
    public GameObject getObject(float x, float y) {
        return model.getObject(x, y);
    }

    @Override
    public void moveObject(GameObject object, float x, float y) {
        transmitter.send(NetworkData.serialize(NetworkData.Operation.MOVE, object.id, x, y));
        model.moveObject(object, x, y);
    }

    @Override
    public void dropObject(GameObject object, float x, float y) {
        transmitter.send(NetworkData.serialize(NetworkData.Operation.DROP, object.id, x, y));
        model.dropObject(object, x, y);
    }

    @Override
    public void hitObject(GameObject object) {
        transmitter.send(NetworkData.serialize(NetworkData.Operation.HIT, object.id));
        model.hitObject(object);
    }

    @Override
    public GameObject extractObject(GameObject object) {
        GameObject extracted = model.extractObject(object);
        if(extracted != null){
            transmitter.send(NetworkData.serialize(NetworkData.Operation.EXTRACT, object.id, extracted.id));
        }
        return extracted;
    }
}
