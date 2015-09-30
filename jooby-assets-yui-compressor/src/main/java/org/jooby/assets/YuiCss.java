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

import java.io.StringReader;
import java.io.StringWriter;

import org.jooby.MediaType;

import com.typesafe.config.Config;
import com.yahoo.platform.yui.compressor.CssCompressor;

/**
 * <h1>yui-css</h1>
 * <p>
 * <a href="http://yui.github.io/yuicompressor">Yui css compressor</a>.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * assets {
 *   fileset {
 *     home: [css/home.css]
 *   }
 *
 *   pipeline {
 *     ...
 *     dist: [yui-css]
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public class YuiCss extends AssetProcessor {

  {
    set("linebreakpos", -1);
  }

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.css.matches(type);
  }

  @Override
  public String process(final String filename, final String source, final Config conf) throws Exception {
    CssCompressor compressor = new CssCompressor(new StringReader(source));
    int linebreakpos = get("linebreakpos");
    StringWriter out = new StringWriter();
    compressor.compress(out, linebreakpos);
    return out.toString();
  }

}
