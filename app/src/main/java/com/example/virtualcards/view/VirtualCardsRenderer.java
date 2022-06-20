package com.example.virtualcards.view;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;

import com.example.virtualcards.R;
import com.example.virtualcards.model.Card;
import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.Model;
import com.example.virtualcards.model.interfaces.ModelSubscriber;
import com.example.virtualcards.view.card.RenderCard;
import com.example.virtualcards.view.card.RenderCardCall;
import com.example.virtualcards.view.geometry.Rectangle;
import com.example.virtualcards.view.shader.Shader;
import com.example.virtualcards.view.texture.Texture;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class VirtualCardsRenderer implements GLSurfaceView.Renderer, ModelSubscriber {

    private final String vertexShaderCode =
            "attribute vec2 position;" +
                    "attribute vec2 texture_coordinate;"+
                    "varying vec2 pass_texture_coordinate;"+
                    "uniform mat4 M;"+
                    "uniform mat4 P;"+
                    "void main() {" +
                    "  pass_texture_coordinate = texture_coordinate;"+
                    "  gl_Position = P * M * vec4(position, 0., 1.);" +
                    "}";

    private final String fragmentShaderCode =
                    "precision mediump float;"+
                    "varying vec2 pass_texture_coordinate;"+
                    "uniform sampler2D texture_unit;" +
                    "void main() {"+
                    "  gl_FragColor = texture2D(texture_unit, pass_texture_coordinate);"+
                    //"  gl_FragColor = vec4(pass_texture_coordinate.x, 0., pass_texture_coordinate.y, 1.);" + //Debug Texture Coordinates
                    "}";

    private Context context;
    private Shader shader;
    private Texture texture;
    private VirtualCardsView view;

    private RenderCard[] renderCards = new RenderCard[52];
    private RenderCardCall[] renderCardCalls = new RenderCardCall[52];

    private boolean ready = false;
    private ArrayList<GameObject> mostRecentCall = new ArrayList<>();

    public VirtualCardsRenderer(Context context, VirtualCardsView view){
        this.context = context;
        this.view = view;
    }

    @Override
    public void onSurfaceCreated(GL10 gl10, EGLConfig eglConfig) {
        GLES20.glClearColor(0.2f, 0.f, 0.2f, 1.f);

        shader = new Shader(vertexShaderCode, fragmentShaderCode);
        texture = new Texture(context, R.drawable.texture_cards);
        float[] textureRegion = texture.region(12, 3, 59, 91);
        //rect = new Rectangle(shader, 59, 91, textureRegion[0], textureRegion[1], textureRegion[2], textureRegion[3]);

        float[] projection = new float[16];

        shader.use();

        Matrix.setIdentityM(projection, 0);
        GLES20.glUniformMatrix4fv(shader.getUniformLocation("M"), 1, false, projection, 0);

        Matrix.orthoM(projection, 0, 0, Model.WIDTH, 0, Model.HEIGHT, -1, 64);
        GLES20.glUniformMatrix4fv(shader.getUniformLocation("P"), 1, false, projection, 0);

        texture.bind(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(shader.getUniformLocation("texture_unit"), 0);

        ready = true;
        update(mostRecentCall);
    }

    @Override
    public void onSurfaceChanged(GL10 gl10, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl10) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        for(int i = 0; i < renderCardCalls.length; i++){
            RenderCardCall call = renderCardCalls[i];
            if(call != null){
                renderCards[i].draw(call.x, call.y, call.faceUp);
            }
        }
    }

    @Override
    public void update(ArrayList<GameObject> gameObjects) {

        if(!ready){

            mostRecentCall = gameObjects;
        }else {

            for (int i = 0; i < renderCardCalls.length; i++) {
                renderCardCalls[i] = null;
            }
            for (GameObject object : gameObjects) {
                if (object instanceof Card) {
                    Card card = (Card) object;
                    int index = card.getValue().i + card.getSuit().i * 13;
                    renderCardCalls[index] = new RenderCardCall(card);
                    if (renderCards[index] == null) {
                        renderCards[index] = new RenderCard(shader, texture, card);
                    }
                }
            }
            view.requestRender();
        }
    }
}
