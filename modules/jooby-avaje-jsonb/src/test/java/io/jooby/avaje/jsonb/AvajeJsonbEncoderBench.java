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
import io.jooby.internal.avaje.jsonb.BufferedJsonOutput;
import io.jooby.output.BufferedOutput;
import io.jooby.output.OutputFactory;
import io.jooby.output.OutputOptions;

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
  private ThreadLocal<BufferedOutput> cache =
      ThreadLocal.withInitial(
          () -> {
            return factory.allocate(1024);
          });

  @Setup
  public void setup() {
    message = Map.of("id", 98, "value", "Hello World");
    jsonb = Jsonb.builder().build();
    factory = OutputFactory.create(OutputOptions.small());
  }

  @Benchmark
  public void withJsonBuffer() {
    factory.wrap(jsonb.toJsonBytes(message));
  }

  @Benchmark
  public void withCachedBufferedOutput() {
    var buffer = cache.get().clear();
    jsonb.toJson(message, jsonb.writer(new BufferedJsonOutput(buffer)));
  }

  @Benchmark
  public void withBufferedOutput() {
    var buffer = factory.allocate(1024);
    jsonb.toJson(message, jsonb.writer(new BufferedJsonOutput(buffer)));
  }

  @Benchmark
  public void withCompositeOutput() {
    var buffer = factory.newComposite();
    jsonb.toJson(message, jsonb.writer(new BufferedJsonOutput(buffer)));
  }
}
