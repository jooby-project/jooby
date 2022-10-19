package io.jooby.test;

import io.jooby.Jooby;

public class TestArgApp extends Jooby {
  public TestArgApp(String name) {
    get("/", ctx -> name);
  }
}
