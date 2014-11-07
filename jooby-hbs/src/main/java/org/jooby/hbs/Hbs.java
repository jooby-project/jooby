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
package org.jooby.hbs;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.util.Map.Entry;
import java.util.Set;

import org.jooby.Body;
import org.jooby.Jooby;
import org.jooby.Mode;
import org.jooby.View;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Helper;
import com.github.jknack.handlebars.HelperRegistry;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.GuavaTemplateCache;
import com.github.jknack.handlebars.cache.NullTemplateCache;
import com.github.jknack.handlebars.cache.TemplateCache;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Hbs implements HelperRegistry, Jooby.Module {

  private static class Engine implements View.Engine {

    private Handlebars handlebars;

    public Engine(final Handlebars handlebars) {
      this.handlebars = requireNonNull(handlebars, "A handlebars instance required.");
    }

    @Override
    public String name() {
      return "hbs";
    }

    @Override
    public void render(final View view, final Body.Writer writer) throws Exception {
      Template template = handlebars.compile(view.name());

      final Context context;
      Object model = view.model();
      if (model instanceof Context) {
        context = (Context) model;
      } else {
        // build new context
        context = Context
            .newBuilder(model)
            .resolver(MapValueResolver.INSTANCE, JavaBeanValueResolver.INSTANCE,
                FieldValueResolver.INSTANCE, MethodValueResolver.INSTANCE)
            .build();
      }

      // rendering it
      writer.text(out -> template.apply(context, out));
    }

    @Override
    public String toString() {
      return name();
    }
  }

  private static final TemplateCache NOOP = NullTemplateCache.INSTANCE;

  private final Handlebars hbs;

  public Hbs(final Handlebars handlebars) {
    this.hbs = requireNonNull(handlebars, "A handlebars instance is required.");
  }

  public Hbs() {
    this(new Handlebars(new ClassPathTemplateLoader("/", ".html")));
  }

  @Override
  public void configure(final Mode mode, final Config config, final Binder binder)
      throws Exception {

    TemplateCache cache = mode
        .ifMode("dev", () -> NOOP)
        .orElseGet(() ->
            new GuavaTemplateCache(CacheBuilder
                .from(config.getString("hbs.cache"))
                .build())
        );
    hbs.with(cache);

    binder.bind(Handlebars.class).toInstance(hbs);

    Multibinder.newSetBinder(binder, Body.Formatter.class).addBinding()
        .toInstance(new Engine(hbs));

  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "hbs.conf");
  }

  @Override
  public <C> Helper<C> helper(final String name) {
    return hbs.helper(name);
  }

  @Override
  public Set<Entry<String, Helper<?>>> helpers() {
    return hbs.helpers();
  }

  @Override
  public <H> Hbs registerHelper(final String name, final Helper<H> helper) {
    hbs.registerHelper(name, helper);
    return this;
  }

  @Override
  public <H> HelperRegistry registerHelperMissing(final Helper<H> helper) {
    hbs.registerHelperMissing(helper);
    return this;
  }

  @Override
  public HelperRegistry registerHelpers(final Object helperSource) {
    hbs.registerHelpers(helperSource);
    return this;
  }

  @Override
  public HelperRegistry registerHelpers(final Class<?> helperSource) {
    hbs.registerHelpers(helperSource);
    return this;
  }

  @Override
  public HelperRegistry registerHelpers(final URI location) throws Exception {
    hbs.registerHelpers(location);
    return this;
  }

  @Override
  public HelperRegistry registerHelpers(final File input) throws Exception {
    hbs.registerHelpers(input);
    return this;
  }

  @Override
  public HelperRegistry registerHelpers(final String filename, final Reader source)
      throws Exception {
    hbs.registerHelpers(filename, source);
    return this;
  }

  @Override
  public HelperRegistry registerHelpers(final String filename, final InputStream source)
      throws Exception {
    hbs.registerHelpers(filename, source);
    return this;
  }

  @Override
  public HelperRegistry registerHelpers(final String filename, final String source)
      throws Exception {
    hbs.registerHelpers(filename, source);
    return this;
  }
}
