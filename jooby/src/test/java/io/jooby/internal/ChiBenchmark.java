package io.jooby.internal;

import io.jooby.Route;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@Fork(5)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class ChiBenchmark {

  private Chi router;

  @Setup
  public void setup() {
    router = new Chi();

    router.insert(route("GET", "/plaintext"));
    router.insert(route("GET", "/articles/{id}"));
    router.insert(route("GET", "/articles/{id}/edit"));
  }

  private Route route(String method, String pattern) {
    return new Route(method, pattern, ctx -> "").setReturnType(String.class);
  }

  @Benchmark
  public void _plaintext() {
    router.find("GET", "/plaintext");
  }

  @Benchmark
  public void articles() {
    router.find("GET", "/articles/123");
  }

  @Benchmark
  public void articlesEdit() {
    router.find("GET", "/articles/123/edit");
  }

}
