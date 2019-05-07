/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby.run;

import java.lang.reflect.Method;
import java.util.function.Consumer;

public class ServerRef implements Consumer<Object> {

  private static volatile Object server;

  @Override public void accept(Object server) {
    ServerRef.server = server;
  }

  public static void stopServer() throws Exception {
    if (server != null) {
      try {
        Method stop = server.getClass().getDeclaredMethod("stop");
        stop.invoke(server);
      } finally {
        server = null;
      }
    }
  }
}
