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

import org.jooby.MediaType;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.PathType;
import org.jruby.embed.ScriptingContainer;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.typesafe.config.Config;

/**
 * <h1>sass</h1>
 * <p>
 * <a href="http://sass-lang.com/">sass-lang</a> implementation from
 * <a href="https://github.com/sass/sass">Sass (ruby)</a> Sass is the most mature, stable, and
 * powerful professional grade CSS extension language in the world.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: home.scss
 *   }
 *
 *   pipeline {
 *     dev: [sass]
 *     dist: [sass]
 *   }
 * }
 * </pre>
 *
 * <h2>options</h2>
 * <pre>
 * assets {
 *   ...
 *   sass {
 *     syntax: scss
 *     dev {
 *       sourceMap: inline
 *     }
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public class Sass extends AssetProcessor {

  private static final Supplier<ScriptingContainer> rctx = Suppliers
      .memoize(() -> new ScriptingContainer(LocalContextScope.THREADSAFE));

  private static final Supplier<Object> script = Suppliers
      .memoize(() -> rctx.get().runScriptlet(PathType.CLASSPATH, "sass.rb"));

  public Sass() {
    set("syntax", "scss");
    set("sourcemap", false);
    set("cache_location", new File(System.getProperty("java.io.tmpdir"), "sass-cache")
        .getAbsolutePath());
  }

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.css.matches(type);
  }

  @Override
  public String process(String filename, final String source, final Config conf) throws Exception {
    boolean isMap = filename.endsWith(".map");
    if (isMap) {
      filename = filename.substring(0, filename.length() - ".map".length());
    }
    Object value = rctx.get().callMethod(script.get(), "render", source, options(),
        filename, getClass(), isMap);
    if (value instanceof AssetProblem) {
      throw new AssetException((AssetProblem) value);
    }
    return value.toString();
  }

}
