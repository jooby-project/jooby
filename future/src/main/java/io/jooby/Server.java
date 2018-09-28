package io.jooby;

import javax.annotation.Nonnull;
import java.nio.file.Path;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public interface Server {

  interface Executor extends java.util.concurrent.Executor {

    default @Override void execute(Runnable task) {
      executeAfter(task, 0, TimeUnit.MILLISECONDS);
    }

    void executeAfter(Runnable task, long delay, TimeUnit unit);
  }

  /** 16KB constant. */
  int _16KB = 0x4000;

  @Nonnull Server mode(@Nonnull Mode mode);

  @Nonnull Server port(int port);

  @Nonnull Server start(@Nonnull Router router);

  @Nonnull Server stop();

  @Nonnull Server tmpdir(@Nonnull Path tmpdir);
}
