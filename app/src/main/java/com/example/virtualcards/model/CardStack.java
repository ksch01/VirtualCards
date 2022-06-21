package com.example.virtualcards.model;

import java.util.ArrayList;
import java.util.Collections;

public class CardStack extends Card {

    static final int ACTION_DRAW = 1;

    private ArrayList<Card> cards = new ArrayList<>();

    CardStack(float x, float y, Card.Suit suit, Card.Value value) {
        super(x, y, suit, value);
    }

    protected static CardStack stackCards(Card card, Card to){
        CardStack stack;
        if(card instanceof CardStack){
            stack = (CardStack) card;
        }else {
            stack = new CardStack(to.x, to.y, null, null);
            stack.add(card);
        }
        stack.add(to);
        return stack;
    }

    private void add(Card card){
        if(card instanceof CardStack){
            cards.addAll(((CardStack) card).cards);
        }else {
            cards.add(card);
        }
    }

    @Override
    public Card.Suit getSuit(){
        return cards.get(0).suit;
    }

    @Override
    public Card.Value getValue(){
        return cards.get(0).value;
    }

    @Override
    public boolean isFaceUp(){
        return cards.get(0).isFaceUp();
    }

    public Card popCard(){
        Card card = cards.get(0);
        cards.remove(0);
        return card;
    }

    @Override
    protected void flip(){
        for (Card card : cards) {
            card.flip();
        }
        Collections.reverse(cards);
    }
}
