package org.jooby.issues;

import org.jooby.Jooby;
import org.jooby.spi.Server;
import org.junit.Test;

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

  }

  @Test
  public void customServer() throws Throwable {
    new Jooby().server(NOOP.class).start();
  }

}
