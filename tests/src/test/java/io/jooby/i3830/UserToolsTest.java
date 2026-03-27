/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3830;

import static org.assertj.core.api.Assertions.assertThat;

import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.mcp.McpModule;

public class UserToolsTest {

  @ServerTest
  public void shouldReturnStructuredJsonObject(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new Jackson3Module());
              // Register the tool using the stateless transport
              app.install(
                  new McpModule(new UserToolsMcp_())
                      .transport(McpModule.Transport.STATELESS_STREAMABLE_HTTP));
            })
        .ready(
            client -> {
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
                    assertThat(response.code()).isEqualTo(200);

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
            });
  }
}
