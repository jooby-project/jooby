package io.jooby.exception;

import io.jooby.StatusCode;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ExceptionTest {

  @Test
  public void shouldCreateBadRequestWithMessage() {
    BadRequestException exception = new BadRequestException("Some");
    assertEquals(StatusCode.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Some", exception.getMessage());
    assertEquals(null, exception.getCause());
  }

  @Test
  public void shouldCreateBadRequestWithMessageCause() {
    IllegalArgumentException cause = new IllegalArgumentException();
    BadRequestException exception = new BadRequestException("Some", cause);
    assertEquals(StatusCode.BAD_REQUEST, exception.getStatusCode());
    assertEquals("Some", exception.getMessage());
    assertEquals(cause, exception.getCause());
  }

  @Test
  public void shouldCreateForbidden() {
    ForbiddenException exception = new ForbiddenException("Some");
    assertEquals(StatusCode.FORBIDDEN, exception.getStatusCode());
    assertEquals("Some", exception.getMessage());
    assertEquals(null, exception.getCause());

    ForbiddenException exceptionNoMessage = new ForbiddenException();
    assertEquals(StatusCode.FORBIDDEN, exceptionNoMessage.getStatusCode());
    assertEquals("", exceptionNoMessage.getMessage());
    assertEquals(null, exceptionNoMessage.getCause());
  }

  @Test
  public void shouldCreateMethodNotAllowed() {
    MethodNotAllowedException exception = new MethodNotAllowedException("GET", Collections.singletonList("POST"));
    assertEquals(StatusCode.METHOD_NOT_ALLOWED, exception.getStatusCode());
    assertEquals("GET", exception.getMethod());
    assertEquals(Collections.singletonList("POST"), exception.getAllow());
    assertEquals(null, exception.getCause());
  }

  @Test
  public void shouldCreateNotAcceptable() {
    NotAcceptableException exception = new NotAcceptableException("text/html");
    assertEquals(StatusCode.NOT_ACCEPTABLE, exception.getStatusCode());
    assertEquals("text/html", exception.getContentType());
    assertEquals(null, exception.getCause());

    NotAcceptableException nulltype = new NotAcceptableException(null);
    assertEquals(StatusCode.NOT_ACCEPTABLE, nulltype.getStatusCode());
    assertEquals(null, nulltype.getContentType());
    assertEquals(null, nulltype.getCause());
  }

  @Test
  public void shouldCreateUnauthorized() {
    UnauthorizedException exception = new UnauthorizedException("Some");
    assertEquals(StatusCode.UNAUTHORIZED, exception.getStatusCode());
    assertEquals("Some", exception.getMessage());
    assertEquals(null, exception.getCause());

    UnauthorizedException exceptionNoMessage = new UnauthorizedException();
    assertEquals(StatusCode.UNAUTHORIZED, exceptionNoMessage.getStatusCode());
    assertEquals("", exceptionNoMessage.getMessage());
    assertEquals(null, exceptionNoMessage.getCause());
  }

  @Test
  public void shouldCreateUnsupportedMediaType() {
    UnsupportedMediaType exception = new UnsupportedMediaType("Some");
    assertEquals(StatusCode.UNSUPPORTED_MEDIA_TYPE, exception.getStatusCode());
    assertEquals("Some", exception.getMessage());
    assertEquals(null, exception.getCause());

    UnsupportedMediaType exceptionNoMessage = new UnsupportedMediaType(null);
    assertEquals(StatusCode.UNSUPPORTED_MEDIA_TYPE, exceptionNoMessage.getStatusCode());
    assertEquals(null, exceptionNoMessage.getMessage());
    assertEquals(null, exceptionNoMessage.getCause());
  }
}
