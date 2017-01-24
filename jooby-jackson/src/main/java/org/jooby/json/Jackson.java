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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Parser;
import org.jooby.Renderer;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

/**
 * <h1>jackson</h1>
 *
 * JSON support from the excellent <a href="https://github.com/FasterXML/jackson">Jackson</a>
 * library.
 *
 * This module provides a JSON {@link Parser} and {@link Renderer}, but also an
 * {@link ObjectMapper}.
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   use(new Jackson());
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
 *   // receiving a json param from a multipart or form url encoded
 *   post("/my-api", req {@literal ->} {
 *     MyObject obj = req.param("my-object").to(MyObject.class);
 *     return obj;
 *   });
 * }
 * </pre>
 *
 * <h2>advanced configuration</h2>
 * <p>
 * If you need a special setting or configuration for your {@link ObjectMapper}:
 * </p>
 *
 * <pre>
 * {
 *   use(new Jackson().configure(mapper {@literal ->} {
 *     // setup your custom object mapper
 *   });
 * }
 * </pre>
 *
 * or provide an {@link ObjectMapper} instance:
 *
 * <pre>
 * {
 *   ObjectMapper mapper = ....;
 *   use(new Jackson(mapper));
 * }
 * </pre>
 *
 * It is possible to wire Jackson modules too:
 *
 * <pre>
 * {
 *
 *   use(new Jackson()
 *      .module(MyJacksonModuleWiredByGuice.class)
 *   );
 * }
 * </pre>
 *
 * This is useful when your jackson module require some dependencies.
 *
 * @author edgar
 * @since 0.6.0
 */
public class Jackson implements Jooby.Module {

  private static class PostConfigurer {

    @Inject
    public PostConfigurer(final ObjectMapper mapper, final Set<Module> jacksonModules) {
      mapper.registerModules(jacksonModules);
    }

  }

  private final Optional<ObjectMapper> mapper;

  private MediaType type = MediaType.json;

  private Consumer<ObjectMapper> configurer;

  private List<Consumer<Multibinder<Module>>> modules = new ArrayList<>();

  /**
   * Creates a new {@link Jackson} module and use the provided {@link ObjectMapper} instance.
   *
   * @param mapper {@link ObjectMapper} to apply.
   */
  public Jackson(final ObjectMapper mapper) {
    this.mapper = Optional.of(requireNonNull(mapper, "The mapper is required."));
  }

  /**
   * Creates a new {@link Jackson} module.
   */
  public Jackson() {
    this.mapper = Optional.empty();
  }

  /**
   * Set the json type supported by this module, default is: <code>application/json</code>.
   *
   * @param type Media type.
   * @return This module.
   */
  public Jackson type(final MediaType type) {
    this.type = type;
    return this;
  }

  /**
   * Set the json type supported by this module, default is: <code>application/json</code>.
   *
   * @param type Media type.
   * @return This module.
   */
  public Jackson type(final String type) {
    return type(MediaType.valueOf(type));
  }

  /**
   * Apply advanced configuration over the provided {@link ObjectMapper}.
   *
   * @param configurer A configurer callback.
   * @return This module.
   */
  public Jackson doWith(final Consumer<ObjectMapper> configurer) {
    this.configurer = requireNonNull(configurer, "ObjectMapper configurer is required.");
    return this;
  }

  /**
   * Register the provided module.
   *
   * @param module A module instance.
   * @return This module.
   */
  public Jackson module(final Module module) {
    requireNonNull(module, "Jackson Module is required.");
    modules.add(binder -> binder.addBinding().toInstance(module));
    return this;
  }

  /**
   * Register the provided {@link Module}. The module will be instantiated by Guice.
   *
   * @param module Module type.
   * @return This module.
   */
  public Jackson module(final Class<? extends Module> module) {
    requireNonNull(module, "Jackson Module is required.");
    modules.add(binder -> binder.addBinding().to(module));
    return this;
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    // provided or default mapper.
    ObjectMapper mapper = this.mapper.orElseGet(() -> {
      ObjectMapper m = new ObjectMapper();
      Locale locale = env.locale();
      // Jackson clone the date format in order to make dateFormat thread-safe
      m.setDateFormat(new SimpleDateFormat(config.getString("application.dateFormat"), locale));
      m.setLocale(locale);
      m.setTimeZone(TimeZone.getTimeZone(config.getString("application.tz")));
      // default modules:
      m.registerModule(new Jdk8Module());
      m.registerModule(new JavaTimeModule());
      m.registerModule(new ParameterNamesModule());
      return m;
    });

    if (configurer != null) {
      configurer.accept(mapper);
    }

    // bind mapper
    binder.bind(ObjectMapper.class).toInstance(mapper);

    // Jackson Modules from Guice
    Multibinder<Module> mbinder = Multibinder.newSetBinder(binder, Module.class);
    modules.forEach(m -> m.accept(mbinder));

    // Jackson Configurer (like a post construct)
    binder.bind(PostConfigurer.class).asEagerSingleton();

    // json parser & renderer
    JacksonParser parser = new JacksonParser(mapper, type);
    JacksonRenderer renderer = new JacksonRenderer(mapper, type);

    Multibinder.newSetBinder(binder, Renderer.class)
        .addBinding()
        .toInstance(renderer);

    Multibinder.newSetBinder(binder, Parser.class)
        .addBinding()
        .toInstance(parser);

    // direct access?
    binder.bind(Key.get(Renderer.class, Names.named(renderer.toString()))).toInstance(renderer);
    binder.bind(Key.get(Parser.class, Names.named(parser.toString()))).toInstance(parser);

  }

}
