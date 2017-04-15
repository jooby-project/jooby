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
import java.util.Optional;

import org.jooby.MediaType;

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
        PathMatcher includes = at(args, 0)
            .map(it -> FileSystems.getDefault().getPathMatcher("glob:" + it))
            .orElse(TRUE);
        PathMatcher excludes = at(args, 1)
            .map(it -> FileSystems.getDefault().getPathMatcher("glob:" + it))
            .orElse(FALSE);

        return ctx.function((self, arguments) -> {
          Path path = Paths.get(arguments.get(0).toString());
          if (includes.matches(path)) {
            return !excludes.matches(path);
          }
          return false;
        });
      }));

      v8.add("j2v8", j2v8);
      return ctx.invoke("rollup.js", source, options(), filename);
    });
  }

  private Optional<Object> at(final V8Array args, final int i) {
    if (i < args.length()) {
      Object value = V8ObjectUtils.getValue(args, i);
      if (value == V8.getUndefined()) {
        return Optional.empty();
      }
      return Optional.ofNullable(value);
    }
    return Optional.empty();
  }
}
