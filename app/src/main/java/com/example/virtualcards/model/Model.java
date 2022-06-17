package com.example.virtualcards.model;

import com.example.virtualcards.model.interfaces.ModelInterface;
import com.example.virtualcards.model.interfaces.ModelSubscriber;

import java.util.ArrayList;

public class Model implements ModelInterface {
    public static final float WIDTH = 640, HEIGHT = 360;
    public static final float MAX_STACK_DISTANCE = 12;

    private ArrayList<GameObject> gameObjects;
    private ModelSubscriber view;

    private Model instance;

    Model(){
        gameObjects = new ArrayList<>();
    }

    public Model getModel(){
        if(instance == null){
            instance = new Model();
        }
        return instance;
    }

    @Override
    public void subscribeView(ModelSubscriber view) {
        this.view = view;
    }

    @Override
    public GameObject getObject(float x, float y) {
        int length = gameObjects.size();
        for(int i = 0; i < length; i++){
            GameObject gameObject = gameObjects.get(i);
            if(gameObject.isOn(x,y))
                return gameObject;
        }
        return null;
    }

    private GameObject getObject(float x, float y, float distance, Class type){
        int length = gameObjects.size();
        for(int i = 0; i < length; i++){
            GameObject gameObject = gameObjects.get(i);
            if(gameObject.isOn(x,y,distance) && type.isInstance(gameObject))
                return gameObject;
        }
        return null;
    }

    @Override
    public void moveObject(GameObject object, float x, float y) {
        x = clampToWidth(x);
        y = clampToHeight(y);

        object.setPos(x,y);

        view.update(gameObjects);
    }

    @Override
    public void dropObject(GameObject object, float x, float y){
        x = clampToWidth(x);
        y = clampToHeight(y);

        if(object instanceof Card){
            GameObject stackTo = getObject(x, y, MAX_STACK_DISTANCE, Card.class);
            if(stackTo != null){
                gameObjects.remove(object);
                gameObjects.remove(stackTo);
                gameObjects.add(CardStack.stackCards((Card) object, (Card)stackTo));
            }
        }

        view.update(gameObjects);
    }

    private float clampToWidth(float x){
        if(x < 0) return 0;
        if(x >= WIDTH) return WIDTH - 1;
        return x;
    }

    private float clampToHeight(float y){
        if(y < 0) return 0;
        if(y >= HEIGHT) return HEIGHT - 1;
        return y;
    }
}
