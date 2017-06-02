package org.jooby.servlet;

import static org.junit.Assert.assertFalse;

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

  @Test
  public void excutor() throws Exception {
    assertFalse(ServletContainer.NOOP.executor().isPresent());
  }
}
