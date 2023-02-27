/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package source;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.annotation.GET;
import io.jooby.annotation.QueryParam;

public class Controller1786 {

  @GET("/required-string-param")
  public String requiredStringParam(@QueryParam @NonNull String value) {
    return value;
  }
}
