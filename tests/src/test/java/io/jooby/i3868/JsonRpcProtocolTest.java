/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3868;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.test.WebClient;

public class JsonRpcProtocolTest {

  @ServerTest
  void shouldTalkJsonRpcUsingJackson3(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              // 1. Install JSON engine
              app.install(new Jackson3Module());

              // REST/tRPC
              app.mvc(new MovieService_());
              // JSON-RPC
              app.mvc(new MovieServiceRpc_());
            })
        .ready(this::assertProtocolData);
  }

  void assertProtocolData(WebClient http) {
    // --- 1. Basic Single Requests (Positional & Named) ---

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
          assertThat(JsonPath.<Integer>read(json, "$.id")).isEqualTo(1);
          assertThat(JsonPath.<String>read(json, "$.result.title")).isEqualTo("The Godfather");
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
          assertThat(JsonPath.<Integer>read(json, "$.id")).isEqualTo(2);
          assertThat(JsonPath.<Integer>read(json, "$.result[0].id")).isEqualTo(2);
          assertThat(JsonPath.<String>read(json, "$.result[0].title")).isEqualTo("Pulp Fiction");
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
          assertThat(JsonPath.<String>read(json, "$.result.title")).isEqualTo("Goodfellas");
        });

    // --- 2. Notifications (No 'id' -> No Content) ---

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
          // JSON-RPC spec dictates notifications must not receive a response object
          assertThat(rsp.code()).isEqualTo(204); // No Content
          assertThat(rsp.body().string()).isEmpty();
        });

    // --- 3. Error Handling & Spec Compliance ---

    // Error: Method Not Found (-32601)
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
          assertThat(rsp.code()).isEqualTo(200); // Errors are still 200 OK in JSON-RPC envelope
          assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32601);
          assertThat(JsonPath.<String>read(json, "$.error.message")).contains("Method not found");
        });

    // Error: Invalid Request (Missing jsonrpc version) (-32600)
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
          assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32600);
        });

    // Error: Parse Error (Malformed JSON) (-32700)
    http.postJson(
        "/rpc",
        "[{ malformed json... ",
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(JsonPath.<Integer>read(json, "$.error.code")).isEqualTo(-32700);
        });

    // Application Exception Bubbling
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
          // Assuming your exception mapper catches NotFoundException and sets a specific code,
          // or falls back to internal error (-32603)
          assertThat(JsonPath.<String>read(json, "$.error.message"))
              .contains("Movie not found: 99");
        });

    // --- 4. Batch Processing ---

    http.postJson(
        "/rpc",
        """
        [
          {
            "jsonrpc": "2.0",
            "method": "movies.getById",
            "params": [1],
            "id": "req-1"
          },
          {
            "jsonrpc": "2.0",
            "method": "movies.getById",
            "params": [2],
            "id": "req-2"
          },
          {
            "jsonrpc": "2.0",
            "method": "movies.deleteMovie",
            "params": [3]
          }
        ]
        """,
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(200);

          // Verify it returned a JSON array
          assertThat(json).startsWith("[");

          // Verify standard requests were fulfilled
          assertThat(JsonPath.<String>read(json, "$[0].id")).isEqualTo("req-1");
          assertThat(JsonPath.<String>read(json, "$[0].result.title")).isEqualTo("The Godfather");
          assertThat(JsonPath.<String>read(json, "$[1].id")).isEqualTo("req-2");

          // Verify notification (deleteMovie) produced NO response element in the array
          assertThat(json).doesNotContain("deleteMovie");
          // Size should be exactly 2, not 3.
          assertThat(JsonPath.<Integer>read(json, "$.length()")).isEqualTo(2);
        });
  }
}
