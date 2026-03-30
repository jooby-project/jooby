/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.instance;

import io.jooby.annotation.GET;
import jakarta.inject.Inject;

public class DIController {
  private final NoDIController controller;

  @Inject
  public DIController(NoDIController controller) {
    this.controller = controller;
  }

  @GET("/di")
  public String di() {
    return controller.noDI();
  }
}
