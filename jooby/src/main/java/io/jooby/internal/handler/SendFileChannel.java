/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class SendFileChannel implements LinkedHandler {
  private Route.Handler next;

  public SendFileChannel(Route.Handler next) {
    this.next = next;
  }

  @NonNull @Override public Object apply(@NonNull Context ctx) {
    try {
      Object result = next.apply(ctx);
      if (ctx.isResponseStarted()) {
        return result;
      }
      if (result instanceof File) {
        result = ((File) result).toPath();
      }
      if (result instanceof Path) {
        if (Files.exists((Path) result)) {
          ctx.setDefaultResponseType(MediaType.byFile((Path) result));
          result = FileChannel.open((Path) result, StandardOpenOption.READ);
        } else {
          throw new FileNotFoundException(result.toString());
        }
      }
      return ctx.send((FileChannel) result);
    } catch (Throwable x) {
      return ctx.sendError(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
