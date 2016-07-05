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
/**
 * This copy of Woodstox XML processor is licensed under the
 * Apache (Software) License, version 2.0 ("the License").
 * See the License for details about distribution rights, and the
 * specific rights regarding derivate works.
 *
 * You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/
 *
 * A copy is also included in the downloadable source code package
 * containing Woodstox, in file "ASL2.0", under the same directory
 * as this file.
 */
package org.jooby.run;

import java.net.URLClassLoader;
import java.util.List;
import java.util.function.Consumer;

import org.gradle.api.Project;
import org.jooby.Jooby;
import org.jooby.Route;

public class JoobyRunner {

  private JoobyProject project;

  private Consumer<List<Route.Definition>> routes;

  public JoobyRunner(final Project project) {
    this.project = new JoobyProject(project);
  }

  public JoobyRunner with(final Consumer<List<Route.Definition>> callback) {
    this.routes = callback;
    return this;
  }

  public void run(final String mainClass, final Consumer<Jooby> callback, final String... args)
      throws Throwable {
    ClassLoader global = Thread.currentThread().getContextClassLoader();
    try (URLClassLoader local = project.newClassLoader()) {
      Thread.currentThread().setContextClassLoader(local);
      Jooby app = (Jooby) local.loadClass(mainClass).newInstance();
      callback.accept(app);
      app.start(args, routes);
    } finally {
      Thread.currentThread().setContextClassLoader(global);
    }
  }

}
