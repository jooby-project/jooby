/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem;

import io.jooby.StatusCode;
import io.jooby.problem.data.App;
import io.jooby.test.JoobyTest;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static io.jooby.MediaType.PROBLEM_JSON;
import static io.jooby.MediaType.html;
import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@JoobyTest(value = App.class, port = 9099)
class ProblemDetailsErrHandlerTest {

  protected static RequestSpecification GENERIC_SPEC = new RequestSpecBuilder()
      .setPort(9099)
      .setContentType(ContentType.JSON)
      .build();

  private static final String INTERNAL_ERROR_MSG =
      "The server encountered an internal error or misconfiguration and was unable " +
      "to complete your request";

  static {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  private static final String CONTENT_TYPE = "Content-Type";

  @Test
  void httpProblem_titleAndStatusOnly_shouldProduceCorrectProblemResponse() {
    given().spec(GENERIC_SPEC)
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
  }

  @Test
  void httpProblem_withTitleStatusAndDetails_shouldProduceCorrectProblemResponse() {
    given().spec(GENERIC_SPEC)
        .get("/throw-http-problem-with-details")
        .then()
        .assertThat()
        .statusCode(422)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Invalid input parameter"))
        .body("status", equalTo(422))
        .body("detail", equalTo("'Username' field may not be empty"));
  }

  @Test
  void httpProblemBuilder_emptyBuilder_shouldThrowInternalServerError() {
    given().spec(GENERIC_SPEC)
        .get("/throw-problem-with-empty-builder")
        .then()
        .assertThat()
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .statusCode(500)
        .body("title", equalTo("Internal Server Error"))
        .body("status", equalTo(500));
  }

  @Test
  void httpProblemBuilder_onlyStatus_shouldThrowInternalServerError() {
    given().spec(GENERIC_SPEC)
        .get("/throw-problem-with-builder-status-only")
        .then()
        .assertThat()
        .statusCode(500)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Internal Server Error"))
        .body("status", equalTo(500));
  }

  @Test
  void httpProblemBuilder_onlyTitle_shouldThrowInternalServerError() {
    given().spec(GENERIC_SPEC)
        .get("/throw-problem-with-builder-title-only")
        .then()
        .assertThat()
        .statusCode(500)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Internal Server Error"))
        .body("status", equalTo(500));
  }

  @Test
  void httpProblemBuilder_allParametersAreSet_shouldHaveAllParameters() {
    given().spec(GENERIC_SPEC)
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
  }

  @Test
  void httpProblem_throwStatusCodeException_shouldHandleStatusCodeException() {
    given().spec(GENERIC_SPEC)
        .get("/throw-status-code-exception")
        .then()
        .assertThat()
        .statusCode(400)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Bad Request (400)"))
        .body("status", equalTo(400))
        .body("detail", nullValue());
  }

  @Test
  void httpProblem_throwStatusCodeExceptionWithMsg_messageShouldBeInTitle() {
    given().spec(GENERIC_SPEC)
        .get("/throw-status-code-exception-with-message")
        .then()
        .assertThat()
        .statusCode(400)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Unable to parse request"))
        .body("status", equalTo(400))
        .body("detail", nullValue());
  }

  @Test
  void httpProblem_throwNumberFormatException_shouldRespondAsBadRequest() {
    given().spec(GENERIC_SPEC)
        .get("/throw-number-format-exception")
        .then()
        .assertThat()
        .statusCode(400)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Bad Request"))
        .body("status", equalTo(400))
        .body("detail", nullValue());
  }

  @Test
  void httpProblem_throwNumberFormatExceptionWithMessage_messageShouldBeInDetail() {
    given().spec(GENERIC_SPEC)
        .get("/throw-number-format-exception-with-message")
        .then()
        .assertThat()
        .statusCode(400)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Bad Request"))
        .body("status", equalTo(400))
        .body("detail", equalTo("Number should be positive"));
  }

  @Test
  void httpProblem_throwIllegalStateException_shouldNotExposeInternalDetails() {
    given().spec(GENERIC_SPEC)
        .get("/throw-illegal-state-exception")
        .then()
        .assertThat()
        .statusCode(500)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Internal Server Error"))
        .body("status", equalTo(500))
        .body("detail", equalTo(INTERNAL_ERROR_MSG));
  }

  @Test
  void builtInException_throwInvalidCsrfTokenException_shouldBeTransformedToProblem() {
    given().spec(GENERIC_SPEC)
        .get("/throw-invalid-csrf-token")
        .then()
        .assertThat()
        .statusCode(403)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Invalid CSRF token"))
        .body("status", equalTo(403))
        .body("detail", equalTo("CSRF token 'lahfuqwefkaslkdfbawiebfsdb=f-dg=-gadfg' is invalid"));
  }

  @Test
  void builtInException_throwMethodNotAllowedException_shouldBeTransformedToProblem() {
    given().spec(GENERIC_SPEC)
        .get("/not-allowed")
        .then()
        .assertThat()
        .statusCode(405)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Method Not Allowed"))
        .body("status", equalTo(405))
        .body("detail", equalTo("HTTP method 'GET' for the requested path '/not-allowed' is not allowed"));
  }

  @Test
  void builtInException_throwNotFoundException_shouldBeTransformedToProblem() {
    given().spec(GENERIC_SPEC)
        .get("/not-found")
        .then()
        .assertThat()
        .statusCode(404)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Not Found"))
        .body("status", equalTo(404))
        .body("detail", equalTo("Route not found. Please verify request 'path'"));
  }

  @Test
  void builtInException_throwMissingValueException_shouldBeTransformedToProblem() {
    given().spec(GENERIC_SPEC)
        .get("/throw-missing-value-exception")
        .then()
        .assertThat()
        .statusCode(400)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Missing value: 'non-existed'"))
        .body("status", equalTo(400))
        .body("detail", nullValue());
  }

  @Test
  void builtInException_throwNotAcceptableException_shouldRespondInHtml() {
    var actualHtml = given().spec(GENERIC_SPEC)
        .when()
        .header("Accept", "application/yaml")
        .get("/throw-not-acceptable-exception")
        .then()
        .assertThat()
        .statusCode(406)
        .header(CONTENT_TYPE, html.toContentTypeHeader(StandardCharsets.UTF_8))
        .extract().body().asString();

    assertThat(actualHtml, containsString("""
        <h2>type: about:blank</h2>
        <h2>title: Not Acceptable</h2>
        <h2>status: 406</h2>
        <h2>detail: Server cannot produce a response matching the list of acceptable values defined in the request's 'Accept' header</h2>
        """));
  }

  @Test
  void builtInException_throwUnsupportedMediaType_shouldBeTransformedToProblem() {
    given().spec(GENERIC_SPEC)
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
  }

  @Test
  void httpProblem_customExceptionHandlingWithCustomRender_customExceptionHandlerShouldRender() {
    given().spec(GENERIC_SPEC)
        .when()
        .get("/throw-my-custom-exception-with-custom-render")
        .then()
        .assertThat()
        .statusCode(418)
        .body("message", equalTo("I'm a teapot"));
  }

  @Test
  void httpProblem_customExceptionCaughtAndPropagateAsProblem_shouldBeTransformedToProblem() {
    given().spec(GENERIC_SPEC)
        .when()
        .get("/throw-my-custom-exception-and-propagate-as-problem")
        .then()
        .assertThat()
        .statusCode(400)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo("Should be propagated"))
        .body("status", equalTo(400));
  }

  @Test
  void httpProblem_customOutOfStockProblem_shouldBeHandledAnyHttpProblem() {
    given().spec(GENERIC_SPEC)
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
        .body("parameters.suggestions", hasItems("Coffee Grinder MX-17", "Coffee Grinder MX-25"));
  }

  @Test
  void httpProblem_sendEmptyBody_shouldRespond422withDetails() {
    given().spec(GENERIC_SPEC)
        .post("/post-empty-body")
        .then()
        .assertThat()
        .statusCode(422)
        .header(CONTENT_TYPE, PROBLEM_JSON)
        .body("title", equalTo(StatusCode.UNPROCESSABLE_ENTITY.reason()))
        .body("status", equalTo(422))
        .body("detail", equalTo("No content to map due to end-of-input"));
  }
}
