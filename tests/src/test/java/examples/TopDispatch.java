/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.annotations.Dispatch;
import io.jooby.annotations.GET;

@Dispatch
public class TopDispatch {
  @GET("/")
  public String classlevel() {
    return Thread.currentThread().getName();
  }

  @GET("/method")
  @Dispatch("single")
  public String methodlevel() {
    return Thread.currentThread().getName();
  }
}
