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
import io.jooby.output.ByteBufferOutputFactory;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;

@Fork(5)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class AvajeJsonbEncoderBench {

  private Jsonb jsonb;
  private Map<String, Object> message;

  private OutputFactory factory;
  private ThreadLocal<Output> cache =
      ThreadLocal.withInitial(
          () -> {
            return factory.newBufferedOutput(1024);
          });

  @Setup
  public void setup() {
    message = Map.of("id", 98, "value", "Hello World");
    jsonb = Jsonb.builder().build();
    factory = new ByteBufferOutputFactory();
  }

  @Benchmark
  public void withJsonBuffer() {
    jsonb.toJsonBytes(message);
  }

  @Benchmark
  public void withDataBufferOutputStream() {
    var buffer = cache.get();
    jsonb.toJson(message, jsonb.writer(new DataBufferJsonOutputBench(buffer)));
    // TODO: add clear to output or use close
    buffer.asByteBuffer().clear();
  }
}
