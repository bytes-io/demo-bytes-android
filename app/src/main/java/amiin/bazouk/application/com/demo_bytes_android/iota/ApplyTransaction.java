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
    private String senderSeed;
    private String addressTo;

    public ApplyTransaction(String protocol,String host,String port,int minWeightMagnitude, String explorerHost,String senderSeed,String addressTo){
        this.protocol = protocol;
        this.host = host;
        this.port = port;
        this.minWeightMagnitude = minWeightMagnitude;
        this.explorerHost = explorerHost;
        this.senderSeed = senderSeed;
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


        List<Transfer> transfers = new ArrayList<Transfer>();
        transfers.add(new Transfer(addressTo, 0));

        try {
            List<Transaction> transctions = iota.makeTx(senderSeed, transfers);

            System.out.println(transctions);
            System.out.println("\n\n see it here " + explorerHost + "/transaction/" + transctions.get(0).getHash() + " \n\n" );

        } catch(Throwable e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
