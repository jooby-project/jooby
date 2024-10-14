/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3508;

import java.util.List;
import java.util.Map;

import io.jooby.annotation.POST;
import io.jooby.annotation.Path;
import io.jooby.i3508.data.AvajeNewAccountRequest;
import io.jooby.i3508.data.HbvNewAccountRequest;
import io.jooby.i3508.data.NewAccountRequest;
import io.jooby.i3508.data.Person;
import jakarta.validation.Valid;

@Path("")
public class Controller3508 {

  @POST("/create-person")
  public void createPerson(@Valid Person person) {}

  @POST("/create-array-of-persons")
  public void createArrayOfPersons(@Valid Person[] persons) {}

  @POST("/create-list-of-persons")
  public void createListOfPersons(@Valid List<Person> persons) {}

  @POST("/create-map-of-persons")
  public void createMapOfPersons(@Valid Map<String, Person> persons) {}

  @POST("/create-new-hbv-account")
  public void createNewAccount(@Valid HbvNewAccountRequest request) {}
  @POST("/create-new-avaje-account")
  public void createNewAccount(@Valid AvajeNewAccountRequest request) {}
}
