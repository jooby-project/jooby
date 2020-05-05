/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import javax.annotation.Nonnull;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

/**
 * Template engine renderer. This class renderer instances of {@link ModelAndView} objects.
 * Template engine rendering is done by checking view name and supported file {@link #extensions()}.
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
    // initialize flash and session attributes (if any)
    ctx.flash();
    ctx.sessionOrNull();

    ctx.setDefaultResponseType(MediaType.html);
    String output = render(ctx, (ModelAndView) value);
    return output.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * True if the template engine is able to render the given view. This method checks if the view
   * name matches one of the {@link #extensions()}.
   *
   * @param modelAndView View to check.
   * @return True when view is supported.
   */
  default boolean supports(@Nonnull ModelAndView modelAndView) {
    String view = modelAndView.getView();
    for (String extension : extensions()) {
      if (view.endsWith(extension)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Number of file extensions supported by the template engine. Default is <code>.html</code>.
   *
   * @return Number of file extensions supported by the template engine.
   *     Default is <code>.html</code>.
   */
  default @Nonnull List<String> extensions() {
    return Collections.singletonList(".html");
  }

  /**
   * Normalize a template path by removing the leading `/` when present.
   *
   * @param templatesPath Template path.
   * @return Normalized path.
   */
  static @Nonnull String normalizePath(@Nonnull String templatesPath) {
    if (templatesPath == null) {
      return null;
    }
    return templatesPath.startsWith("/") ? templatesPath.substring(1) : templatesPath;
  }
}
