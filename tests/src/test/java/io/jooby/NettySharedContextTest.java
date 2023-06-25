/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static io.jooby.ExecutionMode.DEFAULT;
import static io.jooby.ExecutionMode.EVENT_LOOP;
import static io.jooby.SneakyThrows.throwingSupplier;
import static java.lang.Integer.toHexString;
import static java.lang.System.identityHashCode;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.apache.commons.lang3.Validate.isTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.netty.NettyServer;

public class NettySharedContextTest {

  @ServerTest(
      server = NettyServer.class,
      executionMode = {EVENT_LOOP, DEFAULT})
  public void shouldCheckNettySharedContext(ServerTestRunner runner) {
    var concurrentRequests = 100;
    var numberOfRequests = 5000;
    var contexts = new ConcurrentHashMap<String, AtomicInteger>();
    var serverThreads = new ConcurrentHashMap<String, AtomicInteger>();
    var clientThreads = new ConcurrentHashMap<String, AtomicInteger>();
    var totalRequests = new AtomicInteger();
    var totalErrors = new AtomicInteger();
    var totalResponses = new AtomicInteger();
    runner
        .define(
            app -> {
              app.use(
                  next ->
                      ctx -> {
                        // Make sure attributes is empty
                        isTrue(ctx.getAttributes().isEmpty());
                        // Now set something:
                        ctx.setAttribute("foo", "bar");
                        return next.apply(ctx);
                      });
              app.get(
                  "/rnd",
                  ctx -> {
                    var id = ctx.query("id").value();
                    contexts
                        .computeIfAbsent(
                            toHexString(identityHashCode(ctx)), k -> new AtomicInteger())
                        .incrementAndGet();
                    serverThreads
                        .computeIfAbsent(currentThread().getName(), k -> new AtomicInteger())
                        .incrementAndGet();
                    totalRequests.incrementAndGet();
                    return id;
                  });
            })
        .ready(
            conf -> {
              var executor = Executors.newFixedThreadPool(concurrentRequests);
              var start = System.currentTimeMillis();
              try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
                var futures = new ArrayList<CompletableFuture<String>>();
                for (var r = 0; r < numberOfRequests; r++) {
                  var future =
                      supplyAsync(
                              throwingSupplier(
                                  () -> {
                                    var id = UUID.randomUUID().toString();
                                    HttpGet request =
                                        new HttpGet(
                                            "http://localhost:" + conf.getPort() + "/rnd?id=" + id);
                                    clientThreads
                                        .computeIfAbsent(
                                            currentThread().getName(), k -> new AtomicInteger())
                                        .incrementAndGet();
                                    try (var rsp = client.execute(request)) {
                                      var value = EntityUtils.toString(rsp.getEntity());
                                      assertEquals(id, value);
                                      return value;
                                    }
                                  }),
                              executor)
                          .whenComplete(
                              (rsp, cause) -> {
                                totalResponses.incrementAndGet();
                                if (cause != null) {
                                  totalErrors.incrementAndGet();
                                  cause.printStackTrace();
                                }
                              });
                  futures.add(future);
                }
                futures.stream().map(CompletableFuture::join).collect(Collectors.toList());
                assertEquals(concurrentRequests, clientThreads.size());
                assertEquals(numberOfRequests, totalRequests.intValue());
                assertEquals(0, totalErrors.intValue());
                assertEquals(numberOfRequests, totalResponses.intValue());
                assertTrue(contexts.size() <= serverThreads.size());
              }
              executor.shutdown();
            });
  }
}
