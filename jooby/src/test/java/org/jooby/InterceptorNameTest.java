package org.jooby;

import static org.junit.Assert.assertEquals;

import java.util.Optional;

import org.junit.Test;

public class InterceptorNameTest {

  @Test
  public void before() {
    assertEquals("before", new Route.Before() {
      @Override
      public void handle(final Request req, final Response rsp) throws Throwable {
      }
    }.name());
  }

  @Test
  public void beforeSend() {
    assertEquals("before-send", new Route.BeforeSend() {

      @Override
      public Result handle(final Request req, final Response rsp, final Result result)
          throws Exception {
        return null;
      }
    }.name());
  }

  @Test
  public void after() {
    assertEquals("after", new Route.After() {
      @Override
      public void handle(final Request req, final Response rsp, final Optional<Throwable> cause) {
      }
    }.name());
  }

}
