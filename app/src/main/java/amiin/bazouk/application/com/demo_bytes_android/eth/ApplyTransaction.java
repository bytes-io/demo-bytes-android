package amiin.bazouk.application.com.demo_bytes_android.eth;

public class ApplyTransaction {

    private String provider;
    private String explorerHost;
    private String toAddress;

    public ApplyTransaction(String provider, String explorerHost, String toAddress){
        this.provider = provider;
        this.explorerHost = explorerHost;
        this.toAddress = toAddress;
    }

    public void applyTransaction(){

        Eth eth = new Eth(provider);
        long amountInWei = 100;

        try {
            String hash = eth.makeTx(toAddress, amountInWei);

            System.out.println("\n\n see it here " + explorerHost + "/transaction/" + hash + " \n\n" );

        } catch(Throwable e) {
            System.err.println("\nERROR: Something went wrong: " + e.getMessage());
            e.printStackTrace();
        }


    }

}
