package com.example.virtualcards.view.card;

import com.example.virtualcards.model.Card;

public class RenderCardCall {
    public final float x;
    public final float y;
    public final boolean faceUp;
    public RenderCardCall(Card card){
        x = card.getX();
        y = card.getY();
        faceUp = card.isFaceUp();
    }
}
