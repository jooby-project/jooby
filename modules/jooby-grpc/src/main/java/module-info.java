/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
/** GraphQL module. */
module io.jooby.grpc {
  exports io.jooby.grpc;

  requires io.jooby;
  requires static com.github.spotbugs.annotations;
  requires typesafe.config;
  requires org.slf4j;
  requires jul.to.slf4j;
  requires io.grpc;
  requires io.grpc.inprocess;
  requires io.grpc.stub;
}
