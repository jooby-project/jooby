/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.Duration;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import com.google.common.collect.ImmutableMap;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class CookieTest {

  @Test
  public void encodeDecode() {
    assertEquals("", Cookie.encode(ImmutableMap.of()));
    assertEquals("", Cookie.encode(null));
    assertEquals(ImmutableMap.of(), Cookie.decode(""));
    assertEquals(ImmutableMap.of(), Cookie.decode(null));

    assertEquals("foo=bar", Cookie.encode(ImmutableMap.of("foo", "bar")));
    assertEquals(ImmutableMap.of(), Cookie.decode("foo"));
    assertEquals(ImmutableMap.of(), Cookie.decode("foo="));
    assertEquals(ImmutableMap.of(), Cookie.decode("=foo")); // eq == 0
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
  public void encodeDecodeExceptions() {
    // Tests the unreachable UnsupportedEncodingException catch blocks using Mockito
    try (MockedStatic<URLEncoder> encoder = mockStatic(URLEncoder.class);
        MockedStatic<URLDecoder> decoder = mockStatic(URLDecoder.class)) {

      encoder
          .when(() -> URLEncoder.encode(anyString(), anyString()))
          .thenThrow(new UnsupportedEncodingException("mock error"));

      assertThrows(
          UnsupportedEncodingException.class, () -> Cookie.encode(ImmutableMap.of("a", "b")));

      decoder
          .when(() -> URLDecoder.decode(anyString(), anyString()))
          .thenThrow(new UnsupportedEncodingException("mock error"));

      assertThrows(UnsupportedEncodingException.class, () -> Cookie.decode("a=b"));
    }
  }

  @Test
  public void signUnsign() {
    assertEquals(
        "1bqmVaHYY/O6zMFHI8iwJXLWaNmYKbYkuMX4gnRdO+Y|foo=bar",
        Cookie.sign("foo=bar", "987654345!$009P"));
    assertEquals(
        "foo=bar",
        Cookie.unsign("1bqmVaHYY/O6zMFHI8iwJXLWaNmYKbYkuMX4gnRdO+Y|foo=bar", "987654345!$009P"));

    assertEquals(
        "RcFzlzECN2Lv32Ie9jfSWVr13j6OjllJwDDZe4mVS4c|foo=bar&x=u iq",
        Cookie.sign("foo=bar&x=u iq", "987654345!$009P"));
    assertEquals(
        "foo=bar&x=u iq",
        Cookie.unsign(
            "RcFzlzECN2Lv32Ie9jfSWVr13j6OjllJwDDZe4mVS4c|foo=bar&x=u iq", "987654345!$009P"));
  }

  @Test
  public void signUnsignEdgeCasesAndExceptions() {
    String secret = "987654345!$009P";
    assertNull(Cookie.unsign("noseparator", secret));
    assertNull(Cookie.unsign("|emptyvalue", secret));
    assertNull(Cookie.unsign("invalidHash|foo=bar", secret));

    // Force exception in sign block (NullPointerException wrapped in SneakyThrows)
    assertThrows(RuntimeException.class, () -> Cookie.sign(null, secret));
  }

  @Test
  public void testConstructorsGettersSettersAndClone() {
    Cookie c = new Cookie("sid");
    assertEquals("sid", c.getName());
    assertNull(c.getValue());

    c.setValue("123");
    assertEquals("123", c.getValue());

    assertNull(c.getDomain());
    assertEquals("def.com", c.getDomain("def.com"));
    c.setDomain("foo.com");
    assertEquals("foo.com", c.getDomain());
    assertEquals("foo.com", c.getDomain("def.com"));

    assertNull(c.getPath());
    assertEquals("/def", c.getPath("/def"));
    c.setPath("/api");
    assertEquals("/api", c.getPath());
    assertEquals("/api", c.getPath("/def"));

    assertFalse(c.isHttpOnly());
    c.setHttpOnly(true);
    assertTrue(c.isHttpOnly());

    assertFalse(c.isSecure());
    c.setSecure(true);
    assertTrue(c.isSecure());

    assertEquals(-1, c.getMaxAge());
    c.setMaxAge(Duration.ofSeconds(60));
    assertEquals(60, c.getMaxAge());

    c.setMaxAge(-5);
    assertEquals(-1, c.getMaxAge());

    c.setSameSite(SameSite.STRICT);
    assertEquals(SameSite.STRICT, c.getSameSite());

    c.setName("new-name");
    assertEquals("new-name", c.getName());

    Cookie cloned = c.clone();
    assertEquals(c.getName(), cloned.getName());
    assertEquals(c.getValue(), cloned.getValue());
    assertEquals(c.getDomain(), cloned.getDomain());
    assertEquals(c.getPath(), cloned.getPath());
    assertEquals(c.isHttpOnly(), cloned.isHttpOnly());
    assertEquals(c.isSecure(), cloned.isSecure());
    assertEquals(c.getMaxAge(), cloned.getMaxAge());
    assertEquals(c.getSameSite(), cloned.getSameSite());
  }

  @Test
  public void testToStringAndToCookieString() {
    Cookie c = new Cookie("foo");
    assertEquals("foo=", c.toString());
    assertEquals("foo=", c.toCookieString());

    c.setValue("bar");
    assertEquals("foo=bar", c.toString());

    c.setPath("/");
    c.setDomain("example.com");
    c.setSameSite(SameSite.LAX);
    c.setSecure(true);
    c.setHttpOnly(true);
    c.setMaxAge(0);

    String cs = c.toCookieString();
    assertTrue(cs.contains("foo=bar"));
    assertTrue(cs.contains("Path=/"));
    assertTrue(cs.contains("Domain=example.com"));
    assertTrue(cs.contains("SameSite=Lax"));
    assertTrue(cs.contains("Secure"));
    assertTrue(cs.contains("HttpOnly"));
    assertTrue(cs.contains("Max-Age=0"));
    assertTrue(cs.contains("Expires=Thu, 01-Jan-1970 00:00:00 GMT")); // Check exact 0 expiration

    c.setMaxAge(3600); // maxAge > 0 branch
    cs = c.toCookieString();
    assertTrue(cs.contains("Max-Age=3600"));
    assertTrue(cs.contains("Expires="));

    c.setMaxAge(-1); // maxAge < 0 branch
    cs = c.toCookieString();
    assertFalse(cs.contains("Max-Age"));
    assertFalse(cs.contains("Expires"));
  }

  @Test
  public void testQuotesAndEscaping() {
    Cookie c = new Cookie("foo", "v a l u e"); // Space needs quotes
    assertEquals("foo=\"v a l u e\"", c.toCookieString());

    c = new Cookie("foo", "val,ue"); // Comma
    assertEquals("foo=\"val,ue\"", c.toCookieString());

    c = new Cookie("foo", "val;ue"); // Semicolon
    assertEquals("foo=\"val;ue\"", c.toCookieString());

    c = new Cookie("foo", "val\\ue"); // Backslash -> gets escaped
    assertEquals("foo=\"val\\\\ue\"", c.toCookieString());

    c = new Cookie("foo", "val\"ue"); // Double quote -> gets escaped
    assertEquals("foo=\"val\\\"ue\"", c.toCookieString());

    c = new Cookie("foo", "val\tue"); // Tab
    assertEquals("foo=\"val\tue\"", c.toCookieString());

    // Already quoted -> doesn't need quotes if properly enclosed
    c = new Cookie("foo", "\"already quoted\"");
    assertEquals("foo=\"already quoted\"", c.toCookieString());

    // Already quoted but effectively empty body -> length condition edge case
    c = new Cookie("foo", "\"\"");
    assertEquals("foo=\"\"", c.toCookieString());

    // Single quote char requires escaping and surrounding quotes
    c = new Cookie("foo", "\"");
    assertEquals("foo=\"\\\"\"", c.toCookieString());
  }

  @Test
  public void testSessionCookieFactory() {
    Cookie session = Cookie.session("my-sid");
    assertEquals("my-sid", session.getName());
    assertNull(session.getValue());
    assertEquals(-1, session.getMaxAge());
    assertTrue(session.isHttpOnly());
    assertEquals("/", session.getPath());
  }

  @Test
  public void testCreateFromConfig() {
    Config config =
        ConfigFactory.parseMap(
            ImmutableMap.<String, Object>builder()
                .put("session.name", "sid")
                .put("session.value", "123")
                .put("session.path", "/app")
                .put("session.domain", "localhost")
                .put("session.secure", false)
                .put("session.httpOnly", true)
                .put("session.maxAge", "1h")
                .put("session.sameSite", "Strict")
                .build());

    Optional<Cookie> cOpt = Cookie.create("session", config);
    assertTrue(cOpt.isPresent());

    Cookie c = cOpt.get();
    assertEquals("sid", c.getName());
    assertEquals("123", c.getValue());
    assertEquals("/app", c.getPath());
    assertEquals("localhost", c.getDomain());
    assertFalse(c.isSecure());
    assertTrue(c.isHttpOnly());
    assertEquals(3600, c.getMaxAge());
    assertEquals(SameSite.STRICT, c.getSameSite());

    // Empty config namespace branch
    assertFalse(Cookie.create("missing", config).isPresent());

    // Partial config namespace branch (missing optional values)
    Config partialConfig = ConfigFactory.parseMap(ImmutableMap.of("session.name", "partial"));
    Cookie partialCookie = Cookie.create("session", partialConfig).get();
    assertEquals("partial", partialCookie.getName());
    assertNull(partialCookie.getValue());
  }

  @Test
  public void testCreateSameSite() {
    assertEquals(
        SameSite.LAX,
        Cookie.create(
                "mycookie",
                ConfigFactory.parseMap(
                    ImmutableMap.of(
                        "mycookie.name", "foo",
                        "mycookie.value", "bar",
                        "mycookie.sameSite", "Lax")))
            .map(Cookie::getSameSite)
            .orElse(null));

    assertEquals(
        SameSite.NONE,
        Cookie.create(
                "mycookie",
                ConfigFactory.parseMap(
                    ImmutableMap.of(
                        "mycookie.name", "foo",
                        "mycookie.value", "bar",
                        "mycookie.sameSite", "None",
                        "mycookie.secure", true)))
            .map(Cookie::getSameSite)
            .orElse(null));

    Throwable t1 =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Cookie.create(
                    "mycookie",
                    ConfigFactory.parseMap(
                        ImmutableMap.of(
                            "mycookie.name", "foo",
                            "mycookie.value", "bar",
                            "mycookie.sameSite", "None"))));

    assertEquals(
        "Cookies with SameSite=None"
            + " must be flagged as Secure. Call Cookie.setSecure(true)"
            + " before calling Cookie.setSameSite(...).",
        t1.getMessage());

    Throwable t2 =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                Cookie.create(
                    "mycookie",
                    ConfigFactory.parseMap(
                        ImmutableMap.of(
                            "mycookie.name", "foo",
                            "mycookie.value", "bar",
                            "mycookie.sameSite", "Cheese"))));

    assertEquals("Invalid SameSite value 'Cheese'. Use one of: Lax, Strict, None", t2.getMessage());
  }

  @Test
  public void testSameSiteVsSecure() {
    Cookie cookie = new Cookie("foo", "bar");

    Throwable t1 =
        assertThrows(IllegalArgumentException.class, () -> cookie.setSameSite(SameSite.NONE));

    assertEquals(
        "Cookies with SameSite=None"
            + " must be flagged as Secure. Call Cookie.setSecure(true)"
            + " before calling Cookie.setSameSite(...).",
        t1.getMessage());

    cookie.setSecure(true);
    cookie.setSameSite(SameSite.NONE);

    assertEquals(SameSite.NONE, cookie.getSameSite());

    Throwable t2 = assertThrows(IllegalArgumentException.class, () -> cookie.setSecure(false));

    assertEquals(
        "Cookies with SameSite="
            + cookie.getSameSite().getValue()
            + " must be flagged as Secure. Call Cookie.setSameSite(...) with an argument"
            + " allowing non-secure cookies before calling Cookie.setSecure(false).",
        t2.getMessage());

    // Allows secure = false if SameSite does not mandate it
    cookie.setSameSite(SameSite.LAX);
    cookie.setSecure(false);
    assertFalse(cookie.isSecure());

    cookie.setSameSite(null);
    cookie.setSecure(false);
    assertFalse(cookie.isSecure());
  }
}
