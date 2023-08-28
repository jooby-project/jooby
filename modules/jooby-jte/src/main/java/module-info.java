/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/** JTE module. */
module io.jooby.jte {
  exports io.jooby.jte;

  requires transitive io.jooby;
  requires static com.github.spotbugs.annotations;
  requires gg.jte;
  requires gg.jte.runtime;
}
