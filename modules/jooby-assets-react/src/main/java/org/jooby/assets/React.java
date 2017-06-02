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
package org.jooby.assets;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;

import org.jooby.Route;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

/**
 * <h1>react</h1>
 * <p>
 * Write <a href="https://facebook.github.io/react">React</a> applications easily in the JVM.
 * </p>
 *
 * <h2>usage</h2>
 * <p>
 * Download <a href="https://unpkg.com/react@15/dist/react.js">react.js</a> and
 * <a href="https://unpkg.com/react-dom@15/dist/react-dom.js">react-dom.js</a> into
 * <code>public/js/lib</code> folder.
 * </p>
 *
 * <p>
 * Then add the react processor to <code>conf/assets.conf</code>:
 * </p>
 * <pre>{@code
 * assets {
 *   fileset {
 *     index: index.js
 *   }
 *
 *   pipeline {
 *     dev: [react]
 *     dist: [react]
 *   }
 * }
 * }</pre>
 *
 * <p>
 * Write some react code <code>public/js/index.js</code>:
 * </p>
 * <pre>{@code
 *   import React from 'react';
 *   import ReactDOM from 'react-dom';
 *
 *   const Hello = () => (
 *     <p>Hello React</p>
 *   )
 *
 *   ReactDOM.render(<Hello />, document.getElementById('root'));
 * }</pre>
 *
 * <p>
 * Choose one of the available
 * <a href="http://jooby.org/doc/parser-and-renderer/#template-engines">template engines</a> add the
 * <code>index.js</code> to the page:
 *
 * <pre>{@code
 *   <!doctype html>
 *   <html lang="en">
 *     <head>
 *       <meta charset="utf-8">
 *       <meta name="viewport" content="width=device-width, initial-scale=1">
 *       <title>React App</title>
 *     </head>
 *     <body>
 *       <div id="root"></div>
 *       {{ index_scripts | raw}}
 *     </body>
 *   </html>
 * }</pre>
 *
 * <p>
 * The <code>{{ index_scripts | raw}}</code> here is <a href="jooby.org/doc/pebble">pebble
 * expression</a>. Open an browser and try it.
 * </p>
 *
 * <h2>how it works?</h2>
 * <p>
 * This module give you a ready to use react environment with: <code>ES6</code> and <code>JSX</code>
 * support via <a href="http://babeljs.io">babel.js</a> and
 * <a href="https://github.com/rollup/rollup">rollup.js</a>.
 * </p>
 * <p>
 * You don't need to install anything <code>node.js</code>, <code>npm</code>, ... nothing,
 * <a href="http://babeljs.io">babel.js</a> and
 * <a href="https://github.com/rollup/rollup">rollup.js</a> run on top of
 * <a href="https://github.com/eclipsesource/J2V8">j2v8</a> as part of the JVM process.
 * </p>
 *
 * <h2>options</h2>
 * <h3>react-router</h3>
 * <p>
 * Just drop the
 * <a href=
 * "https://unpkg.com/react-router-dom/umd/react-router-dom.js">react-router-dom.js</a>
 * into the <code>public/js/lib</code> folder and use it.
 * </p>
 *
 * <h3>rollup</h3>
 * <p>
 * It supports all the option of <a href="http://jooby.org/doc/assets-rollup/">rollup.js</a>
 * processor.
 * </p>
 *
 * @author edgar
 * @since 1.1.1
 */
public class React extends Rollup {

  public React() {
    set("basedir", "public");
    set("generate", ImmutableMap.of("format", "iife"));
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<String, Object> options() throws Exception {
    Map<String, Object> options = super.options();
    BiFunction<Map<String, Object>, String, Map<String, Object>> option = (src, key) -> {
      Map<String, Object> value = (Map<String, Object>) src.get(key);
      if (value == null) {
        value = new HashMap<>();
        src.put(key, value);
      }
      return value;
    };
    Map<String, Object> plugins = option.apply(options, "plugins");

    Path basedir = Paths.get(get("basedir").toString());
    // react.js and react-dom.js
    Path react = getFile(basedir, "react.js");
    Path reactDom = getFile(basedir, "react-dom.js");
    Optional<Path> reactRouterDom = findFile(basedir, "react-router-dom.js");

    /**
     * Legacy: export default for react and react-dom
     */
    Map<String, Object> legacy = option.apply(plugins, "legacy");
    Set<String> babelExcludes = new HashSet<>();

    legacy.putIfAbsent(Route.normalize(react.toString()), "React");
    legacy.putIfAbsent(Route.normalize(reactDom.toString()), "ReactDOM");

    ImmutableSet.of(react.getParent(), reactDom.getParent()).stream()
        .map(it -> Route.normalize(it.toString()))
        .forEach(exclude -> {
          babelExcludes.add(exclude + File.separator + "*.js");
          babelExcludes.add(exclude + File.separator + "**" + File.separator + "*.js");
        });

    reactRouterDom.ifPresent(path -> {

      legacy.putIfAbsent(Route.normalize(path.toString()),
          ImmutableMap.of("ReactRouterDOM", ImmutableList.of(
              "BrowserRouter",
              "HashRouter",
              "Link",
              "MemoryRouter",
              "NavLink",
              "Prompt",
              "Redirect",
              "Route",
              "Router",
              "StaticRouter",
              "Switch",
              "matchPath",
              "withRouter")));
    });

    /**
     * Alias:
     */
    Map<String, Object> alias = option.apply(plugins, "alias");
    if (!alias.containsKey("react")) {
      alias.putIfAbsent("react", Route.normalize(react.toString()));
      alias.putIfAbsent("react-dom", Route.normalize(reactDom.toString()));
    }

    reactRouterDom.ifPresent(path -> {
      alias.putIfAbsent("react-router-dom", Route.normalize(path.toString()));
    });

    /**
     * Babel:
     */
    Map<String, Object> babel = option.apply(plugins, "babel");
    if (!babel.containsKey("presets")) {
      babel.put("presets", ImmutableList
          .of(ImmutableList.of("es2015", ImmutableMap.of("modules", false)), "react"));
    }
    Optional.ofNullable(babel.get("excludes")).ifPresent(it -> {
      if (it instanceof Collection) {
        babelExcludes.addAll((Collection<? extends String>) it);
      } else {
        babelExcludes.add(it.toString());
      }
    });
    babel.put("excludes", new ArrayList<>(babelExcludes));

    /**
     * context
     */
    options.putIfAbsent("context", "window");

    /**
     * Base dir
     */
    options.remove("basedir");
    return options;
  }

  private Path getFile(final Path basedir, final String filename) throws IOException {
    return findFile(basedir, filename)
        .orElseThrow(() -> new FileNotFoundException(filename + " at " + basedir.toAbsolutePath()));
  }

  private Optional<Path> findFile(final Path basedir, final String filename) throws IOException {
    return Files.walk(basedir)
        .filter(it -> it.toString().endsWith(filename))
        .findFirst()
        .flatMap(it -> Optional.of(basedir.relativize(it)));
  }

}
