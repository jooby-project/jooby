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
package org.jooby.swagger;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Results;
import org.jooby.internal.swagger.SwaggerBuilder;
import org.jooby.internal.swagger.SwaggerHandler;
import org.jooby.internal.swagger.SwaggerModule;
import org.jooby.internal.swagger.SwaggerYml;
import org.jooby.spec.RouteSpec;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.google.common.base.Splitter;

import io.swagger.models.Swagger;
import io.swagger.util.Json;
import io.swagger.util.Yaml;

/**
 * <h1>swagger module</h1>
 *
 * <p>
 * Generates a <a href="http://swagger.io">Swagger spec</a> from an application.
 * </p>
 *
 * <p>
 * <strong>NOTE:</strong> This modules depends on {@link RouteSpec}, please read the
 * {@link RouteSpec} to learn how to use this tool.
 * </p>
 *
 * <h2>exposes</h2>
 * <ul>
 * <li>A <code>/swagger</code> route that will render a Swagger UI</li>
 * <li>A <code>/swagger.json</code> route that will render a Swagger Spec in json format</li>
 * <li>A <code>/swagger.yml</code> route that will render a Swagger Spec in yaml format</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * <p>
 * via Script API:
 * </p>
 *
 * <pre>
 * {
 *    /{@literal *}{@literal *}
 *     {@literal *}Everything about your pets
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
 * }
 * </pre>
 *
 * <p>
 * via MVC API:
 * </p>
 * <pre>
 * &#64;Path("/api/pets")
 * public class Pets {
 *
 *   &#64;Path("/:id")
 *   &#64;GET
 *   public Pet get(String id) {...}
 *
 *   &#64;POST
 *   public Pet post(Pet pet) {...}
 * }
 *
 * {
 *   SwaggerUI.install(this);
 *
 *   // Swagger will generate a swagger spec for the Pets MVC routes.
 *   use(Pets.class);
 * }
 * </pre>
 *
 * <p>
 * By default, Swagger will be mounted at <code>/swagger</code>, <code>/swagger/swagger.json</code>
 * and <code>/swagger/swagger.yml</code>. Go and try it!
 * </p>
 *
 * <p>
 * Or if you want to mount Swagger somewhere else...:
 * </p>
 *
 * <pre>
 * {
 *   SwaggerUI.install("/api/docs", this));
 * }
 * </pre>
 *
 * <p>
 * It is also possible to use Swagger (ui, .json or .yml) on specific resources. For example,
 * suppose we have a <code>Pets.java</code> resource mounted at <code>/pets</code>. The following
 * URL will be available too:
 * </p>
 *
 * <pre>
 *    /swagger/pets (UI)
 *    /swagger/pets/swagger.json (JSON)
 *    /swagger/pets/swagger.yml (YML)
 * </pre>
 *
 * <p>
 * It is a small feature, but very useful if you have a medium-size API.
 * </p>
 *
 * <h2>swagger.conf</h2>
 * <p>
 * Jooby creates a {@link Swagger} model dynamically from MVC {@link RouteSpec}. But also,
 * defines some defaults inside the <code>swagger.conf</code> (see appendix).
 * </p>
 *
 * <p>
 * For example, <code>swagger.info.title</code> is set to <code>application.name</code>. If you want
 * to provide a more friendly name, description or API version... you can do it via your
 * <code>application.conf</code> file:
 * </p>
 *
 * <pre>
 *
 * swagger.info.title = My Awesome API
 * swagger.info.version = v0.1.0
 *
 * </pre>
 *
 * @author edgar
 * @since 0.6.2
 */
public class SwaggerUI {

  private static final Pattern TAG = Pattern.compile("(api)|/");

  private Predicate<RouteSpec> predicate = r -> r.pattern().startsWith("/api");

  private Function<RouteSpec, String> tag = r -> {
    Iterator<String> segments = Splitter.on(TAG)
        .trimResults()
        .omitEmptyStrings()
        .split(r.pattern())
        .iterator();
    return segments.next();
  };

  private String path;

  /**
   * Mount swagger at the given path.
   *
   * @param path A swagger path.
   */
  public SwaggerUI(final String path) {
    this.path = requireNonNull(path, "Path is required.");
  }

  /**
   * Mount swagger at <code>/swagger</code>.
   */
  public SwaggerUI() {
    this("/swagger");
  }

  /**
   * Apply a route filter. By default, it keeps all the routes that starts with <code>/api</code>.
   *
   * @param predicate A filter to apply.
   * @return This instance.
   */
  public SwaggerUI filter(final Predicate<RouteSpec> predicate) {
    this.predicate = requireNonNull(predicate, "Predicate is required.");
    return this;
  }

  /**
   * Creates a swagger tag from a route. The default tag provider extracts takes the first path
   * (ignoring <code>api</code>) from route pattern. For example, tag for <code>/api/pets</code>
   * will be <code>pets</code>.
   *
   * @param tag A tag provider.
   * @return This instance.
   */
  public SwaggerUI tag(final Function<RouteSpec, String> tag) {
    this.tag = requireNonNull(tag, "Tag provider is required.");
    return this;
  }

  /**
   * Publish application routes as Swagger spec.
   *
   * @param app An application.
   */
  public void install(final Jooby app) {
    requireNonNull(app, "Application is required.");
    ObjectMapper mapper = Json.create();
    ObjectWriter yaml = Yaml.pretty();

    app.use(new SwaggerModule(mapper));

    app.assets(path + "/ui/**",
        "/META-INF/resources/webjars/swagger-ui/" + wjversion(app.getClass()) + "/{0}");

    app.get(path + "/swagger.json", path + "/:tag/swagger.json", req -> {
      SwaggerBuilder sb = req.require(SwaggerBuilder.class);
      Swagger swagger = sb.build(req.param("tag").toOptional(), predicate, tag, Swagger.class);
      byte[] json = mapper.writer().withDefaultPrettyPrinter().writeValueAsBytes(swagger);
      return Results.json(json);
    }).name("swagger(json)");

    app.get(path + "/swagger.yml", path + "/:tag/swagger.yml", req -> {
      SwaggerBuilder sb = req.require(SwaggerBuilder.class);
      Swagger swagger = sb.build(req.param("tag").toOptional(), predicate, tag, SwaggerYml.class);
      byte[] yml = yaml.writeValueAsBytes(swagger);
      return Results.ok(yml).type("application/yaml");

    }).name("swagger(yml)");

    app.get(path, path + "/:tag", new SwaggerHandler(path))
        .name("swagger(html)")
        .produces(MediaType.html);
  }

  private static String wjversion(final Class<?> loader) {
    try (InputStream in = loader.getResourceAsStream(
        "/META-INF/maven/org.webjars/swagger-ui/pom.properties")) {
      Properties properties = new Properties();
      properties.load(in);
      return properties.getProperty("version");
    } catch (IOException ex) {
      throw new IllegalStateException("No version found for swagger-ui", ex);
    }
  }

}
