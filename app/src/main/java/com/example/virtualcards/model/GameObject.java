package com.example.virtualcards.model;

import androidx.annotation.NonNull;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;

//TODO: create interface for usage outside of model
//TODO: outsource serialization
public abstract class GameObject{

    public static final int BYTE_SIZE = 28;
    public final UUID id;

    protected float x, y;
    protected float width, height;

    public GameObject(@NonNull UUID id, float x, float y, float width, float height){
        this.id = id;
        setPos(x,y);
        this.width = width;
        this.height = height;
    }
    GameObject(float x, float y, float width, float height){
        id = UUID.randomUUID();
        setPos(x,y);
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
     * @param x x coordinate of point
     * @param y y coordinate of point
     * @return weather the point is considered on this object
     */
    boolean isOn(float x, float y){
        return  x >= this.x && x < this.x + width &&
                y >= this.y && y < this.y + height;
    }

    /**
     * Returns weather the given point is at most delta units away from the origin of this game object.
     * Note that the distance is passed not as unit but square unit to prevent unnecessary calculations.
     * @param x x coordinate of point
     * @param y y coordinate of point
     * @param distance most possible distance from game object center for acceptance in square units
     * @return true if this object is at most distance apart from the given coordinates, else false
     */
    boolean isOn(float x, float y, float distance){
        if(distance < 0)return false;
        float a = this.x - x;
        float b = this.y - y;
        return distance >= ((a * a) + (b * b));
    }

    /**
     * Returns an byte array representing this game object.
     * The returned byte array is structured as follows:
     * <ol>
     *      <li>amount of bytes sent in data as int (4 bytes)</li>
     *      <li>UUID of game object (16 bytes)</li>
     *      <li>x position as float (4 bytes)</li>
     *      <li>y position as float (4 bytes)</li>
     *      <li>type specific data (variable length)</li>
     * </ol>
     * @return byte array representing game object
     */
    public final byte[] getBytes() {
        byte[] subBytes = serialize();

        int size = BYTE_SIZE + subBytes.length + 1;
        ByteBuffer gameObject = ByteBuffer.wrap(new byte[size]);

        gameObject.putInt(size);
        gameObject.putLong(id.getMostSignificantBits());
        gameObject.putLong(id.getLeastSignificantBits());
        gameObject.putFloat(x);
        gameObject.putFloat(y);

        gameObject.put(getIdentifier());
        gameObject.put(subBytes);

        return gameObject.array();
    }

    /**
     * Returns an byte array representing the given game objects.
     * The byte array returned contains the length of the array in the first 4 bytes as int.
     * The rest is the byte representation of each game object as in {@link #getBytes()}.
     * @param gameObjects Game objects to serialize
     * @return Byte array representation of the specified game objects.
     */
    public static byte[] getMultipleBytes(List<GameObject> gameObjects) {
        if(gameObjects == null || gameObjects.isEmpty()) throw new IllegalArgumentException("List of game objects did not contain objects or was null.");

        byte[][] gameObjectBytes = new byte[gameObjects.size()][];
        int totalSize = 4;
        for(int i = 0; i < gameObjects.size(); i++){
            gameObjectBytes[i] = gameObjects.get(i).getBytes();
            totalSize += gameObjectBytes[i].length;
        }

        ByteBuffer bytes = ByteBuffer.wrap(new byte[totalSize]);
        bytes.putInt(totalSize);
        for (byte[] gameObjectByte : gameObjectBytes) {
            bytes.put(gameObjectByte);
        }
        return bytes.array();
    }

    protected abstract byte[] serialize();
    protected abstract byte getIdentifier();
}
