package com.example.virtualcards.view;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.example.virtualcards.model.interfaces.ModelSubscriber;

public class VirtualCardsView extends GLSurfaceView {

    private VirtualCardsRenderer renderer;
    private MotionControl control;

    public VirtualCardsView(Context context, MotionControl control) {
        super(context);

        if(control == null) throw new IllegalArgumentException("MotionControl cannot be set to null.");

        setEGLContextClientVersion(2);

        renderer = new VirtualCardsRenderer(context, this);
        setRenderer(renderer);

        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);

        this.control = control;
    }

    public ModelSubscriber getSubscriber(){
        return renderer;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e){
        return control.onTouchEvent(e);
    }
}
