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
package org.jooby.spec;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Route;
import org.jooby.internal.RouteMetadata;
import org.jooby.internal.mvc.RequestParamNameProviderImpl;
import org.jooby.internal.spec.AppCollector;
import org.jooby.internal.spec.Context;
import org.jooby.internal.spec.ContextImpl;
import org.jooby.internal.spec.DocCollector;
import org.jooby.internal.spec.ResponseTypeCollector;
import org.jooby.internal.spec.RouteCollector;
import org.jooby.internal.spec.RouteParamCollector;
import org.jooby.internal.spec.RouteParamImpl;
import org.jooby.internal.spec.RouteResponseImpl;
import org.jooby.internal.spec.RouteSpecImpl;
import org.jooby.internal.spec.SourceResolver;
import org.jooby.internal.spec.SourceResolverImpl;
import org.jooby.internal.spec.TypeResolverImpl;
import org.jooby.mvc.Body;
import org.jooby.mvc.Header;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.google.common.base.Throwables;
import com.typesafe.config.ConfigFactory;

/**
 * <p>
 * Process and collect {@link RouteSpec} from {@link Jooby} app.
 * </p>
 *
 * @author edgar
 * @since 0.15.0
 */
public class RouteProcessor {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  /**
   * Process a {@link Jooby} application and collect {@link RouteSpec}.
   *
   * @param app A jooby app to process.
   * @return List of route specs.
   */
  public List<RouteSpec> process(final Jooby app) {
    requireNonNull(app, "App is required.");
    return process(app, new File(System.getProperty("user.dir")).toPath());
  }

  /**
   * Process a {@link Jooby} application and collect {@link RouteSpec}.
   *
   * @param app A jooby app to process.
   * @param srcdir Basedir where source code is located. Useful for extracting doc.
   * @return List of route specs.
   */
  public List<RouteSpec> process(final Jooby app, final Path srcdir) {
    return processInternal(app, srcdir, null);
  }

  /**
   * Process a {@link Jooby} application and collect {@link RouteSpec}, but also save a compiled
   * version in the given outdir.
   *
   * @param app A jooby app to process.
   * @param srcdir Basedir where source code is located. Useful for extracting doc.
   * @param outdir Where to save the compiled route specs.
   * @return List of route specs.
   */
  public List<RouteSpec> compile(final Jooby app, final Path srcdir, final Path outdir) {
    requireNonNull(app, "App is required.");
    requireNonNull(srcdir, "Source dir is required.");
    requireNonNull(outdir, "Out dir is required.");

    return processInternal(app, srcdir, outdir);
  }

  /**
   * Process a {@link Jooby} application and collect {@link RouteSpec}.
   *
   * @param appClass A jooby class to process.
   * @param routes Routes to process.
   * @return List of route specs.
   */
  public List<RouteSpec> process(final Class<? extends Jooby> appClass,
      final List<Route.Definition> routes) {
    requireNonNull(appClass, "App class is required.");
    requireNonNull(routes, "Routes are required.");
    return processInternal(appClass, routes, new File(System.getProperty("user.dir")).toPath(),
        null);
  }

  /**
   * Process a {@link Jooby} application and collect {@link RouteSpec}.
   *
   * @param appClass A jooby class to process.
   * @param routes Routes to process.
   * @param srcdir Basedir where source code is located. Useful for extracting doc.
   * @return List of route specs.
   */
  public List<RouteSpec> process(final Class<? extends Jooby> appClass,
      final List<Route.Definition> routes, final Path srcdir) {
    requireNonNull(appClass, "App class is required.");
    requireNonNull(routes, "Routes are required.");
    requireNonNull(srcdir, "Source dir is required.");
    return processInternal(appClass, routes, srcdir, null);
  }

  /**
   * Process a {@link Jooby} application and collect {@link RouteSpec}.
   *
   * @param appClass A jooby app to process.
   * @param routes Routes to process.
   * @param srcdir Basedir where source code is located. Useful for extracting doc.
   * @param outdir Strategy where to save the compiled route specs.
   * @return List of route specs.
   */
  public List<RouteSpec> compile(final Class<? extends Jooby> appClass,
      final List<Route.Definition> routes, final Path srcdir, final Path outdir) {
    requireNonNull(appClass, "App class is required.");
    requireNonNull(routes, "Routes are required.");
    requireNonNull(srcdir, "Source dir is required.");
    requireNonNull(outdir, "Out dir is required.");
    return processInternal(appClass, routes, srcdir, outdir);
  }

  /**
   * Process a {@link Jooby} application and collect {@link RouteSpec}.
   *
   * @param app A jooby app to process.
   * @param srcdir Basedir where source code is located. Useful for extracting doc.
   * @param outdir Where to save the compiled route specs.
   * @return List of route specs.
   */
  private List<RouteSpec> processInternal(final Jooby app, final Path srcdir, final Path outdir) {
    List<Route.Definition> routes = Jooby.exportRoutes(app);
    return processInternal(app.getClass(), routes, srcdir, outdir);
  }

  /**
   * Process a {@link Jooby} application and collect {@link RouteSpec}.
   *
   * @param app A jooby app to process.
   * @param srcdir Basedir where source code is located. Useful for extracting doc.
   * @param outdir Strategy where to save the compiled route specs.
   * @return List of route specs.
   */
  @SuppressWarnings("unchecked")
  private List<RouteSpec> processInternal(final Class<? extends Jooby> appClass,
      final List<Route.Definition> routes, final Path srcdir, final Path outdir) {
    log.debug("processing {}.spec", appClass.getName());
    List<RouteSpec> specs = new ArrayList<>();
    try {
      /**
       * Source resolver.
       */
      SourceResolver src = new SourceResolverImpl(srcdir);

      /**
       * Context with type resolver.
       */
      Context ctx = new ContextImpl(new TypeResolverImpl(appClass.getClassLoader()), src);

      Optional<List<RouteSpec>> ifspecs = ctx.parseSpec(appClass);
      if (ifspecs.isPresent()) {
        // compiled version found.
        return ifspecs.get();
      }

      /**
       * Main AST
       */
      CompilationUnit unit = JavaParser.parse(src.resolveSource(appClass).get(), true);

      /**
       * Find out app node.
       */
      Node appNode = new AppCollector().accept(unit, ctx);

      /** Collect all routes and process them. */
      Set<String> owners = new HashSet<>();
      owners.add(appClass.getName());
      List<Entry<Object, Node>> routeNodes = new RouteCollector(owners::add).accept(appNode, ctx);

      int j = 0;
      for (int i = 0; i < routes.size(); i++) {
        Route.Definition route = routes.get(i);
        Object cursor = null;
        try {
          Route.Filter handler = route.filter();
          Method method = null;

          // find out where the route was defined
          final String owner;
          if (handler instanceof Route.MethodHandler) {
            method = ((Route.MethodHandler) handler).method();
            owner = method.getDeclaringClass().getName();
          } else {
            // lambda script
            owner = handler.getClass().getName();
          }

          if (owners.stream().filter(o -> owner.startsWith(o)).findFirst().isPresent()) {

            log.debug("    found {} {}", route.method(), route.pattern());
            Entry<Object, Node> entry = routeNodes.get(j++);

            Object candidate = entry.getKey();
            if (candidate instanceof Node) {
              cursor = candidate;
              log.debug("\n{}\n", candidate);
              /** doc and response codes . */
              Map<String, Object> doc = new DocCollector().accept((Node) candidate, route.method(),
                  ctx);
              Map<Integer, String> codes = (Map<Integer, String>) doc.remove("@statusCodes");
              String desc = (String) doc.remove("@text");
              String summary = (String) doc.remove("@summary");
              String retDoc = (String) doc.remove("@return");
              Type retType = (Type) doc.remove("@type");

              /** params and return type */
              List<RouteParam> params = Collections.emptyList();
              RouteResponse rsp;
              if (method == null) {
                // script params
                params = new RouteParamCollector(doc, route.method(), route.pattern())
                    .accept(entry.getValue(), ctx);
                // script response
                rsp = new ResponseTypeCollector().accept(entry.getValue(), ctx, retType, retDoc,
                    codes);
              } else {
                params = mvcParams(route, method, doc);
                rsp = new RouteResponseImpl(
                    retType == null ? method.getGenericReturnType() : retType,
                    retDoc, codes);
              }

              /** Create spec . */
              specs.add(new RouteSpecImpl(route, summary, desc, params, rsp));
            } else {
              specs.add((RouteSpec) candidate);
            }
          } else {
            log.debug("    ignoring {} {} from {}", route.method(), route.pattern(), owner);
          }
        } catch (Exception ex) {
          log.error("ignoring {} {} with {}", route.method(), route.pattern(),
              cursor == null ? "<no source code>" : cursor.toString(), ex);
        }
      }
    } catch (ParseException ex) {
      throw new IllegalStateException("Error while processing " + appClass.getName(), ex);
    } catch (Exception ex) {
      throw new IllegalStateException("Error while processing " + appClass, ex);
    }
    if (outdir != null) {
      save(outdir.resolve(appClass.getSimpleName() + ".spec"), specs);
    }
    log.debug("done");
    return specs;
  }

  private void save(final Path path, final List<RouteSpec> specs) {
    File fout = path.toFile();
    fout.getParentFile().mkdirs();
    try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(fout))) {
      log.info("    saving {}", fout);
      out.writeObject(specs);
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }

  private List<RouteParam> mvcParams(final Route.Definition route, final Method m,
      final Map<String, Object> doc) {
    RequestParamNameProviderImpl md = new RequestParamNameProviderImpl(
        new RouteMetadata(Env.DEFAULT.build(ConfigFactory.empty())));

    Parameter[] parameters = m.getParameters();
    List<RouteParam> params = new ArrayList<>(parameters.length);
    for (Parameter parameter : parameters) {
      String name = md.name(parameter);

      final RouteParamType paramType;
      if (parameter.getAnnotation(Body.class) != null) {
        paramType = RouteParamType.BODY;
      } else if (parameter.getAnnotation(Header.class) != null) {
        paramType = RouteParamType.HEADER;
      } else if (route.vars().contains(name)) {
        paramType = RouteParamType.PATH;
      } else if (route.method().equals("GET")) {
        paramType = RouteParamType.QUERY;
      } else {
        paramType = RouteParamType.FORM;
      }

      String pdoc = (String) doc.get(name);

      RouteParamImpl param = new RouteParamImpl(name, parameter.getParameterizedType(),
          paramType, null, pdoc);
      params.add(param);
    }
    return params;
  }
}
