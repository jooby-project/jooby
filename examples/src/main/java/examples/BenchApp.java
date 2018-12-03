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

import io.jooby.App;
import io.jooby.ExecutionMode;
import io.jooby.Filters;

public class BenchApp extends App {

  private static final String MESSAGE = "Hello World!";

  static class Message {
    public final String message;

    public Message(String message) {
      this.message = message;
    }
  }

  {
    filter(Filters.defaultHeaders());

    get("/", ctx -> ctx.sendText(MESSAGE));

    get("/json", ctx -> Thread.currentThread().getName());

    get("/fortune", ctx -> Thread.currentThread().getName());
  }

  public static void main(String[] args) {
    new BenchApp()
        .mode(ExecutionMode.EVENT_LOOP)
        .start()
        .join();
  }
}
