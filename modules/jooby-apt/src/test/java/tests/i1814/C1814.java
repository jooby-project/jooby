/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i1814;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.Reified;
import io.jooby.Route;
import io.jooby.annotations.GET;
import io.jooby.annotations.QueryParam;

public class C1814 {
  @GET("/1814")
  public List<? extends U1814> getUsers(@QueryParam @NonNull String type, Route route) {
    assertEquals(Reified.list(U1814.class).getType(), route.getReturnType());
    return Collections.singletonList(new U1814(type));
  }
}
