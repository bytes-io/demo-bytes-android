package amiin.bazouk.application.com.demo_bytes_android.iota;

import java.util.ArrayList;
import java.util.List;

import jota.IotaAPI;
import jota.dto.response.GetNodeInfoResponse;
import jota.error.ArgumentException;
import jota.model.Input;
import jota.model.Transaction;
import jota.model.Transfer;

public class Iota {
    private IotaAPI iotaApi;

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

    public List<Transaction> makeTx(String senderSeed, List<Transfer> transfers) throws ArgumentException {
		boolean validateInputs = true;
		String remainder = "";
		List<Input> inputs = new ArrayList<Input>();
		List<Transaction> tips = new ArrayList<Transaction>();

		// bundle prep for all transfers
		List<String> trytesBundle = iotaApi.prepareTransfers(senderSeed, security, transfers, remainder, inputs, tips, validateInputs);
		String[] trytes = trytesBundle.toArray(new String[0]);
		String reference = getLatestMilestone();

		return iotaApi.sendTrytes(trytes, depth, minWeightMagnitude, reference);
    }
} 

