package org.jooby.issues;

import org.jooby.Jooby;
import org.jooby.spi.Server;
import org.junit.Test;

import java.util.Optional;
import java.util.concurrent.Executor;

public class Issue430 {

  public static class NOOP implements Server {

    @Override
    public void start() throws Exception {
    }

    @Override
    public void stop() throws Exception {
    }

    @Override
    public void join() throws InterruptedException {
    }

    @Override
    public Optional<Executor> executor() {
      return Optional.empty();
    }

  }

  @Test
  public void customServer() throws Throwable {
    new Jooby().server(NOOP.class).start();
  }

}
