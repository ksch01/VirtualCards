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

    public float getX(){
        return x;
    }

    public float getY(){
        return y;
    }

    public float getWidth() {return  width;}

    public float getHeight() {return height;}

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
     * Note that the distance is passed not as unit but square unit to prevent unnecessary calculations.
     * @param x
     * @param y
     * @param distance most possible distance from game object center for acceptance in square units
     * @return
     */
    boolean isOn(float x, float y, float distance){
        if(distance < 0)return false;
        float a = this.x - x + width * 0.5f;
        float b = this.y - y + height * 0.5f;
        System.out.println("POS("+this.x+", "+this.y+") TAR("+x+", "+y+")");
        return distance >= ((a * a) + (b * b));
    }
}
