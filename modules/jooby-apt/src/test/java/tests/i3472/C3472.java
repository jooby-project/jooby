/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3472;

import io.jooby.annotation.BindParam;
import io.jooby.annotation.GET;

public class C3472 {

  @GET("/3472/mapping")
  public BindBean bind(@BindParam(BeanMapping.class) BindBean bean) {
    return bean;
  }

  @GET("/3472/withName")
  public BindBean bindWithName(
      @BindParam(value = BeanMapping.class, fn = "withName") BindBean bean) {
    return bean;
  }
}
