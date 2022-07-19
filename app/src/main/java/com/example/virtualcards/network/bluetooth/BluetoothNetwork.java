package com.example.virtualcards.network.bluetooth;

import static com.example.virtualcards.network.bluetooth.interfaces.NetworkEventReceiver.EVENT_CODE_CONNECTED;
import static com.example.virtualcards.network.bluetooth.interfaces.NetworkEventReceiver.EVENT_CODE_CONNECTION_FAILED;
import static com.example.virtualcards.network.bluetooth.interfaces.NetworkEventReceiver.EVENT_CODE_DISCONNECTED;
import static com.example.virtualcards.network.bluetooth.interfaces.NetworkEventReceiver.EVENT_CODE_DISCOVERED;

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

import com.example.virtualcards.network.bluetooth.interfaces.NetworkEventReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageTransmitter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//TODO make paired devices available without having to run discovery
//TODO implement reaction to denial of permission in Control component
//TODO implement reaction to disabling bluetooth by user at runtime in Control component
public class BluetoothNetwork implements MessageTransmitter {

    private static final String LOG_TAG = "BluetoothNetwork";

    public static final String BLUETOOTH_APP_NAME = "VirtualCards";
    public static final UUID BLUETOOTH_ID = UUID.fromString("8af00d14-3643-40ac-a34d-ea501d77f660");

    public static final UUID DEVICE_NETWORK_ID = UUID.randomUUID();

    /**
     * Permission request codes
     */
    public static final int REQUEST_BLUETOOTH_ALL = 0,
        REQUEST_BLUETOOTH_ADVERTISE = 1,
        REQUEST_BLUETOOTH_SCAN = 2,
        REQUEST_BLUETOOTH_CONNECT = 3;

    /**
     * Handler message codes
     */
    protected static final int HANDLER_TYPE_MESSAGE = 0,
        HANDLER_TYPE_CONNECTED = 2,
        HANDLER_TYPE_DISCONNECTED = 3,
        HANDLER_TYPE_CONNECTION_FAILED = 4;

    private final AppCompatActivity activity;
    private final BluetoothAdapter bluetoothAdapter;

    private static BluetoothNetwork instance;
    private final ArrayList<BroadcastReceiver> broadcastReceivers = new ArrayList<>();
    private final Map<UUID, BluetoothCommunicationThread> communicationThreadMap = new HashMap<>();

    private BluetoothServerThread serverThread = null;
    private BluetoothClientThread clientThread = null;

    private NetworkEventReceiver eventReceiver;
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
                if(msg.what == HANDLER_TYPE_MESSAGE) {
                    received(ByteBuffer.wrap((byte[]) msg.obj).asReadOnlyBuffer());
                }else if(msg.what == HANDLER_TYPE_CONNECTED) {
                    Object[] content = (Object[])msg.obj;
                    connectedDevice((UUID) content[0], (BluetoothDevice) content[1]);
                }
                else if(msg.what == HANDLER_TYPE_DISCONNECTED)
                    disconnectedThread((BluetoothCommunicationThread) msg.obj);
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

    public void registerEventReceiver(NetworkEventReceiver eventReceiver) {
        this.eventReceiver = eventReceiver;
    }

    void discovered(BluetoothDevice device){
        if(eventReceiver != null)eventReceiver.receive(EVENT_CODE_DISCOVERED, null, device);
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

    private void connectedDevice(UUID deviceId, BluetoothDevice device){
        if(eventReceiver != null) eventReceiver.receive(EVENT_CODE_CONNECTED, deviceId, device);
    }

    private void disconnectedThread(BluetoothCommunicationThread thread){
        if(eventReceiver != null) {
            for (Map.Entry<UUID, BluetoothCommunicationThread> entry : communicationThreadMap.entrySet()) {
                if (entry.getValue().equals(thread)) {
                    communicationThreadMap.remove(entry.getKey());
                    eventReceiver.receive(EVENT_CODE_DISCONNECTED, entry.getKey(), thread.getDevice());
                    return;
                }
            }
        }
    }

    private void connectionFailed(BluetoothDevice device){
        if(eventReceiver != null) eventReceiver.receive(EVENT_CODE_CONNECTION_FAILED, null, device);
    }

    /**
     * Is called whenever a device connected to this device as server or this device connected to another device in this network
     * @param socket the returned socket of the connection
     */
    void connected(BluetoothSocket socket) {
        BluetoothCommunicationThread communicationThread;
        if(openServer) {
            communicationThread = new BluetoothCommunicationThread(this, socket, true);
        }else{
            communicationThread = new BluetoothCommunicationThread(this, socket);
        }
        communicationThread.start();
        if(!openServer) {
            communicationThreadMap.put(BLUETOOTH_ID, communicationThread);

            Log.d(LOG_TAG,"Send message with id: " + DEVICE_NETWORK_ID);
            byte[] idBytes = new byte[16];
            ByteBuffer buffer = ByteBuffer.wrap(idBytes);
            buffer.putLong(DEVICE_NETWORK_ID.getMostSignificantBits()).putLong(DEVICE_NETWORK_ID.getLeastSignificantBits());
            communicationThread.write(idBytes);
        }
    }

    void serverClosed(){
        openServer = false;
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
        for(BluetoothCommunicationThread communicationThread : communicationThreadMap.values()){
            communicationThread.cancel();
        }
    }

    void received(ByteBuffer receivedBytes){
        if (messageReceiver != null) messageReceiver.receive(receivedBytes);
    }

    void receivedDirect(BluetoothCommunicationThread origin, byte[] receivedBytes){

        ByteBuffer buffer = ByteBuffer.wrap(receivedBytes);
        UUID receivedId = new UUID(buffer.getLong(), buffer.getLong());

        origin.setMsgDirect(false);
        communicationThreadMap.put(receivedId, origin);
        handler.obtainMessage(HANDLER_TYPE_CONNECTED, new Object[]{receivedId,origin.getDevice()}).sendToTarget();
    }

    @Override
    public void send(byte[] message){
        for(BluetoothCommunicationThread communicationThread: communicationThreadMap.values()){
            communicationThread.write(message);
        }
    }

    @Override
    public void send(UUID target, byte[] message){
        if(target.equals(BLUETOOTH_ID)){
            send(message);
        }else {
            BluetoothCommunicationThread targetThread = communicationThreadMap.get(target);
            if (targetThread != null)
                targetThread.write(message);
        }
    }

    @Override
    public UUID getId(){
        return DEVICE_NETWORK_ID;
    }

    /**
     * Has to be called in onDestroy of main activity life cycle.
     */
    public void onDestroy(){
        for(BroadcastReceiver receiver : broadcastReceivers){
            activity.unregisterReceiver(receiver);
            broadcastReceivers.remove(receiver);
        }
    }
}
