package org.jooby;

import static org.junit.Assert.assertEquals;

import org.jooby.Response;
import org.jooby.Route;
import org.junit.Test;

public class HttpExceptionTest {

  @Test
  public void exceptionWithStatus() {
    Route.Err exception = new Route.Err(Response.Status.NOT_FOUND);

    assertEquals(Response.Status.NOT_FOUND, exception.status());
    assertEquals("Not Found(404): ", exception.getMessage());
  }

  @Test
  public void exceptionWithStatusAndCause() {
    Exception cause = new IllegalArgumentException();
    Route.Err exception = new Route.Err(Response.Status.NOT_FOUND, cause);

    assertEquals(Response.Status.NOT_FOUND, exception.status());
    assertEquals("Not Found(404): ", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void exceptionWithStatusAndMessage() {
    Route.Err exception = new Route.Err(Response.Status.NOT_FOUND, "GET/missing");

    assertEquals(Response.Status.NOT_FOUND, exception.status());
    assertEquals("Not Found(404): GET/missing", exception.getMessage());
  }

  @Test
  public void exceptionWithStatusCauseAndMessage() {
    Exception cause = new IllegalArgumentException();
    Route.Err exception = new Route.Err(Response.Status.NOT_FOUND, "GET/missing", cause);

    assertEquals(Response.Status.NOT_FOUND, exception.status());
    assertEquals("Not Found(404): GET/missing", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

}
