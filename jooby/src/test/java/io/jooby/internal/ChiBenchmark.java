package io.jooby.internal;

import io.jooby.Context;
import io.jooby.ForwardingContext;
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

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

@Fork(5)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class ChiBenchmark {

  private $Chi router;

  Context plaintext;

  Context article;

  Context articleEdit;

  @Setup
  public void setup() {
    router = new $Chi();

    router.insert(route("GET", "/plaintext"));
    router.insert(route("GET", "/json"));

    plaintext = context("GET", "/plaintext");

    article = context("GET", "/articles/{id}");

    articleEdit = context("GET", "/articles/{id}/edit");
  }

  private Route route(String method, String pattern) {
    return new Route(method, pattern, ctx -> "").setReturnType(String.class);
  }

  @Benchmark
  public void _plaintext() {
    router.find(plaintext, null, null);
  }

  @Benchmark
  public void article() {
    router.find(article, null, null);
  }

  @Benchmark
  public void articleEdit() {
    router.find(articleEdit, null, null);
  }

  private static Context context(String method, String path) {
    return new ForwardingContext(null) {
      @Nonnull @Override public String getMethod() {
        return method;
      }

      @Nonnull @Override public String pathString() {
        return path;
      }
    };
  }

}
