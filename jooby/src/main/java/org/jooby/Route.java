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
package org.jooby;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.jooby.internal.AssetProxy;
import org.jooby.internal.DeferredExecution;
import org.jooby.internal.RouteImpl;
import org.jooby.internal.RouteMatcher;
import org.jooby.internal.RoutePattern;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.primitives.Primitives;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;

/**
 * Routes are a key concept in Jooby. Routes are executed in the same order they are defined
 * (even for Mvc Routes).
 *
 * <h1>Handlers</h1>
 * <p>
 * There are two types of handlers: {@link Route.Handler} and {@link Route.Filter}. They behave very
 * similar, except that a {@link Route.Filter} can decide if the next route handler can be executed
 * or not. For example:
 * </p>
 *
 * <pre>
 *   get("/filter", (req, rsp, chain) {@literal ->} {
 *     if (someCondition) {
 *       chain.next(req, rsp);
 *     } else {
 *       // respond, throw err, etc...
 *     }
 *   });
 * </pre>
 *
 * While a {@link Route.Handler} always execute the next handler:
 *
 * <pre>
 *   get("/path", (req, rsp) {@literal ->} {
 *     rsp.send("handler");
 *   });
 *
 *   // filter version
 *   get("/path", (req, rsp, chain) {@literal ->} {
 *     rsp.send("handler");
 *     chain.next(req, rsp);
 *   });
 * </pre>
 *
 * <h1>Path Patterns</h1>
 * <p>
 * Jooby supports Ant-style path patterns:
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <ul>
 * <li>{@code com/t?st.html} - matches {@code com/test.html} but also {@code com/tast.jsp} or
 * {@code com/txst.html}</li>
 * <li>{@code com/*.html} - matches all {@code .html} files in the {@code com} directory</li>
 * <li><code>com/{@literal **}/test.html</code> - matches all {@code test.html} files underneath the
 * {@code com} path</li>
 * <li>{@code **}/{@code *} - matches any path at any level.</li>
 * <li>{@code *} - matches any path at any level, shorthand for {@code **}/{@code *}.</li>
 * </ul>
 *
 * <h2>Variables</h2>
 * <p>
 * Jooby supports path parameters too:
 * </p>
 * <p>
 * Some examples:
 * </p>
 * <ul>
 * <li><code> /user/{id}</code> - /user/* and give you access to the <code>id</code> var.</li>
 * <li><code> /user/:id</code> - /user/* and give you access to the <code>id</code> var.</li>
 * <li><code> /user/{id:\\d+}</code> - /user/[digits] and give you access to the numeric
 * <code>id</code> var.</li>
 * </ul>
 *
 * <h1>Routes</h1>
 * <p>
 * Routes are executed in the order they are defined, for example:
 * </p>
 *
 * <pre>
 *   get("/", (req, rsp) {@literal ->} {
 *     log.info("first"); // start here and go to second
 *   });
 *
 *   get("/", (req, rsp) {@literal ->} {
 *     log.info("second"); // execute after first and go to final
 *   });
 *
 *   get("/", (req, rsp) {@literal ->} {
 *     rsp.send("final"); // done!
 *   });
 * </pre>
 *
 * Please note first and second routes are converted to a filter, so previous example is the same
 * as:
 *
 * <pre>
 *   get("/", (req, rsp, chain) {@literal ->} {
 *     log.info("first"); // start here and go to second
 *     chain.next(req, rsp);
 *   });
 *
 *   get("/", (req, rsp, chain) {@literal ->} {
 *     log.info("second"); // execute after first and go to final
 *     chain.next(req, rsp);
 *   });
 *
 *   get("/", (req, rsp) {@literal ->} {
 *     rsp.send("final"); // done!
 *   });
 * </pre>
 *
 * <h2>Inline route</h2>
 * <p>
 * An inline route can be defined using Lambda expressions, like:
 * </p>
 *
 * <pre>
 *   get("/", (request, response) {@literal ->} {
 *     response.send("Hello Jooby");
 *   });
 * </pre>
 *
 * Due to the use of lambdas a route is a singleton and you should NOT use global variables.
 * For example this is a bad practice:
 *
 * <pre>
 *  List{@literal <}String{@literal >} names = new ArrayList{@literal <>}(); // names produces side effects
 *  get("/", (req, rsp) {@literal ->} {
 *     names.add(req.param("name").value();
 *     // response will be different between calls.
 *     rsp.send(names);
 *   });
 * </pre>
 *
 * <h2>Mvc Route</h2>
 * <p>
 * A Mvc Route use annotations to define routes:
 * </p>
 *
 * <pre>
 * {
 *   use(MyRoute.class);
 * }
 * </pre>
 *
 * MyRoute.java:
 * <pre>
 *   {@literal @}Path("/")
 *   public class MyRoute {
 *
 *    {@literal @}GET
 *    public String hello() {
 *      return "Hello Jooby";
 *    }
 *   }
 * </pre>
 * <p>
 * Programming model is quite similar to JAX-RS/Jersey with some minor differences and/or
 * simplifications.
 * </p>
 *
 * <p>
 * To learn more about Mvc Routes, please check {@link org.jooby.mvc.Path},
 * {@link org.jooby.mvc.Produces} {@link org.jooby.mvc.Consumes}.
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Route {

  /**
   * Common route properties, like static and global metadata via attributes, path exclusion,
   * produces and consumes types.
   *
   * @author edgar
   * @since 1.0.0.CR
   * @param <T> Attribute subtype.
   */
  interface Props<T extends Props<T>> {
    /**
     * Set route attribute. Only primitives, string, class, enum or array of previous types are
     * allowed as attributes values.
     *
     * @param name Attribute's name.
     * @param value Attribute's value.
     * @return This instance.
     */
    T attr(String name, Object value);

    /**
     * Tell jooby what renderer should use to render the output.
     *
     * @param name A renderer's name.
     * @return This instance.
     */
    default T renderer(final String name) {
      return attr(RENDERER, name);
    }

    /**
     * Set the route name. Route's name, helpful for debugging but also to implement dynamic and
     * advanced routing. See {@link Route.Chain#next(String, Request, Response)}
     *
     *
     * @param name A route's name.
     * @return This instance.
     */
    T name(final String name);

    /**
     * Set the media types the route can consume.
     *
     * @param consumes The media types to test for.
     * @return This instance.
     */
    default T consumes(final MediaType... consumes) {
      return consumes(Arrays.asList(consumes));
    }

    /**
     * Set the media types the route can consume.
     *
     * @param consumes The media types to test for.
     * @return This instance.
     */
    default T consumes(final String... consumes) {
      return consumes(MediaType.valueOf(consumes));
    }

    /**
     * Set the media types the route can consume.
     *
     * @param consumes The media types to test for.
     * @return This instance.
     */
    T consumes(final List<MediaType> consumes);

    /**
     * Set the media types the route can produces.
     *
     * @param produces The media types to test for.
     * @return This instance.
     */
    default T produces(final MediaType... produces) {
      return produces(Arrays.asList(produces));
    }

    /**
     * Set the media types the route can produces.
     *
     * @param produces The media types to test for.
     * @return This instance.
     */
    default T produces(final String... produces) {
      return produces(MediaType.valueOf(produces));
    }

    /**
     * Set the media types the route can produces.
     *
     * @param produces The media types to test for.
     * @return This instance.
     */
    T produces(final List<MediaType> produces);

    /**
     * Excludes one or more path pattern from this route, useful for filter:
     *
     * <pre>
     * {
     *   use("*", req {@literal ->} {
     *    ...
     *   }).excludes("/logout");
     * }
     * </pre>
     *
     * @param excludes A path pattern.
     * @return This instance.
     */
    default T excludes(final String... excludes) {
      return excludes(Arrays.asList(excludes));
    }

    /**
     * Excludes one or more path pattern from this route, useful for filter:
     *
     * <pre>
     * {
     *   use("*", req {@literal ->} {
     *    ...
     *   }).excludes("/logout");
     * }
     * </pre>
     *
     * @param excludes A path pattern.
     * @return This instance.
     */
    public T excludes(final List<String> excludes);

  }

  class Group implements Props<Group> {

    /** List of definitions. */
    private List<Route.Definition> routes = new ArrayList<>();

    private String rootPattern;

    private String prefix;

    public Group(final String pattern, final String prefix) {
      requireNonNull(pattern, "Pattern is required.");
      this.rootPattern = pattern;
      this.prefix = prefix;
    }

    public Group(final String pattern) {
      this(pattern, null);
    }

    public List<Route.Definition> routes() {
      return routes;
    }

    // ********************************************************************************************
    // ALL
    // ********************************************************************************************
    public Group all(final String pattern, final Route.Filter filter) {
      newRoute("*", pattern, filter);
      return this;
    }

    public Group all(final String pattern, final Route.Handler handler) {
      newRoute("*", pattern, handler);
      return this;
    }

    public Group all(final String pattern, final Route.OneArgHandler handler) {
      newRoute("*", pattern, handler);
      return this;
    }

    public Group all(final String pattern, final Route.ZeroArgHandler handler) {
      newRoute("*", pattern, handler);
      return this;
    }

    public Group all(final Route.Filter filter) {
      newRoute("*", "", filter);
      return this;
    }

    public Group all(final Route.Handler handler) {
      newRoute("*", "", handler);
      return this;
    }

    public Group all(final Route.OneArgHandler handler) {
      newRoute("*", "", handler);
      return this;
    }

    public Group all(final Route.ZeroArgHandler handler) {
      newRoute("*", "", handler);
      return this;
    }

    // ********************************************************************************************
    // GET
    // ********************************************************************************************
    public Group get(final String pattern, final Route.Filter filter) {
      newRoute("GET", pattern, filter);
      return this;
    }

    public Group get(final String pattern, final Route.Handler handler) {
      newRoute("GET", pattern, handler);
      return this;
    }

    public Group get(final String pattern, final Route.OneArgHandler handler) {
      newRoute("GET", pattern, handler);
      return this;
    }

    public Group get(final String pattern, final Route.ZeroArgHandler handler) {
      newRoute("GET", pattern, handler);
      return this;
    }

    public Group get(final Route.Filter filter) {
      newRoute("GET", "", filter);
      return this;
    }

    public Group get(final Route.Handler handler) {
      newRoute("GET", "", handler);
      return this;
    }

    public Group get(final Route.OneArgHandler handler) {
      newRoute("GET", "", handler);
      return this;
    }

    public Group get(final Route.ZeroArgHandler handler) {
      newRoute("GET", "", handler);
      return this;
    }

    // ********************************************************************************************
    // POST
    // ********************************************************************************************
    public Group post(final String pattern, final Route.Filter filter) {
      newRoute("POST", pattern, filter);
      return this;
    }

    public Group post(final String pattern, final Route.Handler handler) {
      newRoute("POST", pattern, handler);
      return this;
    }

    public Group post(final String pattern, final Route.OneArgHandler handler) {
      newRoute("POST", pattern, handler);
      return this;
    }

    public Group post(final String pattern, final Route.ZeroArgHandler handler) {
      newRoute("POST", pattern, handler);
      return this;
    }

    public Group post(final Route.Filter filter) {
      newRoute("POST", "", filter);
      return this;
    }

    public Group post(final Route.Handler handler) {
      newRoute("POST", "", handler);
      return this;
    }

    public Group post(final Route.OneArgHandler handler) {
      newRoute("POST", "", handler);
      return this;
    }

    public Group post(final Route.ZeroArgHandler handler) {
      newRoute("POST", "", handler);
      return this;
    }

    // ********************************************************************************************
    // PUT
    // ********************************************************************************************
    public Group put(final String pattern, final Route.Filter filter) {
      newRoute("PUT", pattern, filter);
      return this;
    }

    public Group put(final String pattern, final Route.Handler handler) {
      newRoute("PUT", pattern, handler);
      return this;
    }

    public Group put(final String pattern, final Route.OneArgHandler handler) {
      newRoute("PUT", pattern, handler);
      return this;
    }

    public Group put(final String pattern, final Route.ZeroArgHandler handler) {
      newRoute("PUT", pattern, handler);
      return this;
    }

    public Group put(final Route.Filter filter) {
      newRoute("PUT", "", filter);
      return this;
    }

    public Group put(final Route.Handler handler) {
      newRoute("PUT", "", handler);
      return this;
    }

    public Group put(final Route.OneArgHandler handler) {
      newRoute("PUT", "", handler);
      return this;
    }

    public Group put(final Route.ZeroArgHandler handler) {
      newRoute("PUT", "", handler);
      return this;
    }

    // ********************************************************************************************
    // DELETE
    // ********************************************************************************************
    public Group delete(final String pattern, final Route.Filter filter) {
      newRoute("DELETE", pattern, filter);
      return this;
    }

    public Group delete(final String pattern, final Route.Handler handler) {
      newRoute("DELETE", pattern, handler);
      return this;
    }

    public Group delete(final String pattern, final Route.OneArgHandler handler) {
      newRoute("DELETE", pattern, handler);
      return this;
    }

    public Group delete(final String pattern, final Route.ZeroArgHandler handler) {
      newRoute("DELETE", pattern, handler);
      return this;
    }

    public Group delete(final Route.Filter filter) {
      newRoute("DELETE", "", filter);
      return this;
    }

    public Group delete(final Route.Handler handler) {
      newRoute("DELETE", "", handler);
      return this;
    }

    public Group delete(final Route.OneArgHandler handler) {
      newRoute("DELETE", "", handler);
      return this;
    }

    public Group delete(final Route.ZeroArgHandler handler) {
      newRoute("DELETE", "", handler);
      return this;
    }

    // ********************************************************************************************
    // PATCH
    // ********************************************************************************************
    public Group patch(final String pattern, final Route.Filter filter) {
      newRoute("PATCH", pattern, filter);
      return this;
    }

    public Group patch(final String pattern, final Route.Handler handler) {
      newRoute("PATCH", pattern, handler);
      return this;
    }

    public Group patch(final String pattern, final Route.OneArgHandler handler) {
      newRoute("PATCH", pattern, handler);
      return this;
    }

    public Group patch(final String pattern, final Route.ZeroArgHandler handler) {
      newRoute("PATCH", pattern, handler);
      return this;
    }

    public Group patch(final Route.Filter filter) {
      newRoute("PATCH", "", filter);
      return this;
    }

    public Group patch(final Route.Handler handler) {
      newRoute("PATCH", "", handler);
      return this;
    }

    public Group patch(final Route.OneArgHandler handler) {
      newRoute("PATCH", "", handler);
      return this;
    }

    public Group patch(final Route.ZeroArgHandler handler) {
      newRoute("PATCH", "", handler);
      return this;
    }

    /**
     * Set the route name to the whole collection.
     *
     * @param name Name to use/set.
     * @return This instance.
     */
    @Override
    public Group name(final String name) {
      for (Definition definition : routes) {
        if (prefix != null) {
          definition.name(prefix + "/" + name);
        } else {
          definition.name(name);
        }
      }
      return this;
    }

    @Override
    public Group consumes(final List<MediaType> types) {
      for (Definition definition : routes) {
        definition.consumes(types);
      }
      return this;
    }

    @Override
    public Group produces(final List<MediaType> types) {
      for (Definition definition : routes) {
        definition.produces(types);
      }
      return this;
    }

    @Override
    public Group attr(final String name, final Object value) {
      for (Definition definition : routes) {
        definition.attr(name, value);
      }
      return this;
    }

    @Override
    public Group excludes(final List<String> excludes) {
      for (Definition definition : routes) {
        definition.excludes(excludes);
      }
      return this;
    }

    private void newRoute(final String method, final String pattern,
        final Route.Filter filter) {
      newRoute(new Route.Definition(method, this.rootPattern + pattern, filter));
    }

    private void newRoute(final String method, final String pattern,
        final Route.Handler filter) {
      newRoute(new Route.Definition(method, this.rootPattern + pattern, filter));
    }

    private void newRoute(final String method, final String pattern,
        final Route.OneArgHandler filter) {
      newRoute(new Route.Definition(method, this.rootPattern + pattern, filter));
    }

    private void newRoute(final String method, final String pattern,
        final Route.ZeroArgHandler filter) {
      newRoute(new Route.Definition(method, this.rootPattern + pattern, filter));
    }

    private void newRoute(final Route.Definition route) {
      if (prefix != null) {
        route.name(prefix);
      }
      routes.add(route);
    }

  };

  /**
   * Collection of {@link Route.Definition} useful for registering/setting route options at once.
   *
   * @author edgar
   * @since 0.5.0
   */
  class Collection implements Props<Collection> {

    /** List of definitions. */
    private Route.Definition[] routes;

    /**
     * Creates a new collection of route definitions.
     *
     * @param definitions Collection of route definitions.
     */
    public Collection(final Route.Definition... definitions) {
      this.routes = requireNonNull(definitions, "Route definitions are required.");
    }

    @Override
    public Collection name(final String name) {
      for (Definition definition : routes) {
        definition.name(name);
      }
      return this;
    }

    @Override
    public Collection consumes(final List<MediaType> types) {
      for (Definition definition : routes) {
        definition.consumes(types);
      }
      return this;
    }

    @Override
    public Collection produces(final List<MediaType> types) {
      for (Definition definition : routes) {
        definition.produces(types);
      }
      return this;
    }

    @Override
    public Collection attr(final String name, final Object value) {
      for (Definition definition : routes) {
        definition.attr(name, value);
      }
      return this;
    }

    @Override
    public Collection excludes(final List<String> excludes) {
      for (Definition definition : routes) {
        definition.excludes(excludes);
      }
      return this;
    }

  }

  /**
   * DSL for customize routes.
   *
   * <h1>Defining a new route</h1>
   * <p>
   * It's pretty straight forward:
   * </p>
   *
   * <pre>
   *   public class MyApp extends Jooby {
   *     {
   *        get("/", (req, rsp) {@literal ->} rsp.send("GET"));
   *
   *        post("/", (req, rsp) {@literal ->} rsp.send("POST"));
   *
   *        put("/", (req, rsp) {@literal ->} rsp.send("PUT"));
   *
   *        delete("/", (req, rsp) {@literal ->} rsp.status(Response.Status.NO_CONTENT));
   *     }
   *   }
   * </pre>
   *
   * <h1>Setting what a route can consumes</h1>
   *
   * <pre>
   *   public class MyApp extends Jooby {
   *     {
   *        post("/", (req, resp) {@literal ->} resp.send("POST"))
   *          .consumes(MediaType.json);
   *     }
   *   }
   * </pre>
   *
   * <h1>Setting what a route can produces</h1>
   *
   * <pre>
   *   public class MyApp extends Jooby {
   *     {
   *        post("/", (req, resp) {@literal ->} resp.send("POST"))
   *          .produces(MediaType.json);
   *     }
   *   }
   * </pre>
   *
   * <h1>Adding a name</h1>
   *
   * <pre>
   *   public class MyApp extends Jooby {
   *     {
   *        post("/", (req, resp) {@literal ->} resp.send("POST"))
   *          .name("My Root");
   *     }
   *   }
   * </pre>
   *
   * @author edgar
   * @since 0.1.0
   */
  class Definition implements Props<Definition> {

    /**
     * Route's name.
     */
    private String name = "/anonymous";

    /**
     * A route pattern.
     */
    private RoutePattern cpattern;

    /**
     * The target route.
     */
    private Filter filter;

    /**
     * Defines the media types that the methods of a resource class or can accept. Default is:
     * {@code *}/{@code *}.
     */
    private List<MediaType> consumes = MediaType.ALL;

    /**
     * Defines the media types that the methods of a resource class or can produces. Default is:
     * {@code *}/{@code *}.
     */
    private List<MediaType> produces = MediaType.ALL;

    /**
     * A HTTP verb or <code>*</code>.
     */
    private String method;

    /**
     * A path pattern.
     */
    private String pattern;

    private List<RoutePattern> excludes = Collections.emptyList();

    private Map<String, Object> attributes = new HashMap<>();

    /**
     * Creates a new route definition.
     *
     * @param verb A HTTP verb or <code>*</code>.
     * @param pattern A path pattern.
     * @param handler A route handler.
     */
    public Definition(final String verb, final String pattern,
        final Route.Handler handler) {
      this(verb, pattern, (Route.Filter) handler);
    }

    /**
     * Creates a new route definition.
     *
     * @param verb A HTTP verb or <code>*</code>.
     * @param pattern A path pattern.
     * @param handler A route handler.
     */
    public Definition(final String verb, final String pattern,
        final Route.OneArgHandler handler) {
      this(verb, pattern, (Route.Filter) handler);
    }

    /**
     * Creates a new route definition.
     *
     * @param verb A HTTP verb or <code>*</code>.
     * @param pattern A path pattern.
     * @param handler A route handler.
     */
    public Definition(final String verb, final String pattern,
        final Route.ZeroArgHandler handler) {
      this(verb, pattern, (Route.Filter) handler);
    }

    /**
     * Creates a new route definition.
     *
     * @param method A HTTP verb or <code>*</code>.
     * @param pattern A path pattern.
     * @param filter A callback to execute.
     */
    public Definition(final String method, final String pattern,
        final Filter filter) {
      requireNonNull(pattern, "A route path is required.");
      requireNonNull(filter, "A filter is required.");

      this.method = method.toUpperCase();
      this.cpattern = new RoutePattern(method, pattern);
      // normalized pattern
      this.pattern = cpattern.pattern();
      this.filter = filter;
    }

    /**
     * <h1>Path Patterns</h1>
     * <p>
     * Jooby supports Ant-style path patterns:
     * </p>
     * <p>
     * Some examples:
     * </p>
     * <ul>
     * <li>{@code com/t?st.html} - matches {@code com/test.html} but also {@code com/tast.jsp} or
     * {@code com/txst.html}</li>
     * <li>{@code com/*.html} - matches all {@code .html} files in the {@code com} directory</li>
     * <li><code>com/{@literal **}/test.html</code> - matches all {@code test.html} files underneath
     * the {@code com} path</li>
     * <li>{@code **}/{@code *} - matches any path at any level.</li>
     * <li>{@code *} - matches any path at any level, shorthand for {@code **}/{@code *}.</li>
     * </ul>
     *
     * <h2>Variables</h2>
     * <p>
     * Jooby supports path parameters too:
     * </p>
     * <p>
     * Some examples:
     * </p>
     * <ul>
     * <li><code> /user/{id}</code> - /user/* and give you access to the <code>id</code> var.</li>
     * <li><code> /user/:id</code> - /user/* and give you access to the <code>id</code> var.</li>
     * <li><code> /user/{id:\\d+}</code> - /user/[digits] and give you access to the numeric
     * <code>id</code> var.</li>
     * </ul>
     *
     * @return A path pattern.
     */
    public String pattern() {
      return pattern;
    }

    /**
     * @return List of path variables (if any).
     */
    public List<String> vars() {
      return cpattern.vars();
    }

    /**
     * Recreate a route path and apply the given variables.
     *
     * @param vars Path variables.
     * @return A route pattern.
     */
    public String reverse(final Map<String, Object> vars) {
      return cpattern.reverse(vars);
    }

    /**
     * Recreate a route path and apply the given variables.
     *
     * @param values Path variable values.
     * @return A route pattern.
     */
    public String reverse(final Object... values) {
      return cpattern.reverse(values);
    }

    @Override
    public Definition attr(final String name, final Object value) {
      requireNonNull(name, "Attribute name is required.");
      requireNonNull(value, "Attribute value is required.");

      validate(value);
      attributes.put(name, value);
      return this;
    }

    private boolean validate(final Object value) {
      return Match(value).option(
          Case(v -> Primitives.isWrapperType(Primitives.wrap(v.getClass())), true),
          Case(instanceOf(String.class), true),
          Case(instanceOf(Enum.class), true),
          Case(instanceOf(Class.class), true),
          Case(c -> c.getClass().isArray(), v -> validate(Array.get(v, 0))))
          .getOrElseThrow(() -> new IllegalArgumentException("Unsupported attribute: " + value));
    }

    /**
     * Get an attribute by name.
     *
     * @param name Attribute's name.
     * @return Attribute's value or <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public <T> T attr(final String name) {
      return (T) attributes.get(name);
    }

    /**
     * @return A read only view of attributes.
     */
    public Map<String, Object> attributes() {
      return ImmutableMap.copyOf(attributes);
    }

    /**
     * Test if the route matches the given verb, path, content type and accept header.
     *
     * @param verb A HTTP verb.
     * @param path Current HTTP path.
     * @param contentType The <code>Content-Type</code> header.
     * @param accept The <code>Accept</code> header.
     * @return A route or an empty optional.
     */
    public Optional<Route> matches(final String verb,
        final String path, final MediaType contentType,
        final List<MediaType> accept) {
      String fpath = verb + path;
      if (excludes.size() > 0 && excludes(fpath)) {
        return Optional.empty();
      }
      RouteMatcher matcher = cpattern.matcher(fpath);
      if (matcher.matches()) {
        List<MediaType> result = MediaType.matcher(accept).filter(this.produces);
        if (result.size() > 0 && canConsume(contentType)) {
          // keep accept when */*
          List<MediaType> produces = result.size() == 1 && result.get(0).name().equals("*/*")
              ? accept : this.produces;
          return Optional.of(asRoute(verb, matcher, produces));
        }
      }
      return Optional.empty();
    }

    /**
     * @return HTTP method or <code>*</code>.
     */
    public String method() {
      return method;
    }

    /**
     * @return Handler behind this route.
     */
    public Route.Filter filter() {
      return filter;
    }

    /**
     * Route's name, helpful for debugging but also to implement dynamic and advanced routing. See
     * {@link Route.Chain#next(String, Request, Response)}
     *
     * @return Route name. Default is: <code>anonymous</code>.
     */
    public String name() {
      return name;
    }

    /**
     * Set the route name. Route's name, helpful for debugging but also to implement dynamic and
     * advanced routing. See {@link Route.Chain#next(String, Request, Response)}
     *
     *
     * @param name A route's name.
     * @return This definition.
     */
    @Override
    public Definition name(final String name) {
      checkArgument(!Strings.isNullOrEmpty(name), "A route's name is required.");
      this.name = normalize(name);
      return this;
    }

    /**
     * Test if the route definition can consume a media type.
     *
     * @param type A media type to test.
     * @return True, if the route can consume the given media type.
     */
    public boolean canConsume(final MediaType type) {
      return MediaType.matcher(Arrays.asList(type)).matches(consumes);
    }

    /**
     * Test if the route definition can consume a media type.
     *
     * @param type A media type to test.
     * @return True, if the route can consume the given media type.
     */
    public boolean canConsume(final String type) {
      return MediaType.matcher(MediaType.valueOf(type)).matches(consumes);
    }

    /**
     * Test if the route definition can consume a media type.
     *
     * @param types A media types to test.
     * @return True, if the route can produces the given media type.
     */
    public boolean canProduce(final List<MediaType> types) {
      return MediaType.matcher(types).matches(produces);
    }

    /**
     * Test if the route definition can consume a media type.
     *
     * @param types A media types to test.
     * @return True, if the route can produces the given media type.
     */
    public boolean canProduce(final MediaType... types) {
      return canProduce(Arrays.asList(types));
    }

    /**
     * Test if the route definition can consume a media type.
     *
     * @param types A media types to test.
     * @return True, if the route can produces the given media type.
     */
    public boolean canProduce(final String... types) {
      return canProduce(MediaType.valueOf(types));
    }

    @Override
    public Definition consumes(final List<MediaType> types) {
      checkArgument(types != null && types.size() > 0, "Consumes types are required");
      if (types.size() > 1) {
        this.consumes = Lists.newLinkedList(types);
        Collections.sort(this.consumes);
      } else {
        this.consumes = ImmutableList.of(types.get(0));
      }
      return this;
    }

    @Override
    public Definition produces(final List<MediaType> types) {
      checkArgument(types != null && types.size() > 0, "Produces types are required");
      if (types.size() > 1) {
        this.produces = Lists.newLinkedList(types);
        Collections.sort(this.produces);
      } else {
        this.produces = ImmutableList.of(types.get(0));
      }
      return this;
    }

    @Override
    public Definition excludes(final List<String> excludes) {
      this.excludes = excludes.stream()
          .map(it -> new RoutePattern(method, it))
          .collect(Collectors.toList());
      return this;
    }

    /**
     * @return List of exclusion filters (if any).
     */
    public List<String> excludes() {
      return excludes.stream().map(r -> r.pattern()).collect(Collectors.toList());
    }

    private boolean excludes(final String path) {
      for (RoutePattern pattern : excludes) {
        if (pattern.matcher(path).matches()) {
          return true;
        }
      }
      return false;
    }

    /**
     * @return All the types this route can consumes.
     */
    public List<MediaType> consumes() {
      return ImmutableList.copyOf(this.consumes);
    }

    /**
     * @return All the types this route can produces.
     */
    public List<MediaType> produces() {
      return ImmutableList.copyOf(this.produces);
    }

    @Override
    public String toString() {
      StringBuilder buffer = new StringBuilder();
      buffer.append(method()).append(" ").append(pattern()).append("\n");
      buffer.append("  name: ").append(name()).append("\n");
      buffer.append("  excludes: ").append(excludes).append("\n");
      buffer.append("  consumes: ").append(consumes()).append("\n");
      buffer.append("  produces: ").append(produces()).append("\n");
      return buffer.toString();
    }

    /**
     * Creates a new route.
     *
     * @param method A HTTP verb.
     * @param matcher A route matcher.
     * @param produces List of produces types.
     * @return A new route.
     */
    private Route asRoute(final String method, final RouteMatcher matcher,
        final List<MediaType> produces) {
      Route.Filter filter = this.filter;
      if (filter instanceof AssetProxy) {
        filter = ((AssetProxy) filter).delegate();
      }
      return new RouteImpl(filter, method, matcher.path(), pattern, name, matcher.vars(), consumes,
          produces, attributes);
    }

  }

  /**
   * A forwarding route.
   *
   * @author edgar
   * @since 0.1.0
   */
  class Forwarding implements Route {

    /**
     * Target route.
     */
    private final Route route;

    /**
     * Creates a new {@link Forwarding} route.
     *
     * @param route A target route.
     */
    public Forwarding(final Route route) {
      this.route = requireNonNull(route, "A route is required.");
    }

    @Override
    public String path() {
      return route.path();
    }

    @Override
    public String method() {
      return route.method();
    }

    @Override
    public String pattern() {
      return route.pattern();
    }

    @Override
    public String name() {
      return route.name();
    }

    @Override
    public Map<Object, String> vars() {
      return route.vars();
    }

    @Override
    public List<MediaType> consumes() {
      return route.consumes();
    }

    @Override
    public List<MediaType> produces() {
      return route.produces();
    }

    @Override
    public Map<String, Object> attributes() {
      return route.attributes();
    }

    @Override
    public <T> T attr(final String name) {
      return route.attr(name);
    }

    @Override
    public String toString() {
      return route.toString();
    }

    /**
     * Find a target route.
     *
     * @param route A route to check.
     * @return A target route.
     */
    public static Route unwrap(final Route route) {
      requireNonNull(route, "A route is required.");
      Route root = route;
      while (root instanceof Forwarding) {
        root = ((Forwarding) root).route;
      }
      return root;
    }

  }

  /**
   * Filter is like a {@link Route.Handler} but it decided if the next route handler in the chain
   * can be executed or not. Example of filters are:
   *
   * <h3>Auth handler example</h3>
   *
   * <pre>
   *   String token = req.header("token").value();
   *   if (token != null) {
   *     // validate token...
   *     if (valid(token)) {
   *       chain.next(req, rsp);
   *     }
   *   } else {
   *     rsp.status(403);
   *   }
   * </pre>
   *
   * <h3>Logging/Around handler example</h3>
   *
   * <pre>
   *   long start = System.currentTimeMillis();
   *   chain.next(req, rsp);
   *   long end = System.currentTimeMillis();
   *   log.info("Request: {} took {}ms", req.path(), end - start);
   * </pre>
   *
   * NOTE: Don't forget to call {@link Route.Chain#next(Request, Response)} if next route handler
   * need to be executed.
   *
   * @author edgar
   * @since 0.1.0
   */
  public interface Filter {

    /**
     * The <code>handle</code> method of the Filter is called by the server each time a
     * request/response pair is passed through the chain due to a client request for a resource at
     * the end of the chain.
     * The {@link Route.Chain} passed in to this method allows the Filter to pass on the request and
     * response to the next entity in the chain.
     *
     * <p>
     * A typical implementation of this method would follow the following pattern:
     * </p>
     * <ul>
     * <li>Examine the request</li>
     * <li>Optionally wrap the request object with a custom implementation to filter content or
     * headers for input filtering</li>
     * <li>Optionally wrap the response object with a custom implementation to filter content or
     * headers for output filtering</li>
     * <li>
     * <ul>
     * <li><strong>Either</strong> invoke the next entity in the chain using the {@link Route.Chain}
     * object (<code>chain.next(req, rsp)</code>),</li>
     * <li><strong>or</strong> not pass on the request/response pair to the next entity in the
     * filter chain to block the request processing</li>
     * </ul>
     * <li>Directly set headers on the response after invocation of the next entity in the filter
     * chain.</li>
     * </ul>
     *
     * @param req A HTTP request.
     * @param rsp A HTTP response.
     * @param chain A route chain.
     * @throws Throwable If something goes wrong.
     */
    void handle(Request req, Response rsp, Route.Chain chain) throws Throwable;

  }

  /**
   * A route handler/callback.
   *
   * <pre>
   * public class MyApp extends Jooby {
   *   {
   *      get("/", (req, rsp) {@literal ->} rsp.send("Hello"));
   *   }
   * }
   * </pre>
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Handler extends Filter {

    @Override
    default void handle(final Request req, final Response rsp, final Route.Chain chain)
        throws Throwable {
      handle(req, rsp);
      chain.next(req, rsp);
    }

    /**
     * Callback method for a HTTP request.
     *
     * @param req A HTTP request.
     * @param rsp A HTTP response.
     * @throws Throwable If something goes wrong. The exception will processed by Jooby.
     */
    void handle(Request req, Response rsp) throws Throwable;

  }

  /**
   * A handler from a MVC route, it extends {@link Handler} by adding a reference to the method
   * behind this route.
   *
   * @author edgar
   * @since 0.6.2
   */
  interface MethodHandler extends Handler {
    Method method();
  }

  /**
   * A route handler/callback that doesn't require a {@link Response} object. This handler expect a
   * return type to send as response.
   *
   * <pre>
   * public class MyApp extends Jooby {
   *   {
   *      get("/", (req) {@literal ->} "Hello");
   *   }
   * }
   * </pre>
   *
   * @author edgar
   * @since 0.1.1
   */
  interface OneArgHandler extends Filter {

    @Override
    default void handle(final Request req, final Response rsp, final Route.Chain chain)
        throws Throwable {
      Object result = handle(req);
      rsp.send(result);
      chain.next(req, rsp);
    }

    /**
     * Callback method for a HTTP request.
     *
     * @param req A HTTP request.
     * @return Message to send.
     * @throws Throwable If something goes wrong. The exception will processed by Jooby.
     */
    Object handle(Request req) throws Throwable;
  }

  /**
   * A route handler/callback that doesn't require a {@link Request} or {@link Response} objects.
   * This handler expect a return type to send as response.
   *
   * <pre>
   * public class MyApp extends Jooby {
   *   {
   *      get("/", () {@literal ->} "Hello");
   *   }
   * }
   * </pre>
   *
   * @author edgar
   * @since 0.1.1
   */
  interface ZeroArgHandler extends Filter {

    @Override
    default void handle(final Request req, final Response rsp, final Route.Chain chain)
        throws Throwable {
      Object result = handle();
      rsp.send(result);
      chain.next(req, rsp);
    }

    /**
     * Callback method for a HTTP request.
     *
     * @return Message to send.
     * @throws Throwable If something goes wrong. The exception will processed by Jooby.
     */
    Object handle() throws Throwable;
  }

  /**
   * <h2>before</h2>
   *
   * Allows for customized handler execution chains. It will be invoked before the actual handler.
   *
   * <pre>{@code
   * {
   *   before((req, rsp) -> {
   *     // your code goes here
   *   });
   * }
   * }</pre>
   *
   * You are allowed to modify the request and response objects.
   *
   * Please note that the <code>before</code> handler is just syntax sugar for {@link Route.Filter}.
   * For example, the <code>before</code> handler was implemented as:
   *
   * <pre>{@code
   * {
   *   use("*", "*", (req, rsp, chain) -> {
   *     before(req, rsp);
   *     // your code goes here
   *     chain.next(req, rsp);
   *   });
   * }
   * }</pre>
   *
   * A <code>before</code> handler must to be registered before the actual handler you want to
   * intercept.
   *
   * <pre>{@code
   * {
   *   before((req, rsp) -> {
   *     // your code goes here
   *   });
   *
   *   get("/path", req -> {
   *     // your code goes here
   *     return ...;
   *   });
   * }
   * }</pre>
   *
   * If you reverse the order then it won't work.
   *
   * <p>
   * <strong>Remember</strong>: routes are executed in the order they are defined and the pipeline
   * is executed as long you don't generate a response.
   * </p>
   *
   * @author edgar
   * @since 1.0.0.CR
   */
  interface Before extends Route.Filter {

    @Override
    default void handle(final Request req, final Response rsp, final Chain chain) throws Throwable {
      handle(req, rsp);
      chain.next(req, rsp);
    }

    /**
     * Allows for customized handler execution chains. It will be invoked before the actual handler.
     *
     * @param req Request.
     * @param rsp Response
     * @throws Throwable If something goes wrong.
     */
    void handle(Request req, Response rsp) throws Throwable;
  }

  /**
   * <h2>after</h2>
   *
   * Allows for customized response before send it. It will be invoked at the time a response need
   * to be send.
   *
   * <pre>{@code
   * {
   *   after("GET", "*", (req, rsp, result) -> {
   *     // your code goes here
   *     return result;
   *   });
   * }
   * }</pre>
   *
   * You are allowed to modify the request, response and result objects. The handler returns a
   * {@link Result} which can be the same or an entirely new {@link Result}.
   *
   * Please note that the <code>after</code> handler is just syntax sugar for
   * {@link Route.Filter}.
   * For example, the <code>after</code> handler was implemented as:
   *
   * <pre>{@code
   * {
   *   use("GET", "*", (req, rsp, chain) -> {
   *     chain.next(req, new Response.Forwarding(rsp) {
   *       public void send(Result result) {
   *         rsp.send(after(req, rsp, result);
   *       }
   *     });
   *   });
   * }
   * }</pre>
   *
   * Due <code>after</code> is implemented by wrapping the {@link Response} object. A
   * <code>after</code> handler must to be registered before the actual handler you want to
   * intercept.
   *
   * <pre>{@code
   * {
   *   after("GET", "/path", (req, rsp, result) -> {
   *     // your code goes here
   *     return result;
   *   });
   *
   *   get("/path", req -> {
   *     return "hello";
   *   });
   * }
   * }</pre>
   *
   * If you reverse the order then it won't work.
   *
   * <p>
   * <strong>Remember</strong>: routes are executed in the order they are defined and the pipeline
   * is executed as long you don't generate a response.
   * </p>
   *
   * @author edgar
   * @since 1.0.0.CR
   */
  interface After extends Filter {
    @Override
    default void handle(final Request req, final Response rsp, final Chain chain) throws Throwable {
      chain.next(req, new Response.Forwarding(rsp) {

        @Override
        public void send(final Result result) throws Throwable {
          super.send(handle(req, rsp, result));
        }
      });
    }

    /**
     * Allows for customized response before send it. It will be invoked at the time a response need
     * to be send.
     *
     * @param req Request.
     * @param rsp Response
     * @param result Result.
     * @return Same or new result.
     * @throws Exception If something goes wrong.
     */
    Result handle(Request req, Response rsp, Result result) throws Exception;
  }

  /**
   * <h2>complete</h2>
   *
   * Allows for log and cleanup a request. It will be invoked after we send a response.
   *
   * <pre>{@code
   * {
   *   complete((req, rsp, cause) -> {
   *     // your code goes here
   *   });
   * }
   * }</pre>
   *
   * You are NOT allowed to modify the request and response objects. The <code>cause</code> is an
   * {@link Optional} with a {@link Throwable} useful to identify problems.
   *
   * The goal of the <code>complete</code> handler is to probably cleanup request object and log
   * responses.
   *
   * Please note that the <code>complete</code> handler is just syntax sugar for
   * {@link Route.Filter}.
   * For example, the <code>complete</code> handler was implemented as:
   *
   * <pre>{@code
   * {
   *   use("*", "*", (req, rsp, chain) -> {
   *     Optional<Throwable> err = Optional.empty();
   *     try {
   *       chain.next(req, rsp);
   *     } catch (Throwable cause) {
   *       err = Optional.of(cause);
   *     } finally {
   *       complete(req, rsp, err);
   *     }
   *   });
   * }
   * }</pre>
   *
   * An <code>complete</code> handler must to be registered before the actual handler you want to
   * intercept.
   *
   * <pre>{@code
   * {
   *   complete((req, rsp, cause) -> {
   *     // your code goes here
   *   });
   *
   *   get(req -> {
   *     return "hello";
   *   });
   * }
   * }</pre>
   *
   * If you reverse the order then it won't work.
   *
   * <p>
   * <strong>Remember</strong>: routes are executed in the order they are defined and the pipeline
   * is executed as long you don't generate a response.
   * </p>
   *
   * <h2>example</h2>
   * <p>
   * Suppose you have a transactional resource, like a database connection. The next example shows
   * you how to implement a simple and effective <code>transaction-per-request</code> pattern:
   * </p>
   *
   * <pre>{@code
   * {
   *   // start transaction
   *   before((req, rsp) -> {
   *     DataSource ds = req.require(DataSource.class);
   *     Connection connection = ds.getConnection();
   *     Transaction trx = connection.getTransaction();
   *     trx.begin();
   *     req.set("connection", connection);
   *     return true;
   *   });
   *
   *   // commit/rollback transaction
   *   complete((req, rsp, cause) -> {
   *     // unbind connection from request
   *     try(Connection connection = req.unset("connection").get()) {
   *       Transaction trx = connection.getTransaction();
   *       if (cause.ifPresent()) {
   *         trx.rollback();
   *       } else {
   *         trx.commit();
   *       }
   *     }
   *   });
   *
   *   // your transactional routes goes here
   *   get("/api/something", req -> {
   *     Connection connection = req.get("connection");
   *     // work with connection
   *   });
   * }
   * }</pre>
   *
   * @author edgar
   * @since 1.0.0.CR
   */
  interface Complete extends Filter {

    @Override
    default void handle(final Request req, final Response rsp, final Chain chain) throws Throwable {
      Optional<Throwable> err = Optional.empty();
      try {
        chain.next(req, rsp);
      } catch (DeferredExecution ex) {
        // try it as success
        throw ex;
      } catch (Throwable cause) {
        err = Optional.of(cause);
        throw cause;
      } finally {
        handle(req, rsp, err);
      }
    }

    /**
     * Allows for log and cleanup a request. It will be invoked after we send a response.
     *
     * @param req Request.
     * @param rsp Response
     * @param cause Empty optional on success. Otherwise, it contains the exception.
     */
    void handle(Request req, Response rsp, Optional<Throwable> cause);
  }

  /**
   * Chain of routes to be executed. It invokes the next route in the chain.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Chain {
    /**
     * Invokes the next route in the chain where {@link Route#name()} starts with the given prefix.
     *
     * @param prefix Iterates over the route chain and keep routes that start with the given prefix.
     * @param req A HTTP request.
     * @param rsp A HTTP response.
     * @throws Throwable If invocation goes wrong.
     */
    void next(String prefix, Request req, Response rsp) throws Throwable;

    /**
     * Invokes the next route in the chain.
     *
     * @param req A HTTP request.
     * @param rsp A HTTP response.
     * @throws Throwable If invocation goes wrong.
     */
    default void next(final Request req, final Response rsp) throws Throwable {
      next(null, req, rsp);
    }
  }

  /** Renderer key. */
  Key<Set<Route.Definition>> KEY = Key.get(new TypeLiteral<Set<Route.Definition>>() {
  });

  String GET = "GET";

  String POST = "POST";

  String PUT = "PUT";

  String DELETE = "DELETE";

  String PATCH = "PATCH";

  String HEAD = "HEAD";

  String CONNECT = "CONNECT";

  String OPTIONS = "OPTIONS";

  String TRACE = "TRACE";

  /**
   * Well known HTTP methods.
   */
  List<String> METHODS = ImmutableList.<String> builder()
      .add(GET,
          POST,
          PUT,
          DELETE,
          PATCH,
          HEAD,
          CONNECT,
          OPTIONS,
          TRACE)
      .build();

  /**
   * Renderer attribute.
   *
   * @see Route.Definition#renderer(String)
   */
  String RENDERER = "renderer";

  /**
   * @return Current request path.
   */
  String path();

  /**
   * @return Current HTTP method.
   */
  String method();

  /**
   * @return The currently matched pattern.
   */
  String pattern();

  /**
   * Route's name, helpful for debugging but also to implement dynamic and advanced routing. See
   * {@link Route.Chain#next(String, Request, Response)}
   *
   * @return Route name, defaults to <code>"anonymous"</code>
   */
  String name();

  /**
   * Path variables, either named or by index (capturing group).
   *
   * <pre>
   *   /path/:var
   * </pre>
   *
   * Variable <code>var</code> is accessible by name: <code>var</code> or index: <code>0</code>.
   *
   * @return The currently matched path variables (if any).
   */
  Map<Object, String> vars();

  /**
   * @return List all the types this route can consumes, defaults is: {@code * / *}.
   */
  List<MediaType> consumes();

  /**
   * @return List all the types this route can produces, defaults is: {@code * / *}.
   */
  List<MediaType> produces();

  /**
   * True, when route's name starts with the given prefix. Useful for dynamic routing. See
   * {@link Route.Chain#next(String, Request, Response)}
   *
   * @param prefix Prefix to check for.
   * @return True, when route's name starts with the given prefix.
   */
  default boolean apply(final String prefix) {
    return name().startsWith(prefix);
  }

  /**
   * @return All the available attributes in the execution chain.
   */
  Map<String, Object> attributes();

  /**
   * Attribute by name.
   *
   * @param name Attribute's name.
   * @return Attribute value.
   */
  @SuppressWarnings("unchecked")
  default <T> T attr(final String name) {
    return (T) attributes().get(name);
  }

  /**
   * Normalize a path by removing double or trailing slashes.
   *
   * @param path A path to normalize.
   * @return A normalized path.
   */
  static String normalize(final String path) {
    return RoutePattern.normalize(path);
  }
}
