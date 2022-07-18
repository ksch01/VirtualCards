package com.example.virtualcards.network;

import androidx.annotation.NonNull;

import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.TableModel;
import com.example.virtualcards.model.interfaces.Model;
import com.example.virtualcards.model.interfaces.ModelSubscriber;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageTransmitter;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

//TODO optimize response to clients on network when client messages have to be mirrored
public class VirtualCardsServer implements MessageReceiver, Model {

    private static final byte ID = Byte.MIN_VALUE;

    private static final long MAX_TICK = 32;
    private long last = 0;

    private final MessageTransmitter transmitter;
    private final Model model;

    public VirtualCardsServer(@NonNull Model model, @NonNull MessageTransmitter transmitter){
        this.transmitter = transmitter;
        this.model = model;

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
                break;
            case DROP:
                dropObject(model.getObject((UUID) payload.data.get(0)), (float)payload.data.get(1), (float)payload.data.get(2));
                break;
            case HIT:
                hitObject(model.getObject((UUID) payload.data.get(0)));
                break;
            case EXTRACT:
                extractObject(model.getObject((UUID) payload.data.get(0)), (byte) payload.data.get(1));
                break;
            case RESERVE:
                reserveObject(model.getObject((UUID) payload.data.get(0)), (byte)payload.data.get(1));
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

    @Override
    public GameObject getObject(float x, float y) {
        GameObject gameObject = model.getObject(x, y);
        if(reserveObject(gameObject, ID)){
            return gameObject;
        }else{
            return null;
        }
    }

    @Override
    public GameObject getObject(UUID id) {
        return model.getObject(id);
    }

    @Override
    public boolean isAvailable(GameObject gameObject, byte player) {
        return model.isAvailable(gameObject, player);
    }

    @Override
    public boolean reserveObject(GameObject object, byte player) {
        if(object == null)return false;
        if(model.reserveObject(object,player)){
            transmitter.send(NetworkData.serialize(NetworkData.Operation.RESERVE, object.id, player));
            return true;
        }else{
            return false;
        }
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
    public GameObject dropObject(GameObject object, float x, float y) {
        if(object == null)return null;
        GameObject stack = model.dropObject(object, x, y);
        if(stack == null) {
            transmitter.send(NetworkData.serialize(NetworkData.Operation.DROP, object.id, x, y));
        }else{
            transmitter.send(NetworkData.serialize(NetworkData.Operation.STACK, object.id, stack.id, x, y));
        }
        return stack;
    }

    @Override
    public GameObject dropObject(GameObject object, UUID id, float x, float y) {
        throw new UnsupportedOperationException("Operation drop object of model with id parameter is not available for virtual cards server model.");
    }

    @Override
    public void hitObject(GameObject object) {
        if(object == null)return;
        transmitter.send(NetworkData.serialize(NetworkData.Operation.HIT, object.id));
        model.hitObject(object);
    }

    @Override
    public GameObject extractObject(GameObject object) {
        return extractObject(object, ID);
    }

    private GameObject extractObject(GameObject object, byte player){
        if(object == null)return null;
        GameObject extracted = model.extractObject(object);
        if(extracted != null){
            transmitter.send(NetworkData.serialize(NetworkData.Operation.EXTRACT, object.id, player));
            model.reserveObject(extracted, player);
        }
        return extracted;
    }
}
