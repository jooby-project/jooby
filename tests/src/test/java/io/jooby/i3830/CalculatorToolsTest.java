/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3830;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jooby.Jooby;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.mcp.McpModule;

public class CalculatorToolsTest {

  private void setupMcpApp(Jooby app) {
    app.install(new Jackson3Module());
    app.install(
        new McpModule(new CalculatorToolsMcp_())
            .transport(McpModule.Transport.STATELESS_STREAMABLE_HTTP));
  }

  @ServerTest
  public void shouldCallAddNumbersTool(ServerTestRunner runner) {
    runner
        .define(this::setupMcpApp)
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
  public void shouldGetMathTutorPrompt(ServerTestRunner runner) {
    runner
        .define(this::setupMcpApp)
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
  public void shouldReadStaticResource(ServerTestRunner runner) {
    runner
        .define(this::setupMcpApp)
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
        .define(this::setupMcpApp)
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
        .define(this::setupMcpApp)
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
