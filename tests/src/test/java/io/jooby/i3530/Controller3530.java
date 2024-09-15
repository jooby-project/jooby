/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3530;

import io.jooby.annotation.POST;
import jakarta.validation.Valid;

public class Controller3530 {

  @POST("/3530/controller")
  public Bean3530 create(@Valid Bean3530 value) {
    return value;
  }
}
