/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;

import java.util.concurrent.Executors;

public class DispatchApp extends Jooby {
  {

    setWorker(Executors.newCachedThreadPool());

    decorator(next -> ctx -> {
      System.out.println(Thread.currentThread().getName());
      return next.apply(ctx);
    });

    get("/", ctx -> ctx.query("n").intValue(2));

    dispatch(() -> {
      get("/worker", ctx -> ctx.query("n").intValue(2));
    });

    executor("single", Executors.newSingleThreadExecutor());
    dispatch(() -> {
      get("/worker", ctx -> ctx.query("n").intValue(2));
    });
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.EVENT_LOOP, DispatchApp::new);
  }
}
