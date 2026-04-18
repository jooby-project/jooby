/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */

/** Thymeleaf module. */
module io.jooby.thymeleaf {
  exports io.jooby.thymeleaf;

  requires io.jooby;
  requires static org.jspecify;
  requires typesafe.config;
  requires thymeleaf;
}
