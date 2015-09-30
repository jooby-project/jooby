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

import org.jooby.MediaType;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;

/**
 * <h1>uglify</h1>
 * <p>
 * <a href="https://github.com/mishoo/UglifyJS2">UglifyJs2</a> JavaScript parser / mangler /
 * compressor / beautifier toolkit.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: [js/home.js]
 *   }
 *
 *   pipeline {
 *     ...
 *     dist: [uglify]
 *   }
 * }
 * </pre>
 *
 * <h2>options</h2>
 * <pre>
 * assets {
 *   ...
 *   uglify {
 *     strict: false
 *     output {
 *       beautify: true
 *     }
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public class Uglify extends AssetProcessor {

  public Uglify() {
    /** Parser options, filename is set from script (no need to set here). */
    set("strict", false);

    /** Mangle names, on by default. */
    set("mangle", true);

    /** Compressor options. */
    set("sequences", true); // join consecutive statemets with the “comma operator”
    set("properties", true); // optimize property access", a["foo"] → a.foo
    set("dead_code", true); // discard unreachable code
    set("drop_debugger", true); // discard “debugger” statements
    set("unsafe", false); // some unsafe optimizations (see below)
    set("conditionals", true); // optimize if-s and conditional expressions
    set("comparisons", true); // optimize comparisons
    set("evaluate", true); // evaluate constant expressions
    set("booleans", true); // optimize boolean expressions
    set("loops", true); // optimize loops
    set("unused", true); // drop unused variables/functions
    set("hoist_funs", true); // hoist function declarations
    set("hoist_vars", false); // hoist variable declarations
    set("if_return", true); // optimize if-s followed by return/continue
    set("join_vars", true); // join var declarations
    set("cascade", true); // try to cascade `right` into `left` in sequences
    set("side_effects", true); // drop side-effect-free statements
    set("warnings", true); // warn about potentially dangerous optimizations/code
    set("global_defs", ImmutableMap.of());
  }

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.js.matches(type);
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    return V8Context.run(v8 -> {
      return v8.invoke("uglify.js", source, options(), filename);
    });
  }

}
