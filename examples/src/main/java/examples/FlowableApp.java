/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.reactivex.Flowable;

public class FlowableApp extends Jooby {
  {
    get("/chunk", ctx -> {
      System.out.println(Thread.currentThread());
      return Flowable.range(1, 10)
//          .delay(1, TimeUnit.SECONDS)
          .map(v -> v + ", ");
    });
  }

  public static void main(String[] args) {
    runApp(args, FlowableApp::new);
  }

}
