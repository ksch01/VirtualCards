package com.example.virtualcards.util;

public class ByteIdPool {
    private boolean[] used = new boolean[256];
    private byte last = Byte.MIN_VALUE + 128;

    public byte getNext(){
        byte count = Byte.MIN_VALUE;
        for(int i = last + 129; count < Byte.MAX_VALUE; i++){
            if(!used[i]){
                used[i] = true;
                return (byte)(i - 128);
            }
            count++;
        }
        throw new IllegalStateException("There is no next byte id available.");
    }

    public byte[] getUsed(){
        int count = 0;
        for(boolean b : used)count++;
        byte[] ids = new byte[count];
        count = 0;
        for(int i = 0; i < used.length; i++){
            if(used[i])
                ids[count++] = (byte)(i-128);
        }
        return ids;
    }

    public void reserve(byte b){
        used[b + 128] = true;
        last = b;
    }

    public void free(byte b){
        used[b + 128] = false;
    }
}
