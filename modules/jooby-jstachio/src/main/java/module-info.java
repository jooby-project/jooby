/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/**
 * See {@link io.jooby.jstachio.JStachioModule}
 *
 * @author agentgt
 * @see io.jooby.jstachio.JStachioModule
 */
module io.jooby.jstachio {
  requires transitive io.jstach.jstachio;
  requires transitive io.jooby;
  requires jakarta.inject;
  requires static com.github.spotbugs.annotations;

  exports io.jooby.jstachio;

  uses io.jstach.jstachio.spi.JStachioExtension;
}
