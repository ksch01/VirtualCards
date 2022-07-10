package com.example.virtualcards.network.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

class BluetoothCommunicationThread extends Thread{

    private static final String TAG = "BluetoothCommsThread";

    private final BluetoothNetwork network;
    private final BluetoothSocket socket;
    private final InputStream in;
    private final OutputStream out;
    private final Handler handler;

    private byte[] buffer;
    private boolean msgDirect = false;

    BluetoothCommunicationThread(@NonNull BluetoothNetwork network, @NonNull BluetoothSocket socket){
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
        this.network = network;
        this.handler = network.getHandler();
    }
    BluetoothCommunicationThread(@NonNull BluetoothNetwork network, @NonNull BluetoothSocket socket, boolean msgDirect){
        this(network, socket);
        this.msgDirect = msgDirect;
    }

    @Override
    public void run(){
        buffer = new byte[1024];

        while(true){
            try{

                int bytes = in.read(buffer);

                if(msgDirect){
                    network.receivedDirect(this, buffer);
                }else {
                    handler.obtainMessage(BluetoothNetwork.HANDLER_TYPE_MESSAGE, bytes, -1, buffer).sendToTarget();
                }
            }catch (IOException e){

                break;
            }
        }

        handler.obtainMessage(BluetoothNetwork.HANDLER_TYPE_DISCONNECTED, this).sendToTarget();
    }

    void write(byte[] bytes){
        try{
            out.write(bytes);
        }catch (IOException e){
            Log.e(TAG, "Could not send data.", e);
        }
    }

    void setMsgDirect(boolean msgDirect){
        this.msgDirect = msgDirect;
    }

    BluetoothDevice getDevice(){
        return socket.getRemoteDevice();
    }

    void cancel() {
        try{
            socket.close();
        }catch (IOException e){
            Log.e(TAG, "Could not close the connected socket.", e);
        }
    }
}
