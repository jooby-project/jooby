/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3863;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import io.jooby.Extension;
import io.jooby.avaje.jsonb.AvajeJsonbModule;
import io.jooby.jackson3.Jackson3Module;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.test.WebClient;
import io.jooby.trpc.TrpcModule;

public class TrpcProtocolTest {

  @ServerTest
  void shouldTalkTrpcUsingJackson3(ServerTestRunner runner) {
    shouldTalkTrpc(runner, new Jackson3Module());
  }

  @ServerTest
  void shouldTalkTrpcUsingAvajeJsonB(ServerTestRunner runner) {
    shouldTalkTrpc(runner, new AvajeJsonbModule());
  }

  void shouldTalkTrpc(ServerTestRunner runner, Extension jsonExtension) {
    runner
        .define(
            app -> {
              app.install(jsonExtension);
              app.install(new TrpcModule());
              app.mvc(new MovieService_());
            })
        .ready(this::assertProtocolData);
  }

  void assertProtocolData(WebClient http) {
    // --- 1. Basic & Multi-Argument Calls ---

    // Ping (No Arguments)
    http.get(
        "/trpc/movies.ping",
        rsp -> {
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(rsp.body().string())
              .isEqualToIgnoringNewLines("{\"result\":{\"data\":\"pong\"}}");
        });

    // GetById (Single Primitive Argument - Seamless)
    http.get(
        "/trpc/movies.getById?input=1",
        rsp -> {
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(rsp.body().string())
              .isEqualToIgnoringNewLines(
                  "{\"result\":{\"data\":{\"id\":1,\"title\":\"The Godfather\",\"year\":1972}}}");
        });

    // GetById (Not found)
    http.get(
        "/trpc/movies.getById?input=13",
        rsp -> {
          assertThat(rsp.code()).isEqualTo(404);
          assertThat(rsp.body().string())
              .isEqualToIgnoringNewLines(
                  """
                  {"error":{"message":"Movie not found: 13","code":-32004,"data":{"code":"NOT_FOUND","httpStatus":404,"path":"movies.getById"}}}
                  """);
        });

    // Search (Multi-Argument Tuple)
    http.get(
        "/trpc/movies.search?input=[\"Pulp Fiction\", 1994]",
        rsp -> {
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(rsp.body().string())
              .isEqualToIgnoringNewLines(
                  "{\"result\":{\"data\":[{\"id\":2,\"title\":\"Pulp Fiction\",\"year\":1994}]}}");
        });

    // AddReview (Multi-Argument Mutation)
    http.postJson(
        "/trpc/movies.addReview",
        "[\"The Godfather\", 5, \"Amazing\"]",
        rsp -> {
          var json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(JsonPath.<String>read(json, "$.result.data.status")).isEqualTo("published");
          assertThat(JsonPath.<Integer>read(json, "$.result.data.rating")).isEqualTo(5);
        });

    // --- 2. Seamless vs. Tuple Wrappers ---
    // Create (Strict Seamless Payload for single argument)
    http.postJson(
        "/trpc/movies.create",
        "{\"id\": 1, \"title\": \"The Matrix\", \"year\": 1999}",
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(JsonPath.<String>read(json, "$.result.data.title")).isEqualTo("The Matrix");
        });

    // BulkCreate (Single argument that is inherently an array)
    // Ensures the parser doesn't get confused by the leading '[' on a seamless payload
    http.get(
        "/trpc/movies.bulkCreate",
        Map.of("input", "[{\"id\": 1, \"title\": \"The Matrix\", \"year\": 1999}]"),
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(JsonPath.<String>read(json, "$.result.data[0]"))
              .isEqualTo("Created: The Matrix");
        });

    // --- 3. Reactive & Void Types ---

    // CreateMono (Reactive Pipeline)
    http.postJson(
        "/trpc/movies.createMono",
        "{\"id\": 1, \"title\": \"The Matrix\", \"year\": 1999}",
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(JsonPath.<Integer>read(json, "$.result.data.id")).isEqualTo(1);
        });

    // ResetIndex (Void return type)
    http.post(
        "/trpc/movies.resetIndex",
        rsp -> {
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(rsp.body().string()).contains("\"result\"");
        });

    // --- 4. Error Handling & Edge Cases ---

    // Error: Type Mismatch
    http.postJson(
        "/trpc/movies.addReview",
        "[\"The Godfather\", \"FIVE_STARS\", \"Amazing\"]",
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(400);
          assertThat(JsonPath.<String>read(json, "$.error.data.code")).isEqualTo("BAD_REQUEST");
          assertThat(JsonPath.<String>read(json, "$.error.message")).isNotEmpty();
        });

    // Error: Missing Tuple Wrapper (Multi-argument method given a raw string)
    http.get(
        "/trpc/movies.search?input=\"Pulp Fiction\"",
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(400);
          assertThat(JsonPath.<String>read(json, "$.error.message"))
              .contains("tRPC input for multiple arguments must be a JSON array (tuple)");
        });

    // Error: Procedure Not Found (404)
    http.get(
        "/trpc/movies.doesNotExist",
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(404);
          assertThat(JsonPath.<String>read(json, "$.error.data.code")).isEqualTo("NOT_FOUND");
        });

    // --- 5. Nullability Validation ---

    // Validating a nullable parameter is accepted (Integer)
    http.get(
        "/trpc/movies.search?input=[\"The Godfather\", null]",
        rsp -> {
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(rsp.body().string()).contains("\"The Godfather\"");
        });

    // Validating a required (primitive) parameter rejects null (Seamless path)
    http.get(
        "/trpc/movies.getById?input=null",
        rsp -> {
          String json = rsp.body().string();
          System.out.println(json);
          assertThat(rsp.code()).isEqualTo(400);
          assertThat(JsonPath.<String>read(json, "$.error.data.code")).isEqualTo("BAD_REQUEST");
        });

    // Validating MissingValueException (Not enough arguments in the tuple)
    http.postJson(
        "/trpc/movies.addReview",
        "[\"The Godfather\", 5]",
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(400);
          assertThat(JsonPath.<String>read(json, "$.error.message"))
              .containsIgnoringCase("comment");
        });

    // Validating explicit null on an Object/POJO
    http.postJson(
        "/trpc/movies.updateMetadata",
        "[1, null]",
        rsp -> {
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(rsp.body().string()).contains("\"result\"");
        });
  }
}
