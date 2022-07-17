package com.example.virtualcards.model;

import java.util.UUID;

public class Card extends GameObject {

    public static final int CARD_IDENTIFIER = 1;

    public static final float WIDTH = 59, HEIGHT = 91;

    public enum Suit{
        SPADES((byte)3),
        CLUBS((byte)2),
        HEARTS((byte)1),
        DIAMONDS((byte)0);

        public final byte i;

        Suit(byte i){
            this.i = i;
        }

        public static Suit get(byte i){
            switch (i){
                case 0: return DIAMONDS;
                case 1: return HEARTS;
                case 2: return CLUBS;
                case 3: return SPADES;
            }
            throw new IllegalArgumentException("There is no suit for given i.");
        }
    }
    public enum Value{
        TWO((byte)0),
        THREE((byte)1),
        FOUR((byte)2),
        FIVE((byte)3),
        SIX((byte)4),
        SEVEN((byte)5),
        EIGHT((byte)6),
        NINE((byte)7),
        TEN((byte)8),
        JACK((byte)9),
        QUEEN((byte)10),
        KING((byte)11),
        ACE((byte)12);

        public final byte i;

        Value(byte i){
            this.i = i;
        }

        public static Value get(byte i){
            switch (i){
                case 0: return TWO;
                case 1: return THREE;
                case 2: return FOUR;
                case 3: return FIVE;
                case 4: return SIX;
                case 5: return SEVEN;
                case 6: return EIGHT;
                case 7: return NINE;
                case 8: return TEN;
                case 9: return JACK;
                case 10: return QUEEN;
                case 11: return KING;
                case 12: return ACE;
            }
            throw new IllegalArgumentException("There is no value for given i.");
        }
    }

    protected final Suit suit;
    protected final Value value;
    protected boolean faceUp;

    public Card(float x, float y, Suit suit, Value value) {
        super(x, y, WIDTH, HEIGHT);

        this.suit = suit;
        this.value = value;
    }
    public Card(UUID id, float x, float y, Suit suit, Value value, boolean faceUp){
        super(id, x, y, WIDTH, HEIGHT);

        this.suit = suit;
        this.value = value;
        this.faceUp = faceUp;
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

    @Override
    protected byte[] serialize() {
        return new byte[]{
                suit.i,
                value.i,
                faceUp?(byte)1 : 0
        };
    }

    @Override
    protected byte getIdentifier() {
        return CARD_IDENTIFIER;
    }
}
