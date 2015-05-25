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
package org.jooby.json;

import static java.util.Objects.requireNonNull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Parser;
import org.jooby.Renderer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

/**
 * JSON support via <a href="https://github.com/google/gson">Gson</a> library.
 *
 * <h1>exposes</h1>
 *
 * <ul>
 * <li>A {@link Gson}</li>
 * <li>A {@link Parser}</li>
 * <li>A {@link Renderer}</li>
 * </ul>
 *
 * <h1>usage</h1>
 *
 * <pre>
 * {
 *   use(new Gzon());
 *
 *   // sending
 *   get("/my-api", req {@literal ->} new MyObject());
 *
 *   // receiving a json body
 *   post("/my-api", req {@literal ->} {
 *     MyObject obj = req.body(MyObject.class);
 *     return obj;
 *   });
 *
 *   // direct access to Gson
 *   get("/access", req {@literal ->} {
 *     Gson gson = req.require(Gson.class);
 *     // ...
 *   });
 * }
 * </pre>
 *
 * <h1>configuration</h1>
 *
 * <p>
 * If you need a special setting or configuration for your {@link Gson}:
 * </p>
 *
 * <pre>
 * {
 *   use(new Gzon().doWith(builder {@literal ->} {
 *     builder.setPrettyPrint();
 *     // ...
 *   });
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.6.0
 */
public class Gzon implements Jooby.Module {

  private MediaType type;

  private BiConsumer<GsonBuilder, Config> configurer;

  /**
   * Creates a new {@link Gson}.
   *
   * @param type {@link MediaType} to use.
   */
  public Gzon(final MediaType type) {
    this.type = requireNonNull(type, "Media type is required.");
  }

  /**
   * Creates a new {@link Gson} and set type to: {@link MediaType#json}.
   *
   */
  public Gzon() {
    this(MediaType.json);
  }

  /**
   * Configurer callback.
   *
   * <pre>
   * {
   *   use(new Gzon().doWith(builder {@literal ->} {
   *     builder.setPrettyPrint();
   *     // ...
   *   });
   * }
   * </pre>
   *
   * @param configurer A callback.
   * @return This instance.
   */
  public Gzon doWith(final BiConsumer<GsonBuilder, Config> configurer) {
    this.configurer = requireNonNull(configurer, "Configurer callback is required.");
    return this;
  }

  /**
   * Configurer callback.
   *
   * <pre>
   * {
   *   use(new Gzon().doWith((builder, config) {@literal ->} {
   *     builder.setPrettyPrint();
   *     // ...
   *   });
   * }
   * </pre>
   *
   * @param configurer A callback.
   * @return This instance.
   */
  public Gzon doWith(final Consumer<GsonBuilder> configurer) {
    requireNonNull(configurer, "Configurer callback is required.");
    this.configurer = (gson, conf) -> configurer.accept(gson);
    return this;
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    GsonBuilder gsonb = new GsonBuilder();

    if (configurer != null) {
      configurer.accept(gsonb, config);
    }

    Gson gson = gsonb.create();

    binder.bind(Gson.class).toInstance(gson);

    Multibinder.newSetBinder(binder, Parser.class).addBinding()
        .toInstance(new GsonParser(type, gson));

    Multibinder.newSetBinder(binder, Renderer.class).addBinding()
        .toInstance(new GsonRenderer(type, gson));
  }

}
