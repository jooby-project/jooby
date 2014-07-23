package jooby;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class HttpExceptionTest {

  @Test
  public void exceptionWithStatus() {
    HttpException exception = new HttpException(HttpStatus.NOT_FOUND);

    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals("Not Found(404): ", exception.getMessage());
  }

  @Test
  public void exceptionWithStatusAndCause() {
    Exception cause = new IllegalArgumentException();
    HttpException exception = new HttpException(HttpStatus.NOT_FOUND, cause);

    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals("Not Found(404): ", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void exceptionWithStatusAndMessage() {
    HttpException exception = new HttpException(HttpStatus.NOT_FOUND, "GET/missing");

    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals("Not Found(404): GET/missing", exception.getMessage());
  }

  @Test
  public void exceptionWithStatusCauseAndMessage() {
    Exception cause = new IllegalArgumentException();
    HttpException exception = new HttpException(HttpStatus.NOT_FOUND, "GET/missing", cause);

    assertEquals(HttpStatus.NOT_FOUND, exception.status());
    assertEquals("Not Found(404): GET/missing", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

}
