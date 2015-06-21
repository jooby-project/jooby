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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jooby.Env;
import org.jooby.Jooby;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.MultipleFailureException;
import org.junit.runners.model.Statement;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class JoobyRunner extends BlockJUnit4ClassRunner {

  private Jooby app;

  private int port;

  private Class<?> server;

  public JoobyRunner(final Class<?> klass) throws InitializationError {
    super(klass);
    prepare(klass, null);
  }

  public JoobyRunner(final Class<?> klass, final Class<?> server) throws InitializationError {
    super(klass);
    prepare(klass, server);
  }

  @Override
  protected String getName() {
    if (server != null) {
      return "[" + server.getSimpleName().toLowerCase() + "]";
    }
    return super.getName();
  }

  @Override
  protected String testName(final FrameworkMethod method) {
    if (server != null) {
      return method.getName() + getName();
    }
    return super.testName(method);
  }

  private void prepare(final Class<?> klass, final Class<?> server) throws InitializationError {
    try {
      this.server = server;
      Class<?> appClass = klass;
      port = freePort();
      if (!Jooby.class.isAssignableFrom(appClass)) {
        throw new InitializationError("Invalid jooby app: " + appClass);
      }
      int maxThreads = Math.max(4, Runtime.getRuntime().availableProcessors() / 2) + 2;
      Config config = ConfigFactory.empty("test-config")
          .withValue("server.join", ConfigValueFactory.fromAnyRef(false))
          .withValue("server.http.IdleTimeout", ConfigValueFactory.fromAnyRef("5m"))
          .withValue("server.threads.Min", ConfigValueFactory.fromAnyRef(1))
          .withValue("server.threads.Max", ConfigValueFactory.fromAnyRef(maxThreads))
          .withValue("application.port", ConfigValueFactory.fromAnyRef(port))
          .withValue("undertow.ioThreads", ConfigValueFactory.fromAnyRef(2))
          .withValue("undertow.workerThreads", ConfigValueFactory.fromAnyRef(4))
          .withValue("netty.threads.Parent", ConfigValueFactory.fromAnyRef(2));

      if (server != null) {
        config = config.withFallback(ConfigFactory.empty()
            .withValue("server.module", ConfigValueFactory.fromAnyRef(server.getName())));
      }

      Config testConfig = config;

      app = (Jooby) appClass.newInstance();
      app.use(new Jooby.Module() {
        @Override
        public void configure(final Env mode, final Config config, final Binder binder) {
        }

        @Override
        public Config config() {
          return testConfig;
        }
      });
    } catch (Exception ex) {
      throw new InitializationError(Arrays.asList(ex));
    }
  }

  @Override
  protected Statement withBeforeClasses(final Statement statement) {
    Statement next = super.withBeforeClasses(statement);
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        app.start();

        next.evaluate();
      }
    };
  }

  @Override
  protected Object createTest() throws Exception {
    Object test = super.createTest();
    Guice.createInjector(binder -> {
      binder.bind(Integer.class).annotatedWith(Names.named("port")).toInstance(port);
    }).injectMembers(test);

    return test;
  }

  @Override
  protected Statement withAfterClasses(final Statement statement) {
    Statement next = super.withAfterClasses(statement);
    return new Statement() {

      @Override
      public void evaluate() throws Throwable {
        List<Throwable> errors = new ArrayList<Throwable>();
        try {
          next.evaluate();
        } catch (Throwable e) {
          errors.add(e);
        }

        try {
          app.stop();
        } catch (Exception ex) {
          errors.add(ex);
        }
        if (errors.isEmpty()) {
          return;
        }
        if (errors.size() == 1) {
          throw errors.get(0);
        }
        throw new MultipleFailureException(errors);
      }
    };
  }

  private int freePort() throws IOException {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    }
  }
}
