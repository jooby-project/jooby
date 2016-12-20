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
package org.jooby.test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jooby.Deferred;
import org.jooby.Err;
import org.jooby.Jooby;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Result;
import org.jooby.Results;
import org.jooby.Route;
import org.jooby.Route.After;
import org.jooby.Route.Definition;
import org.jooby.Route.Filter;
import org.jooby.Status;

import com.google.common.collect.Lists;
import com.google.common.reflect.Reflection;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import javaslang.control.Try;

/**
 * <h1>tests</h1>
 * <p>
 * In this section we are going to see how to run unit and integration tests in Jooby.
 * </p>
 *
 * <h2>unit tests</h2>
 * <p>
 * We do offer two programming models:
 * </p>
 * <ul>
 * <li>script programming model; and</li>
 * <li>mvc programming model</li>
 * </ul>
 *
 * <p>
 * You don't need much for <code>MVC</code> routes, because a route got binded to a method of some
 * class. So it is usually very easy and simple to mock and run unit tests against a
 * <code>MVC</code> route.
 * </p>
 *
 * <p>
 * We can't say the same for <code>script</code> routes, because a route is represented by a
 * <code>lambda</code> and there is no easy or simple way to get access to the lambda object.
 * </p>
 *
 * <p>
 * We do provide a {@link MockRouter} which simplify unit tests for <code>script routes</code>:
 * </p>
 *
 * <h3>usage</h3>
 *
 * <pre>{@code
 * public class MyApp extends Jooby {
 *   {
 *     get("/test", () -> "Hello unit tests!");
 *   }
 * }
 * }</pre>
 *
 * <p>
 * A unit test for this route, looks like:
 * </p>
 *
 * <pre>
 * &#64;Test
 * public void simpleTest() {
 *   String result = new MockRouter(new MyApp())
 *      .get("/test");
 *   assertEquals("Hello unit tests!", result);
 * }</pre>
 *
 * <p>
 * Just create a new instance of {@link MockRouter} with your application and call one of the HTTP
 * method, like <code>get</code>, <code>post</code>, etc...
 * </p>
 *
 * <h3>mocks</h3>
 * <p>
 * You're free to choose the mock library of your choice. Here is an example using
 * <a href="http://easymock.org">EasyMock</a>:
 * </p>
 *
 * <pre>{@code
 * {
 *   get("/mock", req -> {
 *     return req.path();
 *   });
 * }
 * }</pre>
 *
 * <p>
 * A test with <a href="http://easymock.org">EasyMock</a> looks like:
 * </p>
 * <pre>
 *
 * &#64;Test
 * public void shouldGetRequestPath() {
 *   Request req = EasyMock.createMock(Request.class);
 *   expect(req.path()).andReturn("/mypath");
 *
 *   EasyMock.replay(req);
 *
 *   String result = new MockRouter(new MyApp(), req)
 *      .get("/mock");
 *
 *   assertEquals("/mypath", result);
 *
 *   EasyMock.verify(req);
 * }
 * </pre>
 *
 * <p>
 * You can mock a {@link Response} object in the same way:
 * </p>
 *
 * <pre>{@code
 * {
 *   get("/mock", (req, rsp) -> {
 *     rsp.send("OK");
 *   });
 * }
 * }</pre>
 *
 * <p>
 * A test with <a href="http://easymock.org">EasyMock</a> looks like:
 * </p>
 * <pre>
 *
 * &#64;Test
 * public void shouldUseResponseSend() {
 *   Request req = EasyMock.createMock(Request.class);
 *   Response rsp = EasyMock.createMock(Response.class);
 *   rsp.send("OK");
 *
 *   EasyMock.replay(req, rsp);
 *
 *   String result = new MockRouter(new MyApp(), req, rsp)
 *      .get("/mock");
 *
 *   assertEquals("OK", result);
 *
 *   EasyMock.verify(req, rsp);
 * }
 * </pre>
 *
 * <p>
 * What about external dependencies? It works in a similar way:
 * </p>
 *
 * <pre>{@code
 * {
 *   get("/", () -> {
 *     HelloService service = require(HelloService.class);
 *     return service.salute();
 *   });
 * }
 * }</pre>
 *
 * <pre>
 * &#64;Test
 * public void shouldMockExternalDependencies() {
 *   HelloService service = EasyMock.createMock(HelloService.class);
 *   expect(service.salute()).andReturn("Hola!");
 *
 *   EasyMock.replay(service);
 *
 *   String result = new MockRouter(new MyApp())
 *      .set(service)
 *      .get("/");
 *
 *   assertEquals("Hola!", result);
 *
 *   EasyMock.verify(service);
 * }
 * </pre>
 *
 * <p>
 * The {@link #set(Object)} call push and register an external dependency (usually a mock). This
 * make it possible to resolve services from <code>require</code> calls.
 * </p>
 *
 * <h3>deferred</h3>
 * <p>
 * Mock of promises are possible too:
 * </p>
 *
 * <pre>{@code
 * {
 *   get("/", promise(deferred -> {
 *     deferred.resolve("OK");
 *   }));
 * }
 * }</pre>
 *
 * <pre>
 * &#64;Test
 * public void shouldMockPromises() {
 *
 *   String result = new MockRouter(new MyApp())
 *      .get("/");
 *
 *   assertEquals("OK", result);
 * }
 * </pre>
 *
 * Previous test works for deferred routes:
 *
 * <pre>{@code
 * {
 *   get("/", deferred(() -> {
 *     return "OK";
 *   }));
 * }
 * }</pre>
 *
 * @author edgar
 */
public class MockRouter {

  private static final Route.Chain NOOP_CHAIN = (req, rsp, next) -> {
  };

  private static class MockResponse extends Response.Forwarding {

    List<Route.After> afterList = new ArrayList<>();
    private AtomicReference<Object> ref;

    public MockResponse(final Response response, final AtomicReference<Object> ref) {
      super(response);
      this.ref = ref;
    }

    @Override
    public void after(final After handler) {
      afterList.add(handler);
    }

    @Override
    public void send(final Object result) throws Throwable {
      rsp.send(result);
      ref.set(result);
    }

    @Override
    public void send(final Result result) throws Throwable {
      rsp.send(result);
      ref.set(result);
    }

  }

  private static final int CLEAN_STACK = 4;

  @SuppressWarnings("rawtypes")
  private Map<Key, Object> registry = new HashMap<>();

  private List<Definition> routes;

  private Request req;

  private Response rsp;

  public MockRouter(final Jooby app) {
    this(app, empty(Request.class), empty(Response.class));
  }

  public MockRouter(final Jooby app, final Request req) {
    this(app, req, empty(Response.class));
  }

  public MockRouter(final Jooby app, final Request req, final Response rsp) {
    this.routes = Jooby.exportRoutes(hackInjector(app));
    this.req = req;
    this.rsp = rsp;
  }

  public MockRouter set(final Object dependency) {
    return set(null, dependency);
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  public MockRouter set(final String name, final Object object) {
    traverse(object.getClass(), type -> {
      Object key = Optional.ofNullable(name)
          .map(it -> Key.get(type, Names.named(name)))
          .orElseGet(() -> Key.get(type));
      registry.putIfAbsent((Key) key, object);
    });
    return this;
  }

  public <T> T get(final String path) throws Throwable {
    return execute(Route.GET, path);
  }

  public <T> T post(final String path) throws Throwable {
    return execute(Route.POST, path);
  }

  public <T> T put(final String path) throws Throwable {
    return execute(Route.PUT, path);
  }

  public <T> T patch(final String path) throws Throwable {
    return execute(Route.PATCH, path);
  }

  public <T> T delete(final String path) throws Throwable {
    return execute(Route.DELETE, path);
  }

  public <T> T execute(final String method, final String path) throws Throwable {
    return execute(method, path, MediaType.all, MediaType.all);
  }

  @SuppressWarnings("unchecked")
  private <T> T execute(final String method, final String path, final MediaType contentType,
      final MediaType... accept) throws Throwable {
    List<Filter> filters = pipeline(method, path, contentType, Arrays.asList(accept));
    if (filters.isEmpty()) {
      throw new Err(Status.NOT_FOUND, path);
    }
    Iterator<Filter> pipeline = filters.iterator();
    AtomicReference<Object> ref = new AtomicReference<>();
    MockResponse rsp = new MockResponse(this.rsp, ref);
    while (ref.get() == null && pipeline.hasNext()) {
      Filter next = pipeline.next();
      if (next instanceof Route.ZeroArgHandler) {
        ref.set(((Route.ZeroArgHandler) next).handle());
      } else if (next instanceof Route.OneArgHandler) {
        ref.set(((Route.OneArgHandler) next).handle(req));
      } else if (next instanceof Route.Handler) {
        ((Route.Handler) next).handle(req, rsp);
      } else {
        next.handle(req, rsp, NOOP_CHAIN);
      }
    }
    Object lastResult = ref.get();
    // after callbacks:
    if (rsp.afterList.size() > 0) {
      Result result = wrap(lastResult);
      for (int i = rsp.afterList.size() - 1; i >= 0; i--) {
        result = rsp.afterList.get(i).handle(req, rsp, result);
      }
      if (Result.class.isInstance(lastResult)) {
        return (T) result;
      }
      return result.get();
    }

    // deferred results:
    if (lastResult instanceof Deferred) {
      Deferred deferred = ((Deferred) lastResult);
      // execute deferred code:
      deferred.handler(req, (v, x) -> {
      });
      // get result
      lastResult = deferred.get();
      if (Throwable.class.isInstance(lastResult)) {
        throw (Throwable) lastResult;
      }
    }
    return (T) lastResult;
  }

  private Result wrap(final Object value) {
    if (value instanceof Result) {
      return (Result) value;
    }
    return Results.with(value);
  }

  private List<Route.Filter> pipeline(final String method, final String path,
      final MediaType contentType,
      final List<MediaType> accept) {
    List<Route.Filter> routes = new ArrayList<>();
    for (Route.Definition routeDef : this.routes) {
      Optional<Route> route = routeDef.matches(method, path, contentType, accept);
      if (route.isPresent()) {
        routes.add(routeDef.filter());
      }
    }
    return routes;
  }

  private Jooby hackInjector(final Jooby app) {
    Try.run(() -> {
      Field field = Jooby.class.getDeclaredField("injector");
      field.setAccessible(true);
      Injector injector = proxyInjector(getClass().getClassLoader(), registry);
      field.set(app, injector);
      registry.put(Key.get(Injector.class), injector);
    }).get();
    return app;
  }

  @SuppressWarnings("rawtypes")
  private static Injector proxyInjector(final ClassLoader loader, final Map<Key, Object> registry) {
    return Reflection.newProxy(Injector.class, (proxy, method, args) -> {
      if (method.getName().equals("getInstance")) {
        Key key = (Key) args[0];
        Object value = registry.get(key);
        if (value == null) {
          Object type = key.getAnnotation() != null ? key : key.getTypeLiteral();
          IllegalStateException iex = new IllegalStateException("Not found: " + type);
          // Skip proxy and some useless lines:
          Try.of(() -> {
            StackTraceElement[] stacktrace = iex.getStackTrace();
            return Lists.newArrayList(stacktrace).subList(CLEAN_STACK, stacktrace.length);
          }).onSuccess(stacktrace -> iex
              .setStackTrace(stacktrace.toArray(new StackTraceElement[stacktrace.size()])));
          throw iex;
        }
        return value;
      }
      throw new UnsupportedOperationException(method.toString());
    });
  }

  @SuppressWarnings("rawtypes")
  private void traverse(final Class type, final Consumer<Class> set) {
    if (type != Object.class) {
      set.accept(type);
      Optional.ofNullable(type.getSuperclass()).ifPresent(it -> traverse(it, set));
      Arrays.asList(type.getInterfaces()).forEach(it -> traverse(it, set));
    }
  }

  private static <T> T empty(final Class<T> type) {
    return Reflection.newProxy(type, (proxy, method, args) -> {
      throw new UnsupportedOperationException(method.toString());
    });
  }

}
