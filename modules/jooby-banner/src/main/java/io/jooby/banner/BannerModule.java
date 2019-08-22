/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.banner;

import com.typesafe.config.Config;
import io.jooby.Extension;
import io.jooby.Jooby;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.inject.Provider;
import java.util.Optional;

import static com.github.lalyos.jfiglet.FigletFont.convertOneLine;
import static io.jooby.ServiceKey.key;
import static java.util.Objects.requireNonNull;

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
 * @since 2.0.0
 */
public class BannerModule implements Extension {

  private static final String FONT = "classpath:/flf/%s.flf";

  private String font = "speed";

  private final Optional<String> text;

  /**
   * Creates a new {@link BannerModule} with the given text.
   *
   * @param text Text to display.
   */
  public BannerModule(final String text) {
    this.text = Optional.of(text);
  }

  /**
   * Default banner, defined by <code>application.name</code>.
   */
  public BannerModule() {
    this.text = Optional.empty();
  }

  @Override
  public void install(@Nonnull Jooby application) throws Exception {
    Logger log = application.getLog();
    Config conf = application.getConfig();
    String name = conf.getString("application.name");
    String version = conf.getString("application.version");
    String text = this.text.orElse(name);

    Provider<String> ascii = () -> {
      try {
        return rtrim(convertOneLine(fontPath(font), text));
      } catch (Throwable t) {
        return text;
      }
    };

    application.getServices().put(key(String.class, "application.banner"), ascii);

    application.onStarting(() -> log.info("\n{} v{}\n", ascii.get(), version));
  }

  /**
   * Set/change default font (speed).
   *
   * @param font A font's name.
   * @return This module.
   */
  public BannerModule font(final String font) {
    this.font = requireNonNull(font, "Font is required.");
    return this;
  }

  static String fontPath(String font) {
    return String.format(FONT, font);
  }

  static String rtrim(String s) {
    int i = s.length() - 1;
    while (i >= 0 && Character.isWhitespace(s.charAt(i))) {
      i--;
    }
    return s.substring(0, i + 1);
  }
}
