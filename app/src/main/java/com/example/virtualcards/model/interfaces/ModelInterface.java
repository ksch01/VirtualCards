package com.example.virtualcards.model.interfaces;

import com.example.virtualcards.model.GameObject;

import java.util.List;
import java.util.UUID;

public interface ModelInterface {

    List<GameObject> getGameObjects();
    void setState(List<GameObject> gameObjects);
    void subscribeView(ModelSubscriber view);

    GameObject getObject(float x, float y);
    GameObject getObject(UUID id);

    void reserveObject(GameObject gameObject, byte player);
    void moveObject(GameObject object, float x, float y);
    void dropObject(GameObject object, float x, float y);
    void hitObject(GameObject object);
    GameObject extractObject (GameObject object);
}
