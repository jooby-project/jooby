/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
import io.jooby.SslProvider;
import io.jooby.conscrypt.ConscryptSslProvider;

module io.jooby.conscrypt {

  requires io.jooby;
  requires com.github.spotbugs.annotations;
  requires org.conscrypt;

  provides SslProvider with ConscryptSslProvider;
}
