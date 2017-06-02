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
package org.jooby.raml;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Results;
import org.jooby.Route;
import org.jooby.internal.raml.RamlBuilder;
import org.jooby.spec.RouteProcessor;
import org.jooby.spec.RouteSpec;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.io.ByteStreams;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

/**
 * <h1>raml</h1>
 * <p>
 * RESTful API Modeling Language (RAML) makes it easy to manage the whole API lifecycle from design
 * to sharing. It's concise - you only write what you need to define - and reusable. It is machine
 * readable API design that is actually human friendly. More at
 * <a href="http://raml.org/">http://raml.org</a>
 * </p>
 *
 * <p>
 * <strong>NOTE:</strong> This modules depends on {@link RouteSpec}, please read the
 * {@link RouteSpec} to learn how to use this tool.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   // define your API... via script or MVC:
 *   /{@literal *}{@literal *}
 *     {@literal *} Everything about your pets
 *     {@literal *}/
 *    use("/api/pets")
 *       /{@literal *}{@literal *}
 *        {@literal *} Get a pet by ID.
 *        {@literal *} &#64;param id Pet ID
 *        {@literal *}/
 *      .get("/:id", req {@literal ->} {
 *        int id = req.param("id").intValue();
 *        DB db = req.require(DB.class);
 *        Pet pet = db.find(Pet.class, id);
 *        return pet;
 *      })
 *      ...;
 *
 *   new Raml().install(this);
 * }
 * </pre>
 *
 * <p>
 * You also need the <code>jooby:spec</code> maven plugin:
 * </p>
 *
 * <pre>
 * &lt;plugin&gt;
 *   &lt;groupId&gt;org.jooby&lt;/groupId&gt;
 *   &lt;artifactId&gt;jooby-maven-plugin&lt;/artifactId&gt;
 *   &lt;executions&gt;
 *     &lt;execution&gt;
 *       &lt;goals&gt;
 *         &lt;goal&gt;spec&lt;/goal&gt;
 *       &lt;/goals&gt;
 *     &lt;/execution&gt;
 *   &lt;/executions&gt;
 * &lt;/plugin&gt;
 * </pre>
 *
 * <p>
 * The plugin compiles the API and produces a <code>.spec</code> file for prod environments.
 * </p>
 *
 * <p>
 * The RAML api-console will be available at <code>/raml</code> and the <code>.raml</code> will be
 * at: <code>/raml/api.raml</code>
 * </p>
 *
 * <h2>options</h2>
 * <p>
 * There are a few options available, let's see what they are:
 * </p>
 *
 * <h3>path</h3>
 * <p>
 * The <code>path</code> option controls where to mount the RAML routes:
 * </p>
 * <pre>
 * {
 *   ...
 *   new Raml("docs").install(this);
 * }
 * </pre>
 *
 * <p>
 * Produces: <code>/docs</code> for api-console and <code>/docs/api.raml</code>. Default path is:
 * <code>/raml</code>.
 * </p>
 *
 * <h3>filter</h3>
 * <p>
 * The <code>filter</code> option controls what is exported to <a href="http://raml.org">RAML</a>:
 * </p>
 *
 * <pre>
 * {
 *   ...
 *   new Raml()
 *     .filter(route {@literal ->} {
 *       return route.pattern().startsWith("/api");
 *     })
 *     .install(this);
 * }
 * </pre>
 *
 * <p>
 * Default filter keeps <code>/api/*</code> routes.
 * </p>
 *
 * <h3>noConsole</h3>
 * <p>
 * This option turn off the api-console:
 * </p>
 *
 * <pre>
 * {
 *   ...
 *   new Raml()
 *     .noConsole()
 *     .install(this);
 * }
 * </pre>
 *
 * <h3>theme</h3>
 * <p>
 * Set the ui-theme for api-console. Available options are <code>light</code> and <code>dark</code>.
 * Default is: <code>light</code>.
 * </p>
 *
 * <pre>
 * {
 *   ...
 *   new Raml()
 *     .theme("dark")
 *     .install(this);
 * }
 * </pre>
 *
 * <h3>clientGenerator</h3>
 * <p>
 * Shows/hide the client generator button from api-console.
 * </p>
 *
 * <h3>tryIt</h3>
 * <p>
 * Expand/collapse the try it panel from api-console.
 * </p>
 *
 * @author edgar
 * @since 0.15.0
 */
public class Raml {

  private static final String DISABLE_TRY_IT = "disable-try-it";

  private static final String DISABLE_RAML_CLIENT_GENERATOR = "disable-raml-client-generator";

  private static final String DISABLE_THEME_SWITCHER = "disable-theme-switcher";

  private static final String RAML = "/api.raml";

  private static final TypeLiteral<Set<Route.Definition>> ROUTES = new TypeLiteral<Set<Route.Definition>>() {
  };

  private String path;

  private Predicate<RouteSpec> filter;

  private String template;

  private Set<String> options = Sets.newHashSet(
      DISABLE_THEME_SWITCHER,
      DISABLE_RAML_CLIENT_GENERATOR);

  private boolean console = true;

  private String theme = "light";

  /**
   * Creates a new {@link Raml}.
   *
   * @param path A raml path.
   */
  public Raml(final String path) {
    this.path = path;
    filter = r -> r.pattern().startsWith("/api") && !r.method().equals("*");
    this.template = read("index.html");
  }

  /**
   * Creates a new {@link Raml} under the <code>/raml</code> path.
   */
  public Raml() {
    this("/raml");
  }

  /**
   * Apply a route filter. By default only routes at <code>/api</code> will be exported.
   *
   * @param filter A route filter.
   * @return This instance.
   */
  public Raml filter(final Predicate<RouteSpec> filter) {
    this.filter = requireNonNull(filter, "Filter is required.");
    return this;
  }

  /**
   * Set a ui-theme for api-console.
   *
   * @param theme Dark or light. Default is light.
   * @return This instance.
   */
  public Raml theme(final String theme) {
    this.theme = requireNonNull(theme, "Theme is required.");
    return this;
  }

  /**
   * Shows/hide client generator button for api-console.
   *
   * @param enabled True shows the button.
   * @return This instance.
   */
  public Raml clientGenerator(final boolean enabled) {
    if (enabled) {
      options.remove(DISABLE_RAML_CLIENT_GENERATOR);
    } else {
      options.add(DISABLE_RAML_CLIENT_GENERATOR);
    }
    return this;
  }

  /**
   * Expand/collapse the try-it panel for api-console.
   *
   * @param enabled True expands the panel.
   * @return This instance.
   */
  public Raml tryIt(final boolean enabled) {
    if (enabled) {
      options.remove(DISABLE_TRY_IT);
    } else {
      options.add(DISABLE_TRY_IT);
    }
    return this;
  }

  /**
   * Turn off api-console.
   *
   * @return This instance.
   */
  public Raml noConsole() {
    this.console = false;
    return this;
  }

  /**
   * Install {@link Raml} in the given app.
   *
   * @param app An application.
   */
  public void install(final Jooby app) {
    app.use(conf());
    app.get(path + RAML, path + "/:tag" + RAML, req -> {
      Set<Route.Definition> routes = req.require(ROUTES);

      Predicate<RouteSpec> predicate = req.param("tag").toOptional().map(t -> {
        return this.filter.and(r -> r.pattern().contains("/" + t));
      }).orElse(this.filter);

      RouteProcessor processor = new RouteProcessor();
      List<RouteSpec> specs = processor.process(app.getClass(), Lists.newArrayList(routes))
          .stream()
          .filter(predicate)
          .collect(Collectors.toList());

      RamlBuilder raml = req.require(RamlBuilder.class);
      return Results.ok(raml.build(specs)).type(MediaType.text);
    }).name("raml")
        .produces(MediaType.text);

    // console
    if (console) {
      app.assets(path + "/static/**", "/org/jooby/raml/dist/{0}").name("api-console/static");
      app.get(path, path + "/:tag", req -> {
        Config conf = req.require(Config.class);
        String name = conf.getString("raml.title");
        String raml = req.param("tag").toOptional()
            .map(t -> path + "/" + t + RAML)
            .orElse(path + RAML);

        String html = template
            .replace("${name}", name)
            .replace("${theme}", theme)
            .replace("${path}", path)
            .replace("${raml}", "." + raml)
            .replace("${opts}", options.stream().collect(Collectors.joining(" ")));
        return Results.ok(html).type(MediaType.html);
      }).name("api-console")
          .produces(MediaType.html);
    }
  }

  private String read(final String name) {
    try (InputStream stream = getClass().getResourceAsStream(name)) {
      return new String(ByteStreams.toByteArray(stream), "UTF-8");
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }

  private static Jooby.Module conf() {
    return new Jooby.Module() {
      @Override
      public void configure(final Env env, final Config conf, final Binder binder) {
      }

      @Override
      public Config config() {
        return ConfigFactory.parseResources(Raml.class, "raml.conf");
      }
    };
  }
}
