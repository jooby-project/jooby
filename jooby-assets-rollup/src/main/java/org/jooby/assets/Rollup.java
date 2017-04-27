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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jooby.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.eclipsesource.v8.V8;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.utils.V8ObjectUtils;
import com.typesafe.config.Config;

/**
 * <h1>rollup.js</h1>
 * <p>
 * <a href="http://rollupjs.org/">rollup.js</a> the next-generation ES6 module bundler.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: ...
 *   }
 *
 *   pipeline {
 *     ...
 *     dev: [rollup]
 *     dist: [rollup]
 *   }
 * }
 * </pre>
 *
 * <h2>options</h2>
 * <h3>generate</h3>
 * <pre>
 *   rollup {
 *     genereate {
 *       format: es
 *     }
 *   }
 * </pre>
 *
 * <p>
 * See
 * <a href="https://github.com/rollup/rollup/wiki/JavaScript-API#bundlegenerate-options-">generate
 * options</a>.
 * </p>
 *
 * <h3>plugins</h3>
 *
 * <h4>babel</h4>
 *
 * <pre>
 *   rollup {
 *     plugins {
 *       babel {
 *         presets: [[es2015, {modules: false}]]
 *       }
 *     }
 *   }
 * </pre>
 *
 * <p>
 * See <a href="https://babeljs.io/">https://babeljs.io</a> for more options.
 * </p>
 *
 * <h4>legacy</h4>
 * <p>
 * This plugins add a <code>export default</code> line to legacy modules:
 * </p>
 *
 * <pre>
 *   rollup {
 *     plugins {
 *       legacy {
 *         "/js/lib/react.js": React
 *       }
 *     }
 *   }
 * </pre>
 *
 * <h4>alias</h4>
 * <p>
 * Set an alias to a common (probably long) path.
 * </p>
 *
 * <pre>
 *   rollup {
 *     plugins {
 *       alias {
 *         "/js/lib/react.js": "react"
 *       }
 *     }
 *   }
 * </pre>
 *
 * <p>
 * Instead of:
 * </p>
 * <pre>
 * import React from 'js/lib/react.js';
 * </pre>
 *
 * <p>
 * Now, you can import a module like:
 * </p>
 * <pre>
 * import React from 'react';
 * </pre>
 *
 * @author edgar
 * @since 0.12.0
 */
public class Rollup extends AssetProcessor {

  static final PathMatcher TRUE = p -> true;
  static final PathMatcher FALSE = p -> false;

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.js.matches(type);
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    return V8Context.run("window", ctx -> {
      V8 v8 = ctx.v8;

      V8Object j2v8 = ctx.hash();
      j2v8.add("createFilter", ctx.function((receiver, args) -> {
        List<PathMatcher> includes = filter(args, 0).stream()
            .map(it -> FileSystems.getDefault().getPathMatcher("glob:" + it))
            .collect(Collectors.toList());
        if (includes.isEmpty()) {
          includes.add(TRUE);
        }

        List<PathMatcher> excludes = filter(args, 1).stream()
            .map(it -> FileSystems.getDefault().getPathMatcher("glob:" + it))
            .collect(Collectors.toList());
        if (excludes.isEmpty()) {
          excludes.add(FALSE);
        }

        return ctx.function((self, arguments) -> {
          Path path = Paths.get(arguments.get(0).toString());
          if (includes.stream().filter(it -> it.matches(path)).findFirst().isPresent()) {
            return !excludes.stream().filter(it -> it.matches(path)).findFirst().isPresent();
          }
          return false;
        });
      }));

      v8.add("j2v8", j2v8);

      Map<String, Object> options = options();
      log.debug("{}", options);

      return ctx.invoke("rollup.js", source, options, filename);
    });
  }

  @SuppressWarnings("unchecked")
  private List<String> filter(final V8Array args, final int i) {
    if (i < args.length()) {
      Object value = V8ObjectUtils.getValue(args, i);
      if (value == V8.getUndefined()) {
        return Collections.emptyList();
      }
      List<String> filter = new ArrayList<>();
      if (value instanceof Collection) {
        filter.addAll((Collection<? extends String>) value);
      } else {
        filter.add(value.toString());
      }
      return filter;
    }
    return Collections.emptyList();
  }
}
