package com.example.client.controller;

import com.example.client.form.LoginForm;
import com.example.client.form.RegistrationForm;
import com.example.client.form.TripForm;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Controller
public class ClientController {

    @Value("${orchestrator.url}")
    private String orchestratorUrl;

    @GetMapping("/register")
    public String getRegisterPage(HttpSession session) {
        // Check if the user is already logged in
        if (session.getAttribute("userId") != null) {
            return "redirect:/";  // Redirect to the home page or another appropriate page
        }
        return "register";
    }

    @PostMapping("/users")
    public String createUser(@ModelAttribute RegistrationForm registrationForm, Model model) {
        String registrationUrl = orchestratorUrl + "/api/users/register";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RegistrationForm> requestEntity = new HttpEntity<>(registrationForm, headers);

        ResponseEntity<String> response = new RestTemplate().postForEntity(registrationUrl, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            String successMessage = response.getBody();
            model.addAttribute("successMessage", successMessage);
            return "redirect:/login";
        } else {
            String errorMessage = response.getBody();
            model.addAttribute("errorMessage", errorMessage);
            return "error";
        }
    }

    @GetMapping("/login")
    public String getLoginPage(HttpSession session) {
        // Check if the user is already logged in
        if (session.getAttribute("userId") != null) {
            return "redirect:/";  // Redirect to the home page or another appropriate page
        }
        return "login";
    }

    @PostMapping("/sessions")
    public String createSession(@ModelAttribute LoginForm loginform, Model model, HttpSession session) {
        String loginUrl = orchestratorUrl + "/api/users/login";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<LoginForm> requestEntity = new HttpEntity<>(loginform, headers);

        ResponseEntity<String> response = new RestTemplate().postForEntity(loginUrl, requestEntity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode sessionDetailsNode;
            try {
                sessionDetailsNode = objectMapper.readTree(response.getBody());

                int userId = sessionDetailsNode.get("userId").asInt();
                String username = sessionDetailsNode.get("username").asText();
                String sessionId = sessionDetailsNode.get("sessionId").asText();

                session.setAttribute("userId", userId);
                session.setAttribute("username", username);
                session.setAttribute("sessionId", sessionId);

                return "redirect:/";
            } catch (IOException e) {

                e.printStackTrace();
                model.addAttribute("errorMessage", "Error parsing session details");
                return "error";
            }
        } else {
            String errorMessage = response.getBody();
            model.addAttribute("errorMessage", errorMessage);
            return "error";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpSession session, Model model) {

        Integer userId = (Integer) session.getAttribute("userId");

        if (userId != null) {

            // Communicate with the server to perform logout
            String logoutUrl = orchestratorUrl + "/api/users/logout";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Set both sessionId and userId in the headers
            headers.set("sessionId", (String) session.getAttribute("sessionId"));

            HttpEntity<String> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<String> response = new RestTemplate().exchange(
                    logoutUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {

                session.removeAttribute("userId");
                session.removeAttribute("username");
                session.removeAttribute("sessionId");

                return "redirect:/login";
            } else {
                // Handle server-side logout failure
                model.addAttribute("errorMessage", "Server-side logout failed");
                return "error";
            }
        } else {
            // User is not logged in, redirect to login page
            return "redirect:/login";
        }
    }

    @GetMapping("/trips/create")
    public String getCreateTripPage(HttpSession session) {
        // Check if the user is already logged in
        if (session.getAttribute("userId") == null) {
            return "redirect:/";  // Redirect to the home page or another appropriate page
        }
        return "create_trip";
    }

    @PostMapping("/trips")
    public String createTrip(@ModelAttribute TripForm tripForm, Model model, HttpSession session) {
        Integer userId = (Integer) session.getAttribute("userId");

        if (userId != null) {
            String createTripUrl = orchestratorUrl + "/api/users/trips/create";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Set both sessionId and userId in the headers
            headers.set("sessionId", (String) session.getAttribute("sessionId"));

            HttpEntity<TripForm> requestEntity = new HttpEntity<>(tripForm, headers);

            ResponseEntity<String> response = new RestTemplate().postForEntity(createTripUrl, requestEntity, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                return "redirect:/";
            } else {
                model.addAttribute("errorMessage", "Trip creation failed");
                return "error";
            }
        } else {
            // User is not logged in, redirect to login page
            return "redirect:/login";
        }
    }

    @GetMapping("/trips/search")
    public String searchTrips(@RequestParam String location, Model model, RedirectAttributes redirectAttributes) {
        String searchTripsUrl = orchestratorUrl + "/api/users/trips/search?location=" + location;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> requestEntity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = new RestTemplate().exchange(
                    searchTripsUrl,
                    HttpMethod.GET,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                List<TripForm> matchingTrips = objectMapper.readValue(response.getBody(), new TypeReference<List<TripForm>>() {});

                redirectAttributes.addFlashAttribute("trips", matchingTrips);

                return "redirect:/";
            } else {
                String errorMessage = "Error retrieving search results. Status: " + response.getStatusCodeValue();
                model.addAttribute("errorMessage", errorMessage);
                return "error";
            }
        } catch (HttpServerErrorException e) {
            String errorMessage = "Server error: " + e.getRawStatusCode();
            model.addAttribute("errorMessage", errorMessage);
            return "error";
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Error parsing search results");
            return "error";
        }
    }
    @PostMapping("/express-interest")
    public String expressInterest(@RequestParam("tripId") int tripId, @RequestParam("userId") int userId, Model model) {
        if (userId != 0) {
            try {
                String serverUrl = orchestratorUrl + "/api/users/express-interest";

                Map<String, Object> requestBody = Map.of(
                        "tripId", tripId,
                        "userId", userId
                );

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

                ResponseEntity<String> response = new RestTemplate().exchange(
                        serverUrl,
                        HttpMethod.POST,
                        requestEntity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.CREATED) {
                    System.out.println("Interest expressed successfully for user with ID: " + userId);
                    return "redirect:/";
                } else {
                    model.addAttribute("errorMessage", "Server-side error: " + response.getStatusCodeValue());
                    return "error";
                }
            } catch (Exception e) {
                e.printStackTrace();
                model.addAttribute("errorMessage", "Error expressing interest");
                return "error";
            }
        } else {
            return "redirect:/login";
        }
    }

    @GetMapping("/trips/{tripId}/expressed-interests")
    public String getExpressedInterests(@PathVariable("tripId") int tripId, Model model) {
        String serverUrl = orchestratorUrl + "/api/users/expressed-interests";

        try {
            // Include tripId as a request parameter
            ResponseEntity<String> response = new RestTemplate().getForEntity(serverUrl + "?tripId=" + tripId, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                List<Map<String, Object>> expressedInterests = objectMapper.readValue(response.getBody(), new TypeReference<List<Map<String, Object>>>() {});
                model.addAttribute("expressedInterests", expressedInterests);
                return "expressed-interests";
            } else if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
                // Handle 404 Not Found
                return "expressed-interests-not-found"; // You can create a custom HTML page for this
            } else {
                String errorMessage = "Error retrieving expressed interests. Status: " + response.getStatusCodeValue();
                model.addAttribute("errorMessage", errorMessage);
                return "error";
            }
        } catch (HttpServerErrorException e) {
            String errorMessage = "Server error: " + e.getRawStatusCode();
            model.addAttribute("errorMessage", errorMessage);
            return "error";
        } catch (IOException e) {
            e.printStackTrace();
            model.addAttribute("errorMessage", "Error parsing expressed interests");
            return "error";
        }
    }



    @GetMapping("/")
    public String getTrips(Model model, HttpSession session) {

        Integer userId = (Integer) session.getAttribute("userId");
        String username = (String) session.getAttribute("username");

        if (userId != null && username != null) {

            model.addAttribute("userId", userId);
            model.addAttribute("username", username);

            return "trips";
        } else {

            return "redirect:/login";
        }
    }

}
