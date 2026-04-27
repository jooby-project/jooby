/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;

import org.junit.jupiter.api.Test;

public class XSSTest {

  @Test
  public void testPrivateConstructor() throws Exception {
    // Access the private constructor to achieve 100% line coverage
    Constructor<XSS> constructor = XSS.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    XSS instance = constructor.newInstance();
    assertNotNull(instance, "Instance should be created successfully via reflection");
  }

  @Test
  public void testUri() {
    // Branch: value == null
    assertEquals("", XSS.uri(null));

    // Branch: value.isEmpty()
    assertEquals("", XSS.uri(""));

    // Branch: Safe characters (should return the same string)
    String safe = "abc-._~123";
    assertEquals(safe, XSS.uri(safe));

    // Branch: Requires escaping (spaces to %20)
    String escaped = XSS.uri("space here");
    assertNotNull(escaped);
    assertTrue(escaped.contains("%20"), "Space should be encoded as %20");
  }

  @Test
  public void testHtml() {
    // Branch: value == null
    assertEquals("", XSS.html(null));

    // Branch: value.isEmpty()
    assertEquals("", XSS.html(""));

    // Branch: Safe characters
    String safe = "safeText";
    assertEquals(safe, XSS.html(safe));

    // Branch: Requires HTML level 2 escaping (<, >, ', ")
    String escaped = XSS.html("<script>alert('xss')</script>");
    assertNotNull(escaped);
    assertTrue(
        escaped.contains("&lt;script&gt;"), "HTML tags should be escaped to named references");
    assertTrue(
        escaped.contains("&#39;") || escaped.contains("&apos;"), "Single quotes should be escaped");
  }

  @Test
  public void testJson() {
    // Branch: value == null
    assertEquals("\"\"", XSS.json(null));

    // Branch: value.isEmpty()
    assertEquals("\"\"", XSS.json(""));

    // Branch: Safe characters
    String safe = "safeString123";
    assertEquals(safe, XSS.json(safe));

    // Branch: Requires JSON level 2 escaping (Quotes, newlines, control characters)
    String escaped = XSS.json("quote\"newline\n");
    assertNotNull(escaped);
    assertTrue(
        escaped.contains("\\\"") || escaped.contains("\\u0022"), "Double quotes should be escaped");
    assertTrue(
        escaped.contains("\\n") || escaped.contains("\\u000A"), "Newlines should be escaped");
  }
}
