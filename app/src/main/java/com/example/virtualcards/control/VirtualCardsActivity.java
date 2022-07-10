package com.example.virtualcards.control;

import androidx.annotation.NonNull;
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
import com.example.virtualcards.model.TableModel;
import com.example.virtualcards.model.interfaces.ModelInterface;
import com.example.virtualcards.network.VirtualCardsClient;
import com.example.virtualcards.network.VirtualCardsServer;
import com.example.virtualcards.network.bluetooth.BluetoothNetwork;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
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

    private VirtualCardsView view;
    private BluetoothNetwork network;
    private VirtualCardsLobby lobby;

    private boolean isClient = false;
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
    private final Map<UUID, View> connectedDevices = new HashMap<>();

    //      IN GAME VARIABLES
    private ModelInterface model;
    private MessageReceiver connectModel;


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
        network.registerMessageReceiver(this::receive);
        network.registerEventReceiver(this::networkEventOccurred);

        model = TableModel.getModel();
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(currentScreen == Screen.GAME)
            view.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(currentScreen == Screen.GAME)
            view.onResume();

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

        lobby = new VirtualCardsLobby(this, network, isClient);
        lobby.registerEventReceiver(this::LobbyEventOccurred);

        connectedDeviceView = findViewById(R.id.connectedDevices);
        connectedInfoView = findViewById(R.id.connectedInfoView);

        serverButtons = findViewById(R.id.serverButtons);
        clientButtons = findViewById(R.id.clientButtons);

        if(isClient){
            clientButtons.setVisibility(View.VISIBLE);
            serverButtons.setVisibility(View.GONE);
        }else{
            serverButtons.setVisibility(View.VISIBLE);
            clientButtons.setVisibility(View.GONE);
        }

        currentScreen = Screen.MENU_LOBBY;
    }

    private void setContentViewGame(byte playerId){
        Control.updateScreenModelRatio(this);
        ModelInterface remoteModel;
        if(isClient){
            VirtualCardsClient clientModel = new VirtualCardsClient(network, playerId);
            this.connectModel = clientModel;
            remoteModel = clientModel;
        }else{
            VirtualCardsServer serverModel = new VirtualCardsServer(network);
            this.connectModel = serverModel;
            remoteModel = serverModel;
        }
        view = new VirtualCardsView(this, Control.getControl(remoteModel));
        remoteModel.subscribeView(view.getSubscriber());

        float x = (TableModel.WIDTH) * 0.5f;
        float y = (TableModel.HEIGHT) * 0.5f;
        model.moveObject(model.getObject(x,  y), x, y);

        setContentView(view);

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

    private String getDeviceName(BluetoothDevice device){
        @SuppressLint("MissingPermission") String name = device.getName();
        name = (name != null) ? name : device.getAddress();
        return name;
    }

    private void addDeviceToDiscoveryView(BluetoothDevice device){
        Button button = new Button(this);

        String name = getDeviceName(device);
        button.setText(name);

        button.setOnClickListener(view -> {
            try {
                isClient = true;

                setContentViewConnecting();
                connectingInfo.setText(getString(R.string.connecting_info, name));
                connectingProgressBar.setVisibility(View.VISIBLE);

                network.openClient(device);
                discoveredDevice = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        discoverDeviceView.addView(button);
    }

    private void addDeviceToConnectedView(String name, UUID id){

        Button button = new Button(this);
        button.setText(name);

        button.setActivated(false);

        connectedDeviceView.addView(button);
        connectedDevices.put(id, button);

        if(!connectedDevice){
            connectedInfoView.setVisibility(View.INVISIBLE);
            connectedDevice = true;
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      SYSTEM UI ON CLICKS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void onBackPressed(){
        if(currentScreen == Screen.MENU_MAIN)
            super.onBackPressed();

        else if(currentScreen == Screen.MENU_LOBBY){
            if(isClient)
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
        setContentViewGame((byte)0);
    }

    public void join(View view){
        setContentViewDiscovery();

        network.activateBluetooth();
        network.discoverDevices();
    }

    public void host(View view){
        try {
            setContentViewLobby();
            network.openServer();
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
        isClient = false;

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
        lobby.startGame();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      LOBBY CALLBACKS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void LobbyEventOccurred(int event, UUID deviceId, String deviceName){
        switch(event){
            case VirtualCardsLobby.EVENT_JOINED:
                addDeviceToConnectedView(deviceName, deviceId);
                break;

            case VirtualCardsLobby.EVENT_LEFT:
                connectedDeviceView.removeView(connectedDevices.get(deviceId));
                break;

            case VirtualCardsLobby.EVENT_GAME_STARTED:
                setContentViewGame(Byte.parseByte(deviceName));
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      NETWORK CALLBACKS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    private void receive(ByteBuffer buffer) {
        Log.i("BLUETOOTH","Message received!");
        if (currentScreen == Screen.GAME) {
            Log.i("BLUETOOTH", "Massage passed to game.");
            connectModel.receive(buffer);
        } else if (lobby != null) {
            Log.i("BLUETOOTH", "Message passed to lobby.");
            lobby.receive(buffer);
        }
    }

    public void networkEventOccurred(int event, UUID deviceId, BluetoothDevice device){
        switch(event){
            case BluetoothNetwork.EVENT_CODE_DISCOVERED:
                discoveredDevice(device);
                break;

            case BluetoothNetwork.EVENT_CODE_CONNECTED:
                if(!isClient)
                    lobby.receive(event, deviceId, device);
                else
                    setContentViewLobby();
                break;

            case BluetoothNetwork.EVENT_CODE_DISCONNECTED:
                if(!isClient)
                    lobby.receive(event, deviceId, device);
                disconnectedDevice();
                break;

            case BluetoothNetwork.EVENT_CODE_CONNECTION_FAILED:
                connectionFailed(device);
        }
    }

    private void discoveredDevice(BluetoothDevice device) {
        if(!discoveredDevice){
            discoveryInfoView.setVisibility(View.INVISIBLE);
        }

        addDeviceToDiscoveryView(device);
        discoveredDevice = true;
    }

    private void disconnectedDevice(){
        if(isClient) {
            if (!(currentScreen == Screen.GAME)) {
                if (!leftLobby)
                    Toast.makeText(this, R.string.info_lobby_closed, Toast.LENGTH_LONG).show();
                else
                    leftLobby = false;
                connectedDevice = false;
                isClient = false;
                setContentViewMain();
            }
        }
    }

    private void connectionFailed(BluetoothDevice device){
        Log.i("ConnectionFailed", "Connection to device (" + device.getAddress() +") failed.");
        if(currentScreen == Screen.INFO_CONNECTING) {
            connectingInfo.setText(R.string.info_wrong_server);
            connectingProgressBar.setVisibility(View.GONE);
        }
    }
}