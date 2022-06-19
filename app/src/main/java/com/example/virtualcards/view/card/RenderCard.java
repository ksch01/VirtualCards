package com.example.virtualcards.view.card;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.example.virtualcards.model.Card;
import com.example.virtualcards.view.geometry.Rectangle;
import com.example.virtualcards.view.shader.Shader;
import com.example.virtualcards.view.texture.Texture;

public class RenderCard {

    public static final int CARD_BACK_REGION_X = 13, CARD_BACK_REGION_Y = 0;

    private Rectangle front;
    private static Rectangle back;

    private float[] model = new float[16];

    private int modelUniformHandle = -1;

    public RenderCard(Shader shader, Texture cardsTexture, Card card){

        float[] region = cardsTexture.region(card.getValue().i, card.getSuit().i, Card.WIDTH, Card.HEIGHT);
        front = new Rectangle(shader, Card.WIDTH, Card.HEIGHT, region[0], region[1], region[2], region[3]);

        if(back == null){
            region = cardsTexture.region(CARD_BACK_REGION_X, CARD_BACK_REGION_Y, Card.WIDTH, Card.HEIGHT);
            back = new Rectangle(shader, Card.WIDTH, Card.HEIGHT, region[0], region[1], region[2], region[3]);
        }

        modelUniformHandle = shader.getUniformLocation("M");
    }

    public void draw(float x, float y, boolean faceUp){

        Matrix.setIdentityM(model, 0);
        Matrix.translateM(model, 0, x, y, 0);
        GLES20.glUniformMatrix4fv(modelUniformHandle, 1, false, model, 0);

        if (faceUp) {
            front.draw();
        } else {
            back.draw();
        }
    }
}
