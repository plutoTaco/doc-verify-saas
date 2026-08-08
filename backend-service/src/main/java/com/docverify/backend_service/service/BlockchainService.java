package com.docverify.backend_service.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Bytes32;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;

import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Collections;

@Service
public class BlockchainService {

    private final Web3j web3j;
    private final Credentials credentials;
    private final String contractAddress;
    private final TransactionManager transactionManager;

    // We inject the properties directly into the constructor
    public BlockchainService(
            @Value("${blockchain.node.url}") String nodeUrl,
            @Value("${blockchain.wallet.private-key}") String privateKey,
            @Value("${blockchain.contract.address}") String contractAddress) {

        this.web3j = Web3j.build(new HttpService(nodeUrl));
        this.credentials = Credentials.create(privateKey);
        this.contractAddress = contractAddress;

        // Hardhat's default local chain ID is 31337
        this.transactionManager = new RawTransactionManager(web3j, credentials, 31337);
    }

    /**
     * Hashes the PDF and submits the transaction to the smart contract.
     * 
     * @param studentAddress The wallet address of the student receiving the
     *                       document
     * @param file           The PDF document being issued
     * @param ipfsCid        The Pinata IPFS hash
     * @return The blockchain transaction hash
     */
    public String issueDocumentOnChain(String studentAddress, MultipartFile file, String ipfsCid)
            throws IOException, NoSuchAlgorithmException {

        // 1. Generate the SHA-256 hash of the PDF file
        byte[] docHash = generateFileHash(file);

        // 2. Define the Smart Contract function we want to call
        // Solidity: function issueDocument(address student, bytes32 docHash, string
        // ipfsCid)
        Function function = new Function(
                "issueDocument",
                Arrays.asList(
                        new Address(studentAddress),
                        new Bytes32(docHash),
                        new Utf8String(ipfsCid)),
                Collections.emptyList() // We don't expect a return value from a state-changing transaction
        );

        // 3. Encode the function into Ethereum ABI hex format
        String encodedFunction = FunctionEncoder.encode(function);

        try {
            // 4. Fetch current network gas price
            BigInteger gasPrice = web3j.ethGasPrice().send().getGasPrice();
            BigInteger gasLimit = BigInteger.valueOf(300_000); // Standard limit for this contract size

            // 5. Sign and send the transaction
            EthSendTransaction transactionResponse = transactionManager.sendTransaction(
                    gasPrice,
                    gasLimit,
                    contractAddress,
                    encodedFunction,
                    BigInteger.ZERO // We are not sending any ETH/Value, just data
            );

            if (transactionResponse.hasError()) {
                throw new RuntimeException("Blockchain Error: " + transactionResponse.getError().getMessage());
            }

            // Return the transaction hash so the frontend can display it
            return transactionResponse.getTransactionHash();

        } catch (Exception e) {
            throw new RuntimeException("Failed to send transaction: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to compute SHA-256 hash of a file.
     */
    private byte[] generateFileHash(MultipartFile file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(file.getBytes());
    }
}