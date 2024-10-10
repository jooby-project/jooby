/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3508;

import io.jooby.Extension;
import io.jooby.StatusCode;
import io.jooby.avaje.validator.AvajeValidatorModule;
import io.jooby.hibernate.validator.HibernateValidatorModule;
import io.jooby.i3508.data.AvajeNewAccountRequest;
import io.jooby.i3508.data.HbvNewAccountRequest;
import io.jooby.i3508.data.NewAccountRequest;
import io.jooby.i3508.data.Person;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.validation.ValidationResult;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.Assertions;

import java.util.List;

import static io.jooby.StatusCode.UNPROCESSABLE_ENTITY_CODE;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

public class Issue3508ProblemDetails {
  private static final StatusCode STATUS_CODE = StatusCode.UNPROCESSABLE_ENTITY;
  public static final String DEFAULT_TITLE = "Validation failed";

  protected static RequestSpecification GENERIC_SPEC = new RequestSpecBuilder()
      .setContentType(ContentType.JSON)
      .build();

  static {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @ServerTest
  public void avajeValidatorTest(ServerTestRunner runner) {
    validatorTest(
        runner,
        new AvajeValidatorModule().validationTitle(DEFAULT_TITLE).statusCode(STATUS_CODE),
        new AvajeNewAccountRequest(),
        "/create-new-avaje-account");
  }

  @ServerTest
  public void hibernateValidatorTest(ServerTestRunner runner) {
    validatorTest(
        runner,
        new HibernateValidatorModule().validationTitle(DEFAULT_TITLE)
            .statusCode(STATUS_CODE),
        new HbvNewAccountRequest(),
        "/create-new-hbv-account");
  }

  private void validatorTest(
      ServerTestRunner runner, Extension extension, NewAccountRequest request, String newAccEndpoint) {
    var sizeLabel = extension instanceof HibernateValidatorModule ? "size" : "length";
    runner
        .use(() -> new App3508(extension, true))
        .ready(
            http -> {
              request.setLogin("jk");
              request.setPassword("123");
              request.setConfirmPassword("1234");
              request.setPerson(new Person(null, "Last Name"));

              List<ValidationResult.ProblemError> actualErrors = spec(runner)
                  .body(request)
                  .post(newAccEndpoint)
                  .then()
                  .assertThat()
                  .statusCode(UNPROCESSABLE_ENTITY_CODE)
                  .body("title", equalTo(DEFAULT_TITLE))
                  .body("status", equalTo(UNPROCESSABLE_ENTITY_CODE))
                  .body("detail", equalTo("5 constraint violation(s) detected"))
                  .extract()
                  .jsonPath()
                  .getList("errors", ValidationResult.ProblemError.class);

              var expectedErrors =
                  List.of(
                      new ValidationResult.ProblemError(
                          "Passwords should match",
                          "/",
                          ValidationResult.ErrorType.GLOBAL),
                      new ValidationResult.ProblemError(
                          sizeLabel + " must be between 8 and 24",
                          "/password",
                          ValidationResult.ErrorType.FIELD),
                      new ValidationResult.ProblemError(
                          "must not be empty",
                          "/person/firstName",
                          ValidationResult.ErrorType.FIELD),
                      new ValidationResult.ProblemError(
                          sizeLabel + " must be between 8 and 24",
                          "/confirmPassword",
                          ValidationResult.ErrorType.FIELD),
                      new ValidationResult.ProblemError(
                          sizeLabel + " must be between 3 and 16",
                          "/login",
                          ValidationResult.ErrorType.FIELD));

              Assertions.assertThat(expectedErrors)
                  .usingRecursiveComparison()
                  .ignoringCollectionOrder()
                  .isEqualTo(actualErrors);

            });
  }

  private RequestSpecification spec(ServerTestRunner runner) {
    return given().spec(GENERIC_SPEC).port(runner.getAllocatedPort());
  }
}
