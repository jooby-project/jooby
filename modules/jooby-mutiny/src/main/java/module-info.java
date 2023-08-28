/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
import io.jooby.ResultHandler;
import io.jooby.mutiny.Mutiny;

/** Mutiny module. */
module io.jooby.mutiny {
  exports io.jooby.mutiny;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires io.smallrye.mutiny;
  requires org.slf4j;

  provides ResultHandler with
      Mutiny;
}
