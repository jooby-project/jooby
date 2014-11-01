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

import javax.inject.Inject;

import org.jooby.Body;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Mode;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

public class Json extends Jooby.Module {

  static class Configurer {

    @Inject
    public Configurer(final ObjectMapper mapper, final Set<Module> jacksonModules) {
      mapper.registerModules(jacksonModules);
    }

  }

  static class BodyHandler implements Body.Formatter, Body.Parser {

    private ObjectMapper mapper;
    private List<MediaType> types;

    public BodyHandler(final ObjectMapper mapper, final List<MediaType> types) {
      this.mapper = checkNotNull(mapper, "An object mapper is required.");
      this.types = requireNonNull(types, "The types is required.");
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

  public Json with(final MediaType... types) {
    this.types = ImmutableList.copyOf(types);
    return this;
  }

  public ObjectMapper mapper() {
    return mapper;
  }

  @Override
  public void configure(final Mode mode, final Config config, final Binder binder) {
    Locale locale = Locale.forLanguageTag(config.getString("application.lang").replace("_", "-"));
    // Jackson clone the date format in order to make dateFormat thread-safe
    mapper.setDateFormat(new SimpleDateFormat(config.getString("application.dateFormat"), locale));
    mapper.setLocale(locale);

    // Jackson Modules from Guice
    Multibinder<Module> moduleBinder = Multibinder.newSetBinder(binder, Module.class);
    modules.forEach(m -> moduleBinder.addBinding().toInstance(m));

    binder.bind(ObjectMapper.class).toInstance(mapper);

    // Jackson Configurer (like a post construct)
    binder.bind(Configurer.class).asEagerSingleton();

    // json body parser & formatter
    BodyHandler json = new BodyHandler(mapper, types);
    Multibinder.newSetBinder(binder, Body.Formatter.class)
        .addBinding()
        .toInstance(json);

    Multibinder.newSetBinder(binder, Body.Parser.class)
        .addBinding()
        .toInstance(json);
  }

}
