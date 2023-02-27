/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.annotation.GET;
import io.jooby.annotation.Path;
import kotlin.coroutines.Continuation;

@Path("/suspend")
public class SuspendRoute {

  @GET
  public Continuation suspendFun(Continuation continuation) {
    return continuation;
  }
}
