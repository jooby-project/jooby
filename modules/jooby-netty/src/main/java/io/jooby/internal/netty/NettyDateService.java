/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.netty;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NettyDateService implements Runnable {
  private static final int DATE_INTERVAL = 1000;
  private CharSequence date;

  public NettyDateService(ScheduledExecutorService scheduler) {
    this.date =
        new NettyString(
            DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
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
}
