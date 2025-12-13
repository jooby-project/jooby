/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package issues.i3820.app;

import static io.jooby.openapi.MvcExtensionGenerator.toMvcExtension;

import io.jooby.Jooby;

/**
 * Library API.
 *
 * <p>An imaginary, but delightful Library API for interacting with library services and
 * information. Built with love by https://jooby.io.
 *
 * @version 1.0.0
 * @license.name Apache 2.0
 * @license.url http://www.apache.org/licenses/LICENSE-2.0.html
 * @contact.name Jooby Demo
 * @contact.url https://jooby.io
 * @contact.email support@jooby.io
 * @server.url https://library.jooby.io
 * @x-logo.url https://redoredocly.github.io/redoc/museum-logo.png
 * @tag Library. Outlines the available actions in the Library System API. The system is designed to
 *     allow users to search for books, view details, and manage the library inventory.
 * @tag Inventory. Managing Inventory
 */
public class AppLib extends Jooby {
  {
    mvc(toMvcExtension(LibApi.class));
  }
}
