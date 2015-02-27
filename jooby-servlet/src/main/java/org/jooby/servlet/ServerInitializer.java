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
package org.jooby.servlet;

import static java.util.Objects.requireNonNull;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.spi.Server;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class ServerInitializer implements ServletContextListener {

  public static class ServletModule implements Jooby.Module {

    @Override
    public void configure(final Env env, final Config config, final Binder binder) {
      binder.bind(Server.class).toInstance(NOOP);
    }

  }

  private static final Server NOOP = new Server() {
    @Override
    public void stop() throws Exception {
    }

    @Override
    public void start() throws Exception {
    }

    @Override
    public void join() throws InterruptedException {

    }
  };

  @Override
  public void contextInitialized(final ServletContextEvent sce) {
    ServletContext ctx = sce.getServletContext();
    String appClass = ctx.getInitParameter("application.class");
    requireNonNull(appClass, "Context param NOT found: application.class");
    try {

      Jooby app = (Jooby) ctx.getClassLoader().loadClass(appClass).newInstance();

      app.use(ConfigFactory.empty()
          .withValue("application.path", ConfigValueFactory.fromAnyRef(ctx.getContextPath()))
          .withValue("server.module", ConfigValueFactory.fromAnyRef(ServletModule.class.getName())));

      app.start();

      ctx.setAttribute(Jooby.class.getName(), app);

    } catch (Throwable ex) {
      throw new IllegalStateException("App didn't to start: " + appClass, ex);
    }

  }

  @Override
  public void contextDestroyed(final ServletContextEvent sce) {
    ServletContext ctx = sce.getServletContext();
    Jooby app = (Jooby) ctx.getAttribute(Jooby.class.getName());
    if (app != null) {
      app.stop();
    }
  }

}
