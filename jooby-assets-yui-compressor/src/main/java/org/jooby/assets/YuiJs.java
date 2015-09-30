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
import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * <h1>yui-js</h1>
 * <p>
 * <a href="http://yui.github.io/yuicompressor">Yui js compressor</a>.
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
 *     dist: [yui-js]
 *   }
 * }
 * </pre>
 *
 * <h2>options</h2>
 *
 * <pre>
 * assets {
 *   ...
 *   yui-js {
 *     munge: true
 *     preserve-semi: true
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public class YuiJs extends AssetProcessor {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  {
    set("munge", true);
    set("preserve-semi", true);
    set("disable-optimizations", false);
    set("linebreakpos", -1);
    set("verbose", false);
  }

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.css.matches(type);
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    ErrorReporter reporter = reporter(log, filename);
    JavaScriptCompressor compressor = new JavaScriptCompressor(new StringReader(source), reporter);
    StringWriter out = new StringWriter();
    compressor.compress(out, get("linebreakpos"), get("munge"), get("verbose"),
        get("preserve-semi"), get("disable-optimizations"));
    return out.toString();
  }

  private static ErrorReporter reporter(final Logger log, final String filename) {
    return new ErrorReporter() {

      @Override
      public void warning(final String message, final String sourceName, final int line,
          final String lineSource, final int lineOffset) {
        log.warn("{}:{}:{}:{}", filename, line, lineOffset, message);
      }

      @Override
      public EvaluatorException runtimeError(final String message, final String sourceName,
          final int line,
          final String lineSource, final int lineOffset) {
        return new EvaluatorException(message, filename, line, lineSource, lineOffset);
      }

      @Override
      public void error(final String message, final String sourceName, final int line,
          final String lineSource, final int lineOffset) {
        throw new AssetException(
            ImmutableList.of(new AssetProblem(filename, line, lineOffset, message)));
      }
    };
  }

}
