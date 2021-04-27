package io.jooby;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultContextTest {
  private final DefaultContext ctx = mock(DefaultContext.class);

  @BeforeEach
  public void setUp() {
    when(ctx.getScheme()).thenReturn("https");
    when(ctx.getHost()).thenReturn("some-host");
    when(ctx.getPort()).thenReturn(443);
    when(ctx.getContextPath()).thenReturn("/context");
    when(ctx.getRequestPath()).thenReturn("/path");
    when(ctx.queryString()).thenReturn("?query");

    when(ctx.getRequestURL()).thenCallRealMethod();
    when(ctx.getRequestURL(any())).thenCallRealMethod();
  }

  @Test
  public void getRequestURL() {
    assertEquals("https://some-host/path?query", ctx.getRequestURL());
  }

  @Test
  public void getRequestURL_withNonStandardPort() {
    when(ctx.getPort()).thenReturn(999);
    assertEquals("https://some-host:999/path?query", ctx.getRequestURL());
  }

  @Test
  public void getRequestURL_withCustomPathWithoutQueryString() {
    assertEquals("https://some-host/context/my-path", ctx.getRequestURL("/my-path"));
  }
}
