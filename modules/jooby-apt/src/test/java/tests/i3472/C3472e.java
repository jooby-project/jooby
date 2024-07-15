/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3472;

import io.jooby.annotation.GET;

public class C3472e {

  @GET("/3472/custom-bind")
  public BindBean bind(@CustomBind BindBean bean) {
    return bean;
  }
}
