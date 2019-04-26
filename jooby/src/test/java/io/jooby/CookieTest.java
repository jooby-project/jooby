package io.jooby;

import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CookieTest {

  @Test
  public void encodeDecode() {
    assertEquals("", Cookie.encode(ImmutableMap.of()));
    assertEquals(ImmutableMap.of(), Cookie.decode(""));

    assertEquals("foo=bar", Cookie.encode(ImmutableMap.of("foo", "bar")));
    assertEquals(ImmutableMap.of(), Cookie.decode("foo"));
    assertEquals(ImmutableMap.of(), Cookie.decode("foo="));
    assertEquals(ImmutableMap.of("foo", "b"), Cookie.decode("foo=b"));
    assertEquals(ImmutableMap.of("foo", "bar"), Cookie.decode("foo=bar"));
    assertEquals(ImmutableMap.of("foo", "bar"), Cookie.decode("foo=bar&"));
    assertEquals(ImmutableMap.of("foo", "bar"), Cookie.decode("foo=bar&&&"));
    assertEquals(ImmutableMap.of("foo", "bar", "x", "y"), Cookie.decode("foo=bar&x=y"));
    assertEquals(ImmutableMap.of("foo", "bar", "x", "y"), Cookie.decode("foo=bar&x=y&u"));
    assertEquals("foo=bar+ok", Cookie.encode(ImmutableMap.of("foo", "bar ok")));
    assertEquals(ImmutableMap.of("foo", "bar ok"), Cookie.decode("foo=bar+ok"));
    assertEquals("foo=bar&x=y", Cookie.encode(ImmutableMap.of("foo", "bar", "x", "y")));
  }

  @Test
  public void singUnsign() {
    assertEquals("1bqmVaHYY/O6zMFHI8iwJXLWaNmYKbYkuMX4gnRdO+Y|foo=bar",
        Cookie.sign("foo=bar", "987654345!$009P"));
    assertEquals("foo=bar",
        Cookie.unsign("1bqmVaHYY/O6zMFHI8iwJXLWaNmYKbYkuMX4gnRdO+Y|foo=bar", "987654345!$009P"));

    assertEquals("RcFzlzECN2Lv32Ie9jfSWVr13j6OjllJwDDZe4mVS4c|foo=bar&x=u iq",
        Cookie.sign("foo=bar&x=u iq", "987654345!$009P"));
    assertEquals("foo=bar&x=u iq", Cookie
        .unsign("RcFzlzECN2Lv32Ie9jfSWVr13j6OjllJwDDZe4mVS4c|foo=bar&x=u iq", "987654345!$009P"));
  }
}
