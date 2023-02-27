/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import java.util.UUID;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class Controller1786b {

  @GET("/required-param")
  public UUID requiredParam(@QueryParam @NonNull UUID value) {
    return value;
  }
}
