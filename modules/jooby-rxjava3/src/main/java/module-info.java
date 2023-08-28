/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
import io.jooby.ResultHandler;
import io.jooby.rxjava3.Reactivex;

/** Rx module. */
module io.jooby.rxjava3 {
  exports io.jooby.rxjava3;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires io.reactivex.rxjava3;
  requires org.reactivestreams;
  requires org.slf4j;

  provides ResultHandler with
      Reactivex;
}
