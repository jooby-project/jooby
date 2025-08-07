/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem;

import static io.jooby.MediaType.*;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.List;
import java.util.Map;

import org.hamcrest.Matchers;

import io.jooby.StatusCode;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.problem.data.App;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.http.Header;
import io.restassured.specification.RequestSpecification;

class ProblemDetailsHandlerTest {

  protected static RequestSpecification GENERIC_SPEC =
      new RequestSpecBuilder().setContentType(ContentType.JSON).build();

  private static final String INTERNAL_ERROR_MSG =
      "The server encountered an error or misconfiguration and was unable "
          + "to complete your request";

  static {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  private static final String CONTENT_TYPE = "Content-Type";

  @ServerTest
  void titleAndStatusOnly_shouldProduceCorrectProblemResponse(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/throw-simple-http-problem")
                  .then()
                  .assertThat()
                  .statusCode(422)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("timestamp", is(notNullValue()))
                  .body("type", equalTo("about:blank"))
                  .body("title", equalTo("Id may not be null"))
                  .body("status", equalTo(422))
                  .body("detail", nullValue())
                  .body("instance", nullValue())
                  .body("$", not(hasKey("parameters")))
                  .body("$", not(hasKey("errors")));
            });
  }

  @ServerTest
  void noAcceptAndContentTypeHeaders_shouldProduceProblemAsText(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              Header accept = new Header("Accept", null);
              Header type = new Header(CONTENT_TYPE, null);
              var resp =
                  spec(runner)
                      .header(accept)
                      .header(type)
                      .get("/throw-simple-http-problem")
                      .then()
                      .assertThat()
                      .statusCode(422)
                      .header(
                          CONTENT_TYPE, Matchers.equalToIgnoringCase(text.toContentTypeHeader()))
                      .extract()
                      .asString();
              assertThat(
                  resp,
                  matchesRegex(
                      "HttpProblem\\{timestamp='.*', type=about:blank, title='Id may not be null',"
                          + " status=422, detail='null', instance=null\\}"));
            });
  }

  @ServerTest
  void titleStatusAndDetails_shouldProduceCorrectProblemResponse(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/throw-http-problem-with-details")
                  .then()
                  .assertThat()
                  .statusCode(422)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Invalid input parameter"))
                  .body("status", equalTo(422))
                  .body("detail", equalTo("'Username' field may not be empty"));
            });
  }

  @ServerTest
  void allParametersAreSet_shouldHaveAllParameters(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/throw-problem-with-builder-all-parameters")
                  .then()
                  .assertThat()
                  .statusCode(422)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .header("x-string-header", equalTo("string"))
                  .header("x-int-header", equalTo("12000"))
                  .header("x-object-header", equalTo("{test=45}"))
                  .body("timestamp", is(notNullValue()))
                  .body("type", equalTo("http://example.com/invalid-params"))
                  .body("title", equalTo("Invalid input parameters"))
                  .body("status", equalTo(422))
                  .body("detail", equalTo("'Name' may not be empty"))
                  .body("instance", equalTo("http://example.com/invalid-params/3325"))
                  .body("parameters.key1", equalTo("value1"))
                  .body("parameters.key2", equalTo(List.of(1, 2, 3, 4, 5)))
                  .body("parameters.key3", equalTo(Map.of("m1", List.of(), "m2", Map.of())))
                  .body("errors.size()", equalTo(2));
            });
  }

  @ServerTest
  void throwStatusCodeException_shouldHandleStatusCodeException(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/throw-status-code-exception")
                  .then()
                  .assertThat()
                  .statusCode(400)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Bad Request (400)"))
                  .body("status", equalTo(400))
                  .body("detail", nullValue());
            });
  }

  @ServerTest
  void throwStatusCodeExceptionWithMsg_messageShouldBeInTitle(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/throw-status-code-exception-with-message")
                  .then()
                  .assertThat()
                  .statusCode(400)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Unable to parse request"))
                  .body("status", equalTo(400))
                  .body("detail", nullValue());
            });
  }

  @ServerTest
  void throwNumberFormatException_shouldRespondAsBadRequest(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/throw-number-format-exception")
                  .then()
                  .assertThat()
                  .statusCode(400)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Bad Request"))
                  .body("status", equalTo(400))
                  .body("detail", nullValue());
            });
  }

  @ServerTest
  void throwNumberFormatExceptionWithMessage_messageShouldBeInDetail(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/throw-number-format-exception-with-message")
                  .then()
                  .assertThat()
                  .statusCode(400)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Bad Request"))
                  .body("status", equalTo(400))
                  .body("detail", equalTo("Number should be positive"));
            });
  }

  @ServerTest
  void throwIllegalStateException_shouldNotExposeInternalDetails(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/throw-illegal-state-exception")
                  .then()
                  .assertThat()
                  .statusCode(500)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Server Error"))
                  .body("status", equalTo(500))
                  .body("detail", equalTo(INTERNAL_ERROR_MSG));
            });
  }

  @ServerTest
  void throwInvalidCsrfTokenException_shouldBeTransformedToProblem(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/throw-invalid-csrf-token")
                  .then()
                  .assertThat()
                  .statusCode(403)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Invalid CSRF token"))
                  .body("status", equalTo(403))
                  .body(
                      "detail",
                      equalTo("CSRF token 'lahfuqwefkaslkdfbawiebfsdb=f-dg=-gadfg' is invalid"));
            });
  }

  @ServerTest
  void throwMethodNotAllowedException_shouldBeTransformedToProblem(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/not-allowed")
                  .then()
                  .assertThat()
                  .statusCode(405)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Method Not Allowed"))
                  .body("status", equalTo(405))
                  .body(
                      "detail",
                      equalTo("HTTP method 'GET' is not allowed. Allowed methods are: [POST]"));
            });
  }

  @ServerTest
  void throwNotFoundException_shouldBeTransformedToProblem(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/not-found")
                  .then()
                  .assertThat()
                  .statusCode(404)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Not Found"))
                  .body("status", equalTo(404))
                  .body(
                      "detail",
                      equalTo("Route '/not-found' not found. Please verify request path"));
            });
  }

  @ServerTest
  void throwMissingValueException_shouldBeTransformedToProblem(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .get("/throw-missing-value-exception")
                  .then()
                  .assertThat()
                  .statusCode(400)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Missing value: 'non-existed'"))
                  .body("status", equalTo(400))
                  .body("detail", nullValue());
            });
  }

  @ServerTest
  void throwNotAcceptableException_shouldRespondInHtml(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              var actualHtml =
                  spec(runner)
                      .when()
                      .header("Accept", "application/yaml")
                      .get("/throw-not-acceptable-exception")
                      .then()
                      .assertThat()
                      .statusCode(406)
                      .header(CONTENT_TYPE, html.toContentTypeHeader())
                      .extract()
                      .body()
                      .asString();

              assertThat(
                  actualHtml,
                  containsString(
                      """
<h2>type: about:blank</h2>
<h2>title: Not Acceptable</h2>
<h2>status: 406</h2>
<h2>detail: Server cannot produce a response matching the list of acceptable values defined in the request's 'Accept' header</h2>
"""));
            });
  }

  @ServerTest
  void throwUnsupportedMediaType_shouldBeTransformedToProblem(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .when()
                  .header("Content-Type", "application/json")
                  .get("/throw-unsupported-media-type")
                  .then()
                  .assertThat()
                  .statusCode(415)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Unsupported Media Type"))
                  .body("status", equalTo(415))
                  .body("detail", equalTo("Media type 'application/json' is not supported"));
            });
  }

  @ServerTest
  void customExceptionHandlingWithCustomRender_customExceptionHandlerShouldRender(
      ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .when()
                  .get("/throw-my-custom-exception-with-custom-render")
                  .then()
                  .assertThat()
                  .statusCode(418)
                  .body("message", equalTo("I'm a teapot"));
            });
  }

  @ServerTest
  void customExceptionCaughtAndPropagateAsProblem_shouldBeTransformedToProblem(
      ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .when()
                  .get("/throw-my-custom-exception-and-propagate-as-problem")
                  .then()
                  .assertThat()
                  .statusCode(400)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo("Should be propagated"))
                  .body("status", equalTo(400));
            });
  }

  @ServerTest
  void customOutOfStockProblem_shouldBeHandledAsHttpProblem(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .when()
                  .get("/throw-inherited-out-of-stock-problem")
                  .then()
                  .assertThat()
                  .statusCode(400)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("type", equalTo("https://example.org/out-of-stock"))
                  .body("title", equalTo("Out of Stock"))
                  .body("status", equalTo(400))
                  .body("detail", equalTo("'Coffee Grinder MX-15' is no longer available"))
                  .body(
                      "parameters.suggestions",
                      hasItems("Coffee Grinder MX-17", "Coffee Grinder MX-25"));
            });
  }

  @ServerTest
  void sendEmptyBody_shouldRespond422withDetails(ServerTestRunner runner) {
    runner
        .use(App::new)
        .ready(
            http -> {
              spec(runner)
                  .post("/post-empty-body")
                  .then()
                  .assertThat()
                  .statusCode(422)
                  .header(CONTENT_TYPE, PROBLEM_JSON)
                  .body("title", equalTo(StatusCode.UNPROCESSABLE_ENTITY.reason()))
                  .body("status", equalTo(422))
                  .body("detail", equalTo("No content to map due to end-of-input"));
            });
  }

  private RequestSpecification spec(ServerTestRunner runner) {
    return given().spec(GENERIC_SPEC).port(runner.getAllocatedPort());
  }
}
