package io.github.kliushnichenko.jooby.mcp.example;

import jakarta.inject.Singleton;

@Singleton
public class WeatherService {

    public String getWeather(double latitude, double longitude) {
        // Simulate fetching weather data for the given location
        // In a real application, this would involve calling a weather API
        return "The weather in Numenor is sunny with a temperature of 25°C.";
    }
}
