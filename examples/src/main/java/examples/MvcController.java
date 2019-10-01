/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import examples.jpa.Person;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;
import io.jooby.annotations.PathParam;
import io.jooby.annotations.QueryParam;

import java.util.List;

@Path("/jsonlist")
public class MvcController {

  @POST("/{id}")
  public List<Person> post(@PathParam String id,  List<Person> people) {
    return people;
  }
}
