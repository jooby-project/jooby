package io.jooby.test;

import io.jooby.StatusCode;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.restassured.http.ContentType;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class Issue2611 {

  @ServerTest
  public void shouldReturnBadRequestIfFileFormFieldIsMissing(ServerTestRunner runner) {
    runner.define(app ->
        app.post("/2611/files", ctx -> ctx.file("nonExistentField"))
    ).ready(client ->
        given()
            .port(client.getPort())
            .multiPart("testKey", "testValue")
            .accept(ContentType.JSON)
            .when()
            .post("/2611/files")
            .then()
            .assertThat()
            .statusCode(StatusCode.BAD_REQUEST_CODE)
            .body("message", equalTo("Field 'nonExistentField' is missing"))
    );
  }
}
