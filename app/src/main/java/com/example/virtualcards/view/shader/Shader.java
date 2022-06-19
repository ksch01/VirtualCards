package com.example.virtualcards.view.shader;

import android.opengl.GLES20;

public class Shader {

    private final int handle;

    public Shader(String vertexCode, String fragmentCode){
        int vertexHandle = loadShader(GLES20.GL_VERTEX_SHADER, vertexCode);
        int fragmentHandle = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentCode);

        handle = GLES20.glCreateProgram();
        GLES20.glAttachShader(handle, vertexHandle);
        GLES20.glAttachShader(handle, fragmentHandle);
        GLES20.glLinkProgram(handle);
    }

    public void use(){
        GLES20.glUseProgram(handle);
    }

    public int getAttributeLocation(String attributeName){
        return GLES20.glGetAttribLocation(handle, attributeName);
    }

    public int getUniformLocation(String uniformName){
        return GLES20.glGetUniformLocation(handle, uniformName);
    }

    private static int loadShader(int type, String shaderCode){

        int shader = GLES20.glCreateShader(type);

        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);

        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            System.out.println("[GL ERROR] - Could not compile shader: " + GLES20.glGetShaderInfoLog(shader));
        }

        return shader;
    }
}
