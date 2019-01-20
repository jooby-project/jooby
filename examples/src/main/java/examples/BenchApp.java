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

import io.jooby.Decorators;
import io.jooby.Jooby;
import io.jooby.ExecutionMode;

public class BenchApp extends Jooby {

  private static final String MESSAGE = "Hello World!";

  static class Message {
    public final String message;

    public Message(String message) {
      this.message = message;
    }
  }

  {
    mode(ExecutionMode.EVENT_LOOP);

    decorate(Decorators.defaultHeaders());

    get("/", ctx -> ctx.sendString(MESSAGE));

    get("/json", ctx -> Thread.currentThread().getName());

    get("/fortune", ctx -> Thread.currentThread().getName());
  }

  public static void main(String[] args) {
    run(BenchApp::new, args);
  }
}
