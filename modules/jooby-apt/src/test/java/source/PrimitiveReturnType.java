/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import io.jooby.annotations.GET;

public class PrimitiveReturnType {
  @GET
  public int returnType() {
    return 0;
  }
}
