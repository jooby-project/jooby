/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3508;

import static io.jooby.StatusCode.UNPROCESSABLE_ENTITY_CODE;
import static io.jooby.validation.BeanValidator.validate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;

import com.fasterxml.jackson.databind.json.JsonMapper;
import io.jooby.Extension;
import io.jooby.StatusCode;
import io.jooby.avaje.validator.AvajeValidatorModule;
import io.jooby.hibernate.validator.HibernateValidatorModule;
import io.jooby.i3508.data.AvajeNewAccountRequest;
import io.jooby.i3508.data.HbvNewAccountRequest;
import io.jooby.i3508.data.NewAccountRequest;
import io.jooby.i3508.data.Person;
import io.jooby.jackson.JacksonModule;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.validation.BeanValidator;
import io.jooby.validation.ValidationResult;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class Issue3508 {
  private static final StatusCode STATUS_CODE = StatusCode.UNPROCESSABLE_ENTITY;
  public static final String DEFAULT_TITLE = "Validation failed";

  @ServerTest
  public void shouldValidateUsingProxy(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              app.install(new JacksonModule());
              app.install(new HibernateValidatorModule());

              app.get("/3508/query", validate(ctx -> ctx.query().to(Bean3508.class)));

              app.post("/3508/post", validate(ctx -> ctx.body(Bean3508.class)));

              app.use(BeanValidator.validate());
              app.get("/3508/queryWithType", ctx -> ctx.query(Bean3508.class));
            })
        .ready(
            http -> {
              http.get(
                  "/3508/queryWithType",
                  rsp -> {
                    assertEquals(StatusCode.UNPROCESSABLE_ENTITY.value(), rsp.code());
                  });
              http.get(
                  "/3508/query",
                  rsp -> {
                    assertEquals(StatusCode.UNPROCESSABLE_ENTITY.value(), rsp.code());
                  });
              http.post(
                  "/3508/post",
                  RequestBody.create("{}", MediaType.parse("application/json")),
                  rsp -> {
                    assertEquals(StatusCode.UNPROCESSABLE_ENTITY.value(), rsp.code());
                  });
            });
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
        new HibernateValidatorModule().validationTitle(DEFAULT_TITLE).statusCode(STATUS_CODE),
        new HbvNewAccountRequest(),
        "/create-new-hbv-account");
  }

  private void validatorTest(
      ServerTestRunner runner, Extension extension, NewAccountRequest request, String newAccEndpoint) {
    var sizeLabel = extension instanceof HibernateValidatorModule ? "size" : "length";
    var json = JsonMapper.builder().build();
    runner
        .use(() -> new App3508(extension, false))
        .ready(
            http -> {
              http.post(
                  "/create-person",
                  RequestBody.create(
                      json.writeValueAsBytes(new Person(null, "Last Name")),
                      MediaType.get("application/json")),
                  rsp -> {
                    assertEquals(UNPROCESSABLE_ENTITY_CODE, rsp.code());
                    var actualResult = json.readValue(rsp.body().string(), ValidationResult.class);
                    var fieldError =
                        new ValidationResult.Error(
                            "firstName",
                            List.of("must not be empty"),
                            ValidationResult.ErrorType.FIELD);
                    var expectedResult = buildResult(List.of(fieldError));

                    assertThat(expectedResult)
                        .usingRecursiveComparison()
                        .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.messages")
                        .isEqualTo(actualResult);
                  });

              http.post(
                  "/create-array-of-persons",
                  RequestBody.create(
                      json.writeValueAsBytes(
                          List.of(
                              new Person("First Name", "Last Name"),
                              new Person(null, "Last Name 2"))),
                      MediaType.get("application/json")),
                  rsp -> {
                    assertEquals(UNPROCESSABLE_ENTITY_CODE, rsp.code());
                    var actualResult = json.readValue(rsp.body().string(), ValidationResult.class);

                    var fieldError =
                        new ValidationResult.Error(
                            "firstName",
                            List.of("must not be empty"),
                            ValidationResult.ErrorType.FIELD);
                    var expectedResult = buildResult(List.of(fieldError));

                    assertThat(expectedResult)
                        .usingRecursiveComparison()
                        .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.messages")
                        .isEqualTo(actualResult);
                  });

              http.post(
                  "/create-list-of-persons",
                  RequestBody.create(
                      json.writeValueAsBytes(
                          List.of(
                              new Person("First Name", "Last Name"),
                              new Person(null, "Last Name 2"))),
                      MediaType.get("application/json")),
                  rsp -> {
                    assertEquals(UNPROCESSABLE_ENTITY_CODE, rsp.code());
                    var actualResult = json.readValue(rsp.body().string(), ValidationResult.class);
                    var fieldError =
                        new ValidationResult.Error(
                            "firstName",
                            List.of("must not be empty"),
                            ValidationResult.ErrorType.FIELD);
                    var expectedResult = buildResult(List.of(fieldError));

                    assertThat(expectedResult)
                        .usingRecursiveComparison()
                        .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.messages")
                        .isEqualTo(actualResult);
                  });

              http.post(
                  "/create-map-of-persons",
                  RequestBody.create(
                      json.writeValueAsBytes(
                          Map.of(
                              new Person("First Name", "Last Name"),
                              new Person(null, "Last Name 2"))),
                      MediaType.get("application/json")),
                  rsp -> {
                    assertEquals(UNPROCESSABLE_ENTITY_CODE, rsp.code());
                    var actualResult = json.readValue(rsp.body().string(), ValidationResult.class);
                    var fieldError =
                        new ValidationResult.Error(
                            "firstName",
                            List.of("must not be empty"),
                            ValidationResult.ErrorType.FIELD);
                    ValidationResult expectedResult = buildResult(List.of(fieldError));

                    Assertions.assertThat(expectedResult)
                        .usingRecursiveComparison()
                        .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.messages")
                        .isEqualTo(actualResult);
                  });

              request.setLogin("jk");
              request.setPassword("123");
              request.setConfirmPassword("1234");
              request.setPerson(new Person(null, "Last Name"));
              http.post(
                  newAccEndpoint,
                  RequestBody.create(
                      json.writeValueAsBytes(request), MediaType.get("application/json")),
                  rsp -> {
                    assertEquals(UNPROCESSABLE_ENTITY_CODE, rsp.code());
                    var actualResult = json.readValue(rsp.body().string(), ValidationResult.class);
                    var errors =
                        List.of(
                            new ValidationResult.Error(
                                null,
                                List.of("Passwords should match"),
                                ValidationResult.ErrorType.GLOBAL),
                            new ValidationResult.Error(
                                "password",
                                List.of(sizeLabel + " must be between 8 and 24"),
                                ValidationResult.ErrorType.FIELD),
                            new ValidationResult.Error(
                                "person.firstName",
                                List.of("must not be empty"),
                                ValidationResult.ErrorType.FIELD),
                            new ValidationResult.Error(
                                "confirmPassword",
                                List.of(sizeLabel + " must be between 8 and 24"),
                                ValidationResult.ErrorType.FIELD),
                            new ValidationResult.Error(
                                "login",
                                List.of(sizeLabel + " must be between 3 and 16"),
                                ValidationResult.ErrorType.FIELD));

                    ValidationResult expectedResult = buildResult(errors);

                    Assertions.assertThat(expectedResult)
                        .usingRecursiveComparison()
                        .ignoringCollectionOrderInFieldsMatchingRegexes("errors")
                        .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.messages")
                        .isEqualTo(actualResult);
                  });
            });
  }

  private ValidationResult buildResult(List<ValidationResult.Error> errors) {
    return new ValidationResult(DEFAULT_TITLE, UNPROCESSABLE_ENTITY_CODE, errors);
  }
}
