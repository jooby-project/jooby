/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.util.List;

import io.jooby.annotations.POST;
import io.jooby.annotations.Path;

@Path("/1391")
public class Controller1391 {

  public static class Data1391 {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @POST
  public List<Data1391> post(List<Data1391> data) {
    return data;
  }
}
