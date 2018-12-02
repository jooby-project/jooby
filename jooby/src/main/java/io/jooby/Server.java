package io.jooby;

import javax.annotation.Nonnull;
import java.util.concurrent.TimeUnit;

public interface Server {

  /** 16KB constant. */
  int _16KB = 0x4000;

  int port();

  @Nonnull Server port(int port);

  @Nonnull Server deploy(App application);

  @Nonnull Server start();

  @Nonnull default void join() {
    try {
      Thread.currentThread().join();
    } catch (InterruptedException x) {
      Thread.currentThread().interrupt();
    }
  }

  @Nonnull Server stop();

}
