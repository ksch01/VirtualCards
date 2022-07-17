package com.example.virtualcards.model.interfaces;

import com.example.virtualcards.model.GameObject;

import java.util.ArrayList;
import java.util.List;

public interface ModelSubscriber {
    void update(List<GameObject> gameObjects);
}
