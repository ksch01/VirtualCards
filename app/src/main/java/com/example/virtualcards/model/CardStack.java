package com.example.virtualcards.model;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

public class CardStack extends Card {

    public static final byte CARD_STACK_IDENTIFIER = 2;

    private final ArrayList<Card> cards = new ArrayList<>();

    CardStack(float x, float y) {
        super(x, y, null, null);
    }
    private CardStack(UUID id, float x, float y){
        super(id,x,y,null,null,false);
    }
    public CardStack(UUID id, float x, float y, @NonNull Collection<Card> cards){
        super(id, x, y,null,null, false);
        if(cards.size() < 2)throw new IllegalArgumentException("Can not create card stack with less then two cards.");
        this.cards.addAll(cards);
    }

    protected static CardStack stackCards(UUID id, Card card, Card to){
        CardStack stack;
        if(card instanceof CardStack){
            stack = (CardStack) card;
        }else {
            stack = new CardStack(id, to.x, to.y);
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

    protected boolean isEmpty(){
        return cards.isEmpty();
    }

    @Override
    protected void flip(){
        for (Card card : cards) {
            card.flip();
        }
        Collections.reverse(cards);
    }

    @Override
    protected byte[] serialize(){
        ByteBuffer cardStackBytes = ByteBuffer.wrap(new byte[cards.size() * 19]);
        for(Card card : cards){
            cardStackBytes.putLong(card.id.getMostSignificantBits());
            cardStackBytes.putLong(card.id.getLeastSignificantBits());
            cardStackBytes.put(card.serialize());
        }
        return cardStackBytes.array();
    }

    @Override
    protected byte getIdentifier() {
        return CARD_STACK_IDENTIFIER;
    }
}
