/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3830;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;

import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.SneakyThrows;
import io.jooby.jackson.JacksonModule;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.mcp.McpModule;
import io.jooby.mcp.jackson2.McpJackson2Module;
import io.jooby.mcp.jackson3.McpJackson3Module;
import io.jooby.test.WebClient;

public class UserToolsTest {

  private void setupMcpApp(Jooby app, Extension... extensions) {
    for (var extension : extensions) {
      app.install(extension);
    }
    app.install(
        new McpModule(new UserToolsMcp_())
            .transport(McpModule.Transport.STATELESS_STREAMABLE_HTTP));
  }

  @ServerTest
  public void shouldReturnStructuredJsonObjectOnJackson2(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, new JacksonModule(), new McpJackson2Module()))
        .ready(assertStructuredJson());
  }

  @ServerTest
  public void shouldReturnStructuredJsonObjectOnJackson3(ServerTestRunner runner) {
    runner
        .define(app -> setupMcpApp(app, new Jackson3Module(), new McpJackson3Module()))
        .ready(assertStructuredJson());
  }

  private static SneakyThrows.Consumer<WebClient> assertStructuredJson() {
    return client -> {
      // 1. Ask for the structured user profile
      String jsonRpcRequest =
          """
          {
            "jsonrpc": "2.0",
            "id": "req-user-1",
            "method": "tools/call",
            "params": {
              "name": "get_user_profile",
              "arguments": {
                "username": "edgar"
              }
            }
          }
          """;

      client.header("Accept", "text/event-stream, application/json");
      client.header("Content-Type", "application/json");

      client.postJson(
          "/mcp",
          jsonRpcRequest,
          response -> {
            Assertions.assertThat(response.code()).isEqualTo(200);

            String body = response.body().string();

            // 2. Verify the response ID matches
            assertThat(body).containsPattern("\"id\":\\s*\"req-user-1\"");

            // 3. Verify the Java Record was correctly serialized into the tool's text
            // content!
            assertThat(body)
                .contains("\"username\":\"edgar\"")
                .contains("\"role\":\"admin\"")
                .contains("\"active\":true")
                .as(
                    "The output should be a fully structured JSON payload representing the"
                        + " Record");
          });
    };
  }
}
