package com.example.virtualcards.network.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

class BluetoothCommunicationThread extends Thread{

    private static final String TAG = "BluetoothCommsThread";

    private final BluetoothNetwork network;
    private final BluetoothSocket socket;
    private final InputStream in;
    private final OutputStream out;
    private final Handler handler;

    private byte[] buffer;
    private boolean msgDirect = false;

    private int assembly = -1;
    private ByteBuffer assemblyBuffer = ByteBuffer.allocate(4096);

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

                if(assembly == -1) {

                    byte[] message;
                    int offset = 0;

                    do {
                        int messageLength = readHeader(buffer, offset);
                        offset += 4;
                        if (messageLength > bytes - offset) {
                            assemblyBuffer.put(readMessage(buffer, bytes - offset, offset));
                            assembly = messageLength - bytes + offset;
                            break;
                        } else {

                            message = readMessage(buffer, messageLength, offset);
                            offset += messageLength;
                            if (msgDirect) {
                                network.receivedDirect(this, message);
                            } else {
                                handler.obtainMessage(BluetoothNetwork.HANDLER_TYPE_MESSAGE, message.length, -1, message).sendToTarget();
                            }
                        }
                    } while (offset < bytes);

                }else{

                    if(assembly > bytes){
                        assemblyBuffer.put(readMessage(buffer, bytes, 0));
                        assembly -= bytes;
                    }else{
                        assemblyBuffer.put(readMessage(buffer, assembly, 0));
                        if (msgDirect) {
                            network.receivedDirect(this, assemblyBuffer.array());
                        } else {
                            handler.obtainMessage(BluetoothNetwork.HANDLER_TYPE_MESSAGE, assemblyBuffer.position(), -1, assemblyBuffer.array()).sendToTarget();
                        }
                        assemblyBuffer.clear();
                        assembly = -1;
                    }
                }
            }catch (IOException e){

                break;
            }
        }

        handler.obtainMessage(BluetoothNetwork.HANDLER_TYPE_DISCONNECTED, this).sendToTarget();
    }

    void write(byte[] bytes){
        if(bytes.length > 4096)throw new IllegalArgumentException("Messages are only allowed up to 4096 bytes.");
        try{
            out.write(withHeader(bytes));
        }catch (IOException e){
            Log.e(TAG, "Could not send data.", e);
        }
    }

    private byte[] withHeader(byte[] bytes){
        int length = bytes.length;
        byte[] message = new byte[length + 4];
        message[0] = (byte) (length & 0xFF);
        message[1] = (byte) ((length >> 8) & 0xFF);
        message[2] = (byte) ((length >> 16) & 0xFF);
        message[3] = (byte) ((length >> 24) & 0xFF);
        for(int i = 0; i < bytes.length; i++){
            message[i + 4] = bytes[i];
        }
        return message;
    }

    private int readHeader(byte[] bytes, int offset){
        int header = ((int)bytes[offset + 3] << 24) | ((int)bytes[offset + 2] & 0xFF) << 16 | ((int)bytes[offset + 1] & 0xFF) << 8 | ((int)bytes[offset] & 0xFF);
        return header;
    }

    private byte[] readMessage(byte[] bytes, int length, int offset){
        byte[] message = new byte[length];
        for(int i = 0; i < length; i++){
            message[i] = bytes[i + offset];
        }
        return message;
    }

    void setMsgDirect(boolean msgDirect) {
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
