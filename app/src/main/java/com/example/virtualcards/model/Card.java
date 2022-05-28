package com.example.virtualcards.model;

public class Card extends GameObject {

    public static final float WIDTH = 59, HEIGHT = 91;

    public enum Suit{
        SPADES,
        CLUBS,
        HEARTS,
        DIAMONDS
    }
    public enum Value{
        TWO,
        THREE,
        FOUR,
        FIVE,
        SIX,
        SEVEN,
        EIGHT,
        NINE,
        TEN,
        JACK,
        QUEEN,
        KING,
        ACE
    }

    protected final Suit suit;
    protected final Value value;
    protected boolean faceUp;

    Card(float x, float y, Suit suit, Value value) {
        super(x, y, WIDTH, HEIGHT);

        this.suit = suit;
        this.value = value;

        faceUp = false;
    }

    protected void flip(){
        faceUp = !faceUp;
    }

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public Suit getSuit(){
        return suit;
    }

    public Value getValue(){
        return value;
    }

    public boolean isFaceUp(){
        return faceUp;
    }
}
