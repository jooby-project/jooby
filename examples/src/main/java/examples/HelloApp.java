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
import io.jooby.Filters;
import io.jooby.FileUpload;
import io.jooby.json.Jackson;

public class HelloApp extends Jooby {

  public static class User {

    public final String id;

    public final String pass;

    public final FileUpload pic;

    public User(String id, String pass, FileUpload pic) {
      this.id = id;
      this.pass = pass;
      this.pic = pic;
    }
  }
  private static final String MESSAGE = "Hello World!";

  static class Message {
    public final String message;

    public Message(String message) {
      this.message = message;
    }
  }

  {
    decorate(next -> ctx -> {
      System.out.println(Thread.currentThread());
      return next.apply(ctx);
    });

    decorate(Filters.defaultHeaders());

    get("/", ctx -> ctx.sendText(MESSAGE));

    get("/{foo}", ctx -> ctx.sendText("Hello World!"));

    post("/user", ctx -> {
      User user = ctx.multipart(User.class);
      return user.pic.toString();
    });

    renderer(new Jackson());
    get("/json", ctx -> ctx.type("application/json").send(new Message("Hello World!")));

    error((ctx, cause, statusCode) -> {
      cause.printStackTrace();
      ctx.statusCode(statusCode)
          .sendText(statusCode.reason());
    });
  }

  public static void main(String[] args) {
    new HelloApp()
        .start()
        .join();
  }
}
