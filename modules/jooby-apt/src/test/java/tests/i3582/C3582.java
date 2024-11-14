/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3582;

import com.google.inject.Inject;
import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

@Annotation3582
public class C3582 {

  @Inject
  public C3582() {}

  @GET("/hello")
  public String helloWorld(@QueryParam String property) {
    return "hello world" + property;
  }
}
