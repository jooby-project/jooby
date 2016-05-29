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

import org.jooby.Route.Mapper;
import org.jooby.handlers.AssetHandler;

/**
 * Route DSL. Constructs and creates several flavors of jooby routes.
 *
 * @author edgar
 * @since 0.16.0
 */
public interface Routes {

  /**
   * Define one or more routes under the same namespace:
   *
   * <pre>
   * {
   *   use("/pets")
   *     .get("/{id}", req {@literal ->} db.get(req.param("id").value()))
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
   * Append a new filter that matches any method under the given path.
   *
   * @param verb A HTTP verb.
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Definition use(String verb, String path, Route.Filter filter);

  /**
   * Append a new route handler that matches any method under the given path.
   *
   * @param verb A HTTP verb.
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition use(String verb, String path, Route.Handler handler);

  /**
   * Append a new route handler that matches any method under the given path.
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
   * Append a route that supports HTTP GET method:
   *
   * <pre>
   *   get("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition get(String path, Route.Handler handler);

  /**
   * Append two routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/mode/:id", req {@literal ->} {
   *     return req.param("id").toOptional(String.class);
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
  Route.Collection get(String path1, String path2, Route.Handler handler);

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", req {@literal ->} {
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
  Route.Collection get(String path1, String path2, String path3, Route.Handler handler);

  /**
   * Append route that supports HTTP GET method:
   *
   * <pre>
   *   get("/", (req) {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition get(String path, Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/model/:id", req {@literal ->} {
   *     return req.param("id").toOptional(String.class);
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
  Route.Collection get(String path1, String path2, Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", req {@literal ->} {
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
  Route.Collection get(String path1, String path2, String path3, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP GET method:
   *
   * <pre>
   *   get("/", () {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition get(String path, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/model/:id", req {@literal ->} {
   *     return req.param("id").toOptional(String.class);
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
  Route.Collection get(String path1, String path2, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/p1", "/p2", "/p3", req {@literal ->} {
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
  Route.Collection get(String path1, String path2, String path3, Route.ZeroArgHandler handler);

  /**
   * Append a filter that supports HTTP GET method:
   *
   * <pre>
   *   get("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Definition get(String path, Route.Filter filter);

  /**
   * Append three routes that supports HTTP GET method on the same handler:
   *
   * <pre>
   *   get("/model", "/model/:id", req {@literal ->} {
   *     return req.param("id").toOptional(String.class);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
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
   *   get("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
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
   * This is a singleton route so make sure you don't share or use global variables.
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
   *   post("/p1", "/p2", req {@literal ->} {
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
  Route.Collection post(String path1, String path2, Route.Handler handler);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", req {@literal ->} {
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
  Route.Collection post(String path1, String path2, String path3, Route.Handler handler);

  /**
   * Append route that supports HTTP POST method:
   *
   * <pre>
   *   post("/", (req) {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
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
   * This is a singleton route so make sure you don't share or use global variables.
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
   * This is a singleton route so make sure you don't share or use global variables.
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
   * This is a singleton route so make sure you don't share or use global variables.
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
   *   post("/p1", "/p2", req {@literal ->} {
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
  Route.Collection post(String path1, String path2, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", req {@literal ->} {
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
   * This is a singleton route so make sure you don't share or use global variables.
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
   *   post("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection post(String path1, String path2,
      Route.Filter filter);

  /**
   * Append three routes that supports HTTP POST method on the same handler:
   *
   * <pre>
   *   post("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
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
  Route.Collection post(String path1, String path2,
      String path3, Route.Filter filter);

  /**
   * Append a route that supports HTTP HEAD method:
   *
   * <pre>
   *   post("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
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
   *   head("/", (req) {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition head(String path,
      Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP HEAD method:
   *
   * <pre>
   *   head("/", () {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition head(String path,
      Route.ZeroArgHandler handler);

  /**
   * Append a route that supports HTTP HEAD method:
   *
   * <pre>
   *   post("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Definition head(String path,
      Route.Filter filter);

  /**
   * Append a new route that automatically handles HEAD request from existing GET routes.
   *
   * <pre>
   *   get("/", (req, rsp) {@literal ->} {
   *     rsp.send(something); // This route provides default HEAD for this GET route.
   *   });
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
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition options(String path,
      Route.Handler handler);

  /**
   * Append route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", (req) {@literal ->}
   *     Body.status(200).header("Allow", "GET, POST")
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition options(String path,
      Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP OPTIONS method:
   *
   * <pre>
   *   options("/", () {@literal ->}
   *     Body.status(200).header("Allow", "GET, POST")
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition options(String path,
      Route.ZeroArgHandler handler);

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
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  Route.Definition options(String path,
      Route.Filter filter);

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
   * </pre>
   *
   * OPTINOS / produces a response with a Allow header set to: GET, POST.
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
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A route to execute.
   * @return A new route definition.
   */
  Route.Definition put(String path,
      Route.Handler handler);

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
  Route.Collection put(String path1, String path2,
      Route.Handler handler);

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
  Route.Collection put(String path1, String path2,
      String path3, Route.Handler handler);

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", (req) {@literal ->}
   *    Body.status(202)
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition put(String path,
      Route.OneArgHandler handler);

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
  Route.Collection put(String path1, String path2,
      String path3, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", () {@literal ->} {
   *     Body.status(202)
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition put(String path,
      Route.ZeroArgHandler handler);

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
  Route.Collection put(String path1, String path2,
      Route.ZeroArgHandler handler);

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
  Route.Collection put(String path1, String path2,
      String path3, Route.ZeroArgHandler handler);

  /**
   * Append route that supports HTTP PUT method:
   *
   * <pre>
   *   put("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  Route.Definition put(String path,
      Route.Filter filter);

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
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection put(String path1, String path2,
      Route.Filter filter);

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
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection put(String path1, String path2,
      String path3, Route.Filter filter);

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", (req, rsp) {@literal ->} {
   *     rsp.send(something);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A route to execute.
   * @return A new route definition.
   */
  Route.Definition patch(String path,
      Route.Handler handler);

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
  Route.Collection patch(String path1, String path2,
      Route.Handler handler);

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
  Route.Collection patch(String path1, String path2,
      String path3, Route.Handler handler);

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", (req) {@literal ->}
   *    Body.status(202)
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition patch(String path,
      Route.OneArgHandler handler);

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
  Route.Collection patch(String path1, String path2,
      Route.OneArgHandler handler);

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
  Route.Collection patch(String path1, String path2,
      String path3, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", () {@literal ->} {
   *     Body.status(202)
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition patch(String path,
      Route.ZeroArgHandler handler);

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
  Route.Collection patch(String path1, String path2,
      Route.ZeroArgHandler handler);

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
  Route.Collection patch(String path1, String path2,
      String path3, Route.ZeroArgHandler handler);

  /**
   * Append route that supports HTTP PATCH method:
   *
   * <pre>
   *   patch("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  Route.Definition patch(String path,
      Route.Filter filter);

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
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection patch(String path1, String path2,
      Route.Filter filter);

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
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection patch(String path1, String path2,
      String path3, Route.Filter filter);

  /**
   * Append a route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", (req, rsp) {@literal ->} {
   *     rsp.status(304);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition delete(String path,
      Route.Handler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", req {@literal ->} {
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
  Route.Collection delete(String path1,
      String path2,
      Route.Handler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", req {@literal ->} {
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
  Route.Collection delete(String path1, String path2, String path3, Route.Handler handler);

  /**
   * Append route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", (req) {@literal ->}
   *     Body.status(204)
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition delete(String path,
      Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", req {@literal ->} {
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
  Route.Collection delete(String path1, String path2, Route.OneArgHandler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", req {@literal ->} {
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
  Route.Collection delete(String path1, String path2, String path3, Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP DELETE method:
   *
   * <pre>
   *   delete("/", () {@literal ->}
   *     Body.status(204)
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition delete(String path,
      Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", req {@literal ->} {
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
  Route.Collection delete(String path1,
      String path2, Route.ZeroArgHandler handler);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", req {@literal ->} {
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
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  Route.Definition delete(String path,
      Route.Filter filter);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", req {@literal ->} {
   *     return req.path();
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path1 A path pattern.
   * @param path2 A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Collection delete(String path1,
      String path2, Route.Filter filter);

  /**
   * Append three routes that supports HTTP DELETE method on the same handler:
   *
   * <pre>
   *   delete("/p1", "/p2", "/p3", req {@literal ->} {
   *     return req.path();
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
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A callback to execute.
   * @return A new route definition.
   */
  Route.Definition trace(String path,
      Route.Handler handler);

  /**
   * Append route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", (req) {@literal ->}
   *     "trace"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition trace(String path,
      Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", () {@literal ->}
   *     "trace"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition trace(String path,
      Route.ZeroArgHandler handler);

  /**
   * Append a route that supports HTTP TRACE method:
   *
   * <pre>
   *   trace("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A callback to execute.
   * @return A new route definition.
   */
  Route.Definition trace(String path,
      Route.Filter filter);

  /**
   * Append a default trace implementation under the given path. Default trace response, looks
   * like:
   *
   * <pre>
   *  TRACE /path
   *     header1: value
   *     header2: value
   *
   * </pre>
   *
   * @return A new route definition.
   */
  Route.Definition trace();

  /**
   * Append a route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition connect(String path,
      Route.Handler handler);

  /**
   * Append route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", (req) {@literal ->}
   *     "hello"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition connect(String path,
      Route.OneArgHandler handler);

  /**
   * Append route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", () {@literal ->}
   *     "connected"
   *   );
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param handler A handler to execute.
   * @return A new route definition.
   */
  Route.Definition connect(String path,
      Route.ZeroArgHandler handler);

  /**
   * Append a route that supports HTTP CONNECT method:
   *
   * <pre>
   *   connect("/", (req, rsp, chain) {@literal ->} {
   *     chain.next(req, rsp);
   *   });
   * </pre>
   *
   * This is a singleton route so make sure you don't share or use global variables.
   *
   * @param path A path pattern.
   * @param filter A filter to execute.
   * @return A new route definition.
   */
  Route.Definition connect(String path,
      Route.Filter filter);

  /**
   * Send a static file.
   *
   * <pre>
   *   assets("/assets/**");
   * </pre>
   *
   * Resources are served from root of classpath, for example <code>GET /assets/file.js</code> will
   * be resolve as classpath resource at the same location.
   *
   * @param path The path to publish.
   * @return A new route definition.
   */
  Route.Definition assets(String path);

  /**
   * Send a static file.
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
   * Send a static file.
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
   * @param handler Asset handler.
   * @return A new route definition.
   */
  Route.Definition assets(String path, AssetHandler handler);

  /**
   * <p>
   * Append one or more routes defined in the given class.
   * </p>
   *
   * <pre>
   *   use(MyRoute.class);
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
  WebSocket.Definition ws(String path, WebSocket.Handler handler);

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
   * A call to <code>/four</code> outputs <code>4</code>.
   *
   * @param mapper Route mapper to append.
   * @return This instance.
   */
  Routes map(final Mapper<?> mapper);

  /**
   * Setup a route error handler. Default error handler {@link Err.DefHandler} does content
   * negotation and this method allow to override/complement default handler.
   *
   * @param err A route error handler.
   * @return This jooby instance.
   */
  Routes err(Err.Handler err);

  /**
   * Setup a route error handler.The error handler will be executed if the current exception is an
   * instance of this type.
   *
   * @param type Exception type. The error handler will be executed if the current exception is an
   *        instance of this type.
   * @param handler A route error handler.
   * @return This jooby instance.
   */
  default Routes err(final Class<? extends Throwable> type, final Err.Handler handler) {
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
  default Routes err(final int statusCode, final Err.Handler handler) {
    return err((req, rsp, err) -> {
      if (statusCode == err.statusCode()) {
        handler.handle(req, rsp, err);
      }
    });
  }

}
