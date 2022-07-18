package com.example.virtualcards.network;

import androidx.annotation.NonNull;

import com.example.virtualcards.control.Controls;
import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.interfaces.Model;
import com.example.virtualcards.model.interfaces.ModelSubscriber;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageTransmitter;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

public class VirtualCardsClient implements MessageReceiver, Model {

    private static final long MAX_TICK = 64;
    private final byte id;

    private final MessageTransmitter transmitter;
    private final Model model;
    private Controls controls;

    private GameObject desiredObject;

    private long last;

    public VirtualCardsClient(@NonNull Model model, @NonNull MessageTransmitter transmitter, byte playerId){
        this.transmitter = transmitter;
        this.model = model;
        id = playerId;
    }

    public void setControls(@NonNull Controls controls){
        this.controls = controls;
    }

    @Override
    public void receive(ByteBuffer receivedBytes) {
        Payload payload = NetworkData.deserialize(receivedBytes);

        switch(payload.operation){
            case RESERVE:
                byte playerId = (byte)payload.data.get(1);
                if(id == playerId){
                    controls.obtainObjectAsynchronously(model.getObject((UUID)payload.data.get(0)));
                }else {
                    model.reserveObject(model.getObject((UUID) payload.data.get(0)), playerId);
                }
                break;
            case MOVE:
            case DROP:
                model.moveObject(model.getObject((UUID) payload.data.get(0)), (float)payload.data.get(1), (float)payload.data.get(2));
                break;
            case HIT:
                model.hitObject(model.getObject((UUID) payload.data.get(0)));
                break;
            case EXTRACT:
                playerId = (byte)payload.data.get(1);
                GameObject object = model.extractObject(model.getObject((UUID) payload.data.get(0)));
                if(id == playerId) {
                    controls.obtainObjectAsynchronously(object);
                }else{
                    model.reserveObject(object, playerId);
                }
                break;
            case STACK:
                model.dropObject(model.getObject((UUID) payload.data.get(0)), (UUID)payload.data.get(1), (float)payload.data.get(2), (float)payload.data.get(3));
                break;
            case SYNC:
                model.setState((List<GameObject>) payload.data.get(0));
        }
    }

    private boolean isSendValid(NetworkData.Operation operation){
        if(operation.isDispensable){
            if(last == 0){
                last = System.currentTimeMillis();
                return true;
            }else{
                long now = System.currentTimeMillis();
                if(now - last > MAX_TICK){
                    last = now;
                    return true;
                }else{
                    return false;
                }
            }
        }
        return false;
    }

    @Override
    public List<GameObject> getGameObjects() {
        return model.getGameObjects();
    }

    @Override
    public void setState(List<GameObject> gameObjects) {
        model.setState(gameObjects);
    }

    @Override
    public void subscribeView(ModelSubscriber view) {
        model.subscribeView(view);
    }

    @Override
    public GameObject getObject(float x, float y) {
        GameObject object = model.getObject(x,y);
        if(object != null){
            desiredObject = object;
            transmitter.send(NetworkData.serialize(NetworkData.Operation.RESERVE, object.id, id));
        }
        return null;
    }

    @Override
    public GameObject getObject(UUID id) {
        return model.getObject(id);
    }

    @Override
    public boolean isAvailable(GameObject gameObject, byte player) {
        throw new UnsupportedOperationException("Operation \"is available\" from model is not available for virtual cards client model.");
    }

    @Override
    public boolean reserveObject(GameObject gameObject, byte player) {
        throw new UnsupportedOperationException("Operation reserve from model is not available for virtual cards client model.");
    }

    @Override
    public void moveObject(GameObject object, float x, float y) {
        if(object != null && isSendValid(NetworkData.Operation.MOVE))
            transmitter.send(NetworkData.serialize(NetworkData.Operation.MOVE, object.id, x, y));
    }

    @Override
    public GameObject dropObject(GameObject object, float x, float y) {
        if(object != null)
            transmitter.send(NetworkData.serialize(NetworkData.Operation.DROP, object.id, x, y));
        return null;
    }

    @Override
    public GameObject dropObject(GameObject object, UUID id, float x, float y) {
        throw new UnsupportedOperationException("Operation drop object with parameter id from model is not available for virtual cards client model.");
    }

    @Override
    public void hitObject(GameObject object) {
        if(desiredObject != null) {
            transmitter.send(NetworkData.serialize(NetworkData.Operation.HIT, desiredObject.id));
            desiredObject = null;
        }
    }

    @Override
    public GameObject extractObject(GameObject object) {
        if(object != null)
            transmitter.send(NetworkData.serialize(NetworkData.Operation.EXTRACT, object.id, id));
        return null;
    }
}
