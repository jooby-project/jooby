/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.jooby.SneakyThrows;

public class NettyDateService {
  private static final AtomicReference<String> cachedDateString = new AtomicReference<>();
  private static final Runnable INVALIDATE_TASK = () -> cachedDateString.set(null);
  private static final int DATE_INTERVAL = 1000;
  private final ScheduledExecutorService scheduler;

  public NettyDateService(ScheduledExecutorService scheduler) {
    this.scheduler = scheduler;
  }

  public String date() {
    var dateString = cachedDateString.get();
    if (dateString == null) {
      // set the time and register a timer to invalidate it
      // note that this is racey, it does not matter if multiple threads do this
      // the perf cost of synchronizing would be more than the perf cost of multiple threads running
      // it
      long realTime = System.currentTimeMillis();
      long mod = realTime % DATE_INTERVAL;
      long toGo = DATE_INTERVAL - mod;
      dateString = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
      if (cachedDateString.compareAndSet(null, dateString)) {
        try {
          scheduler.schedule(INVALIDATE_TASK, toGo, TimeUnit.MILLISECONDS);
        } catch (RejectedExecutionException rejected) {
          if (!scheduler.isShutdown()) {
            throw SneakyThrows.propagate(rejected);
          }
        }
      }
    }
    return dateString;
  }
}
