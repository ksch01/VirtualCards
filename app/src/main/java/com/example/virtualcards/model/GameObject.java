package com.example.virtualcards.model;

public class GameObject {

    protected float x, y;
    protected float width, height;

    GameObject(float x, float y, float width, float height){
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    void setPos(float x, float y){
        this.x = x;
        this.y = y;
    }

    /**
     * Returns weather the given point is on this game object.
     * @param x
     * @param y
     * @return
     */
    boolean isOn(float x, float y){
        return  x >= this.x && x < this.x + width &&
                y >= this.y && y < this.y + height;
    }

    /**
     * Returns weather the given point is at most delta units away from the origin of this game object.
     * @param x
     * @param y
     * @param distance most possible distance from game object center for acceptance
     * @return
     */
    boolean isOn(float x, float y, float distance){
        if(distance < 0)return false;
        float a = this.x - x;
        float b = this.y - y;
        return distance <= Math.sqrt(a * a + b * b);
    }
}
