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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.annotation.Nonnull;

import org.jooby.internal.RouteImpl;
import org.jooby.internal.RouteMatcher;
import org.jooby.internal.RoutePattern;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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
 *     names.add(req.param("name").stringValue();
 *     // response will be different between calls.
 *     rsp.send(names);
 *   });
 * </pre>
 *
 * <h2>External route</h2>
 * <p>
 * An external route can be defined by using a {@link Class route class}, like:
 * </p>
 *
 * <pre>
 *   get("/", handler(ExternalRoute.class)); //or
 *
 *   ...
 *   // ExternalRoute.java
 *   public class ExternalRoute implements Route.Handler {
 *     public void handle(Request req, Response rsp) throws Exception {
 *       rsp.send("Hello Jooby");
 *     }
 *   }
 * </pre>
 *
 * <h2>Mvc Route</h2>
 * <p>
 * A Mvc Route use annotations to define routes:
 * </p>
 *
 * <pre>
 *   route(MyRoute.class);
 *   ...
 *   // MyRoute.java
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
 * {@link org.jooby.mvc.Produces} {@link org.jooby.mvc.Consumes} and {@link org.jooby.mvc.Viewable}.
 * </p>
 *
 * @author edgar
 * @since 0.1.0
 */
public interface Route {

  /**
   * Collection of {@link Route.Definition} useful for registering/setting route options at once.
   *
   * @author edgar
   * @since 0.5.0
   */
  class Definitions {

    /** List of definitions. */
    private Route.Definition[] definitions;

    /**
     * Creates a new collection of route definitions.
     *
     * @param definitions Collection of route definitions.
     */
    public Definitions(final Route.Definition[] definitions) {
      this.definitions = requireNonNull(definitions, "Route definitions are required.");
    }

    /**
     * Set the route name to the whole collection.
     *
     * @param name Name to use/set.
     * @return This instance.
     */
    public Definitions name(final String name) {
      for (Definition definition : definitions) {
        definition.name(name);
      }
      return this;
    }

    /**
     * Set what a route can consumes.
     *
     * @param types Media types.
     * @return This instance.
     */
    public Definitions consumes(final MediaType... types) {
      for (Definition definition : definitions) {
        definition.consumes(types);
      }
      return this;
    }

    /**
     * Set what a route can consumes.
     *
     * @param types Media types.
     * @return This instance.
     */
    public Definitions consumes(final String... types) {
      for (Definition definition : definitions) {
        definition.consumes(types);
      }
      return this;
    }

    /**
     * Set what a route can produces.
     *
     * @param types Media types.
     * @return This instance.
     */
    public Definitions produces(final MediaType... types) {
      for (Definition definition : definitions) {
        definition.produces(types);
      }
      return this;
    }

    /**
     * Set what a route can produces.
     *
     * @param types Media types.
     * @return This instance.
     */
    public Definitions produces(final String... types) {
      for (Definition definition : definitions) {
        definition.produces(types);
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
  class Definition {

    /**
     * Route's name.
     */
    private String name = "anonymous";

    /**
     * A route pattern.
     */
    private RoutePattern compiledPattern;

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
    private String verb;

    /**
     * A path pattern.
     */
    private String pattern;

    /**
     * Creates a new route definition.
     *
     * @param verb A HTTP verb or <code>*</code>.
     * @param pattern A path pattern.
     * @param handler A route handler.
     */
    public Definition(final @Nonnull String verb, final @Nonnull String pattern,
        final @Nonnull Route.Handler handler) {
      this(verb, pattern, (req, rsp, chain) -> {
        handler.handle(req, rsp);
        chain.next(req, rsp);
      });
    }

    /**
     * Creates a new route definition.
     *
     * @param verb A HTTP verb or <code>*</code>.
     * @param pattern A path pattern.
     * @param handler A route handler.
     */
    public Definition(final @Nonnull String verb, final @Nonnull String pattern,
        final @Nonnull Route.OneArgHandler handler) {
      this(verb, pattern, (req, rsp, chain) -> {
        Object result = handler.handle(req);
        rsp.send(result);
        chain.next(req, rsp);
      });
    }

    /**
     * Creates a new route definition.
     *
     * @param verb A HTTP verb or <code>*</code>.
     * @param pattern A path pattern.
     * @param handler A route handler.
     */
    public Definition(final @Nonnull String verb, final @Nonnull String pattern,
        final @Nonnull Route.ZeroArgHandler handler) {
      this(verb, pattern, (req, rsp, chain) -> {
        Object result = handler.handle();
        rsp.send(result);
        chain.next(req, rsp);
      });
    }

    /**
     * Creates a new route definition.
     *
     * @param verb A HTTP verb or <code>*</code>.
     * @param pattern A path pattern.
     * @param filter A callback to execute.
     */
    public Definition(final @Nonnull String verb, final @Nonnull String pattern,
        final @Nonnull Filter filter) {
      try {
        requireNonNull(verb, "HTTP verb is required.");
        Verb.valueOf(verb.toUpperCase());
      } catch (IllegalArgumentException ex) {
        if (!"*".equals(verb)) {
          throw new IllegalArgumentException("Invalid HTTP verb: " + verb);
        }
      }
      requireNonNull(pattern, "A route path is required.");
      requireNonNull(filter, "A filter is required.");

      this.verb = verb.toUpperCase();
      this.compiledPattern = new RoutePattern(verb, pattern);
      // normalized pattern
      this.pattern = compiledPattern.pattern();
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
    public @Nonnull String pattern() {
      return pattern;
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
    public @Nonnull Optional<Route> matches(final @Nonnull Verb verb,
        final @Nonnull String path, final @Nonnull MediaType contentType,
        final @Nonnull List<MediaType> accept) {
      RouteMatcher matcher = compiledPattern.matcher(verb.name() + path);
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
     * @return HTTP verb or <code>*</code>
     */
    public @Nonnull String verb() {
      return verb;
    }

    /**
     * @return Route name. Default is: <code>anonymous</code>.
     */
    public @Nonnull String name() {
      return name;
    }

    /**
     * Set the route name.
     *
     * @param name A route's name.
     * @return This definition.
     */
    public @Nonnull Definition name(final @Nonnull String name) {
      checkArgument(!Strings.isNullOrEmpty(name), "A route's name is required.");
      this.name = name;
      return this;
    }

    /**
     * Test if the route definition can consume a media type.
     *
     * @param type A media type to test.
     * @return True, if the route can consume the given media type.
     */
    public boolean canConsume(@Nonnull final MediaType type) {
      return MediaType.matcher(Arrays.asList(type)).matches(consumes);
    }

    /**
     * Test if the route definition can consume a media type.
     *
     * @param type A media type to test.
     * @return True, if the route can consume the given media type.
     */
    public boolean canConsume(@Nonnull final String type) {
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

    /**
     * Set the media types the route can consume.
     *
     * @param consumes The media types to test for.
     * @return This route definition.
     */
    public Definition consumes(final MediaType... consumes) {
      return consumes(Arrays.asList(consumes));
    }

    /**
     * Set the media types the route can consume.
     *
     * @param consumes The media types to test for.
     * @return This route definition.
     */
    public Definition consumes(final String... consumes) {
      return consumes(MediaType.valueOf(consumes));
    }

    /**
     * Set the media types the route can consume.
     *
     * @param consumes The media types to test for.
     * @return This route definition.
     */
    public Definition consumes(final List<MediaType> consumes) {
      checkArgument(consumes != null && consumes.size() > 0, "Consumes types are required");
      if (consumes.size() > 1) {
        this.consumes = Lists.newLinkedList(consumes);
        Collections.sort(this.consumes);
      } else {
        this.consumes = ImmutableList.of(consumes.get(0));
      }
      return this;
    }

    /**
     * Set the media types the route can produces.
     *
     * @param produces The media types to test for.
     * @return This route definition.
     */
    public Definition produces(final MediaType... produces) {
      return produces(Arrays.asList(produces));
    }

    public Definition produces(final String... produces) {
      return produces(MediaType.valueOf(produces));
    }

    /**
     * Set the media types the route can produces.
     *
     * @param produces The media types to test for.
     * @return This route definition.
     */
    public Definition produces(final List<MediaType> produces) {
      checkArgument(produces != null && produces.size() > 0, "Produces types are required");
      if (produces.size() > 1) {
        this.produces = Lists.newLinkedList(produces);
        Collections.sort(this.produces);
      } else {
        this.produces = ImmutableList.of(produces.get(0));
      }
      return this;
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
      buffer.append(verb()).append(" ").append(pattern()).append("\n");
      buffer.append("  name: ").append(name()).append("\n");
      buffer.append("  consume: ").append(consumes()).append("\n");
      buffer.append("  produces: ").append(produces()).append("\n");
      return buffer.toString();
    }

    /**
     * Creates a new route.
     *
     * @param verb A HTTP verb.
     * @param matcher A route matcher.
     * @param produces List of produces types.
     * @return A new route.
     */
    private Route asRoute(final Verb verb, final RouteMatcher matcher,
        final List<MediaType> produces) {
      return new RouteImpl(filter, verb, matcher.path(), pattern, name, matcher.vars(), consumes,
          produces);
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
    public Verb verb() {
      return route.verb();
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
    public Map<String, String> vars() {
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
    public String toString() {
      return route.toString();
    }

    /**
     * Find a target route.
     *
     * @param route A route to check.
     * @return A target route.
     */
    public static @Nonnull Route unwrap(final @Nonnull Route route) {
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
   *   String token = req.header("token").stringValue();
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
     * @throws Exception If something goes wrong.
     */
    void handle(Request req, Response rsp, Route.Chain chain) throws Exception;

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
  interface Handler {

    /**
     * Callback method for a HTTP request.
     *
     * @param req A HTTP request.
     * @param rsp A HTTP response.
     * @throws Exception If something goes wrong. The exception will processed by Jooby.
     */
    void handle(Request req, Response rsp) throws Exception;

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
  interface OneArgHandler {

    /**
     * Callback method for a HTTP request.
     *
     * @param req A HTTP request.
     * @return Message to send.
     * @throws Exception If something goes wrong. The exception will processed by Jooby.
     */
    Object handle(Request req) throws Exception;
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
  interface ZeroArgHandler {

    /**
     * Callback method for a HTTP request.
     *
     * @return Message to send.
     * @throws Exception If something goes wrong. The exception will processed by Jooby.
     */
    Object handle() throws Exception;
  }

  /**
   * Chain of routes to be executed. It invokes the next route in the chain.
   *
   * @author edgar
   * @since 0.1.0
   */
  interface Chain {
    /**
     * Invokes the next route in the chain.
     *
     * @param req A HTTP request.
     * @param rsp A HTTP response.
     * @throws Exception If invocation goes wrong.
     */
    void next(@Nonnull Request req, @Nonnull Response rsp) throws Exception;
  }

  /**
   * @return Current request path.
   */
  String path();

  /**
   * @return Current request verb.
   */
  Verb verb();

  /**
   * @return The currently matched pattern.
   */
  String pattern();

  /**
   * @return Route name, defaults to <code>"anonymous"</code>
   */
  String name();

  /**
   * @return The currently matched path variables (if any).
   */
  Map<String, String> vars();

  /**
   * @return List all the types this route can consumes, defaults is: {@code * / *}.
   */
  List<MediaType> consumes();

  /**
   * @return List all the types this route can produces, defaults is: {@code * / *}.
   */
  List<MediaType> produces();

}
