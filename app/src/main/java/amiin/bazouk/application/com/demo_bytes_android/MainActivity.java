package amiin.bazouk.application.com.demo_bytes_android;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.List;

import amiin.bazouk.application.com.demo_bytes_android.bluetooth.BluetoothThreadClient;
import amiin.bazouk.application.com.demo_bytes_android.bluetooth.BluetoothThreadServer;
import amiin.bazouk.application.com.demo_bytes_android.hotspot.MyOreoWifiManager;
import me.aflak.bluetooth.Bluetooth;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends PermissionsActivity {

    private static final int PERMISSION_ACCESS_COARSE_LOCATION_CODE = 11 ;
    private static final int UID_TETHERING = -5;
    private WebSocketServer server;
    private OkHttpClient client;
    private WebSocket webSocketClient;
    private int CLIENT_DISCONNECTED_CODE = 1000;
    private Runnable mRunnableServer;
    private Runnable mRunnableClient;
    private Handler mHandler = new Handler();
    private long mStartTXServer = 0;
    private long mStartRXServer = 0;
    private long mStartTXClient = 0;
    private long mStartRXClient = 0;
    private List<ScanResult> wifiList = new ArrayList<>();
    private WifiManager mWifiManager;
    private BroadcastReceiver mWifiScanReceiver = null;
    private BluetoothThreadClient bluetoothThreadClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        mRunnableServer = new Runnable() {
            public void run() {
                long [] res = new long[2];
                NetworkStatsManager networkStatsManager;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    networkStatsManager = getApplicationContext().getSystemService(NetworkStatsManager.class);
                    NetworkStats networkStatsWifi = null;
                    NetworkStats networkStatsMobile = null;
                    try {
                        Calendar calendar = Calendar.getInstance();
                        calendar.add(Calendar.DATE, 1);
                        if (networkStatsManager != null) {
                            networkStatsWifi = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI,
                                    "", 0, calendar.getTimeInMillis(), UID_TETHERING);
                            networkStatsMobile = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE,
                                    "", 0, calendar.getTimeInMillis(), UID_TETHERING);
                        }
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    NetworkStats.Bucket bucket;

                    if (networkStatsWifi != null) {
                        while (networkStatsWifi.hasNextBucket()) {
                            bucket = new NetworkStats.Bucket();
                            networkStatsWifi.getNextBucket(bucket);
                            res[0] += bucket.getTxBytes();
                            res[1] += bucket.getRxBytes();
                        }
                    }
                    if (networkStatsMobile != null) {
                        while (networkStatsMobile.hasNextBucket()) {
                            bucket = new NetworkStats.Bucket();
                            networkStatsMobile.getNextBucket(bucket);
                            res[0] += bucket.getTxBytes();
                            res[1] += bucket.getRxBytes();
                        }
                    }
                    if(networkStatsMobile != null || networkStatsWifi != null) {
                        res[0] -= mStartTXServer;
                        res[1] -= mStartRXServer;
                    }
                }
                else {
                    res[0] = TrafficStats.getUidTxBytes(UID_TETHERING)- mStartTXServer;
                    res[1] = TrafficStats.getUidRxBytes(UID_TETHERING)- mStartRXServer;
                }

                if(server!=null) {
                    String numberOfClients = "Number of client: " + String.valueOf(server.connections().size());
                    String totalConsumption = "Total consumption: " + String.valueOf(res[0]) + " " + String.valueOf(res[1]);
                    TextView numberOfClientsTextView = findViewById(R.id.number_of_clients);
                    TextView dataTextView = findViewById(R.id.data);
                    numberOfClientsTextView.setTypeface(null);
                    dataTextView.setTextSize(25);
                    numberOfClientsTextView.setText(numberOfClients);
                    dataTextView.setText(totalConsumption);

                    mHandler.postDelayed(mRunnableServer, 10000);
                }
            }
        };

        mRunnableClient = new Runnable() {
            public void run() {
                long [] res = new long[2];
                res[0] = TrafficStats.getTotalTxBytes()- mStartTXClient;
                res[1] = TrafficStats.getTotalRxBytes()- mStartRXClient;

                String totalConsumption = "Total consumption: "+String.valueOf(res[0])+" "+String.valueOf(res[1]);
                TextView numberOfClientsTextView = findViewById(R.id.number_of_clients);
                TextView dataTextView = findViewById(R.id.data);
                numberOfClientsTextView.setTypeface(null);
                dataTextView.setText("");
                numberOfClientsTextView.setText(totalConsumption);

                mHandler.postDelayed(mRunnableClient, 10000);
            }
        };

        findViewById(R.id.sell_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread serverThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(server==null) {
                            startServer();
                            if(server!=null){
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        getNetworkStatsServer();
                                    }
                                });
                            }
                        }
                        else{
                            stopServer();
                        }
                    }
                });
                serverThread.start();
            }
        });

        findViewById(R.id.connect_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Thread clientThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if(client == null) {
                            startClient();
                        }
                        else{
                            stopClient(CLIENT_DISCONNECTED_CODE);
                            mWifiManager.setWifiEnabled(false);
                        }
                    }
                });
                clientThread.start();
            }
        });

        setUpToolBar();
    }

    private void stopServer() {
        try {
            turnOffHotspot();
            server.stop();
            server=null;
            mHandler.removeCallbacks(mRunnableServer);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    findViewById(R.id.connect_button).setEnabled(true);
                    ((Button)findViewById(R.id.sell_button)).setText(getResources().getString(R.string.sell));
                    goBackToFirstTextViews();
                    mStartTXServer = 0;
                    mStartRXServer = 0;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void turnOffHotspot() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MyOreoWifiManager myOreoWifiManager = new MyOreoWifiManager(this);
            myOreoWifiManager.stopTethering();
        }
        else{
            if(mWifiManager == null){
                mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            }
            if(mWifiManager != null) {
                mWifiManager.setWifiEnabled(true);
            }
        }
    }

    private void stopClient(int code) {
        webSocketClient.close(code,"");
        mHandler.removeCallbacks(mRunnableClient);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.sell_button).setEnabled(true);
                ((Button)findViewById(R.id.connect_button)).setText(getResources().getString(R.string.connect));
                goBackToFirstTextViews();
                mStartTXClient = 0;
                mStartRXClient = 0;
            }
        });
        client = null;
        webSocketClient = null;
    }

    private void getNetworkStatsClient() {
        mStartTXClient = TrafficStats.getTotalTxBytes();
        mStartRXClient = TrafficStats.getTotalRxBytes();

        mHandler.postDelayed(mRunnableClient, 1000);
    }

    private void goBackToFirstTextViews() {
        TextView readyToConnectTextView = findViewById(R.id.number_of_clients);
        TextView niceWordingTextView = findViewById(R.id.data);
        readyToConnectTextView.setTypeface(null,Typeface.BOLD);
        niceWordingTextView.setTextSize(15);
        readyToConnectTextView.setText(getResources().getString(R.string.ready_to_connect));
        niceWordingTextView.setText(getResources().getString(R.string.nice_wording));
    }

    private void startClient() {

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.enable();
        if(!mBluetoothAdapter.isEnabled()){
            return;
        }
        getPasswordThroughBluetooth(mBluetoothAdapter);
        long time = System.currentTimeMillis();
        while(true) {
            if (System.currentTimeMillis() > time + 90000)
            {
                return;
            }
            if(bluetoothThreadClient!=null && bluetoothThreadClient.getPassword()!=null) {
                System.out.println("The password is :"+bluetoothThreadClient.getPassword());
                break;
            }

        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_ACCESS_COARSE_LOCATION_CODE);
            }
            else{
                getWifiList();
            }
        }
        else{
            getWifiList();
        }
        if(connectToHotspot()) {
            connectToServer();
        }
    }

    private void getPasswordThroughBluetooth(final BluetoothAdapter bluetoothAdapter) {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && device.getName()!=null && device.getName().length()>=6 && device.getName().substring(0,6).equals("bytes-")) {
                        bluetoothThreadClient = new BluetoothThreadClient(device,bluetoothAdapter);
                        bluetoothThreadClient.start();
                        //Bluetooth bluetooth = new Bluetooth(MainActivity.this);
                        //bluetooth.pair(device);
                        //device.createBond();
                        //int pin=intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
                        //byte[] pinBytes;
                        //try {
                        //    pinBytes = (""+pin).getBytes("UTF-8");
                        //    device.setPin(pinBytes);
                        //} catch (UnsupportedEncodingException e) {
                        //    e.printStackTrace();
                        //}
                        //device.setPairingConfirmation(true);
                    }
                }
                if( BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int pin=intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
                        byte[] pinBytes;
                        try {
                            pinBytes = (""+pin).getBytes("UTF-8");
                            device.setPin(pinBytes);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        device.setPairingConfirmation(true);
                    }
                }
                /*if( BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {

                    }
                }*/

            }
        };
        registerReceiver(mReceiver, filter);
        bluetoothAdapter.startDiscovery();
    }

    private void connectToServer() {
        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        client = new OkHttpClient();
        final WebSocketListener webSocketListener = new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        builder.setTitle(getResources().getString(R.string.connected_to_server))
                                .setMessage(getResources().getString(R.string.connected_to_server))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert).show();
                        getNetworkStatsClient();
                        findViewById(R.id.sell_button).setEnabled(false);
                        ((Button)findViewById(R.id.connect_button)).setText(getResources().getString(R.string.disconnect));
                    }
                });
            }

            @Override
            public void onClosed(WebSocket webSocket, int code, String reason) {
                webSocket.close(code, null);
                stopClient(code);
                mWifiManager.setWifiEnabled(false);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        builder.setTitle(getResources().getString(R.string.connection_closed))
                                .setMessage(getResources().getString(R.string.connection_of_client_closed))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert).show();
                    }
                });
            }
            @Override
            public void onFailure(WebSocket webSocket, Throwable t, final Response response) {
                if(t.getClass()== ConnectException.class) {
                    mWifiManager.setWifiEnabled(false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            builder.setTitle(getResources().getString(R.string.connection_failed))
                                    .setMessage(getResources().getString(R.string.connection_of_client_failed))
                                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    }).setIcon(android.R.drawable.ic_dialog_alert).show();
                        }
                    });
                }
                if(t.getClass() == SocketException.class){
                    onClosed(webSocket,CLIENT_DISCONNECTED_CODE,"");
                }
            }
        };
        Request request = new Request.Builder().url("ws://192.168.43.1:38301").build();
        webSocketClient = client.newWebSocket(request, webSocketListener);
        client.dispatcher().executorService().shutdown();
    }

    private boolean connectToHotspot() {
        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        long time = System.currentTimeMillis();
        while(System.currentTimeMillis()<time+15000){
            if(wifiList.size()>0){
                break;
            }
        }
        boolean isConnected = false;
        for(ScanResult scanResult: wifiList){
            String ssid = scanResult.SSID;
            if(ssid.length()>=6 && ssid.substring(0,6).equals("bytes-")){
                connect(ssid,scanResult.capabilities);
                time = System.currentTimeMillis();
                while(System.currentTimeMillis()<time+15000){
                    if(isConnectedToInternet()){
                        isConnected = true;
                        break;
                    }
                }
            }
            if(isConnected){
                break;
            }
        }
        unregisterReceiver(mWifiScanReceiver);
        mWifiScanReceiver = null;
        wifiList = new ArrayList<>();
        if(!isConnected){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.setTitle(getResources().getString(R.string.connection_not_found))
                            .setMessage(getResources().getString(R.string.connection_cannot_be_found))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                }
            });
        }
        return isConnected;
    }

    private void connect(String ssid,String capabilities) {
        if(mWifiManager == null) {
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }
        if(mWifiManager!=null) {
            mWifiManager.setWifiEnabled(true);
            WifiConfiguration conf = new WifiConfiguration();
            conf.SSID = String.format("\"%s\"", ssid);
            String password = "12345678";
            conf.status = WifiConfiguration.Status.ENABLED;
            conf.priority = 40;
            if (capabilities.equals("WEP")) {
                Log.v("rht", "Configuring WEP");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
                conf.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

                if (password.matches("^[0-9a-fA-F]+$")) {
                    conf.wepKeys[0] = password;
                } else {
                    conf.wepKeys[0] = "\"".concat(password).concat("\"");
                }

                conf.wepTxKeyIndex = 0;

            } else if (capabilities.contains("WPA")) {
                Log.v("rht", "Configuring WPA");

                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

                conf.preSharedKey = "\"" + password + "\"";

            } else {
                Log.v("rht", "Configuring OPEN network");
                conf.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
                conf.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
                conf.allowedAuthAlgorithms.clear();
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
                conf.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
                conf.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            }

            int netId = mWifiManager.addNetwork(conf);
            if (netId == -1) {
                netId = getExistingNetworkId(conf.SSID,mWifiManager);
            }

            mWifiManager.disconnect();
            mWifiManager.enableNetwork(netId, true);
            mWifiManager.reconnect();
        }
    }

    private int getExistingNetworkId(String SSID, WifiManager wifiManager) {
        List<WifiConfiguration> configuredNetworks = wifiManager.getConfiguredNetworks();
        if (configuredNetworks != null) {
            for (WifiConfiguration existingConfig : configuredNetworks) {
                if (existingConfig.SSID.equals(SSID)) {
                    return existingConfig.networkId;
                }
            }
        }
        return -1;
    }

    private void startServer() {
        final AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(this);
        }
        if(!isConnectedToInternet()){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.setTitle(getResources().getString(R.string.not_connected_to_internet))
                            .setMessage(getResources().getString(R.string.you_are_not_connected_to_internet))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                }
            });
            return;
        }
        turnOnHotspot();
        long time = System.currentTimeMillis();
        boolean isHotspotTurnOn = false;
        while(System.currentTimeMillis()<time+15000){
            if(isHotspotOn()){
               isHotspotTurnOn = true;
               break;
            }
        }
        if(!isHotspotTurnOn){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.setTitle(getResources().getString(R.string.turn_on_hotspot))
                            .setMessage(getResources().getString(R.string.turn_on_hotspot))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                }
            });
            return;
        }

        boolean isHotspotCorrect = isIpHotspotCorrect();
        time = System.currentTimeMillis();
        while(System.currentTimeMillis()<time+15000){
            if(isIpHotspotCorrect()){
                isHotspotCorrect = true;
                break;
            }
        }
        if(!isHotspotCorrect){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    builder.setTitle(getResources().getString(R.string.change_hotspot_address))
                            .setMessage(getResources().getString(R.string.change_hotspot_address_to_default_address))
                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                }
                            }).setIcon(android.R.drawable.ic_dialog_alert).show();
                }
            });
            return;
        }

        String ipAddress = "192.168.43.1";
        InetSocketAddress inetSockAddress = new InetSocketAddress(ipAddress,38301);
        server = new WebSocketServer(inetSockAddress){
            @Override
            public void onOpen(org.java_websocket.WebSocket conn, ClientHandshake handshake) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        builder.setTitle(getResources().getString(R.string.new_client_connected))
                                .setMessage(getResources().getString(R.string.new_client_connected))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert).show();
                    }
                });
            }
            @Override
            public void onClose(org.java_websocket.WebSocket conn, int code, String reason, boolean remote) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        builder.setTitle(getResources().getString(R.string.connection_closed))
                                .setMessage(getResources().getString(R.string.connection_of_server_closed))
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                    }
                                }).setIcon(android.R.drawable.ic_dialog_alert).show();
                    }
                });
            }

            @Override
            public void onMessage(org.java_websocket.WebSocket conn, String message) {
            }

            @Override
            public void onError(org.java_websocket.WebSocket conn, Exception ex) {
            }
        };
        server.start();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                findViewById(R.id.connect_button).setEnabled(false);
                ((Button)findViewById(R.id.sell_button)).setText(getResources().getString(R.string.stop_selling));
            }
        });
        checkIfConnectedToWifi();

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.enable();
        ensureDiscoverable();
        //acceptPairing();
        if(mBluetoothAdapter.isEnabled()) {
            new BluetoothThreadServer(mBluetoothAdapter).start();
        }
    }

    private void acceptPairing() {
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_PAIRING_REQUEST);
        filter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        final BroadcastReceiver mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if( BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        int pin=intent.getIntExtra("android.bluetooth.device.extra.PAIRING_KEY", 0);
                        byte[] pinBytes;
                        try {
                            pinBytes = (""+pin).getBytes("UTF-8");
                            device.setPin(pinBytes);
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                }
                /*if( BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && device.getBondState() == BluetoothDevice.BOND_BONDING) {
                       if(!hasWindowFocus()){
                           View v = getCurrentFocus();
                           int a = 0;
                       }
                       else{
                           View v = getCurrentFocus();
                           int a = 0;
                        }
                    }
                }*/
            }
        };
        registerReceiver(mReceiver, filter);
    }

    private void ensureDiscoverable() {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 800);
            startActivity(discoverableIntent);
    }

    private void checkIfConnectedToWifi() {
        Thread checkIfConnectedToWifiThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(true){
                    if(!isConnectedToInternet()){
                        stopServer();
                        return;
                    }
                }
            }
        });
        checkIfConnectedToWifiThread.start();
    }

    private boolean isConnectedToInternet() {
        final ConnectivityManager connectivityManager = (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager!=null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
            return networkInfo != null && networkInfo.isAvailable() && networkInfo.isConnected();
        }
        return false;
    }

    private void turnOnHotspot() {
        if(mWifiManager == null){
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        }
        WifiConfiguration wifiCon = new WifiConfiguration();
        wifiCon.SSID = "bytes-";
        wifiCon.wepKeys[0] = "12345678";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            MyOreoWifiManager myOreoWifiManager = new MyOreoWifiManager(this);
            myOreoWifiManager.startTethering();
        }
        else{
            try {
                Method method = mWifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
                method.invoke(mWifiManager, wifiCon, true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isIpHotspotCorrect() {
        try {
            for(Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                NetworkInterface intf = en.nextElement();
                for(Enumeration<InetAddress> enumIpAdress = intf.getInetAddresses(); enumIpAdress.hasMoreElements();){
                    InetAddress inetAddress = enumIpAdress.nextElement();
                    if(inetAddress.getHostAddress().equals("192.168.43.1")){
                        return true;
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return false;
    }

    private boolean isHotspotOn(){
        return new WifiApManager().isWifiApEnabled();
    }

    enum WIFI_AP_STATE {
        WIFI_AP_STATE_DISABLING,
        WIFI_AP_STATE_DISABLED,
        WIFI_AP_STATE_ENABLING,
        WIFI_AP_STATE_ENABLED,
        WIFI_AP_STATE_FAILED
    }

    public class WifiApManager {
        private final WifiManager mWifiManager;

        WifiApManager() {
            mWifiManager = (WifiManager)getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }

        /*the following method is for getting the wifi hotspot state*/

        WIFI_AP_STATE getWifiApState() {
            try {
                Method method = mWifiManager.getClass().getMethod("getWifiApState");

                int tmp = ((Integer) method.invoke(mWifiManager));

                if (tmp > 10) {
                    tmp = tmp - 10;
                }

                return WIFI_AP_STATE.class.getEnumConstants()[tmp];
            } catch (Exception e) {
                Log.e(this.getClass().toString(), "", e);
                return WIFI_AP_STATE.WIFI_AP_STATE_FAILED;
            }
        }

        /**
         * Return whether Wi-Fi Hotspot is enabled or disabled.
         *
         * @return {@code true} if Wi-Fi AP is enabled
         * @see #getWifiApState()
         */
        boolean isWifiApEnabled() {
            return getWifiApState() == WIFI_AP_STATE.WIFI_AP_STATE_ENABLED;
        }
    }

    private void getNetworkStatsServer() {
        NetworkStatsManager networkStatsManager;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            networkStatsManager = getApplicationContext().getSystemService(NetworkStatsManager.class);
            NetworkStats networkStatsWifi = null;
            NetworkStats networkStatsMobile = null;
            try {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DATE, 1);
                if (networkStatsManager != null) {
                    networkStatsWifi = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI,
                            "", 0, calendar.getTimeInMillis(), UID_TETHERING);
                    networkStatsMobile = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE,
                            "", 0, calendar.getTimeInMillis(), UID_TETHERING);
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            NetworkStats.Bucket bucket;

            if (networkStatsWifi != null) {
                while (networkStatsWifi.hasNextBucket()) {
                    bucket = new NetworkStats.Bucket();
                    networkStatsWifi.getNextBucket(bucket);
                    mStartTXServer += bucket.getTxBytes();
                    mStartRXServer += bucket.getRxBytes();
                }
            }

            if (networkStatsWifi != null) {
                if (networkStatsMobile != null) {
                    while (networkStatsMobile.hasNextBucket()) {
                        bucket = new NetworkStats.Bucket();
                        networkStatsMobile.getNextBucket(bucket);
                        mStartTXServer += bucket.getTxBytes();
                        mStartRXServer += bucket.getRxBytes();
                    }
                }
            }
        }
        else {
            mStartTXServer = TrafficStats.getUidTxBytes(UID_TETHERING);
            mStartRXServer = TrafficStats.getUidRxBytes(UID_TETHERING);
        }

        mHandler.postDelayed(mRunnableServer, 1000);
    }

    private void getWifiList() {
        if(mWifiManager!=null) {
            mWifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        }
        if(mWifiManager!=null){
            mWifiManager.setWifiEnabled(true);
            mWifiScanReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context c, Intent intent) {
                    if (intent.getAction()!= null && intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                        wifiList = mWifiManager.getScanResults();
                    }
                }
            };
            registerReceiver(mWifiScanReceiver,new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
            mWifiManager.startScan();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_ACCESS_COARSE_LOCATION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getWifiList();
                    if(connectToHotspot()) {
                        connectToServer();
                    }
                }
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        if(mWifiScanReceiver!=null) {
            unregisterReceiver(mWifiScanReceiver);
        }
    }

    @Override
    public void finish(){
        super.finish();
        if(mWifiScanReceiver!=null) {
            unregisterReceiver(mWifiScanReceiver);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mWifiScanReceiver!=null) {
            unregisterReceiver(mWifiScanReceiver);
        }
    }
    
    private void setUpToolBar(){

        ((RadioButton)findViewById(R.id.radio_button_1)).setChecked(true);
        findViewById(R.id.radio_button_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,SellActivity.class));
            }
        });
        findViewById(R.id.radio_button_3).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,ConnectActivity.class));
            }
        });
        findViewById(R.id.radio_button_4).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this,ConnectActivity.class));
            }
        });
    }
}
