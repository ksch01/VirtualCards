package com.example.virtualcards.control;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.Model;
import com.example.virtualcards.view.MotionControl;

public class Control implements MotionControl {

    public static final float DEFAULT_DISPLAY_WIDTH = 1920, DEFAULT_DISPLAY_HEIGHT = 1080;
    public static final float DEFAULT_RATIO_WIDTH = Model.WIDTH / DEFAULT_DISPLAY_WIDTH, DEFAULT_RATIO_HEIGHT = Model.HEIGHT / DEFAULT_DISPLAY_HEIGHT;

    private static Control instance;
    private Model model;
    private GameObject selectedObject;
    private static float[] ratio = new float[]{DEFAULT_RATIO_WIDTH,DEFAULT_RATIO_HEIGHT};;

    private Control(Model model){
        this.model = model;
    }

    public static Control getControl(){
        if(instance == null)
            instance = new Control(Model.getModel());
        return instance;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        float x = e.getX() * ratio[0];
        float y = Model.HEIGHT - e.getY() * ratio[1];

        switch(e.getAction()){
            case MotionEvent.ACTION_MOVE:

                model.moveObject(selectedObject, x, y);
                break;
            case MotionEvent.ACTION_DOWN:

                selectedObject = model.getObject(x,y);
                System.out.println("SELECTED OBJECT: " + selectedObject);
                break;

            case MotionEvent.ACTION_UP:

                selectedObject = null;
                System.out.println("UNSELECTED OBJECT");
                break;
        }
        return true;
    }

    public static void updateScreenModelRatio(Context context){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        ratio[0] = Model.WIDTH / width;
        ratio[1] = Model.HEIGHT / height;
        System.out.println("[Updated Screen Model Ratio] ("+ratio[0]+", "+ratio[1]+")");
    }
}
