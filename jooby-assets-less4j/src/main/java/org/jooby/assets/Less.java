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

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jooby.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.sommeri.less4j.Less4jException;
import com.github.sommeri.less4j.LessCompiler;
import com.github.sommeri.less4j.LessCompiler.CompilationResult;
import com.github.sommeri.less4j.LessCompiler.Configuration;
import com.github.sommeri.less4j.LessCompiler.SourceMapConfiguration;
import com.github.sommeri.less4j.LessSource;
import com.github.sommeri.less4j.core.ThreadUnsafeLessCompiler;
import com.typesafe.config.Config;

/**
 * <h1>less4j</h1>
 * <p>
 * <a href="https://github.com/SomMeri/less4j">Less4j</a> is a port of
 * <a href="http://lesscss.org/">Less</a> written in Java. <a href="http://lesscss.org/">Less</a> is
 * a CSS pre-processor, meaning that it extends the CSS language, adding features that allow
 * variables, mixins, functions and many other techniques that allow you to make CSS that is more
 * maintainable, themable and extendable.
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
 *     dev: [less]
 *     dist: [less]
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
 *     dev: [less]
 *     dist: [less]
 *   }
 *
 *   less {
 *     dev {
 *       sourceMap.linkSourceMap : true
 *     }
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public class Less extends AssetProcessor {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  {
    set("compressing", false);

    set("sourceMap", null);
  }

  @Override
  public boolean matches(final MediaType type) {
    return MediaType.css.matches(type);
  }

  @Override
  public String process(final String filename, final String source, final Config conf)
      throws Exception {
    String path = filename;
    try {
      LessCompiler compiler = new ThreadUnsafeLessCompiler();
      LessSource src = new LessStrSource(source, path);

      CompilationResult result = compiler.compile(src, lessConf(conf));

      result.getWarnings().forEach(warn -> {
        log.warn("{}:{}:{}:{}: {}", path, warn.getType(), warn.getLine(),
            warn.getCharacter(), warn.getMessage());
      });

      if (path.endsWith(".map")) {
        return result.getSourceMap();
      } else {
        return result.getCss();
      }
    } catch (Less4jException ex) {
      List<AssetProblem> problems = ex.getErrors().stream()
          .map(it -> new AssetProblem(path, it.getLine(), it.getCharacter(), it.getMessage()))
          .collect(Collectors.toList());
      throw new AssetException(problems);
    }
  }

  private Configuration lessConf(final Config conf) {
    Configuration configuration = new Configuration();

    configuration.setCompressing(get("compressing"));

    SourceMapConfiguration sourceMap = configuration.getSourceMapConfiguration();
    Map<String, Object> map = get("sourceMap");
    if (map == null) {
      sourceMap.setLinkSourceMap(false);
    } else {
      sourceMap.setEncodingCharset(
          map.getOrDefault("encodingCharset", conf.getAnyRef("application.charset")).toString());
      sourceMap.setIncludeSourcesContent((Boolean) map.getOrDefault("includeSourcesContent", true));
      sourceMap.setInline((Boolean) map.getOrDefault("inline", true));
      sourceMap.setRelativizePaths((Boolean) map.getOrDefault("relativizePaths", true));
      sourceMap.setLinkSourceMap((Boolean) map.getOrDefault("linkSourceMap", true));
    }

    return configuration;
  }

}
