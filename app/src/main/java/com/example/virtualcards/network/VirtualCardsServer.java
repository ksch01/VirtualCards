package com.example.virtualcards.network;

import androidx.annotation.NonNull;

import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.TableModel;
import com.example.virtualcards.model.interfaces.ModelInterface;
import com.example.virtualcards.model.interfaces.ModelSubscriber;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageTransmitter;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

public class VirtualCardsServer implements MessageReceiver, ModelInterface {

    private static final byte ID = Byte.MIN_VALUE;

    private static final long MAX_TICK = 64;
    private long last = 0;

    private final MessageTransmitter transmitter;
    private final ModelInterface model;

    public VirtualCardsServer(@NonNull MessageTransmitter transmitter){
        this.transmitter = transmitter;
        this.model = TableModel.getModel();

        float x = (TableModel.WIDTH) * 0.5f;
        float y = (TableModel.HEIGHT) * 0.5f;
        model.moveObject(model.getObject(x,  y), x, y);

        sendSync();
    }

    @Override
    public void receive(ByteBuffer receivedBytes) {
        Payload payload = NetworkData.deserialize(receivedBytes);
        switch(payload.operation){
            case MOVE:
                moveObject(model.getObject((UUID) payload.data.get(0)), (float)payload.data.get(1), (float)payload.data.get(2));
            case RESERVE:

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

    private void sendSync(){
        transmitter.send(NetworkData.serialize(NetworkData.Operation.SYNC, model.getGameObjects()));
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
    public void reserveObject(GameObject object, byte player) {
        if(object != null)return;
        transmitter.send(NetworkData.serialize(NetworkData.Operation.RESERVE, object.id, player));
        model.reserveObject(object, player);
    }

    @Override
    public void moveObject(GameObject object, float x, float y) {
        if(object == null)return;
        if(isSendValid(NetworkData.Operation.MOVE)) {
            transmitter.send(NetworkData.serialize(NetworkData.Operation.MOVE, object.id, x, y));
        }
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
