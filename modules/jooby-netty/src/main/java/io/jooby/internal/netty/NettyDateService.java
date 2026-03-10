/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.*;

import io.netty.util.concurrent.DefaultThreadFactory;

public class NettyDateService implements Runnable {
  private static final int DATE_INTERVAL = 1000;
  private final ScheduledExecutorService scheduler;
  // Make volatile for thread visibility across EventLoop threads
  private volatile CharSequence date;
  private final ScheduledFuture<?> future;

  public NettyDateService() {
    this.scheduler =
        Executors.newSingleThreadScheduledExecutor(new DefaultThreadFactory("http-date-keeper"));
    this.date =
        new NettyString(
            DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
    this.future =
        scheduler.scheduleAtFixedRate(this, DATE_INTERVAL, DATE_INTERVAL, TimeUnit.MILLISECONDS);
  }

  public CharSequence date() {
    return this.date;
  }

  @Override
  public void run() {
    this.date =
        new NettyString(
            DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
  }

  // Allow the server to gracefully terminate the background task
  public void stop() {
    try {
      if (this.future != null) {
        this.future.cancel(false);
      }
    } catch (Exception ignored) {
      if (scheduler != null) {
        scheduler.shutdown();
      }
    }
  }
}
