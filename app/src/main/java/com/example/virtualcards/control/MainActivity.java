package com.example.virtualcards.control;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.virtualcards.R;
import com.example.virtualcards.model.Model;
import com.example.virtualcards.network.VirtualCardsServer;
import com.example.virtualcards.network.bluetooth.BluetoothNetwork;
import com.example.virtualcards.view.VirtualCardsView;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity{

    private VirtualCardsView virtualCardsView;
    private BluetoothNetwork network;

    private boolean discoveredDevice = false;
    private LinearLayout discoverDeviceView;
    private TextView discoveryInfoView;

    private boolean connectedDevice = false;
    private LinearLayout connectedDeviceView;
    private TextView connectedInfoView;
    private ConstraintLayout serverButtons;
    private ConstraintLayout clientButtons;
    private Map<BluetoothDevice, View> connectedDevices = new HashMap<>();

    private boolean client = false;
    private boolean leftLobby = false;

    private boolean inGame;

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      APP LIFECYCLE
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressLint("MissingPermission")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inGame = false;

        setContentViewMain();

        network = BluetoothNetwork.getNetwork(this);
        network.activateBluetooth();
        network.registerDiscoveredReceiver(this::discoveredDevice);
        network.registerConnectedReceiver(this::connectedDevice);
        network.registerDisconnectedReceiver(this::disconnectedDevice);
        network.registerConnectionFailedReceiver(this::connectionFailed);
        network.registerMessageReceiver(new VirtualCardsServer(network));
    }

    @Override
    protected void onPause(){
        super.onPause();

        if(inGame)
            virtualCardsView.onPause();
    }

    @Override
    protected void onResume(){
        super.onResume();

        if(inGame)
            virtualCardsView.onResume();

        hideSystemUI();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        network.onDestroy();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      LAYOUT/MENU SWITCHES
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void setContentViewMain(){
        setContentView(R.layout.main_menu);
    }

    private void setContentViewDiscovery(){
        setContentView(R.layout.discovery_menu);

        discoverDeviceView = findViewById(R.id.discoveredDevices);
        discoveryInfoView = findViewById(R.id.discoveryInfo);
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
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //      UI HELPER METHODS
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

        if(deviceName != null){
            button.setText(deviceName);
        }else{
            button.setText(deviceMac);
        }

        button.setOnClickListener(view -> {
            try {
                client = true;
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
    //      MAIN MENU ON CLICKS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public void demo(View view){
        Control.updateScreenModelRatio(this);
        Model model = Model.getModel();
        virtualCardsView = new VirtualCardsView(this, Control.getControl(model));
        model.subscribeView(virtualCardsView.getSubscriber());
        setContentView(virtualCardsView);

        float x = (Model.WIDTH) * 0.5f;
        float y = (Model.HEIGHT) * 0.5f;
        model.moveObject(model.getObject(x,  y), x, y);

        inGame = true;
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
    //      DISCOVER MENU ON CLICKS
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public void back(View view){
        network.stopDiscoverDevices();
        discoveredDevice = false;
        setContentViewMain();
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
            if (!inGame) {
                if (!leftLobby)
                    Toast.makeText(this, R.string.tst_lobby_closed, Toast.LENGTH_LONG).show();
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

    public void connectionFailed(BluetoothDevice device){
        Toast.makeText(this, R.string.tst_wrong_server, Toast.LENGTH_LONG).show();
    }
}