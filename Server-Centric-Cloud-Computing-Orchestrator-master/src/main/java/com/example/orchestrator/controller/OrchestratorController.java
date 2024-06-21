package com.example.orchestrator.controller;

import com.example.orchestrator.externalapi.RandomNumberGenerator;
import com.example.orchestrator.form.LoginForm;
import com.example.orchestrator.form.RegistrationForm;
import com.example.orchestrator.form.TripForm;

import com.example.orchestrator.session.SessionManager;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.orchestrator.externalapi.WeatherForecast.getWeatherForecast;

@RestController
@RequestMapping("/api/users")
public class OrchestratorController {

    private static final String USERDATA_JSON_FILE_PATH = "userdata.json";
    private static final String TRIPS_JSON_FILE_PATH = "trips.json";

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody RegistrationForm registrationForm) {
        try {
            int userId = RandomNumberGenerator();
            registrationForm.setUserId(userId);

            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(registrationForm);
            saveJsonToFile(json, USERDATA_JSON_FILE_PATH);

            String responseMessage = "Registration successful, User ID: " + userId;
            return ResponseEntity.status(HttpStatus.CREATED).body(responseMessage);

        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing registration");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<String> loginUser(@RequestBody LoginForm loginForm) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            List<String> lines = Files.readAllLines(Paths.get(USERDATA_JSON_FILE_PATH));

            for (String line : lines) {
                JsonNode userNode = objectMapper.readTree(line);

                String storedUsername = userNode.has("username") ? userNode.get("username").asText() : null;
                String storedPassword = userNode.has("password") ? userNode.get("password").asText() : null;
                int storedUserID = userNode.has("userId") ? userNode.get("userId").asInt() : -1;

                if (storedUserID != -1 && storedUsername != null && storedPassword != null
                        && storedUsername.equals(loginForm.getUsername()) && storedPassword.equals(loginForm.getPassword())) {

                    String sessionId = SessionManager.createSession(storedUserID);

                    ObjectNode sessionDetails = objectMapper.createObjectNode();
                    sessionDetails.put("sessionId", sessionId);
                    sessionDetails.put("userId", storedUserID);
                    sessionDetails.put("username", storedUsername);

                    return ResponseEntity.ok(sessionDetails.toString());
                }
            }

            return ResponseEntity.badRequest().body("Invalid credentials");
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing login");
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestHeader("sessionId") String sessionId) {
        SessionManager.invalidateSession(sessionId);
        return ResponseEntity.ok("Logout successful");
    }

    @PostMapping("/trips/create")
    public ResponseEntity<String> createTrip(@RequestBody TripForm tripForm, @RequestHeader("sessionId") String sessionId) {
        Integer userId = SessionManager.getUserIdFromSession(sessionId);
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid session");
        }

        try {
            int tripId = RandomNumberGenerator();
            String location = tripForm.getLocation();
            String startDate = tripForm.getStartDate();
            String endDate = tripForm.getEndDate();

            String weatherDesc = getWeatherForecast(location, startDate, endDate);

            tripForm.setTripId(tripId);
            tripForm.setUserId(userId);
            tripForm.setWeather(weatherDesc);

            System.out.println(tripForm.getStartDate());
            System.out.println(tripForm.getEndDate());

            ObjectMapper objectMapper = new ObjectMapper();
            String tripJson = objectMapper.writeValueAsString(tripForm);

            saveJsonToFile(tripJson, TRIPS_JSON_FILE_PATH);

            String responseMessage = "Trip created successfully, Trip ID: " + tripId;
            return ResponseEntity.status(HttpStatus.CREATED).body(responseMessage);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing trip creation");
        }
    }

    @GetMapping("/trips/search")
    public ResponseEntity<List<TripForm>> searchTrips(@RequestParam String location) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            List<String> tripLines = Files.readAllLines(Paths.get(TRIPS_JSON_FILE_PATH));
            List<TripForm> matchingTrips = new ArrayList<>();

            for (String line : tripLines) {
                TripForm tripForm = objectMapper.readValue(line, TripForm.class);
                if (tripForm.getLocation().equalsIgnoreCase(location)) {
                    matchingTrips.add(tripForm);
                }
            }

            return ResponseEntity.ok(matchingTrips);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


    @PostMapping("/express-interest")
    public ResponseEntity<String> expressInterest(@RequestBody Map<String, Object> requestBody) {
        try {
            int tripId = (int) requestBody.get("tripId");
            int userId = (int) requestBody.get("userId");

            if (isUserAlreadyExpressedInterest(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("User already expressed interest for Trip ID: " + tripId);
            }

            ObjectMapper objectMapper = new ObjectMapper();
            String expressedInterestsJsonFilePath = "expressed_interests.json";

            List<Map<String, Object>> existingInterests = new ArrayList<>();
            File existingInterestsFile = new File(expressedInterestsJsonFilePath);
            if (existingInterestsFile.exists()) {
                existingInterests = objectMapper.readValue(existingInterestsFile, new TypeReference<List<Map<String, Object>>>() {});
            }

            Map<String, Object> newInterest = new HashMap<>();
            newInterest.put("tripId", tripId);
            newInterest.put("userId", userId);
            existingInterests.add(newInterest);

            objectMapper.writeValue(existingInterestsFile, existingInterests);

            String responseMessage = "Expressed interest successfully for Trip ID: " + tripId;
            return ResponseEntity.status(HttpStatus.CREATED).body(responseMessage);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error expressing interest");
        }
    }

    private boolean isUserAlreadyExpressedInterest(int userId) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String expressedInterestsJsonFilePath = "expressed_interests.json";

        List<Map<String, Object>> existingInterests = new ArrayList<>();
        File existingInterestsFile = new File(expressedInterestsJsonFilePath);
        if (existingInterestsFile.exists()) {
            existingInterests = objectMapper.readValue(existingInterestsFile, new TypeReference<List<Map<String, Object>>>() {});
        }

        return existingInterests.stream().anyMatch(interest -> interest.containsKey("userId") && (int) interest.get("userId") == userId);
    }

    @GetMapping("expressed-interests")
    public ResponseEntity<List<Map<String, Object>>> getExpressedInterestsForTrip(@RequestParam int tripId) {
        try {
            List<Map<String, Object>> allExpressedInterests = readExpressedInterestsFromFile();

            List<Map<String, Object>> expressedInterestsForTrip = allExpressedInterests.stream()
                    .filter(interest -> interest.containsKey("tripId") && (int) interest.get("tripId") == tripId)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(expressedInterestsForTrip);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private List<Map<String, Object>> readExpressedInterestsFromFile() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String expressedInterestsJsonFilePath = "expressed_interests.json";

        List<Map<String, Object>> existingInterests = new ArrayList<>();
        File existingInterestsFile = new File(expressedInterestsJsonFilePath);
        if (existingInterestsFile.exists()) {
            existingInterests = objectMapper.readValue(existingInterestsFile, new TypeReference<List<Map<String, Object>>>() {});
        }

        return existingInterests;
    }

    private int RandomNumberGenerator() {
        RandomNumberGenerator randomNumberGenerator = new RandomNumberGenerator();
        return randomNumberGenerator.getUserId();
    }

    private void saveJsonToFile(String json, String filePath) throws IOException {
        try (FileWriter writer = new FileWriter(new File(filePath), true)) {
            writer.write(json);
            writer.write(System.lineSeparator());
        } catch (IOException e) {
            throw new IOException("Error saving JSON to file", e);
        }
    }
}