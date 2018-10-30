package amiin.bazouk.application.com.demo_bytes_android.iota;

import java.util.ArrayList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetNewAddressResponse;
import jota.dto.response.GetNodeInfoResponse;
import jota.error.ArgumentException;
import jota.model.Input;
import jota.model.Transaction;
import jota.model.Transfer;

public class Iota {
    private IotaAPI iotaApi;

    private String senderSeed = "XDETDPOUHPRFA9GBTNTPSYWPZVHVSJQP9DZHF9YMOLPIDHYMHHNMDJLQZM9KGMZAZSUQQ9JWRBWYJLZPU";
    public int minWeightMagnitude = 14;
    public int depth = 3;
    public int security = 2;

    public Iota(String protocol, String host, String port)
    {
        iotaApi = new IotaAPI.Builder()
                .protocol(protocol)
                .host(host)
                .port(port)
                .build();
    }

    public String getLatestMilestone() throws ArgumentException {
        GetNodeInfoResponse nodeInfo = iotaApi.getNodeInfo();
        String latestMilestoneHash = nodeInfo.getLatestMilestone();
        // System.out.println("\n NodeInfo: Latest Milestone Index: " + latestMilestoneHash);

        return latestMilestoneHash;
    }

    public List<String> makeTx(String addressTo, long amountIni) throws ArgumentException {
        boolean validateInputs = true;
        List<Input> inputs = new ArrayList<Input>();
        List<Transaction> tips = new ArrayList<Transaction>();

        List<Transfer> transfers = new ArrayList<Transfer>();
        transfers.add(new Transfer(addressTo, amountIni));

        GetNewAddressResponse getNewAddressResponse = iotaApi.getNewAddress(senderSeed, security, 0, false, 1, false);
        String remainderAddress = getNewAddressResponse.getAddresses().get(0);

        // bundle prep for all transfers
        List<String> trytesBundle = iotaApi.prepareTransfers(senderSeed, security, transfers, remainderAddress, inputs, tips, false);

        String[] trytes = trytesBundle.toArray(new String[0]);
        System.out.println("\n trytesBundle: " + trytesBundle.get(0).length());
        System.out.println("\n trytes: " + trytes[0].length());
        System.out.println(isTrytes(trytes[0], 2673));


        String reference = getLatestMilestone();
        List<Transaction> transactions = iotaApi.sendTrytes(trytes, depth, minWeightMagnitude, reference);


//		SendTransferResponse sendTransferResponse = iotaApi.sendTransfer(senderSeed, security, depth, minWeightMagnitude, transfers, inputs, "", validateInputs, false, tips);
//		List<Transaction> transactions = sendTransferResponse.getTransactions();

        System.out.println("\n transactions: " + transactions);


        List<String> tails = new ArrayList<String>();
        for (Transaction t : transactions) {
            tails.add(t.getHash());
        }
        return tails;
    }

    public Boolean verifyTx(List<String> tails) {
        return true;
    }

    public static boolean isTrytes(final String trytes, final int length) {
        return trytes.matches("^[A-Z9]{" + (length == 0 ? "0," : length) + "}$");
    }

} 

