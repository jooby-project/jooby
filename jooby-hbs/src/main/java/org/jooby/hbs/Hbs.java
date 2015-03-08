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

import java.util.function.Consumer;

import org.jooby.Body;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.View;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.cache.GuavaTemplateCache;
import com.github.jknack.handlebars.cache.NullTemplateCache;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class Hbs implements Jooby.Module {

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
            // merge request locals (req+sessions locals)
            .combine(writer.locals())
            .resolver(
                MapValueResolver.INSTANCE,
                JavaBeanValueResolver.INSTANCE,
                MethodValueResolver.INSTANCE,
                FieldValueResolver.INSTANCE,
                new LocalsValueResolver(),
                new ConfigValueResolver()
            )
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

  private final Handlebars hbs;

  public Hbs(final Handlebars handlebars) {
    this.hbs = requireNonNull(handlebars, "A handlebars instance is required.");
  }

  public Hbs() {
    this(new Handlebars(new ClassPathTemplateLoader("/", ".html")));
  }

  public Hbs doWith(final Consumer<Handlebars> block) {
    requireNonNull(block, "A hbs block is required.").accept(hbs);
    return this;
  };

  @Override
  public void configure(final Env mode, final Config config, final Binder binder) {

    // cache
    if ("dev".equals(mode.name()) || config.getString("hbs.cache").isEmpty()) {
      // noop cache
      hbs.with(NullTemplateCache.INSTANCE);
    } else {
      hbs.with(new GuavaTemplateCache(
          CacheBuilder
              .from(config.getString("hbs.cache"))
              .build()
          ));
    }

    binder.bind(Handlebars.class).toInstance(hbs);
    Engine engine = new Engine(hbs);

    Multibinder.newSetBinder(binder, Body.Formatter.class).addBinding()
        .toInstance(engine);

    // direct accessf
    binder.bind(Key.get(View.Engine.class, Names.named(engine.name()))).toInstance(engine);
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "hbs.conf");
  }

}
