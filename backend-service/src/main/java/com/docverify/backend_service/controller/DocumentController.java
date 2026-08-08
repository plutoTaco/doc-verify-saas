package com.docverify.backend_service.controller;

import com.docverify.backend_service.service.BlockchainService;
import com.docverify.backend_service.service.PinataService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/documents")
@CrossOrigin(origins = "*") // Enables cross-origin requests for local frontend testing
public class DocumentController {

    private final PinataService pinataService;
    private final BlockchainService blockchainService;

    // Dependency Injection via constructor
    public DocumentController(PinataService pinataService, BlockchainService blockchainService) {
        this.pinataService = pinataService;
        this.blockchainService = blockchainService;
    }

    /**
     * Issues a document by uploading it to IPFS and registering its hash on the
     * blockchain.
     * 
     * @param studentAddress Wallet address of the recipient
     * @param file           PDF document file
     * @return JSON payload containing IPFS CID, Blockchain TX Hash, and status
     */
    @PostMapping(value = "/issue", consumes = "multipart/form-data")
    public ResponseEntity<Map<String, Object>> issueDocument(
            @RequestParam("studentAddress") String studentAddress,
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        if (file == null || file.isEmpty()) {
            response.put("success", false);
            response.put("message", "Uploaded document file cannot be empty.");
            return ResponseEntity.badRequest().body(response);
        }

        try {
            // 1. Upload the PDF to IPFS via Pinata API
            String ipfsCid = pinataService.uploadToIpfs(file);

            // 2. Hash document in memory and send raw transaction via Web3j
            String txHash = blockchainService.issueDocumentOnChain(studentAddress, file, ipfsCid);

            // 3. Construct success response payload
            response.put("success", true);
            response.put("message", "Document issued and registered on-chain successfully.");
            response.put("ipfsCid", ipfsCid);
            response.put("transactionHash", txHash);
            response.put("studentAddress", studentAddress);
            response.put("filename", file.getOriginalFilename());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to process document issuance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}