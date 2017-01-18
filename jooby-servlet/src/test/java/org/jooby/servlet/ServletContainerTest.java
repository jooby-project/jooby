package org.jooby.servlet;

import org.junit.Test;

public class ServletContainerTest {

  @Test
  public void start() throws Exception {
    ServletContainer.NOOP.start();
  }

  @Test
  public void stop() throws Exception {
    ServletContainer.NOOP.stop();
  }

  @Test
  public void join() throws Exception {
    ServletContainer.NOOP.join();
  }
}
