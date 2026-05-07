/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.htmx;

import io.jooby.ModelAndView;
import io.jooby.annotation.GET;
import io.jooby.annotation.htmx.*;

public class ClaimedRouteHx {

  @GET("/")
  public ModelAndView<Object> index() {
    return null;
  }

  @GET("/tasks")
  @HxView("tasks.hbs")
  public User3936 tasks() {
    return null;
  }
}
