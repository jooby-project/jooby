/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.i3756;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

@Path("/C3756")
public class C3756 {
  private final S3756 s3756;

  public C3756(S3756 s3756) {
    super();
    this.s3756 = s3756;
  }

  @GET
  public String handle() {
    s3756.accept("hello");
    return "hello";
  }
}
