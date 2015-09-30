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

import java.util.Arrays;
import java.util.List;

import org.jooby.Env;
import org.jooby.MediaType;

import com.typesafe.config.Config;

/**
 * <h1>props</h1>
 * <p>
 * Replace expressions like: <code>${foo}</code> to the value of the property foo. Default
 * delimiters are: <code>${}</code>
 * </p>
 *
 * <h2>usage</h2>
 * <pre>
 * assets {
 *   fileset {
 *     home: ...
 *   }
 *
 *   pipeline {
 *     dev: [props]
 *     dist: [props]
 *   }
 * }
 * </pre>
 * <h2>options</h2>
 * <p>
 * The only available option is: <code>delims</code>
 * </p>
 * <pre>
 * assets{
 *   fileset {
 *     home: ...
 *   }
 *
 *   pipeline {
 *     dev: [props]
 *     dist: [props]
 *   }
 *   props {
 *     delims: [{{, }}]
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @see Env#resolve(String)
 */
public class Props extends AssetProcessor {

  {
    set("delims", Arrays.asList("${", "}"));
  }

  @Override
  public boolean matches(final MediaType type) {
    return true;
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    Env env = Env.DEFAULT.build(conf);
    List<String> delims = get("delims");
    return env.resolve(source, delims.get(0), delims.get(1));
  }

}
