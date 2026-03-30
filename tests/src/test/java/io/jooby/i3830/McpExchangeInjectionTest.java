/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3830;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.atomic.AtomicReference;

import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.mcp.McpModule;
import io.jooby.mcp.jackson3.McpJackson3Module;

public class McpExchangeInjectionTest {

  @ServerTest
  public void shouldInjectExchangeAndAccessSession(ServerTestRunner runner) throws Exception {
    runner
        .define(
            app -> {
              app.install(new Jackson3Module());
              app.install(new McpJackson3Module());
              // Register the module using the STREAMABLE_HTTP transport
              app.install(
                  new McpModule(new CalculatorToolsMcp_())
                      .transport(McpModule.Transport.STREAMABLE_HTTP));
            })
        .ready(
            client -> {
              AtomicReference<String> sessionId = new AtomicReference<>();

              // 1. STREAMABLE_HTTP requires a formal MCP initialize handshake via POST
              // to generate the session ID.
              String initRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "init-1",
                    "method": "initialize",
                    "params": {
                      "protocolVersion": "2024-11-05",
                      "capabilities": {},
                      "clientInfo": { "name": "test-client", "version": "1.0.0" }
                    }
                  }
                  """;

              // The transport provider strictly requires these Accept headers
              client.header("Accept", "text/event-stream, application/json");
              client.header("Content-Type", "application/json");

              client.postJson(
                  "/mcp",
                  initRequest,
                  response -> {
                    assertEquals(200, response.code());

                    // 2. Extract the session ID from the headers
                    String header = response.header("mcp-session-id");
                    assertNotNull(
                        header, "mcp-session-id header must be present in initialization response");
                    sessionId.set(header);
                  });

              // 3. Construct the Tool request
              String toolRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "tool-1",
                    "method": "tools/call",
                    "params": {
                      "name": "get_session_info",
                      "arguments": {}
                    }
                  }
                  """;

              // 4. Send the tool request, appending the session ID we just obtained
              client.header("Accept", "text/event-stream, application/json");
              client.header("Content-Type", "application/json");
              client.header("mcp-session-id", sessionId.get());

              client.postJson(
                  "/mcp",
                  toolRequest,
                  response -> {
                    assertEquals(200, response.code());

                    var body = response.body().string();

                    assertThat(body)
                        .contains("\"id\":\"tool-1\"")
                        .as("The response should contain the calculated result");
                    ;
                  });
            });
  }
}
