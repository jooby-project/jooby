package io.jooby.validation;

import io.jooby.test.JoobyTest;
import io.jooby.validation.app.App;
import io.jooby.validation.app.NewAccountRequest;
import io.jooby.validation.app.Person;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.jooby.StatusCode.UNPROCESSABLE_ENTITY_CODE;
import static io.jooby.validation.app.App.DEFAULT_TITLE;
import static io.restassured.RestAssured.given;

@JoobyTest(value = App.class, port = 8099)
public class BeanValidatorTest {

    protected static RequestSpecification SPEC = new RequestSpecBuilder()
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

        ValidationResult actualResult = given().spec(SPEC).
                with()
                .body(person)
                .post("/create-person")
                .then()
                .assertThat()
                .statusCode(UNPROCESSABLE_ENTITY_CODE)
                .extract().as(ValidationResult.class);

        var fieldError = new FieldError(
                "firstName",
                List.of("must not be empty", "must not be null")
        );
        ValidationResult expectedResult = buildResult(new Errors(List.of(), List.of(fieldError)));

        Assertions.assertThat(expectedResult)
                .usingRecursiveComparison()
                .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.fieldErrors\\.messages")
                .isEqualTo(actualResult);
    }

    @Test
    public void validate_arrayOfPerson_shouldDetect2Violations() {
        Person person1 = new Person("First Name", "Last Name");
        Person person2 = new Person(null, "Last Name 2");

        ValidationResult actualResult = given().spec(SPEC).
                with()
                .body(new Person[]{person1, person2})
                .post("/create-array-of-persons")
                .then()
                .assertThat()
                .statusCode(UNPROCESSABLE_ENTITY_CODE)
                .extract().as(ValidationResult.class);

        var fieldError = new FieldError(
                "firstName",
                List.of("must not be empty", "must not be null")
        );
        ValidationResult expectedResult = buildResult(new Errors(List.of(), List.of(fieldError)));

        Assertions.assertThat(expectedResult)
                .usingRecursiveComparison()
                .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.fieldErrors\\.messages")
                .isEqualTo(actualResult);
    }

    @Test
    public void validate_listOfPerson_shouldDetect2Violations() {
        Person person1 = new Person("First Name", "Last Name");
        Person person2 = new Person(null, "Last Name 2");

        ValidationResult actualResult = given().spec(SPEC).
                with()
                .body(List.of(person1, person2))
                .post("/create-list-of-persons")
                .then()
                .assertThat()
                .statusCode(UNPROCESSABLE_ENTITY_CODE)
                .extract().as(ValidationResult.class);

        var fieldError = new FieldError(
                "firstName",
                List.of("must not be empty", "must not be null")
        );
        ValidationResult expectedResult = buildResult(new Errors(List.of(), List.of(fieldError)));

        Assertions.assertThat(expectedResult)
                .usingRecursiveComparison()
                .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.fieldErrors\\.messages")
                .isEqualTo(actualResult);
    }

    @Test
    public void validate_mapOfPerson_shouldDetect2Violations() {
        Person person1 = new Person("First Name", "Last Name");
        Person person2 = new Person(null, "Last Name 2");

        ValidationResult actualResult = given().spec(SPEC).
                with()
                .body(Map.of("1", person1, "2", person2))
                .post("/create-map-of-persons")
                .then()
                .assertThat()
                .statusCode(UNPROCESSABLE_ENTITY_CODE)
                .extract().as(ValidationResult.class);

        var fieldError = new FieldError(
                "firstName",
                List.of("must not be empty", "must not be null")
        );
        ValidationResult expectedResult = buildResult(new Errors(List.of(), List.of(fieldError)));

        Assertions.assertThat(expectedResult)
                .usingRecursiveComparison()
                .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.fieldErrors\\.messages")
                .isEqualTo(actualResult);
    }

    @Test
    public void validate_newAccountBean_shouldDetect6Violations() {
        NewAccountRequest request = new NewAccountRequest();
        request.setLogin("jk");
        request.setPassword("123");
        request.setConfirmPassword("1234");
        request.setPerson(new Person(null, "Last Name"));

        ValidationResult actualResult = given().spec(SPEC).
                with()
                .body(request)
                .post("/create-new-account")
                .then()
                .assertThat()
                .statusCode(UNPROCESSABLE_ENTITY_CODE)
                .extract().as(ValidationResult.class);

        List<FieldError> fieldErrors = new ArrayList<>() {{
            add(new FieldError(
                    "person.firstName",
                    List.of("must not be empty", "must not be null"))
            );
            add(new FieldError(
                    "login",
                    List.of("size must be between 3 and 16"))
            );
            add(new FieldError(
                    "password",
                    List.of("size must be between 8 and 24"))
            );
            add(new FieldError(
                    "confirmPassword",
                    List.of("size must be between 8 and 24"))
            );
        }};

        String objectErrors = "Passwords should match";
        Errors errors = new Errors(List.of(objectErrors), fieldErrors);
        ValidationResult expectedResult = buildResult(errors);

        Assertions.assertThat(expectedResult)
                .usingRecursiveComparison()
                .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.fieldErrors")
                .ignoringCollectionOrderInFieldsMatchingRegexes("errors\\.fieldErrors\\.messages")
                .isEqualTo(actualResult);
    }

    private ValidationResult buildResult(Errors errors) {
        return new ValidationResult(DEFAULT_TITLE, UNPROCESSABLE_ENTITY_CODE, errors);
    }
}
