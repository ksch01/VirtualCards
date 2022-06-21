package com.example.virtualcards.view.texture;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES20;
import android.opengl.GLUtils;

public class Texture {

    private int handle;

    private int width, height;

    private Texture(){

        int[] texture = new int[1];
        GLES20.glGenTextures(1, texture, 0);
        handle = texture[0];

        if(handle == 0)throw new RuntimeException("Texture could not be created.");
    }

    public static Texture loadTexture(Context context, int resourceId){
        Texture texture = new Texture();

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;

        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resourceId, options);

        texture.width = bitmap.getWidth();
        texture.height = bitmap.getHeight();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture.handle);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
        bitmap.recycle();

        return texture;
    }

    public float[] region(float x, float y, float width, float height){
        float[] region = new float[4];
        region[0] = x / this.width;
        region[1] = y / this.height;
        region[2] = (x+width) / this.width;
        region[3] = (y+height) / this.height;
        return region;
    }
    public float[] region(int x, int y, float width, float height){
        float[] region = new float[4];
        float x1 = x * width, y1 = y * height;
        region[0] = x1 / this.width;
        region[1] = y1 / this.height;
        region[2] = (x1 + width) / this.width;
        region[3] = (y1 + height) / this.height;
        return region;
    }
    public float[] region(int x, int y, float width, float height, float offsetX, float offsetY){
        float[] region = region(x,y,width,height);
        region[0] += offsetX;
        region[1] += offsetY;
        region[2] += offsetX;
        region[3] += offsetY;
        return region;
    }

    public void bind(int textureUnit){
        GLES20.glActiveTexture(textureUnit);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, handle);
    }
}
