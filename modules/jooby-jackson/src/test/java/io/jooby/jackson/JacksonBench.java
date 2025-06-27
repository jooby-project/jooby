/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;

@Fork(5)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class JacksonBench {
  private ObjectMapper mapper;
  private Map<String, Object> message;

  private OutputFactory factory;
  private ThreadLocal<Output> cache =
      ThreadLocal.withInitial(() -> factory.newBufferedOutput(1024));

  @Setup
  public void setup() {
    message = Map.of("id", 98, "value", "Hello World");
    mapper = new ObjectMapper();
    factory = OutputFactory.create(false);
  }

  @Benchmark
  public void bytes() throws JsonProcessingException {
    mapper.writeValueAsBytes(message);
  }

  @Benchmark
  public void wrapBytes() throws JsonProcessingException {
    factory.wrap(mapper.writeValueAsBytes(message));
  }

  @Benchmark
  public void output() throws IOException {
    var buffer = cache.get().clear();
    mapper.writeValue(buffer.asOutputStream(), message);
  }
}
