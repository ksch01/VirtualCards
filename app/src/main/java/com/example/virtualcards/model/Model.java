package com.example.virtualcards.model;

import com.example.virtualcards.model.interfaces.ModelInterface;
import com.example.virtualcards.model.interfaces.ModelSubscriber;

import java.util.ArrayList;

public class Model implements ModelInterface {
    public static final float HEIGHT = 360, WIDTH = HEIGHT * 2.0277777f;
    public static final float MAX_STACK_DISTANCE = 10 * 10;

    private ArrayList<GameObject> gameObjects;
    private ModelSubscriber view;

    private static Model instance;

    Model(){
        gameObjects = new ArrayList<>();
        gameObjects.add(createFullStack());
    }

    private static final Card createFullStack(){
        float centerX = (WIDTH - Card.WIDTH) * 0.5f, centerY = (HEIGHT - Card.HEIGHT) * 0.5f;
        Card previous = null;
        for(Card.Suit suit : Card.Suit.values()){
            for(Card.Value value : Card.Value.values()){
                Card current = new Card(centerX, centerY, suit, value);
                if(previous != null){
                    previous = CardStack.stackCards(current, previous);
                }else{
                    previous = current;
                }
            }
        }
        return previous;
    }

    public static Model getModel(){
        if(instance == null)
            instance = new Model();
        return instance;
    }

    @Override
    public void subscribeView(ModelSubscriber view) {
        this.view = view;
    }

    @Override
    public GameObject getObject(float x, float y) {
        int length = gameObjects.size()-1;
        for(int i = length; i >= 0; i--){
            GameObject gameObject = gameObjects.get(i);
            if(gameObject.isOn(x,y)) {
                if(i != gameObjects.size()-1){
                    gameObjects.remove(i);
                    gameObjects.add(gameObject);
                }
                return gameObject;
            }
        }
        return null;
    }

    private GameObject getObject(GameObject object, float distance, Class type){
        int length = gameObjects.size();

        for(int i = 0; i < length; i++){

            GameObject gameObject = gameObjects.get(i);
            if(gameObject == object){
                continue;
            }

            boolean isOn = gameObject.isOn(object.x,object.y,distance);

            if(isOn && type.isInstance(gameObject)) {
                return gameObject;
            }
        }

        return null;
    }

    @Override
    public void moveObject(GameObject object, float x, float y) {
        if(object == null)return;

        x = clampToWidth(object, x - object.width * 0.5f);
        y = clampToHeight(object, y - object.height * 0.5f);

        object.setPos(x,y);

        notifySubscriber();
    }

    @Override
    public void dropObject(GameObject object, float x, float y){
        if(object == null)return;

        x = clampToWidth(object, x);
        y = clampToHeight(object, y);

        if(object instanceof Card){
            System.out.println("DROP CARD/STACK (" + x + ", " + y + ") " + object);
            GameObject stackTo = getObject(object, MAX_STACK_DISTANCE, Card.class);
            if(stackTo != null){
                gameObjects.remove(object);
                gameObjects.remove(stackTo);
                gameObjects.add(CardStack.stackCards((Card) object, (Card)stackTo));
            }
        }

        notifySubscriber();
    }

    @Override
    public void hitObject(GameObject object){
        if(object == null)return;

        if(object instanceof Card) {
            ((Card) object).flip();
            notifySubscriber();
        }
    }

    @Override
    public GameObject extractObject(GameObject object){
        if(object == null)return null;

        if(object instanceof CardStack) {
            Card card = ((CardStack) object).popCard();
            gameObjects.add(card);
            notifySubscriber();
            return card;
        }else
            return null;
    }

    private void notifySubscriber(){
        ArrayList<GameObject> copy = new ArrayList<>(gameObjects.size());
        for(GameObject object : gameObjects)
            copy.add(object);
        view.update(copy);
    }

    private float clampToWidth(GameObject object, float x){
        if(x < 0) return 0;
        float x1 = x + object.width;
        if(x1 >= WIDTH) return WIDTH - object.width;
        return x;
    }

    private float clampToHeight(GameObject object, float y){
        if(y < 0) return 0;
        float y1 = y + object.height;
        if(y1 >= HEIGHT) return HEIGHT - object.height;
        return y;
    }
}
