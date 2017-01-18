/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.test;

import static java.util.Objects.requireNonNull;

import org.jooby.Jooby;
import org.junit.rules.ExternalResource;

/**
 * <p>
 * Junit rule to run integration tests. You can choose between @ClassRule or @Rule. The next example
 * uses ClassRule:
 *
 * <pre>
 * import org.jooby.test.JoobyRule;
 *
 * public class MyIntegrationTest {
 *
 *   &#64;ClassRule
 *   private static JoobyRule bootstrap = new JoobyRule(new MyApp());
 *
 * }
 * </pre>
 *
 * <p>
 * Here one and only one instance will be created, which means the application start before the
 * first test and stop after the last test. Application state is shared between tests.
 * </p>
 * <p>
 * While with Rule a new application is created per test. If you have N test, then the application
 * will start/stop N times:
 * </p>
 *
 * <pre>
 * import org.jooby.test.JoobyRule;
 *
 * public class MyIntegrationTest {
 *
 *   &#64;Rule
 *   private static JoobyRule bootstrap = new JoobyRule(new MyApp());
 *
 * }
 * </pre>
 *
 * <p>
 * You are free to choice the HTTP client of your choice, like Fluent Apache HTTP client, REST
 * Assured, etc..
 * </p>
 * <p>
 * Here is a full example with REST Assured:
 * </p>
 *
 * <pre>{@code
 * import org.jooby.Jooby;
 *
 * public class MyApp extends Jooby {
 *
 *   {
 *     get("/", () -> "I'm real");
 *   }
 *
 * }
 *
 * import org.jooby.test.JoobyRyle;
 *
 * public class MyIntegrationTest {
 *
 *   &#64;ClassRule
 *   static JoobyRule bootstrap = new JoobyRule(new MyApp());
 *
 *   &#64;Test
 *   public void integrationTestJustWorks() {
 *     get("/")
 *       .then()
 *       .assertThat()
 *       .body(equalTo("I'm real"));
 *   }
 * }
 * }</pre>
 *
 * @author edgar
 */
public class JoobyRule extends ExternalResource {

  private Jooby app;

  /**
   * Creates a new {@link JoobyRule} to run integration tests.
   *
   * @param app Application to test.
   */
  public JoobyRule(final Jooby app) {
    this.app = requireNonNull(app, "App required.");
  }

  @Override
  protected void before() throws Throwable {
    app.start("server.join=false");
  }

  @Override
  protected void after() {
    app.stop();
  }
}
