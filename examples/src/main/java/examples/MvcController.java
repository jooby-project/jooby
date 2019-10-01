/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import examples.jpa.Person;
import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

import java.util.List;

@Path("/jsonlist")
public class MvcController {

  @POST
  public List<Person> post(List<Person> people) {
    return people;
  }
}
