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

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

import org.jooby.Route.Mapper;
import org.jooby.handlers.AssetHandler;

/**
 * Route DSL. Constructs and creates several flavors of jooby routes.
 *
 * @author edgar
 * @since 0.16.0
 */
public interface Router {

  /**
   * Import content from provide application (routes, parsers/renderers, start/stop callbacks, ...
   * etc.).
   *
   * @param app Routes provider.
   * @return This jooby instance.
   */
  Router use(final Jooby app);

  /**
   * Import content from provide application (routes, parsers/renderers, start/stop callbacks, ...
   * etc.). Routes will be mounted at the provided path.
   *
   * @param path Path to mount the given app.
   * @param app Routes provider.
   * @return This jooby instance.
   */
  Router use(final String path, final Jooby app);

  /**
   * Define one or more routes under the same namespace:
   *
   * <pre>
   * {
   *   use("/pets")
   *     .get("/:id", req {@literal ->} db.get(req.param("id").value()))
   *     .get(() {@literal ->} db.values());
   * }
   * </pre>
   *
   * @param pattern Global pattern to use.
   * @return A route namespace.
   */
  Route.Group use(String pattern);

  /**
   * Append a new filter that matches any method under the given path.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Definition use(String path, Route.Filter filter);

  /**
   * Append a new filter that matches the given method and path.
   *
   * @param method A HTTP method.
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Definition use(String method, String path, Route.Filter filter);

  /**
   * Append a new route handler that matches the given method and path. Like
   * {@link #use(String, String, org.jooby.Route.Filter)} but you don't have to explicitly call
   * {@link Route.Chain#next(Request, Response)}.
   *
   * @param method A HTTP method.
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition use(String method, String path, Route.Handler handler);

  /**
   * Append a new route handler that matches any method under the given path. Like
   * {@link #use(String, org.jooby.Route.Filter)} but you don't have to explicitly call
   * {@link Route.Chain#next(Request, Response)}.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition use(String path, Route.Handler handler);

  /**
   * Append a new route handler that matches any method under the given path.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition use(String path, Route.OneArgHandler handler);

  /**
   * Append a route that matches the HTTP GET method:
   *
   * <pre>
   *   get("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition get(String path, Route.Handler handler);

  /**
   * Append two routes that matches the HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/mode/:id", (req, rsp) {@literal ->} {
   *     rsp.send(req.param("id").toOptional(String.class));
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection get(String path1, String path2, Route.Handler handler);

  /**
   * Append three routes that matches the HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", (req, rsp) {@literal ->} {
   *     rsp.send(req.path());
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection get(String path1, String path2, String path3, Route.Handler handler);

  /**
   * Append route that matches the HTTP GET method:
   *
   * <pre>
   *   get("/", req {@literal ->} {
   *     return "hello";
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition get(String path, Route.OneArgHandler handler);

  /**
   * Append three routes that matches the HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/model/:id", req {@literal ->} {
   *     return req.param("id").toOptional(String.class);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection get(String path1, String path2, Route.OneArgHandler handler);

  /**
   * Append three routes that matches the HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection get(String path1, String path2, String path3, Route.OneArgHandler handler);

  /**
   * Append route that matches HTTP GET method:
   *
   * <pre>
   *   get("/", () {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition get(String path, Route.ZeroArgHandler handler);

  /**
   * Append three routes that matches the HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", () {@literal ->} {
   *     return "OK";
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection get(String path1, String path2, Route.ZeroArgHandler handler);

  /**
   * Append three routes that matches HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", () {@literal ->} {
   *     return "OK";
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection get(String path1, String path2, String path3, Route.ZeroArgHandler handler);

  /**
   * Append a filter that matches HTTP GET method:
   *
   * <pre>
   *   get("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Definition get(String path, Route.Filter filter);

  /**
   * Append three routes that matches the HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/model/:id", (req, rsp, chain) {@literal ->} {
   *     req.param("id").toOptional(String.class);
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection get(String path1, String path2, Route.Filter filter);

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection get(String path1, String path2, String path3, Route.Filter filter);

  /**
   * Append a route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition post(String path, Route.Handler handler);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", (req, rsp) {@literal ->} {
   *     rsp.send(req.path());
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection post(String path1, String path2, Route.Handler handler);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", (req, rsp) {@literal ->} {
   *     rsp.send(req.path());
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection post(String path1, String path2, String path3, Route.Handler handler);

  /**
   * Append route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", req {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition post(String path, Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection post(String path1, String path2, Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection post(String path1, String path2, String path3, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", () {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition post(String path, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", {@literal ->} {
   *     return "OK";
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection post(String path1, String path2, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", () {@literal ->} {
   *     return "OK";
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection post(String path1, String path2, String path3, Route.ZeroArgHandler handler);

  /**
   * Append a route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Definition post(String path, Route.Filter filter);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2",(req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection post(String path1, String path2, Route.Filter filter);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection post(String path1, String path2, String path3, Route.Filter filter);

  /**
   * Append a route that supports HTTP HEAD method:
   *
   * <pre>
   *   post("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition head(String path, Route.Handler handler);

  /**
   * Append route that supports HTTP HEAD method:
   *
   * <pre>
   *   head("/", req {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition head(String path, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP HEAD method:
   *
   * <pre>
   *   head("/", () {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition head(String path, Route.ZeroArgHandler handler);

  /**
   * Append a route that supports HTTP HEAD method:
   *
   * <pre>
   *   post("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Definition head(String path, Route.Filter filter);

  /**
   * Append a new route that automatically handles HEAD request from existing GET routes.
   *
   * <pre>
   * {
   *   head();
   * }
   * </pre>
   *
   * @return A new route definition.
   */
  Route.Definition head();

  /**
   * Append a route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", (req, rsp) {@literal ->} {
   *     rsp.header("Allow", "GET, POST");
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition options(String path, Route.Handler handler);

  /**
   * Append route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", req {@literal ->}
   *     return Results.with(200).header("Allow", "GET, POST")
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition options(String path, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", () {@literal ->}
   *     return Results.with(200).header("Allow", "GET, POST")
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition options(String path, Route.ZeroArgHandler handler);

  /**
   * Append a route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", (req, rsp, chain) {@literal ->} {
   *     rsp.header("Allow", "GET, POST");
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  Route.Definition options(String path, Route.Filter filter);

  /**
   * Append a new route that automatically handles OPTIONS requests.
   *
   * <pre>
   *   get("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   *
   *   post("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   *
   *   options("/");
   * </pre>
   *
   * <code>OPTIONS /</code> produces a response with a Allow header set to: GET, POST.
   *
   * @return A new route definition.
   */
  Route.Definition options();

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A route to execute.
   * @return A new route definition.
   */
  Route.Definition put(String path, Route.Handler handler);

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", (req, rsp) {@literal ->} {
   *     rsp.send(req.path());
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection put(String path1, String path2, Route.Handler handler);

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", "/p3", (req, rsp) {@literal ->} {
   *     rsp.send(req.path());
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection put(String path1, String path2, String path3, Route.Handler handler);

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", req {@literal ->}
   *    return Results.accepted();
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition put(String path, Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection put(String path1, String path2,
      Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection put(String path1, String path2, String path3, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", () {@literal ->} {
   *     return Results.accepted()
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition put(String path, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection put(String path1, String path2, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection put(String path1, String path2, String path3, Route.ZeroArgHandler handler);

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  Route.Definition put(String path, Route.Filter filter);

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection put(String path1, String path2, Route.Filter filter);

  /**
   * Append three routes that supports HTTP PUT method on the same handler:
   *
   * <pre>
   *   put("/p1", "/p2", "/p3", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection put(String path1, String path2, String path3, Route.Filter filter);

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A route to execute.
   * @return A new route definition.
   */
  Route.Definition patch(String path, Route.Handler handler);

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection patch(String path1, String path2, Route.Handler handler);

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", "/p3", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection patch(String path1, String path2, String path3, Route.Handler handler);

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", req {@literal ->}
   *    Results.ok()
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition patch(String path, Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection patch(String path1, String path2, Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection patch(String path1, String path2, String path3, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", () {@literal ->} {
   *     return Results.ok();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition patch(String path, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", () {@literal ->} {
   *     return Results.ok();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection patch(String path1, String path2, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", "/p3", () {@literal ->} {
   *     return Results.ok();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection patch(String path1, String path2, String path3, Route.ZeroArgHandler handler);

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  Route.Definition patch(String path, Route.Filter filter);

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection patch(String path1, String path2, Route.Filter filter);

  /**
   * Append three routes that supports HTTP PATCH method on the same handler:
   *
   * <pre>
   *   patch("/p1", "/p2", "/p3", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection patch(String path1, String path2, String path3, Route.Filter filter);

  /**
   * Append a route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", (req, rsp) {@literal ->} {
   *     rsp.status(204);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition delete(String path, Route.Handler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", (req, rsp) {@literal ->} {
   *     rsp.status(204);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection delete(String path1, String path2, Route.Handler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", (req, rsp) {@literal ->} {
   *     rsp.status(204);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection delete(String path1, String path2, String path3, Route.Handler handler);

  /**
   * Append route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", req {@literal ->}
   *     return Results.noContent();
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition delete(String path, Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", req {@literal ->} {
   *     return Results.noContent();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection delete(String path1, String path2, Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3",req {@literal ->} {
   *     return Results.noContent();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection delete(String path1, String path2, String path3, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", () {@literal ->}
   *     return Results.noContent();
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition delete(String path, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", () {@literal ->} {
   *     return Results.noContent();
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection delete(String path1, String path2, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", req {@literal ->} {
   *     return Results.noContent();
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Collection delete(String path1, String path2, String path3, Route.ZeroArgHandler handler);

  /**
   * Append a route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", (req, rsp, chain) {@literal ->} {
   *     rsp.status(304);
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  Route.Definition delete(String path, Route.Filter filter);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", (req, rsp, chain) {@literal ->} {
   *     rsp.status(304);
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection delete(String path1, String path2, Route.Filter filter);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", (req, rsp, chain) {@literal ->} {
   *     rsp.status(304);
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param path3 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection delete(String path1,
      String path2, String path3,
      Route.Filter filter);

  /**
   * Append a route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", (req, rsp) {@literal ->} {
   *     rsp.send(...);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A callback to execute.
   * @return A new route definition.
   */
  Route.Definition trace(String path, Route.Handler handler);

  /**
   * Append route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", req {@literal ->}
   *     "trace"
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition trace(String path, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", () {@literal ->}
   *     "trace"
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition trace(String path, Route.ZeroArgHandler handler);

  /**
   * Append a route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  Route.Definition trace(String path, Route.Filter filter);

  /**
   * Append a default trace implementation under the given path. Default trace response, looks
   * like:
   *
   * <pre>
   *  TRACE /path
   *     header1: value
   *     header2: value
   * </pre>
   *
   * @return A new route definition.
   */
  Route.Definition trace();

  /**
   * Append a route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", (req, rsp) {@literal ->} {
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition connect(String path, Route.Handler handler);

  /**
   * Append route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", req {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition connect(String path, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", () {@literal ->}
   *     "connected"
   *   );
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition connect(String path, Route.ZeroArgHandler handler);

  /**
   * Append a route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Definition connect(String path, Route.Filter filter);

  /**
   * Static files handler.
   *
   * <pre>
   *   assets("/assets/**");
   * </pre>
   *
   * Resources are served from root of classpath, for example <code>GET /assets/file.js</code> will
   * be resolve as classpath resource at the same location.
   *
   * The {@link AssetHandler} one step forward and add support for serving files from a CDN out of
   * the box. All you have to do is to define a <code>assets.cdn</code> property:
   *
   * <pre>
   * assets.cdn = "http://d7471vfo50fqt.cloudfront.net"
   * </pre>
   *
   * A GET to <code>/assets/js/index.js</code> will be redirected to:
   * <code>http://d7471vfo50fqt.cloudfront.net/assets/js/index.js</code>.
   *
   * You can turn on/off <code>ETag</code> and <code>Last-Modified</code> headers too using
   * <code>assets.etag</code> and <code>assets.lastModified</code>. These two properties are enabled
   * by default.
   *
   * @param path The path to publish.
   * @return A new route definition.
   */
  Route.Definition assets(String path);

  /**
   * Static files handler. Like {@link #assets(String)} but let you specify a different classpath
   * location.
   *
   * <p>
   * Basic example
   * </p>
   *
   * <pre>
   *   assets("/js/**", "/");
   * </pre>
   *
   * A request for: <code>/js/jquery.js</code> will be translated to: <code>/lib/jquery.js</code>.
   *
   * <p>
   * Webjars example:
   * </p>
   *
   * <pre>
   *   assets("/js/**", "/resources/webjars/{0}");
   * </pre>
   *
   * A request for: <code>/js/jquery/2.1.3/jquery.js</code> will be translated to:
   * <code>/resources/webjars/jquery/2.1.3/jquery.js</code>.
   * The <code>{0}</code> represent the <code>**</code> capturing group.
   *
   * <p>
   * Another webjars example:
   * </p>
   *
   * <pre>
   *   assets("/js/*-*.js", "/resources/webjars/{0}/{1}/{0}.js");
   * </pre>
   *
   * <p>
   * A request for: <code>/js/jquery-2.1.3.js</code> will be translated to:
   * <code>/resources/webjars/jquery/2.1.3/jquery.js</code>.
   * </p>
   *
   * @param path The path to publish.
   * @param location A resource location.
   * @return A new route definition.
   */
  Route.Definition assets(String path, String location);

  /**
   * Send static files, like {@link #assets(String)} but let you specify a custom
   * {@link AssetHandler}.
   *
   * @param path The path to publish.
   * @param handler Asset handler.
   * @return A new route definition.
   */
  Route.Definition assets(String path, AssetHandler handler);

  /**
   * <p>
   * Append MVC routes from a controller like class:
   * </p>
   *
   * <pre>
   *   use(MyRoute.class);
   * </pre>
   *
   * Where MyRoute.java is:
   *
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
   * @param routeClass A route(s) class.
   * @return This jooby instance.
   */
  Route.Collection use(Class<?> routeClass);

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
   * @param handler Before handler.
   * @param chain Chain of before handler.
   * @return A new route definition.
   */
  default Route.Collection before(final Route.Before handler, final Route.Before... chain) {
    return before("*", handler, chain);
  }

  /**
   * <h2>before</h2>
   *
   * Allows for customized handler execution chains. It will be invoked before the actual handler.
   *
   * <pre>{@code
   * {
   *   before("*", (req, rsp) -> {
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
   *   use("*", (req, rsp, chain) -> {
   *     before(req, rsp);
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
   *   before("/path", (req, rsp) -> {
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
   * @param pattern Pattern to intercept.
   * @param handler Before handler.
   * @param chain Chain of before handler.
   * @return A new route definition.
   */
  default Route.Collection before(final String pattern, final Route.Before handler,
      final Route.Before... chain) {
    return before("*", pattern, handler, chain);
  }

  /**
   * <h2>before</h2>
   *
   * Allows for customized handler execution chains. It will be invoked before the actual handler.
   *
   * <pre>{@code
   * {
   *   before("GET", "*", (req, rsp) -> {
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
   *   use("GET", "*", (req, rsp, chain) -> {
   *     before(req, rsp);
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
   *   before("GET", "/path", (req, rsp) -> {
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
   * @param method HTTP method to intercept.
   * @param pattern Pattern to intercept.
   * @param handler Before handler.
   * @param chain Chain of before handler.
   * @return A new route definition.
   */
  Route.Collection before(String method, String pattern, Route.Before handler,
      Route.Before... chain);

  /**
   * <h2>after</h2>
   *
   * Allows for customized response before sending it. It will be invoked at the time a response
   * need
   * to be send.
   *
   * <pre>{@code
   * {
   *   after((req, rsp, result) -> {
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
   *   use("*", (req, rsp, chain) -> {
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
   *   after((req, rsp, result) -> {
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
   * @param handler After handler.
   * @param chain After chain.
   * @return A new route definition.
   */
  default Route.Collection after(final Route.After handler, final Route.After... chain) {
    return after("*", handler, chain);
  }

  /**
   * <h2>after</h2>
   *
   * Allows for customized response before sending it. It will be invoked at the time a response
   * need to be send.
   *
   * <pre>{@code
   * {
   *   before("*", (req, rsp, result) -> {
   *     // your code goes here
   *     return result;
   *   });
   * }
   * }</pre>
   *
   * You are allowed to modify the request, response and result objects. The handler returns a
   * {@link Result} which can be the same or an entirely new {@link Result}.
   *
   * Please note that the <code>after</code> handler is just syntax sugar for {@link Route.Filter}.
   * For example, the <code>after</code> handler was implemented as:
   *
   * <pre>{@code
   * {
   *   use("*", (req, rsp, chain) -> {
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
   *   after("/path", (req, rsp, result) -> {
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
   * @param pattern Pattern to intercept.
   * @param handler After handler.
   * @param chain After chain.
   * @return A new route definition.
   */
  default Route.Collection after(final String pattern, final Route.After handler,
      final Route.After... chain) {
    return after("*", pattern, handler, chain);
  }

  /**
   * <h2>after</h2>
   *
   * Allows for customized response before sending it. It will be invoked at the time a response
   * need to be send.
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
   * @param method HTTP method to intercept.
   * @param pattern Pattern to intercept.
   * @param handler After handler.
   * @param chain After chain.
   * @return A new route definition.
   */
  Route.Collection after(String method, String pattern, Route.After handler, Route.After... chain);

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
   * The goal of the <code>after</code> handler is to probably cleanup request object and log
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
   * @param handler Complete handler.
   * @param chain Complete chain.
   * @return A new route definition.
   */
  default Route.Collection complete(final Route.Complete handler, final Route.Complete... chain) {
    return complete("*", handler, chain);
  }

  /**
   * <h2>complete</h2>
   *
   * Allows for log and cleanup a request. It will be invoked after we send a response.
   *
   * <pre>{@code
   * {
   *   complete("*", (req, rsp, cause) -> {
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
   *   complete("/path", (req, rsp, cause) -> {
   *     // your code goes here
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
   * <h2>example</h2>
   * <p>
   * Suppose you have a transactional resource, like a database connection. The next example shows
   * you how to implement a simple and effective <code>transaction-per-request</code> pattern:
   * </p>
   *
   * <pre>{@code
   * {
   *   // start transaction
   *   before("/api/*", (req, rsp) -> {
   *     DataSource ds = req.require(DataSource.class);
   *     Connection connection = ds.getConnection();
   *     Transaction trx = connection.getTransaction();
   *     trx.begin();
   *     req.set("connection", connection);
   *     return true;
   *   });
   *
   *   // commit/rollback transaction
   *   complete("/api/*", (req, rsp, cause) -> {
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
   * @param pattern Pattern to intercept.
   * @param handler Complete handler.
   * @param chain Complete chain.
   * @return A new route definition.
   */
  default Route.Collection complete(final String pattern, final Route.Complete handler,
      final Route.Complete... chain) {
    return complete("*", pattern, handler, chain);
  }

  /**
   * <h2>after</h2>
   *
   * Allows for log and cleanup a request. It will be invoked after we send a response.
   *
   * <pre>{@code
   * {
   *   complete("*", "*", (req, rsp, cause) -> {
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
   *   complete("*", "/path", (req, rsp, cause) -> {
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
   *     try(Connection connection = req.unset("connection")) {
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
   *   get("/my-trx-route", req -> {
   *     Connection connection = req.get("connection");
   *     // work with connection
   *   });
   * }
   * }</pre>
   *
   * @param method HTTP method to intercept.
   * @param pattern Pattern to intercept.
   * @param handler Complete handler.
   * @param chain Complete chain.
   * @return A new route definition.
   */
  Route.Collection complete(String method, String pattern, Route.Complete handler,
      Route.Complete... chain);

  /**
   * Append a new WebSocket handler under the given path.
   *
   * <pre>
   *   ws("/ws", (socket) {@literal ->} {
   *     // connected
   *     socket.onMessage(message {@literal ->} {
   *       System.out.println(message);
   *     });
   *     socket.send("Connected"):
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A connect callback.
   * @return A new WebSocket definition.
   */
  default WebSocket.Definition ws(final String path, final WebSocket.Handler handler) {
    return ws(path, (WebSocket.FullHandler) handler);
  }

  /**
   * Append a new WebSocket handler under the given path.
   *
   * <pre>
   *   ws("/ws", (req, socket) {@literal ->} {
   *     // connected
   *     socket.onMessage(message {@literal ->} {
   *       System.out.println(message);
   *     });
   *     socket.send("Connected"):
   *   });
   * </pre>
   *
   * @param path A path pattern.
   * @param handler A connect callback.
   * @return A new WebSocket definition.
   */
  WebSocket.Definition ws(String path, WebSocket.FullHandler handler);

  /**
   * Add a server-sent event handler.
   *
   * <pre>{@code
   * {
   *   sse("/path",(req, sse) -> {
   *     // 1. connected
   *     sse.send("data"); // 2. send/push data
   *   });
   * }
   * }</pre>
   *
   * @param path Event path.
   * @param handler Callback. It might executed in a different thread (web server choice).
   * @return A route definition.
   */
  Route.Definition sse(String path, Sse.Handler handler);

  /**
   * Add a server-sent event handler.
   *
   * <pre>{@code
   * {
   *   sse("/path", sse -> {
   *     // 1. connected
   *     sse.send("data"); // 2. send/push data
   *   });
   * }
   * }</pre>
   *
   * @param path Event path.
   * @param handler Callback. It might executed in a different thread (web server choice).
   * @return A route definition.
   */
  Route.Definition sse(String path, Sse.Handler1 handler);

  /**
   * Apply common configuration and attributes to a group of routes:
   *
   * <pre>{@code
   * {
   *   with(() -> {
   *     get("/foo", ...);
   *
   *     get("/bar", ...);
   *
   *     get("/etc", ...);
   *
   *     ...
   *   }).attr("v1", "k1")
   *     .excludes("/public/**");
   * }
   * }</pre>
   *
   * All the routes wrapped by <code>with</code> will have a <code>v1</code> attribute and will
   * excludes/ignores a <code>/public</code> request.
   *
   * @param callback Route callback.
   * @return A route collection.
   */
  Route.Collection with(Runnable callback);

  /**
   * Apply the mapper to all the functional routes.
   *
   * <pre>{@code
   * {
   *   mapper((Integer v) -> v * 2);
   *
   *   mapper(v -> Integer.parseInt(v.toString()));
   *
   *   get("/four", () -> "2");
   * }
   * }</pre>
   *
   * A call to <code>/four</code> outputs <code>4</code>. Mapper are applied in reverse order.
   *
   * @param mapper Route mapper to append.
   * @return This instance.
   */
  Router map(final Mapper<?> mapper);

  /**
   * Setup a route error handler. Default error handler {@link Err.DefHandler} does content
   * negotation and this method allow to override/complement default handler.
   *
   * This is a catch all error handler.
   *
   * <h2>html</h2>
   *
   * If a request has an Accept: <code>text/html</code> header. Then, the default err handler will
   * ask to a {@link View.Engine} to render the <code>err</code> view.
   *
   * The default model has these attributes:
   * <pre>
   * message: exception string
   * stacktrace: exception stack-trace as an array of string
   * status: status code, like 400
   * reason: status code reason, like BAD REQUEST
   * </pre>
   *
   * Here is a simply <code>public/err.html</code> error page:
   *
   * <pre>
   * &lt;html&gt;
   * &lt;body&gt;
   * {{ "{{status" }}}}:{{ "{{reason" }}}}
   * &lt;/body&gt;
   * &lt;/html&gt;
   * </pre>
   *
   * HTTP status code will be set too.
   *
   * @param err A route error handler.
   * @return This jooby instance.
   */
  Router err(Err.Handler err);

  /**
   * Setup a custom error handler.The error handler will be executed if the current exception is an
   * instance of given type type.
   *
   * @param type Exception type. The error handler will be executed if the current exception is an
   *        instance of this type.
   * @param handler A route error handler.
   * @return This jooby instance.
   */
  default Router err(final Class<? extends Throwable> type, final Err.Handler handler) {
    return err((req, rsp, err) -> {
      Throwable cause = err.getCause();
      if (type.isInstance(cause)) {
        handler.handle(req, rsp, err);
      }
    });
  }

  /**
   * Setup a route error handler. The error handler will be executed if current status code matches
   * the one provided.
   *
   * @param statusCode The status code to match.
   * @param handler A route error handler.
   * @return This jooby instance.
   */
  default Router err(final int statusCode, final Err.Handler handler) {
    return err((req, rsp, err) -> {
      if (statusCode == err.statusCode()) {
        handler.handle(req, rsp, err);
      }
    });
  }

  /**
   * Setup a route error handler. The error handler will be executed if current status code matches
   * the one provided.
   *
   * @param code The status code to match.
   * @param handler A route error handler.
   * @return This jooby instance.
   */
  default Router err(final Status code, final Err.Handler handler) {
    return err((req, rsp, err) -> {
      if (code.value() == err.statusCode()) {
        handler.handle(req, rsp, err);
      }
    });
  }

  /**
   * Setup a route error handler. The error handler will be executed if current status code matches
   * the one provided.
   *
   * @param predicate Apply the error handler if the predicate evaluates to <code>true</code>.
   * @param handler A route error handler.
   * @return This jooby instance.
   */
  default Router err(final Predicate<Status> predicate, final Err.Handler handler) {
    return err((req, rsp, err) -> {
      if (predicate.test(Status.valueOf(err.statusCode()))) {
        handler.handle(req, rsp, err);
      }
    });
  }

  /**
   * Produces a deferred response, useful for async request processing. By default a
   * {@link Deferred} results run in the current thread.
   *
   * This is intentional because application code (your code) always run in a worker thread. There
   * is a thread pool of <code>100</code> worker threads, defined by the property:
   * <code>server.threads.Max</code>.
   *
   * That's why a {@link Deferred} result runs in the current thread (no need to use a new thread),
   * unless you want to apply a different thread model and or use a reactive/async library.
   *
   * As a final thought you might want to reduce the number of worker thread if you are going to a
   * build a full reactive/async application.
   *
   * <h2>usage</h2>
   *
   * <pre>
   * {
   *    get("/async", promise(deferred {@literal ->} {
   *      try {
   *        deferred.resolve(...); // success value
   *      } catch (Exception ex) {
   *        deferred.reject(ex); // error value
   *      }
   *    }));
   *  }
   * </pre>
   *
   * <p>
   * This method is useful for integrating reactive/async libraries. Here is an example on how to
   * use <a href="https://github.com/ReactiveX/RxJava">RxJava</a>:
   * </p>
   *
   * <pre>{@code
   * {
   *    get("/rx", promise(deferred -> {
   *      Observable.create(s -> {
   *      s.onNext(...);
   *      s.onCompleted();
   *    }).subscribeOn(Schedulers.computation())
   *      .subscribe(deferred::resolve, deferred::reject);
   *   }));
   * }
   * }</pre>
   *
   * <p>
   * This is just an example because there is a <a href="http://jooby.org/doc/rxjava">Rx module</a>.
   * </p>
   *
   * <p>
   * Checkout the {@link #deferred(org.jooby.Route.ZeroArgHandler)} methods to see how to use a
   * plain {@link Executor}.
   * </p>
   *
   * @param initializer Deferred initializer.
   * @return A new deferred handler.
   * @see Deferred
   */
  Route.OneArgHandler promise(Deferred.Initializer initializer);

  /**
   * Produces a deferred response, useful for async request processing. Like
   * {@link #promise(org.jooby.Deferred.Initializer)} but allow you to specify an {@link Executor}
   * to use. See {@link Jooby#executor(Executor)} and {@link Jooby#executor(String, Executor)}.
   *
   * <h2>usage</h2>
   *
   * <pre>
   * {
   *    executor("forkjoin", new ForkJoinPool());
   *
   *    get("/async", promise("forkjoin", deferred {@literal ->} {
   *      try {
   *        deferred.resolve(...); // success value
   *      } catch (Exception ex) {
   *        deferred.reject(ex); // error value
   *      }
   *    }));
   *  }
   * </pre>
   *
   * <p>
   * Checkout the {@link #deferred(org.jooby.Route.ZeroArgHandler)} methods to see how to use a
   * plain {@link Executor}.
   * </p>
   *
   * @param executor Executor to run the deferred.
   * @param initializer Deferred initializer.
   * @return A new deferred handler.
   * @see Deferred
   */
  Route.OneArgHandler promise(String executor, Deferred.Initializer initializer);

  /**
   * Produces a deferred response, useful for async request processing. Like
   * {@link #promise(org.jooby.Deferred.Initializer)} but give you access to {@link Request}.
   *
   * <h2>usage</h2>
   *
   * <pre>
   * {
   *    ExecutorService executor = ...;
   *
   *    get("/async", promise((req, deferred) {@literal ->} {
   *      executor.execute(() {@literal ->} {
   *        try {
   *          deferred.resolve(req.param("param").value()); // success value
   *        } catch (Exception ex) {
   *          deferred.reject(ex); // error value
   *        }
   *      });
   *    }));
   *  }
   * </pre>
   *
   * @param initializer Deferred initializer.
   * @return A new deferred handler.
   * @see Deferred
   */
  Route.OneArgHandler promise(Deferred.Initializer0 initializer);

  /**
   * Produces a deferred response, useful for async request processing. Like
   * {@link #promise(String, org.jooby.Deferred.Initializer)} but give you access to
   * {@link Request}.
   *
   * <h2>usage</h2>
   *
   * <pre>
   * {
   *    get("/async", promise("myexec", (req, deferred) {@literal ->} {
   *      // resolve a success value
   *      deferred.resolve(req.param("param").value());
   *    }));
   *  }
   * </pre>
   *
   * @param executor Executor to run the deferred.
   * @param initializer Deferred initializer.
   * @return A new deferred handler.
   * @see Deferred
   */
  Route.OneArgHandler promise(String executor, Deferred.Initializer0 initializer);

  /**
   * Functional version of {@link #promise(org.jooby.Deferred.Initializer)}. To use ideally with one
   * or more {@link Executor}:
   *
   * <pre>{@code
   * {
   *   executor("cached", Executors.newCachedExecutor());
   *
   *   get("/fork", deferred("cached", req -> {
   *     return req.param("value").value();
   *   }));
   * }
   * }</pre>
   *
   * This handler automatically {@link Deferred#resolve(Object)} or
   * {@link Deferred#reject(Throwable)} a route handler response.
   *
   * @param executor Executor to run the deferred.
   * @param handler Application block.
   * @return A new deferred handler.
   */
  default Route.ZeroArgHandler deferred(final String executor, final Route.OneArgHandler handler) {
    return () -> Deferred.deferred(executor, handler);
  }

  /**
   * Functional version of {@link #promise(org.jooby.Deferred.Initializer)}.
   *
   * Using the default executor (current thread):
   *
   * <pre>{@code
   * {
   *   get("/fork", deferred(req -> {
   *     return req.param("value").value();
   *   }));
   * }
   * }</pre>
   *
   * Using a custom executor:
   *
   * <pre>{@code
   * {
   *   executor(new ForkJoinPool());
   *
   *   get("/fork", deferred(req -> {
   *     return req.param("value").value();
   *   }));
   * }
   * }</pre>
   *
   * This handler automatically {@link Deferred#resolve(Object)} or
   * {@link Deferred#reject(Throwable)} a route handler response.
   *
   * @param handler Application block.
   * @return A new deferred handler.
   */
  default Route.ZeroArgHandler deferred(final Route.OneArgHandler handler) {
    return () -> Deferred.deferred(handler);
  }

  /**
   * Functional version of {@link #promise(org.jooby.Deferred.Initializer)}. To use ideally with one
   * or more {@link Executor}:
   *
   * <pre>{@code
   * {
   *   executor("cached", Executors.newCachedExecutor());
   *
   *   get("/fork", deferred("cached", () -> {
   *     return "OK";
   *   }));
   * }
   * }</pre>
   *
   * This handler automatically {@link Deferred#resolve(Object)} or
   * {@link Deferred#reject(Throwable)} a route handler response.
   *
   * @param executor Executor to run the deferred.
   * @param handler Application block.
   * @return A new deferred handler.
   */
  default Route.ZeroArgHandler deferred(final String executor, final Route.ZeroArgHandler handler) {
    return () -> Deferred.deferred(executor, handler);
  }

  /**
   * Functional version of {@link #promise(org.jooby.Deferred.Initializer)}.
   *
   * Using the default executor (current thread):
   *
   * <pre>{@code
   * {
   *   get("/fork", deferred(() -> {
   *     return req.param("value").value();
   *   }));
   * }
   * }</pre>
   *
   * Using a custom executor:
   *
   * <pre>{@code
   * {
   *   executor(new ForkJoinPool());
   *
   *   get("/fork", deferred(() -> {
   *     return req.param("value").value();
   *   }));
   * }
   * }</pre>
   *
   * This handler automatically {@link Deferred#resolve(Object)} or
   * {@link Deferred#reject(Throwable)} a route handler response.
   *
   * @param handler Application block.
   * @return A new deferred handler.
   */
  default Route.ZeroArgHandler deferred(final Route.ZeroArgHandler handler) {
    return () -> Deferred.deferred(handler);
  }
}
