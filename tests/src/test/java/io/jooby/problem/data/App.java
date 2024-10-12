/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem.data;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.Jooby;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.jooby.exception.InvalidCsrfToken;
import io.jooby.exception.StatusCodeException;
import io.jooby.handler.AccessLogHandler;
import io.jooby.jackson.JacksonModule;
import io.jooby.problem.HttpProblem;

import java.net.URI;
import java.util.List;
import java.util.Map;

public class App extends Jooby {

  {
    Config problemDetailsConfig = ConfigFactory.parseMap(
        Map.of("problem.details.enabled", true,
            "problem.details.log4xxErrors", true,
            "problem.details.muteCodes", List.of(405, 406),
            "problem.details.muteTypes", List.of("io.jooby.exception.UnauthorizedException"))
    );
    getEnvironment()
        .setConfig(problemDetailsConfig.withFallback(getConfig()));

    use(new AccessLogHandler());
    install(new JacksonModule());

    get("/throw-simple-http-problem", ctx -> {
      throw HttpProblem.valueOf(StatusCode.UNPROCESSABLE_ENTITY, "Id may not be null");
    });

    get("/throw-http-problem-with-details", ctx -> {
      throw HttpProblem.valueOf(StatusCode.UNPROCESSABLE_ENTITY,
          "Invalid input parameter",
          "'Username' field may not be empty");
    });

    get("/throw-problem-with-empty-builder", ctx -> {
      throw HttpProblem.builder().build();
    });

    get("/throw-problem-with-builder-status-only", ctx -> {
      throw HttpProblem.builder()
          .status(StatusCode.UNPROCESSABLE_ENTITY)
          .build();
    });

    get("/throw-problem-with-builder-title-only", ctx -> {
      throw HttpProblem.builder()
          .title("Only Title")
          .build();
    });

    get("/throw-problem-with-builder-all-parameters", ctx -> {
      throw HttpProblem.builder()
          .type(URI.create("http://example.com/invalid-params"))
          .title("Invalid input parameters")
          .status(StatusCode.UNPROCESSABLE_ENTITY)
          .detail("'Name' may not be empty")
          .instance(URI.create("http://example.com/invalid-params/3325"))
          .header("x-string-header", "string")
          .header("x-int-header", 12_000)
          .header("x-object-header", Map.of("test", 45))
          .param("key1", "value1")
          .param("key2", List.of(1, 2, 3, 4, 5))
          .param("key3", Map.of("m1", List.of(), "m2", Map.of()))
          .error(new CustomError("Error 1", "#/firstName", "FIELD"))
          .errors(List.of(new CustomError("Error 2", "#/lastName", "FIELD")))
          .build();
    });

    get("/throw-status-code-exception", ctx -> {
      throw new StatusCodeException(StatusCode.BAD_REQUEST);
    });

    get("/throw-status-code-exception-with-message", ctx -> {
      throw new StatusCodeException(StatusCode.BAD_REQUEST, "Unable to parse request");
    });

    get("/throw-number-format-exception", ctx -> {
      throw new NumberFormatException();
    });

    get("/throw-number-format-exception-with-message", ctx -> {
      throw new NumberFormatException("Number should be positive");
    });

    get("/throw-illegal-state-exception", ctx -> {
      throw new IllegalStateException();
    });

    get("/throw-invalid-csrf-token", ctx -> {
      throw new InvalidCsrfToken("lahfuqwefkaslkdfbawiebfsdb=f-dg=-gadfg");
    });

    post("/not-allowed", ctx -> ctx.send("Hello"));

    get("/throw-missing-value-exception", ctx -> {
      ctx.getRouter().attribute("non-existed"); // throws MissingValueException
      return ctx.send("Hello");
    });

    get("/throw-not-acceptable-exception", ctx -> {
      return ctx.render(Map.of("say", "hi"));
    });

    get("/throw-unsupported-media-type", ctx -> ctx.send("hello"))
        .consumes(MediaType.xml);

    get("/throw-my-custom-exception-with-custom-render", ctx -> {
      throw new MyCustomException("Demo MyCustomException");
    });

    get("/throw-my-custom-exception-and-propagate-as-problem", ctx -> {
      throw new MyCustomExceptionToPropagate("Should be propagated");
    });

    get("/throw-inherited-out-of-stock-problem", ctx -> {
      throw new OutOfStockProblem("Coffee Grinder MX-15");
    });

    post("/post-empty-body", ctx -> ctx.body(Object.class));

    errorCode(MismatchedInputException.class, StatusCode.UNPROCESSABLE_ENTITY);

    error(MyCustomException.class, (ctx, cause, code) -> {
      ctx.setResponseType(MediaType.json)
          .setResponseCode(418)
          .render(Map.of("message", "I'm a teapot"));
    });

    error(MyCustomExceptionToPropagate.class, (ctx, cause, code) -> {
      MyCustomExceptionToPropagate ex = (MyCustomExceptionToPropagate) cause;
      var p = HttpProblem.valueOf(StatusCode.BAD_REQUEST, ex.getMessage());
      ctx.getRouter().getErrorHandler().apply(ctx, p, code);
    });
  }

  public static void main(final String[] args) {
    runApp(args, App::new);
  }
}
