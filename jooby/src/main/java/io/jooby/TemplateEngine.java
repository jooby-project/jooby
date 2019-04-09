/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
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
public interface TemplateEngine extends Renderer {

  /**
   * Render a model and view instance as String.
   *
   * @param ctx Web context.
   * @param modelAndView Model and view.
   * @return Rendered template.
   * @throws Exception If something goes wrong.
   */
  String apply(Context ctx, ModelAndView modelAndView) throws Exception;

  @Override default byte[] render(@Nonnull Context ctx, @Nonnull Object value) throws Exception {
    ctx.setDefaultContentType(MediaType.html);
    String output = apply(ctx, (ModelAndView) value);
    return output.getBytes(StandardCharsets.UTF_8);
  }
}
