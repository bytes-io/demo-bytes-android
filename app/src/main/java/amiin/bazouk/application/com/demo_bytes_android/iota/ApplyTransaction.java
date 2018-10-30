package amiin.bazouk.application.com.demo_bytes_android.iota;

import java.util.ArrayList;
import java.util.List;

import jota.error.ArgumentException;
import jota.model.Transaction;
import jota.model.Transfer;

public class ApplyTransaction {

    // devnet
    private String protocol;
    private String host;
    private String port;
    private int minWeightMagnitude;
    private String explorerHost;
    private String addressTo;

    public ApplyTransaction(String protocol,String host,String port,int minWeightMagnitude, String explorerHost,String addressTo){
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.minWeightMagnitude = minWeightMagnitude;
        this.explorerHost = explorerHost;
        this.addressTo = addressTo;
    }
    // mainnet
		/*String protocol = "http";
		String host = "node02.iotatoken.nl";
		String port = "14265";
		int minWeightMagnitude = 14;
		String explorerHost = "https://thetangle.org";*/

    public void applyTransaction(){
        Iota iota = new Iota(protocol, host, port);
        iota.minWeightMagnitude = minWeightMagnitude;


        String addressTo = "IETGETEQSAAJUCCKDVBBGPUNQVUFNTHNMZYUCXXBFXYOOOQOHC9PTMP9RRIMIOQRDPATHPVQXBRXIKFDDRDPQDBWTY";
        long amountIni = 1;

        try {
            List<String> tails = iota.makeTx(addressTo, amountIni);

            System.out.println(tails);
            System.out.println("\n\n see it here " + explorerHost + "/transaction/" + tails.get(0) + " \n\n" );

        } catch(Throwable e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }


    }
}
