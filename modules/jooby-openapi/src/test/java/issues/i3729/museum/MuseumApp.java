/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.museum;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Jooby;

/**
 * Redocly Museum API.
 *
 * <p>An imaginary, but delightful Museum API for interacting with museum services and information.
 * Built with love by Redocly.
 *
 * @version 1.0.0
 * @contact
 */
public class MuseumApp extends Jooby {
  {
    mvc(toMvcExtension(Museum.class));
    mvc(toMvcExtension(Events.class));
    mvc(toMvcExtension(Tickets.class));
  }
}
