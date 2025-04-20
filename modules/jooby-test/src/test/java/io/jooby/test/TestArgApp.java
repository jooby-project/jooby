/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import java.util.List;

import io.jooby.Jooby;
import io.jooby.StartupSummary;

public class TestArgApp extends Jooby {
  public TestArgApp(String name) {
    setStartupSummary(List.of(StartupSummary.NONE));
    get("/", ctx -> name);
  }
}
