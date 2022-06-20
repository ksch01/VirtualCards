package com.example.virtualcards.model;

public class Card extends GameObject {

    static final int ACTION_FLIP = 0;

    public static final float WIDTH = 59, HEIGHT = 91;

    public enum Suit{
        SPADES(3),
        CLUBS(2),
        HEARTS(1),
        DIAMONDS(0);

        public final int i;

        Suit(int i){
            this.i = i;
        }
    }
    public enum Value{
        TWO(0),
        THREE(1),
        FOUR(2),
        FIVE(3),
        SIX(4),
        SEVEN(5),
        EIGHT(6),
        NINE(7),
        TEN(8),
        JACK(9),
        QUEEN(10),
        KING(11),
        ACE(12);

        public final int i;

        Value(int i){
            this.i = i;
        }
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
