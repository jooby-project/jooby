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

import io.netty.util.AsciiString;

public class NettyDateService implements Runnable {
  private static final int DATE_INTERVAL = 1000;
  private AsciiString date;

  public NettyDateService(ScheduledExecutorService scheduler) {
    scheduler.scheduleAtFixedRate(this, 0, DATE_INTERVAL, TimeUnit.MILLISECONDS);
  }

  public AsciiString date() {
    return this.date;
  }

  @Override
  public void run() {
    this.date =
        new AsciiString(
            DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC)));
  }
}
