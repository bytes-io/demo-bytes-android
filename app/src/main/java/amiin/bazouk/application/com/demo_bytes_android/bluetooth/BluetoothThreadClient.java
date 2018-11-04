package amiin.bazouk.application.com.demo_bytes_android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Build;

import java.io.IOException;
import java.util.UUID;

public class BluetoothThreadClient extends Thread {
    private static final UUID MY_UUID = UUID.fromString("F0000000-0451-4000-B000-000000000000");
    private final BluetoothSocket mmSocket;
    private BluetoothAdapter mBluetoothAdapter;
    private String password = null;

    public BluetoothThreadClient(BluetoothDevice device, BluetoothAdapter mBluetoothAdapter) {
        BluetoothSocket tmp = null;

        try {
            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
        } catch (IOException e) {

        }
        mmSocket = tmp;
        this.mBluetoothAdapter = mBluetoothAdapter;
    }

    public void run() {
        mBluetoothAdapter.cancelDiscovery();

        try {
            mmSocket.connect();
        } catch (IOException connectException) {
            // Unable to connect; close the socket and return.
            try {
                mmSocket.close();
            } catch (IOException closeException) {
            }
            return;
        }
        readData(mmSocket);
    }

    private void readData(BluetoothSocket mmSocket) {
        byte[] mmBuffer = new byte[1024];
        while (true) {
            try {
                int n = mmSocket.getInputStream().read(mmBuffer);
                password = new String(mmBuffer,"UTF-8").substring(0,n);
                int a = 0;
            } catch (IOException e) {
                break;
            }
        }
    }

    public String getPassword() {
        return password;
    }

    public void cancel() {
        try {
            mmSocket.close();
        } catch (IOException e) {
        }
    }
}