/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package examples;

import io.jooby.Jooby;
import io.jooby.ExecutionMode;

import java.util.concurrent.Executors;

public class DispatchApp extends Jooby {
  {

    worker(Executors.newCachedThreadPool());

    filter(next -> ctx -> {
      System.out.println(Thread.currentThread().getName());
      return next.apply(ctx);
    });

    after((ctx, value) -> {
      Number n = (Number) value;
      return n.intValue() * 2;
    });

    get("/", ctx -> ctx.query("n").intValue(2));

    dispatch(() -> {
      get("/worker", ctx -> ctx.query("n").intValue(2));
    });
  }

  public static void main(String[] args) {
    new DispatchApp()
        .mode(ExecutionMode.EVENT_LOOP)
        .start()
        .join();
  }
}
