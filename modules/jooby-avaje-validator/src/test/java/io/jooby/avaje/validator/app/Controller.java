/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.validator.app;

import java.util.List;
import java.util.Map;

import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import jakarta.validation.Valid;

@Path("")
public class Controller {

  @POST("/create-person")
  public void createPerson(@Valid Person person) {}

  @POST("/create-array-of-persons")
  public void createArrayOfPersons(@Valid Person[] persons) {}

  @POST("/create-list-of-persons")
  public void createListOfPersons(@Valid List<Person> persons) {}

  @POST("/create-map-of-persons")
  public void createMapOfPersons(@Valid Map<String, Person> persons) {}

  @POST("/create-new-account")
  public void createNewAccount(@Valid NewAccountRequest request) {}
}
