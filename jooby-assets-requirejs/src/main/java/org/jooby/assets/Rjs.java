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

import com.typesafe.config.Config;

/**
 * <h1>rjs</h1>
 * <p>
 * <a href="http://requirejs.org/docs/optimization.html">require.js optimizer</a> resolve and
 * optimize require.js files.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: js/home.js
 *   }
 *
 *   pipeline {
 *     dev: [rjs]
 *     dist: [rjs]
 *   }
 * }
 * </pre>
 * <p>
 * NOTE: The fileset have to define the main module (root/main entry point) and require.js will do
 * all the work.
 * </p>
 *
 * <h2>options</h2>
 * <pre>
 * assets {
 *   ...
 *   rjs {
 *     optimize: none
 *     ...
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public class Rjs extends AssetProcessor {

  public Rjs() {
    set("optimize", "none");
  }

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.js.matches(type);
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    return V8Context.run(v8 -> {
      String path = filename.startsWith("/") ? filename.substring(1) : filename;
      return v8.invoke("r.js", source, options(), path);
    });
  }

}
