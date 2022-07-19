package com.example.virtualcards.control;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.virtualcards.network.bluetooth.BluetoothNetwork;
import com.example.virtualcards.network.bluetooth.interfaces.NetworkEventReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageReceiver;
import com.example.virtualcards.network.bluetooth.interfaces.MessageTransmitter;
import com.example.virtualcards.util.ByteIdPool;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

//TODO remove assertions after validation
//TODO free byte ids of disconnected devices
//TODO do something when there is no more space in the lobby and someone is trying to connect
public class VirtualCardsLobby implements MessageReceiver, NetworkEventReceiver {

    public static final String TAG = "VirtualCardsLobby";

    private static final byte MESSAGE_LOBBY_INFO = 0;
    private static final byte MESSAGE_JOINED = 1;
    private static final byte MESSAGE_LEFT = 2;
    private static final byte MESSAGE_START_GAME = 3;

    private static final byte PLAYER_ID_HOST = Byte.MIN_VALUE;
    private static final byte PLAYER_ID_NONE = Byte.MAX_VALUE;

    public static final int EVENT_JOINED = 0,
            EVENT_LEFT = 1,
            EVENT_GAME_STARTED = 2;

    public interface LobbyEventReceiver{
        void receive(int eventCode, UUID targetId, String targetName);
    }

    private final MessageTransmitter transmitter;
    private LobbyEventReceiver eventReceiver;
    private final boolean isClient;

    private final Map<UUID, String> clients;
    private final ByteIdPool idPool;
    private byte playerId;

    public VirtualCardsLobby(Context context, @NonNull MessageTransmitter transmitter, boolean isClient){
        this.transmitter = transmitter;
        this.isClient = isClient;
        clients = new HashMap<>();
        clients.put(transmitter.getId(), Settings.Secure.getString(context.getContentResolver(), "bluetooth_name"));
        if(!isClient) {
            idPool = new ByteIdPool();
            idPool.reserve(PLAYER_ID_NONE);
            idPool.reserve(PLAYER_ID_HOST);
            playerId = PLAYER_ID_HOST;
        }else{
            idPool = null;
            playerId = PLAYER_ID_NONE;
        }
    }

    public void registerEventReceiver(LobbyEventReceiver eventReceiver){
        this.eventReceiver = eventReceiver;
    }

    private void receiveEvent(int eventCode, UUID targetId, String targetName){
        if(eventReceiver != null)eventReceiver.receive(eventCode, targetId, targetName);
    }

    private void sendJoined(UUID id, String deviceName){
        assert !isClient : "MESSAGE_JOINED should not be send by client";

        byte[] nameBytes = deviceName.getBytes(StandardCharsets.UTF_8);
        ByteBuffer messageBuffer = ByteBuffer.wrap(new byte[nameBytes.length + 21]);
        messageBuffer.put(MESSAGE_JOINED)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits())
                .putInt(nameBytes.length)
                .put(nameBytes);

        Log.d(TAG, "Joined sent.");
        transmitter.send(messageBuffer.array());
    }

    private void receiveJoined(ByteBuffer receivedBytes){
        assert isClient : "MESSAGE_JOINED should not be received by server";

        Log.d(TAG, "Joined received.");

        UUID id = new UUID(receivedBytes.getLong(), receivedBytes.getLong());
        if(id.equals(transmitter.getId())){
            byte[] nameBytes = new byte[receivedBytes.getInt()];
            receivedBytes.get(nameBytes);
            String deviceName = new String(nameBytes, StandardCharsets.UTF_8);

            Log.i(TAG, "Received join with device name: " + deviceName);
            return;
        }

        byte[] nameBytes = new byte[receivedBytes.getInt()];
        receivedBytes.get(nameBytes);
        String deviceName = new String(nameBytes, StandardCharsets.UTF_8);

        Log.i(TAG, "Received join with device name: " + deviceName);
        receiveEvent(EVENT_JOINED, id, new String(nameBytes, StandardCharsets.UTF_8));
    }

    private void sendLeft(UUID id){
        assert !isClient : "MESSAGE_LEFT should not be send by client";

        ByteBuffer messageBuffer = ByteBuffer.wrap(new byte[17]);
        messageBuffer.put(MESSAGE_LEFT)
                .putLong(id.getMostSignificantBits())
                .putLong(id.getLeastSignificantBits());

        transmitter.send(messageBuffer.array());
    }

    private void receiveLeft(ByteBuffer receivedBytes){
        assert isClient : "MESSAGE_LEFT should not be received by server";


        UUID id = new UUID(receivedBytes.getLong(), receivedBytes.getLong());
        receiveEvent(EVENT_LEFT, id, clients.get(id));
    }

    private void sendLobbyInfo(UUID targetId){
        if(isClient){
            byte[] nameBytes = clients.get(targetId).getBytes(StandardCharsets.UTF_8);
            ByteBuffer messageBuffer = ByteBuffer.wrap(new byte[21 + nameBytes.length]);
            messageBuffer.put(MESSAGE_LOBBY_INFO)
                    .putLong(targetId.getMostSignificantBits())
                    .putLong(targetId.getLeastSignificantBits())
                    .putInt(nameBytes.length)
                    .put(nameBytes);

            Log.d(TAG, "Lobby info accept sent.");
            transmitter.send(messageBuffer.array());
        }else {
            Log.d(TAG, "Assembling lobby info message.");

            int messageSize = 6;
            Map<UUID, byte[]> names = new HashMap<>();
            for (Map.Entry<UUID, String> client : clients.entrySet()) {
                byte[] nameBytes = client.getValue().getBytes(StandardCharsets.UTF_8);
                messageSize += nameBytes.length + 20;
                names.put(client.getKey(), nameBytes);
            }
            Log.d(TAG, "Calculated total message size of " + messageSize);

            ByteBuffer messageBuffer = ByteBuffer.wrap(new byte[messageSize]);
            messageBuffer.put(MESSAGE_LOBBY_INFO)
                    .put(idPool.getNext())
                    .putInt(names.size());
            for (Map.Entry<UUID, byte[]> clientName : names.entrySet()) {
                byte[] nameBytes = clientName.getValue();
                UUID id = clientName.getKey();
                Log.d(TAG, "Adding name to message with size " + nameBytes.length);
                messageBuffer
                        .putLong(id.getMostSignificantBits())
                        .putLong(id.getLeastSignificantBits())
                        .putInt(nameBytes.length)
                        .put(nameBytes);
            }

            Log.d(TAG, "Lobby info sent.");
            transmitter.send(targetId, messageBuffer.array());
        }
    }

    private void receiveLobbyInfo(ByteBuffer receivedBytes){
        if(isClient) {
            Log.d(TAG, "Lobby info received.");

            playerId = receivedBytes.get();
            int amountNames = receivedBytes.getInt();
            Log.d(TAG, "Names in lobby info: " + amountNames);
            for (int i = 0; i < amountNames; i++) {
                UUID id = new UUID(receivedBytes.getLong(), receivedBytes.getLong());
                int length = receivedBytes.getInt();
                Log.d(TAG, "Name " + (i + 1) + " has length: " + length);
                byte[] nameBytes = new byte[length];
                receivedBytes.get(nameBytes);
                receiveEvent(EVENT_JOINED, id, new String(nameBytes, StandardCharsets.UTF_8));
            }

            sendLobbyInfo(transmitter.getId());
        }else{
            Log.d(TAG, "Lobby info accepted received.");

            UUID id = new UUID(receivedBytes.getLong(), receivedBytes.getLong());
            byte[] nameBytes = new byte[receivedBytes.getInt()];
            receivedBytes.get(nameBytes);
            String name = new String(nameBytes, StandardCharsets.UTF_8);


            clients.put(id, name);
            sendJoined(id, name);
            receiveEvent(EVENT_JOINED, id, name);
        }
    }

    private void sendStartGame(){
        assert !isClient : "MESSAGE_START_GAME should not send received by client";

        transmitter.send(new byte[]{MESSAGE_START_GAME});
    }

    private void receivedStartGame(){
        assert isClient : "MESSAGE_START_GAME should not be received by server";

        receiveEvent(EVENT_GAME_STARTED, null, String.valueOf(playerId));
    }

    @Override
    public void receive(ByteBuffer receivedBytes) {
        byte messageType = receivedBytes.get();
        Log.d(TAG, "Received Message (" + messageType + ").");
        switch (messageType){
            case MESSAGE_LOBBY_INFO:
                receiveLobbyInfo(receivedBytes);
                break;
            case MESSAGE_JOINED:
                receiveJoined(receivedBytes);
                break;
            case MESSAGE_LEFT:
                receiveLeft(receivedBytes);
                break;
            case MESSAGE_START_GAME:
                receivedStartGame();
        }
    }

    @Override
    public void receive(int eventCode, UUID receiverId, BluetoothDevice device) {
        switch (eventCode){
            case NetworkEventReceiver.EVENT_CODE_CONNECTED:
                if(!isClient) {
                    sendLobbyInfo(receiverId);
                }
                break;

            case NetworkEventReceiver.EVENT_CODE_DISCONNECTED:
                if(!isClient){

                    sendLeft(receiverId);
                    receiveEvent(EVENT_LEFT, receiverId, clients.get(receiverId));
                    clients.remove(receiverId);
                }
        }
    }

    public void startGame(){
        if(isClient)throw new IllegalStateException("Cannot start game from a client lobby.");

        sendStartGame();
        receiveEvent(EVENT_GAME_STARTED, null, String.valueOf(playerId));
    }
}
