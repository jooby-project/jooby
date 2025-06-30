/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import java.util.List;
import java.util.Optional;

import io.jooby.Context;
import io.jooby.Session;
import io.jooby.annotation.*;

@Path("/api")
public class ControllerExample {

  @GET({"/foo", "bar"})
  public String doSomething(@QueryParam Optional<String> q) {
    return "xxx";
  }

  @GET
  @POST("/post")
  public String twoMethods(
      @QueryParam boolean bool,
      @QueryParam short s,
      @QueryParam int i,
      @QueryParam char c,
      @QueryParam long l,
      @QueryParam float f,
      @QueryParam double d) {
    return "xxx";
  }

  @GET
  @Path(("/path"))
  public String pathAtMethodLevel(Context ctx) {
    return "xxx";
  }

  @Path("/path-only")
  public String pathOnly() {
    return "xxx";
  }

  @Path("/session")
  public String ifSession(Optional<Session> ifSession) {
    return "xxx";
  }

  @GET("/returnList")
  @Deprecated
  public List<String> returnList() {
    return null;
  }

  @POST("/bean")
  public ABean save(ABean bean) {
    return bean;
  }
}
