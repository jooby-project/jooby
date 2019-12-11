package io.jooby;

public class TestApp extends Jooby {
  {
    setRouterOptions(RouterOption.LOW_CASE, RouterOption.NO_TRAILING_SLASH);
    setContextPath("/test");
    get("/", ctx -> "OK");
  }
}
