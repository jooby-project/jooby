package io.github.kliushnichenko.jooby.mcp.example;

import io.github.kliushnichenko.jooby.mcp.annotation.McpServer;
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import jakarta.inject.Singleton;
import lombok.RequiredArgsConstructor;

/**
 * @author kliushnichenko
 */
@Singleton
@McpServer("weather")
@RequiredArgsConstructor
public class WeatherServer {

    private final WeatherService weatherService;

    @Tool(name = "get_weather")
    public String getWeather(double latitude, double longitude) {
        return weatherService.getWeather(latitude, longitude);
    }
}
