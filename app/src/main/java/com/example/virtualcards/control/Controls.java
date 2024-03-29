package com.example.virtualcards.control;

import android.content.Context;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;

import androidx.annotation.NonNull;

import com.example.virtualcards.model.GameObject;
import com.example.virtualcards.model.TableModel;
import com.example.virtualcards.model.interfaces.Model;
import com.example.virtualcards.view.MotionControl;

//TODO fix x error, game objects may be selected with an offset where actually was pressed on the x axis
public class Controls implements MotionControl {

    public static final float DEFAULT_DISPLAY_WIDTH = 1920, DEFAULT_DISPLAY_HEIGHT = 1080;
    public static final float DEFAULT_RATIO_WIDTH = TableModel.WIDTH / DEFAULT_DISPLAY_WIDTH, DEFAULT_RATIO_HEIGHT = TableModel.HEIGHT / DEFAULT_DISPLAY_HEIGHT;
    private static final float MAGIC_WIDTH_FAULT_KILLER = 0.95f;

    /**
     * Square of amount of movement that is allowed before an object is seen as moved
     */
    private static final float ACTION_SENSITIVITY = 1.5f * 1.5f;
    /**
     * Time in milliseconds that has to pass having selected an object and not moved it that the extract method of the model will be called on that object
     * @see #ACTION_SENSITIVITY
     */
    private static final long EXTRACTION_TIME = 600;
    /**
     * Time in milliseconds that has to pass after selecting an object so when deselecting it the action method will not be called
     */
    private static final long ACTION_TIME = 100;

    private final Model model;
    private GameObject controlledObject;
    private static final float[] ratio = new float[]{DEFAULT_RATIO_WIDTH,DEFAULT_RATIO_HEIGHT};;

    public Controls(@NonNull Model model){
        this.model = model;
    }

    private long selectedTime;
    private boolean moved = false;
    private float lastX, lastY;
    @Override
    public boolean onTouchEvent(MotionEvent e)
    {
        float x = modelX(e.getX());
        float y = modelY(e.getY());

        switch(e.getAction()){
            case MotionEvent.ACTION_MOVE:

                if(!moved){
                    if(System.currentTimeMillis() - selectedTime >= EXTRACTION_TIME){
                        GameObject extracted = model.extractObject(controlledObject);
                        if(extracted != null)
                            controlledObject = extracted;
                        moved = true;
                    }else{
                        float dx = lastX - x;
                        float dy = lastY - y;
                        lastX = x;
                        lastY = y;
                        float delta = dx*dx+dy*dy;
                        if(delta > ACTION_SENSITIVITY){
                            moved = true;
                        }
                    }
                }
                model.moveObject(controlledObject, x, y);

                break;

            case MotionEvent.ACTION_DOWN:

                selectedTime = System.currentTimeMillis();
                lastX = x;
                lastY = y;
                controlledObject = model.getObject(x,y);
                break;

            case MotionEvent.ACTION_UP:

                if(System.currentTimeMillis() - selectedTime <= ACTION_TIME){
                    model.hitObject(controlledObject);
                }else{
                    model.dropObject(controlledObject, x, y);
                }
                controlledObject = null;
                moved = false;
                break;
        }
        return true;
    }

    public void obtainObjectAsynchronously(GameObject gameObject){
        this.controlledObject = gameObject;
    }

    private static float modelX(float screenX){
        return screenX * ratio[0];
    }
    private static float modelY(float screenY){
        return TableModel.HEIGHT - screenY * ratio[1];
    }

    public static void updateScreenModelRatio(Context context){
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(metrics);
        float pixelWidth = metrics.widthPixels;
        float pixelHeight = metrics.heightPixels;

        ratio[0] = TableModel.WIDTH / pixelWidth * MAGIC_WIDTH_FAULT_KILLER;
        ratio[1] = TableModel.HEIGHT / pixelHeight;
        Log.i("Control", "Mapped screen model ratio ("+ratio[0]+", "+ratio[1]+") with screen pixels ("+pixelWidth+", "+pixelHeight+")");
    }
}
