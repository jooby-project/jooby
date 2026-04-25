/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3830;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import io.jooby.Jooby;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.mcp.McpChain;
import io.jooby.mcp.McpInvoker;
import io.jooby.mcp.McpModule;
import io.jooby.mcp.McpOperation;
import io.jooby.mcp.jackson3.McpJackson3Module;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;

public class McpArgumentModifierTest {

  private static final UUID uuid = UUID.randomUUID();

  private void setupMcpApp(Jooby app, McpModule.Transport transport) {
    app.install(new Jackson3Module());
    app.install(new McpJackson3Module());
    app.install(
        new McpModule(new ArgumentModifierMcp_())
            .invoker(
                new McpInvoker() {
                  @Override
                  public <R> R invoke(
                      @Nullable McpSyncServerExchange exchange,
                      @NonNull McpTransportContext transportContext,
                      @NonNull McpOperation operation,
                      @NonNull McpChain next)
                      throws Exception {
                    operation.setArgument("user", new CustomArg(uuid.toString()));
                    return next.proceed(exchange, transportContext, operation);
                  }
                })
            .transport(transport));
  }

  @ServerTest
  public void shouldIntroduceArguments(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.STREAMABLE_HTTP))
        .ready(
            client -> {
              AtomicReference<String> sessionId = new AtomicReference<>();

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
                      "name": "customArgument",
                      "arguments": { }
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

                    assertThat(body).contains("\"text\":\"%s\"".formatted(uuid.toString()));
                  });
            });
  }
}
