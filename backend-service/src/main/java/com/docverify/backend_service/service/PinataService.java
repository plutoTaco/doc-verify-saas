package com.docverify.backend_service.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
public class PinataService {

    @Value("${pinata.api.url}")
    private String pinataApiUrl;

    @Value("${pinata.jwt}")
    private String pinataJwt;

    private final OkHttpClient client;
    private final ObjectMapper objectMapper;

    // Dependency Injection via constructor
    public PinataService() {
        this.client = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Uploads a MultipartFile to IPFS via Pinata API and returns the CID.
     * 
     * @param file The PDF document to upload
     * @return The IPFS CID (Content Identifier) string
     */
    public String uploadToIpfs(MultipartFile file) throws IOException {
        // 1. Target the correct Pinata v2 pinning endpoint
        String endpoint = pinataApiUrl + "/pinning/pinFileToIPFS";

        // 2. Build the multipart request payload exactly how Pinata expects it
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getOriginalFilename(),
                        RequestBody.create(file.getBytes(), MediaType.parse(file.getContentType())))
                .build();

        // 3. Construct the HTTP Request with your secret JWT
        Request request = new Request.Builder()
                .url(endpoint)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + pinataJwt)
                .build();

        // 4. Execute the network call
        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed to upload to Pinata. HTTP Code: " + response.code() + " Message: "
                        + response.message());
            }

            // 5. Parse the JSON response using Spring's built-in Jackson mapper
            assert response.body() != null;
            String responseBody = response.body().string();
            JsonNode jsonNode = objectMapper.readTree(responseBody);

            // 6. Return just the CID (labeled 'IpfsHash' by Pinata)
            return jsonNode.get("IpfsHash").asText();
        }
    }
}