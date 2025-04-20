/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.util.List;

import io.jooby.Jooby;
import io.jooby.RouterOption;
import io.jooby.StartupSummary;

public class TestApp extends Jooby {
  {
    setStartupSummary(List.of(StartupSummary.NONE));
    setRouterOptions(RouterOption.IGNORE_CASE, RouterOption.IGNORE_TRAILING_SLASH);
    setContextPath("/test");
    get("/", ctx -> "OK");
  }
}
