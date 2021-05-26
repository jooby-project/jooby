package io.jooby.i2240;

import io.jooby.Jooby;

public class ChildApp2240 extends Jooby {

  {
    get("/child", ctx -> require(Service2240.class).foo());
  }
}
