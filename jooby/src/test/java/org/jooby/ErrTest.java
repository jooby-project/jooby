package org.jooby;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ErrTest {

  @Test
  public void exceptionWithStatus() {
    Err exception = new Err(Status.NOT_FOUND);

    assertEquals(Status.NOT_FOUND.value(), exception.statusCode());
    assertEquals("Not Found(404)", exception.getMessage());
  }

  @Test
  public void exceptionWithIntStatus() {
    Err exception = new Err(404);

    assertEquals(Status.NOT_FOUND.value(), exception.statusCode());
    assertEquals("Not Found(404)", exception.getMessage());
  }

  @Test
  public void exceptionWithStatusAndCause() {
    Exception cause = new IllegalArgumentException();
    Err exception = new Err(Status.NOT_FOUND, cause);

    assertEquals(Status.NOT_FOUND.value(), exception.statusCode());
    assertEquals("Not Found(404)", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void exceptionWithIntStatusAndCause() {
    Exception cause = new IllegalArgumentException();
    Err exception = new Err(404, cause);

    assertEquals(Status.NOT_FOUND.value(), exception.statusCode());
    assertEquals("Not Found(404)", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void exceptionWithStatusAndMessage() {
    Err exception = new Err(Status.NOT_FOUND, "GET/missing");

    assertEquals(Status.NOT_FOUND.value(), exception.statusCode());
    assertEquals("Not Found(404): GET/missing", exception.getMessage());
  }

  @Test
  public void exceptionWithIntStatusAndMessage() {
    Err exception = new Err(404, "GET/missing");

    assertEquals(Status.NOT_FOUND.value(), exception.statusCode());
    assertEquals("Not Found(404): GET/missing", exception.getMessage());
  }

  @Test
  public void exceptionWithStatusCauseAndMessage() {
    Exception cause = new IllegalArgumentException();
    Err exception = new Err(Status.NOT_FOUND, "GET/missing", cause);

    assertEquals(Status.NOT_FOUND.value(), exception.statusCode());
    assertEquals("Not Found(404): GET/missing", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void exceptionWithIntStatusCauseAndMessage() {
    Exception cause = new IllegalArgumentException();
    Err exception = new Err(404, "GET/missing", cause);

    assertEquals(Status.NOT_FOUND.value(), exception.statusCode());
    assertEquals("Not Found(404): GET/missing", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

}
