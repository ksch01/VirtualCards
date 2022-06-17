package com.example.virtualcards.model.interfaces;

import com.example.virtualcards.model.GameObject;

public interface ModelInterface {

    void subscribeView(ModelSubscriber view);

    GameObject getObject(float x, float y);

    void moveObject(GameObject object, float x, float y);
    void dropObject(GameObject object, float x, float y);
}
