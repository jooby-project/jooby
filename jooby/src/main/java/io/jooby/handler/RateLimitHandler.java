/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.SneakyThrows;
import io.jooby.StatusCode;

/**
 * Rate limit handler using https://github.com/vladimir-bukhtoyarov/bucket4j.
 *
 * <p>NOTE: bucket4j must be included as part of your project dependencies (classpath).
 *
 * <p>Example 1: 10 requests per minute
 *
 * <pre>{@code
 * {
 *   Bandwidth limit = Bandwidth.simple(10, Duration.ofMinutes(1));
 *   Bucket bucket = Bucket4j.builder().addLimit(limit).build();
 *
 *   before(new RateLimitHandler(bucket));
 * }
 * }</pre>
 *
 * Example 2: 10 requests per minute per IP address
 *
 * <pre>{@code
 * {
 *   before(new RateLimitHandler(remoteAddress -> {
 *       Bandwidth limit = Bandwidth.simple(10, Duration.ofMinutes(1));
 *       return Bucket4j.builder().addLimit(limit).build();
 *   }));
 * }
 * }</pre>
 *
 * Example 3: 10 requests per minute using an <code>ApiKey</code> header.
 *
 * <pre>{@code
 * {
 *   before(new RateLimitHandler(key -> {
 *       Bandwidth limit = Bandwidth.simple(10, Duration.ofMinutes(1));
 *       return Bucket4j.builder().addLimit(limit).build();
 *   }, "ApiKey"));
 * }
 * }</pre>
 *
 * Example 4: Rate limit in a cluster
 *
 * <pre>{@code
 * {
 *   // Get one of the proxy manager from bucket4j
 *   ProxyManager<String> buckets = ...;
 *   before(RateLimitHandler.cluster(key -> {
 *       buckets.getProxy(key, () -> {
 *           return Bucket4j.configurationBuilder()
 *             .addLimit(Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1))))
 *             .build();
 *       });
 *   }));
 * }
 * }</pre>
 *
 * @author edgar
 * @since 2.5.2
 */
public class RateLimitHandler implements Route.Before {

  private final Function<Context, Bucket> factory;

  /**
   * Rate limit per IP/Remote Address.
   *
   * @param bucketFactory Bucket factory.
   */
  public RateLimitHandler(@NonNull SneakyThrows.Function<String, Bucket> bucketFactory) {
    this(bucketFactory, Context::getRemoteAddress);
  }

  /**
   * Rate limit per header key.
   *
   * @param bucketFactory Bucket factory.
   * @param headerName Header to use as key.
   */
  public RateLimitHandler(
      @NonNull SneakyThrows.Function<String, Bucket> bucketFactory, @NonNull String headerName) {
    this(bucketFactory, ctx -> ctx.header(headerName).value());
  }

  /**
   * Rate limiter with a custom key provider.
   *
   * @param bucketFactory Bucket factory.
   * @param classifier Key provider.
   */
  public RateLimitHandler(
      @NonNull SneakyThrows.Function<String, Bucket> bucketFactory,
      @NonNull SneakyThrows.Function<Context, String> classifier) {
    this(byKey(bucketFactory, classifier));
  }

  /**
   * Rate limiter with a shared/global bucket.
   *
   * @param bucket Bucket to use.
   */
  public RateLimitHandler(@NonNull Bucket bucket) {
    this((Function<Context, Bucket>) ctx -> bucket);
  }

  private RateLimitHandler(Function<Context, Bucket> factory) {
    this.factory = factory;
  }

  /**
   * Rate limiter per IP/Remote address using a cluster.
   *
   * @param proxyManager Cluster bucket configuration.
   * @return Rate limiter.
   */
  public static @NonNull RateLimitHandler cluster(
      @NonNull SneakyThrows.Function<String, Bucket> proxyManager) {
    return cluster(proxyManager, Context::getRemoteAddress);
  }

  /**
   * Rate limiter per header key using a cluster.
   *
   * @param proxyManager Cluster bucket configuration.
   * @param headerName Header to use as key.
   * @return Rate limiter.
   */
  public static @NonNull RateLimitHandler cluster(
      @NonNull SneakyThrows.Function<String, Bucket> proxyManager, @NonNull String headerName) {
    return cluster(proxyManager, ctx -> ctx.header(headerName).value());
  }

  /**
   * Rate limiter per key using a cluster.
   *
   * @param proxyManager Cluster bucket configuration.
   * @param classifier Key provider.
   * @return Rate limiter.
   */
  public static RateLimitHandler cluster(
      @NonNull SneakyThrows.Function<String, Bucket> proxyManager,
      @NonNull SneakyThrows.Function<Context, String> classifier) {
    return new RateLimitHandler(
        (Function<Context, Bucket>) ctx -> proxyManager.apply(classifier.apply(ctx)));
  }

  @Override
  public void apply(@NonNull Context ctx) throws Exception {
    Bucket bucket = factory.apply(ctx);
    // tryConsume returns false immediately if no tokens available with the bucket
    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      ctx.setResponseHeader("X-Rate-Limit-Remaining", probe.getRemainingTokens());
    } else {
      ctx.setResponseHeader(
          "X-Rate-Limit-Retry-After-Milliseconds",
          NANOSECONDS.toMillis(probe.getNanosToWaitForRefill()));
      ctx.send(StatusCode.TOO_MANY_REQUESTS);
    }
  }

  private static Function<Context, Bucket> byKey(
      SneakyThrows.Function<String, Bucket> bucketFactory,
      SneakyThrows.Function<Context, String> classifier) {
    Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    return ctx -> buckets.computeIfAbsent(classifier.apply(ctx), bucketFactory);
  }
}
