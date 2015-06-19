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

import io.swagger.models.Swagger;
import io.swagger.util.Json;
import io.swagger.util.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;

import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Route;
import org.jooby.internal.swagger.SwaggerBuilder;
import org.jooby.internal.swagger.SwaggerHandler;
import org.jooby.internal.swagger.SwaggerModule;
import org.jooby.internal.swagger.SwaggerYml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

/**
 * <h1>swagger module</h1>
 *
 * <p>
 * Generate a Swagger Spec from MVC routes.
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
 *   use(new SwaggerUI());
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
 *   use(new SwaggerUI("/api/docs"));
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
 * Jooby creates a {@link Swagger} model dynamically from MVC {@link Route.Definition}. But also,
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
 * <h2>limitations</h2>
 * <p>
 * Sadly, ONLY MVC routes are supported. Inline/lambda routes has no supports for now.
 * </p>
 *
 * @author edgar
 * @since 0.6.2
 */
public class SwaggerUI extends Jooby {

  public SwaggerUI(final String path) {

    ObjectMapper mapper = Json.create();
    ObjectWriter yaml = Yaml.pretty();

    use(new SwaggerModule(mapper));

    renderer((value, ctx) -> {
      if (value instanceof SwaggerYml) {
        ctx.type("application/yaml")
            .send(yaml.writeValueAsBytes(value));
      } else if (value instanceof Swagger) {
        ctx.type(MediaType.json)
            .send(mapper.writer().withDefaultPrettyPrinter().writeValueAsBytes(value));
      }
    });

    assets(path + "/ui/**", "/META-INF/resources/webjars/swagger-ui/" + wjversion() + "/{0}");

    get(path + "/swagger.json",
        path + "/:tag/swagger.json",
        req -> req.require(SwaggerBuilder.class)
            .build(filter(req.param("tag").toOptional()), Swagger.class))
        .name("swagger(json)");

    get(path + "/swagger.yml", path + "/:tag/swagger.yml",
        req -> req.require(SwaggerBuilder.class)
            .build(filter(req.param("tag").toOptional()), SwaggerYml.class))
        .name("swagger(yml)");

    get(path, path + "/:tag", new SwaggerHandler(path))
        .name("swagger(html)")
        .produces(MediaType.html);

  }

  private String wjversion() {
    try (InputStream in = getClass().getResourceAsStream(
        "/META-INF/maven/org.webjars/swagger-ui/pom.properties")) {
      Properties properties = new Properties();
      properties.load(in);
      return properties.getProperty("version");
    } catch (IOException ex) {
      throw new IllegalStateException("No version found for swagger-ui", ex);
    }
  }

  private Predicate<String> filter(final Optional<String> tag) {
    return tag.map(this::matches).orElseGet(this::any);
  }

  private Predicate<String> matches(final String tag) {
    return it -> tag.equals(it);
  }

  private Predicate<String> any() {
    return it -> true;
  }

  public SwaggerUI() {
    this("/swagger");
  }

}
