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

import org.jooby.Env;
import org.jooby.Jooby;
import org.junit.rules.ExternalResource;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JoobyRule extends ExternalResource {

  private static class NoJoin implements Jooby.Module {

    @Override
    public void configure(final Env env, final Config config, final Binder binder) {
    }

    @Override
    public Config config() {
       return ConfigFactory.empty("test-config")
       .withValue("server.join", ConfigValueFactory.fromAnyRef(false));
    }
  }

  private Jooby app;

  public JoobyRule(final Jooby app) {
    this.app = requireNonNull(app, "App required.");

    app.use(new NoJoin());
  }

  @Override
  protected void before() throws Throwable {
    app.start();
  }

  @Override
  protected void after() {
    app.stop();
  }
}
