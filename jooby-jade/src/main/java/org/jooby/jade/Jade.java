/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.jade;

import static java.util.Objects.requireNonNull;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Renderer;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;

import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.ClasspathTemplateLoader;
import de.neuland.jade4j.template.TemplateLoader;

/**
 * <h1>jade</h1>
 * <p>
 * <a href="https://github.com/neuland/jade4j">jade4j's</a> intention is to be able to process jade
 * templates in Java without the need of a JavaScript environment, while being fully compatible with
 * the original jade syntax.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   use(new Jade());
 *
 *   get("/", req {@literal ->} Results.html("index").put("model", new MyModel());
 *
 *   // or via API
 *   get("/jade-api", req {@literal ->} {
 *     JadeConfiguration jade = req.require(JadeConfiguration.class);
 *     JadeTemplate template = jade.getTemplate("index");
 *     template.renderTemplate(...);
 *   });
 * }
 * </pre>
 *
 * <p>
 * Templates are loaded from root of classpath: <code>/</code> and must ends with:
 * <code>.jade</code>
 * file extension.
 * </p>
 *
 * <h2>req locals</h2>
 *
 * <p>
 * A template engine has access to ```request locals``` (a.k.a attributes). Here is an example:
 * </p>
 * <pre>
 * {
 *   use(new Jade());
 *
 *   get("*", req {@literal ->} {
 *     req.set("req", req);
 *     req.set("session", req.session());
 *   });
 * }
 * </pre>
 *
 * <p>
 * By default, there is no access to ```req``` or ```session``` from your template. This example
 * shows how to do it.
 * </p>
 *
 * <h2>template loader</h2>
 * <p>
 * Templates are loaded from the root of classpath and must ends with <code>.jade</code>. You can
 * change the extensions too:
 * </p>
 *
 * <pre>
 * {
 *   use(new Jade(".html"));
 * }
 * </pre>
 *
 * <p>
 * Keep in mind if you change it file name must ends with: <code>.html.jade</code>.
 * </p>
 *
 * <h2>template cache</h2>
 * <p>
 * Cache is OFF when <code>application.env = dev</code> (useful for template reloading), otherwise
 * is ON and does not expire, unless you explicitly set <code>jade.caching</code>.
 * </p>
 *
 * <h2>pretty print</h2>
 * <p>
 * Pretty print is on when <code>application.env = dev </code>, otherwise is off, unless unless you
 * explicitly set <code>jade.prettyprint</code>.
 * </p>
 *
 * <p>
 * That's all folks! Enjoy it!!!
 * </p>
 *
 * @since 1.0.0
 */
public class Jade implements Jooby.Module {

  static class IOTemplateLoader implements TemplateLoader {

    private TemplateLoader loader;

    public IOTemplateLoader(final TemplateLoader loader) {
      this.loader = loader;
    }

    @Override
    public long getLastModified(final String name) throws IOException {
      return loader.getLastModified(name);
    }

    @Override
    public Reader getReader(final String name) throws IOException {
      try {
        return loader.getReader(name);
      } catch (NullPointerException ex) {
        throw new FileNotFoundException(name);
      }
    }

  }

  private BiConsumer<JadeConfiguration, Config> callback;

  private String suffix;

  /**
   * Creates a {@link Jade} instance with a custom suffix.
   *
   * @param suffix A suffix like <code>.html</code>. But keep in mind the final extension will be
   *        <code>.html.jade</code>
   */
  public Jade(final String suffix) {
    this.suffix = requireNonNull(suffix, "Suffix is required.");
  }

  /**
   * Creates a {@link Jade} instance with default suffix <code>.jade</code>.
   */
  public Jade() {
    this(".jade");
  }

  /**
   * Configure callback that let you tweak or modified a {@link JadeConfiguration}.
   *
   * @param callback A callback.
   * @return This module.
   */
  public Jade doWith(final Consumer<JadeConfiguration> callback) {
    requireNonNull(callback, "Configurer callback is required.");
    return doWith((j, c) -> callback.accept(j));
  }

  /**
   * Configure callback that let you tweak or modified a {@link JadeConfiguration}.
   *
   * @param callback A callback.
   * @return This module.
   */
  public Jade doWith(final BiConsumer<JadeConfiguration, Config> callback) {
    this.callback = requireNonNull(callback, "Configurer callback is required.");
    return this;
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    JadeConfiguration jadeconf = new JadeConfiguration();
    boolean dev = env.name().equals("dev");
    boolean caching = conf.hasPath("jade.caching")
        ? conf.getBoolean("jade.caching")
        : !dev;
    boolean prettyPrint = conf.hasPath("jade.prettyprint")
        ? conf.getBoolean("jade.prettyprint")
        : dev;

    jadeconf.setCaching(caching);
    jadeconf.setPrettyPrint(prettyPrint);

    Map<String, Object> sharedVariables = new HashMap<>(2);
    sharedVariables.put("env", env);
    sharedVariables.put("xss", new XssHelper(env));
    jadeconf.setSharedVariables(sharedVariables);

    jadeconf.setTemplateLoader(new ClasspathTemplateLoader());

    if (callback != null) {
      callback.accept(jadeconf, conf);
    }

    // rewrite template loader avoid NPE
    jadeconf.setTemplateLoader(new IOTemplateLoader(jadeconf.getTemplateLoader()));

    binder.bind(JadeConfiguration.class)
        .toInstance(jadeconf);

    Multibinder.newSetBinder(binder, Renderer.class).addBinding()
        .toInstance(new Engine(jadeconf, suffix));
  }

}
