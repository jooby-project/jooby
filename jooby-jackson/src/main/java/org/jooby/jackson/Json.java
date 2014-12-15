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
package org.jooby.jackson;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import java.text.SimpleDateFormat;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.jooby.Body;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.MediaType;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

public class Json implements Jooby.Module {

  private static class PostConfigurer {

    @Inject
    public PostConfigurer(final ObjectMapper mapper, final Set<Module> jacksonModules) {
      mapper.registerModules(jacksonModules);
    }

  }

  private static class BodyHandler implements Body.Formatter, Body.Parser {

    private ObjectMapper mapper;
    private List<MediaType> types;

    public BodyHandler(final ObjectMapper mapper, final List<MediaType> types) {
      this.mapper = mapper;
      this.types = types;
    }

    @Override
    public List<MediaType> types() {
      return types;
    }

    @Override
    public boolean canParse(final TypeLiteral<?> type) {
      return mapper.canDeserialize(mapper.constructType(type.getType()));
    }

    @Override
    public boolean canFormat(final Class<?> type) {
      return mapper.canSerialize(type);
    }

    @Override
    public <T> T parse(final TypeLiteral<T> type, final Body.Reader reader) throws Exception {
      return reader.text(in -> mapper.readValue(in, mapper.constructType(type.getType())));
    }

    @Override
    public void format(final Object body, final Body.Writer writer) throws Exception {
      writer.text(out -> mapper.writeValue(out, body));
    }

    @Override
    public String toString() {
      return "json";
    }

  }

  private final ObjectMapper mapper;

  private final Set<Module> modules = new LinkedHashSet<>();

  private List<MediaType> types = ImmutableList.of(MediaType.json);

  public Json(final ObjectMapper mapper) {
    this.mapper = checkNotNull(mapper, "An object mapper is required.");
    this.modules.add(new Jdk8Module());
    // Java 8 dates
    this.modules.add(new JSR310Module());
  }

  public Json() {
    this(new ObjectMapper());
  }

  public Json types(final MediaType... types) {
    return types(ImmutableList.copyOf(types));
  }

  public Json types(final List<MediaType> types) {
    this.types = ImmutableList.copyOf(types);
    return this;
  }

  public Json types(final String... types) {
    return types(MediaType.valueOf(types));
  }

  public Json doWith(final Consumer<ObjectMapper> block) {
    requireNonNull(block, "A json block is required.").accept(mapper);
    return this;
  }

  @Override
  public void configure(final Env mode, final Config config, final Binder binder) {
    Locale locale = Locale.forLanguageTag(config.getString("application.lang").replace("_", "-"));
    // Jackson clone the date format in order to make dateFormat thread-safe
    mapper.setDateFormat(new SimpleDateFormat(config.getString("application.dateFormat"), locale));
    mapper.setLocale(locale);
    mapper.setTimeZone(TimeZone.getTimeZone(config.getString("application.tz")));

    // Jackson Modules from Guice
    Multibinder<Module> moduleBinder = Multibinder.newSetBinder(binder, Module.class);
    modules.forEach(m -> moduleBinder.addBinding().toInstance(m));

    binder.bind(ObjectMapper.class).toInstance(mapper);

    // Jackson Configurer (like a post construct)
    binder.bind(PostConfigurer.class).asEagerSingleton();

    // json body parser & formatter
    BodyHandler json = new BodyHandler(mapper, types);

    Multibinder.newSetBinder(binder, Body.Formatter.class)
        .addBinding()
        .toInstance(json);

    Multibinder.newSetBinder(binder, Body.Parser.class)
        .addBinding()
        .toInstance(json);

    // direct access?
    binder.bind(Key.get(Body.Formatter.class, Names.named(json.toString()))).toInstance(json);
    binder.bind(Key.get(Body.Parser.class, Names.named(json.toString()))).toInstance(json);
  }

}
