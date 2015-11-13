package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import org.jooby.Cookie;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Cookie.Definition.class, CookieImpl.class, System.class })
public class CookieImplTest {

  static final DateTimeFormatter fmt = DateTimeFormatter
      .ofPattern("E, dd-MMM-yyyy HH:mm:ss z", Locale.ENGLISH)
      .withZone(ZoneId.of("GMT"));

  @Test
  public void encodeNameAndValue() throws Exception {
    assertEquals("jooby.sid=1234;Version=1", new Cookie.Definition("jooby.sid", "1234").toCookie()
        .encode());
  }

  @Test
  public void escapeQuote() throws Exception {
    assertEquals("jooby.sid=\"a\\\"b\";Version=1", new Cookie.Definition("jooby.sid", "a\"b").toCookie()
        .encode());
  }

  @Test
  public void escapeSlash() throws Exception {
    assertEquals("jooby.sid=\"a\\\\b\";Version=1", new Cookie.Definition("jooby.sid", "a\\b").toCookie()
        .encode());
  }

  @Test
  public void oneChar() throws Exception {
    assertEquals("jooby.sid=1;Version=1", new Cookie.Definition("jooby.sid", "1").toCookie()
        .encode());
  }

  @Test
  public void escapeValueStartingWithQuoute() throws Exception {
    assertEquals("jooby.sid=\"\\\"1\";Version=1", new Cookie.Definition("jooby.sid", "\"1").toCookie()
        .encode());
  }

  @Test(expected = IllegalArgumentException.class)
  public void badChar() throws Exception {
    char ch = '\n';
    new Cookie.Definition("name", "" + ch).toCookie().encode();
  }

  @Test(expected = IllegalArgumentException.class)
  public void badChar2() throws Exception {
    char ch = 0x7f;
    new Cookie.Definition("name", "" + ch).toCookie().encode();
  }

  @Test
  public void encodeSessionCookie() throws Exception {
    assertEquals("jooby.sid=1234;Version=1", new Cookie.Definition("jooby.sid", "1234").maxAge(-1)
        .toCookie().encode());
  }

  @Test
  public void nullValue() throws Exception {
    assertEquals("jooby.sid=;Version=1", new Cookie.Definition("jooby.sid", "").maxAge(-1)
        .toCookie().encode());
  }

  @Test
  public void emptyValue() throws Exception {
    assertEquals("jooby.sid=;Version=1", new Cookie.Definition("jooby.sid", "").maxAge(-1)
        .toCookie().encode());
  }

  @Test
  public void quotedValue() throws Exception {
    assertEquals("jooby.sid=\"val 1\";Version=1", new Cookie.Definition("jooby.sid", "\"val 1\"")
        .maxAge(-1)
        .toCookie().encode());
  }

  @Test
  public void encodeHttpOnly() throws Exception {
    assertEquals("jooby.sid=1234;Version=1;HttpOnly",
        new Cookie.Definition("jooby.sid", "1234").httpOnly(true).toCookie()
            .encode());
  }

  @Test
  public void encodeSecure() throws Exception {
    assertEquals("jooby.sid=1234;Version=1;Secure",
        new Cookie.Definition("jooby.sid", "1234").secure(true).toCookie()
            .encode());
  }

  @Test
  public void encodePath() throws Exception {
    assertEquals("jooby.sid=1234;Version=1;Path=/",
        new Cookie.Definition("jooby.sid", "1234").path("/").toCookie().encode());
  }

  @Test
  public void encodeDomain() throws Exception {
    assertEquals("jooby.sid=1234;Version=1;Domain=example.com",
        new Cookie.Definition("jooby.sid", "1234").domain("example.com").toCookie().encode());
  }

  @Test
  public void encodeComment() throws Exception {
    assertEquals("jooby.sid=1234;Version=1;Comment=\"1,2,3\"",
        new Cookie.Definition("jooby.sid", "1234").comment("1,2,3").toCookie()
            .encode());
  }

  @Test
  public void encodeMaxAge0() throws Exception {
    assertEquals("jooby.sid=1234;Version=1;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00 GMT",
        new Cookie.Definition("jooby.sid", "1234").maxAge(0).toCookie().encode());
  }

  @Test
  public void encodeMaxAge60() throws Exception {
    assertTrue(new Cookie.Definition("jooby.sid", "1234")
        .maxAge(60).toCookie().encode().startsWith("jooby.sid=1234;Version=1;Max-Age=60"));

    long millis = 1428708685066L;
    new MockUnit()
        .expect(unit -> {
          unit.mockStatic(System.class);
          expect(System.currentTimeMillis()).andReturn(millis);
        })
        .run(unit -> {
          Instant instant = Instant.ofEpochMilli(millis + 60 * 1000L);
          assertEquals("jooby.sid=1234;Version=1;Max-Age=60;Expires=" + fmt.format(instant),
              new Cookie.Definition("jooby.sid", "1234").maxAge(60).toCookie().encode());
        });
  }

  @Test
  public void encodeEverything() throws Exception {
    assertTrue(new Cookie.Definition("jooby.sid", "1234")
        .maxAge(60).toCookie().encode().startsWith("jooby.sid=1234;Version=1;Max-Age=60"));

    long millis = 1428708685066L;
    new MockUnit()
        .expect(unit -> {
          unit.mockStatic(System.class);
          expect(System.currentTimeMillis()).andReturn(millis);
        })
        .run(
            unit -> {
              Instant instant = Instant.ofEpochMilli(millis + 120 * 1000L);
              assertEquals(
                  "jooby.sid=1234;Version=1;Path=/;Domain=example.com;Secure;HttpOnly;Max-Age=120;Expires="
                      + fmt.format(instant) + ";Comment=c",
                  new Cookie.Definition("jooby.sid", "1234")
                      .comment("c")
                      .domain("example.com")
                      .httpOnly(true)
                      .maxAge(120)
                      .path("/")
                      .secure(true)
                      .toCookie()
                      .encode()
              );
            });
  }
}
