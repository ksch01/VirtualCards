package com.example.virtualcards.network;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.virtualcards.control.Control;
import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.TableModel;
import com.example.virtualcards.model.interfaces.ModelInterface;
import com.example.virtualcards.model.interfaces.ModelSubscriber;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageTransmitter;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

public class VirtualCardsClient implements MessageReceiver, ModelInterface {

    private static final long MAX_TICK = 64;
    private final byte id;

    private final Control control;

    private final MessageTransmitter transmitter;
    private final TableModel model;

    private long last;

    public VirtualCardsClient(@NonNull MessageTransmitter transmitter, byte playerId){
        this.transmitter = transmitter;
        this.model = TableModel.getModel();
        id = playerId;
        control = Control.getControl(this);
    }

    @Override
    public void receive(ByteBuffer receivedBytes) {
        Payload payload = NetworkData.deserialize(receivedBytes);

        switch(payload.operation){
            case RESERVE:
                Log.i("VCC_COMMS", "Received reserve for object " + payload.data.get(0) + " by player " + payload.data.get(1));
                byte playerId = (byte)payload.data.get(1);
                if(id == playerId){
                    control.obtainObjectAsynchronously(model.getObject((UUID)payload.data.get(0)));
                }else {
                    model.reserveObject(model.getObject((UUID) payload.data.get(0)), (byte) payload.data.get(1));
                }
                break;
            case MOVE:
                model.moveObject(model.getObject((UUID) payload.data.get(0)), (float)payload.data.get(1), (float)payload.data.get(2));
                break;
            case HIT:
                model.hitObject(model.getObject((UUID) payload.data.get(0)));
                break;
            case DROP:
                model.dropObject(model.getObject((UUID) payload.data.get(0)), (float)payload.data.get(1), (float)payload.data.get(2));
                break;
            case EXTRACT:
                break;
            case SHUFFLE:
                ;
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
            transmitter.send(NetworkData.serialize(NetworkData.Operation.RESERVE, object.id, id));
        }
        return null;
    }

    @Override
    public GameObject getObject(UUID id) {
        return model.getObject(id);
    }

    @Override
    public void reserveObject(GameObject gameObject, byte player) {

    }

    @Override
    public void moveObject(GameObject object, float x, float y) {
        if(object != null && isSendValid(NetworkData.Operation.MOVE))
            transmitter.send(NetworkData.serialize(NetworkData.Operation.MOVE, object.id, x, y));
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
