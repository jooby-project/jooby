/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3863;

import static org.assertj.core.api.Assertions.assertThat;

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
    http.get(
        "/trpc/movies.ping",
        rsp -> {
          assertThat(rsp.body().string())
              .isEqualToIgnoringNewLines(
                  """
                  {"result":{"data":"pong"}}
                  """);
        });
    http.get(
        "/trpc/movies.getById?input=[1]",
        rsp -> {
          assertThat(rsp.body().string())
              .isEqualToIgnoringNewLines(
                  """
                  {"result":{"data":{"id":1,"title":"The Godfather","year":1972}}}
                  """);
        });
    // Test Multi-Arg Query (Pulp Fiction 1994)
    // input=["Pulp Fiction", 1994]
    http.get(
        "/trpc/movies.search?input=[\"Pulp Fiction\", 1994]",
        rsp -> {
          assertThat(rsp.body().string())
              .isEqualToIgnoringNewLines(
                  """
                  {"result":{"data":[{"id":2,"title":"Pulp Fiction","year":1994}]}}
                  """);
        });
    // Test Multi-Arg Mutation
    // Body: ["The Godfather", 5, "Amazing"]
    http.postJson(
        "/trpc/movies.addReview",
        "[\"The Godfather\", 5, \"Amazing\"]",
        rsp -> {
          var json = rsp.body().string();

          String status = JsonPath.read(json, "$.result.data.status");
          int rating = JsonPath.read(json, "$.result.data.rating");

          assertThat(status).isEqualTo("published");
          assertThat(rating).isEqualTo(5);
        });
    // 3. The tRPC payload (The Tuple)
    // Notice the outer `[` and `]` wrapping the actual JSON object.
    http.postJson(
        "/trpc/movies.create",
        "[{\"id\": 1, \"title\": \"The Matrix\", \"year\": 1999}]",
        rsp -> {
          String json = rsp.body().string();

          // 4. Validating the tRPC envelope and data using JsonPath + AssertJ
          assertThat(rsp.code()).isEqualTo(200);

          assertThat(JsonPath.<Integer>read(json, "$.result.data.id")).isEqualTo(1);
          assertThat(JsonPath.<String>read(json, "$.result.data.title")).isEqualTo("The Matrix");
          assertThat(JsonPath.<Integer>read(json, "$.result.data.year")).isEqualTo(1999);
        });

    // reactive
    http.postJson(
        "/trpc/movies.createMono",
        "[{\"id\": 1, \"title\": \"The Matrix\", \"year\": 1999}]",
        rsp -> {
          String json = rsp.body().string();

          // 4. Validating the tRPC envelope and data using JsonPath + AssertJ
          assertThat(rsp.code()).isEqualTo(200);

          assertThat(JsonPath.<Integer>read(json, "$.result.data.id")).isEqualTo(1);
          assertThat(JsonPath.<String>read(json, "$.result.data.title")).isEqualTo("The Matrix");
          assertThat(JsonPath.<Integer>read(json, "$.result.data.year")).isEqualTo(1999);
        });

    http.post(
        "/trpc/movies.resetIndex",
        rsp -> {
          assertThat(rsp.body().string())
              .isEqualToIgnoringNewLines(
                  """
                  {"result":{}}
                  """);
        });

    // 1. Test Error: Type Mismatch (Sending a String instead of an Integer for 'stars')
    http.postJson(
        "/trpc/movies.addReview",
        "[\"The Godfather\", \"FIVE_STARS\", \"Amazing\"]",
        rsp -> {
          String json = rsp.body().string();

          assertThat(rsp.code()).isEqualTo(400);

          // tRPC puts error details inside an "error" envelope, not "result"
          assertThat(JsonPath.<String>read(json, "$.error.data.code")).isEqualTo("BAD_REQUEST");
          assertThat(JsonPath.<Integer>read(json, "$.error.data.httpStatus")).isEqualTo(400);

          // The message should complain about the parsing failure
          String message = JsonPath.read(json, "$.error.message");
          assertThat(message).isNotEmpty();
        });

    // 2. Test Error: Missing Tuple Wrapper (Sending Object instead of Array tuple)
    http.postJson(
        "/trpc/movies.create",
        "{\"id\": 1, \"title\": \"The Matrix\", \"year\": 1999}",
        rsp -> {
          String json = rsp.body().string();

          assertThat(rsp.code()).isEqualTo(400);
          assertThat(JsonPath.<String>read(json, "$.error.data.code")).isEqualTo("BAD_REQUEST");

          assertThat(JsonPath.<String>read(json, "$.error.message"))
              .isEqualTo("tRPC body must be a JSON array (tuple)");

          // Ideally, the message explicitly states that an array was expected
          assertThat(JsonPath.<String>read(json, "$.error.message")).containsIgnoringCase("array");
        });

    // 3. Test Error: Procedure Not Found (404)
    http.get(
        "/trpc/movies.doesNotExist",
        rsp -> {
          String json = rsp.body().string();

          assertThat(rsp.code()).isEqualTo(404);
          assertThat(JsonPath.<String>read(json, "$.error.data.code")).isEqualTo("NOT_FOUND");
          assertThat(JsonPath.<Integer>read(json, "$.error.data.httpStatus")).isEqualTo(404);
        });

    // 1. Validating a nullable parameter is accepted when passing `null`
    // Assuming `movies.search` has a signature like: search(String title, Integer year)
    // where `year` is nullable. Sending: ["The Godfather", null]
    http.get(
        "/trpc/movies.search?input=[\"The Godfather\", null]",
        rsp -> {
          assertThat(rsp.code()).isEqualTo(200);
          assertThat(rsp.body().string())
              .isEqualToIgnoringNewLines(
                  """
                  {"result":{"data":[{"id":1,"title":"The Godfather","year":1972}]}}
                  """);
        });

    // 2. Validating a required (non-nullable) parameter rejects `null`
    // Assuming `movies.getById` has signature: getById(int id)
    // Sending: [null]
    http.get(
        "/trpc/movies.getById?input=[null]",
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(400);

          // Should trigger a parsing error because a primitive cannot be null
          assertThat(JsonPath.<String>read(json, "$.error.data.code")).isEqualTo("BAD_REQUEST");
          assertThat(JsonPath.<String>read(json, "$.error.message")).containsIgnoringCase("id");
        });

    // 3. Validating MissingValueException (Not enough arguments)
    // Assuming `movies.addReview` requires 3 arguments: addReview(String title, int stars, String
    // comment)
    // Sending only 2: ["The Godfather", 5]
    http.postJson(
        "/trpc/movies.addReview",
        "[\"The Godfather\", 5]",
        rsp -> {
          String json = rsp.body().string();
          assertThat(rsp.code()).isEqualTo(400);

          // Should trigger Jooby's MissingValueException because the array ended too early
          assertThat(JsonPath.<String>read(json, "$.error.data.code")).isEqualTo("BAD_REQUEST");
          assertThat(JsonPath.<String>read(json, "$.error.message"))
              .containsIgnoringCase("comment");
        });

    // 4. Validating explicit `null` on an Object/POJO (if allowed)
    // Assuming a method like: updateMetadata(int id, Metadata data) where `data` is nullable
    // Sending: [1, null]
    http.postJson(
        "/trpc/movies.updateMetadata",
        "[1, null]",
        rsp -> {
          String json = rsp.body().string();
          System.out.println(json);
          assertThat(rsp.code()).isEqualTo(200);
          // Validates the generator correctly used nextIsNull for the POJO decoder
          assertThat(json).contains("\"result\"");
        });
  }
}
