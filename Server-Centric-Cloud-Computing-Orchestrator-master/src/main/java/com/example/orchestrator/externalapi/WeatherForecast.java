package com.example.orchestrator.externalapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class WeatherForecast {
    private static final String WEATHER_FORECAST_API_URL = "http://api.weatherapi.com/v1/forecast.json?";
    private static final String MY_API_KEY = "50930ec6055a4d8fbc5144100232712";

    public static String getWeatherForecast(String location, String startDate, String endDate) {
        try {
            long forecastDays = calculateForecastDays(endDate);
            String apiUrl = WEATHER_FORECAST_API_URL +
                    "key=" + MY_API_KEY +
                    "&q=" + location +
                    "&days=" + forecastDays;

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode jsonResponse = objectMapper.readTree(response.body());
                JsonNode forecastArray = jsonResponse.path("forecast").path("forecastday");

                StringBuilder weatherData = new StringBuilder();

                for (JsonNode forecastNode : forecastArray) {
                    String date = forecastNode.path("date").asText();
                    String temperature = forecastNode.path("day").path("avgtemp_c").asText();
                    String condition = forecastNode.path("day").path("condition").path("text").asText();

                    if (date.compareTo(startDate) >= 0 && date.compareTo(endDate) <= 0) {
                        weatherData.append("Date: ").append(date).append("\n");
                        weatherData.append("Temp: ").append(temperature).append("°C\n");
                        weatherData.append("Condition: ").append(condition).append("\n\n");
                    }
                }

                return weatherData.toString();
            } else {
                System.out.println("Failed to fetch weather forecast. Status code: " + response.statusCode());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private static long calculateForecastDays(String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate today = LocalDate.now();
        LocalDate end = LocalDate.parse(endDate, formatter);

        return today.until(end).getDays() + 1;
    }
}
