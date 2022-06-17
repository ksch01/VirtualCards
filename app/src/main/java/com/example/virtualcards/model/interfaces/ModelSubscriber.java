package com.example.virtualcards.model.interfaces;

import com.example.virtualcards.model.GameObject;

import java.util.ArrayList;

public interface ModelSubscriber {
    void update(ArrayList<GameObject> gameObjects);
}
