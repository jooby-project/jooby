package io.jooby;

import java.util.concurrent.Executor;

public class MvcApp extends Jooby {
  {
    Executor noop = task -> {};

    executor("single", noop);

    mvc(new MvcController());
  }
}
