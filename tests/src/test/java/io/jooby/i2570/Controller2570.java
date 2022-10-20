/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i2570;

import io.jooby.annotations.Consumes;
import io.jooby.annotations.FormParam;
import io.jooby.annotations.POST;

public class Controller2570 {
  @POST("/2570")
  @Consumes("multipart/form-data")
  public String createFile(@FormParam Document2570 document) {
    return document.getName() + ": " + document.getFile().getFileName();
  }
}
