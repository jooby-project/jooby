/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.FileUpload;
import io.jooby.annotation.FormParam;
import io.jooby.annotation.POST;

public class FormController {

  @POST("/single")
  public String postSingle(@FormParam String name) {
    return "...";
  }

  @POST("/multiple")
  public String postMultiple(
      @FormParam String firstname, @FormParam String lastname, @FormParam FileUpload picture) {
    return "...";
  }

  @POST("/bean")
  public String postBean(@FormParam AForm form) {
    return "...";
  }
}
