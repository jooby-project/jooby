/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.i3422;

import io.jooby.Route;
import io.jooby.annotation.ResultType;

@ResultType(types = ReactiveType.class, handler = "toReactive")
public class ReactiveTypeGenerator {

  public static Route.Handler toReactive(Route.Handler next) {
    return next;
  }
}
