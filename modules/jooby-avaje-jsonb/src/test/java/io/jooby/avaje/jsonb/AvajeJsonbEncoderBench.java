/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.avaje.jsonb;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import io.avaje.jsonb.Jsonb;
import io.jooby.buffer.BufferOptions;
import io.jooby.buffer.BufferedOutput;
import io.jooby.buffer.BufferedOutputFactory;
import io.jooby.internal.avaje.jsonb.BufferedJsonOutput;

@Fork(5)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class AvajeJsonbEncoderBench {

  private Jsonb jsonb;
  private Map<String, Object> message;

  private BufferedOutputFactory factory;
  private ThreadLocal<BufferedOutput> cache =
      ThreadLocal.withInitial(
          () -> {
            return factory.newBufferedOutput(1024);
          });

  @Setup
  public void setup() {
    message = Map.of("id", 98, "value", "Hello World");
    jsonb = Jsonb.builder().build();
    factory = BufferedOutputFactory.create(BufferOptions.small());
  }

  @Benchmark
  public void withJsonBuffer() {
    jsonb.toJsonBytes(message);
  }

  @Benchmark
  public void witCachedBufferedOutput() {
    var buffer = cache.get().clear();
    jsonb.toJson(message, jsonb.writer(new BufferedJsonOutput(buffer)));
  }

  @Benchmark
  public void witBufferedOutput() {
    var buffer = factory.newBufferedOutput(1024);
    jsonb.toJson(message, jsonb.writer(new BufferedJsonOutput(buffer)));
  }
}
