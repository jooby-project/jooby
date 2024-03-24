/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import io.jooby.MessageEncoder;
import io.jooby.Route;

@Fork(5)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ChiBench {

  private Chi router;

  @Setup
  public void setup() {
    this.router = new Chi();
    router.insert(route("GET", "/api/user/edit", stringHandler("static")));
    router.insert(route("GET", "/api/user/{id}", stringHandler("param")));
    router.insert(route("GET", "/api/user/*", stringHandler("tail")));
    // put more
    router.insert(route("GET", "/api/page/edit", stringHandler("static")));
    router.insert(route("GET", "/api/page/{id}", stringHandler("id")));
    router.insert(route("GET", "/api/page/*", stringHandler("tail")));
  }

  @Benchmark
  public void dynamicPath() {
    router.find("GET", "/api/user/123").matches();
  }

  @Benchmark
  public void staticPath() {
    router.find("GET", "/api/page/edit").matches();
  }

  private Route.Handler stringHandler(String foo) {
    return ctx -> foo;
  }

  private Route route(String method, String pattern, Route.Handler handler) {
    return new Route(method, pattern, handler).setEncoder(MessageEncoder.TO_STRING);
  }
}
