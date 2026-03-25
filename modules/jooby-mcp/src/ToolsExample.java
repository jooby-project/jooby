package io.github.kliushnichenko.jooby.mcp.example;

import io.github.kliushnichenko.jooby.mcp.annotation.OutputSchema;
import io.github.kliushnichenko.jooby.mcp.annotation.Tool;
import io.github.kliushnichenko.jooby.mcp.annotation.ToolArg;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

/**
 * @author kliushnichenko
 */
@Singleton
public class ToolsExample {

    private static final String PI_SIGN_IMAGE = "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";

    public record ArithmeticResult(String operation, double result, String expression) {
    }

    @Tool(name = "add", description = "Adds two numbers together")
    public ArithmeticResult add(
            @ToolArg(name = "first", description = "First number to add") int a,
            @ToolArg(name = "second", description = "Second number to add") int b
    ) {
        int result = a + b;
        return new ArithmeticResult("addition", result, a + " + " + b + " = " + result);
    }

    @Tool
    public String subtract(int a, int b, McpSyncServerExchange exchange) {
        int result = a - b;
        return String.valueOf(result);
    }

    @Tool(name = "pi_sign_image", description = "Returns an image of the Pi")
    public McpSchema.ImageContent getPiSignImage() {
        return new McpSchema.ImageContent(null, PI_SIGN_IMAGE, "image/png");
    }

    @Tool(name = "get_client_info", description = "Returns the information about the client initiated the request")
    @OutputSchema.Suppressed
    public McpSchema.Implementation getClientInfo(McpSyncServerExchange exchange) {
        return exchange.getClientInfo();
    }
}
