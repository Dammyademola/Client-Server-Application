package com.example.orchestrator.externalapi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.io.IOException;
import org.json.JSONObject;

public class RandomNumberGenerator {
    private static final String RANDOM_ORG_API_URL = "https://api.random.org/json-rpc/4/invoke";
    private static final String YOUR_API_KEY = "913e3c25-b3ed-4c28-a0b6-752a88923e74";

    public int getUserId() {
        HttpClient client = HttpClient.newHttpClient();

        // Build your JSON-RPC request body with your parameters
        String requestBody = "{ \"jsonrpc\": \"2.0\", \"method\": \"generateIntegers\", " +
                "\"params\": { \"apiKey\": \"" + YOUR_API_KEY + "\", \"n\": 1, \"min\": 1, \"max\": 10000000, \"replacement\": true }, " +
                "\"id\": 42 }";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(RANDOM_ORG_API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        try {
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JSONObject jsonResponse = new JSONObject(response.body());
                if (jsonResponse.has("result")) {
                    int randomNumber = jsonResponse.getJSONObject("result")
                            .getJSONObject("random")
                            .getJSONArray("data")
                            .getInt(0);
                    return randomNumber;
                } else {
                    throw new RuntimeException("Random.org API response does not contain 'result' field.");
                }
            } else {
                throw new RuntimeException("HTTP request failed with status code: " + response.statusCode());
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during API request", e);
        }
    }
}

