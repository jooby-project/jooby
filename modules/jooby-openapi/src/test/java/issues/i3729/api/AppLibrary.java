/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3729.api;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Jooby;

/**
 * Library API.
 *
 * <p>Available data: Books and authors.
 *
 * @version 4.0.0
 * @server.url https://api.fake-museum-example.com/v1
 * @contact.name Jooby
 * @contact.url https://jooby.io
 * @license.name Apache
 * @contact.email support@jooby.io
 * @license.url https://jooby.io/LICENSE
 * @x-logo.url https://redocly.github.io/redoc/museum-logo.png
 * @x-logo.altText Museum logo
 */
public class AppLibrary extends Jooby {

  {
    mvc(toMvcExtension(LibraryApi.class));
  }
}
