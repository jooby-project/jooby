/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3868;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import io.jooby.Jooby;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;

public abstract class AbstractJsonRpcProtocolTest {

  /**
   * Subclasses must provide the specific JSON engine module (e.g., JacksonModule, Jackson3Module).
   */
  protected abstract void installJsonEngine(Jooby app);

  // Helper to keep test setup DRY
  private void setupApp(Jooby app) {
    installJsonEngine(app);
    app.mvc(new MovieServiceRpc_());
    app.mvc(new MovieServiceRpcRpc_());
  }

  @ServerTest
  void shouldHandleSingleRequests(ServerTestRunner runner) {
    runner
        .define(this::setupApp)
        .ready(
            http -> {
              // GetById (Positional Arguments Array)
              http.postJson(
                  "/rpc",
                  """
                  {
                    "jsonrpc": "2.0",
                    "method": "movies.getById",
                    "params": [1],
                    "id": 1
                  }
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    // Spec: Success must have 'result' and must NOT have 'error'
                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("result").doesNotContainKey("error");

                    assertThat(JsonPath.<Integer>read(json, "$.id")).isEqualTo(1);
                    assertThat(JsonPath.<String>read(json, "$.result.title"))
                        .isEqualTo("The Godfather");
                  });

              // Search (Named Arguments Object)
              http.postJson(
                  "/rpc",
                  """
                  {
                    "jsonrpc": "2.0",
                    "method": "movies.search",
                    "params": {"title": "Pulp Fiction", "year": 1994},
                    "id": 2
                  }
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("result").doesNotContainKey("error");

                    assertThat(JsonPath.<Integer>read(json, "$.id")).isEqualTo(2);
                    assertThat(JsonPath.<Integer>read(json, "$.result[0].id")).isEqualTo(2);
                    assertThat(JsonPath.<String>read(json, "$.result[0].title"))
                        .isEqualTo("Pulp Fiction");
                  });

              // Create (Complex Object passed by position)
              http.postJson(
                  "/rpc",
                  """
                  {
                    "jsonrpc": "2.0",
                    "method": "movies.create",
                    "params": [{"id": 3, "title": "Goodfellas", "year": 1990}],
                    "id": 3
                  }
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("result").doesNotContainKey("error");

                    assertThat(JsonPath.<String>read(json, "$.result.title"))
                        .isEqualTo("Goodfellas");
                  });
            });
  }

  @ServerTest
  void shouldHandleNotifications(ServerTestRunner runner) {
    runner
        .define(this::setupApp)
        .ready(
            http -> {
              http.postJson(
                  "/rpc",
                  """
                  {
                    "jsonrpc": "2.0",
                    "method": "movies.deleteMovie",
                    "params": [1]
                  }
                  """,
                  rsp -> {
                    assertThat(rsp.code()).isEqualTo(204); // No Content
                    assertThat(rsp.body().string()).isEmpty();
                  });
            });
  }

  @ServerTest
  void shouldHandleErrors(ServerTestRunner runner) {
    runner
        .define(this::setupApp)
        .ready(
            http -> {
              // Method Not Found (-32601)
              http.postJson(
                  "/rpc",
                  """
                  {
                    "jsonrpc": "2.0",
                    "method": "movies.unknownMethod",
                    "params": [],
                    "id": 4
                  }
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    // Spec: Error must have 'error' and must NOT have 'result'
                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("error").doesNotContainKey("result");

                    assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32601);
                    assertThat(JsonPath.<String>read(json, "$.error.message"))
                        .contains("Method not found");
                  });

              // Invalid Request (-32600)
              http.postJson(
                  "/rpc",
                  """
                  {
                    "method": "movies.getById",
                    "params": [1],
                    "id": 5
                  }
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("error").doesNotContainKey("result");

                    assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32600);
                  });

              // Parse Error (-32700)
              http.postJson(
                  "/rpc",
                  "[{ malformed json... ",
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("error").doesNotContainKey("result");

                    assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32700);
                  });

              // Application Exception Bubbling (-32004)
              http.postJson(
                  "/rpc",
                  """
                  {
                    "jsonrpc": "2.0",
                    "method": "movies.getById",
                    "params": [99],
                    "id": 6
                  }
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("error").doesNotContainKey("result");

                    assertThat(JsonPath.<String>read(json, "$.error.message"))
                        .contains("Not found");
                    assertThat(JsonPath.<String>read(json, "$.error.data"))
                        .contains("Movie not found: 99");
                  });
            });
  }

  @ServerTest
  void shouldHandleInvalidParams(ServerTestRunner runner) {
    runner
        .define(this::setupApp)
        .ready(
            http -> {
              // 1. Missing Argument (Named)
              http.postJson(
                  "/rpc",
                  """
                  {"jsonrpc": "2.0", "method": "movies.search", "params": {"title": "Pulp Fiction"}, "id": 10}
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("error").doesNotContainKey("result");

                    assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32602);
                  });

              // 2. Missing Argument (Positional)
              http.postJson(
                  "/rpc",
                  """
                  {"jsonrpc": "2.0", "method": "movies.search", "params": ["Pulp Fiction"], "id": 11}
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("error").doesNotContainKey("result");

                    assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32602);
                  });

              // 3. Type Mismatch
              http.postJson(
                  "/rpc",
                  """
                  {"jsonrpc": "2.0", "method": "movies.search", "params": {"title": "Pulp Fiction", "year": "ninety-four"}, "id": 12}
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("error").doesNotContainKey("result");

                    assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32602);
                  });

              // 4. Empty Params (Array)
              http.postJson(
                  "/rpc",
                  """
                  {"jsonrpc": "2.0", "method": "movies.getById", "params": [], "id": 13}
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("error").doesNotContainKey("result");

                    assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32602);
                  });

              // 5. Omitted Params Object entirely
              http.postJson(
                  "/rpc",
                  """
                  {"jsonrpc": "2.0", "method": "movies.getById", "id": 14}
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("error").doesNotContainKey("result");

                    assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32602);
                  });
            });
  }

  @ServerTest
  void shouldHandleBatchProcessing(ServerTestRunner runner) {
    runner
        .define(this::setupApp)
        .ready(
            http -> {
              http.postJson(
                  "/rpc",
                  """
                  [
                    {"jsonrpc": "2.0", "method": "movies.getById", "params": [1], "id": "req-1"},
                    {"jsonrpc": "2.0", "method": "movies.getById", "params": [2], "id": "req-2"},
                    {"jsonrpc": "2.0", "method": "movies.deleteMovie", "params": [3]}
                  ]
                  """,
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);
                    assertThat(json).startsWith("[");

                    // Assert length is exactly 2 (notification should be dropped)
                    assertThat(JsonPath.<Integer>read(json, "$.length()")).isEqualTo(2);

                    // Assert mutual exclusivity on the individual batch elements
                    Map<String, Object> element0 = JsonPath.read(json, "$[0]");
                    Map<String, Object> element1 = JsonPath.read(json, "$[1]");

                    // Both should be successes
                    assertThat(element0).containsKey("result").doesNotContainKey("error");
                    assertThat(element1).containsKey("result").doesNotContainKey("error");

                    // Order-agnostic assertions using JsonPath filters!
                    List<String> req1Title =
                        JsonPath.read(json, "$[?(@.id == 'req-1')].result.title");
                    assertThat(req1Title).containsExactly("The Godfather");

                    List<String> req2Id = JsonPath.read(json, "$[?(@.id == 'req-2')].id");
                    assertThat(req2Id).containsExactly("req-2");

                    // Notification (deleteMovie) should leave no trace in the response
                    assertThat(json).doesNotContain("deleteMovie");
                  });
            });
  }

  @ServerTest
  void shouldHandleBatchEdgeCases(ServerTestRunner runner) {
    runner
        .define(this::setupApp)
        .ready(
            http -> {
              // Edge Case 1: Empty Array
              http.postJson(
                  "/rpc",
                  "[]",
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);
                    assertThat(json).doesNotStartWith("["); // MUST be a single object

                    Map<String, Object> root = JsonPath.read(json, "$");
                    assertThat(root).containsKey("error").doesNotContainKey("result");

                    assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32600);
                    assertThat(JsonPath.<String>read(json, "$.error.message"))
                        .containsIgnoringCase("Invalid Request");
                  });

              // Edge Case 2: Array containing completely invalid data
              http.postJson(
                  "/rpc",
                  "[1, 2, 3]",
                  rsp -> {
                    String json = rsp.body().string();
                    assertThat(rsp.code()).isEqualTo(200);
                    assertThat(json).startsWith("[");
                    assertThat(JsonPath.<Integer>read(json, "$.length()")).isEqualTo(3);

                    // Check mutual exclusivity on all error elements in the batch
                    List<Map<String, Object>> array = JsonPath.read(json, "$");
                    for (Map<String, Object> element : array) {
                      assertThat(element).containsKey("error").doesNotContainKey("result");
                    }

                    // All three elements should be Invalid Request errors
                    List<Integer> errorCodes = JsonPath.read(json, "$[*].error.code");
                    assertThat(errorCodes).containsExactly(-32600, -32600, -32600);
                  });
            });
  }
}
