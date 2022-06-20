package com.example.virtualcards.control;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.Model;
import com.example.virtualcards.model.interfaces.ModelInterface;
import com.example.virtualcards.view.MotionControl;

public class Control implements MotionControl {

    public static final float DEFAULT_DISPLAY_WIDTH = 1920, DEFAULT_DISPLAY_HEIGHT = 1080;
    public static final float DEFAULT_RATIO_WIDTH = Model.WIDTH / DEFAULT_DISPLAY_WIDTH, DEFAULT_RATIO_HEIGHT = Model.HEIGHT / DEFAULT_DISPLAY_HEIGHT;
    private static final float MAGIC_WIDTH_FAULT_KILLER = 0.95f;

    private static final float ACTION_SENSITIVITY = 1.5f;
    private static final long EXTRACTION_TIME = 600;
    private static final long ACTION_TIME = 100;

    private static Control instance;
    private ModelInterface model;
    private GameObject selectedObject;
    private static float[] ratio = new float[]{DEFAULT_RATIO_WIDTH,DEFAULT_RATIO_HEIGHT};;

    private Control(ModelInterface model){
        if(model == null)throw new IllegalArgumentException("Model is not allowed to be null.");
        this.model = model;
    }

    public static Control getControl(ModelInterface model){
        if(instance == null)
            instance = new Control(model);
        return instance;
    }

    private long selectedTime;
    private boolean moved = false;
    private float lastX, lastY;
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        float x = e.getX() * ratio[0];
        float y = Model.HEIGHT - e.getY() * ratio[1];

        switch(e.getAction()){
            case MotionEvent.ACTION_MOVE:

                if(moved == false){
                    if(System.currentTimeMillis() - selectedTime >= EXTRACTION_TIME){
                        GameObject extracted = model.extractObject(selectedObject);
                        if(extracted != null)
                            selectedObject = extracted;
                        moved = true;
                    }else{
                        float dx = lastX - x;
                        float dy = lastY - y;
                        lastX = x;
                        lastY = y;
                        float delta = (float)Math.sqrt(dx*dx+dy*dy);
                        if(delta > ACTION_SENSITIVITY){
                            moved = true;
                        }
                    }
                }
                model.moveObject(selectedObject, x, y);

                break;

            case MotionEvent.ACTION_DOWN:

                selectedTime = System.currentTimeMillis();
                lastX = x;
                lastY = y;
                selectedObject = model.getObject(x,y);
                break;

            case MotionEvent.ACTION_UP:

                if(System.currentTimeMillis() - selectedTime <= ACTION_TIME){
                    model.hitObject(selectedObject);
                }else{
                    model.dropObject(selectedObject, x, y);
                }
                selectedObject = null;
                moved = false;
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

        ratio[0] = Model.WIDTH / width * MAGIC_WIDTH_FAULT_KILLER;
        ratio[1] = Model.HEIGHT / height;
        System.out.println("[Updated Screen Model Ratio] ("+ratio[0]+", "+ratio[1]+")");
    }
}
