package com.example.virtualcards.model;

public interface ModelInterface {

    void subscribeView(CardGameView view);

    GameObject getObject(float x, float y);

    void moveObject(GameObject object, float x, float y);
    void dropObject(GameObject object, float x, float y);
}
