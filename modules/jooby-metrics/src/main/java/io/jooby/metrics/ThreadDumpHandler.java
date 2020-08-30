/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.metrics;

import com.codahale.metrics.jvm.ThreadDump;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;
import io.jooby.StatusCode;
import io.jooby.internal.metrics.NoCacheHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;

public class ThreadDumpHandler implements Route.Handler {

  private final Logger log = LoggerFactory.getLogger(getClass());

  private ThreadDump threadDump;

  {
    try {
      // Some PaaS like Google App Engine blacklist java.lang.management
      this.threadDump = new ThreadDump(ManagementFactory.getThreadMXBean());
    } catch (Exception ex) {
      log.warn("Thread dump isn't available", ex);
    }
  }

  @Nonnull
  @Override
  public Object apply(@Nonnull Context ctx) {
    Object data;
    StatusCode status;
    if (threadDump == null) {
      data = "Sorry your runtime environment does not allow to dump threads.";
      status = StatusCode.NOT_IMPLEMENTED;
    } else {
      final ByteArrayOutputStream output = new ByteArrayOutputStream();
      threadDump.dump(output);
      data = output.toByteArray();
      status = StatusCode.OK;
    }

    ctx.setResponseCode(status);
    ctx.setResponseType(MediaType.text);
    NoCacheHeader.add(ctx);
    return data;
  }
}
