package com.example.virtualcards.model.interfaces;

import com.example.virtualcards.model.GameObject;

import java.util.List;
import java.util.UUID;

public interface Model {

    List<GameObject> getGameObjects();
    void setState(List<GameObject> gameObjects);
    void subscribeView(ModelSubscriber view);

    GameObject getObject(float x, float y);
    GameObject getObject(UUID id);

    boolean isAvailable(GameObject gameObject, byte player);
    boolean reserveObject(GameObject gameObject, byte player);

    void moveObject(GameObject object, float x, float y);
    GameObject dropObject(GameObject object, float x, float y);
    GameObject dropObject(GameObject object, UUID id, float x, float y);
    void hitObject(GameObject object);
    GameObject extractObject (GameObject object);
}
