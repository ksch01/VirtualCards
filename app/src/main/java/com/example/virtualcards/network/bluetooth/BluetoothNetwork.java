package com.example.virtualcards.network.bluetooth;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.virtualcards.network.bluetooth.interfaces.DeviceReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageTransmitter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.UUID;

public class BluetoothNetwork implements MessageTransmitter {

    public static final String BLUETOOTH_APP_NAME = "VirtualCards";
    public static final UUID BLUETOOTH_ID = UUID.fromString("8af00d14-3643-40ac-a34d-ea501d77f660");

    public static final byte MESSAGE_TARGET_NETWORK = 0;
    public static final byte MESSAGE_TARGET_APPLICATION = 1;

    public static final int REQUEST_BLUETOOTH_ALL = 0;
    public static final int REQUEST_BLUETOOTH_ADVERTISE = 1;
    public static final int REQUEST_BLUETOOTH_SCAN = 2;
    public static final int REQUEST_BLUETOOTH_CONNECT = 3;

    protected static final int HANDLER_TYPE_MESSAGE = 0;
    protected static final int HANDLER_TYPE_CONNECTED = 1;
    protected static final int HANDLER_TYPE_DISCONNECTED = 2;
    protected static final int HANDLER_TYPE_CONNECTION_FAILED = 3;

    private AppCompatActivity activity;
    private BluetoothAdapter bluetoothAdapter;

    private static BluetoothNetwork instance;
    private final ArrayList<BroadcastReceiver> broadcastReceivers = new ArrayList<>();
    private ArrayList<BluetoothCommunicationThread> communicationThreads = new ArrayList<>();

    private BluetoothServerThread serverThread = null;
    private BluetoothClientThread clientThread = null;

    private DeviceReceiver discoveredReceiver;
    private DeviceReceiver connectedReceiver;
    private DeviceReceiver disconnectedReceiver;
    private DeviceReceiver connectionFailedReceiver;
    private MessageReceiver messageReceiver;

    private boolean openServer = false;

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
                if(msg.what == HANDLER_TYPE_MESSAGE)
                    received(ByteBuffer.wrap((byte[]) msg.obj));
                else if(msg.what == HANDLER_TYPE_CONNECTED)
                    connectedDevice((BluetoothDevice) msg.obj);
                else if(msg.what == HANDLER_TYPE_DISCONNECTED)
                    disconnectedDevice((BluetoothDevice) msg.obj);
                else if(msg.what == HANDLER_TYPE_CONNECTION_FAILED)
                    connectionFailed((BluetoothDevice) msg.obj);
            }
        };

        this.activity = activity;
    }

    /**
     * Returns the instance of the network according to the singleton pattern.
     * If an instance has been created previously the provided activity is not used.
     * @param activity activity that the network runs in. Only has to be passed when calling this method for the first time
     * @return the network instance
     */
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
     * The permissions will be requested no matter if bluetooth is already activated.
     */
    @SuppressLint("MissingPermission")
    public void activateBluetooth() {
        requestPermissions();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBluetoothIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);

            activity.startActivity(enableBluetoothIntent);
        }
    }

    /**
     * Request all permissions required to use bluetooth functionalities.
     * For API level 28 and below location permission will be requested too.
     */
    private void requestPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT);
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.BLUETOOTH_SCAN);
        if (activity.checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE);
        if (Build.VERSION.SDK_INT <= 28) {
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }
        }else{
            if (activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
            }
        }
        activity.requestPermissions(permissions.toArray(new String[0]), REQUEST_BLUETOOTH_ALL);
    }

    /**
     * Registers the specified device receiver.
     * Whenever a new device is discovered the instance of the discovered device is passed to the last registered receiver.
     * @param discoveryReceiver receiver that should receive info on new discovered devices.
     */
    public void registerDiscoveredReceiver(DeviceReceiver discoveryReceiver) {
        this.discoveredReceiver = discoveryReceiver;
    }

    /**
     * Is called whenever a device is discovered by bluetooth discovery
     * @param device the bluetooth device that was discovered
     */
    void discovered(BluetoothDevice device) {
        if (discoveredReceiver != null) discoveredReceiver.receive(device);
    }

    /**
     * Makes this device discoverable for the specified amount of time in seconds.
     * @param secondsDiscoverable amount of time in seconds this device should be discoverable.
     */
    public void makeDiscoverable(int secondsDiscoverable) {
        Intent makeDiscoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        makeDiscoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, secondsDiscoverable);
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_ADVERTISE}, REQUEST_BLUETOOTH_ADVERTISE);
        }
        activity.startActivity(makeDiscoverableIntent);
    }

    /**
     * Starts the bluetooth discovery.
     * Whenever a new device is discovered it will be submitted to the registered device receiver.
     * @see #registerDiscoveredReceiver(DeviceReceiver)
     */
    public void discoverDevices() {
        Log.i("BluetoothDiscovery", "Started Discovery");
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_SCAN);
        }
        bluetoothAdapter.startDiscovery();
    }

    /**
     * Stops bluetooth discovery if already running.
     */
    public void stopDiscoverDevices() {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            activity.requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQUEST_BLUETOOTH_SCAN);
        }
        bluetoothAdapter.cancelDiscovery();
    }

    /**
     * Registers the specified message receiver.
     * Whenever a new message is received through this bluetooth network the received byte array is passed to the last registered receiver.
     * @param receiver Receiver that should receive data transferred through this network.
     */
    public void registerMessageReceiver(MessageReceiver receiver){
        this.messageReceiver = receiver;
    }

    private void connectedDevice(BluetoothDevice device){
        if(connectedReceiver != null) connectedReceiver.receive(device);
    }

    private void disconnectedDevice(BluetoothDevice device){
        if(disconnectedReceiver != null) disconnectedReceiver.receive(device);
    }

    private void connectionFailed(BluetoothDevice device){
        if(connectionFailedReceiver != null) connectionFailedReceiver.receive(device);
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

    void serverClosed(){
        openServer = false;
    }

    /**
     * Registers the specified connected receiver.
     * Whenever a new device connected to the device running this network the connected device is passed to the last registered receiver.
     * @param connectedReceiver
     */
    public void registerConnectedReceiver(DeviceReceiver connectedReceiver){
        this.connectedReceiver = connectedReceiver;
    }

    /**
     * Registers the specified disconnected receiver.
     * Whenever a device that was connected is disconnected, no matter which side cut the connection, the device is passed to the last registered receiver.
     * @param disconnectedReceiver
     */
    public void registerDisconnectedReceiver(DeviceReceiver disconnectedReceiver){
        this.disconnectedReceiver = disconnectedReceiver;
    }

    public void registerConnectionFailedReceiver(DeviceReceiver connectionFailedReceiver){
        this.connectionFailedReceiver = connectionFailedReceiver;
    }

    /**
     * Opens this device for bluetooth connections as the server.
     * @throws IOException if the bluetooth server thread fails to start
     * @throws IllegalStateException if the device is already opened for new connections as server or client
     */
    public void openServer() throws IOException, IllegalStateException{
        if(openServer)throw new IllegalStateException("Device is already running server or client.");
        openServer = true;
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
    }

    /**
     * Opens this device for one bluetooth connection as the client.
     * @param serverDevice The device that should be connected to
     * @throws IOException If the bluetooth client thread fails to start
     */
    public void openClient(@NonNull BluetoothDevice serverDevice) throws IOException {
        clientThread = new BluetoothClientThread(this, serverDevice);
        clientThread.start();
    }

    /**
     * Closes all existing connections. Does not close server for new connections.
     */
    public void closeConnections() {
        if(clientThread != null){
            clientThread.cancel();
        }
        for(BluetoothCommunicationThread communicationThread : communicationThreads){
            communicationThread.cancel();
        }
    }

    void received(ByteBuffer receivedBytes){
        if(messageReceiver != null)messageReceiver.receive(receivedBytes);
    }

    @Override
    public void send(byte[] writeBytes){
        for(BluetoothCommunicationThread communicationThread: communicationThreads){
            communicationThread.write(writeBytes);
        }
    }

    /**
     * Has to be called in onDestroy of main activity life cycle.
     */
    public void onDestroy(){
        for(BroadcastReceiver receiver : broadcastReceivers){
            activity.unregisterReceiver(receiver);
        }
    }
}
