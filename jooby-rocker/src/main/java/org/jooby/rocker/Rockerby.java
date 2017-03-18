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
package org.jooby.rocker;

import org.jooby.Env;
import org.jooby.Jooby.Module;
import org.jooby.Renderer;
import org.jooby.View;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

/**
 * <h1>rocker</h1>
 * <p>
 * Java 8 optimized, memory efficient, speedy template engine producing statically typed, plain java
 * objects.
 * </p>
 * <p>
 * <a href="https://github.com/fizzed/rocker">Rocker</a> is a Java 8 optimized
 * (runtime compat with 6+), near zero-copy rendering, speedy template engine that produces
 * statically typed, plain java object templates that are compiled along with the rest of your
 * project.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * {
 *   use(new Rockerby());
 *
 *   // Rocker API:
 *   get("/", () -> views.index.template("Rocker"));
 * }
 * }</pre>
 *
 * <h2>rocker idioms</h2>
 * <p>
 * <a href="https://github.com/fizzed/rocker">Rocker</a> support two flavors. The one showed before
 * is the recommend way of using <a href="https://github.com/fizzed/rocker">Rocker</a>.
 * </p>
 *
 * <p>
 * The <strong>static</strong>, <strong>efficient</strong> and <strong>type-safe</strong> flavor:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Rockerby());
 *
 *   get("/", () -> views.index.template("Rocker"));
 * }
 * }</pre>
 *
 * <p>
 * The <strong>dynamic</strong> flavor is available via {@link View} objects:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Rockerby());
 *
 *   get("/", () -> Results.html("views/index").put("message", "Rocker"));
 * }
 * }</pre>
 *
 * <p>
 * This is just syntax sugar for:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Rockerby());
 *
 *   get("/", () -> {
 *     return Rocker.template("views/index.rocker.html").bind("message", "Rocker");
 *   });
 * }
 * }</pre>
 *
 * <h2>code generation</h2>
 *
 * <h3>maven</h3>
 * <p>
 * We do provide code generation via Maven profile. All you have to do is to write a
 * <code>rocker.activator</code> file inside the <code>src/etc</code> folder. File presence triggers
 * generation of source code.
 * </p>
 *
 * <h3>gradle</h3>
 * <p>
 * Please refer to <a href="https://github.com/fizzed/rocker/issues/33">Rocker documentation</a> for
 * Gradle.
 * </p>
 *
 * <h2>hot reload</h2>
 * <p>
 * You don't need <a href="https://github.com/fizzed/rocker#hot-reloading">Rocker hot reload</a> as
 * long as you start your application in development with
 * <a href="http://jooby.org/doc/devtools/">jooby:run</a>. Because
 * <a href="http://jooby.org/doc/devtools/">jooby:run</a> already restart the application on
 * <code>class changes</code>.
 * </p>
 *
 * <p>
 * That's all folks!!
 * </p>
 *
 * @author edgar
 * @since 1.1.0
 */
public class Rockerby implements Module {

  private final String prefix;

  private final String suffix;

  /**
   * Creates a new {@link Rockerby}.
   *
   * @param prefix Template prefix.
   * @param suffix Template suffix.
   */
  public Rockerby(final String prefix, final String suffix) {
    this.prefix = prefix;
    this.suffix = suffix;
  }

  /**
   * Creates a new {@link Rockerby}.
   *
   * @param prefix Template prefix.
   */
  public Rockerby(final String prefix) {
    this(prefix, ".rocker.html");
  }

  /**
   * Creates a new {@link Rockerby}.
   */
  public Rockerby() {
    this("");
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) throws Throwable {
    Multibinder.newSetBinder(binder, Renderer.class)
        .addBinding()
        .toInstance(new RockerRenderer(prefix, suffix));
  }

}
