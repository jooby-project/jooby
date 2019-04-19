package io.jooby;

public class TestApp extends Jooby {
  {
    setContextPath("/test");
    get("/", ctx -> "OK");
  }
}
