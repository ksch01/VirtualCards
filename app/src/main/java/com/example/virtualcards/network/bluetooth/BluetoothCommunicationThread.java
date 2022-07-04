package com.example.virtualcards.network.bluetooth;

import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class BluetoothCommunicationThread extends Thread{

    private static final String TAG = "BluetoothCommsThread";

    private final BluetoothSocket socket;
    private final InputStream in;
    private final OutputStream out;
    private final Handler handler;

    private byte[] buffer;

    public BluetoothCommunicationThread(@NonNull Handler handler, @NonNull BluetoothSocket socket){
        super();

        this.socket = socket;
        InputStream in = null;
        OutputStream out = null;

        try{
            in = socket.getInputStream();
        } catch (IOException e){
            Log.e(TAG, "Could not access input stream.", e);
        }

        try{
            out = socket.getOutputStream();
        } catch (IOException e){
            Log.e(TAG, "Could not access output stream.", e);
        }

        this.in = in;
        this.out = out;
        this.handler = handler;
    }

    @Override
    public void run(){
        buffer = new byte[1024];

        while(true){
            try{
                int bytes = in.read(buffer);
                Log.i(TAG, "Successfully read "+bytes+" bytes.");

                handler.obtainMessage(BluetoothNetwork.HANDLER_TYPE_MESSAGE, bytes, -1, buffer).sendToTarget();
            }catch (IOException e){
                Log.d(TAG, "Input stream disconnected.");
                break;
            }
        }

        handler.obtainMessage(BluetoothNetwork.HANDLER_TYPE_DISCONNECTED, socket.getRemoteDevice()).sendToTarget();
    }

    public void write(byte[] bytes){
        try{
            out.write(bytes);
            Log.i(TAG, "Successfully wrote " + bytes.length +" bytes");
        }catch (IOException e){
            Log.e(TAG, "Could not send data.", e);
        }
    }

    public void cancel() {
        try{
            socket.close();
        }catch (IOException e){
            Log.e(TAG, "Could not close the connected socket.", e);
        }
    }
}
