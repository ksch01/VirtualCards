package com.example.virtualcards.view;

import android.content.Context;
import android.opengl.GLSurfaceView;

import com.example.virtualcards.model.interfaces.ModelSubscriber;

public class VirtualCardsView extends GLSurfaceView {

    private VirtualCardsRenderer renderer;

    public VirtualCardsView(Context context) {
        super(context);

        setEGLContextClientVersion(2);

        renderer = new VirtualCardsRenderer(context, this);
        setRenderer(renderer);

        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    public ModelSubscriber getSubscriber(){
        return renderer;
    }
}
