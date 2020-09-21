package io.jooby;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.ConfigFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
  public void signUnsign() {
    assertEquals("1bqmVaHYY/O6zMFHI8iwJXLWaNmYKbYkuMX4gnRdO+Y|foo=bar",
        Cookie.sign("foo=bar", "987654345!$009P"));
    assertEquals("foo=bar",
        Cookie.unsign("1bqmVaHYY/O6zMFHI8iwJXLWaNmYKbYkuMX4gnRdO+Y|foo=bar", "987654345!$009P"));

    assertEquals("RcFzlzECN2Lv32Ie9jfSWVr13j6OjllJwDDZe4mVS4c|foo=bar&x=u iq",
        Cookie.sign("foo=bar&x=u iq", "987654345!$009P"));
    assertEquals("foo=bar&x=u iq", Cookie
        .unsign("RcFzlzECN2Lv32Ie9jfSWVr13j6OjllJwDDZe4mVS4c|foo=bar&x=u iq", "987654345!$009P"));
  }

  @Test
  public void testCreateSameSite() {
    assertEquals(SameSite.LAX, Cookie.create("mycookie",
        ConfigFactory.parseMap(ImmutableMap.of(
            "mycookie.name", "foo",
            "mycookie.value", "bar",
            "mycookie.sameSite", "Lax")))
        .map(Cookie::getSameSite)
        .orElse(null));

    assertEquals(SameSite.NONE, Cookie.create("mycookie",
        ConfigFactory.parseMap(ImmutableMap.of(
            "mycookie.name", "foo",
            "mycookie.value", "bar",
            "mycookie.sameSite", "None",
            "mycookie.secure", true)))
        .map(Cookie::getSameSite)
        .orElse(null));

    Throwable t1 = assertThrows(IllegalArgumentException.class, () -> Cookie.create("mycookie",
        ConfigFactory.parseMap(ImmutableMap.of(
            "mycookie.name", "foo",
            "mycookie.value", "bar",
            "mycookie.sameSite", "None"))));

    assertEquals("Cookies with SameSite=None"
        + " must be flagged as Secure. Call Cookie.setSecure(true)"
        + " before calling Cookie.setSameSite(...).", t1.getMessage());

    Throwable t2 = assertThrows(IllegalArgumentException.class, () -> Cookie.create("mycookie",
        ConfigFactory.parseMap(ImmutableMap.of(
            "mycookie.name", "foo",
            "mycookie.value", "bar",
            "mycookie.sameSite", "Cheese"))));

    assertEquals("Invalid SameSite value 'Cheese'. Use one of: Lax, Strict, None", t2.getMessage());
  }

  @Test
  public void testSameSiteVsSecure() {
    Cookie cookie = new Cookie("foo", "bar");

    Throwable t1 = assertThrows(IllegalArgumentException.class, () -> cookie.setSameSite(SameSite.NONE));

    assertEquals("Cookies with SameSite=None"
        + " must be flagged as Secure. Call Cookie.setSecure(true)"
        + " before calling Cookie.setSameSite(...).", t1.getMessage());

    cookie.setSecure(true);
    cookie.setSameSite(SameSite.NONE);

    assertEquals(SameSite.NONE, cookie.getSameSite());


    Throwable t2 = assertThrows(IllegalArgumentException.class, () -> cookie.setSecure(false));

    assertEquals("Cookies with SameSite=" + cookie.getSameSite().getValue()
        + " must be flagged as Secure. Call Cookie.setSameSite(...) with an argument"
        + " allowing non-secure cookies before calling Cookie.setSecure(false).", t2.getMessage());

    cookie.setSameSite(null);
    cookie.setSecure(false);

    assertFalse(cookie.isSecure());
  }
}
