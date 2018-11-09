package amiin.bazouk.application.com.demo_bytes_android.iota;

import java.util.ArrayList;
import java.util.List;
import java.text.DateFormat;
import java.util.Date;

import android.content.Context;
import amiin.bazouk.application.com.demo_bytes_android.R;
import jota.error.ArgumentException;
import jota.model.Input;

public class ApplyTransaction {
    private static Iota iota = null;

    private static String protocol;
    private static String host;
    private static String port;
    private static int minWeightMagnitude;
    private static String explorerHost;
    private static String toAddress;
    private static String senderSeed;

    public static void paySeller(Context context) {

        if (iota == null) {
            iota = createIota(context);
        }

        long amountIni = 1;
        try {
            System.out.println("before makeTx: " + DateFormat.getDateTimeInstance()
                    .format(new Date()) );
            List<String> tails = iota.makeTx(toAddress, amountIni);
            System.out.println("after makeTx: " + DateFormat.getDateTimeInstance()
                    .format(new Date()) );

            System.out.println(tails);
            System.out.println("\n\n see it here " + explorerHost + "/transaction/" + tails.get(0) + " \n\n" );

        } catch(Throwable e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public static String getCurrentAddress(Context context) throws ArgumentException {

        if (iota == null) {
            iota = createIota(context);
        }

        return iota.getCurrentAddress();
    }

    public static ResponseGetBalance getBalance(Context context) throws ArgumentException {

        if (iota == null) {
            iota = createIota(context);
        }

        long balanceInI = iota.getBalance();
        double balanceInUsd = balanceInI * 0.5;
        return new ResponseGetBalance(balanceInI, balanceInUsd);
    }

    public static ResponsePayOut payOut(Context context, String payOutAddress, long amountIni) {

        if (iota == null) {
            iota = createIota(context);
        }

        List<String> tails = new ArrayList<String>();
        try {
            tails = iota.makeTx(payOutAddress, amountIni);
        } catch(Throwable e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }

        String hash = tails.get(0);
        String link = explorerHost + "/transaction/" + tails.get(0);
        return new ResponsePayOut(hash, link, "Pending");
    }

    private static Iota createIota(Context context) {
        String network = context.getResources().getString(R.string.network);

        if (network.equals("mainnet")) {

            protocol = context.getResources().getString(R.string.mainnet_protocol);
            host = context.getResources().getString(R.string.mainnet_host);
            port = context.getResources().getString(R.string.mainnet_port);
            minWeightMagnitude = context.getResources().getInteger(R.integer.mainnet_min_weight_magnitude);
            explorerHost = context.getResources().getString(R.string.mainnet_explorer_host);
            toAddress = context.getResources().getString(R.string.mainnet_to_address);
            senderSeed = context.getResources().getString(R.string.mainnet_sender_seed);
        } else {

            protocol = context.getResources().getString(R.string.testnet_protocol);
            host = context.getResources().getString(R.string.testnet_host);
            port = context.getResources().getString(R.string.testnet_port);
            minWeightMagnitude = context.getResources().getInteger(R.integer.testnet_min_weight_magnitude);
            explorerHost = context.getResources().getString(R.string.testnet_explorer_host);
            toAddress = context.getResources().getString(R.string.testnet_to_address);
            senderSeed = context.getResources().getString(R.string.testnet_sender_seed);
        }

        System.out.println("new IOTA [start]: " + DateFormat.getDateTimeInstance()
                .format(new Date()));
        Iota iota = new Iota(protocol, host, port, senderSeed);
        System.out.println("new IOTA [done]: " + DateFormat.getDateTimeInstance()
                .format(new Date()) );

        iota.minWeightMagnitude = minWeightMagnitude;
        return iota;
    }
}

