/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/**
 * Avaje jsonb module.
 */
module io.jooby.avaje.jsonb {
  exports io.jooby.avaje.jsonb;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires io.avaje.jsonb;
}
