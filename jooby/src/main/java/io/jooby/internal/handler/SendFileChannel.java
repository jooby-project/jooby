/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.Route;

import javax.annotation.Nonnull;
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

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    try {
      Object file = next.apply(ctx);
      if (file instanceof File) {
        file = ((File) file).toPath();
      }
      if (file instanceof Path) {
        if (Files.exists((Path) file)) {
          ctx.setDefaultResponseType(MediaType.byFile((Path) file));
          file = FileChannel.open((Path) file, StandardOpenOption.READ);
        } else {
          throw new FileNotFoundException(file.toString());
        }
      }
      return ctx.send((FileChannel) file);
    } catch (Throwable x) {
      return ctx.sendError(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
