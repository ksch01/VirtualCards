package com.example.virtualcards.view;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class GameGLSurfaceView extends GLSurfaceView {

    private GLSurfaceView.Renderer renderer;

    public GameGLSurfaceView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new GameGLRenderer();
        setRenderer(renderer);

        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }
}
