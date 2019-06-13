package io.jooby.internal;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.Cookie;
import io.jooby.Formdata;
import io.jooby.MediaType;
import io.jooby.Multipart;
import io.jooby.QueryString;
import io.jooby.Route;
import io.jooby.Router;
import io.jooby.Sender;
import io.jooby.StatusCode;
import io.jooby.Value;
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
import javax.annotation.Nullable;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Fork(5)
@Warmup(iterations = 3)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@State(Scope.Benchmark)
public class ChiBenchmark {

  private $Chi router;

  Context foo;

  Context fooBar;

  @Setup
  public void setup() {
    router = new $Chi();

    router.insert(route("GET", "/foo"));
    router.insert(route("GET", "/foo/{bar}"));

    foo = context("GET", "/foo");

    fooBar = context("GET", "/foo/xuqy");
  }

  private Route route(String method, String pattern) {
    return new Route(method, pattern, String.class, null, null, null, null, null, null);
  }

  @Benchmark
  public void staticMatch() {
    router.find(foo, null, null);
  }

  @Benchmark
  public void paramMatch() {
    router.find(fooBar, null, null);
  }

  private Context context(String method, String path) {
    return new Context() {
      @Nonnull @Override public Map<String, Object> getAttributes() {
        return null;
      }

      @Nonnull @Override public Router getRouter() {
        return null;
      }

      @Nonnull @Override public Map<String, String> cookieMap() {
        return null;
      }

      @Nonnull @Override public String getMethod() {
        return method;
      }

      @Nonnull @Override public Route getRoute() {
        return null;
      }

      @Nonnull @Override public Context setRoute(@Nonnull Route route) {
        return null;
      }

      @Nonnull @Override public String pathString() {
        return path;
      }

      @Nonnull @Override public Map<String, String> pathMap() {
        return null;
      }

      @Nonnull @Override public Context setPathMap(@Nonnull Map<String, String> pathMap) {
        return null;
      }

      @Nonnull @Override public QueryString query() {
        return null;
      }

      @Nonnull @Override public Value headers() {
        return null;
      }

      @Nonnull @Override public String getRemoteAddress() {
        return null;
      }

      @Nonnull @Override public String getProtocol() {
        return null;
      }

      @Nonnull @Override public String getScheme() {
        return null;
      }

      @Nonnull @Override public Formdata form() {
        return null;
      }

      @Nonnull @Override public Multipart multipart() {
        return null;
      }

      @Nonnull @Override public Body body() {
        return null;
      }

      @Override public boolean isInIoThread() {
        return false;
      }

      @Nonnull @Override public Context dispatch(@Nonnull Runnable action) {
        return null;
      }

      @Nonnull @Override
      public Context dispatch(@Nonnull Executor executor, @Nonnull Runnable action) {
        return null;
      }

      @Nonnull @Override public Context detach(@Nonnull Runnable action) {
        return null;
      }

      @Nonnull @Override
      public Context setResponseHeader(@Nonnull String name, @Nonnull String value) {
        return null;
      }

      @Nonnull @Override public Context setResponseLength(long length) {
        return null;
      }

      @Nonnull @Override public Context setResponseCookie(@Nonnull Cookie cookie) {
        return null;
      }

      @Nonnull @Override public Context setResponseType(@Nonnull String contentType) {
        return null;
      }

      @Nonnull @Override
      public Context setResponseType(@Nonnull MediaType contentType, @Nullable Charset charset) {
        return null;
      }

      @Nonnull @Override public Context setDefaultResponseType(@Nonnull MediaType contentType) {
        return null;
      }

      @Nonnull @Override public MediaType getResponseType() {
        return null;
      }

      @Nonnull @Override public Context setResponseCode(int statusCode) {
        return null;
      }

      @Nonnull @Override public StatusCode getResponseCode() {
        return null;
      }

      @Nonnull @Override public OutputStream responseStream() {
        return null;
      }

      @Nonnull @Override public Sender responseSender() {
        return null;
      }

      @Nonnull @Override
      public PrintWriter responseWriter(@Nonnull MediaType contentType, @Nullable Charset charset) {
        return null;
      }

      @Nonnull @Override public Context send(@Nonnull String data, @Nonnull Charset charset) {
        return null;
      }

      @Nonnull @Override public Context removeResponseHeader(@Nonnull String name) {
        return null;
      }

      @Nonnull @Override public Context send(@Nonnull byte[] data) {
        return null;
      }

      @Nonnull @Override public Context send(@Nonnull ByteBuffer data) {
        return null;
      }

      @Nonnull @Override public Context send(@Nonnull ReadableByteChannel channel) {
        return null;
      }

      @Nonnull @Override public Context send(@Nonnull InputStream input) {
        return null;
      }

      @Nonnull @Override public Context send(@Nonnull FileChannel file) {
        return null;
      }

      @Nonnull @Override public Context send(@Nonnull StatusCode statusCode) {
        return null;
      }

      @Override public boolean isResponseStarted() {
        return false;
      }
    };
  }

}
