package com.example.virtualcards.view.card;

import com.example.virtualcards.model.Card;

public class RenderCardCall implements Comparable<RenderCardCall>{
    public final float x;
    public final float y;
    public final boolean faceUp;
    public final int index;
    public RenderCardCall(Card card, int index){
        x = card.getX();
        y = card.getY();
        faceUp = card.isFaceUp();
        this.index = index;
    }

    @Override
    public int compareTo(RenderCardCall renderCardCall) {
        return index - renderCardCall.index;
    }
}
