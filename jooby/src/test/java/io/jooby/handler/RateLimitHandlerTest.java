/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.jooby.Context;
import io.jooby.StatusCode;
import io.jooby.value.Value;

public class RateLimitHandlerTest {

  private Context ctx;
  private Bucket bucket;
  private ConsumptionProbe probe;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    bucket = mock(Bucket.class);
    probe = mock(ConsumptionProbe.class);

    // Fix: bucket4j expects a long, so we must use anyLong() instead of anyInt()
    when(bucket.tryConsumeAndReturnRemaining(anyLong())).thenReturn(probe);
  }

  @Test
  @DisplayName("Verify single shared bucket and successful consumption branch")
  void testSharedBucketAndSuccessfulConsumption() throws Exception {
    RateLimitHandler handler = new RateLimitHandler(bucket);

    when(probe.isConsumed()).thenReturn(true);
    when(probe.getRemainingTokens()).thenReturn(42L);

    handler.apply(ctx);

    verify(ctx).setResponseHeader("X-Rate-Limit-Remaining", 42L);
  }

  @Test
  @DisplayName("Verify rejected consumption branch sends TOO_MANY_REQUESTS and retry header")
  void testRejectedConsumption() throws Exception {
    RateLimitHandler handler = new RateLimitHandler(bucket);

    when(probe.isConsumed()).thenReturn(false);

    // Simulate 2000 milliseconds in nanos
    long nanosToWait = TimeUnit.MILLISECONDS.toNanos(2000);
    when(probe.getNanosToWaitForRefill()).thenReturn(nanosToWait);

    handler.apply(ctx);

    verify(ctx).setResponseHeader("X-Rate-Limit-Retry-After-Milliseconds", 2000L);
    verify(ctx).send(StatusCode.TOO_MANY_REQUESTS);
  }

  @Test
  @DisplayName("Verify RemoteAddress constructor and local caching via ConcurrentHashMap")
  void testRemoteAddressConstructorAndCaching() throws Exception {
    AtomicInteger factoryCalls = new AtomicInteger(0);
    RateLimitHandler handler =
        new RateLimitHandler(
            key -> {
              factoryCalls.incrementAndGet();
              assertEquals("192.168.1.1", key);
              return bucket;
            });

    when(ctx.getRemoteAddress()).thenReturn("192.168.1.1");
    when(probe.isConsumed()).thenReturn(true);

    // Call twice for the same IP
    handler.apply(ctx);
    handler.apply(ctx);

    // Factory should only be invoked once due to the byKey ConcurrentHashMap cache
    assertEquals(1, factoryCalls.get());
    verify(ctx, times(2)).getRemoteAddress();
  }

  @Test
  @DisplayName("Verify Header constructor extracts key correctly")
  void testHeaderConstructor() throws Exception {
    RateLimitHandler handler =
        new RateLimitHandler(
            key -> {
              assertEquals("my-api-key", key);
              return bucket;
            },
            "X-API-Key");

    Value headerValue = mock(Value.class);
    when(ctx.header("X-API-Key")).thenReturn(headerValue);
    when(headerValue.value()).thenReturn("my-api-key");
    when(probe.isConsumed()).thenReturn(true);

    handler.apply(ctx);

    verify(ctx).header("X-API-Key");
  }

  @Test
  @DisplayName("Verify custom classifier constructor")
  void testCustomClassifierConstructor() throws Exception {
    RateLimitHandler handler = new RateLimitHandler(key -> bucket, c -> "custom-key");

    when(probe.isConsumed()).thenReturn(true);
    handler.apply(ctx);
    // Success implies the custom classifier executed without errors
  }

  @Test
  @DisplayName("Verify cluster RemoteAddress factory method")
  void testClusterRemoteAddress() throws Exception {
    RateLimitHandler handler =
        RateLimitHandler.cluster(
            key -> {
              assertEquals("10.0.0.1", key);
              return bucket;
            });

    when(ctx.getRemoteAddress()).thenReturn("10.0.0.1");
    when(probe.isConsumed()).thenReturn(true);

    handler.apply(ctx);

    verify(ctx).getRemoteAddress();
  }

  @Test
  @DisplayName("Verify cluster Header factory method")
  void testClusterHeader() throws Exception {
    RateLimitHandler handler =
        RateLimitHandler.cluster(
            key -> {
              assertEquals("cluster-api-key", key);
              return bucket;
            },
            "Cluster-Auth");

    Value headerValue = mock(Value.class);
    when(ctx.header("Cluster-Auth")).thenReturn(headerValue);
    when(headerValue.value()).thenReturn("cluster-api-key");
    when(probe.isConsumed()).thenReturn(true);

    handler.apply(ctx);

    verify(ctx).header("Cluster-Auth");
  }

  @Test
  @DisplayName("Verify cluster custom classifier prevents local caching")
  void testClusterCustomClassifierNoCaching() throws Exception {
    AtomicInteger factoryCalls = new AtomicInteger(0);

    // The cluster proxy manager factory should be called on every request
    RateLimitHandler handler =
        RateLimitHandler.cluster(
            key -> {
              factoryCalls.incrementAndGet();
              return bucket;
            },
            c -> "dynamic-cluster-key");

    when(probe.isConsumed()).thenReturn(true);

    // Call twice
    handler.apply(ctx);
    handler.apply(ctx);

    // Unlike standard constructors using byKey(), the cluster implementation
    // queries the proxy manager dynamically on every request.
    assertEquals(2, factoryCalls.get());
  }
}
