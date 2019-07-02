/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;

/**
 * Template engine renderer. This class renderer instances of {@link ModelAndView} objects.
 *
 * @since 2.0.0
 * @author edgar
 */
public interface TemplateEngine extends MessageEncoder {

  /** Name of application property that defines the template path. */
  String TEMPLATE_PATH = "templates.path";

  /** Default template path. */
  String PATH = "views";

  /**
   * Render a model and view instance as String.
   *
   * @param ctx Web context.
   * @param modelAndView Model and view.
   * @return Rendered template.
   * @throws Exception If something goes wrong.
   */
  String render(Context ctx, ModelAndView modelAndView) throws Exception;

  @Override default byte[] encode(@Nonnull Context ctx, @Nonnull Object value) throws Exception {
    ctx.setDefaultResponseType(MediaType.html);
    String output = render(ctx, (ModelAndView) value);
    return output.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Normalize a template path by removing the leading `/` when present.
   *
   * @param templatesPath Template path.
   * @return Normalized path.
   */
  static @Nonnull String normalizePath(@Nonnull String templatesPath) {
    return templatesPath.startsWith("/") ? templatesPath.substring(1) : templatesPath;
  }
}
