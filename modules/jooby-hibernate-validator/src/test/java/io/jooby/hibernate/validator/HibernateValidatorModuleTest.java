/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.hibernate.validator;

import static io.jooby.StatusCode.UNPROCESSABLE_ENTITY_CODE;
import static io.jooby.hibernate.validator.app.App.DEFAULT_TITLE;
import static io.restassured.RestAssured.given;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import io.jooby.hibernate.validator.app.App;
import io.jooby.hibernate.validator.app.NewAccountRequest;
import io.jooby.hibernate.validator.app.Person;
import io.jooby.test.JoobyTest;
import io.jooby.validation.ValidationResult;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;

@JoobyTest(value = App.class, port = 8099)
public class HibernateValidatorModuleTest {

  protected static RequestSpecification SPEC =
      new RequestSpecBuilder()
          .setPort(8099)
          .setContentType(ContentType.JSON)
          .setAccept(ContentType.JSON)
          .build();

  static {
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
  }

  @Test
  public void validate_personBean_shouldDetect2Violations() {
    Person person = new Person(null, "Last Name");

    ValidationResult actualResult =
        given()
            .spec(SPEC)
            .with()
            .body(person)
            .post("/create-person")
            .then()
            .assertThat()
            .statusCode(UNPROCESSABLE_ENTITY_CODE)
            .extract()
            .as(ValidationResult.class);

    var fieldError =
        new ValidationResult.Error(
            "firstName",
            List.of("must not be empty", "must not be null"),
            ValidationResult.ErrorType.FIELD);
    ValidationResult expectedResult = buildResult(List.of(fieldError));

    Assertions.assertThat(expectedResult)
        .usingRecursiveComparison()
        .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.messages")
        .isEqualTo(actualResult);
  }

  @Test
  public void validate_arrayOfPerson_shouldDetect2Violations() {
    Person person1 = new Person("First Name", "Last Name");
    Person person2 = new Person(null, "Last Name 2");

    ValidationResult actualResult =
        given()
            .spec(SPEC)
            .with()
            .body(new Person[] {person1, person2})
            .post("/create-array-of-persons")
            .then()
            .assertThat()
            .statusCode(UNPROCESSABLE_ENTITY_CODE)
            .extract()
            .as(ValidationResult.class);

    var fieldError =
        new ValidationResult.Error(
            "firstName",
            List.of("must not be empty", "must not be null"),
            ValidationResult.ErrorType.FIELD);
    ValidationResult expectedResult = buildResult(List.of(fieldError));

    Assertions.assertThat(expectedResult)
        .usingRecursiveComparison()
        .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.messages")
        .isEqualTo(actualResult);
  }

  @Test
  public void validate_listOfPerson_shouldDetect2Violations() {
    Person person1 = new Person("First Name", "Last Name");
    Person person2 = new Person(null, "Last Name 2");

    ValidationResult actualResult =
        given()
            .spec(SPEC)
            .with()
            .body(List.of(person1, person2))
            .post("/create-list-of-persons")
            .then()
            .assertThat()
            .statusCode(UNPROCESSABLE_ENTITY_CODE)
            .extract()
            .as(ValidationResult.class);

    var fieldError =
        new ValidationResult.Error(
            "firstName",
            List.of("must not be empty", "must not be null"),
            ValidationResult.ErrorType.FIELD);
    ValidationResult expectedResult = buildResult(List.of(fieldError));

    Assertions.assertThat(expectedResult)
        .usingRecursiveComparison()
        .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.messages")
        .isEqualTo(actualResult);
  }

  @Test
  public void validate_mapOfPerson_shouldDetect2Violations() {
    Person person1 = new Person("First Name", "Last Name");
    Person person2 = new Person(null, "Last Name 2");

    ValidationResult actualResult =
        given()
            .spec(SPEC)
            .with()
            .body(Map.of("1", person1, "2", person2))
            .post("/create-map-of-persons")
            .then()
            .assertThat()
            .statusCode(UNPROCESSABLE_ENTITY_CODE)
            .extract()
            .as(ValidationResult.class);

    var fieldError =
        new ValidationResult.Error(
            "firstName",
            List.of("must not be empty", "must not be null"),
            ValidationResult.ErrorType.FIELD);
    ValidationResult expectedResult = buildResult(List.of(fieldError));

    Assertions.assertThat(expectedResult)
        .usingRecursiveComparison()
        .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.messages")
        .isEqualTo(actualResult);
  }

  @Test
  public void validate_newAccountBean_shouldDetect6Violations() {
    NewAccountRequest request = new NewAccountRequest();
    request.setLogin("jk");
    request.setPassword("123");
    request.setConfirmPassword("1234");
    request.setPerson(new Person(null, "Last Name"));

    ValidationResult actualResult =
        given()
            .spec(SPEC)
            .with()
            .body(request)
            .post("/create-new-account")
            .then()
            .assertThat()
            .statusCode(UNPROCESSABLE_ENTITY_CODE)
            .extract()
            .as(ValidationResult.class);

    List<ValidationResult.Error> errors =
        new ArrayList<>() {
          {
            add(
                new ValidationResult.Error(
                    null, List.of("Passwords should match"), ValidationResult.ErrorType.GLOBAL));
            add(
                new ValidationResult.Error(
                    "person.firstName",
                    List.of("must not be empty", "must not be null"),
                    ValidationResult.ErrorType.FIELD));
            add(
                new ValidationResult.Error(
                    "login",
                    List.of("size must be between 3 and 16"),
                    ValidationResult.ErrorType.FIELD));
            add(
                new ValidationResult.Error(
                    "password",
                    List.of("size must be between 8 and 24"),
                    ValidationResult.ErrorType.FIELD));
            add(
                new ValidationResult.Error(
                    "confirmPassword",
                    List.of("size must be between 8 and 24"),
                    ValidationResult.ErrorType.FIELD));
          }
        };

    ValidationResult expectedResult = buildResult(errors);

    Assertions.assertThat(expectedResult)
        .usingRecursiveComparison()
        .ignoringCollectionOrderInFieldsMatchingRegexes("errors")
        .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.messages")
        .isEqualTo(actualResult);
  }

  private ValidationResult buildResult(List<ValidationResult.Error> errors) {
    return new ValidationResult(DEFAULT_TITLE, UNPROCESSABLE_ENTITY_CODE, errors);
  }
}
