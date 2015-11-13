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

import java.util.LinkedHashMap;
import java.util.Map;

import org.jooby.MediaType;

import com.typesafe.config.Config;

/**
 * <h1>jshint</h1>
 * <p>
 * <a href="http://jshint.com/">JSHint</a> is a community-driven tool
 * to detect errors and potential problems in JavaScript code
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
 *     dev: [jshint]
 *     ...
 *   }
 * }
 * </pre>
 *
 * <h2>options</h2>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: ...
 *   }
 *
 *   pipeline {
 *     dev: [jshint]
 *     ...
 *   }
 *
 *   jshint {
 *     browser: true
 *     devel: true
 *     forin: true
 *     ...
 *     predef: {
 *       jquery: true
 *     }
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public class Jshint extends AssetProcessor {

  public Jshint() {
    set("browser", true);
    set("devel", true);
    set("forin", true);
    set("noarg", true);
    set("noempty", true);
    set("eqeqeq", true);
    set("bitwise", true);
    set("undef", true);
    set("curly", true);
    set("trailing", true);
    set("white", true);
    set("globalstrict", false);
    set("strict", false);
  }

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.js.matches(type);
  }

  @SuppressWarnings("unchecked")
  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    return V8Context.run("global", v8 -> {
      Map<String, Object> options = new LinkedHashMap<>(options());
      Map<String, Object> predef = (Map<String, Object>) options.remove("predef");
      options.remove("excludes");
      return v8.invoke("jshint.js", source, options, predef, filename);
    });
  }

}
