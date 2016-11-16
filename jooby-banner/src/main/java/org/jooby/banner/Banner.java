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
package org.jooby.banner;

import static com.github.lalyos.jfiglet.FigletFont.convertOneLine;
import static java.util.Objects.requireNonNull;

import java.util.Optional;

import javax.inject.Provider;

import org.jooby.Env;
import org.jooby.Jooby.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

import javaslang.control.Try;

/**
 * <h1>banner</h1>
 * <p>
 * Prints out an ASCII art banner on startup using
 * <a href="https://github.com/lalyos/jfiglet">jfiglet</a>.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>{@code
 * package com.myapp;
 *
 * {
 *   use(new Banner());
 * }
 * }</pre>
 *
 * <p>
 * Prints out the value of <code>application.name</code> which here is <code>myapp</code>. Or you
 * can specify the text to prints out:
 * </p>
 *
 * <pre>{@code
 * package com.myapp;
 *
 * {
 *   use(new Banner("my awesome app"));
 * }
 * }</pre>
 *
 * <h2>font</h2>
 * <p>
 * You can pick and use the font of your choice via {@link #font(String)} option:
 * </p>
 *
 * <pre>{@code
 * package com.myapp;
 *
 * {
 *   use(new Banner("my awesome app").font("slant"));
 * }
 * }</pre>
 *
 * <p>
 * Font are distributed within the library inside the <code>/flf</code> classpath folder. A full
 * list of fonts is available <a href="http://patorjk.com/software/taag">here</a>.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR3
 */
public class Banner implements Module {

  private static final String FONT = "classpath:/flf/%s.flf";

  private String font = "speed";

  private final Optional<String> text;

  public Banner(final String text) {
    this.text = Optional.of(text);
  }

  public Banner() {
    this.text = Optional.empty();
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    String name = conf.getString("application.name");
    Logger log = LoggerFactory.getLogger(name);
    String v = conf.getString("application.version");
    String text = this.text.orElse(name);

    Provider<String> ascii = () -> Try
        .of(() -> trimEnd(convertOneLine(String.format(FONT, font), text)))
        .getOrElse(text);

    binder.bind(Key.get(String.class, Names.named("application.banner"))).toProvider(ascii);

    env.onStart(() -> {
      log.info("\n{} v{}\n", ascii.get(), v);
    });
  }

  public Banner font(final String font) {
    this.font = requireNonNull(font, "Font is required.");
    return this;
  }
  
  private String trimEnd(String str) {
      int len = str.length();
      int st = 0;
      char[] val = str.toCharArray();
      while ((st < len) && (val[len - 1] <= ' ')) {
          len--;
      }
      return str.substring(st, len);
  }
}
