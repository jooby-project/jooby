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
public class ChiStaticPathsBench {

  private Chi router;

  @Setup
  public void setup() {
    this.router = new Chi();
    router.insert(route("GET", "/plaintext", stringHandler("plaintext")));
    router.insert(route("GET", "/json", stringHandler("json")));
    router.insert(route("GET", "/fortune", stringHandler("fortune")));
    router.insert(route("GET", "/db", stringHandler("db")));
    router.insert(route("GET", "/updates", stringHandler("updates")));
    router.insert(route("GET", "/queries", stringHandler("queries")));
  }

  @Benchmark
  public void plaintext() {
    router.find("GET", "/plaintext").matches();
  }

  private Route.Handler stringHandler(String foo) {
    return ctx -> foo;
  }

  private Route route(String method, String pattern, Route.Handler handler) {
    return new Route(method, pattern, handler).setEncoder(MessageEncoder.TO_STRING);
  }
}
