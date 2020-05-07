package io.jooby;

public class TestArgApp extends Jooby {
  public TestArgApp(String name) {
    get("/", ctx -> name);
  }
}
