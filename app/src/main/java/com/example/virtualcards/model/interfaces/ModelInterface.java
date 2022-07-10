package com.example.virtualcards.model.interfaces;

import com.example.virtualcards.model.GameObject;

import java.util.UUID;

public interface ModelInterface {

    void subscribeView(ModelSubscriber view);

    GameObject getObject(float x, float y);
    GameObject getObject(UUID id);

    void reserveObject(byte player, GameObject gameObject);

    void moveObject(GameObject object, float x, float y);
    void dropObject(GameObject object, float x, float y);
    void hitObject(GameObject object);
    GameObject extractObject (GameObject object);
}
