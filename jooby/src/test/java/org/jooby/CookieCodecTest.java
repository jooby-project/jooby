package org.jooby;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.collect.ImmutableMap;

public class CookieCodecTest {

  @Test
  public void encode() {
    assertEquals("success=OK", Cookie.URL_ENCODER.apply(ImmutableMap.of("success", "OK")));
    assertEquals("success=semi%3Bcolon",
        Cookie.URL_ENCODER.apply(ImmutableMap.of("success", "semi;colon")));
    assertEquals("success=eq%3Duals",
        Cookie.URL_ENCODER.apply(ImmutableMap.of("success", "eq=uals")));

    assertEquals("success=OK&error=404",
        Cookie.URL_ENCODER.apply(ImmutableMap.of("success", "OK", "error", "404")));
  }

  @Test
  public void decode() {
    assertEquals(ImmutableMap.of("success", "OK"), Cookie.URL_DECODER.apply("success=OK"));
    assertEquals(ImmutableMap.of("success", "OK", "foo", "bar"),
        Cookie.URL_DECODER.apply("success=OK&foo=bar"));
    assertEquals(ImmutableMap.of("semicolon", "semi;colon"),
        Cookie.URL_DECODER.apply("semicolon=semi%3Bcolon"));
  }
}
