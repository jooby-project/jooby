package io.jooby.hibernate.validator.app;

import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@Path("")
public class Controller {

    @POST("/create-person")
    public void createPerson(@Valid Person person) {
    }

    @POST("/create-array-of-persons")
    public void createArrayOfPersons(@Valid Person[] persons) {
    }

    @POST("/create-list-of-persons")
    public void createListOfPersons(@Valid List<Person> persons) {
    }

    @POST("/create-map-of-persons")
    public void createMapOfPersons(@Valid Map<String, Person> persons) {
    }

    @POST("/create-new-account")
    public void createNewAccount(@Valid NewAccountRequest request) {
    }
}
