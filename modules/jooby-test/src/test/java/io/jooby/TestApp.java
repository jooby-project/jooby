package io.jooby;

public class TestApp extends Jooby {
  {
    setRouterOptions(RouterOption.IGNORE_CASE, RouterOption.IGNORE_TRAILING_SLASH);
    setContextPath("/test");
    get("/", ctx -> "OK");
  }
}
