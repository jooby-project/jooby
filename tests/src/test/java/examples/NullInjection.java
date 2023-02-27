/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class NullInjection {

  public static class QParam {
    int foo;

    Integer bar;

    public QParam(int foo, Integer bar) {
      this.foo = foo;
      this.bar = bar;
    }

    public void setBaz(int baz) {}

    @Override
    public String toString() {
      return foo + ":" + bar;
    }
  }

  @GET("/nonnull")
  public Object nonnulArg(@QueryParam int v) {
    return v;
  }

  @GET("/nullok")
  public Object nonnulArg(@QueryParam Integer v) {
    return String.valueOf(v);
  }

  @GET("/nullbean")
  public Object nonnulArg(@QueryParam QParam bean) {
    return bean;
  }
}
