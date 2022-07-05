package com.example.virtualcards.network;

import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.interfaces.ModelInterface;
import com.example.virtualcards.model.interfaces.ModelSubscriber;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;

import java.nio.ByteBuffer;

public class VirtualCardsClient implements MessageReceiver, ModelInterface {

    @Override
    public void receive(ByteBuffer receivedBytes) {

    }

    @Override
    public void subscribeView(ModelSubscriber view) {

    }

    @Override
    public GameObject getObject(float x, float y) {
        return null;
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
