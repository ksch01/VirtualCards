package com.example.virtualcards.control;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.example.virtualcards.R;
import com.example.virtualcards.model.Model;
import com.example.virtualcards.network.bluetooth.BluetoothNetwork;
import com.example.virtualcards.view.VirtualCardsView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VirtualCardsActivity extends AppCompatActivity{

    public enum Screen {
        MENU_MAIN, MENU_DISCOVER, MENU_LOBBY, INFO_CONNECTING, GAME
    }
    private Screen currentScreen;

    private VirtualCardsView virtualCardsView;
    private BluetoothNetwork network;

    public static final byte MESSAGE_SET_NAME = 0;
    public static final byte MESSAGE_ACK_NAME = 1;
    public static final byte MESSAGE_START_GAME = 2;

    private Map<Byte, UUID> clients = new HashMap<>();
    private UUID networkId = UUID.randomUUID();
    public static final byte NAME_HOST = Byte.MIN_VALUE;
    public static final byte NAME_NONE = Byte.MAX_VALUE;
    private byte lastName = Byte.MIN_VALUE;
    private byte networkName = Byte.MAX_VALUE;

    private boolean client = false;
    private boolean leftLobby = false;

    //      DISCOVERED MENU VARIABLES
    private boolean discoveredDevice = false;
    private LinearLayout discoverDeviceView;
    private TextView discoveryInfoView;

    //      CONNECTION INFO SCREEN VARIABLES
    private TextView connectingInfo;
    private ProgressBar connectingProgressBar;

    //      LOBBY MENU VARIABLES
    private boolean connectedDevice = false;
    private LinearLayout connectedDeviceView;
    private TextView connectedInfoView;
    private ConstraintLayout serverButtons;
    private ConstraintLayout clientButtons;
    private Map<BluetoothDevice, View> connectedDevices = new HashMap<>();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      APP LIFECYCLE
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentViewMain();

        network = BluetoothNetwork.getNetwork(this);
        network.activateBluetooth();
        network.registerDiscoveredReceiver(this::discoveredDevice);
        network.registerConnectedReceiver(this::connectedDevice);
        network.registerDisconnectedReceiver(this::disconnectedDevice);
        network.registerConnectionFailedReceiver(this::connectionFailed);
        network.registerMessageReceiver(this::receive);
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(currentScreen == Screen.GAME)
            virtualCardsView.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(currentScreen == Screen.GAME)
            virtualCardsView.onResume();

        hideSystemUI();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        network.onDestroy();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      CONTENT VIEW SWITCHES
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void setContentViewMain(){
        setContentView(R.layout.main_menu);
        currentScreen = Screen.MENU_MAIN;
    }

    private void setContentViewDiscovery(){
        setContentView(R.layout.discovery_menu);

        discoverDeviceView = findViewById(R.id.discoveredDevices);
        discoveryInfoView = findViewById(R.id.discoveryInfo);

        currentScreen = Screen.MENU_DISCOVER;
    }

    private void setContentViewConnecting(){
        setContentView(R.layout.discovery_connecting);

        connectingInfo = findViewById(R.id.connectingInfo);
        connectingProgressBar = findViewById(R.id.progressBar);

        currentScreen = Screen.INFO_CONNECTING;
    }

    private void setContentViewLobby(){
        setContentView(R.layout.lobby_menu);

        connectedDeviceView = findViewById(R.id.connectedDevices);
        connectedInfoView = findViewById(R.id.connectedInfoView);

        serverButtons = findViewById(R.id.serverButtons);
        clientButtons = findViewById(R.id.clientButtons);

        if(client){
            clientButtons.setVisibility(View.VISIBLE);
            serverButtons.setVisibility(View.GONE);
        }else{
            serverButtons.setVisibility(View.VISIBLE);
            clientButtons.setVisibility(View.GONE);
        }

        currentScreen = Screen.MENU_LOBBY;
    }

    private void setContentViewGame(){
        Control.updateScreenModelRatio(this);
        Model model = Model.getModel();
        virtualCardsView = new VirtualCardsView(this, Control.getControl(model));
        model.subscribeView(virtualCardsView.getSubscriber());

        float x = (Model.WIDTH) * 0.5f;
        float y = (Model.HEIGHT) * 0.5f;
        model.moveObject(model.getObject(x,  y), x, y);

        setContentView(virtualCardsView);

        currentScreen = Screen.GAME;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      HELPER METHODS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void hideSystemUI(){
        ActionBar actionBar = getSupportActionBar();
        if(actionBar != null)actionBar.hide();

        View decorView = getWindow().getDecorView();
        int uiOptions =
                View.SYSTEM_UI_FLAG_FULLSCREEN|
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);
    }

    private void addDeviceToDiscoveryView(BluetoothDevice device){
        String deviceName = device.getName();
        String deviceMac = device.getAddress();

        Button button = new Button(this);

        String name;
        if(deviceName != null){
            name = deviceName;
        }else{
            name = deviceMac;
        }
        button.setText(name);

        button.setOnClickListener(view -> {
            try {
                client = true;

                setContentViewConnecting();
                connectingInfo.setText("connecting to " + name);
                connectingProgressBar.setVisibility(View.VISIBLE);

                network.openClient(device);
                discoveredDevice = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        discoverDeviceView.addView(button);
    }

    private void addDeviceToConnectedView(BluetoothDevice device){
        String deviceName = device.getName();
        String deviceMac = device.getAddress();

        Button button = new Button(this);

        if(deviceName != null){
            button.setText(deviceName);
        }else{
            button.setText(deviceMac);
        }

        button.setActivated(false);

        connectedDeviceView.addView(button);
        connectedDevices.put(device, button);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      SYSTEM UI ON CLICKS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onBackPressed(){
        if(currentScreen == Screen.MENU_MAIN)
            super.onBackPressed();

        else if(currentScreen == Screen.MENU_LOBBY){
            if(client)
                leave(null);
            else
                close(null);
        }

        else if(currentScreen == Screen.MENU_DISCOVER)
            back(null);

        else if(currentScreen == Screen.INFO_CONNECTING)
            cancel(null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      MAIN MENU ON CLICKS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public void demo(View view){
        setContentViewGame();
    }

    public void join(View view){
        setContentViewDiscovery();

        network.activateBluetooth();
        network.discoverDevices();
    }

    public void host(View view){
        try {
            network.openServer();
            setContentViewLobby();
        } catch (IOException e) {
            Toast.makeText(this, "Server could not be created.", Toast.LENGTH_LONG).show();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      DISCOVER MENU/ CONNECTION INFO ON CLICKS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void back(View view){
        network.stopDiscoverDevices();
        discoveredDevice = false;
        setContentViewMain();
    }

    public void cancel(View view){
        network.closeConnections();
        client = false;

        join(null);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      LOBBY MENU ON CLICKS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void makeDiscoverable(View view){
        network.makeDiscoverable(60);
    }

    public void close(View view){
        network.closeServer();
        network.closeConnections();
        connectedDevice = false;
        setContentViewMain();
    }

    public void start(View view){
        sendGameStarted();
        setContentViewGame();
    }

    public void leave(View view){
        leftLobby = true;
        network.closeConnections();
        connectedDevice = false;
        setContentViewMain();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      NETWORK MESSAGING
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private byte getNextName(){
        if(lastName == NAME_NONE)return lastName;
        return ++lastName;
    }

    private void sendName(){
        network.send(new byte[]{MESSAGE_SET_NAME, getNextName()});
    }

    private void acknowledgeName(byte name, UUID networkId){
        byte[] message = new byte[18];
        ByteBuffer messageBuffer = ByteBuffer.wrap(message);
        messageBuffer.put(MESSAGE_ACK_NAME);
        messageBuffer.put(name);
        messageBuffer.putLong(networkId.getMostSignificantBits());
        messageBuffer.putLong(networkId.getLeastSignificantBits());
        network.send(message);
    }

    private void sendGameStarted(){
        byte[] message = new byte[1];
        message[0] = MESSAGE_START_GAME;
        network.send(message);
    }

    private void receive(ByteBuffer bytes){
        switch(bytes.get()){
            case MESSAGE_SET_NAME:
                acknowledgeName(bytes.get(), networkId);
                break;
            case MESSAGE_ACK_NAME:
                if (client){
                    if(networkName == NAME_NONE){
                        byte id = bytes.get();
                        UUID toId = new UUID(bytes.getLong(), bytes.getLong());
                        if(toId.equals(networkId)){
                            networkName = id;
                            Log.i("DeviceNetworkNamed", "Network name was set to " + id);
                        }
                        Log.i("NetworkInControl", "ack was " + id + " for " + toId + ", own is " + networkId);
                    }
                }
                else {
                    Byte name = bytes.get();
                    if(!clients.containsKey(name)) {
                        UUID id = new UUID(bytes.getLong(), bytes.getLong());
                        clients.put(name, id);
                        acknowledgeName(name, id);
                    }
                }
                break;
            case MESSAGE_START_GAME:
                setContentViewGame();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      NETWORK CALLBACKS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void discoveredDevice(BluetoothDevice device) {
        if(!discoveredDevice){
            discoveryInfoView.setVisibility(View.INVISIBLE);
        }

        addDeviceToDiscoveryView(device);
        discoveredDevice = true;
    }

    public void connectedDevice(BluetoothDevice device) {
        if(client){
            setContentViewLobby();
        }else{
            Log.i("ServerSendName", "New device connected suggesting network name to device");
            sendName();
        }

        if(!connectedDevice){
            connectedInfoView.setVisibility(View.INVISIBLE);
        }

        addDeviceToConnectedView(device);
    }

    public void disconnectedDevice(BluetoothDevice device){
        if(client) {
            if (!(currentScreen == Screen.GAME)) {
                if (!leftLobby)
                    Toast.makeText(this, R.string.info_lobby_closed, Toast.LENGTH_LONG).show();
                else
                    leftLobby = false;
                connectedDevice = false;
                client = false;
                setContentViewMain();
            }
        }else{
            connectedDeviceView.removeView(connectedDevices.get(device));
        }
    }

    //TODO fix in network: client connection seems to be automatically denied when not connecting for first time
    public void connectionFailed(BluetoothDevice device){
        Log.i("ConnectionFailed", "Connection to device (" + device.getAddress() +") failed.");
        if(currentScreen == Screen.INFO_CONNECTING) {
            connectingInfo.setText(R.string.info_wrong_server);
            connectingProgressBar.setVisibility(View.GONE);
        }
    }
}