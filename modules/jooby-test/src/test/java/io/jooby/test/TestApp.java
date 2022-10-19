package io.jooby.test;

import io.jooby.Jooby;
import io.jooby.RouterOption;

public class TestApp extends Jooby {
  {
    setRouterOptions(RouterOption.IGNORE_CASE, RouterOption.IGNORE_TRAILING_SLASH);
    setContextPath("/test");
    get("/", ctx -> "OK");
  }
}
