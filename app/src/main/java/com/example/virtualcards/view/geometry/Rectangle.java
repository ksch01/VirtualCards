package com.example.virtualcards.view.geometry;

import android.opengl.GLES20;
import android.opengl.Matrix;

import com.example.virtualcards.view.shader.Shader;
import com.example.virtualcards.view.texture.Texture;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class Rectangle {

    private FloatBuffer vertexBuffer;

    private Shader shader;
    private int positionHandle = -1;
    private int textureCoordinateHandle = -1;
    private int textureUnitHandle = -1;
    private int modelUniformHandle = -1;

    private static Texture texture;

    private float[] model;

    public static final int POSITION_COORDINATES_PER_VERTEX = 2;
    public static final int TEXTURE_COORDINATES_PER_VERTEX = 2;
    public static final int BYTES_PER_COORDINATE = 4;
    public static final int COORDINATES_PER_VERTEX = POSITION_COORDINATES_PER_VERTEX + TEXTURE_COORDINATES_PER_VERTEX;
    public static final int BYTES_PER_VERTEX = COORDINATES_PER_VERTEX * BYTES_PER_COORDINATE;
    public static final int AMOUNT_VERTICES = 6;
    public static final int AMOUNT_BYTES = AMOUNT_VERTICES * BYTES_PER_VERTEX;

    public Rectangle(Shader shader, float width, float height, float u, float v, float u2, float v2){

        initializeModelMatrix();

        initializeBuffer(width, height, u, v, u2, v2);

        initializeShaderHandles(shader);
    }
    public Rectangle(Shader shader, Texture texture, float width, float height, float u, float v, float u2, float v2){

        initializeModelMatrix();

        initializeBuffer(width, height, u, v, u2, v2);

        this.shader = shader;
        initializeShaderHandles(shader);

        this.texture = texture;
    }

    private void initializeModelMatrix(){
        model = new float[16];
        Matrix.setIdentityM(model, 0);
    }

    private void initializeShaderHandles(Shader shader){
        positionHandle = shader.getAttributeLocation("position");
        textureCoordinateHandle = shader.getAttributeLocation("texture_coordinate");
        textureUnitHandle = shader.getUniformLocation("texture_unit");
        modelUniformHandle = shader.getUniformLocation("M");
    }

    private void initializeBuffer(float width, float height, float u, float v, float u2, float v2){
        vertexBuffer = ByteBuffer.allocateDirect(AMOUNT_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        vertexBuffer.put(0).put(0).put(u).put(v2);
        vertexBuffer.put(width).put(height).put(u2).put(v);
        vertexBuffer.put(0).put(height).put(u).put(v);
        vertexBuffer.put(0).put(0).put(u).put(v2);
        vertexBuffer.put(width).put(0).put(u2).put(v2);
        vertexBuffer.put(width).put(height).put(u2).put(v);
        vertexBuffer.position(0);
    }

    public void draw(){
        if(shader != null)
            shader.use();

        if(texture != null) {
            texture.bind(GLES20.GL_TEXTURE0);
            GLES20.glUniform1i(textureUnitHandle, 0);
        }

        vertexBuffer.position(0);
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, POSITION_COORDINATES_PER_VERTEX, GLES20.GL_FLOAT, false, BYTES_PER_VERTEX, vertexBuffer);

        vertexBuffer.position(POSITION_COORDINATES_PER_VERTEX);
        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, TEXTURE_COORDINATES_PER_VERTEX, GLES20.GL_FLOAT, false, BYTES_PER_VERTEX, vertexBuffer);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, AMOUNT_VERTICES);

        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);
    }

    public void draw(float x, float y, float rotation){
        x = x * 0.5f; y = y * 0.5f;
        Matrix.rotateM(model, 0, rotation, 0, 0, 1);
        Matrix.translateM(model, 0, x, y, 0);
        GLES20.glUniformMatrix4fv(modelUniformHandle, 1, false, model, 0);
        draw();
    }
}
