/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/** GraphQL module. */
module io.jooby.graphql {
  exports io.jooby.graphql;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires com.graphqljava;
  requires com.google.gson;
}
