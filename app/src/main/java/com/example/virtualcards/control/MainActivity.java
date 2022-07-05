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
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity{

    public enum Screen {
        MENU_MAIN, MENU_DISCOVER, MENU_LOBBY, INFO_CONNECTING, GAME
    }
    private Screen currentScreen;

    private VirtualCardsView virtualCardsView;
    private BluetoothNetwork network;

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
        //TODO Stub
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

    //TODO fix in network: client connection automatically denied when not connecting for first time
    public void connectionFailed(BluetoothDevice device){
        Log.i("ConnectionFailed", "Connection to device " + device.getName() + " (" + device.getAddress() +") failed.");
        if(currentScreen == Screen.INFO_CONNECTING) {
            connectingInfo.setText(R.string.info_wrong_server);
            connectingProgressBar.setVisibility(View.GONE);
        }
    }
}