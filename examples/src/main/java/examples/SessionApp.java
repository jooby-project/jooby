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
package examples;

import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.Session;
import io.jooby.SessionOptions;

public class SessionApp extends Jooby {

  {
    setSessionOptions(new SessionOptions());
    get("/exists", ctx -> ctx.sessionOrNull() != null);

    get("/create", ctx -> {
      Session session = ctx.session();
      ctx.queryMap().forEach(session::put);
      return session.toMap();
    });
  }

  public static void main(String[] args) {
    runApp(args, ExecutionMode.EVENT_LOOP, SessionApp::new);
  }
}
