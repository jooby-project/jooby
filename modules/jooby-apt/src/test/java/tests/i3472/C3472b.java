/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3472;

import io.jooby.annotation.BindParam;
import io.jooby.annotation.GET;

public class C3472b {

  @GET("/3472/bean-factory-method")
  public BindBean bind(@BindParam BindBean bean) {
    return bean;
  }

  @GET("/3472/bean-constructor")
  public String bind(@BindParam BindBeanConstructor bean) {
    return bean.toString();
  }
}
