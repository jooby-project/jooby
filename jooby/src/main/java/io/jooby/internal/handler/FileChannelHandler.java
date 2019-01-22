package io.jooby.internal.handler;

import io.jooby.Context;
import io.jooby.Route;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Function;

public class FileChannelHandler implements ChainedHandler {
  private Route.Handler next;

  public FileChannelHandler(Route.Handler next) {
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
          file = FileChannel.open((Path) file, StandardOpenOption.READ);
        } else {
          throw new FileNotFoundException(file.toString());
        }
      }
      return ctx.sendFile((FileChannel) file);
    } catch (Throwable x) {
      return ctx.sendError(x);
    }
  }

  @Override public Route.Handler next() {
    return next;
  }
}
