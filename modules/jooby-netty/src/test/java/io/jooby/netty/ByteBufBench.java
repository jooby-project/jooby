/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.netty;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import io.netty.buffer.Unpooled;

@Fork(5)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
public class ByteBufBench {
  private String string;

  @Setup
  public void setup() {
    string = "Hello World!";
  }

  @Benchmark
  public void copiedBufferUtf8() {
    Unpooled.copiedBuffer(string, StandardCharsets.UTF_8);
  }

  @Benchmark
  public void copiedBufferUSAscii() {
    Unpooled.copiedBuffer(string, StandardCharsets.US_ASCII);
  }

  @Benchmark
  public void stringUtf8Bytes() {
    Unpooled.wrappedBuffer(string.getBytes(StandardCharsets.UTF_8));
  }

  @Benchmark
  public void stringUSAsciiBytes() {
    Unpooled.wrappedBuffer(string.getBytes(StandardCharsets.US_ASCII));
  }
}
