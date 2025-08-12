/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.museum;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Jooby;

/**
 * Redocly Museum API
 *
 * <p>An imaginary, but delightful Museum API for interacting with museum services and information.
 * Built with love by Redocly.
 *
 * @version 1.0.0
 * @contact.email team@redocly.com
 * @contact.url https://redocly.com/docs/cli/
 * @x-logo.url https://redocly.github.io/redoc/museum-logo.png
 * @x-logo.altText Museum logo
 * @license.name MIT
 * @license.url https://opensource.org/license/mit/
 * @server.url https://api.fake-museum-example.com/v1
 */
public class MuseumApp extends Jooby {
  {
    mvc(toMvcExtension(Museum.class));
    mvc(toMvcExtension(Events.class));
    mvc(toMvcExtension(Tickets.class));
  }
}
