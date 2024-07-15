/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3472;

import io.jooby.Context;
import io.jooby.annotation.BindParam;
import io.jooby.annotation.GET;

public class C3472 {

  @GET("/3472")
  public BindBean bind(@BindParam BindBean bean) {
    return bean;
  }

  @GET("/3472/named")
  public BindBean bindName(@BindParam(fn = "bindWithName") BindBean bean) {
    return bean;
  }

  @GET("/3472/static")
  public BindBean bindStatic(@BindParam(BindBean.class) BindBean bean) {
    return bean;
  }

  @GET("/3472/extends")
  public BindBean bindExtends(@SubAnnotation BindBean bean) {
    return bean;
  }

  public BindBean convert(Context ctx) {
    return new BindBean(ctx.query("value").value());
  }

  public BindBean bindWithName(Context ctx) {
    return new BindBean("fn:" + ctx.query("value").value());
  }
}
