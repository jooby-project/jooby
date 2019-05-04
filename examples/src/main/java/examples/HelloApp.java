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
import io.jooby.RouterOptions;

import java.util.stream.Stream;

public class HelloApp extends Jooby {

  {
    Stream.of(getClass(), Jooby.class, getLog().getClass())
        .forEach(clazz -> {
          System.out.println(clazz.getName() + " loaded by: " + clazz.getClassLoader());
        });
    setRouterOptions(new RouterOptions().setCaseSensitive(false).setIgnoreTrailingSlash(false));

    get("/foo/bar", ctx -> {
      return ctx.pathString() + "oo";
    });

    get("/foo/{bar}", ctx -> {
      return ctx.path("bar").value();
    });
  }

  public static void main(String[] args) {
    runApp(args, HelloApp::new);
  }
}
