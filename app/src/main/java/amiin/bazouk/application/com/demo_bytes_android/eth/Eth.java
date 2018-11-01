package amiin.bazouk.application.com.demo_bytes_android.eth;


import java.io.IOException;
import java.math.BigInteger;
import java.util.concurrent.ExecutionException;

import org.web3j.crypto.Credentials;
import org.web3j.crypto.RawTransaction;
import org.web3j.crypto.TransactionEncoder;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.EthGetBalance;
import org.web3j.protocol.core.methods.response.EthGetTransactionCount;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.utils.Convert;
import org.web3j.utils.Numeric;

public class Eth {
    public static final BigInteger GAS_PRICE = BigInteger.valueOf(20_000_000_000L);
    public static final BigInteger GAS_LIMIT_ETHER_TX = BigInteger.valueOf(21_000);
    public static final BigInteger GAS_LIMIT_GREETER_TX = BigInteger.valueOf(500_000L);

    private final Web3j web3j;
    private String privateKey = "39cfa6d3165bf4e97fd1ef2d675cdde1c14325dffe480ebc23fe1b6d23a0c7b2";
    private String fromAddress = "0x4e5Ed1b11E2909EcE662655FBe7dC037059ab0E4";

    public Eth(String provider) {
        web3j = Web3j.build(new HttpService(provider));

    }

    public String makeTx(String toAddress, long amountInWei) throws InterruptedException, ExecutionException, IOException {
        BigInteger amountWei = BigInteger.valueOf(amountInWei);

        System.out.println("Your balance before Tx: " + getBalanceEther(fromAddress) + "\n");

        System.out.println("Transfer " + amountInWei + " Wei to account");

        EthGetTransactionCount transactionCount = web3j
                .ethGetTransactionCount(fromAddress, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get();

        BigInteger nonce = transactionCount.getTransactionCount();
        System.out.println("Nonce for sending address (coinbase): " + nonce);

        RawTransaction rawTransaction = RawTransaction.createEtherTransaction(
                nonce,
                GAS_PRICE,
                GAS_LIMIT_ETHER_TX,
                toAddress,
                amountWei);

        Credentials credentials = Credentials.create(privateKey);
        byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
        String hexValue = Numeric.toHexString(signedMessage);
        EthSendTransaction ethSendTransaction = web3j.ethSendRawTransaction(hexValue).send();
        String txHash = ethSendTransaction.getTransactionHash();

        System.out.println("Tx hash: " + txHash);
        return txHash;
    }

    private BigInteger getBalanceEther(String address) throws InterruptedException, ExecutionException {
        EthGetBalance balance = web3j
                .ethGetBalance(address, DefaultBlockParameterName.LATEST)
                .sendAsync()
                .get();

        return balance.getBalance();
    }
}
