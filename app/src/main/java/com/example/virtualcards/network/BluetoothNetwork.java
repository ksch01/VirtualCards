package com.example.virtualcards.network;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothNetwork {

    public static final String BLUETOOTH_APP_NAME = "VirtualCards";
    public static final UUID BLUETOOTH_ID = UUID.fromString("8af00d14-3643-40ac-a34d-ea501d77f660");

    public static final int REQUEST_BLUETOOTH_ALL = 0;
    public static final int REQUEST_BLUETOOTH_ADVERTISE = 1;
    public static final int REQUEST_BLUETOOTH_SCAN = 2;
    public static final int REQUEST_BLUETOOTH_CONNECT = 3;

    private AppCompatActivity activity;
    private BluetoothAdapter bluetoothAdapter;

    private static BluetoothNetwork instance;
    private final ArrayList<BroadcastReceiver> broadcastReceivers = new ArrayList<>();
    private ArrayList<BluetoothCommunicationThread> communicationThreads = new ArrayList<>();

    private BluetoothServerThread serverThread = null;
    private BluetoothClientThread clientThread = null;

    private DeviceReceiver discoveredReceiver;
    private DeviceReceiver connectedReceiver;
    private MessageReceiver messageReceiver;

    private boolean isConnected = false;

    private final Handler handler;

    private BluetoothNetwork(@NonNull AppCompatActivity activity) {

        bluetoothAdapter = activity.getSystemService(BluetoothManager.class).getAdapter();
        if (bluetoothAdapter == null)
            throw new IllegalStateException("This device does not support bluetooth.");

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        BroadcastReceiver discoveryReceiver = new DiscoveryFoundBroadcastReceiver(this);
        activity.registerReceiver(discoveryReceiver, filter);

        broadcastReceivers.add(discoveryReceiver);

        handler = new Handler(Looper.getMainLooper()){
            @Override
            public void handleMessage(Message msg){
                Log.i("BT_NETWORK_HANDLER", "received message "+msg);
                if(msg.obj instanceof  byte[])
                    received((byte[]) msg.obj);
                else if(msg.obj instanceof BluetoothDevice){
                    connectedDevice((BluetoothDevice) msg.obj);
                }
            }
        };

        this.activity = activity;
    }

    public static BluetoothNetwork getNetwork(@NonNull AppCompatActivity activity) {
        if (instance == null)
            instance = new BluetoothNetwork(activity);

        return instance;
    }

    protected AppCompatActivity getActivity() {
        return activity;
    }

    protected BluetoothAdapter getBluetoothAdapter() {
        return bluetoothAdapter;
    }

    protected Handler getHandler() {
        return handler;
    }

    //TODO implement reaction to denial of permission in Control component
    //TODO implement reaction to disabling bluetooth by user at runtime in Control component

    /**
     * Enables the bluetooth Adapter if not already enabled.
     * If the app lacks the permission to use Bluetooth the permission is requested with the code {@link #REQUEST_BLUETOOTH_ALL}.
     */
    public void activateBluetooth() {
        requestPermissions();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            activity.startActivity(enableBluetoothIntent);
        }
    }

    private void requestPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        activity.requestPermissions(permissions.toArray(new String[permissions.size()]), REQUEST_BLUETOOTH_ALL);
    }

    /**
     * @return if bluetooth is currently enabled
     */
    public boolean isBluetoothActivated() {
        return bluetoothAdapter.isEnabled();
    }

    public void registerDiscoveredReceiver(DeviceReceiver discoveryReceiver) {
        this.discoveredReceiver = discoveryReceiver;
    }

    /**
     * Is called whenever a device is discovered by bluetooth discovery
     * @param device the bluetooth device that was discovered
     */
    void discovered(BluetoothDevice device) {
        if (discoveredReceiver != null) discoveredReceiver.received(device);
    }

    public void makeDiscoverable(int secondsDiscoverable) {
        Intent makeDiscoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        makeDiscoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, secondsDiscoverable);
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, REQUEST_BLUETOOTH_ADVERTISE);
        }
        activity.startActivity(makeDiscoverableIntent);
    }

    public void discoverDevices() {
        Log.i("BluetoothDiscovery", "Started Discovery");
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_SCAN);
        }
        bluetoothAdapter.startDiscovery();
    }

    public void stopDiscoverDevices() {
        Log.i("BluetoothDiscovery", "Canceled Discovery");
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_SCAN);
        }
        bluetoothAdapter.cancelDiscovery();
    }

    public void registerMessageReceiver(MessageReceiver receiver){
        this.messageReceiver = receiver;
    }

    private void connectedDevice(BluetoothDevice device){
        if(connectedReceiver != null) connectedReceiver.received(device);
    }

    /**
     * Is called whenever a device connected to this device as server or this device connected to another device in this network
     * @param socket the returned socket of the connection
     */
    void connected(BluetoothSocket socket) {
        BluetoothCommunicationThread communicationThread = new BluetoothCommunicationThread(handler, socket);
        communicationThreads.add(communicationThread);
        communicationThread.start();
    }

    public void registerConnectedReceiver(DeviceReceiver connectedReceiver){
        this.connectedReceiver = connectedReceiver;
    }

    public void openServer() throws IOException, IllegalStateException{
        if(isConnected)throw new IllegalStateException("Device is already running server or client.");
        isConnected = true;
        serverThread = new BluetoothServerThread(this);
        serverThread.start();
    }

    /**
     * Closes the server. After being closed no new connections can be initiated.
     * Already existing connections will not be discarded by this method.
     * @throws IllegalStateException If there is no server currently running.
     */
    public void closeServer() throws IllegalStateException{
        if(serverThread == null)throw new IllegalStateException("There is no server that could be closed.");
        serverThread.cancel();
        isConnected = false;
    }

    public void openClient(@NonNull BluetoothDevice serverDevice) throws IOException {
        if(isConnected)throw new IllegalStateException("Device is already running client or server.");
        isConnected = true;
        clientThread = new BluetoothClientThread(this, serverDevice);
        clientThread.start();
    }

    /**
     * Closes all existing connections. Does not close server for new connections.
     */
    public void closeConnections() {
        if(clientThread != null){
            clientThread.cancel();
            isConnected = false;
        }
        for(BluetoothCommunicationThread communicationThread : communicationThreads){
            communicationThread.cancel();
        }
    }

    void received(byte[] receivedBytes){
        if(messageReceiver != null)messageReceiver.received(receivedBytes);
    }

    public void send(byte[] writeBytes){
        for(BluetoothCommunicationThread communicationThread: communicationThreads){
            communicationThread.write(writeBytes);
        }
    }

    public void onDestroy(){
        for(BroadcastReceiver receiver : broadcastReceivers){
            activity.unregisterReceiver(receiver);
        }
    }
}
