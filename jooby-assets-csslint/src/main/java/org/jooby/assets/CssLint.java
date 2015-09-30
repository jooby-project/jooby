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
 * <h1>csslint</h1>
 * <p>
 * <a href="http://csslint.net/">CSSLint</a> automated linting of Cascading Stylesheets.
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
 *     dev: [csslint]
 *     ...
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public class CssLint extends AssetProcessor {

  public CssLint() {
  }

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.css.matches(type);
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    return V8Context.run(v8 -> {
      return v8.invoke("csslint.js", source, options(), filename);
    });
  }

}
