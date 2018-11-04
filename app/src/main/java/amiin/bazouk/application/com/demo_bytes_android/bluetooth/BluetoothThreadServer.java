package amiin.bazouk.application.com.demo_bytes_android.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;

import java.io.IOException;
import java.util.UUID;

public class BluetoothThreadServer extends Thread {
    private static final String NAME = "bluetooth-bytes";
    private final BluetoothServerSocket mmServerSocket;

    public BluetoothThreadServer(BluetoothAdapter mBluetoothAdapter) {
        BluetoothServerSocket tmp = null;
        try {
            UUID MY_UUID = UUID.fromString("F0000000-0451-4000-B000-000000000000");
            //mBluetoothAdapter.setName("bytes-bazouk");
            tmp = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
        } catch (IOException e) {

        }
        mmServerSocket = tmp;
    }

    public void run() {
        BluetoothSocket socket;
        while (true) {
            try {
                socket = mmServerSocket.accept();
            } catch (IOException e) {
                break;
            }

            if (socket != null) {
                sendData(socket);
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            }
        }
    }

    private void sendData(BluetoothSocket socket) {
        try {
            String password = "12345678";
            socket.getOutputStream().write(password.getBytes());
        } catch (IOException e) {
        }
    }

    // Closes the connect socket and causes the thread to finish.
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) {

        }
    }
}