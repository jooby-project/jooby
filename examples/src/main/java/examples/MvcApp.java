/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MvcApp extends Jooby {

  {
    ExecutorService single = Executors.newSingleThreadExecutor();
    executor("single", single);

    ExecutorService cached = Executors.newCachedThreadPool();
    executor("cached", cached);

    mvc(new PlainText());
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.EVENT_LOOP, MvcApp::new);
  }
}
