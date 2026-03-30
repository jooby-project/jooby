/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3830;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.jooby.Jooby;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.mcp.McpModule;
import io.jooby.mcp.jackson3.McpJackson3Module;

public class CalculatorToolsTest {

  private void setupMcpApp(Jooby app, McpModule.Transport transport) {
    app.install(new Jackson3Module());
    app.install(new McpJackson3Module());
    app.install(new McpModule(new CalculatorToolsMcp_()).transport(transport));
  }

  @ServerTest
  public void shouldCallToolOverStreamableHttp(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.STREAMABLE_HTTP))
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
                      "name": "add_numbers",
                      "arguments": { "a": 5, "b": 10 }
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
                        .contains("15")
                        .as("The response should contain the calculated result");
                    ;
                  });
            });
  }

  @ServerTest
  public void shouldCallToolOverSse(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.SSE))
        .ready(
            client -> {
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

              String initializedNotification =
                  """
                  {
                    "jsonrpc": "2.0",
                    "method": "notifications/initialized"
                  }
                  """;

              String toolRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "tool-1",
                    "method": "tools/call",
                    "params": {
                      "name": "add_numbers",
                      "arguments": { "a": 5, "b": 10 }
                    }
                  }
                  """;

              client
                  .sse("/mcp/sse")
                  .next(
                      event -> {
                        assertThat(event.getEvent()).isEqualTo("endpoint");

                        // 1. Get the endpoint and trim any sneaky SSE newlines
                        String endpoint = event.getData().toString().trim();
                        String absoluteUri =
                            endpoint.startsWith("http")
                                ? endpoint
                                : "http://localhost:" + runner.getAllocatedPort() + endpoint;

                        // 2. Fire the POSTs in a background thread to prevent blocking the SSE
                        // listener
                        Thread.startVirtualThread(
                            () -> {
                              try (var httpClient = HttpClient.newHttpClient()) {
                                // Step A: Send Initialize
                                HttpRequest req1 =
                                    HttpRequest.newBuilder(URI.create(absoluteUri))
                                        .header("Content-Type", "application/json")
                                        .POST(HttpRequest.BodyPublishers.ofString(initRequest))
                                        .build();
                                httpClient.send(req1, HttpResponse.BodyHandlers.discarding());

                                // Step B: Send Initialized Notification (Required by spec)
                                HttpRequest req2 =
                                    HttpRequest.newBuilder(URI.create(absoluteUri))
                                        .header("Content-Type", "application/json")
                                        .POST(
                                            HttpRequest.BodyPublishers.ofString(
                                                initializedNotification))
                                        .build();
                                httpClient.send(req2, HttpResponse.BodyHandlers.discarding());

                                // Step C: Finally, call the tool!
                                HttpRequest req3 =
                                    HttpRequest.newBuilder(URI.create(absoluteUri))
                                        .header("Content-Type", "application/json")
                                        .POST(HttpRequest.BodyPublishers.ofString(toolRequest))
                                        .build();
                                httpClient.send(req3, HttpResponse.BodyHandlers.discarding());

                              } catch (Exception e) {
                                e.printStackTrace();
                              }
                            });
                      })
                  .next(
                      event -> {
                        // 3. The first message back is the result of our 'initialize' handshake
                        assertThat(event.getEvent()).isEqualTo("message");
                        assertThat(event.getData().toString()).contains("\"id\":\"init-1\"");
                      })
                  .next(
                      event -> {
                        // 4. The second message back is the actual tool execution result
                        assertThat(event.getEvent()).isEqualTo("message");
                        assertThat(event.getData().toString())
                            .containsPattern("\"id\":\\s*\"tool-1\"")
                            .contains("15");
                      })
                  .verify();
            });
  }

  @ServerTest
  public void shouldCallAddNumbersTool(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.STATELESS_STREAMABLE_HTTP))
        .ready(
            client -> {
              String jsonRpcRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "req-tool-1",
                    "method": "tools/call",
                    "params": {
                      "name": "add_numbers",
                      "arguments": { "a": 5, "b": 10 }
                    }
                  }
                  """;

              client.header("Content-Type", "application/json");
              client.postJson(
                  "/mcp",
                  jsonRpcRequest,
                  response -> {
                    assertEquals(200, response.code());
                    String body = response.body().string();
                    assertTrue(
                        body.contains("\"id\":\"req-tool-1\"")
                            || body.contains("\"id\": \"req-tool-1\""));
                    assertTrue(body.contains("15"), "Tool execution should return 15");
                  });
            });
  }

  @ServerTest
  public void shouldCallToolOverWebSocket(ServerTestRunner runner) throws Exception {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.WEBSOCKET))
        .ready(
            client -> {
              CountDownLatch initLatch = new CountDownLatch(1);
              CountDownLatch toolLatch = new CountDownLatch(1);

              AtomicReference<String> initResponse = new AtomicReference<>();
              AtomicReference<String> toolResponse = new AtomicReference<>();

              // 1. Connect to the Jooby WS endpoint using Java's native HttpClient
              String wsUri = "ws://localhost:" + runner.getAllocatedPort() + "/mcp";
              HttpClient httpClient = HttpClient.newHttpClient();

              WebSocket webSocket =
                  httpClient
                      .newWebSocketBuilder()
                      .buildAsync(
                          URI.create(wsUri),
                          new WebSocket.Listener() {
                            StringBuilder textBuilder = new StringBuilder();

                            @Override
                            public CompletionStage<?> onText(
                                WebSocket ws, CharSequence data, boolean last) {
                              textBuilder.append(data);
                              if (last) {
                                String message = textBuilder.toString();
                                textBuilder.setLength(0); // reset buffer

                                // Route the incoming messages to the correct assertions
                                if (message.contains("\"id\":\"init-1\"")
                                    || message.contains("\"id\": \"init-1\"")) {
                                  initResponse.set(message);
                                  initLatch.countDown();
                                } else if (message.contains("\"id\":\"tool-1\"")
                                    || message.contains("\"id\": \"tool-1\"")) {
                                  toolResponse.set(message);
                                  toolLatch.countDown();
                                }
                              }
                              return WebSocket.Listener.super.onText(ws, data, last);
                            }
                          })
                      .join();

              // 2. Send the Initialize Request
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
              webSocket.sendText(initRequest, true).join();

              // Wait up to 2 seconds for the server to reply to the initialization
              assertThat(initLatch.await(2, TimeUnit.SECONDS)).isTrue();
              assertThat(initResponse.get()).contains("\"id\":\"init-1\"");

              // 3. Send the Initialized Notification
              // (This is fire-and-forget! The server sends nothing back for notifications)
              String initializedNotification =
                  """
                  {
                    "jsonrpc": "2.0",
                    "method": "notifications/initialized"
                  }
                  """;
              webSocket.sendText(initializedNotification, true).join();

              // 4. Send the Tool Execution Request
              String toolRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "tool-1",
                    "method": "tools/call",
                    "params": {
                      "name": "add_numbers",
                      "arguments": { "a": 5, "b": 10 }
                    }
                  }
                  """;
              webSocket.sendText(toolRequest, true).join();

              // Wait up to 2 seconds for the tool result
              assertThat(toolLatch.await(2, TimeUnit.SECONDS)).isTrue();

              // 5. Verify the tool successfully executed over the socket!
              assertThat(toolResponse.get())
                  .containsPattern("\"id\":\\s*\"tool-1\"")
                  .contains("15")
                  .as("The response should contain the calculated result: 15");

              // Clean up the connection
              webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Test complete").join();
            });
  }

  @ServerTest
  public void shouldGetMathTutorPrompt(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.STATELESS_STREAMABLE_HTTP))
        .ready(
            client -> {
              String jsonRpcRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "req-prompt-1",
                    "method": "prompts/get",
                    "params": {
                      "name": "math_tutor",
                      "arguments": { "topic": "Algebra" }
                    }
                  }
                  """;

              client.header("Content-Type", "application/json");
              client.postJson(
                  "/mcp",
                  jsonRpcRequest,
                  response -> {
                    assertEquals(200, response.code());
                    String body = response.body().string();
                    assertTrue(
                        body.contains("\"id\":\"req-prompt-1\"")
                            || body.contains("\"id\": \"req-prompt-1\""));
                    assertTrue(
                        body.contains("explain the concept of Algebra"),
                        "Prompt should contain the formatted topic");
                  });
            });
  }

  @ServerTest
  public void shouldGetUnknownTool(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.STATELESS_STREAMABLE_HTTP))
        .ready(
            client -> {
              String jsonRpcRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "req-tool-1",
                    "method": "tools/call",
                    "params": {
                      "name": "some_tool",
                      "arguments": { "topic": "Algebra" }
                    }
                  }
                  """;

              client.header("Content-Type", "application/json");
              client.postJson(
                  "/mcp",
                  jsonRpcRequest,
                  response -> {
                    assertEquals(200, response.code());
                    String body = response.body().string();
                    assertThat(body)
                        .isEqualToNormalizingWhitespace(
                            """
                            {"jsonrpc":"2.0","id":"req-tool-1","error":{"code":-32602,"message":"Unknown tool: invalid_tool_name","data":"Tool not found: some_tool"}}
                            """);
                  });
            });
  }

  @ServerTest
  public void shouldGetInvalidParams(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.STATELESS_STREAMABLE_HTTP))
        .ready(
            client -> {
              String jsonRpcRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "req-tool-1",
                    "method": "tools/call",
                    "params": {
                      "name": "add_numbers",
                      "arguments": { "a": 5, "b": "10" }
                    }
                  }
                  """;

              client.header("Content-Type", "application/json");
              client.postJson(
                  "/mcp",
                  jsonRpcRequest,
                  response -> {
                    assertEquals(200, response.code());
                    String body = response.body().string();
                    assertThat(body)
                        .containsIgnoringWhitespaces(
                            """
                            "result":
                            """)
                        .containsIgnoringWhitespaces("\"isError\":true");
                  });
            });
  }

  @ServerTest
  public void shouldReadStaticResource(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.STATELESS_STREAMABLE_HTTP))
        .ready(
            client -> {
              String jsonRpcRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "req-res-1",
                    "method": "resources/read",
                    "params": {
                      "uri": "calculator://manual/usage"
                    }
                  }
                  """;

              client.header("Content-Type", "application/json");
              client.postJson(
                  "/mcp",
                  jsonRpcRequest,
                  response -> {
                    assertEquals(200, response.code());
                    String body = response.body().string();
                    assertTrue(
                        body.contains("\"id\":\"req-res-1\"")
                            || body.contains("\"id\": \"req-res-1\""));
                    assertTrue(
                        body.contains("Calculator supports basic arithmetic"),
                        "Resource should return the manual text");
                  });
            });
  }

  @ServerTest
  public void shouldReadResourceTemplate(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.STATELESS_STREAMABLE_HTTP))
        .ready(
            client -> {
              String jsonRpcRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "req-res-2",
                    "method": "resources/read",
                    "params": {
                      "uri": "calculator://history/alice"
                    }
                  }
                  """;

              client.header("Content-Type", "application/json");
              client.postJson(
                  "/mcp",
                  jsonRpcRequest,
                  response -> {
                    assertEquals(200, response.code());
                    String body = response.body().string();
                    assertTrue(
                        body.contains("\"id\":\"req-res-2\"")
                            || body.contains("\"id\": \"req-res-2\""));
                    assertTrue(
                        body.contains("History for alice"),
                        "Resource template should correctly extract the 'alice' path variable");
                  });
            });
  }

  @ServerTest
  public void shouldGetHistoryCompletion(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, McpModule.Transport.STATELESS_STREAMABLE_HTTP))
        .ready(
            client -> {
              String jsonRpcRequest =
                  """
                  {
                    "jsonrpc": "2.0",
                    "id": "req-comp-1",
                    "method": "completion/complete",
                    "params": {
                      "ref": {
                        "type": "ref/resource",
                        "uri": "calculator://history/{user}"
                      },
                      "argument": {
                        "name": "user",
                        "value": "al"
                      }
                    }
                  }
                  """;

              client.header("Content-Type", "application/json");
              client.postJson(
                  "/mcp",
                  jsonRpcRequest,
                  response -> {
                    assertEquals(200, response.code());
                    String body = response.body().string();
                    assertTrue(
                        body.contains("\"id\":\"req-comp-1\"")
                            || body.contains("\"id\": \"req-comp-1\""));
                    assertTrue(
                        body.contains("alice"),
                        "Completion should return 'alice' in the values array");
                    assertTrue(
                        body.contains("bob"), "Completion should return 'bob' in the values array");
                  });
            });
  }
}
